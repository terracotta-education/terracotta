package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.Question;
import edu.iu.terracotta.dao.entity.QuestionMc;
import edu.iu.terracotta.dao.entity.RegradeDetails;
import edu.iu.terracotta.dao.entity.RetakeDetails;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AssessmentDto;
import edu.iu.terracotta.dao.model.dto.QuestionDto;
import edu.iu.terracotta.dao.model.dto.SubmissionDto;
import edu.iu.terracotta.dao.model.enums.MultipleSubmissionScoringScheme;
import edu.iu.terracotta.dao.model.enums.RegradeOption;
import edu.iu.terracotta.dao.repository.AnswerEssaySubmissionRepository;
import edu.iu.terracotta.dao.repository.AnswerFileSubmissionRepository;
import edu.iu.terracotta.dao.repository.AnswerMcSubmissionRepository;
import edu.iu.terracotta.dao.repository.AssessmentRepository;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ConditionRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.ExposureGroupConditionRepository;
import edu.iu.terracotta.dao.repository.ParticipantRepository;
import edu.iu.terracotta.dao.repository.QuestionMcRepository;
import edu.iu.terracotta.dao.repository.QuestionRepository;
import edu.iu.terracotta.dao.repository.SubmissionRepository;
import edu.iu.terracotta.dao.repository.TreatmentRepository;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.AssessmentSubmissionService;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.service.app.QuestionService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.app.integrations.IntegrationClientService;
import edu.iu.terracotta.service.app.integrations.IntegrationLaunchParameterService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
@SuppressWarnings({"PMD.PreserveStackTrace", "PMD.GuardLogStatement"})
public class AssessmentServiceImpl implements AssessmentService {

    public static final int TITLE_MAX_LENGTH = 255;

    @Autowired private AnswerEssaySubmissionRepository answerEssaySubmissionRepository;
    @Autowired private AnswerFileSubmissionRepository answerFileSubmissionRepository;
    @Autowired private AnswerMcSubmissionRepository answerMcSubmissionRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private AssessmentRepository assessmentRepository;
    @Autowired private ConditionRepository conditionRepository;
    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private ExposureGroupConditionRepository exposureGroupConditionRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private QuestionMcRepository questionMcRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private TreatmentRepository treatmentRepository;
    @Autowired private ApiJwtService apiJwtService;
    @Autowired private AssessmentSubmissionService assessmentSubmissionService;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private IntegrationClientService integrationClientService;
    @Autowired private IntegrationLaunchParameterService integrationLaunchParameterService;
    @Autowired private ParticipantService participantService;
    @Autowired private QuestionService questionService;
    @Autowired private SubmissionService submissionService;

    @PersistenceContext private EntityManager entityManager;

    private List<Assessment> findAllByTreatmentId(Long treatmentId) {
        return assessmentRepository.findByTreatment_TreatmentId(treatmentId);
    }

    @Override
    public List<AssessmentDto> getAllAssessmentsByTreatment(Long treatmentId, boolean submissions) throws AssessmentNotMatchingException {
        List<Assessment> assessmentList = findAllByTreatmentId(treatmentId);
        List<AssessmentDto> assessmentDtoList = new ArrayList<>();

        for (Assessment assessment : assessmentList) {
            assessmentDtoList.add(toDto(assessment, false, false, submissions, false));
        }

        return assessmentDtoList;
    }

