package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.AnswerMcSubmissionOption;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.QuestionMc;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.dto.AnswerDto;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AnswerService;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Component
@SuppressWarnings({"PMD.PreserveStackTrace"})
public class AnswerServiceImpl implements AnswerService {

    @Autowired private AllRepositories allRepositories;
    @Autowired private FileStorageService fileStorageService;

    @PersistenceContext private EntityManager entityManager;

    /*
    MULTIPLE CHOICE
     */
    @Override
    public List<AnswerDto> findAllByQuestionIdMC(Long questionId, boolean showCorrectAnswer) {
        return CollectionUtils.emptyIfNull(allRepositories.answerMcRepository.findByQuestion_QuestionId(questionId)).stream()
            .map(answerMc -> toDtoMC(answerMc, answerMc.getAnswerOrder(), showCorrectAnswer))
            .toList();
    }

    /**
     * Apply submission specific, possibly random, ordering to answers.
     */
    @Override
    public List<AnswerDto> findAllByQuestionIdMC(QuestionSubmission questionSubmission, boolean showCorrectAnswer) {
        List<AnswerMc> answerList = allRepositories.answerMcRepository.findByQuestion_QuestionId(questionSubmission.getQuestion().getQuestionId());

        // Get the answers in the order they are to be presented for this submission
        List<AnswerMcSubmissionOption> answerMcSubmissionOptions = questionSubmission.getAnswerMcSubmissionOptions();

        // sort options
        answerMcSubmissionOptions.sort(Comparator.comparingLong(AnswerMcSubmissionOption::getAnswerOrder));

        // loop over them and add to dto list
        List<AnswerDto> answerDtoList = new ArrayList<>();
        int answerOrder = 0;

        for (AnswerMcSubmissionOption answerMcSubmissionOption : answerMcSubmissionOptions) {
            answerDtoList.add(toDtoMC(answerMcSubmissionOption.getAnswerMc(), answerOrder++, showCorrectAnswer));
        }

        // check for any missing answers and add them to the list as well
        for (AnswerMc answerMc : answerList) {
            if (answerDtoList.stream().noneMatch(a -> a.getAnswerId().equals(answerMc.getAnswerMcId()))) {
                answerDtoList.add(toDtoMC(answerMc, answerOrder++, showCorrectAnswer));
            }
        }

        return answerDtoList;
    }

    @Override
    public AnswerDto getAnswerMC(Long answerId) {
        AnswerMc answerMc = allRepositories.answerMcRepository.findByAnswerMcId(answerId);

        return toDtoMC(answerMc, answerMc.getAnswerOrder(), false);
    }

