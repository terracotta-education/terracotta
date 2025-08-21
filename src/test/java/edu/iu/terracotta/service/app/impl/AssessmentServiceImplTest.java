package edu.iu.terracotta.service.app.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.RegradeDetails;
import edu.iu.terracotta.dao.entity.RetakeDetails;
import edu.iu.terracotta.dao.entity.Submission;
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
import edu.iu.terracotta.dao.model.enums.MultipleSubmissionScoringScheme;
import edu.iu.terracotta.dao.model.enums.RegradeOption;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.lang3.StringUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"PMD.AvoidAccessibilityAlteration"})
public class AssessmentServiceImplTest extends BaseTest {

    @Spy
    @InjectMocks
    private AssessmentServiceImpl assessmentService;

    @Mock private Assessment assessment1;

    private Method retrieveTreatmentAssessment;
    private Method validateRevealAssignmentResponsesSettings;
    private Method verifySubmissionLimit;
    private Method verifySubmissionWaitTime;

    @BeforeEach
    public void beforeEach() throws NoSuchMethodException, SecurityException, GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException,
            AssignmentNotMatchingException, ExperimentNotMatchingException, DataServiceException, QuestionNotMatchingException, TerracottaConnectorException {
        MockitoAnnotations.openMocks(this);

        setup();
        clearInvocations(questionService, submissionService);

        retrieveTreatmentAssessment = AssessmentServiceImpl.class.getDeclaredMethod("retrieveTreatmentAssessment", long.class, long.class, long.class);
        retrieveTreatmentAssessment.setAccessible(true);
        validateRevealAssignmentResponsesSettings = AssessmentServiceImpl.class.getDeclaredMethod("validateRevealAssignmentResponsesSettings", AssessmentDto.class);
        validateRevealAssignmentResponsesSettings.setAccessible(true);
        verifySubmissionLimit = AssessmentServiceImpl.class.getDeclaredMethod("verifySubmissionLimit", Integer.class, int.class);
        verifySubmissionLimit.setAccessible(true);
        verifySubmissionWaitTime = AssessmentServiceImpl.class.getDeclaredMethod("verifySubmissionWaitTime", Float.class, List.class);
        verifySubmissionWaitTime.setAccessible(true);

        when(assessmentRepository.existsByTreatment_Condition_Experiment_ExperimentIdAndTreatment_Condition_ConditionIdAndTreatment_TreatmentIdAndAssessmentId(anyLong(), anyLong(), anyLong(), anyLong())).thenReturn(true);
        when(assessmentRepository.findByTreatment_TreatmentId(anyLong())).thenReturn(List.of(assessment));
        when(assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(anyLong(), anyString())).thenReturn(Optional.of(assignment));
        when(conditionRepository.findByExperiment_ExperimentIdOrderByConditionIdAsc(anyLong())).thenReturn(List.of(condition));
        when(exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.of(exposureGroupCondition));
        when(exposureGroupConditionRepository.getByGroup_GroupIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.of(exposureGroupCondition));
        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(participant);
        when(questionMcRepository.findAllById(anyList())).thenReturn(List.of(questionMc));
        when(questionRepository.findByAssessment_AssessmentIdOrderByQuestionOrder(anyLong())).thenReturn(Collections.emptyList());
        when(submissionRepository.findByAssessment_AssessmentId(anyLong())).thenReturn(List.of(submission));
        when(treatmentRepository.findByCondition_ConditionIdAndAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(anyLong(), anyLong())).thenReturn(List.of(treatment));

        when(fileStorageService.parseHTMLFiles(anyString(), anyString())).thenReturn(StringUtils.EMPTY);
        when(participantService.handleExperimentParticipant(any(Experiment.class), any(SecuredInfo.class))).thenReturn(participant);
        when(questionService.duplicateQuestionsForAssessment(anyLong(), any(Assessment.class))).thenReturn(List.of(question));

        when(assessment.isAllowStudentViewResponses()).thenReturn(true);
        when(assessment.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.MOST_RECENT);
        when(assessment.getQuestions()).thenReturn(Collections.emptyList());
        when(assessmentDto.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.MOST_RECENT.toString());
        when(assessmentDto.getQuestions()).thenReturn(List.of(questionDto));
        when(assignment.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.MOST_RECENT);
        when(condition.getDefaultCondition()).thenReturn(true);
        when(questionDto.getQuestionId()).thenReturn(1L);
        when(regradeDetails.getEditedMCQuestionIds()).thenReturn(List.of(1L));
        when(regradeDetails.getRegradeOption()).thenReturn(RegradeOption.BOTH);
    }

