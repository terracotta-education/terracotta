package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Question;
import edu.iu.terracotta.dao.entity.QuestionMc;
import edu.iu.terracotta.dao.entity.QuestionSubmission;
import edu.iu.terracotta.dao.entity.integrations.Integration;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AnswerDto;
import edu.iu.terracotta.dao.model.dto.QuestionDto;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.dao.repository.AssessmentRepository;
import edu.iu.terracotta.dao.repository.QuestionRepository;
import edu.iu.terracotta.dao.repository.QuestionSubmissionRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidQuestionTypeException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.service.app.AnswerService;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.app.QuestionService;
import edu.iu.terracotta.service.app.integrations.IntegrationService;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class QuestionServiceImpl implements QuestionService {

    @Autowired private AssessmentRepository assessmentRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private QuestionSubmissionRepository questionSubmissionRepository;
    @Autowired private AnswerService answerService;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private IntegrationService integrationService;

    @PersistenceContext private EntityManager entityManager;

    private List<Question> findAllByAssessmentId(Long assessmentId) {
        return questionRepository.findByAssessment_AssessmentIdOrderByQuestionOrder(assessmentId);
    }

    @Override
    public List<QuestionDto> getQuestions(Long assessmentId) {
        return CollectionUtils.emptyIfNull(findAllByAssessmentId(assessmentId)).stream()
            .map(question -> toDto(question, false, false))
            .toList();
    }

    @Override
    public Question getQuestion(Long id) {
        return questionRepository.findByQuestionId(id);
    }

    @Override
    public QuestionDto postQuestion(QuestionDto questionDto, long assessmentId, boolean answers, boolean isNew) throws IdInPostException, DataServiceException, MultipleChoiceLimitReachedException, IntegrationNotFoundException, IntegrationClientNotFoundException {
        if (questionDto.getQuestionId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        questionDto.setAssessmentId(assessmentId);
        Question question;

        try {
            validateQuestionType(questionDto);
            question = save(fromDto(questionDto));

            switch (question.getQuestionType()) {
                case MC:
                    if (CollectionUtils.isEmpty(questionDto.getAnswers())) {
                        break;
                    }

                    for (AnswerDto answerDto : questionDto.getAnswers()) {
                        answerService.postAnswerMC(answerDto, question.getQuestionId());
                    }
                    break;
                case INTEGRATION:
                    if (isNew) {
                        question.setIntegration(integrationService.create(question, questionDto.getIntegrationClientId()));
                        break;
                    }

                    integrationService.duplicate(integrationService.findByUuid(questionDto.getIntegration().getId()), question);
                    break;
                case ESSAY:
                case FILE:
                case PAGE_BREAK:
                default:
                    break;
            }
        } catch (DataServiceException | InvalidQuestionTypeException | NegativePointsException ex) {
            throw new DataServiceException("Error 105: Unable to create Question: " + ex.getMessage(), ex);
        }

        return toDto(question, answers, true);
    }

    @Override
    public QuestionDto toDto(Question question, boolean answers, boolean showCorrectAnswer) {
        return toDto(question, null, answers, showCorrectAnswer);
    }

    @Override
    public QuestionDto toDto(Question question, Long submissionId, boolean answers, boolean showCorrectAnswer) {
        QuestionDto questionDto = new QuestionDto();
        questionDto.setQuestionId(question.getQuestionId());
        questionDto.setHtml(fileStorageService.parseHTMLFiles(question.getHtml(), question.getAssessment().getTreatment().getAssignment().getExposure().getExperiment().getPlatformDeployment().getLocalUrl()));
        questionDto.setQuestionOrder(question.getQuestionOrder());
        questionDto.setPoints(question.getPoints());
        questionDto.setAssessmentId(question.getAssessment().getAssessmentId());
        questionDto.setQuestionType(question.getQuestionType().name());
        questionDto.setIntegration(integrationService.toDto(question.getIntegration()));

        if (QuestionTypes.MC == question.getQuestionType()) {
            if (answers) {
                Optional<QuestionSubmission> questionSubmission = Optional.empty();

                if (submissionId != null) {
                    questionSubmission = questionSubmissionRepository.findByQuestion_QuestionIdAndSubmission_SubmissionId(question.getQuestionId(), submissionId);
                }

                if (questionSubmission.isPresent()) {
                    // Apply submission specific order to answers
                    questionDto.setAnswers(answerService.findAllByQuestionIdMC(questionSubmission.get(), showCorrectAnswer));
                } else {
                    questionDto.setAnswers(answerService.findAllByQuestionIdMC(question.getQuestionId(), showCorrectAnswer));
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

        if (questionDto.getPoints() < 0) {
            throw new NegativePointsException("Error 142: The point value cannot be negative.");
        }

        question.setPoints(questionDto.getPoints());
        question.setQuestionOrder(questionDto.getQuestionOrder());
        question.setQuestionType(questionType);
        Optional<Assessment> assessment = assessmentRepository.findById(questionDto.getAssessmentId());

        if (assessment.isEmpty()) {
            throw new DataServiceException("The assessment for the question does not exist");
        }

        question.setAssessment(assessment.get());

        return question;
    }

    @Override
    public Question save(Question question) {
        return questionRepository.save(question);
    }

    @Override
    @Transactional
    public void updateQuestion(Map<Question, QuestionDto> map)
        throws NegativePointsException, IntegrationNotFoundException, IntegrationNotMatchingException, IntegrationConfigurationNotFoundException,
            IntegrationConfigurationNotMatchingException, IntegrationClientNotFoundException {
        for (Map.Entry<Question, QuestionDto> entry : map.entrySet()) {
            Question question = entry.getKey();
            QuestionDto questionDto = entry.getValue();
            question.setHtml(questionDto.getHtml());
            question.setQuestionOrder(questionDto.getQuestionOrder());

            if (questionDto.getPoints() < 0) {
                throw new NegativePointsException("Error 142: The point value cannot be negative.");
            }

            question.setPoints(questionDto.getPoints());

            switch (question.getQuestionType()) {
                case MC:
                    ((QuestionMc) question).setRandomizeAnswers(questionDto.isRandomizeAnswers());
                    break;
                case INTEGRATION:
                    question.setIntegration(
                        integrationService.update(questionDto.getIntegration(), question)
                    );
                    break;
                case ESSAY:
                case FILE:
                case PAGE_BREAK:
                default:
                    break;
            }

            save(question);
        }
    }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException {
        Question question = questionRepository.findByQuestionId(id);

        if (question.getIntegration() != null) {
            integrationService.delete(question.getIntegration());
        }

        questionRepository.deleteByQuestionId(id);
    }

    @Override
    public Question findByQuestionId(Long id) {
        return questionRepository.findByQuestionId(id);
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId, Long questionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/questions/{questionId}")
                .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId, questionId).toUri());

        return headers;
    }

    @Override
    public void validateQuestionType(QuestionDto questionDto) throws InvalidQuestionTypeException {
        if (questionDto.getQuestionType() == null) {
            throw new InvalidQuestionTypeException("Error 119: Must include a question type in the post.");
        }

        if (!EnumUtils.isValidEnum(QuestionTypes.class, questionDto.getQuestionType())) {
            throw new InvalidQuestionTypeException("Error 103: Please use a supported question type.");
        }
    }

    @Override
    public List<Question> duplicateQuestionsForAssessment(Long oldAssessmentId, Assessment newAssessment) throws DataServiceException, QuestionNotMatchingException {
        if (newAssessment == null) {
            throw new DataServiceException("The new assessment with the given ID does not exist");
        }

        List<Question> originalQuestions = findAllByAssessmentId(oldAssessmentId);

        if (CollectionUtils.isEmpty(originalQuestions)) {
            return Collections.emptyList();
        }

        List<Question> questions = new ArrayList<>();

        for (Question originalQuestion : originalQuestions) {
            entityManager.detach(originalQuestion);
            Long originalQuestionId = originalQuestion.getQuestionId();
            Integration integration = originalQuestion.getIntegration();
            originalQuestion.setQuestionId(null);
            originalQuestion.setAssessment(newAssessment);
            originalQuestion.setIntegration(null);
            Question newQuestion = questionRepository.save(originalQuestion);

            answerService.duplicateAnswersForQuestion(originalQuestionId, newQuestion);

            if (newQuestion.isIntegration()) {
                integrationService.duplicate(integration, newQuestion);
                questionRepository.save(newQuestion);
            }

            questions.add(newQuestion);
        }

        return questions;
    }

}
