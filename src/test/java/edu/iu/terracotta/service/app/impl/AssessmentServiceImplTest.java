package edu.iu.terracotta.service.app.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import edu.iu.terracotta.BaseTest;
import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.RegradeDetails;
import edu.iu.terracotta.model.app.RetakeDetails;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import edu.iu.terracotta.model.app.dto.QuestionDto;
import edu.iu.terracotta.model.app.enumerator.MultipleSubmissionScoringScheme;
import edu.iu.terracotta.model.app.enumerator.RegradeOption;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
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

    private Method verifySubmissionLimit;
    private Method verifySubmissionWaitTime;

    @BeforeEach
    public void beforeEach() throws DataServiceException, AssessmentNotMatchingException, GroupNotMatchingException, ParticipantNotMatchingException,
            ParticipantNotUpdatedException, AssignmentNotMatchingException, IdInPostException, NoSuchMethodException, SecurityException, QuestionNotMatchingException,
            MultipleChoiceLimitReachedException, ExperimentNotMatchingException {
        MockitoAnnotations.openMocks(this);

        setup();
        clearInvocations(questionService, submissionService);

        verifySubmissionLimit = AssessmentServiceImpl.class.getDeclaredMethod("verifySubmissionLimit", Integer.class, int.class);
        verifySubmissionLimit.setAccessible(true);
        verifySubmissionWaitTime = AssessmentServiceImpl.class.getDeclaredMethod("verifySubmissionWaitTime", Float.class, List.class);
        verifySubmissionWaitTime.setAccessible(true);


        when(assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(anyLong(), anyString())).thenReturn(assignment);
        when(exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.of(exposureGroupCondition));
        when(exposureGroupConditionRepository.getByGroup_GroupIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.of(exposureGroupCondition));
        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(participant);
        when(questionRepository.findByAssessment_AssessmentIdOrderByQuestionOrder(anyLong())).thenReturn(Collections.emptyList());
        when(submissionRepository.findByAssessment_AssessmentId(anyLong())).thenReturn(Collections.singletonList(submission));
        when(treatmentRepository.findByCondition_ConditionIdAndAssignment_AssignmentId(anyLong(), anyLong())).thenReturn(Collections.singletonList(treatment));

        when(fileStorageService.parseHTMLFiles(anyString(), anyString())).thenReturn(StringUtils.EMPTY);
        when(participantService.handleExperimentParticipant(any(Experiment.class), any(SecuredInfo.class))).thenReturn(participant);
        when(questionService.duplicateQuestionsForAssessment(anyLong(), any(Assessment.class))).thenReturn(Collections.singletonList(question));
        when(questionService.findAllByAssessmentId(anyLong())).thenReturn(Arrays.asList(question)); // requires modifiable list

        when(assessment.isAllowStudentViewResponses()).thenReturn(true);
        when(assessment.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.MOST_RECENT);
        when(assessment.getQuestions()).thenReturn(Collections.emptyList());
        when(assessmentDto.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.MOST_RECENT.toString());
        when(assessmentDto.getQuestions()).thenReturn(Collections.singletonList(questionDto));
        when(condition.getDefaultCondition()).thenReturn(true);
        when(questionDto.getQuestionId()).thenReturn(1L);
        when(regradeDetails.getEditedMCQuestionIds()).thenReturn(Collections.singletonList(1L));
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
    public void testViewAssessment() throws ExperimentNotMatchingException, ParticipantNotMatchingException, AssessmentNotMatchingException, GroupNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException {
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
    public void testViewAssessmentNoSubmissions() throws ExperimentNotMatchingException, ParticipantNotMatchingException, AssessmentNotMatchingException, GroupNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException {
        when(submission.getAssessment()).thenReturn(assessment1);
        when(assessment1.getAssessmentId()).thenReturn(2L);
        when(submissionService.findByParticipantIdAndAssessmentId(anyLong(), anyLong())).thenReturn(Collections.emptyList());
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
    public void testViewAssessmentOverMaxSubmissionsAttempts() throws ExperimentNotMatchingException, ParticipantNotMatchingException, AssessmentNotMatchingException, GroupNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, AssignmentAttemptException {
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
    public void testViewAssessmentWaitTimeNotReached() throws ExperimentNotMatchingException, ParticipantNotMatchingException, AssessmentNotMatchingException, GroupNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, AssignmentAttemptException {
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
    public void testViewAssessmentNotAllowedOther() throws ExperimentNotMatchingException, ParticipantNotMatchingException, AssessmentNotMatchingException, GroupNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, AssignmentAttemptException {
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
    public void testViewAssessmentNoSubmittedScores() throws ExperimentNotMatchingException, ParticipantNotMatchingException, AssessmentNotMatchingException, GroupNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException {
        when(submissionService.getScoreFromMultipleSubmissions(any(Participant.class), any(Assessment.class))).thenReturn(0F);
        when(submissionService.findByParticipantIdAndAssessmentId(anyLong(), anyLong())).thenReturn(Collections.emptyList());
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
        when(assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(anyLong(), anyString())).thenReturn(null);
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
        when(assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(anyLong(), anyString())).thenReturn(null);
        Exception exception = assertThrows(AssessmentNotMatchingException.class, () -> { assessmentService.getAssessmentByConditionId(1L, "1", 1l); });

        assertEquals("Error 127: This assignment does not exist in Terracotta for this experiment", exception.getMessage());
    }

    @Test
    public void testUpdateAssessmentWithNewQuestion()
        throws TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException,
        AssessmentNotMatchingException, IdInPostException, DataServiceException, NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException {
            when(questionService.findAllByAssessmentId(anyLong())).thenReturn(Collections.emptyList());
        when(questionDto.getQuestionId()).thenReturn(null);
        assessmentService.updateAssessment(1L, assessmentDto, true);

        verify(questionService).postQuestion(any(QuestionDto.class), anyLong(), anyBoolean());
        verify(questionService, never()).getQuestion(anyLong());
        verify(questionService, never()).updateQuestion(anyMap());
        verify(questionService, never()).deleteById(anyLong());
    }

    @Test
    public void testUpdateAssessmentWithExistingQuestion()
        throws TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException,
        AssessmentNotMatchingException, IdInPostException, DataServiceException, NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException {
        when(questionDto.getQuestionId()).thenReturn(1L);
        assessmentService.updateAssessment(1L, assessmentDto, true);

        verify(questionService, never()).postQuestion(any(QuestionDto.class), anyLong(), anyBoolean());
        verify(questionService).getQuestion(anyLong());
        verify(questionService).updateQuestion(anyMap());
        verify(questionService, never()).deleteById(anyLong());
    }

    @Test
    public void testUpdateAssessmentWithQuestionNotFound() throws QuestionNotMatchingException {
        when(questionService.getQuestion(anyLong())).thenReturn(null);
        Exception exception = assertThrows(QuestionNotMatchingException.class, () -> { assessmentService.updateAssessment(1L, assessmentDto, true); });

        assertEquals(TextConstants.QUESTION_NOT_MATCHING, exception.getMessage());
    }

    @Test
    public void testUpdateAssessmentWithDeletedQuestion()
        throws TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException,
        AssessmentNotMatchingException, IdInPostException, DataServiceException, NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException {
        when(assessmentDto.getQuestions()).thenReturn(Collections.emptyList());
        assessmentService.updateAssessment(1L, assessmentDto, true);

        verify(questionService, never()).postQuestion(any(QuestionDto.class), anyLong(), anyBoolean());
        verify(questionService, never()).getQuestion(anyLong());
        verify(questionService, never()).updateQuestion(anyMap());
        verify(questionService).deleteById(anyLong());
    }

    @Test
    public void testUpdateAssessmentWithDeletedQuestionNoExistingFound()
        throws TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException,
        AssessmentNotMatchingException, IdInPostException, DataServiceException, NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException {
        when(assessmentDto.getQuestions()).thenReturn(Collections.emptyList());
        when(questionService.findAllByAssessmentId(anyLong())).thenReturn(Collections.emptyList());
        assessmentService.updateAssessment(1L, assessmentDto, true);

        verify(questionService, never()).postQuestion(any(QuestionDto.class), anyLong(), anyBoolean());
        verify(questionService, never()).getQuestion(anyLong());
        verify(questionService, never()).updateQuestion(anyMap());
        verify(questionService, never()).deleteById(anyLong());
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

        assertDoesNotThrow(() -> verifySubmissionWaitTime.invoke(assessmentService, .1F, Collections.singletonList(submission)));
    }

    @Test
    public void testVerifySubmissionWaitTimeNotAllowed() {
        Submission submission = new Submission();
        submission.setDateSubmitted(Timestamp.from(Instant.now().minus(30, ChronoUnit.MINUTES)));

        InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> verifySubmissionWaitTime.invoke(assessmentService, 1F, Collections.singletonList(submission)));
        assertTrue(e.getCause() instanceof AssignmentAttemptException);
        assertEquals(TextConstants.ASSIGNMENT_SUBMISSION_WAIT_TIME_NOT_REACHED, e.getCause().getMessage());
    }

    @Test
    public void textRegradeQuestions() throws DataServiceException {
        assessmentService.regradeQuestions(regradeDetails, 1L);

        verify(submissionService).gradeSubmission(any(Submission.class), any(RegradeDetails.class));
    }

    @Test
    public void textRegradeQuestionsNoSumbissions() throws DataServiceException {
        when(submissionRepository.findByAssessment_AssessmentId(anyLong())).thenReturn(Collections.emptyList());

        assessmentService.regradeQuestions(regradeDetails, 1L);

        verify(submissionService, never()).gradeSubmission(any(Submission.class), any(RegradeDetails.class));
    }

    @Test
    public void textRegradeQuestionsRegradeOptionNA() throws DataServiceException {
        when(regradeDetails.getRegradeOption()).thenReturn(RegradeOption.NA);

        assessmentService.regradeQuestions(regradeDetails, 1L);

        verify(submissionService, never()).gradeSubmission(any(Submission.class), any(RegradeDetails.class));
    }

    @Test
    public void textRegradeQuestionsNoRegradeDetails() throws DataServiceException {
        assessmentService.regradeQuestions(null, 1L);

        verify(submissionService, never()).gradeSubmission(any(Submission.class), any(RegradeDetails.class));
    }

    @Test
    public void textRegradeQuestionsNoEditedMCQuestionIds() throws DataServiceException {
        when(regradeDetails.getEditedMCQuestionIds()).thenReturn(Collections.emptyList());

        assessmentService.regradeQuestions(regradeDetails, 1L);

        verify(submissionService, never()).gradeSubmission(any(Submission.class), any(RegradeDetails.class));
    }
}