    @Test
    public void testDuplicateAssessment() throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, TreatmentNotMatchingException, QuestionNotMatchingException {
        Assessment assessment = assessmentService.duplicateAssessment(1L, 2L);

        assertNotNull(assessment);
        assertEquals(1L, assessment.getAssessmentId());
    }

    @Test
    public void testDuplicateAssessmentNotFound() throws IdInPostException, ExceedingLimitException, AssessmentNotMatchingException {
        when(assessmentRepository.findByAssessmentId(anyLong())).thenReturn(null);

        Exception exception = assertThrows(DataServiceException.class, () -> { assessmentService.duplicateAssessment(1L, 2L); });

        assertEquals("The assessment with the given ID does not exist", exception.getMessage());
    }

    @Test
    public void testViewAssessment() throws ExperimentNotMatchingException, ParticipantNotMatchingException, AssessmentNotMatchingException, GroupNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, DataServiceException, ApiException, IOException, AssignmentDatesException, ConnectionException, TerracottaConnectorException {
        AssessmentDto assessmentDto = assessmentService.viewAssessment(1l, securedInfo);

        assertNotNull(assessmentDto);
        assertNotNull(assessmentDto.getRetakeDetails());
        assertEquals(1F, assessmentDto.getRetakeDetails().getKeptScore());
        assertEquals(1, assessmentDto.getRetakeDetails().getSubmissionAttemptsCount());
        assertTrue(assessmentDto.getRetakeDetails().isRetakeAllowed());
        assertNull(assessmentDto.getRetakeDetails().getRetakeNotAllowedReason());
        assertEquals(1F, assessmentDto.getRetakeDetails().getLastAttemptScore());
        assertEquals(1, assessmentDto.getSubmissions().size());
    }

    @Test
    public void testViewAssessmentNoSubmissions() throws ExperimentNotMatchingException, ParticipantNotMatchingException, AssessmentNotMatchingException, GroupNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, DataServiceException, ApiException, IOException, AssignmentDatesException, ConnectionException, TerracottaConnectorException {
        when(submission.getAssessment()).thenReturn(assessment1);
        when(assessment1.getAssessmentId()).thenReturn(2L);
        when(submissionRepository.findByParticipant_IdAndAssessment_AssessmentId(anyLong(), anyLong())).thenReturn(Collections.emptyList());
        AssessmentDto assessmentDto = assessmentService.viewAssessment(1l, securedInfo);

        assertNotNull(assessmentDto);
        assertNotNull(assessmentDto.getRetakeDetails());
        assertEquals(1F, assessmentDto.getRetakeDetails().getKeptScore());
        assertEquals(0, assessmentDto.getRetakeDetails().getSubmissionAttemptsCount());
        assertTrue(assessmentDto.getRetakeDetails().isRetakeAllowed());
        assertNull(assessmentDto.getRetakeDetails().getRetakeNotAllowedReason());
        assertEquals(0, assessmentDto.getSubmissions().size());
    }

