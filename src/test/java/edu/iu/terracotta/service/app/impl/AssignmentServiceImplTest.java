package edu.iu.terracotta.service.app.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Optional;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.dao.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AssignmentDto;
import edu.iu.terracotta.dao.model.enums.MultipleSubmissionScoringScheme;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentMoveException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.utils.TextConstants;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AssignmentServiceImplTest extends BaseTest {

    private AssignmentServiceImpl assignmentService;

    @Mock private LtiUserEntity instructorUser;

    private Date dueDate = new Date();

    @BeforeEach
    public void beforeEach() throws AssessmentNotMatchingException, AssignmentAttemptException, NumberFormatException, IdInPostException, DataServiceException, ExceedingLimitException, TreatmentNotMatchingException, QuestionNotMatchingException, ApiException, TerracottaConnectorException, GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, ExperimentNotMatchingException {
        MockitoAnnotations.openMocks(this);

        setup();

        // apiClient/apiJwtService/lmsUtils below also collide with CanvasApiClientImpl/CanvasApiJwtServiceImpl/
        // BrightspaceLmsUtilsImpl mocks in BaseServiceTest (see the @InjectMocks pitfall note there), so this
        // class is constructed manually instead of relying on @InjectMocks, which non-deterministically wired
        // the wrong mocks and left apiClient/lmsUtils calls silently unstubbed.
        assignmentService = Mockito.spy(
            new AssignmentServiceImpl(
                answerEssaySubmissionRepository,
                answerFileSubmissionRepository,
                answerMcSubmissionRepository,
                assessmentRepository,
                assignmentRepository,
                apiTokenRepository,
                experimentRepository,
                exposureRepository,
                ltiContextRepository,
                ltiUserRepository,
                platformDeploymentRepository,
                submissionRepository,
                treatmentRepository,
                apiJwtService,
                assessmentService,
                assignmentTreatmentService,
                caliperService,
                componentUtils,
                integrationLaunchService,
                integrationTokenService,
                lmsUtils,
                participantService,
                submissionService,
                apiClient
            )
        );
        clearInvocations(assignmentRepository, apiClient);

        when(assessmentRepository.findByTreatment_Assignment_AssignmentId(anyLong())).thenReturn(Collections.singletonList(assessment));
        when(assignmentRepository.findByExposure_ExposureIdAndSoftDeletedOrderByAssignmentOrderAsc(anyLong(), anyBoolean())).thenReturn(Collections.singletonList(assignment));
        when(assignmentRepository.findByExposure_ExposureIdAndSoftDeletedOrderByAssignmentOrderDesc(anyLong(), anyBoolean())).thenReturn(Collections.singletonList(assignment));
        when(submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(anyLong())).thenReturn(0l);
        when(treatmentRepository.findByAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(anyLong())).thenReturn(Collections.emptyList());
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong())).thenReturn(instructorUser);
        // any(PlatformDeployment.class) below excludes null, so instructorUser.getPlatformDeployment() must be stubbed
        // or apiJwtService.unsecureToken(...)'s stub silently won't match, returning an unstubbed empty map instead
        when(instructorUser.getPlatformDeployment()).thenReturn(platformDeployment);

        when(participantService.handleExperimentParticipant(any(Experiment.class), any(SecuredInfo.class))).thenReturn(participant);
        when(assessmentService.getAssessmentForParticipant(any(Participant.class), any(SecuredInfo.class))).thenReturn(assessment);
        doNothing().when(assessmentService).verifySubmissionLimit(anyInt(), anyInt());
        doNothing().when(assessmentService).verifySubmissionWaitTime(anyFloat(), anyList());
        when(apiClient.listAssignment(eq(instructorUser), anyString(), anyString())).thenReturn(Optional.empty());
        when(apiClient.createLmsAssignment(any(LtiUserEntity.class), any(Assignment.class), anyString())).thenReturn(lmsAssignment);
        when(lmsAssignment.getSecureParams()).thenReturn(RESOURCE_LINK_ID);
        when(assignmentTreatmentService.duplicateTreatment(anyLong(), any(Assignment.class), any(SecuredInfo.class))).thenReturn(treatmentDto);

        when(assignment.getDueDate()).thenReturn(dueDate);
        when(assignment.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.MOST_RECENT);
        when(assignment.isPublished()).thenReturn(true);
        when(assignmentDto.getExposureId()).thenReturn(1L);
        when(assignmentDto.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.MOST_RECENT.toString());
        when(canvasAssignmentExtended.isPublished()).thenReturn(true);
        when(canvasAssignmentExtended.getDueAt()).thenReturn(dueDate);
        when(canvasApiJwtService.unsecureToken(anyString(), any(PlatformDeployment.class))).thenReturn(Collections.singletonMap("lti_assignment_id", "1"));
        when(apiJwtService.unsecureToken(anyString(), any(PlatformDeployment.class))).thenReturn(Collections.singletonMap("lti_assignment_id", "1"));
    }

    @Test
    public void testDuplicateAssignment() throws DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
            AssignmentNotCreatedException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, NumberFormatException,
            ExceedingLimitException, TreatmentNotMatchingException, QuestionNotMatchingException, ApiException, TerracottaConnectorException {
        when(assignmentDto.getAssignmentId()).thenReturn(null);

        AssignmentDto retVal = assignmentService.duplicateAssignment(1L, securedInfo);

        assertNotNull(retVal);
        verify(assignmentRepository).save(any(Assignment.class));
    }

    @Test
    public void testDuplicateAssignmentWithTreatments() throws DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
            AssignmentNotCreatedException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException,
            NumberFormatException, ExceedingLimitException, TreatmentNotMatchingException, QuestionNotMatchingException, ApiException, TerracottaConnectorException {
        when(assignmentDto.getAssignmentId()).thenReturn(null);
        when(treatmentRepository.findByAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(anyLong())).thenReturn(Collections.singletonList(treatment));

        AssignmentDto retVal = assignmentService.duplicateAssignment(0L, securedInfo);

        assertNotNull(retVal);
        verify(assignmentTreatmentService).duplicateTreatment(eq(treatment.getTreatmentId()), any(Assignment.class), eq(securedInfo));
    }

    @Test
    public void testDuplicateAssessmentNotFound() throws IdInPostException, AssessmentNotMatchingException {
        when(assignmentRepository.findByAssignmentId(anyLong())).thenReturn(null);

        Exception exception = assertThrows(DataServiceException.class, () -> { assignmentService.duplicateAssignment(1L, securedInfo); });

        assertEquals("The assignment with the given ID does not exist", exception.getMessage());
    }

    @Test
    public void testGetAssignments() throws AssessmentNotMatchingException, ApiException, NumberFormatException, TerracottaConnectorException {
        List<AssignmentDto> retVal = assignmentService.getAssignments(0L, false, false, securedInfo);

        assertNotNull(retVal);
        assertEquals(1, retVal.size());
    }

    @Test
    public void testGetAssignmentsNoCanvasAssignmentFound() throws AssessmentNotMatchingException, ApiException, NumberFormatException, TerracottaConnectorException {
        when(apiClient.listAssignment(eq(instructorUser),anyString(), anyString())).thenReturn(Optional.empty());
        List<AssignmentDto> retVal = assignmentService.getAssignments(0L, false, false, securedInfo);

        assertNotNull(retVal);
        assertEquals(1, retVal.size());
    }

    @Test
    public void testGetAssignmentsNoAssignmentsFound() throws AssessmentNotMatchingException, ApiException, NumberFormatException, TerracottaConnectorException {
        when(assignmentRepository.findByExposure_ExposureIdAndSoftDeletedOrderByAssignmentOrderAsc(anyLong(), anyBoolean())).thenReturn(Collections.emptyList());
        when(assignmentRepository.findByExposure_ExposureIdAndSoftDeletedOrderByAssignmentOrderDesc(anyLong(), anyBoolean())).thenReturn(Collections.emptyList());

        List<AssignmentDto> retVal = assignmentService.getAssignments(0L, false, false, securedInfo);

        assertNotNull(retVal);
        assertEquals(0, retVal.size());
    }

    @Test
    public void testDeleteAssignmentHard() throws EmptyResultDataAccessException, AssignmentNotEditedException, ApiException, TerracottaConnectorException {
        assignmentService.deleteById(1L, securedInfo);

        verify(assignmentRepository).deleteById(1L);
        verify(assignmentRepository, never()).saveAndFlush(any(Assignment.class));
    }

    @Test
    public void testDeleteAssignmentNotFound() throws EmptyResultDataAccessException, AssignmentNotEditedException, ApiException, TerracottaConnectorException, IOException {
        when(assignmentRepository.findById(anyLong())).thenReturn(Optional.empty());

        assignmentService.deleteById(1L, securedInfo);

        verify(assignmentRepository, never()).deleteById(anyLong());
        verify(assignmentRepository, never()).saveAndFlush(any(Assignment.class));
        verify(apiClient, never()).deleteAssignmentInLms(any(Assignment.class), anyString(), any(LtiUserEntity.class));
    }

    @Test
    public void testDeleteAssignmentSoft() throws EmptyResultDataAccessException, AssignmentNotEditedException, ApiException, TerracottaConnectorException {
        when(submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(anyLong())).thenReturn(1L);

        assignmentService.deleteById(1L, securedInfo);

        verify(assignmentRepository, never()).deleteByAssignmentId(anyLong());
        verify(assignmentRepository).saveAndFlush(any(Assignment.class));
    }

    @Test
    public void testMoveAssignment() throws NumberFormatException, DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
            AssignmentNotCreatedException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException,
            ExceedingLimitException, TreatmentNotMatchingException, ExposureNotMatchingException, AssignmentMoveException, AssignmentNotEditedException, QuestionNotMatchingException, AssignmentNotMatchingException,
            AssignmentNotCreatedException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, ExceedingLimitException, TreatmentNotMatchingException, ExposureNotMatchingException, AssignmentMoveException, AssignmentNotEditedException, QuestionNotMatchingException, ApiException {
        when(assignmentDto.getAssignmentId()).thenReturn(null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        AssignmentDto newAssignmentDto = assignmentService.moveAssignment(2l, assignmentDto, 1L, 2l, securedInfo);

        assertNotNull(newAssignmentDto);
        verify(assignmentRepository).save(any(Assignment.class));
    }

    @Test
    public void testMoveAssignmentExposuresMatch() throws IdInPostException, AssessmentNotMatchingException {
        Exception exception = assertThrows(AssignmentMoveException.class, () -> {
            assignmentService.moveAssignment(2l, assignmentDto, 1L, 1l, securedInfo);
        });

        assertEquals(TextConstants.UNABLE_TO_MOVE_ASSIGNMENT_EXPOSURE_SAME, exception.getMessage());
    }

    @Test
    public void testMoveAssignmentNoTargetExposureMatch() throws IdInPostException, AssessmentNotMatchingException {
        when(exposureRepository.findByExposureId(anyLong())).thenReturn(null);
        Exception exception = assertThrows(ExposureNotMatchingException.class, () -> { assignmentService.moveAssignment(2l, assignmentDto, 1L, 2l, securedInfo); });

        assertEquals(TextConstants.EXPOSURE_NOT_MATCHING, exception.getMessage());
    }

    @Test
    public void testFindAllByExposureId() {
        List<Assignment> retVal = assignmentService.findAllByExposureId(1L, false);

        assertNotNull(retVal);
        assertEquals(1, retVal.size());
    }

    @Test
    public void testGetAssignmentsNoInstructorUser() throws AssessmentNotMatchingException, ApiException, NumberFormatException, TerracottaConnectorException {
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong())).thenReturn(null);

        List<AssignmentDto> retVal = assignmentService.getAssignments(1L, false, false, securedInfo);

        assertNotNull(retVal);
        assertEquals(1, retVal.size());
        verify(assignmentTreatmentService, never()).setAssignmentDtoAttrs(anyList(), any(LtiUserEntity.class));
    }

    @Test
    public void testPostAssignmentSuccess() throws IdInPostException, DataServiceException, TitleValidationException, AssignmentNotCreatedException,
            AssessmentNotMatchingException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, NumberFormatException, ApiException, TerracottaConnectorException {
        when(assignmentDto.getAssignmentId()).thenReturn(null);
        when(assignmentDto.getTitle()).thenReturn("New Assignment");

        AssignmentDto retVal = assignmentService.postAssignment(assignmentDto, 1L, 1L, securedInfo);

        assertNotNull(retVal);
        verify(assignmentRepository).save(any(Assignment.class));
        verify(assignmentRepository).saveAndFlush(any(Assignment.class));
    }

    @Test
    public void testPostAssignmentIdInPost() {
        Exception exception = assertThrows(IdInPostException.class, () -> assignmentService.postAssignment(assignmentDto, 1L, 1L, securedInfo));

        assertEquals(TextConstants.ID_IN_POST_ERROR, exception.getMessage());
    }

    @Test
    public void testPostAssignmentRevealCorrectAnswersWithoutResponses() {
        when(assignmentDto.getAssignmentId()).thenReturn(null);
        when(assignmentDto.isAllowStudentViewCorrectAnswers()).thenReturn(true);

        Exception exception = assertThrows(RevealResponsesSettingValidationException.class, () -> assignmentService.postAssignment(assignmentDto, 1L, 1L, securedInfo));

        assertEquals("Error 151: Cannot allow students to view correct answers if they are not allowed to view responses.", exception.getMessage());
    }

    @Test
    public void testPostAssignmentResponsesDatesInvalidOrder() {
        when(assignmentDto.getAssignmentId()).thenReturn(null);
        Timestamp after = new Timestamp(System.currentTimeMillis() + 10_000);
        Timestamp before = new Timestamp(System.currentTimeMillis());
        when(assignmentDto.getStudentViewResponsesAfter()).thenReturn(after);
        when(assignmentDto.getStudentViewResponsesBefore()).thenReturn(before);

        Exception exception = assertThrows(RevealResponsesSettingValidationException.class, () -> assignmentService.postAssignment(assignmentDto, 1L, 1L, securedInfo));

        assertEquals("Error 152: Start date of revealing student responses must come before end date.", exception.getMessage());
    }

    @Test
    public void testPostAssignmentCorrectAnswersDatesInvalidOrder() {
        when(assignmentDto.getAssignmentId()).thenReturn(null);
        Timestamp after = new Timestamp(System.currentTimeMillis() + 10_000);
        Timestamp before = new Timestamp(System.currentTimeMillis());
        when(assignmentDto.getStudentViewCorrectAnswersAfter()).thenReturn(after);
        when(assignmentDto.getStudentViewCorrectAnswersBefore()).thenReturn(before);

        Exception exception = assertThrows(RevealResponsesSettingValidationException.class, () -> assignmentService.postAssignment(assignmentDto, 1L, 1L, securedInfo));

        assertEquals("Error 153: Start date of revealing correct answers must come before end date.", exception.getMessage());
    }

    @Test
    public void testPostAssignmentCorrectAnswersAfterBeforeResponsesAfter() {
        when(assignmentDto.getAssignmentId()).thenReturn(null);
        Timestamp earlier = new Timestamp(System.currentTimeMillis());
        Timestamp later = new Timestamp(System.currentTimeMillis() + 10_000);
        when(assignmentDto.getStudentViewCorrectAnswersAfter()).thenReturn(earlier);
        when(assignmentDto.getStudentViewResponsesAfter()).thenReturn(later);

        Exception exception = assertThrows(RevealResponsesSettingValidationException.class, () -> assignmentService.postAssignment(assignmentDto, 1L, 1L, securedInfo));

        assertEquals("Error 154: Start date of revealing correct answers must equal or come after start date of revealing student responses.", exception.getMessage());
    }

    @Test
    public void testPostAssignmentCorrectAnswersBeforeAfterResponsesBefore() {
        when(assignmentDto.getAssignmentId()).thenReturn(null);
        Timestamp earlier = new Timestamp(System.currentTimeMillis());
        Timestamp later = new Timestamp(System.currentTimeMillis() + 10_000);
        when(assignmentDto.getStudentViewResponsesBefore()).thenReturn(earlier);
        when(assignmentDto.getStudentViewCorrectAnswersBefore()).thenReturn(later);

        Exception exception = assertThrows(RevealResponsesSettingValidationException.class, () -> assignmentService.postAssignment(assignmentDto, 1L, 1L, securedInfo));

        assertEquals("Error 155: End date of revealing correct answers must equal or come before end date of revealing student responses.", exception.getMessage());
    }

    @Test
    public void testFromDtoSuccess() throws DataServiceException {
        when(assignmentDto.getTitle()).thenReturn("My Assignment");
        when(assignmentDto.getAssignmentOrder()).thenReturn(2);
        when(assignmentDto.getNumOfSubmissions()).thenReturn(3);
        when(assignmentDto.getHoursBetweenSubmissions()).thenReturn(1.5F);
        when(assignmentDto.getCumulativeScoringInitialPercentage()).thenReturn(50F);

        Assignment retVal = assignmentService.fromDto(assignmentDto);

        assertNotNull(retVal);
        assertEquals("My Assignment", retVal.getTitle());
        assertEquals(exposure, retVal.getExposure());
        assertEquals(MultipleSubmissionScoringScheme.MOST_RECENT, retVal.getMultipleSubmissionScoringScheme());
    }

    @Test
    public void testFromDtoExposureNotFound() {
        when(exposureRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(DataServiceException.class, () -> assignmentService.fromDto(assignmentDto));

        assertEquals("The exposure for the assignment does not exist", exception.getMessage());
    }

    @Test
    public void testSave() {
        Assignment retVal = assignmentService.save(assignment);

        assertNotNull(retVal);
        verify(assignmentRepository).save(assignment);
    }

    @Test
    public void testFindById() {
        Optional<Assignment> retVal = assignmentService.findById(1L);

        assertNotNull(retVal);
        assertEquals(assignment, retVal.get());
    }

    @Test
    public void testGetAssignment() {
        Assignment retVal = assignmentService.getAssignment(1L);

        assertEquals(assignment, retVal);
    }

    @Test
    public void testUpdateAssignmentsWrapper() throws TitleValidationException, ApiException, AssignmentNotEditedException,
            RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, AssignmentNotMatchingException, TerracottaConnectorException {
        String currentAssignmentTitle = assignment.getTitle();
        when(assignmentDto.getTitle()).thenReturn(currentAssignmentTitle);

        List<AssignmentDto> retVal = assignmentService.updateAssignments(List.of(assignmentDto, assignmentDto), securedInfo);

        assertNotNull(retVal);
        assertEquals(2, retVal.size());
    }

    @Test
    public void testPutAssignment() throws TitleValidationException, ApiException, AssignmentNotEditedException,
            RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, AssignmentNotMatchingException, TerracottaConnectorException {
        String currentAssignmentTitle = assignment.getTitle();
        when(assignmentDto.getTitle()).thenReturn(currentAssignmentTitle);

        AssignmentDto retVal = assignmentService.putAssignment(1L, assignmentDto, securedInfo);

        assertNotNull(retVal);
    }

    @Test
    public void testUpdateAssignmentNotFound() {
        when(assignmentRepository.findByAssignmentId(anyLong())).thenReturn(null);

        Exception exception = assertThrows(AssignmentNotMatchingException.class, () -> assignmentService.updateAssignment(1L, assignmentDto, securedInfo));

        assertEquals(TextConstants.ASSIGNMENT_NOT_MATCHING, exception.getMessage());
    }

    @Test
    public void testUpdateAssignmentBlankTitle() {
        when(assignment.getTitle()).thenReturn("");
        when(assignmentDto.getTitle()).thenReturn(null);

        Exception exception = assertThrows(TitleValidationException.class, () -> assignmentService.updateAssignment(1L, assignmentDto, securedInfo));

        assertEquals("Error 100: Please give the assignment a name.", exception.getMessage());
    }

    @Test
    public void testUpdateAssignmentTitleTooLong() {
        when(assignmentDto.getTitle()).thenReturn("a".repeat(256));

        Exception exception = assertThrows(TitleValidationException.class, () -> assignmentService.updateAssignment(1L, assignmentDto, securedInfo));

        assertEquals("Error 101: The title must be 255 characters or less.", exception.getMessage());
    }

    @Test
    public void testUpdateAssignmentTitleChanged() throws TitleValidationException, ApiException, AssignmentNotEditedException,
            RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, AssignmentNotMatchingException, TerracottaConnectorException, IOException {
        when(assignmentDto.getTitle()).thenReturn("Updated Title");

        Assignment retVal = assignmentService.updateAssignment(1L, assignmentDto, securedInfo);

        assertNotNull(retVal);
        verify(apiClient).editAssignmentNameInLms(eq(assignment), anyString(), eq("Updated Title"), any(LtiUserEntity.class));
        verify(assignmentRepository).saveAndFlush(assignment);
    }

    @Test
    public void testUpdateAssignmentTitleUnchanged() throws TitleValidationException, ApiException, AssignmentNotEditedException,
            RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, AssignmentNotMatchingException, TerracottaConnectorException, IOException {
        String currentAssignmentTitle = assignment.getTitle();
        when(assignmentDto.getTitle()).thenReturn(currentAssignmentTitle);

        Assignment retVal = assignmentService.updateAssignment(1L, assignmentDto, securedInfo);

        assertNotNull(retVal);
        verify(apiClient, never()).editAssignmentNameInLms(any(Assignment.class), anyString(), anyString(), any(LtiUserEntity.class));
    }

    @Test
    public void testUpdateAssignmentCumulativeMissingPercentage() {
        String currentAssignmentTitle = assignment.getTitle();
        when(assignmentDto.getTitle()).thenReturn(currentAssignmentTitle);
        when(assignmentDto.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.CUMULATIVE.toString());
        when(assignmentDto.getCumulativeScoringInitialPercentage()).thenReturn(null);

        Exception exception = assertThrows(MultipleAttemptsSettingsValidationException.class, () -> assignmentService.updateAssignment(1L, assignmentDto, securedInfo));

        assertEquals("Error 156: Must set cumulative scoring initial percentage when scoring scheme is CUMULATIVE", exception.getMessage());
    }

    @Test
    public void testUpdateAssignmentCumulativeInvalidNumOfSubmissions() {
        String currentAssignmentTitle = assignment.getTitle();
        when(assignmentDto.getTitle()).thenReturn(currentAssignmentTitle);
        when(assignmentDto.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.CUMULATIVE.toString());
        when(assignmentDto.getCumulativeScoringInitialPercentage()).thenReturn(50F);
        when(assignmentDto.getNumOfSubmissions()).thenReturn(1);

        Exception exception = assertThrows(MultipleAttemptsSettingsValidationException.class, () -> assignmentService.updateAssignment(1L, assignmentDto, securedInfo));

        assertEquals("Error 157: Number of submissions must be greater than 1, but not infinite, when scoring scheme is CUMULATIVE", exception.getMessage());
    }

    @Test
    public void testValidateTitleTooLong() {
        Exception exception = assertThrows(TitleValidationException.class, () -> assignmentService.validateTitle("a".repeat(256)));

        assertEquals("Error 101: Assignment title must not be empty and 255 characters or less.", exception.getMessage());
    }

    @Test
    public void testValidateTitleValid() throws TitleValidationException {
        assignmentService.validateTitle("A valid title");
    }

    @Test
    public void testAssignmentBelongsToExperimentAndExposure() {
        when(assignmentRepository.existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndAssignmentId(anyLong(), anyLong(), anyLong())).thenReturn(true);

        assertTrue(assignmentService.assignmentBelongsToExperimentAndExposure(1L, 1L, 1L));
    }

    @Test
    public void testAssignmentBelongsToExperiment() {
        when(assignmentRepository.existsByExposure_Experiment_ExperimentIdAndAssignmentId(anyLong(), anyLong())).thenReturn(true);

        assertTrue(assignmentService.assignmentBelongsToExperiment(1L, 1L));
    }

    @Test
    public void testSendAssignmentGradeToLms() throws Exception {
        when(submission.isSubmitted()).thenReturn(true);
        when(submissionRepository.findByAssessment_Treatment_Assignment_AssignmentId(anyLong())).thenReturn(List.of(submission));

        assignmentService.sendAssignmentGradeToLms(assignment);

        verify(submissionService).sendSubmissionGradesToLmsWithLti(List.of(submission), false);
    }

    @Test
    public void testLaunchAssignmentExperimentNotFound() throws Exception {
        when(experimentRepository.findById(anyLong())).thenReturn(Optional.empty());

        ResponseEntity<Object> retVal = assignmentService.launchAssignment(1L, securedInfo);

        assertEquals(HttpStatus.UNAUTHORIZED, retVal.getStatusCode());
        assertEquals(TextConstants.EXPERIMENT_NOT_MATCHING, retVal.getBody());
    }

    @Test
    public void testLaunchAssignmentTwoArgOverloadDelegates() throws Exception {
        when(submissionRepository.findByParticipant_IdAndAssessment_AssessmentId(anyLong(), anyLong())).thenReturn(Collections.emptyList());
        when(submissionService.datesAllowed(anyLong(), anyLong(), any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<Object> retVal = assignmentService.launchAssignment(1L, securedInfo);

        assertEquals(HttpStatus.UNAUTHORIZED, retVal.getStatusCode());
        assertEquals(TextConstants.ASSIGNMENT_LOCKED, retVal.getBody());
    }

    @Test
    public void testLaunchAssignmentDatesNotAllowed() throws Exception {
        when(submissionRepository.findByParticipant_IdAndAssessment_AssessmentId(anyLong(), anyLong())).thenReturn(Collections.emptyList());
        when(submissionService.datesAllowed(anyLong(), anyLong(), any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<Object> retVal = assignmentService.launchAssignment(1L, securedInfo, false);

        assertEquals(HttpStatus.UNAUTHORIZED, retVal.getStatusCode());
        assertEquals(TextConstants.ASSIGNMENT_LOCKED, retVal.getBody());
    }

    @Test
    public void testLaunchAssignmentSuccessCreatesSubmissionAndMarksExperimentStarted() throws Exception {
        when(submissionRepository.findByParticipant_IdAndAssessment_AssessmentId(anyLong(), anyLong())).thenReturn(Collections.emptyList());
        when(submissionService.datesAllowed(anyLong(), anyLong(), any(SecuredInfo.class))).thenReturn(true);
        when(submissionService.createNewSubmission(any(Assessment.class), any(Participant.class), any(SecuredInfo.class))).thenReturn(submission);
        when(submissionService.toDto(any(Submission.class), anyBoolean(), anyBoolean())).thenReturn(submissionDto);

        ResponseEntity<Object> retVal = assignmentService.launchAssignment(1L, securedInfo, false);

        assertEquals(HttpStatus.OK, retVal.getStatusCode());
        assertEquals(submissionDto, retVal.getBody());
        verify(experimentRepository).save(experiment);
    }

    @Test
    public void testLaunchAssignmentAttemptExceptionCaught() throws Exception {
        when(submissionRepository.findByParticipant_IdAndAssessment_AssessmentId(anyLong(), anyLong())).thenReturn(Collections.emptyList());
        doThrow(new AssignmentAttemptException("locked out")).when(assessmentService).verifySubmissionLimit(anyInt(), anyInt());

        ResponseEntity<Object> retVal = assignmentService.launchAssignment(1L, securedInfo, false);

        assertEquals(HttpStatus.UNAUTHORIZED, retVal.getStatusCode());
        assertEquals("locked out", retVal.getBody());
    }

    @Test
    public void testLaunchAssignmentRegradesIncompleteFullyAnsweredSubmission() throws Exception {
        when(submission.getDateSubmitted()).thenReturn(null);
        when(submissionRepository.findByParticipant_IdAndAssessment_AssessmentId(anyLong(), anyLong())).thenReturn(List.of(submission));
        when(answerEssaySubmissionRepository.countByQuestionSubmission_QuestionSubmissionIdIn(anyList())).thenReturn(1L);
        when(answerFileSubmissionRepository.countByQuestionSubmission_QuestionSubmissionIdIn(anyList())).thenReturn(0L);
        when(answerMcSubmissionRepository.countByQuestionSubmission_QuestionSubmissionIdIn(anyList())).thenReturn(0L);
        when(apiJwtService.isLearner(any(SecuredInfo.class))).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);
        when(submissionService.datesAllowed(anyLong(), anyLong(), any(SecuredInfo.class))).thenReturn(true);
        when(submissionService.createNewSubmission(any(Assessment.class), any(Participant.class), any(SecuredInfo.class))).thenReturn(submission);
        when(submissionService.toDto(any(Submission.class), anyBoolean(), anyBoolean())).thenReturn(submissionDto);

        ResponseEntity<Object> retVal = assignmentService.launchAssignment(1L, securedInfo, false);

        verify(submissionService).finalizeAndGrade(eq(1L), eq(securedInfo), eq(true));
        assertEquals(HttpStatus.OK, retVal.getStatusCode());
    }

    @Test
    public void testLaunchAssignmentReturnsExistingIncompleteSubmission() throws Exception {
        when(submission.getDateSubmitted()).thenReturn(null);
        when(submissionRepository.findByParticipant_IdAndAssessment_AssessmentId(anyLong(), anyLong())).thenReturn(List.of(submission));
        when(answerEssaySubmissionRepository.countByQuestionSubmission_QuestionSubmissionIdIn(anyList())).thenReturn(0L);
        when(answerFileSubmissionRepository.countByQuestionSubmission_QuestionSubmissionIdIn(anyList())).thenReturn(0L);
        when(answerMcSubmissionRepository.countByQuestionSubmission_QuestionSubmissionIdIn(anyList())).thenReturn(0L);
        when(submissionService.toDto(any(Submission.class), anyBoolean(), anyBoolean())).thenReturn(submissionDto);

        ResponseEntity<Object> retVal = assignmentService.launchAssignment(1L, securedInfo, false);

        assertEquals(HttpStatus.OK, retVal.getStatusCode());
        assertEquals(submissionDto, retVal.getBody());
        verify(integrationTokenService).create(submission, securedInfo);
        verify(integrationLaunchService).buildUrl(eq(submission), anyInt(), eq(integration));
        verify(caliperService).sendAssignmentRestarted(submission, securedInfo);
    }

    @Test
    public void testCheckAndRestoreAllAssignmentsInLms() throws Exception {
        when(platformDeploymentRepository.findAll()).thenReturn(List.of(platformDeployment));

        assignmentService.checkAndRestoreAllAssignmentsInLms();

        verify(assignmentRepository).findAssignmentsToCheckByPlatform(1L);
    }

    @Test
    public void testCheckAndRestoreAssignmentsInLmsRestoresMissingAssignment() throws Exception {
        when(assignmentRepository.findAssignmentsToCheckByPlatform(anyLong())).thenReturn(List.of(assignment));
        when(apiClient.restoreAssignment(any(Assignment.class))).thenReturn(lmsAssignment);

        assignmentService.checkAndRestoreAssignmentsInLms(1L);

        verify(apiClient).restoreAssignment(assignment);
    }

    @Test
    public void testCheckAndRestoreAssignmentsInLmsSkipsExistingAssignment() throws Exception {
        when(assignmentRepository.findAssignmentsToCheckByPlatform(anyLong())).thenReturn(List.of(assignment));
        when(apiClient.checkAssignmentExists(any(LtiUserEntity.class), anyString(), anyString())).thenReturn(Optional.of(lmsAssignment));

        assignmentService.checkAndRestoreAssignmentsInLms(1L);

        verify(apiClient, never()).restoreAssignment(any(Assignment.class));
    }

    @Test
    public void testCheckLmsAssignmentExistsNoToken() throws ApiException, NumberFormatException, TerracottaConnectorException {
        when(apiTokenRepository.findByUser(any(LtiUserEntity.class))).thenReturn(Optional.empty());

        assertTrue(assignmentService.checkLmsAssignmentExists(assignment, instructorUser));

        verify(apiClient, never()).checkAssignmentExists(any(LtiUserEntity.class), anyString(), anyString());
    }

    @Test
    public void testCheckLmsAssignmentExistsFound() throws ApiException, NumberFormatException, TerracottaConnectorException {
        when(apiClient.checkAssignmentExists(any(LtiUserEntity.class), anyString(), anyString())).thenReturn(Optional.of(lmsAssignment));

        assertTrue(assignmentService.checkLmsAssignmentExists(assignment, instructorUser));
    }

    @Test
    public void testCheckLmsAssignmentExistsNotFound() throws ApiException, NumberFormatException, TerracottaConnectorException {
        when(apiClient.checkAssignmentExists(any(LtiUserEntity.class), anyString(), anyString())).thenReturn(Optional.empty());

        assertFalse(assignmentService.checkLmsAssignmentExists(assignment, instructorUser));
    }

    @Test
    public void testRestoreAssignmentInLmsPublishedSendsGrades() throws Exception {
        when(apiClient.restoreAssignment(any(Assignment.class))).thenReturn(lmsAssignment);
        when(submissionRepository.findByAssessment_Treatment_Assignment_AssignmentId(anyLong())).thenReturn(Collections.emptyList());

        Assignment retVal = assignmentService.restoreAssignmentInLms(assignment);

        assertEquals(assignment, retVal);
        verify(submissionService).sendSubmissionGradesToLmsWithLti(Collections.emptyList(), false);
    }

    @Test
    public void testRestoreAssignmentInLmsNotPublishedSkipsGrades() throws Exception {
        when(assignment.isPublished()).thenReturn(false);
        when(apiClient.restoreAssignment(any(Assignment.class))).thenReturn(lmsAssignment);

        assignmentService.restoreAssignmentInLms(assignment);

        verify(submissionService, never()).sendSubmissionGradesToLmsWithLti(anyList(), anyBoolean());
    }

    @Test
    public void testRestoreAssignmentInLmsGradeSendExceptionIsCaught() throws Exception {
        when(apiClient.restoreAssignment(any(Assignment.class))).thenReturn(lmsAssignment);
        when(submissionRepository.findByAssessment_Treatment_Assignment_AssignmentId(anyLong())).thenReturn(Collections.emptyList());
        doThrow(new ApiException("lms error")).when(submissionService).sendSubmissionGradesToLmsWithLti(anyList(), anyBoolean());

        Assignment retVal = assignmentService.restoreAssignmentInLms(assignment);

        assertEquals(assignment, retVal);
    }

    @Test
    public void testGetAllAssignmentsForLmsCourse() throws ApiException, TerracottaConnectorException, DataServiceException {
        List<LmsAssignment> retVal = assignmentService.getAllAssignmentsForLmsCourse(securedInfo);

        assertNotNull(retVal);
        assertEquals(1, retVal.size());
    }

    @Test
    public void testGetAllAssignmentsForLmsCourseContextNotFound() {
        when(ltiContextRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(DataServiceException.class, () -> assignmentService.getAllAssignmentsForLmsCourse(securedInfo));
    }

    @Test
    public void testGetLmsAssignmentByIdFound() throws ApiException, TerracottaConnectorException {
        when(apiClient.listAssignment(eq(instructorUser), anyString(), anyString())).thenReturn(Optional.of(lmsAssignment));

        Optional<LmsAssignment> retVal = assignmentService.getLmsAssignmentById("lms-1", securedInfo);

        assertTrue(retVal.isPresent());
    }

    @Test
    public void testGetLmsAssignmentByIdNotFound() throws ApiException, TerracottaConnectorException {
        Optional<LmsAssignment> retVal = assignmentService.getLmsAssignmentById("lms-1", securedInfo);

        assertTrue(retVal.isEmpty());
    }

    @Test
    public void testBuildHeaders() {
        HttpHeaders headers = assignmentService.buildHeaders(UriComponentsBuilder.newInstance(), 1L, 2L, 3L);

        assertNotNull(headers.getLocation());
        assertTrue(headers.getLocation().toString().contains("/api/experiments/1/exposures/2/assignments/3"));
    }

    @Test
    public void testCreateAssignmentInLmsSuccess() throws AssignmentNotCreatedException, TerracottaConnectorException {
        Assignment retVal = assignmentService.createAssignmentInLms(instructorUser, assignment, 1L, "course-1");

        assertEquals(assignment, retVal);
        verify(assignment).setLmsAssignmentId("1");
    }

    @Test
    public void testCreateAssignmentInLmsApiException() throws ApiException, TerracottaConnectorException {
        when(apiClient.createLmsAssignment(any(LtiUserEntity.class), any(Assignment.class), anyString())).thenThrow(new ApiException("failed"));

        assertThrows(AssignmentNotCreatedException.class, () -> assignmentService.createAssignmentInLms(instructorUser, assignment, 1L, "course-1"));
    }

    @Test
    public void testEditAssignmentNameInLmsSuccess() throws AssignmentNotEditedException, ApiException, TerracottaConnectorException, IOException {
        assignmentService.editAssignmentNameInLms(assignment, "course-1", "New Name", instructorUser);

        verify(apiClient).editAssignmentNameInLms(assignment, "course-1", "New Name", instructorUser);
    }

    @Test
    public void testEditAssignmentNameInLmsException() throws ApiException, IOException, TerracottaConnectorException {
        doThrow(new ApiException("failed")).when(apiClient).editAssignmentNameInLms(any(Assignment.class), anyString(), anyString(), any(LtiUserEntity.class));

        assertThrows(AssignmentNotEditedException.class, () -> assignmentService.editAssignmentNameInLms(assignment, "course-1", "New Name", instructorUser));
    }

    @Test
    public void testDeleteAssignmentInLmsSuccess() throws AssignmentNotEditedException, ApiException, TerracottaConnectorException, IOException {
        assignmentService.deleteAssignmentInLms(assignment, "course-1", instructorUser);

        verify(apiClient).deleteAssignmentInLms(assignment, "course-1", instructorUser);
    }

    @Test
    public void testDeleteAssignmentInLmsException() throws ApiException, IOException, TerracottaConnectorException {
        doThrow(new ApiException("failed")).when(apiClient).deleteAssignmentInLms(any(Assignment.class), anyString(), any(LtiUserEntity.class));

        assertThrows(AssignmentNotEditedException.class, () -> assignmentService.deleteAssignmentInLms(assignment, "course-1", instructorUser));
    }

    @Test
    public void testDeleteAllFromExperimentNoAssignments() throws TerracottaConnectorException, ApiException, IOException {
        when(assignmentRepository.findByExposure_Experiment_ExperimentId(anyLong())).thenReturn(Collections.emptyList());

        assignmentService.deleteAllFromExperiment(1L, securedInfo);

        verify(apiClient, never()).deleteAssignmentInLms(any(Assignment.class), anyString(), any(LtiUserEntity.class));
    }

    @Test
    public void testDeleteAllFromExperimentLmsErrorIsCaught() throws ApiException, IOException, TerracottaConnectorException {
        doThrow(new ApiException("failed")).when(apiClient).deleteAssignmentInLms(any(Assignment.class), anyString(), any(LtiUserEntity.class));

        assignmentService.deleteAllFromExperiment(1L, securedInfo);

        verify(apiClient).deleteAssignmentInLms(any(Assignment.class), anyString(), any(LtiUserEntity.class));
    }

    @Test
    public void testIsSingleVersionByIdTrue() {
        assertTrue(assignmentService.isSingleVersion(1L));
    }

    @Test
    public void testIsSingleVersionTrueWhenOneTreatment() {
        when(treatmentRepository.findByAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(anyLong())).thenReturn(List.of(treatment));

        assertTrue(assignmentService.isSingleVersion(assignment));
    }

    @Test
    public void testIsSingleVersionFalseWhenMultipleTreatments() {
        when(treatmentRepository.findByAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(anyLong())).thenReturn(List.of(treatment, treatment));

        assertFalse(assignmentService.isSingleVersion(assignment));
    }

    @Test
    public void testIsSingleVersionNullAssignmentThrows() {
        assertThrows(IllegalArgumentException.class, () -> assignmentService.isSingleVersion((Assignment) null));
    }

}