    @Override
    public AnswerDto postAnswerMC(AnswerDto answerDto, long questionId) throws IdInPostException, DataServiceException, MultipleChoiceLimitReachedException{
        if (answerDto.getAnswerId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        answerDto.setQuestionId(questionId);
        answerDto.setAnswerType(getQuestionType(questionId));

        if (!QuestionTypes.MC.toString().equals(answerDto.getAnswerType())) {
            throw new DataServiceException("Error 103: Answer type not supported.");
        }

        limitReached(questionId);
        AnswerMc answerMc;

        try {
            answerMc = fromDtoMC(answerDto);
        } catch (DataServiceException ex) {
            throw new DataServiceException("Error 105: Unable to create Answer: " + ex.getMessage());
        }

        return toDtoMC(saveMC(answerMc), answerMc.getAnswerOrder(), false);
    }

    @Override
    public AnswerDto toDtoMC(AnswerMc answer, int answerOrder, boolean showCorrectAnswer) {
        AnswerDto answerDto = new AnswerDto();
        answerDto.setAnswerId(answer.getAnswerMcId());
        answerDto.setHtml(fileStorageService.parseHTMLFiles(
            answer.getHtml(),
            answer.getQuestion().getAssessment().getTreatment().getAssignment().getExposure().getExperiment().getPlatformDeployment().getLocalUrl())
        );
        answerDto.setAnswerOrder(answerOrder);
        answerDto.setQuestionId(answer.getQuestion().getQuestionId());
        answerDto.setAnswerType(QuestionTypes.MC.toString());

        if (showCorrectAnswer) {
            answerDto.setCorrect(answer.getCorrect());
        } else {
            answerDto.setCorrect(null);
        }

        return answerDto;
    }

    @Override
    public AnswerMc fromDtoMC(AnswerDto answerDto) throws DataServiceException {
        AnswerMc answer = new AnswerMc();
        answer.setAnswerMcId(answerDto.getAnswerId());
        answer.setHtml(answerDto.getHtml());
        answer.setCorrect(answerDto.getCorrect());
        answer.setAnswerOrder(answerDto.getAnswerOrder());
        Optional<Question> question = allRepositories.questionRepository.findById(answerDto.getQuestionId());

        if (!question.isPresent()) {
            throw new DataServiceException("The question for the answer does not exist");
        }

        answer.setQuestion(question.get());

        return answer;
    }

    private AnswerMc saveMC(AnswerMc answer) {
        return allRepositories.answerMcRepository.save(answer);
    }

    @Override
    public AnswerMc findByAnswerId(Long answerId) {
        return allRepositories.answerMcRepository.findByAnswerMcId(answerId);
    }

    @Override
    @Transactional
    public List<AnswerDto> updateAnswerMC(Map<AnswerMc, AnswerDto> map) {
        List<AnswerDto> answerDtos = new ArrayList<>();

        for (Map.Entry<AnswerMc, AnswerDto> entry : map.entrySet()) {
            AnswerMc answerMc = entry.getKey();
            AnswerDto answerDto = entry.getValue();

            if (answerDto.getHtml() != null) {
                answerMc.setHtml(answerDto.getHtml());
            }

            if (answerDto.getAnswerOrder() != null) {
                answerMc.setAnswerOrder(answerDto.getAnswerOrder());
            }

            if (answerDto.getCorrect() != null) {
                answerMc.setCorrect(answerDto.getCorrect());
            }

            answerDtos.add(toDtoMC(saveMC(answerMc), answerMc.getAnswerOrder(), true));
        }

        return answerDtos;
    }


    @Override
    public void deleteByIdMC(Long id) {
        allRepositories.answerMcRepository.deleteByAnswerMcId(id);
    }

    @Override
    public void limitReached(Long questionId) throws MultipleChoiceLimitReachedException {
        List<AnswerMc> answerList = allRepositories.answerMcRepository.findByQuestion_QuestionId(questionId);

        if (answerList.size() == 20) {
            throw new MultipleChoiceLimitReachedException("Error 120: The multiple choice option limit of 20 options has been reached.");
        }
    }

    @Override
    public String getQuestionType(Long questionId) {
        return allRepositories.questionRepository.findByQuestionId(questionId).getQuestionType().toString();
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long questionId, Long answerId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path(
                "/api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/questions/{questionId}/answers/{answerId}")
                .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId, questionId, answerId).toUri());

        return headers;
    }

    @Override
    public List<AnswerMc> duplicateAnswersForQuestion(Long originalQuestionId, Question newQuestion) throws QuestionNotMatchingException {
        if (originalQuestionId == null || newQuestion == null) {
            throw new QuestionNotMatchingException(TextConstants.QUESTION_NOT_MATCHING);
        }

        if (!(newQuestion instanceof QuestionMc)) {
            // not MC; nothing to duplicate
            return Collections.emptyList();
        }

        // copy MC options
        return CollectionUtils.emptyIfNull(allRepositories.answerMcRepository.findByQuestion_QuestionId(originalQuestionId)).stream()
            .map(
                answerMc -> {
                    entityManager.detach(answerMc);
                    answerMc.setAnswerMcId(null);
                    answerMc.setQuestion(newQuestion);

                    return saveMC(answerMc);
                }
            )
            .collect(Collectors.toList());
    }

}
