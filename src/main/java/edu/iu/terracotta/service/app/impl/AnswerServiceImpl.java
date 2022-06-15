package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.AnswerMcSubmissionOption;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.dto.AnswerDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AnswerService;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.utils.TextConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AnswerServiceImpl implements AnswerService {

    @Autowired
    AllRepositories allRepositories;

    @Autowired
    FileStorageService fileStorageService;

    /*
    MULTIPLE CHOICE
     */
    @Override
    public List<AnswerDto> findAllByQuestionIdMC(Long questionId, boolean student) {
        List<AnswerMc> answerList = allRepositories.answerMcRepository.findByQuestion_QuestionId(questionId);
        List<AnswerDto> answerDtoList = new ArrayList<>();
        if (!answerList.isEmpty()) {
            for (AnswerMc answerMc : answerList) {
                answerDtoList.add(toDtoMC(answerMc, answerMc.getAnswerOrder(), student));
            }
        }
        return answerDtoList;
    }

    /**
     * Apply submission specific, possibly random, ordering to answers.
     */
    @Override
    public List<AnswerDto> findAllByQuestionIdMC(QuestionSubmission questionSubmission) {
        List<AnswerMc> answerList = allRepositories.answerMcRepository
                .findByQuestion_QuestionId(questionSubmission.getQuestion().getQuestionId());

        // Get the answers in the order they are to be presented for this submission
        List<AnswerMcSubmissionOption> answerMcSubmissionOptions = questionSubmission.getAnswerMcSubmissionOptions();

        // sort options
        answerMcSubmissionOptions.sort(Comparator.comparingLong(AnswerMcSubmissionOption::getAnswerOrder));

        // loop over them and add to dto list
        List<AnswerDto> answerDtoList = new ArrayList<>();
        int answerOrder = 0;
        for (AnswerMcSubmissionOption answerMcSubmissionOption : answerMcSubmissionOptions) {
            answerDtoList.add(toDtoMC(answerMcSubmissionOption.getAnswerMc(), answerOrder++, true));
        }

        // check for any missing answers and add them to the list as well
        for (AnswerMc answerMc : answerList) {
            if (answerDtoList.stream().noneMatch(a -> a.getAnswerId().equals(answerMc.getAnswerMcId()))) {
                answerDtoList.add(toDtoMC(answerMc, answerOrder++, true));
            }
        }

        return answerDtoList;
    }

    @Override
    public AnswerDto getAnswerMC(Long answerId, boolean student){
        AnswerMc answerMc = allRepositories.answerMcRepository.findByAnswerMcId(answerId);
        return toDtoMC(answerMc, answerMc.getAnswerOrder(), student);
    }

    @Override
    public AnswerDto postAnswerMC(AnswerDto answerDto, long questionId) throws IdInPostException, DataServiceException, MultipleChoiceLimitReachedException{
        if (answerDto.getAnswerId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        answerDto.setQuestionId(questionId);
        answerDto.setAnswerType(getQuestionType(questionId));
        if ("MC".equals(answerDto.getAnswerType())) {
            limitReached(questionId);
            AnswerMc answerMc;
            try {
                answerMc = fromDtoMC(answerDto);
            } catch (DataServiceException ex) {
                throw new DataServiceException("Error 105: Unable to create Answer: " + ex.getMessage());
            }
            return toDtoMC(saveMC(answerMc), answerMc.getAnswerOrder(),false);
        } else {
            throw new DataServiceException("Error 103: Answer type not supported.");
        }
    }

    @Override
    public AnswerDto toDtoMC(AnswerMc answer, int answerOrder, boolean student) {
        AnswerDto answerDto = new AnswerDto();
        answerDto.setAnswerId(answer.getAnswerMcId());
        answerDto.setHtml(fileStorageService.parseHTMLFiles(answer.getHtml()));
        answerDto.setAnswerOrder(answerOrder);
        answerDto.setQuestionId(answer.getQuestion().getQuestionId());
        answerDto.setAnswerType("MC");
        if(student){
            answerDto.setCorrect(null);
        } else {
            answerDto.setCorrect(answer.getCorrect());
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
        if(question.isPresent()){
            answer.setQuestion(question.get());
        } else {
            throw new DataServiceException("The question for the answer does not exist");
        }
        return answer;
    }

    @Override
    public AnswerMc saveMC(AnswerMc answer) { return allRepositories.answerMcRepository.save(answer); }

    @Override
    public AnswerMc findByAnswerId(Long answerId) { return allRepositories.answerMcRepository.findByAnswerMcId(answerId); }

    @Override
    @Transactional
    public void updateAnswerMC(Map<AnswerMc, AnswerDto> map){
        for(Map.Entry<AnswerMc, AnswerDto> entry : map.entrySet()){
            AnswerMc answerMc = entry.getKey();
            AnswerDto answerDto = entry.getValue();
            if(answerDto.getHtml() != null)
                answerMc.setHtml(answerDto.getHtml());
            if(answerDto.getAnswerOrder() != null)
                answerMc.setAnswerOrder(answerDto.getAnswerOrder());
            if(answerDto.getCorrect() != null)
                answerMc.setCorrect(answerDto.getCorrect());
            saveMC(answerMc);
        }
    }


    @Override
    public void deleteByIdMC(Long id) { allRepositories.answerMcRepository.deleteByAnswerMcId(id); }

    @Override
    public boolean mcAnswerBelongsToQuestionAndAssessment(Long assessmentId, Long questionId, Long answerId) {
        return allRepositories.answerMcRepository.existsByQuestion_Assessment_AssessmentIdAndQuestion_QuestionIdAndAnswerMcId(assessmentId, questionId, answerId);
    }

    @Override
    public void limitReached(Long questionId) throws MultipleChoiceLimitReachedException {
        List<AnswerMc> answerList = allRepositories.answerMcRepository.findByQuestion_QuestionId(questionId);
        if(answerList.size() == 20){
            throw new MultipleChoiceLimitReachedException("Error 120: The multiple choice option limit of 20 options has been reached.");
        }
    }

    @Override
    public String getQuestionType(Long questionId){
        return allRepositories.questionRepository.findByQuestionId(questionId).getQuestionType().toString();
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long questionId, Long answerId){
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path(
                "/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}/answers/{answer_id}")
                .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId, questionId, answerId).toUri());
        return headers;
    }
}
