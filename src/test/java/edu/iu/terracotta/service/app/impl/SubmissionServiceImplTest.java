package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.dao.model.lti.enums.LtiAgsScope;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.AnswerMcSubmission;
import edu.iu.terracotta.dao.entity.AnswerMcSubmissionOption;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.Group;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.Question;
import edu.iu.terracotta.dao.entity.QuestionSubmission;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.entity.SubmissionComment;
import edu.iu.terracotta.dao.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.dao.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.dao.model.dto.SubmissionCommentDto;
import edu.iu.terracotta.dao.model.dto.SubmissionDto;
import edu.iu.terracotta.dao.model.enums.MultipleSubmissionScoringScheme;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.dao.model.enums.RegradeOption;
import edu.iu.terracotta.dao.repository.SubmissionCommentRepository;
import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.service.app.SubmissionCommentService;

public class SubmissionServiceImplTest extends BaseTest {

    // NOTE: SubmissionServiceImpl's constructor depends directly on ApiClient and ApiJwtService.
    // BaseServiceTest also provides CanvasApiClientImpl/CanvasApiJwtServiceImpl mocks which also
    // implement those interfaces, so @InjectMocks type-based constructor matching is ambiguous and
    // can silently wire the wrong mock. The service is therefore constructed manually below instead
    // of relying on @InjectMocks. Additionally, SubmissionCommentRepository/SubmissionCommentService
    // are not provided anywhere in the Base*Test hierarchy, so they must be declared locally here.
    @Mock private SubmissionCommentRepository submissionCommentRepository;
    @Mock private SubmissionCommentService submissionCommentService;

    private SubmissionServiceImpl submissionService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
        clearInvocations(assignmentRepository);

        submissionService = new SubmissionServiceImpl(
            answerEssaySubmissionRepository,
            answerFileSubmissionRepository,
            answerMcRepository,
            answerMcSubmissionOptionRepository,
            answerMcSubmissionRepository,
            assessmentRepository,
            assignmentRepository,
            exposureGroupConditionRepository,
            participantRepository,
            questionSubmissionRepository,
            submissionCommentRepository,
            submissionRepository,
            treatmentRepository,
            integrationLaunchService,
            questionSubmissionService,
            submissionCommentService,
            assessmentSubmissionService,
            advantageAgsService,
            caliperService,
            apiJwtService,
            integrationTokenService,
            apiClient
        );

        when(answerMcRepository.findByQuestion_QuestionId(anyLong())).thenReturn(Collections.singletonList(answerMc));
        when(answerMcSubmissionOptionRepository.save(any(AnswerMcSubmissionOption.class))).thenReturn(null);
        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(participant);

        when(answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.singletonList(answerMcSubmission));
        when(questionSubmissionService.automaticGradingMC(any(QuestionSubmission.class), any(AnswerMcSubmission.class))).thenReturn(questionSubmission);

