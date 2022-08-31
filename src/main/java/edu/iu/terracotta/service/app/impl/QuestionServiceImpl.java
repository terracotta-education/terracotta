package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidQuestionTypeException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.QuestionMc;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.dto.QuestionDto;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AnswerService;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.app.QuestionService;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class QuestionServiceImpl implements QuestionService {

    @Autowired
    AllRepositories allRepositories;

    @Autowired
    AnswerService answerService;

    @Autowired
    FileStorageService fileStorageService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Question> findAllByAssessmentId(Long assessmentId) {
        return allRepositories.questionRepository.findByAssessment_AssessmentIdOrderByQuestionOrder(assessmentId);
    }

    @Override
    public List<QuestionDto> getQuestions(Long assessmentId){
        List<Question> questions = findAllByAssessmentId(assessmentId);
        List<QuestionDto> questionDtoList = new ArrayList<>();
        for(Question question : questions){
            questionDtoList.add(toDto(question, false, false));
        }
        return questionDtoList;
    }

    @Override
    public Question getQuestion(Long id){ return allRepositories.questionRepository.findByQuestionId(id); }

    @Override
    public QuestionDto postQuestion(QuestionDto questionDto, long assessmentId, boolean answers) throws IdInPostException, DataServiceException {
        if(questionDto.getQuestionId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }
        questionDto.setAssessmentId(assessmentId);
        Question question;
        try {
            validateQuestionType(questionDto);
            question = fromDto(questionDto);
        } catch (DataServiceException | InvalidQuestionTypeException | NegativePointsException ex) {
            throw new DataServiceException("Error 105: Unable to create Question: " + ex.getMessage());
        }
        return toDto(save(question), answers, true);
    }

    @Override
    public QuestionDto toDto(Question question, boolean answers, boolean student) {
        return toDto(question, null, answers, student);
    }

    @Override
    public QuestionDto toDto(Question question, Long submissionId, boolean answers, boolean student) {

        QuestionDto questionDto = new QuestionDto();
        questionDto.setQuestionId(question.getQuestionId());
        questionDto.setHtml(fileStorageService.parseHTMLFiles(question.getHtml()));
        questionDto.setQuestionOrder(question.getQuestionOrder());
        questionDto.setPoints(question.getPoints());
        questionDto.setAssessmentId(question.getAssessment().getAssessmentId());
        questionDto.setQuestionType(question.getQuestionType().name());
        if (question.getQuestionType() == QuestionTypes.MC) {
            if (answers) {
                Optional<QuestionSubmission> questionSubmission = Optional.empty();
                if (submissionId != null) {
                    questionSubmission = this.allRepositories.questionSubmissionRepository
                            .findByQuestion_QuestionIdAndSubmission_SubmissionId(question.getQuestionId(),
                                    submissionId);
                }
                if (questionSubmission.isPresent()) {
                    // Apply submission specific order to answers
                    questionDto.setAnswers(answerService.findAllByQuestionIdMC(questionSubmission.get()));
                } else {
                    questionDto.setAnswers(answerService.findAllByQuestionIdMC(question.getQuestionId(), student));
                }
            }
            questionDto.setRandomizeAnswers(((QuestionMc) question).isRandomizeAnswers());
        }
        return questionDto;
    }

    @Override
    public Question fromDto(QuestionDto questionDto) throws DataServiceException, NegativePointsException {

        Question question;
        QuestionTypes questionType = QuestionTypes.valueOf(questionDto.getQuestionType());
        if (questionType == QuestionTypes.MC) {
            QuestionMc questionMc = new QuestionMc();
            questionMc.setRandomizeAnswers(questionDto.isRandomizeAnswers());
            question = questionMc;
        } else {
            question = new Question();
        }
        question.setQuestionId(questionDto.getQuestionId());
        question.setHtml(questionDto.getHtml());
        if(questionDto.getPoints() >= 0){
            question.setPoints(questionDto.getPoints());
        } else {
            throw new NegativePointsException("Error 142: The point value cannot be negative.");
        }
        question.setQuestionOrder(questionDto.getQuestionOrder());
        question.setQuestionType(questionType);
        Optional<Assessment> assessment = allRepositories.assessmentRepository.findById(questionDto.getAssessmentId());
        if(assessment.isPresent()) {
            question.setAssessment(assessment.get());
        } else {
            throw new DataServiceException("The assessment for the question does not exist");
        }
        return question;
    }

    @Override
    public Question save(Question question) { return allRepositories.questionRepository.save(question); }

    @Override
    public Optional<Question> findById(Long id) { return allRepositories.questionRepository.findById(id); }

    @Override
    @Transactional
    public void updateQuestion(Map<Question, QuestionDto> map) throws NegativePointsException {
        for(Map.Entry<Question, QuestionDto> entry : map.entrySet()){
            Question question = entry.getKey();
            QuestionDto questionDto = entry.getValue();
            question.setHtml(questionDto.getHtml());
            question.setQuestionOrder(questionDto.getQuestionOrder());
            if(questionDto.getPoints() >= 0){
                question.setPoints(questionDto.getPoints());
            } else {
                throw new NegativePointsException("Error 142: The point value cannot be negative.");
            }
            if(question.getQuestionType() == QuestionTypes.MC) {
                ((QuestionMc)question).setRandomizeAnswers(questionDto.isRandomizeAnswers());
            }
            save(question);
        }
    }

    @Override
    public void saveAndFlush(Question questionToChange) { allRepositories.questionRepository.saveAndFlush(questionToChange); }


    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException {
        allRepositories.questionRepository.deleteByQuestionId(id);
    }

    @Override
    public boolean questionBelongsToAssessment(Long assessmentId, Long questionId) {
        return allRepositories.questionRepository.existsByAssessment_AssessmentIdAndQuestionId(assessmentId, questionId);
    }

    @Override
    public Question findByQuestionId(Long id) {
        return allRepositories.questionRepository.findByQuestionId(id);
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long questionId){
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}")
                .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId, questionId).toUri());
        return headers;
    }

    @Override
    public void validateQuestionType(QuestionDto questionDto) throws InvalidQuestionTypeException {
        if(questionDto.getQuestionType() == null){
            throw new InvalidQuestionTypeException("Error 119: Must include a question type in the post.");
        }
        if(!EnumUtils.isValidEnum(QuestionTypes.class, questionDto.getQuestionType())){
            throw new InvalidQuestionTypeException("Error 103: Please use a supported question type.");
        }
    }

    @Override
    public List<QuestionDto> duplicateQuestionsForAssessment(Long oldAssessmentId, Long newAssessmentId) throws DataServiceException {
        Assessment oldAssessment = allRepositories.assessmentRepository.findByAssessmentId(oldAssessmentId);

        if (oldAssessment == null) {
            throw new DataServiceException("The old assessment with the given ID does not exist");
        }

        Assessment newAssessment = allRepositories.assessmentRepository.findByAssessmentId(newAssessmentId);

        if (newAssessment == null) {
            throw new DataServiceException("The new assessment with the given ID does not exist");
        }

        List<QuestionDto> questionDtos = CollectionUtils.emptyIfNull(oldAssessment.getQuestions()).stream()
            .map(
                question -> {
                    entityManager.detach(question);
                    question.setQuestionId(null);
                    question.setAssessment(newAssessment);
                    Question newQuestion = save(question);

                    return toDto(newQuestion, false, false);
                }
            )
            .collect(Collectors.toList());

        return questionDtos;
    }

}