    @Test
    public void testViewAssessmentOverMaxSubmissionsAttempts() throws ExperimentNotMatchingException, ParticipantNotMatchingException, AssessmentNotMatchingException, GroupNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, AssignmentAttemptException, DataServiceException, ApiException, IOException, AssignmentDatesException, ConnectionException, TerracottaConnectorException {
        doThrow(new AssignmentAttemptException(TextConstants.LIMIT_OF_SUBMISSIONS_REACHED)).when(assessmentService).verifySubmissionLimit(anyInt(), anyInt());

        AssessmentDto assessmentDto = assessmentService.viewAssessment(1l, securedInfo);

        assertNotNull(assessmentDto);
        assertNotNull(assessmentDto.getRetakeDetails());
        assertEquals(1F, assessmentDto.getRetakeDetails().getKeptScore());
        assertEquals(1, assessmentDto.getRetakeDetails().getSubmissionAttemptsCount());
        assertFalse(assessmentDto.getRetakeDetails().isRetakeAllowed());
        assertEquals(RetakeDetails.RetakeNotAllowedReason.MAX_NUMBER_ATTEMPTS_REACHED.toString(), assessmentDto.getRetakeDetails().getRetakeNotAllowedReason());
        assertEquals(1F, assessmentDto.getRetakeDetails().getLastAttemptScore());
    }

    @Test
    public void testViewAssessmentWaitTimeNoSubmissions() throws ExperimentNotMatchingException, ParticipantNotMatchingException, AssessmentNotMatchingException, GroupNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, AssignmentAttemptException, DataServiceException, ApiException, IOException, AssignmentDatesException, ConnectionException, TerracottaConnectorException {
        when(submissionRepository.findByParticipant_IdAndAssessment_AssessmentId(anyLong(), anyLong())).thenReturn(Collections.emptyList());
        AssessmentDto assessmentDto = assessmentService.viewAssessment(1l, securedInfo);

        assertNotNull(assessmentDto);
        assertNotNull(assessmentDto.getRetakeDetails());
        assertEquals(1F, assessmentDto.getRetakeDetails().getKeptScore());
        assertEquals(0, assessmentDto.getRetakeDetails().getSubmissionAttemptsCount());
        assertTrue(assessmentDto.getRetakeDetails().isRetakeAllowed());
        assertNull(assessmentDto.getRetakeDetails().getRetakeNotAllowedReason());
        assertEquals(null, assessmentDto.getRetakeDetails().getLastAttemptScore());
        assertEquals(0, assessmentDto.getSubmissions().size());
    }

    @Test
    public void testViewAssessmentWaitTimeNotReached() throws ExperimentNotMatchingException, ParticipantNotMatchingException, AssessmentNotMatchingException, GroupNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, AssignmentAttemptException, DataServiceException, ApiException, IOException, AssignmentDatesException, ConnectionException, TerracottaConnectorException {
        doThrow(new AssignmentAttemptException(TextConstants.ASSIGNMENT_SUBMISSION_WAIT_TIME_NOT_REACHED)).when(assessmentService).verifySubmissionLimit(anyInt(), anyInt());

        AssessmentDto assessmentDto = assessmentService.viewAssessment(1l, securedInfo);

        assertNotNull(assessmentDto);
        assertNotNull(assessmentDto.getRetakeDetails());
        assertEquals(1F, assessmentDto.getRetakeDetails().getKeptScore());
        assertEquals(1, assessmentDto.getRetakeDetails().getSubmissionAttemptsCount());
        assertFalse(assessmentDto.getRetakeDetails().isRetakeAllowed());
        assertEquals(RetakeDetails.RetakeNotAllowedReason.WAIT_TIME_NOT_REACHED.toString(), assessmentDto.getRetakeDetails().getRetakeNotAllowedReason());
        assertEquals(1F, assessmentDto.getRetakeDetails().getLastAttemptScore());
    }