        when(answerMc.getCorrect()).thenReturn(true);
        when(assessment.getQuestions()).thenReturn(Collections.singletonList(question));
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);
        when(questionMc.getPoints()).thenReturn(10F);
        when(regradeDetails.getEditedMCQuestionIds()).thenReturn(Collections.singletonList(1L));
        when(regradeDetails.getRegradeOption()).thenReturn(RegradeOption.FULL);
    }

    /**
     * Builds a Submission mock representing a fully-graded submission (not needing manual grading)
     * with the given score, for use in multiple-submission scoring scheme tests.
     */
    private Submission mockGradedSubmission(float score) {
        Submission gradedSubmission = mock(Submission.class);
        when(gradedSubmission.getAssessment()).thenReturn(assessment);
        when(gradedSubmission.isGradeOverridden()).thenReturn(false);
        when(gradedSubmission.getAlteredCalculatedGrade()).thenReturn(score);
        when(gradedSubmission.getQuestionSubmissions()).thenReturn(Collections.emptyList());

        return gradedSubmission;
    }

    /**
     * Builds a Submission mock that still needs manual grading (has an ungraded ESSAY question
     * submission), for use in multiple-submission scoring scheme tests.
     */
    private Submission mockSubmissionNeedingManualGrading() {
        Submission needsGradingSubmission = mock(Submission.class);
        Question essayQuestion = mock(Question.class);
        when(essayQuestion.getQuestionType()).thenReturn(QuestionTypes.ESSAY);
        when(essayQuestion.getPoints()).thenReturn(5F);
        QuestionSubmission ungradedQuestionSubmission = mock(QuestionSubmission.class);
        when(ungradedQuestionSubmission.getQuestion()).thenReturn(essayQuestion);
        when(ungradedQuestionSubmission.getAlteredGrade()).thenReturn(null);
        when(needsGradingSubmission.getQuestionSubmissions()).thenReturn(List.of(ungradedQuestionSubmission));

        return needsGradingSubmission;
    }

    @Test
    public void testPostSubmissionNotStarted() throws IdInPostException, ParticipantNotMatchingException, InvalidUserException, DataServiceException, IntegrationTokenNotFoundException {
        submissionService.postSubmission(SubmissionDto.builder().build(), 0l, securedInfo, 0l, false);

        verify(assignmentRepository).save(assignment);
    }

    @Test
    public void testPostSubmissionAlreadyStarted() throws IdInPostException, ParticipantNotMatchingException, InvalidUserException, DataServiceException, IntegrationTokenNotFoundException {
        when(assignment.isStarted()).thenReturn(true);
        submissionService.postSubmission(SubmissionDto.builder().build(), 0l, securedInfo, 0l, false);

        verify(assignmentRepository, never()).save(assignment);
    }

    @Test
    public void testCreateNewSubmissionNotStarted() throws IdInPostException, ParticipantNotMatchingException, InvalidUserException, DataServiceException, IntegrationTokenNotFoundException {
        when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);

        submissionService.createNewSubmission(assessment, participant, securedInfo);

        verify(assignmentRepository).save(assignment);
    }

    @Test
    public void testCreateNewSubmissionAlreadyStarted() throws IdInPostException, ParticipantNotMatchingException, InvalidUserException, DataServiceException, IntegrationTokenNotFoundException {
        when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);
        when(assignment.isStarted()).thenReturn(true);
        submissionService.createNewSubmission(assessment, participant, securedInfo);

        verify(assignmentRepository, never()).save(assignment);
    }

    // test toDto when questionSubmissions is true and not submitted, calls
    // QuestionSubmissionService with answerSubmissions=true
    @Test
    public void testToDtoWithQuestionSubmissionsWhenSubmissionNotSubmitted() throws IOException {

        when(submission.getDateSubmitted()).thenReturn(null);

        List<QuestionSubmission> questionSubmissions = new ArrayList<>();
        QuestionSubmission qs1 = new QuestionSubmission();
        QuestionSubmission qs2 = new QuestionSubmission();
        questionSubmissions.add(qs1);
        questionSubmissions.add(qs2);

        when(questionSubmissionRepository.findBySubmission_SubmissionId(anyLong())).thenReturn(questionSubmissions);

        submissionService.toDto(submission, true, false);

        verify(questionSubmissionService).toDto(eq(qs1), eq(true), eq(false), anyMap(), anyMap(), anyMap(), anyMap());
        verify(questionSubmissionService).toDto(eq(qs2), eq(true), eq(false), anyMap(), anyMap(), anyMap(), anyMap());
    }

    // test toDto when questionSubmissions is true and submitted, calls
    // QuestionSubmissionService with answerSubmissions=false
    @Test
    public void testToDtoWithQuestionSubmissionsWhenSubmissionIsSubmitted() throws IOException {

        when(submission.getDateSubmitted()).thenReturn(new Timestamp(System.currentTimeMillis()));

        List<QuestionSubmission> questionSubmissions = new ArrayList<>();
        QuestionSubmission qs1 = new QuestionSubmission();
        QuestionSubmission qs2 = new QuestionSubmission();
        questionSubmissions.add(qs1);
        questionSubmissions.add(qs2);

        when(questionSubmissionRepository.findBySubmission_SubmissionId(anyLong())).thenReturn(questionSubmissions);

        submissionService.toDto(submission, true, false);

        verify(questionSubmissionService).toDto(eq(qs1), eq(false), eq(false), anyMap(), anyMap(), anyMap(), anyMap());
        verify(questionSubmissionService).toDto(eq(qs2), eq(false), eq(false), anyMap(), anyMap(), anyMap(), anyMap());
    }

    // test allowedSubmission that when participant has revoked consent but is
    // assigned to a group and submission is for default treatment, should NOT
    // throw exception
    @Test
    public void testAllowedSubmissionWithConsentRevokedAndGroupAssignmentAndDefaultTreatment() {
        when(participant.getConsent()).thenReturn(false);
        Group group1 = new Group();
        group1.setGroupId(1L);
        when(participant.getGroup()).thenReturn(group1);
        when(condition.getDefaultCondition()).thenReturn(true);

        ExposureGroupCondition exposureGroupCondition = new ExposureGroupCondition();
        Group group2 = new Group();
        group2.setGroupId(2L);
        exposureGroupCondition.setGroup(group2);
        // Don't expect this to be called, but for completeness this is stubbed out
        when(exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.of(exposureGroupCondition));
        assertNotEquals(group1, group2);

        assertDoesNotThrow(() -> {
            submissionService.allowedSubmission(1l, securedInfo);
        });
    }

    // test allowedSubmission that when participant has revoked consent but is
    // assigned to a group and submission is NOT for default treatment, SHOULD
    // throw exception
    @Test
    public void testAllowedSubmissionWithConsentRevokedAndGroupAssignmentAndNotDefaultTreatment() {
        when(participant.getConsent()).thenReturn(false);
        Group group1 = new Group();
        group1.setGroupId(1L);
        when(participant.getGroup()).thenReturn(group1);
        when(condition.getDefaultCondition()).thenReturn(false);

        ExposureGroupCondition exposureGroupCondition = new ExposureGroupCondition();
        Group group2 = new Group();
        group2.setGroupId(2L);
        exposureGroupCondition.setGroup(group2);
        // Don't expect this to be called, but for completeness this is stubbed out
        when(exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.of(exposureGroupCondition));
        assertNotEquals(group1, group2);

        assertThrows(SubmissionNotMatchingException.class, () -> {
            submissionService.allowedSubmission(1l, securedInfo);
        });
    }

    // additional allowedSubmission branches not covered above: submission not
    // found, user mismatch, group present and matching/mismatching the
    // exposure group condition, and the single-treatment fallback.

    @Test
    public void testAllowedSubmissionThrowsWhenSubmissionNotFound() {
        when(submissionRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(SubmissionNotMatchingException.class, () -> submissionService.allowedSubmission(1L, securedInfo));
    }

    @Test
    public void testAllowedSubmissionThrowsWhenUserMismatch() {
        when(securedInfo.getUserId()).thenReturn("some-other-user");

        assertThrows(SubmissionNotMatchingException.class, () -> submissionService.allowedSubmission(1L, securedInfo));
    }

    @Test
    public void testAllowedSubmissionReturnsWhenGroupMatchesExposureGroupCondition() {
        // defaults: participant.getConsent() == true, participant.getGroup() ==
        // group, exposureGroupCondition.getGroup() == group, so the group != null
        // branch is taken and the group matches the exposure group condition
        assertDoesNotThrow(() -> submissionService.allowedSubmission(1L, securedInfo));
    }

    @Test
    public void testAllowedSubmissionReturnsWhenGroupMismatchButSingleTreatment() {
        Group otherGroup = new Group();
        otherGroup.setGroupId(99L);
        when(exposureGroupCondition.getGroup()).thenReturn(otherGroup);
        // default treatmentRepository stub returns a single treatment

        assertDoesNotThrow(() -> submissionService.allowedSubmission(1L, securedInfo));
    }

    @Test
    public void testAllowedSubmissionThrowsWhenGroupMismatchAndMultipleTreatments() {
        Group otherGroup = new Group();
        otherGroup.setGroupId(99L);
        when(exposureGroupCondition.getGroup()).thenReturn(otherGroup);
        when(treatmentRepository.findByAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(anyLong())).thenReturn(List.of(treatment, treatment));

        assertThrows(SubmissionNotMatchingException.class, () -> submissionService.allowedSubmission(1L, securedInfo));
    }

    @Test
    public void testAllowedSubmissionThrowsWhenNoExposureGroupConditionAndMultipleTreatments() {
        when(exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(treatmentRepository.findByAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(anyLong())).thenReturn(List.of(treatment, treatment));

        assertThrows(SubmissionNotMatchingException.class, () -> submissionService.allowedSubmission(1L, securedInfo));
    }

    // isOwnSubmission

    @Test
    public void testIsOwnSubmissionReturnsTrueWhenUserMatches() {
        assertTrue(submissionService.isOwnSubmission(1L, securedInfo));
    }

    @Test
    public void testIsOwnSubmissionReturnsFalseWhenUserMismatch() {
        when(securedInfo.getUserId()).thenReturn("some-other-user");

        assertFalse(submissionService.isOwnSubmission(1L, securedInfo));
    }

    @Test
    public void testIsOwnSubmissionReturnsFalseWhenSubmissionNotFound() {
        when(submissionRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertFalse(submissionService.isOwnSubmission(1L, securedInfo));
    }

    // getSubmissions

    @Test
    public void testGetSubmissionsInstructorFiltersOutTestStudents() throws NoSubmissionsException {
        Participant testStudentParticipant = mock(Participant.class);
        when(testStudentParticipant.isTestStudent()).thenReturn(true);
        Submission testStudentSubmission = mock(Submission.class);
        when(testStudentSubmission.getParticipant()).thenReturn(testStudentParticipant);

        when(submissionRepository.findByAssessment_AssessmentId(anyLong())).thenReturn(List.of(submission, testStudentSubmission));

        List<SubmissionDto> result = submissionService.getSubmissions(1L, USER_ID, 1L, false);

        assertEquals(1, result.size());
    }

    @Test
    public void testGetSubmissionsStudentThrowsWhenNoSubmissions() {
        when(submissionRepository.findByParticipant_Id(anyLong())).thenReturn(Collections.emptyList());

        assertThrows(NoSubmissionsException.class, () -> submissionService.getSubmissions(1L, USER_ID, 1L, true));
    }

    @Test
    public void testGetSubmissionsStudentReturnsDtos() throws NoSubmissionsException {
        when(submissionRepository.findByParticipant_Id(anyLong())).thenReturn(List.of(submission));

        List<SubmissionDto> result = submissionService.getSubmissions(1L, USER_ID, 1L, true);

        assertEquals(1, result.size());
    }

    // getSubmission

    @Test
    public void testGetSubmissionInstructor() throws NoSubmissionsException {
        Submission result = submissionService.getSubmission(1L, USER_ID, 1L, false);

        assertEquals(submission, result);
    }

    @Test
    public void testGetSubmissionStudentFound() throws NoSubmissionsException {
        when(submissionRepository.findByParticipant_IdAndSubmissionId(anyLong(), anyLong())).thenReturn(Optional.of(submission));

        Submission result = submissionService.getSubmission(1L, USER_ID, 1L, true);

        assertEquals(submission, result);
    }

    @Test
    public void testGetSubmissionStudentNotFoundThrows() {
        when(submissionRepository.findByParticipant_IdAndSubmissionId(anyLong(), anyLong())).thenReturn(Optional.empty());

        assertThrows(NoSubmissionsException.class, () -> submissionService.getSubmission(1L, USER_ID, 1L, true));
    }

    // postSubmission additional branches

    @Test
    public void testPostSubmissionThrowsWhenIdAlreadyPresent() {
        SubmissionDto dto = SubmissionDto.builder().submissionId(5L).build();

        assertThrows(IdInPostException.class, () -> submissionService.postSubmission(dto, 0L, securedInfo, 0L, false));
    }

    @Test
    public void testPostSubmissionWrapsDataServiceExceptionFromFromDto() {
        when(participantRepository.findById(anyLong())).thenReturn(Optional.empty());
        SubmissionDto dto = SubmissionDto.builder().build();

        DataServiceException ex = assertThrows(DataServiceException.class, () -> submissionService.postSubmission(dto, 0L, securedInfo, 0L, false));
        assertTrue(ex.getMessage().contains("Error 105"));
    }

    @Test
    public void testPostSubmissionWithIntegrationBuildsLaunchUrl() throws IdInPostException, ParticipantNotMatchingException, InvalidUserException, DataServiceException, IntegrationTokenNotFoundException {
        when(submission.isIntegration()).thenReturn(true);
        when(submission.getIntegrationTokenLaunchedAt()).thenReturn(new Timestamp(System.currentTimeMillis()));

        SubmissionDto result = submissionService.postSubmission(SubmissionDto.builder().build(), 0L, securedInfo, 0L, false);

        assertEquals(INTEGRATION_LAUNCH_URL, result.getIntegrationLaunchUrl());
        verify(integrationLaunchService, atLeastOnce()).buildUrl(any(Submission.class), any(Integer.class), any());
    }

    // updateSubmissions

    @Test
    public void testUpdateSubmissionsStudentReturnsImmediately() throws ConnectionException, DataServiceException, ApiException, IOException, TerracottaConnectorException {
        Map<Submission, SubmissionDto> map = Map.of(submission, submissionDto);

        submissionService.updateSubmissions(map, true);

        verify(submissionRepository, never()).save(any(Submission.class));
        verify(advantageAgsService, never()).getToken(any(), any());
    }

    @Test
    public void testUpdateSubmissionsEmptyMapReturnsImmediately() throws ConnectionException, DataServiceException, ApiException, IOException, TerracottaConnectorException {
        submissionService.updateSubmissions(Collections.emptyMap(), false);

        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    public void testUpdateSubmissionsSuccessSendsGradeToLmsOnce() throws ConnectionException, DataServiceException, ApiException, IOException, TerracottaConnectorException {
        when(assignment.getResourceLinkId()).thenReturn(RESOURCE_LINK_ID);
        Map<Submission, SubmissionDto> map = Map.of(submission, submissionDto);

        submissionService.updateSubmissions(map, false);

        verify(submissionRepository).save(submission);
        verify(advantageAgsService).postScore(any(), any(), any(), eq("1"), any());
    }

    @Test
    public void testUpdateSubmissionsThrowsWhenLineItemNotFound() {
        // no assignment.getResourceLinkId() stub, so it won't match the default lineItem's resourceLinkId
        Map<Submission, SubmissionDto> map = Map.of(submission, submissionDto);

        DataServiceException ex = assertThrows(DataServiceException.class, () -> submissionService.updateSubmissions(map, false));
        assertTrue(ex.getMessage().contains("Error 136"));
    }

    // toDto additional branches

    @Test
    public void testToDtoWithSubmissionComments() {
        SubmissionComment comment = mock(SubmissionComment.class);
        SubmissionCommentDto commentDto = mock(SubmissionCommentDto.class);
        when(submissionCommentRepository.findBySubmission_SubmissionId(anyLong())).thenReturn(List.of(comment));
        when(submissionCommentService.toDto(comment)).thenReturn(commentDto);

        SubmissionDto result = submissionService.toDto(submission, false, true);

        assertEquals(1, result.getSubmissionCommentDtoList().size());
        verify(submissionCommentService).toDto(comment);
    }

    @Test
    public void testToDtoWithIntegrationSetsLaunchFields() {
        when(submission.isIntegration()).thenReturn(true);
        when(submission.getIntegrationTokenLaunchedAt()).thenReturn(new Timestamp(1_000_000L));
        when(submission.isIntegrationFeedbackEnabled()).thenReturn(true);

        SubmissionDto result = submissionService.toDto(submission, false, false);

        assertTrue(result.isIntegrationFeedbackEnabled());
        assertEquals(INTEGRATION_LAUNCH_URL, result.getIntegrationLaunchUrl());
        verify(integrationLaunchService).buildUrl(submission, 0, submission.getIntegration());
    }

    // fromDto

    @Test
    public void testFromDtoInstructorSetsGradeFields() throws DataServiceException {
        SubmissionDto dto = SubmissionDto.builder()
            .participantId(1L)
            .assessmentId(1L)
            .calculatedGrade(5F)
            .alteredCalculatedGrade(6F)
            .totalAlteredGrade(7F)
            .gradeOverridden(true)
            .lateSubmission(true)
            .dateSubmitted(new Timestamp(System.currentTimeMillis()))
            .build();

        Submission result = submissionService.fromDto(dto, false);

        assertEquals(5F, result.getCalculatedGrade());
        assertEquals(6F, result.getAlteredCalculatedGrade());
        assertEquals(7F, result.getTotalAlteredGrade());
        assertTrue(result.isGradeOverridden());
        assertTrue(result.isLateSubmission());
        assertEquals(participant, result.getParticipant());
        assertEquals(assessment, result.getAssessment());
    }

    @Test
    public void testFromDtoStudentDoesNotSetGradeFields() throws DataServiceException {
        SubmissionDto dto = SubmissionDto.builder()
            .participantId(1L)
            .assessmentId(1L)
            .calculatedGrade(5F)
            .gradeOverridden(true)
            .build();

        Submission result = submissionService.fromDto(dto, true);

        assertNull(result.getCalculatedGrade());
        assertFalse(result.isGradeOverridden());
    }

    @Test
    public void testFromDtoThrowsWhenParticipantNotFound() {
        when(participantRepository.findById(anyLong())).thenReturn(Optional.empty());
        SubmissionDto dto = SubmissionDto.builder().participantId(99L).assessmentId(1L).build();

        assertThrows(DataServiceException.class, () -> submissionService.fromDto(dto, false));
    }

    @Test
    public void testFromDtoThrowsWhenAssessmentNotFound() {
        when(assessmentRepository.findById(anyLong())).thenReturn(Optional.empty());
        SubmissionDto dto = SubmissionDto.builder().participantId(1L).assessmentId(99L).build();

        assertThrows(DataServiceException.class, () -> submissionService.fromDto(dto, false));
    }

    // deleteById

    @Test
    public void testDeleteByIdDelegatesToRepository() {
        submissionService.deleteById(5L);

        verify(submissionRepository).deleteBySubmissionId(5L);
    }

    // finalizeAndGrade

    @Test
    public void testFinalizeAndGradeThrowsWhenSubmissionNotFound() {
        when(submissionRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(DataServiceException.class, () -> submissionService.finalizeAndGrade(1L, securedInfo, false));
    }

    @Test
    public void testFinalizeAndGradeSuccessWhenAlreadySubmittedAndDatesAllowed() throws Exception {
        when(assignment.getResourceLinkId()).thenReturn(RESOURCE_LINK_ID);

        submissionService.finalizeAndGrade(1L, securedInfo, false);

        verify(submissionRepository).saveAndFlush(submission);
        verify(caliperService).sendAssignmentSubmitted(submission, securedInfo);
        verify(advantageAgsService).postScore(any(), any(), any(), eq("1"), any());
    }

    @Test
    public void testFinalizeAndGradeThrowsAssignmentDatesExceptionWhenUnlockAtAfterSubmission() {
        when(securedInfo.getUnlockAt()).thenReturn(new Timestamp(System.currentTimeMillis() + 1_000_000L));

        assertThrows(AssignmentDatesException.class, () -> submissionService.finalizeAndGrade(1L, securedInfo, false));
    }

    @Test
    public void testFinalizeAndGradeThrowsAssignmentDatesExceptionWhenLockAtBeforeSubmission() {
        when(securedInfo.getLockAt()).thenReturn(new Timestamp(0L));

        assertThrows(AssignmentDatesException.class, () -> submissionService.finalizeAndGrade(1L, securedInfo, false));
    }

    @Test
    public void testFinalizeAndGradeSetsLateSubmissionWhenDateSubmittedNullAndPastDue() throws Exception {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp past = new Timestamp(now.getTime() - 1_000_000L);
        when(submission.getDateSubmitted()).thenReturn(null);
        when(securedInfo.getDueAt()).thenReturn(past);
        when(submission.getUpdatedAt()).thenReturn(now);
        when(questionSubmission.getUpdatedAt()).thenReturn(past);
        when(assignment.getResourceLinkId()).thenReturn(RESOURCE_LINK_ID);

        submissionService.finalizeAndGrade(1L, securedInfo, false);

        verify(submission).setLateSubmission(true);
        verify(submission).setDateSubmitted(now);
    }

    @Test
    public void testFinalizeAndGradeAddsOneMillisecondWhenLastUpdatedEqualsCreatedAt() throws Exception {
        Timestamp t = new Timestamp(System.currentTimeMillis());
        when(submission.getDateSubmitted()).thenReturn(null);
        when(submission.getUpdatedAt()).thenReturn(t);
        when(submission.getCreatedAt()).thenReturn(t);
        when(submission.getQuestionSubmissions()).thenReturn(Collections.emptyList());
        when(assignment.getResourceLinkId()).thenReturn(RESOURCE_LINK_ID);

        submissionService.finalizeAndGrade(1L, securedInfo, false);

        ArgumentCaptor<Timestamp> captor = ArgumentCaptor.forClass(Timestamp.class);
        verify(submission).setDateSubmitted(captor.capture());
        assertEquals(t.getTime() + 1, captor.getValue().getTime());
    }

    // datesAllowed (public overload)

    @Test
    public void testDatesAllowedTrueWhenNoBounds() {
        assertTrue(submissionService.datesAllowed(1L, 1L, securedInfo));
    }

    @Test
    public void testDatesAllowedFalseWhenUnlockAtInFuture() {
        when(securedInfo.getUnlockAt()).thenReturn(new Timestamp(System.currentTimeMillis() + 1_000_000L));

        assertFalse(submissionService.datesAllowed(1L, 1L, securedInfo));
    }

    // grade

    @Test
    public void testGradeThrowsWhenSubmissionNotFound() {
        when(submissionRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(DataServiceException.class, () -> submissionService.grade(1L, securedInfo));
    }

    @Test
    public void testGradeSuccess() throws DataServiceException {
        submissionService.grade(1L, securedInfo);

        verify(submissionRepository).saveAndFlush(submission);
    }

    // validateDto

    @Test
    public void testValidateDtoThrowsWhenParticipantNotFound() {
        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(null);

        assertThrows(ParticipantNotMatchingException.class, () -> submissionService.validateDto(1L, USER_ID, SubmissionDto.builder().build()));
    }

    @Test
    public void testValidateDtoThrowsWhenStudentAltersGrade() {
        SubmissionDto dto = SubmissionDto.builder().alteredCalculatedGrade(5F).build();

        assertThrows(InvalidUserException.class, () -> submissionService.validateDto(1L, USER_ID, dto));
    }

    // validateUser

    @Test
    public void testValidateUserDoesNotThrowWhenFound() {
        when(submissionRepository.findByParticipant_IdAndSubmissionId(anyLong(), anyLong())).thenReturn(Optional.of(submission));

        assertDoesNotThrow(() -> submissionService.validateUser(1L, USER_ID, 1L));
    }

    @Test
    public void testValidateUserThrowsWhenNotFound() {
        when(submissionRepository.findByParticipant_IdAndSubmissionId(anyLong(), anyLong())).thenReturn(Optional.empty());

        assertThrows(InvalidUserException.class, () -> submissionService.validateUser(1L, USER_ID, 1L));
    }

    // buildHeaders

    @Test
    public void testBuildHeadersSetsLocation() {
        org.springframework.web.util.UriComponentsBuilder builder = org.springframework.web.util.UriComponentsBuilder.newInstance();

        org.springframework.http.HttpHeaders headers = submissionService.buildHeaders(builder, 1L, 2L, 3L, 4L, 5L);

        assertNotNull(headers.getLocation());
        assertTrue(headers.getLocation().toString().contains("/1/conditions/2/treatments/3/assessments/4/submissions/5"));
    }

    // getAllSubmissionsForMultipleAssignments

    @Test
    public void testGetAllSubmissionsForMultipleAssignments() throws ApiException, TerracottaConnectorException, IOException {
        Map<String, List<LmsSubmission>> result = submissionService.getAllSubmissionsForMultipleAssignments(ltiUserEntity, "course1", List.of("a1", "a2"));

        assertEquals(2, result.size());
        verify(apiClient, times(2)).listSubmissions(eq(ltiUserEntity), anyString(), eq("course1"));
    }

    // sendSubmissionGradesToLmsWithLti (multi-submission overload, verifies token caching)

    @Test
    public void testSendSubmissionGradesToLmsWithLtiCachesTokensPerPlatformDeployment() throws ConnectionException, DataServiceException, ApiException, IOException, TerracottaConnectorException {
        when(assignment.getResourceLinkId()).thenReturn(RESOURCE_LINK_ID);

        submissionService.sendSubmissionGradesToLmsWithLti(List.of(submission, submission), false);

        verify(advantageAgsService, times(1)).getToken(eq(LtiAgsScope.SCORES), any(PlatformDeployment.class));
        verify(advantageAgsService, times(1)).getToken(eq(LtiAgsScope.RESULTS), any(PlatformDeployment.class));
        verify(advantageAgsService, times(2)).postScore(any(), any(), any(), eq("1"), any());
    }

    // createNewSubmission: createIntegrationLaunchUrl swallows NoSubmissionsException

    @Test
    public void testCreateNewSubmissionIntegrationHandlesNoSubmissionsException() throws IntegrationTokenNotFoundException {
        when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);
        when(submission.isIntegration()).thenReturn(true);
        when(submissionRepository.findByParticipant_Id(anyLong())).thenReturn(Collections.emptyList());

        Submission result = submissionService.createNewSubmission(assessment, participant, securedInfo);

        assertEquals(submission, result);
        verify(integrationLaunchService).buildUrl(submission, 0, submission.getIntegration());
    }

    // isManualGradingNeeded

    @Test
    public void testIsManualGradingNeededFalseWhenGradeAltered() {
        when(assessmentSubmissionService.isGradeAltered(submission)).thenReturn(true);

        assertFalse(submissionService.isManualGradingNeeded(submission));
    }

    @Test
    public void testIsManualGradingNeededTrueForUngradedEssayWithPoints() {
        when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);
        when(question.getPoints()).thenReturn(5F);
        when(questionSubmission.getAlteredGrade()).thenReturn(null);

        assertTrue(submissionService.isManualGradingNeeded(submission));
    }

    @Test
    public void testIsManualGradingNeededFalseForMcQuestion() {
        // default question type from beforeEach() is MC
        assertFalse(submissionService.isManualGradingNeeded(submission));
    }

    // getSubmissionScore

    @Test
    public void testGetSubmissionScoreZeroMaxScoreReturnsFullCredit() {
        when(assessmentSubmissionService.calculateMaxScore(assessment)).thenReturn(0F);

        assertEquals(1F, submissionService.getSubmissionScore(submission));
    }

    @Test
    public void testGetSubmissionScoreReturnsTotalAlteredGradeWhenOverridden() {
        when(submission.isGradeOverridden()).thenReturn(true);
        when(submission.getTotalAlteredGrade()).thenReturn(8F);

        assertEquals(8F, submissionService.getSubmissionScore(submission));
    }

    // getScoreFromMultipleSubmissions / computeScoreFromSubmissions (all scoring schemes)

    @Test
    public void testGetScoreFromMultipleSubmissionsEmptyListReturnsNull() {
        when(submissionRepository.findByParticipant_IdAndAssessment_AssessmentIdAndDateSubmittedNotNullOrderByDateSubmitted(anyLong(), anyLong())).thenReturn(Collections.emptyList());

        assertNull(submissionService.getScoreFromMultipleSubmissions(participant, assessment));
    }

    @Test
    public void testGetScoreFromMultipleSubmissionsSingleAllowedNotNeedingGrading() {
        when(assessment.getNumOfSubmissions()).thenReturn(1);
        Submission graded = mockGradedSubmission(5F);
        when(submissionRepository.findByParticipant_IdAndAssessment_AssessmentIdAndDateSubmittedNotNullOrderByDateSubmitted(anyLong(), anyLong())).thenReturn(List.of(graded));

        assertEquals(5F, submissionService.getScoreFromMultipleSubmissions(participant, assessment));
    }

    @Test
    public void testGetScoreFromMultipleSubmissionsSingleAllowedNeedingGradingReturnsNull() {
        when(assessment.getNumOfSubmissions()).thenReturn(1);
        Submission needsGrading = mockSubmissionNeedingManualGrading();
        when(submissionRepository.findByParticipant_IdAndAssessment_AssessmentIdAndDateSubmittedNotNullOrderByDateSubmitted(anyLong(), anyLong())).thenReturn(List.of(needsGrading));

        assertNull(submissionService.getScoreFromMultipleSubmissions(participant, assessment));
    }

    @Test
    public void testGetScoreFromMultipleSubmissionsMostRecentSkipsUngraded() {
        when(assessment.getNumOfSubmissions()).thenReturn(3);
        when(assessment.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.MOST_RECENT);
        Submission first = mockGradedSubmission(2F);
        Submission mostRecentNeedsGrading = mockSubmissionNeedingManualGrading();
        when(submissionRepository.findByParticipant_IdAndAssessment_AssessmentIdAndDateSubmittedNotNullOrderByDateSubmitted(anyLong(), anyLong())).thenReturn(List.of(first, mostRecentNeedsGrading));

        assertEquals(2F, submissionService.getScoreFromMultipleSubmissions(participant, assessment));
    }

    @Test
    public void testGetScoreFromMultipleSubmissionsAverage() {
        when(assessment.getNumOfSubmissions()).thenReturn(3);
        when(assessment.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.AVERAGE);
        Submission s1 = mockGradedSubmission(2F);
        Submission s2 = mockSubmissionNeedingManualGrading();
        Submission s3 = mockGradedSubmission(4F);
        when(submissionRepository.findByParticipant_IdAndAssessment_AssessmentIdAndDateSubmittedNotNullOrderByDateSubmitted(anyLong(), anyLong())).thenReturn(List.of(s1, s2, s3));

        assertEquals(3F, submissionService.getScoreFromMultipleSubmissions(participant, assessment));
    }

    @Test
    public void testGetScoreFromMultipleSubmissionsHighest() {
        when(assessment.getNumOfSubmissions()).thenReturn(3);
        when(assessment.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.HIGHEST);
        Submission s1 = mockGradedSubmission(2F);
        Submission s2 = mockGradedSubmission(9F);
        Submission s3 = mockSubmissionNeedingManualGrading();
        when(submissionRepository.findByParticipant_IdAndAssessment_AssessmentIdAndDateSubmittedNotNullOrderByDateSubmitted(anyLong(), anyLong())).thenReturn(List.of(s1, s2, s3));

        assertEquals(9F, submissionService.getScoreFromMultipleSubmissions(participant, assessment));
    }

    @Test
    public void testGetScoreFromMultipleSubmissionsCumulative() {
        when(assessment.getNumOfSubmissions()).thenReturn(3);
        when(assessment.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.CUMULATIVE);
        when(assessment.getCumulativeScoringInitialPercentage()).thenReturn(50F);
        Submission s1 = mockGradedSubmission(10F);
        Submission s2 = mockGradedSubmission(20F);
        Submission s3 = mockGradedSubmission(30F);
        when(submissionRepository.findByParticipant_IdAndAssessment_AssessmentIdAndDateSubmittedNotNullOrderByDateSubmitted(anyLong(), anyLong())).thenReturn(List.of(s1, s2, s3));

        assertEquals(17.5F, submissionService.getScoreFromMultipleSubmissions(participant, assessment));
    }

    @Test
    public void testGetScoreFromMultipleSubmissionsCumulativeFirstNeedsGrading() {
        when(assessment.getNumOfSubmissions()).thenReturn(2);
        when(assessment.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.CUMULATIVE);
        when(assessment.getCumulativeScoringInitialPercentage()).thenReturn(50F);
        Submission s1 = mockSubmissionNeedingManualGrading();
        Submission s2 = mockGradedSubmission(10F);
        when(submissionRepository.findByParticipant_IdAndAssessment_AssessmentIdAndDateSubmittedNotNullOrderByDateSubmitted(anyLong(), anyLong())).thenReturn(List.of(s1, s2));

        assertEquals(5F, submissionService.getScoreFromMultipleSubmissions(participant, assessment));
    }

    // getScoresFromMultipleSubmissions

    @Test
    public void testGetScoresFromMultipleSubmissionsOnlyIncludesParticipantsWithScores() {
        Participant participant2 = mock(Participant.class);
        when(participant2.getParticipantId()).thenReturn(2L);

        Submission scored = mockGradedSubmission(7F);
        when(scored.getParticipant()).thenReturn(participant);

        when(submissionRepository.findByParticipant_IdInAndAssessment_AssessmentIdAndDateSubmittedNotNullOrderByDateSubmitted(anyList(), anyLong()))
            .thenReturn(List.of(scored));

        Map<Long, Float> result = submissionService.getScoresFromMultipleSubmissions(List.of(participant, participant2), assessment);

        assertEquals(1, result.size());
        assertEquals(7F, result.get(1L));
    }

}
