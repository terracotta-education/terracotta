package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.LmsUserBatchStatusDto;
import edu.iu.terracotta.dao.model.dto.StepDto;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentLockedException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.utils.TextConstants;

public class StepsControllerTest extends BaseTest {

    private StepsController stepsController;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        // Constructed manually rather than via @InjectMocks: ApiJwtService is also implemented by the
        // inherited canvasApiJwtService mock (see the ambiguity warning in BaseServiceTest), so
        // constructor-injection-by-type could silently wire the wrong ApiJwtService mock.
        stepsController = new StepsController(exposureService, participantService, participantAsyncService, groupService, submissionService, assessmentService, assignmentService, questionSubmissionService, apiJwtService);

        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
    }

    private StepDto stepDto(String step) {
        return StepDto.builder().step(step).build();
    }

    private StepDto stepDto(String step, Map<String, String> parameters) {
        return StepDto.builder().step(step).parameters(parameters).build();
    }

    @Test
    void exposureTypeHappyPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);

        ResponseEntity<Object> response = stepsController.postStep(1L, false, stepDto(StepsController.EXPOSURE_TYPE), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(exposureService).createExposures(1L);
    }

    @Test
    void exposureTypePermissionDeniedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Object> response = stepsController.postStep(1L, false, stepDto(StepsController.EXPOSURE_TYPE), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
        verify(exposureService, never()).createExposures(anyLong());
    }

    @Test
    void participationTypeHappyPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        UUID batchId = UUID.randomUUID();
        LmsUserBatchStatusDto lmsUserBatchStatusDto = LmsUserBatchStatusDto.builder().batchId(batchId).status(LmsUserBatchStatus.IN_PROGRESS).build();
        when(participantService.startPrepareParticipation(1L)).thenReturn(lmsUserBatchStatusDto);

        ResponseEntity<Object> response = stepsController.postStep(1L, false, stepDto(StepsController.PARTICIPATION_TYPE), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(lmsUserBatchStatusDto, response.getBody());
        verify(participantAsyncService).prepareParticipationAsync(1L, securedInfo, batchId);
        verify(participantService, never()).prepareParticipation(anyLong(), any());
    }

    @Test
    void participationTypePermissionDeniedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Object> response = stepsController.postStep(1L, false, stepDto(StepsController.PARTICIPATION_TYPE), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(participantAsyncService, never()).prepareParticipationAsync(anyLong(), any(), any());
    }

    @Test
    void getStepStatusReturnsStatusWhenFound() throws Exception {
        UUID batchId = UUID.randomUUID();
        LmsUserBatchStatusDto lmsUserBatchStatusDto = LmsUserBatchStatusDto.builder().batchId(batchId).status(LmsUserBatchStatus.COMPLETED).build();
        when(participantService.getPrepareParticipationStatus(batchId)).thenReturn(Optional.of(lmsUserBatchStatusDto));

        ResponseEntity<Object> response = stepsController.getStepStatus(1L, batchId, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(lmsUserBatchStatusDto, response.getBody());
    }

    @Test
    void getStepStatusReturnsNotFoundWhenMissing() throws Exception {
        UUID batchId = UUID.randomUUID();
        when(participantService.getPrepareParticipationStatus(batchId)).thenReturn(Optional.empty());

        ResponseEntity<Object> response = stepsController.getStepStatus(1L, batchId, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void distributionTypeHappyPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);

        ResponseEntity<Object> response = stepsController.postStep(1L, false, stepDto(StepsController.DISTRIBUTION_TYPE), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(groupService).createAndAssignGroupsToConditionsAndExposures(1L, securedInfo, false);
    }

    @Test
    void distributionTypePermissionDeniedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Object> response = stepsController.postStep(1L, false, stepDto(StepsController.DISTRIBUTION_TYPE), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void studentSubmissionParametersNullTest() throws Exception {
        ResponseEntity<Object> response = stepsController.postStep(1L, false, stepDto(StepsController.STUDENT_SUBMISSION), httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(TextConstants.SUBMISSION_IDS_MISSING, response.getBody());
    }

    @Test
    void studentSubmissionEmptyIdsTest() throws Exception {
        ResponseEntity<Object> response = stepsController.postStep(
            1L,
            false,
            stepDto(StepsController.STUDENT_SUBMISSION, Map.of("submissionIds", "")),
            httpServletRequest
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void studentSubmissionLearnerHappyPathTest() throws Exception {
        when(apiJwtService.isLearner(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Object> response = stepsController.postStep(
            1L,
            true,
            stepDto(StepsController.STUDENT_SUBMISSION, Map.of("submissionIds", "5")),
            httpServletRequest
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(questionSubmissionService).canSubmit(securedInfo, 1L, true);
        verify(submissionService).allowedSubmission(5L, securedInfo);
        verify(submissionService).finalizeAndGrade(5L, securedInfo, true);
    }

    @Test
    void studentSubmissionLearnerTooManyIdsTest() throws Exception {
        when(apiJwtService.isLearner(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Object> response = stepsController.postStep(
            1L,
            false,
            stepDto(StepsController.STUDENT_SUBMISSION, Map.of("submissionIds", "5,6")),
            httpServletRequest
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(TextConstants.SUBMISSION_IDS_MISSING, response.getBody());
    }

    @Test
    void studentSubmissionInstructorHappyPathTest() throws Exception {
        when(apiJwtService.isLearner(securedInfo)).thenReturn(false);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);

        ResponseEntity<Object> response = stepsController.postStep(
            1L,
            false,
            stepDto(StepsController.STUDENT_SUBMISSION, Map.of("submissionIds", "5,6")),
            httpServletRequest
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(submissionService).finalizeAndGrade(5L, securedInfo, false);
        verify(submissionService).finalizeAndGrade(6L, securedInfo, false);
    }

    @Test
    void studentSubmissionPermissionDeniedTest() throws Exception {
        when(apiJwtService.isLearner(securedInfo)).thenReturn(false);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Object> response = stepsController.postStep(
            1L,
            false,
            stepDto(StepsController.STUDENT_SUBMISSION, Map.of("submissionIds", "5")),
            httpServletRequest
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void studentSubmissionAssignmentAttemptExceptionCaughtTest() throws Exception {
        when(apiJwtService.isLearner(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        doThrow(new AssignmentAttemptException("no attempts left")).when(questionSubmissionService).canSubmit(securedInfo, 1L, false);

        ResponseEntity<Object> response = stepsController.postStep(
            1L,
            false,
            stepDto(StepsController.STUDENT_SUBMISSION, Map.of("submissionIds", "5")),
            httpServletRequest
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("no attempts left", response.getBody());
    }

    @Test
    void studentSubmissionAssignmentLockedExceptionCaughtTest() throws Exception {
        when(apiJwtService.isLearner(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        doThrow(new AssignmentLockedException("locked")).when(questionSubmissionService).canSubmit(securedInfo, 1L, false);

        ResponseEntity<Object> response = stepsController.postStep(
            1L,
            false,
            stepDto(StepsController.STUDENT_SUBMISSION, Map.of("submissionIds", "5")),
            httpServletRequest
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("locked", response.getBody());
    }

    @Test
    void postAssignmentParametersNullTest() throws Exception {
        ResponseEntity<Object> response = stepsController.postStep(1L, false, stepDto(StepsController.POST_ASSIGNMENT), httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void postAssignmentEmptyIdsTest() throws Exception {
        ResponseEntity<Object> response = stepsController.postStep(
            1L,
            false,
            stepDto(StepsController.POST_ASSIGNMENT, Map.of("assignmentIds", "")),
            httpServletRequest
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void postAssignmentPermissionDeniedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Object> response = stepsController.postStep(
            1L,
            false,
            stepDto(StepsController.POST_ASSIGNMENT, Map.of("assignmentIds", "9")),
            httpServletRequest
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void postAssignmentNotFoundTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(assignmentService.findById(9L)).thenReturn(Optional.empty());

        ResponseEntity<Object> response = stepsController.postStep(
            1L,
            false,
            stepDto(StepsController.POST_ASSIGNMENT, Map.of("assignmentIds", "9")),
            httpServletRequest
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().toString().contains(TextConstants.ASSIGNMENT_NOT_MATCHING));
    }

    @Test
    void postAssignmentHappyPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(assignmentService.findById(9L)).thenReturn(Optional.of(assignment));

        ResponseEntity<Object> response = stepsController.postStep(
            1L,
            false,
            stepDto(StepsController.POST_ASSIGNMENT, Map.of("assignmentIds", "9")),
            httpServletRequest
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(assignmentService, times(1)).sendAssignmentGradeToLms(assignment);
    }

    @Test
    void launchAssignmentPermissionDeniedTest() throws Exception {
        when(apiJwtService.isLearner(securedInfo)).thenReturn(false);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Object> response = stepsController.postStep(1L, false, stepDto(StepsController.LAUNCH_ASSIGNMENT), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void launchAssignmentHappyPathTest() throws Exception {
        when(apiJwtService.isLearner(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        ResponseEntity<Object> launched = new ResponseEntity<>("launch-payload", HttpStatus.OK);
        when(assignmentService.launchAssignment(1L, securedInfo)).thenReturn(launched);

        ResponseEntity<Object> response = stepsController.postStep(1L, false, stepDto(StepsController.LAUNCH_ASSIGNMENT), httpServletRequest);

        assertEquals(launched, response);
    }

    @Test
    void launchAssignmentExceptionCaughtTest() throws Exception {
        when(apiJwtService.isLearner(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        doThrow(new AssignmentAttemptException("blocked")).when(questionSubmissionService).canSubmit(securedInfo, 1L, false);

        ResponseEntity<Object> response = stepsController.postStep(1L, false, stepDto(StepsController.LAUNCH_ASSIGNMENT), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("blocked", response.getBody());
    }

    @Test
    void launchConsentAssignmentHappyPathExistingParticipantTest() throws Exception {
        when(apiJwtService.isLearner(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        when(participantService.getParticipants(1L, USER_ID, true, securedInfo, false)).thenReturn(List.of(participantDto));

        ResponseEntity<Object> response = stepsController.postStep(1L, false, stepDto(StepsController.LAUNCH_CONSENT_ASSIGNMENT), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(participantDto, response.getBody());
        verify(participantService, never()).ensureParticipantExists(anyLong(), any());
        verify(participantService, never()).refreshParticipants(1L);
    }

    @Test
    void launchConsentAssignmentCreatesParticipantWhenEmptyTest() throws Exception {
        when(apiJwtService.isLearner(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        when(participantService.getParticipants(1L, USER_ID, true, securedInfo, false))
            .thenReturn(Collections.emptyList(), List.of(participantDto));

        ResponseEntity<Object> response = stepsController.postStep(1L, false, stepDto(StepsController.LAUNCH_CONSENT_ASSIGNMENT), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(participantDto, response.getBody());
        verify(participantService, times(1)).ensureParticipantExists(1L, securedInfo);
        verify(participantService, never()).refreshParticipants(1L);
    }

    @Test
    void launchConsentAssignmentPermissionDeniedTest() throws Exception {
        when(apiJwtService.isLearner(securedInfo)).thenReturn(false);

        ResponseEntity<Object> response = stepsController.postStep(1L, false, stepDto(StepsController.LAUNCH_CONSENT_ASSIGNMENT), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void viewAssignmentPermissionDeniedTest() throws Exception {
        when(apiJwtService.isLearner(securedInfo)).thenReturn(false);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Object> response = stepsController.postStep(1L, false, stepDto(StepsController.VIEW_ASSIGNMENT), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void viewAssignmentHappyPathTest() throws Exception {
        when(apiJwtService.isLearner(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        when(assessmentService.viewAssessment(1L, securedInfo)).thenReturn(assessmentDto);

        ResponseEntity<Object> response = stepsController.postStep(1L, false, stepDto(StepsController.VIEW_ASSIGNMENT), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(assessmentDto, response.getBody());
    }

    @Test
    void viewAssignmentExceptionCaughtTest() throws Exception {
        when(apiJwtService.isLearner(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        doThrow(new AssessmentNotMatchingException("no assessment")).when(assessmentService).viewAssessment(1L, securedInfo);

        ResponseEntity<Object> response = stepsController.postStep(1L, false, stepDto(StepsController.VIEW_ASSIGNMENT), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("no assessment", response.getBody());
    }

    @Test
    void unknownStepReturnsBadRequestTest() throws Exception {
        ResponseEntity<Object> response = stepsController.postStep(1L, false, stepDto("not_a_real_step"), httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void propagatesBadTokenExceptionFromExperimentAllowedTest() throws Exception {
        doThrow(new BadTokenException("bad token")).when(apiJwtService).experimentAllowed(securedInfo, 1L);

        assertThrows(BadTokenException.class, () -> stepsController.postStep(1L, false, stepDto(StepsController.EXPOSURE_TYPE), httpServletRequest));
    }

}