    @Test
    public void testViewAssessmentNotAllowedOther() throws ExperimentNotMatchingException, ParticipantNotMatchingException, AssessmentNotMatchingException, GroupNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, AssignmentAttemptException, DataServiceException, ApiException, IOException, AssignmentDatesException, ConnectionException, TerracottaConnectorException {
        doThrow(new AssignmentAttemptException(TextConstants.ASSIGNMENT_LOCKED)).when(assessmentService).verifySubmissionLimit(anyInt(), anyInt());

        AssessmentDto assessmentDto = assessmentService.viewAssessment(1l, securedInfo);

        assertNotNull(assessmentDto);
        assertNotNull(assessmentDto.getRetakeDetails());
        assertEquals(1F, assessmentDto.getRetakeDetails().getKeptScore());
        assertEquals(1, assessmentDto.getRetakeDetails().getSubmissionAttemptsCount());
        assertFalse(assessmentDto.getRetakeDetails().isRetakeAllowed());
        assertEquals(RetakeDetails.RetakeNotAllowedReason.OTHER.toString(), assessmentDto.getRetakeDetails().getRetakeNotAllowedReason());
        assertEquals(1F, assessmentDto.getRetakeDetails().getLastAttemptScore());
    }

    @Test
    public void testViewAssessmentNoSubmittedScores() throws ExperimentNotMatchingException, ParticipantNotMatchingException, AssessmentNotMatchingException, GroupNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, DataServiceException, ApiException, IOException, AssignmentDatesException, ConnectionException, TerracottaConnectorException {
        when(submissionService.getScoreFromMultipleSubmissions(any(Participant.class), any(Assessment.class))).thenReturn(0F);
        when(submissionRepository.findByParticipant_IdAndAssessment_AssessmentId(anyLong(), anyLong())).thenReturn(Collections.emptyList());
        AssessmentDto assessmentDto = assessmentService.viewAssessment(1l, securedInfo);

        assertNotNull(assessmentDto);
        assertNotNull(assessmentDto.getRetakeDetails());
        assertEquals(0F, assessmentDto.getRetakeDetails().getKeptScore());
        assertEquals(0, assessmentDto.getRetakeDetails().getSubmissionAttemptsCount());
        assertTrue(assessmentDto.getRetakeDetails().isRetakeAllowed());
        assertNull(assessmentDto.getRetakeDetails().getRetakeNotAllowedReason());
        assertNull(assessmentDto.getRetakeDetails().getLastAttemptScore());
    }