    @Override
    public AssessmentDto postAssessment(AssessmentDto assessmentDto, long treatmentId)
            throws IdInPostException, DataServiceException, TitleValidationException, AssessmentNotMatchingException {
        if (assessmentDto.getAssessmentId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        // treatment level settings are ignored in the DTO when creating an
        // assessment and will be copied from the Treatment's Assignment. These
        // settings can be updated for an assessment by calling
        // updateAssessment.
        assessmentDto = defaultAssessment(assessmentDto, treatmentId);
        Assessment assessment;

        try {
            assessment = fromDto(assessmentDto);
            assessment.setQuestions(new ArrayList<>());
        } catch (DataServiceException ex) {
            throw new DataServiceException("Error 105: Unable to create Assessment: " + ex.getMessage());
        }

        Assessment assessmentSaved = save(assessment);
        updateTreatment(treatmentId, assessmentSaved);

        return toDto(assessmentSaved, false, false, false, false);
    }

    private AssessmentDto toDto(Assessment assessment, Participant participant, boolean canViewSubmissions) throws AssessmentNotMatchingException {
        AssessmentDto assessmentDto = toDto(assessment, null, false, false, false, false);

        List<Submission> participantAssessmentSubmissionsSubmitted = CollectionUtils.emptyIfNull(submissionRepository.findByParticipant_IdAndAssessment_AssessmentId(participant.getParticipantId(), assessment.getAssessmentId())).stream()
            .filter(submission -> submission.getDateSubmitted() != null)
            .toList();

        List<SubmissionDto> submissionDtosSubmitted = CollectionUtils.emptyIfNull(participantAssessmentSubmissionsSubmitted).stream()
            .map(submission -> submissionService.toDto(submission, false, false))
            .toList();

        if (canViewSubmissions) {
            assessmentDto.setSubmissions(submissionDtosSubmitted);
        }

        RetakeDetails retakeDetails = new RetakeDetails();

        try {
            verifySubmissionLimit(assessment.getNumOfSubmissions(), submissionDtosSubmitted.size());
            verifySubmissionWaitTime(assessment.getHoursBetweenSubmissions(), participantAssessmentSubmissionsSubmitted);

            retakeDetails.setRetakeAllowed(true);
        } catch (AssignmentAttemptException e) {
            retakeDetails.setRetakeAllowed(false);
            retakeDetails.setRetakeNotAllowedReason(RetakeDetails.calculateRetakeNotAllowedReason(e.getMessage()));
        }

        Optional<Submission> lastSubmission = CollectionUtils.emptyIfNull(participantAssessmentSubmissionsSubmitted)
                .stream()
                .sorted(Comparator.comparingLong(Submission::getSubmissionId).reversed())
                .findFirst();

        if (lastSubmission.isPresent()) {
            // set last submission score
            retakeDetails.setLastAttemptScore(submissionService.getSubmissionScore(lastSubmission.get()));
        }

        retakeDetails.setKeptScore(submissionService.getScoreFromMultipleSubmissions(participant, assessment));
        retakeDetails.setSubmissionAttemptsCount(submissionDtosSubmitted.size());
        assessmentDto.setRetakeDetails(retakeDetails);

        return assessmentDto;
    }

    @Override
    public AssessmentDto toDto(Assessment assessment, boolean questions, boolean answers, boolean submissions, boolean isStudent) throws AssessmentNotMatchingException {
        return toDto(assessment, null, questions, answers, submissions, isStudent);
    }

    @Override
    public AssessmentDto toDto(Assessment assessment, Long submissionId, boolean questions, boolean answers, boolean submissions, boolean isStudent)
            throws AssessmentNotMatchingException {
        Long submissionsCompletedCount = null;
        Long submissionsInProgressCount = null;
        AssessmentDto assessmentDto = new AssessmentDto();
        assessmentDto.setAssessmentId(assessment.getAssessmentId());
        assessmentDto.setHtml(fileStorageService.parseHTMLFiles(assessment.getHtml(), assessment.getTreatment().getAssignment().getExposure().getExperiment().getPlatformDeployment().getLocalUrl()));
        assessmentDto.setAutoSubmit(assessment.isAutoSubmit());
        assessmentDto.setNumOfSubmissions(assessment.getNumOfSubmissions());
        assessmentDto.setHoursBetweenSubmissions(assessment.getHoursBetweenSubmissions());
        assessmentDto.setMultipleSubmissionScoringScheme(assessment.getMultipleSubmissionScoringScheme().name());
        assessmentDto.setCumulativeScoringInitialPercentage(assessment.getCumulativeScoringInitialPercentage());
        assessmentDto.setAllowStudentViewResponses(assessment.isAllowStudentViewResponses());
        assessmentDto.setStudentViewResponsesAfter(assessment.getStudentViewResponsesAfter());
        assessmentDto.setStudentViewResponsesBefore(assessment.getStudentViewResponsesBefore());
        assessmentDto.setAllowStudentViewCorrectAnswers(assessment.isAllowStudentViewCorrectAnswers());
        assessmentDto.setStudentViewCorrectAnswersAfter(assessment.getStudentViewCorrectAnswersAfter());
        assessmentDto.setStudentViewCorrectAnswersBefore(assessment.getStudentViewCorrectAnswersBefore());
        assessmentDto.setQuestions(handleQuestionDtos(assessment, submissionId, questions, answers, isStudent));
        assessmentDto.setIntegration(assessment.isIntegration());
        assessmentDto.setIntegrationClients(
            integrationClientService.toDto(
                integrationClientService.getAll(),
                assessment.getTreatment().getCondition().getExperiment().getLtiContextEntity().getToolDeployment().getPlatformDeployment().getLocalUrl()
            )
        );

        if (assessment.isIntegration()) {
            // is integration-type assessment; set preview url
            assessmentDto.setIntegrationPreviewUrl(
                String.format(
                    "%s%s",
                    assessment.getIntegration().getLaunchUrl(),
                    integrationLaunchParameterService.buildPreviewQueryString(assessment.getIntegration())
                )
            );
        }

        List<SubmissionDto> submissionDtoList = new ArrayList<>();
        Long conditionId = assessment.getTreatment().getCondition().getConditionId();
        Long exposureId = assessment.getTreatment().getAssignment().getExposure().getExposureId();
        Optional<ExposureGroupCondition> exposureGroupCondition = exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(conditionId, exposureId);
        Long groupId = null;

        if (exposureGroupCondition.isEmpty()) {
            throw new AssessmentNotMatchingException(String.format("Error 124: Assessment [%s] is without a group", assessment.getAssessmentId()));
        }

        groupId = exposureGroupCondition.get().getGroup().getGroupId();
        Map<Participant, Boolean> participantStatus = new HashMap<>();
        List<Submission> assessmentSubmissions = CollectionUtils.emptyIfNull(assessment.getSubmissions()).stream()
            .filter(submission -> !submission.getParticipant().isTestStudent())
            .toList();

        if (submissions) {
            for (Submission submission : assessmentSubmissions) {
                // We add the status. False if in progress, true if submitted.
                if (submission.getDateSubmitted() != null) {
                    participantStatus.put(submission.getParticipant(), true);
                    // Only include the submission if it was submitted
                    submissionDtoList.add(submissionService.toDto(submission, false, false));
                } else { //We considered submitted an assessment if it has been submitted at least one time by the user
                    //including if he is in the middle of taking it again.
                    if (!participantStatus.containsKey(submission.getParticipant())) {
                        participantStatus.put(submission.getParticipant(), false);
                    }
                }
            }

            submissionsCompletedCount = 0L;
            submissionsInProgressCount = 0L;

            for (Map.Entry<Participant, Boolean> status : participantStatus.entrySet()) {
                if (status.getValue()) {
                    submissionsCompletedCount = submissionsCompletedCount + 1;
                } else {
                    submissionsInProgressCount = submissionsInProgressCount + 1;
                }
            }

            assessmentDto.setSubmissionsExpected(participantRepository.countDistinctByGroup_GroupId(groupId));
            assessmentDto.setSubmissionsCompletedCount(submissionsCompletedCount);
            assessmentDto.setSubmissionsInProgressCount(submissionsInProgressCount);
        }

        assessmentDto.setStarted(CollectionUtils.isNotEmpty(assessmentSubmissions));
        assessmentDto.setSubmissions(submissionDtoList);
        assessmentDto.setTreatmentId(assessment.getTreatment().getTreatmentId());
        assessmentDto.setMaxPoints(assessmentSubmissionService.calculateMaxScore(assessment));

        return assessmentDto;
    }

    private List<QuestionDto> handleQuestionDtos(Assessment assessment, Long submissionId, boolean showQuestions, boolean showAnswers, boolean isStudent) {
        if (!showQuestions) {
            return Collections.emptyList();
        }

        return CollectionUtils.emptyIfNull(questionRepository.findByAssessment_AssessmentIdOrderByQuestionOrder(assessment.getAssessmentId())).stream()
            .map(question -> questionService.toDto(question, submissionId, showAnswers, !isStudent))
            .toList();
    }

    @Override
    public Assessment fromDto(AssessmentDto assessmentDto) throws DataServiceException {
        Assessment assessment = new Assessment();
        assessment.setAssessmentId(assessmentDto.getAssessmentId());
        assessment.setHtml(assessmentDto.getHtml());
        assessment.setAutoSubmit(assessmentDto.isAutoSubmit());
        assessment.setNumOfSubmissions(assessmentDto.getNumOfSubmissions());
        assessment.setHoursBetweenSubmissions(assessmentDto.getHoursBetweenSubmissions());
        assessment.setMultipleSubmissionScoringScheme(MultipleSubmissionScoringScheme.valueOf(assessmentDto.getMultipleSubmissionScoringScheme()));
        assessment.setCumulativeScoringInitialPercentage(assessmentDto.getCumulativeScoringInitialPercentage());
        assessment.setAllowStudentViewResponses(assessmentDto.isAllowStudentViewResponses());
        assessment.setStudentViewResponsesAfter(assessmentDto.getStudentViewResponsesAfter());
        assessment.setStudentViewResponsesBefore(assessmentDto.getStudentViewResponsesBefore());
        assessment.setAllowStudentViewCorrectAnswers(assessmentDto.isAllowStudentViewCorrectAnswers());
        assessment.setStudentViewCorrectAnswersAfter(assessmentDto.getStudentViewCorrectAnswersAfter());
        assessment.setStudentViewCorrectAnswersBefore(assessmentDto.getStudentViewCorrectAnswersBefore());

        Optional<Treatment> treatment = treatmentRepository.findById(assessmentDto.getTreatmentId());

        if (treatment.isEmpty()) {
            throw new DataServiceException("The treatment for the assessment does not exist");
        }

        assessment.setTreatment(treatment.get());

        return assessment;
    }

    private Assessment save(Assessment assessment) {
        return assessmentRepository.save(assessment);
    }

    @Override
    public Assessment getAssessment(Long id) {
        return assessmentRepository.findByAssessmentId(id);
    }

    @Override
    public AssessmentDto putAssessment(Long id, AssessmentDto assessmentDto, boolean processQuestions)
            throws TitleValidationException, RevealResponsesSettingValidationException,
                MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, IdInPostException, DataServiceException,
                NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException, IntegrationClientNotFoundException, IntegrationNotFoundException {
        return toDto(updateAssessment(id, assessmentDto, processQuestions), true, true, false, false);
    }

    @Override
    public Assessment updateAssessment(Long id, AssessmentDto assessmentDto, boolean processQuestions)
            throws TitleValidationException, RevealResponsesSettingValidationException,
                MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, IdInPostException, DataServiceException,
                NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException, IntegrationClientNotFoundException, IntegrationNotFoundException {
        Assessment assessment = assessmentRepository.findByAssessmentId(id);

        if (assessment == null) {
            throw new AssessmentNotMatchingException(TextConstants.ASSESSMENT_NOT_MATCHING);
        }

        // handle special cases for integration types
        assessmentDto.setAllowStudentViewCorrectAnswers(!assessment.isIntegration() && assessmentDto.isAllowStudentViewCorrectAnswers());
        assessmentDto.setStudentViewCorrectAnswersAfter(assessment.isIntegration() ? null : assessmentDto.getStudentViewCorrectAnswersAfter());
        assessmentDto.setStudentViewCorrectAnswersBefore(assessment.isIntegration() ? null : assessmentDto.getStudentViewCorrectAnswersBefore());

        validateMultipleAttemptsSettings(assessmentDto);
        validateRevealAssignmentResponsesSettings(assessmentDto);
        assessment.setAllowStudentViewResponses(assessmentDto.isAllowStudentViewResponses());
        assessment.setStudentViewResponsesAfter(assessmentDto.getStudentViewResponsesAfter());
        assessment.setStudentViewResponsesBefore(assessmentDto.getStudentViewResponsesBefore());
        assessment.setAllowStudentViewCorrectAnswers(assessmentDto.isAllowStudentViewCorrectAnswers());
        assessment.setStudentViewCorrectAnswersAfter(assessmentDto.getStudentViewCorrectAnswersAfter());
        assessment.setStudentViewCorrectAnswersBefore(assessment.isIntegration() ? null : assessmentDto.getStudentViewCorrectAnswersBefore());
        assessment.setHtml(assessmentDto.getHtml());
        assessment.setAutoSubmit(assessmentDto.isAutoSubmit());
        assessment.setNumOfSubmissions(assessmentDto.getNumOfSubmissions());
        assessment.setHoursBetweenSubmissions(assessmentDto.getHoursBetweenSubmissions());
        assessment.setMultipleSubmissionScoringScheme(MultipleSubmissionScoringScheme.valueOf(assessmentDto.getMultipleSubmissionScoringScheme()));
        assessment.setCumulativeScoringInitialPercentage(assessmentDto.getCumulativeScoringInitialPercentage());

        if (processQuestions) {
            processAssessmentQuestions(assessmentDto);
        }

        return save(assessment);
    }

    private void processAssessmentQuestions(AssessmentDto assessmentDto) throws IdInPostException, DataServiceException, QuestionNotMatchingException, NegativePointsException, MultipleChoiceLimitReachedException, IntegrationClientNotFoundException, IntegrationNotFoundException {
        if (CollectionUtils.isNotEmpty(assessmentDto.getQuestions())) {
            List<Long> existingQuestionIds = CollectionUtils.emptyIfNull(questionRepository.findByAssessment_AssessmentIdOrderByQuestionOrder(assessmentDto.getAssessmentId())).stream()
                .map(Question::getQuestionId)
                .collect(Collectors.toList()); // needs to be a modifiable list

            Map<Question, QuestionDto> questionMap = new HashMap<>();

            for (QuestionDto questionDto : assessmentDto.getQuestions()) {
                if (questionDto.getQuestionId() == null) {
                    // create new question
                    questionService.postQuestion(questionDto, assessmentDto.getAssessmentId(), false, false);
                    continue;
                }

                // update question
                Question question = questionRepository.findByQuestionId(questionDto.getQuestionId());

                if (question == null) {
                    throw new QuestionNotMatchingException(TextConstants.QUESTION_NOT_MATCHING);
                }

                // remove question ID from list, as it exists in the question DTO list
                existingQuestionIds.remove(question.getQuestionId());
                questionMap.put(question, questionDto);
            }

            if (MapUtils.isNotEmpty(questionMap)) {
                try {
                    questionService.updateQuestion(questionMap);
                } catch (NegativePointsException | IntegrationNotFoundException | IntegrationNotMatchingException | IntegrationConfigurationNotFoundException | IntegrationConfigurationNotMatchingException e) {
                    throw new DataServiceException(e.getMessage(), e);
                }
            }

            // remove questions not passed in
            CollectionUtils.emptyIfNull(existingQuestionIds).stream()
                .forEach(existingQuestionId -> questionRepository.deleteByQuestionId(existingQuestionId));
        } else {
            // delete all questions from the assessment; none were passed in
            List<Question> questions = questionRepository.findByAssessment_AssessmentIdOrderByQuestionOrder(assessmentDto.getAssessmentId());

            CollectionUtils.emptyIfNull(questions).stream()
                .forEach(question -> questionRepository.deleteByQuestionId(question.getQuestionId()));
        }
    }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException {
        assessmentRepository.deleteByAssessmentId(id);
    }

    private void validateMultipleAttemptsSettings(AssessmentDto assessmentDto) throws MultipleAttemptsSettingsValidationException {
        MultipleSubmissionScoringScheme multipleSubmissionScoringScheme = MultipleSubmissionScoringScheme.valueOf(assessmentDto.getMultipleSubmissionScoringScheme());

        // validate that if multipleSubmissionScoringScheme is CUMULATIVE, then cumulativeScoringInitialPercentage is not null
        if (multipleSubmissionScoringScheme == MultipleSubmissionScoringScheme.CUMULATIVE && assessmentDto.getCumulativeScoringInitialPercentage() == null) {
            throw new MultipleAttemptsSettingsValidationException("Error 156: Must set cumulative scoring initial percentage when scoring scheme is CUMULATIVE");
        }

        // validate that if multipleSubmissionScoringScheme is CUMULATIVE, then
        // numOfSubmissions is not null and greater than 1
        if (multipleSubmissionScoringScheme == MultipleSubmissionScoringScheme.CUMULATIVE && (assessmentDto.getNumOfSubmissions() == null || assessmentDto.getNumOfSubmissions() <= 1)) {
            throw new MultipleAttemptsSettingsValidationException("Error 157: Number of submissions must be greater than 1, but not infinite, when scoring scheme is CUMULATIVE");
        }
    }

    private void validateRevealAssignmentResponsesSettings(AssessmentDto assessmentDto) throws RevealResponsesSettingValidationException {
        // validate that if allowStudentViewCorrectAnswers then also allowStudentViewResponses must be true
        if (assessmentDto.isAllowStudentViewCorrectAnswers() && !assessmentDto.isAllowStudentViewResponses()) {
            throw new RevealResponsesSettingValidationException("Error 151: Cannot allow students to view correct answers if they are not allowed to view responses.");
        }

        // Validate that view responses 'after' date comes before the 'before' date
        if (assessmentDto.getStudentViewResponsesAfter() != null
                && assessmentDto.getStudentViewResponsesBefore() != null
                && !assessmentDto.getStudentViewResponsesAfter().before(assessmentDto.getStudentViewResponsesBefore())) {
            throw new RevealResponsesSettingValidationException("Error 152: Start date of revealing student responses must come before end date.");
        }

        // Validate that view correct answers 'after' date comes before the 'before' date
        if (assessmentDto.getStudentViewCorrectAnswersAfter() != null
                && assessmentDto.getStudentViewCorrectAnswersBefore() != null
                && !assessmentDto.getStudentViewCorrectAnswersAfter().before(assessmentDto.getStudentViewCorrectAnswersBefore())) {
            throw new RevealResponsesSettingValidationException("Error 153: Start date of revealing correct answers must come before end date.");
        }

        // Validate studentViewCorrectAnswersAfter is greater than or equal to studentViewResponsesAfter
        if (assessmentDto.getStudentViewCorrectAnswersAfter() != null
                && assessmentDto.getStudentViewResponsesAfter() != null
                && !(assessmentDto.getStudentViewCorrectAnswersAfter().equals(assessmentDto.getStudentViewResponsesAfter())
                        || assessmentDto.getStudentViewCorrectAnswersAfter().after(assessmentDto.getStudentViewResponsesAfter()))) {

            throw new RevealResponsesSettingValidationException("Error 154: Start date of revealing correct answers must equal or come after start date of revealing student responses.");
        }

        // Validate studentViewCorrectAnswersBefore is less than or equal to studentViewResponsesBefore
        if (assessmentDto.getStudentViewCorrectAnswersBefore() != null
                && assessmentDto.getStudentViewResponsesBefore() != null
                && !(assessmentDto.getStudentViewCorrectAnswersBefore().equals(assessmentDto.getStudentViewResponsesBefore())
                        || assessmentDto.getStudentViewCorrectAnswersBefore().before(assessmentDto.getStudentViewResponsesBefore()))) {

            throw new RevealResponsesSettingValidationException("Error 155: End date of revealing correct answers must equal or come before end date of revealing student responses.");
        }
    }

    @Override
    public AssessmentDto defaultAssessment(AssessmentDto assessmentDto, Long treatmentId) {
        assessmentDto.setTreatmentId(treatmentId);

        Treatment treatment = treatmentRepository.findByTreatmentId(treatmentId);
        Assignment assignment = treatment.getAssignment();

        // Default multiple attempts settings to assignment level settings
        assessmentDto.setNumOfSubmissions(assignment.getNumOfSubmissions());
        assessmentDto.setHoursBetweenSubmissions(assignment.getHoursBetweenSubmissions());
        assessmentDto.setMultipleSubmissionScoringScheme(assignment.getMultipleSubmissionScoringScheme().name());
        assessmentDto.setCumulativeScoringInitialPercentage(assignment.getCumulativeScoringInitialPercentage());
        assessmentDto.setAutoSubmit(true);

        // Default reveal treatment responses settings to assignment level settings.
        assessmentDto.setAllowStudentViewResponses(assignment.isAllowStudentViewResponses());
        assessmentDto.setStudentViewResponsesAfter(assignment.getStudentViewResponsesAfter());
        assessmentDto.setStudentViewResponsesBefore(assignment.getStudentViewResponsesBefore());
        assessmentDto.setAllowStudentViewCorrectAnswers(assignment.isAllowStudentViewCorrectAnswers());
        assessmentDto.setStudentViewCorrectAnswersAfter(assignment.getStudentViewCorrectAnswersAfter());
        assessmentDto.setStudentViewCorrectAnswersBefore(assignment.getStudentViewCorrectAnswersBefore());

        return assessmentDto;
    }

    @Override
    public void updateTreatment(Long treatmentId, Assessment assessment) {
        Treatment treatment = treatmentRepository.findByTreatmentId(treatmentId);
        treatment.setAssessment(assessment);
        treatmentRepository.saveAndFlush(treatment);
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}")
                .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId).toUri());

        return headers;
    }

    @Override
    public Assessment duplicateAssessment(long assessmentId, long treatmentId)
            throws DataServiceException, AssessmentNotMatchingException, TreatmentNotMatchingException, QuestionNotMatchingException {
        Treatment treatment = treatmentRepository.findByTreatmentId(treatmentId);

        if (treatment == null) {
            throw new TreatmentNotMatchingException(TextConstants.TREATMENT_NOT_MATCHING);
        }

        return duplicateAssessment(assessmentId, treatment, null);
    }

    @Override
    public Assessment duplicateAssessment(long assessmentId, Treatment treatment, Assignment assignment)
            throws DataServiceException, AssessmentNotMatchingException, QuestionNotMatchingException {
        Assessment from = getAssessment(assessmentId);

        if (from == null) {
            throw new DataServiceException("The assessment with the given ID does not exist");
        }

        entityManager.detach(from);

        from.setQuestions(Collections.emptyList());
        from.setSubmissions(Collections.emptyList());

        // reset ID
        Long oldAssessmentId = from.getAssessmentId();
        from.setAssessmentId(null);

        from.setTreatment(treatment);

        Assessment newAssessment = save(from);

        // duplicate questions
        questionService.duplicateQuestionsForAssessment(oldAssessmentId, newAssessment);

        return newAssessment;
    }

    @Override
    public Assessment getAssessmentForParticipant(Participant participant, SecuredInfo securedInfo) throws AssessmentNotMatchingException {
        Assessment assessment = null;

        if (!participant.getConsent()) {
            //We need the default condition assessment
            for (Condition condition : participant.getExperiment().getConditions()) {
                if (condition.getDefaultCondition()) {
                    assessment = getAssessmentByConditionId(participant.getExperiment().getExperimentId(), securedInfo.getLmsAssignmentId(), condition.getConditionId());
                    break;
                }
            }
        } else {
            if (participant.getGroup() != null) {
                assessment = getAssessmentByGroupId(participant.getExperiment().getExperimentId(), securedInfo.getLmsAssignmentId(), participant.getGroup().getGroupId());
            }
        }

        if (assessment == null) {
            throw new AssessmentNotMatchingException("There is no assessment available for this user");
        }

        return assessment;
    }

    @Override
    public Assessment getAssessmentByGroupId(Long experimentId, String lmsAssignmentId, Long groupId) throws AssessmentNotMatchingException {
        Assignment assignment = assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(experimentId, lmsAssignmentId);

        if (assignment == null) {
            throw new AssessmentNotMatchingException("Error 127: This assignment does not exist in Terracotta for this experiment");
        }

        Optional<ExposureGroupCondition> exposureGroupCondition = exposureGroupConditionRepository.getByGroup_GroupIdAndExposure_ExposureId(groupId, assignment.getExposure().getExposureId());

        if (exposureGroupCondition.isEmpty()) {
            throw new AssessmentNotMatchingException("Error 130: This assignment does not have a condition assigned for the participant group.");
        }

        return retrieveTreatmentAssessment(exposureGroupCondition.get().getCondition().getConditionId(), assignment.getAssignmentId(), experimentId);
    }

    @Override
    public Assessment getAssessmentByConditionId(Long experimentId, String lmsAssignmentId, Long conditionId) throws AssessmentNotMatchingException {
        Assignment assignment = assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(experimentId, lmsAssignmentId);

        if (assignment == null) {
            throw new AssessmentNotMatchingException("Error 127: This assignment does not exist in Terracotta for this experiment");
        }


        return retrieveTreatmentAssessment(conditionId, assignment.getAssignmentId(), experimentId);
    }

    private Assessment retrieveTreatmentAssessment(long conditionId, long assignmentId, long experimentId) throws AssessmentNotMatchingException {
        List<Treatment> treatments = treatmentRepository.findByCondition_ConditionIdAndAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(conditionId, assignmentId);

        if (treatments.isEmpty()) {
            // no treatment, check default condition for treatment as this may be a single treatment assignment
            List<Condition> conditions = conditionRepository.findByExperiment_ExperimentIdOrderByConditionIdAsc(experimentId);

            Optional<Condition> defaultCondition = conditions.stream()
                .filter(c -> BooleanUtils.isTrue(c.getDefaultCondition()))
                .findFirst();

            if (defaultCondition.isEmpty()) {
                throw new AssessmentNotMatchingException("Error 131: This assignment does not have a treatment assigned.");
            }

            treatments = treatmentRepository.findByCondition_ConditionIdAndAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(defaultCondition.get().getConditionId(), assignmentId);

            if (treatments.isEmpty()) {
                throw new AssessmentNotMatchingException("Error 131: This assignment does not have a treatment assigned.");
            }
        }

        if (treatments.size() > 1) {
            throw new AssessmentNotMatchingException("Error 132: This assignment has ambiguous treatments. Please contact a Terracotta administrator");
        }

        if (treatments.get(0).getAssessment() == null) {
            throw new AssessmentNotMatchingException("Error 133: The treatment for this assignment does not have an assessment created");
        }

        return treatments.get(0).getAssessment();
    }

    @Override
    public AssessmentDto viewAssessment(long experimentId, SecuredInfo securedInfo)
            throws ExperimentNotMatchingException, ParticipantNotMatchingException, AssessmentNotMatchingException,
                GroupNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, DataServiceException, IOException, AssignmentDatesException, ConnectionException, ApiException, TerracottaConnectorException {
        Optional<Experiment> experiment = experimentRepository.findById(experimentId);

        if (experiment.isEmpty()) {
            throw new ExperimentNotMatchingException(TextConstants.EXPERIMENT_NOT_MATCHING);
        }

        Participant participant = participantService.handleExperimentParticipant(experiment.get(), securedInfo);
        Assessment assessment = null;

        if (BooleanUtils.isNotTrue(participant.getConsent())) {
            // need the default condition assessment
            Optional<Long> conditionId = experiment.get().getConditions().stream()
                .filter(Condition::getDefaultCondition)
                .map(Condition::getConditionId)
                .findFirst();

            if (conditionId.isPresent()) {
                assessment = getAssessmentByConditionId(experimentId, securedInfo.getLmsAssignmentId(), conditionId.get());
            }
        } else {
            if (participant.getGroup() != null) {
                assessment = getAssessmentByGroupId(experimentId, securedInfo.getLmsAssignmentId(), participant.getGroup().getGroupId());
            }
        }

        if (assessment == null) {
            throw new AssessmentNotMatchingException("There is no assessment available for this user");
        }

        List<Submission> submissionList = submissionRepository.findByParticipant_IdAndAssessment_AssessmentId(participant.getParticipantId(), assessment.getAssessmentId());

        if (CollectionUtils.isNotEmpty(submissionList)) {
            for (Submission submission : submissionList) {
                //   - if one of them is not submitted, (and we can use it, we need to return that one),
                if (submission.getDateSubmitted() == null) {
                    AtomicInteger answerSubmissionCount = new AtomicInteger(0);
                    submission.getQuestionSubmissions()
                        .forEach(
                            questionSubmission -> {
                                answerSubmissionCount.addAndGet(answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId()).size());
                                answerSubmissionCount.addAndGet(answerFileSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId()).size());
                                answerSubmissionCount.addAndGet(answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(questionSubmission.getQuestionSubmissionId()).size());
                            }
                        );

                    if (answerSubmissionCount.get() == assessment.getQuestions().size()) {
                        // all questions have an answer; finalize and grade
                        submissionService.finalizeAndGrade(
                            submission.getSubmissionId(),
                            securedInfo,
                            apiJwtService.isLearner(securedInfo) && !apiJwtService.isInstructorOrHigher(securedInfo)
                        );
                        log.info("Previous assessment ID: [{}] has an incomplete submission ID: [{}]. Regrading and finalizing.", assessment.getAssessmentId(), submission.getSubmissionId());
                    }
                }
            }
        }

        return toDto(assessment, participant, assessment.isAllowStudentViewResponses());
    }

    @Override
    public void verifySubmissionLimit(Integer limit, int existingSubmissionsCount) throws AssignmentAttemptException {
        if (limit == null && existingSubmissionsCount == 0) {
            // limit == null: multiple attempts not allowed; existing submission attempts must be 0
            return;
        }

        if (limit != null && limit.equals(0)) {
            // limit == 0: unlimited attempts
            return;
        }

        if (limit != null && existingSubmissionsCount < limit) {
            // limit not null: existing submission attempts must be less than the limit allowed
            return;
        }

        throw new AssignmentAttemptException(TextConstants.LIMIT_OF_SUBMISSIONS_REACHED);
    }

    @Override
    public void verifySubmissionWaitTime(Float waitTime, List<Submission> submissionList) throws AssignmentAttemptException {
        if (waitTime == null || waitTime.equals(0F)) {
            // waitTime is null or 0: no wait time limit
            return;
        }

        if (CollectionUtils.isEmpty(submissionList)) {
            // no submissions exist: no wait time limit for first submission
            return;
        }

        // calculate the allowable submission time limit
        Timestamp limit = Timestamp.from(Instant.now().minus(Math.round(waitTime * 60 * 60), ChronoUnit.SECONDS));

        // check for any submissions after the allowable time
        Optional<Submission> invalidSubmission = submissionList.stream()
            .filter(submission -> submission.getDateSubmitted().after(limit))
            .findAny();

        if (invalidSubmission.isEmpty()) {
            return;
        }

        // there are existing submissions that are not passed the time limit
        throw new AssignmentAttemptException(TextConstants.ASSIGNMENT_SUBMISSION_WAIT_TIME_NOT_REACHED);
    }

    @Override
    public void regradeQuestions(RegradeDetails regradeDetails, long assessmentId) throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException {
        if (regradeDetails == null) {
            return;
        }

        if (CollectionUtils.isEmpty(regradeDetails.getEditedMCQuestionIds())) {
            return;
        }

        if (RegradeOption.NA == regradeDetails.getRegradeOption()) {
            return;
        }

        log.info("Processing regrade option: [{}] with edited MC question IDs: [{}] for assessment ID: [{}]",
            regradeDetails.getRegradeOption(),
            StringUtils.join(regradeDetails.getEditedMCQuestionIds(), ","),
            assessmentId
        );

        if (RegradeOption.NONE == regradeDetails.getRegradeOption()) {
            return;
        }

        List<Submission> submissions = submissionRepository.findByAssessment_AssessmentId(assessmentId);

        // regrade option selected; perform regrade
        for (Submission submission : submissions) {
            Submission gradedSubmission = assessmentSubmissionService.gradeSubmission(submission, regradeDetails);
            submissionService.sendSubmissionGradeToLmsWithLti(gradedSubmission, false);
        }

        updateRegradedQuestionStatus(regradeDetails);
        log.info("Regrading complete for assessment ID: [{}]", assessmentId);
    }

    /**
     * Updates the edited {@link QuestionMc}s with the given {@link RegradeOption}
     *
     * @param regradeDetails
     */
    private void updateRegradedQuestionStatus(RegradeDetails regradeDetails) {
        if (CollectionUtils.isEmpty(regradeDetails.getEditedMCQuestionIds())) {
            return;
        }

        questionMcRepository.findAllById(regradeDetails.getEditedMCQuestionIds()).forEach(
            questionMc -> {
                questionMc.setRegradeOption(regradeDetails.getRegradeOption());
                questionMcRepository.save(questionMc);
            }
        );
    }

}