    @Test
    public void testViewAssessmentNoExperiment() throws IdInPostException, ExceedingLimitException, AssessmentNotMatchingException {
        when(experimentRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(ExperimentNotMatchingException.class, () -> { assessmentService.viewAssessment(1l, securedInfo); });

        assertEquals(TextConstants.EXPERIMENT_NOT_MATCHING, exception.getMessage());
    }

    @Test
    public void testGetAssessmentByGroupId() throws AssessmentNotMatchingException {
        Assessment assessment = assessmentService.getAssessmentByGroupId(1L, "1", 1l);

        assertNotNull(assessment);
    }

    @Test
    public void testGetAssessmentByGroupIdNoAssignment() throws AssessmentNotMatchingException {
        when(assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(anyLong(), anyString())).thenReturn(Optional.empty());
        Exception exception = assertThrows(AssessmentNotMatchingException.class, () -> { assessmentService.getAssessmentByGroupId(1L, "1", 1L); });

        assertEquals("Error 127: This assignment does not exist in Terracotta for this experiment", exception.getMessage());
    }

    @Test
    public void testGetAssessmentByGroupIdNoExposureGroupCondition() throws AssessmentNotMatchingException {
        when(exposureGroupConditionRepository.getByGroup_GroupIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.empty());
        Exception exception = assertThrows(AssessmentNotMatchingException.class, () -> { assessmentService.getAssessmentByGroupId(1L, "1", 1L); });

        assertEquals("Error 130: This assignment does not have a condition assigned for the participant group.", exception.getMessage());
    }

    @Test
    public void testGetAssessmentByConditionId() throws AssessmentNotMatchingException {
        Assessment assessment = assessmentService.getAssessmentByConditionId(1L, "1", 1l);

        assertNotNull(assessment);
    }

    @Test
    public void testGetAssessmentByConditionIdNoAssignment() throws AssessmentNotMatchingException {
        when(assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(anyLong(), anyString())).thenReturn(Optional.empty());
        Exception exception = assertThrows(AssessmentNotMatchingException.class, () -> { assessmentService.getAssessmentByConditionId(1L, "1", 1l); });

        assertEquals("Error 127: This assignment does not exist in Terracotta for this experiment", exception.getMessage());
    }

    @Test
    public void testUpdateAssessmentWithNewQuestion()
        throws TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException,
        AssessmentNotMatchingException, IdInPostException, DataServiceException, NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException,
        IntegrationNotFoundException, IntegrationNotMatchingException, IntegrationConfigurationNotFoundException, IntegrationConfigurationNotMatchingException, IntegrationClientNotFoundException {
        when(questionRepository.findByAssessment_AssessmentIdOrderByQuestionOrder(anyLong())).thenReturn(Collections.emptyList());
        when(questionDto.getQuestionId()).thenReturn(null);
        assessmentService.updateAssessment(1L, assessmentDto, true);

        verify(questionService).postQuestion(any(QuestionDto.class), anyLong(), anyBoolean(), anyBoolean());
        verify(questionRepository, never()).findByQuestionId(anyLong());
        verify(questionService, never()).updateQuestion(anyMap());
        verify(questionRepository, never()).deleteById(anyLong());
    }

    @Test
    public void testUpdateAssessmentWithExistingQuestion()
        throws TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException,
        AssessmentNotMatchingException, IdInPostException, DataServiceException, NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException,
        IntegrationNotFoundException, IntegrationNotMatchingException, IntegrationConfigurationNotFoundException, IntegrationConfigurationNotMatchingException, IntegrationClientNotFoundException {
        when(questionDto.getQuestionId()).thenReturn(1L);
        assessmentService.updateAssessment(1L, assessmentDto, true);

        verify(questionService, never()).postQuestion(any(QuestionDto.class), anyLong(), anyBoolean(), anyBoolean());
        verify(questionRepository).findByQuestionId(anyLong());
        verify(questionService).updateQuestion(anyMap());
        verify(questionRepository, never()).deleteById(anyLong());
    }

    @Test
    public void testUpdateAssessmentWithQuestionNotFound() throws QuestionNotMatchingException {
        when(questionRepository.findByQuestionId(anyLong())).thenReturn(null);
        Exception exception = assertThrows(QuestionNotMatchingException.class, () -> { assessmentService.updateAssessment(1L, assessmentDto, true); });

        assertEquals(TextConstants.QUESTION_NOT_MATCHING, exception.getMessage());
    }

    @Test
    public void testUpdateAssessmentWithDeletedQuestion()
        throws TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException,
        AssessmentNotMatchingException, IdInPostException, DataServiceException, NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException,
        IntegrationNotFoundException, IntegrationNotMatchingException, IntegrationConfigurationNotFoundException, IntegrationConfigurationNotMatchingException, IntegrationClientNotFoundException {
        when(assessmentDto.getQuestions()).thenReturn(Collections.emptyList());
        when(questionRepository.findByAssessment_AssessmentIdOrderByQuestionOrder(anyLong())).thenReturn(Arrays.asList(question)); // requires modifiable list

        assessmentService.updateAssessment(1L, assessmentDto, true);

        verify(questionService, never()).postQuestion(any(QuestionDto.class), anyLong(), anyBoolean(), anyBoolean());
        verify(questionRepository, never()).findByQuestionId(anyLong());
        verify(questionService, never()).updateQuestion(anyMap());
        verify(questionRepository).deleteByQuestionId(anyLong());
    }

    @Test
    public void testUpdateAssessmentWithDeletedQuestionNoExistingFound()
        throws TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException,
        AssessmentNotMatchingException, IdInPostException, DataServiceException, NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException,
        IntegrationNotFoundException, IntegrationNotMatchingException, IntegrationConfigurationNotFoundException, IntegrationConfigurationNotMatchingException, IntegrationClientNotFoundException {
        when(assessmentDto.getQuestions()).thenReturn(Collections.emptyList());
        when(questionRepository.findByAssessment_AssessmentIdOrderByQuestionOrder(anyLong())).thenReturn(Collections.emptyList());

        assessmentService.updateAssessment(1L, assessmentDto, true);

        verify(questionService, never()).postQuestion(any(QuestionDto.class), anyLong(), anyBoolean(), anyBoolean());
        verify(questionRepository, never()).findByQuestionId(anyLong());
        verify(questionService, never()).updateQuestion(anyMap());
        verify(questionRepository, never()).deleteById(anyLong());
    }

    @Test
    public void testVerifyNumSubmissionsLimitNullExistingAttemptNotExists() {
        assertDoesNotThrow(() -> verifySubmissionLimit.invoke(assessmentService, null, 0));
    }

    @Test
    public void testVerifyNumSubmissionsLimitZeroIsUnlimited() {
        assertDoesNotThrow(() -> verifySubmissionLimit.invoke(assessmentService, 0, 1));
    }

    @Test
    public void testVerifyNumSubmissionsLessThanLimit() {
        assertDoesNotThrow(() -> verifySubmissionLimit.invoke(assessmentService, 2, 1));
    }

    @Test
    public void testVerifyNumSubmissionsLimitNullExistingAttemptExists() {
        InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> verifySubmissionLimit.invoke(assessmentService, null, 2));
        assertTrue(e.getCause() instanceof AssignmentAttemptException);
        assertEquals(TextConstants.LIMIT_OF_SUBMISSIONS_REACHED, e.getCause().getMessage());
    }

    @Test
    public void testVerifyNumSubmissionsGreaterThanLimit() {
        InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> verifySubmissionLimit.invoke(assessmentService, 1, 2));
        assertTrue(e.getCause() instanceof AssignmentAttemptException);
        assertEquals(TextConstants.LIMIT_OF_SUBMISSIONS_REACHED, e.getCause().getMessage());
    }

    @Test
    public void testVerifySubmissionWaitTimeNull() {
        assertDoesNotThrow(() -> verifySubmissionWaitTime.invoke(assessmentService, null, Collections.emptyList()));
    }

    @Test
    public void testVerifySubmissionWaitTimeZero() {
        assertDoesNotThrow(() -> verifySubmissionWaitTime.invoke(assessmentService, 0F, Collections.emptyList()));
    }

    @Test
    public void testVerifySubmissionWaitTimeAllowed() {
        Submission submission = new Submission();
        submission.setDateSubmitted(Timestamp.from(Instant.now().minus(30, ChronoUnit.MINUTES)));

        assertDoesNotThrow(() -> verifySubmissionWaitTime.invoke(assessmentService, .1F, List.of(submission)));
    }

    @Test
    public void testVerifySubmissionWaitTimeNotAllowed() {
        Submission submission = new Submission();
        submission.setDateSubmitted(Timestamp.from(Instant.now().minus(30, ChronoUnit.MINUTES)));

        InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> verifySubmissionWaitTime.invoke(assessmentService, 1F, List.of(submission)));
        assertTrue(e.getCause() instanceof AssignmentAttemptException);
        assertEquals(TextConstants.ASSIGNMENT_SUBMISSION_WAIT_TIME_NOT_REACHED, e.getCause().getMessage());
    }

    @Test
    public void testRegradeQuestions() throws DataServiceException, ConnectionException, ApiException, IOException, TerracottaConnectorException {
        assessmentService.regradeQuestions(regradeDetails, 1L);

        verify(assessmentSubmissionService).gradeSubmission(any(Submission.class), any(RegradeDetails.class));
        verify(submissionService).sendSubmissionGradeToLmsWithLti(any(Submission.class), anyBoolean());
    }

    @Test
    public void testRegradeQuestionsNoSumbissions() throws DataServiceException, ConnectionException, ApiException, IOException, TerracottaConnectorException {
        when(submissionRepository.findByAssessment_AssessmentId(anyLong())).thenReturn(Collections.emptyList());

        assessmentService.regradeQuestions(regradeDetails, 1L);

        verify(assessmentSubmissionService, never()).gradeSubmission(any(Submission.class), any(RegradeDetails.class));
    }

    @Test
    public void testRegradeQuestionsRegradeOptionNA() throws DataServiceException, ConnectionException, ApiException, IOException, TerracottaConnectorException {
        when(regradeDetails.getRegradeOption()).thenReturn(RegradeOption.NA);

        assessmentService.regradeQuestions(regradeDetails, 1L);

        verify(assessmentSubmissionService, never()).gradeSubmission(any(Submission.class), any(RegradeDetails.class));
    }

    @Test
    public void testRegradeQuestionsNoRegradeDetails() throws DataServiceException, ConnectionException, ApiException, IOException, TerracottaConnectorException {
        assessmentService.regradeQuestions(null, 1L);

        verify(assessmentSubmissionService, never()).gradeSubmission(any(Submission.class), any(RegradeDetails.class));
    }

    @Test
    public void testRegradeQuestionsNoEditedMCQuestionIds() throws DataServiceException, ConnectionException, ApiException, IOException, TerracottaConnectorException {
        when(regradeDetails.getEditedMCQuestionIds()).thenReturn(Collections.emptyList());

        assessmentService.regradeQuestions(regradeDetails, 1L);

        verify(assessmentSubmissionService, never()).gradeSubmission(any(Submission.class), any(RegradeDetails.class));
    }

    @Test
    public void testGetAllAssessmentsByTreatment() throws IdInPostException, DataServiceException, TitleValidationException, AssessmentNotMatchingException {
        List<AssessmentDto> retVal = assessmentService.getAllAssessmentsByTreatment(1L, false, securedInfo);

        assertNotNull(retVal);
        assertEquals(1, retVal.size());
    }

    @Test
    public void testGetAllAssessmentsByTreatmentWithSubmissions() throws IdInPostException, DataServiceException, TitleValidationException, AssessmentNotMatchingException {
        List<AssessmentDto> retVal = assessmentService.getAllAssessmentsByTreatment(1L, true, securedInfo);

        assertNotNull(retVal);
        assertEquals(1, retVal.size());
    }

    @Test
    public void testPostAssessment() throws IdInPostException, DataServiceException, TitleValidationException, AssessmentNotMatchingException {
        when(assessmentDto.getAssessmentId()).thenReturn(null);
        AssessmentDto retVal = assessmentService.postAssessment(assessmentDto, 1L, securedInfo);

        assertNotNull(retVal);
    }

    @Test
    public void testPostAssessmentIdInPostExceptionThrown() {
        IdInPostException e = assertThrows(IdInPostException.class, () -> assessmentService.postAssessment(assessmentDto, 1L, securedInfo));
        assertEquals(TextConstants.ID_IN_POST_ERROR, e.getMessage());
    }

    @Test
    public void testAssessmentBelongsToExperimentAndConditionAndTreatmentCorrectTrueResponseFalse() {
        when(assessmentDto.isAllowStudentViewCorrectAnswers()).thenReturn(true);
        when(assessmentDto.isAllowStudentViewResponses()).thenReturn(false);

        InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> validateRevealAssignmentResponsesSettings.invoke(assessmentService, assessmentDto));
        assertTrue(e.getCause() instanceof RevealResponsesSettingValidationException);
        assertEquals("Error 151: Cannot allow students to view correct answers if they are not allowed to view responses.", e.getCause().getMessage());
    }

    @Test
    public void testAssessmentBelongsToExperimentAndConditionAndTreatmentViewResponsesBeforeIsAfterAfter() {
        when(assessmentDto.getStudentViewResponsesAfter()).thenReturn(Timestamp.from(Instant.now()));
        when(assessmentDto.getStudentViewResponsesBefore()).thenReturn(Timestamp.from(Instant.now().minusSeconds(1)));

        InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> validateRevealAssignmentResponsesSettings.invoke(assessmentService, assessmentDto));
        assertTrue(e.getCause() instanceof RevealResponsesSettingValidationException);
        assertEquals("Error 152: Start date of revealing student responses must come before end date.", e.getCause().getMessage());
    }

    @Test
    public void testAssessmentBelongsToExperimentAndConditionAndTreatmentViewCorrectBeforeIsAfterAfter() {
        when(assessmentDto.getStudentViewCorrectAnswersAfter()).thenReturn(Timestamp.from(Instant.now()));
        when(assessmentDto.getStudentViewCorrectAnswersBefore()).thenReturn(Timestamp.from(Instant.now().minusSeconds(1)));

        InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> validateRevealAssignmentResponsesSettings.invoke(assessmentService, assessmentDto));
        assertTrue(e.getCause() instanceof RevealResponsesSettingValidationException);
        assertEquals("Error 153: Start date of revealing correct answers must come before end date.", e.getCause().getMessage());
    }

    @Test
    public void testAssessmentBelongsToExperimentAndConditionAndTreatmentViewCorrectIsEqualOrAfterResponses() {
        when(assessmentDto.getStudentViewResponsesAfter()).thenReturn(Timestamp.from(Instant.now()));
        when(assessmentDto.getStudentViewCorrectAnswersAfter()).thenReturn(Timestamp.from(Instant.now().minusSeconds(1)));

        InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> validateRevealAssignmentResponsesSettings.invoke(assessmentService, assessmentDto));
        assertTrue(e.getCause() instanceof RevealResponsesSettingValidationException);
        assertEquals("Error 154: Start date of revealing correct answers must equal or come after start date of revealing student responses.", e.getCause().getMessage());
    }

    @Test
    public void testAssessmentBelongsToExperimentAndConditionAndTreatmentViewCorrectIsEqualOrBeforeResponses() {
        when(assessmentDto.getStudentViewResponsesBefore()).thenReturn(Timestamp.from(Instant.now()));
        when(assessmentDto.getStudentViewCorrectAnswersBefore()).thenReturn(Timestamp.from(Instant.now().plusSeconds(1)));

        InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> validateRevealAssignmentResponsesSettings.invoke(assessmentService, assessmentDto));
        assertTrue(e.getCause() instanceof RevealResponsesSettingValidationException);
        assertEquals("Error 155: End date of revealing correct answers must equal or come before end date of revealing student responses.", e.getCause().getMessage());
    }

    @Test
    public void testBuildHeaders() {
        HttpHeaders header = assessmentService.buildHeaders(UriComponentsBuilder.newInstance(), 1L, 1L, 1L, 1L);

        assertNotNull(header);
    }

    @Test
    public void testGetAssessmentForParticipant() throws AssessmentNotMatchingException {
        Assessment retVal = assessmentService.getAssessmentForParticipant(participant, securedInfo);

        assertNotNull(retVal);
    }

    @Test
    public void testGetAssessmentForParticipantNoConsent() throws AssessmentNotMatchingException {
        when(participant.getConsent()).thenReturn(false);

        Assessment retVal = assessmentService.getAssessmentForParticipant(participant, securedInfo);

        assertNotNull(retVal);
    }

    @Test
    public void testRetrieveTreatmentAssessmentNoTreatment() {
        when(treatmentRepository.findByCondition_ConditionIdAndAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(anyLong(), anyLong())).thenReturn(Collections.emptyList());
        when(treatmentRepository.findByCondition_ConditionIdAndAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(anyLong(), anyLong())).thenReturn(Collections.emptyList());
        when(condition.getDefaultCondition()).thenReturn(true);

        InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> retrieveTreatmentAssessment.invoke(assessmentService, 1l, 1l, 1l));
        assertTrue(e.getCause() instanceof AssessmentNotMatchingException);
        assertEquals("Error 131: This assignment does not have a treatment assigned.", e.getCause().getMessage());
    }
}
