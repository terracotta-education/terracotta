package edu.iu.terracotta.service.app.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.Optional;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.repository.AssignmentRepository;
import edu.iu.terracotta.repository.SubmissionRepository;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;

import javax.persistence.EntityManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.dto.AssignmentDto;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.repository.PlatformDeploymentRepository;
import edu.iu.terracotta.repository.TreatmentRepository;
import edu.iu.terracotta.utils.TextConstants;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AssignmentServiceImplTest {

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    @Mock
    private AllRepositories allRepositories;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private PlatformDeploymentRepository platformDeploymentRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private PlatformDeployment platformDeployment;

    @Mock
    private AssignmentExtended assignmentExtended;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private CanvasAPIClient canvasAPIClient;

    @Mock
    private Exposure exposure;

    @Mock
    private Experiment experiment;

    private Assignment assignment;
    private Assignment newAssignment;
    private Date dueDate = new Date();
    private Method verifyAssignmentSubmissionLimit;
    private Method verifySubmissionWaitTime;

    @BeforeEach
    public void beforeEach() throws NoSuchMethodException, SecurityException, DataServiceException, AssessmentNotMatchingException, CanvasApiException {
        MockitoAnnotations.openMocks(this);

        clearInvocations(assignmentRepository);
        verifyAssignmentSubmissionLimit = AssignmentServiceImpl.class.getDeclaredMethod("verifyAssignmentSubmissionLimit", Integer.class, int.class);
        verifyAssignmentSubmissionLimit.setAccessible(true);
        verifySubmissionWaitTime = AssignmentServiceImpl.class.getDeclaredMethod("verifySubmissionWaitTime", Float.class, List.class);
        verifySubmissionWaitTime.setAccessible(true);

        allRepositories.assignmentRepository = assignmentRepository;
        allRepositories.platformDeploymentRepository = platformDeploymentRepository;
        allRepositories.submissionRepository = submissionRepository;
        allRepositories.treatmentRepository = treatmentRepository;

        assignment = new Assignment();
        assignment.setAssignmentId(1L);
        assignment.setLmsAssignmentId("1");
        assignment.setExposure(exposure);

        newAssignment = new Assignment();
        newAssignment.setAssignmentId(2L);
        newAssignment.setLmsAssignmentId("1");
        newAssignment.setExposure(exposure);

        when(allRepositories.assignmentRepository.getOne(anyLong())).thenReturn(assignment);
        when(allRepositories.submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(anyLong())).thenReturn(0L);
        when(canvasAPIClient.listAssignment(anyString(), anyInt(), any(PlatformDeployment.class))).thenReturn(Optional.empty());
        when(exposure.getExperiment()).thenReturn(experiment);
        when(experiment.getPlatformDeployment()).thenReturn(new PlatformDeployment());
        when(allRepositories.assignmentRepository.findByAssignmentId(anyLong())).thenReturn(assignment);
        when(allRepositories.assignmentRepository.findByExposure_ExposureIdAndSoftDeleted(anyLong(), anyBoolean())).thenReturn(Collections.singletonList(assignment));
        when(allRepositories.assignmentRepository.save(any(Assignment.class))).thenReturn(newAssignment);
        when(allRepositories.platformDeploymentRepository.getOne(anyLong())).thenReturn(platformDeployment);
        when(allRepositories.treatmentRepository.findByAssignment_AssignmentId(anyLong())).thenReturn(Collections.emptyList());
        when(canvasAPIClient.listAssignment(anyString(), anyInt(), any(PlatformDeployment.class))).thenReturn(Optional.of(assignmentExtended));
        when(exposure.getExposureId()).thenReturn(1L);
        when(assignmentExtended.isPublished()).thenReturn(true);
        when(assignmentExtended.getDueAt()).thenReturn(dueDate);
    }

    @Test
    public void duplicateAssignmentTest() throws DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
                                                AssignmentNotCreatedException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, NumberFormatException, CanvasApiException {
        AssignmentDto assignmentDto = assignmentService.duplicateAssignment(0L, "0", 0l);

        assertNotNull(assignmentDto);
        assertEquals(2L, assignmentDto.getAssignmentId());
        assertNull(assignment.getAssignmentId());
    }

    @Test
    public void testDuplicateAssessmentNotFound() throws IdInPostException, AssessmentNotMatchingException {
        when(allRepositories.assignmentRepository.findByAssignmentId(anyLong())).thenReturn(null);

        Exception exception = assertThrows(DataServiceException.class, () -> { assignmentService.duplicateAssignment(1L, "0", 0l); });

        assertEquals("The assignment with the given ID does not exist", exception.getMessage());
    }

    @Test
    public void testGetAssignments() throws AssessmentNotMatchingException, CanvasApiException {
        List<AssignmentDto> assignmentDtos = assignmentService.getAssignments(0L, "0", 0l, false, false);

        assertNotNull(assignmentDtos);
        assertEquals(1, assignmentDtos.size());
        assertTrue(assignmentDtos.get(0).isPublished());
        assertEquals(dueDate, assignmentDtos.get(0).getDueDate());
    }

    @Test
    public void testGetAssignmentsNoCanvasAssignmentFound() throws AssessmentNotMatchingException, CanvasApiException {
        when(canvasAPIClient.listAssignment(anyString(), anyInt(), any(PlatformDeployment.class))).thenReturn(Optional.empty());
        List<AssignmentDto> assignmentDtos = assignmentService.getAssignments(0L, "0", 0l, false, false);

        assertNotNull(assignmentDtos);
        assertEquals(1, assignmentDtos.size());
        assertFalse(assignmentDtos.get(0).isPublished());
        assertNull(assignmentDtos.get(0).getDueDate());
    }

    @Test
    public void testGetAssignmentsNoAssignmentsFound() throws AssessmentNotMatchingException, CanvasApiException {
        when(allRepositories.assignmentRepository.findByExposure_ExposureIdAndSoftDeleted(anyLong(), anyBoolean())).thenReturn(Collections.emptyList());
        List<AssignmentDto> assignmentDtos = assignmentService.getAssignments(0L, "0", 0l, false, false);

        assertNotNull(assignmentDtos);
        assertEquals(0, assignmentDtos.size());
    }

    public void testVerifyNumSubmissionsLimitNull() {
        assertDoesNotThrow(() -> verifyAssignmentSubmissionLimit.invoke(assignmentService, null, 1));
    }

    @Test
    public void testVerifyNumSubmissionsLimitZero() {
        assertDoesNotThrow(() -> verifyAssignmentSubmissionLimit.invoke(assignmentService, 0, 1));
    }

    @Test
    public void testVerifyNumSubmissionsLessThanLimit() {
        assertDoesNotThrow(() -> verifyAssignmentSubmissionLimit.invoke(assignmentService, 2, 1));
    }

    @Test
    public void testVerifyNumSubmissionsGreaterThanLimit() {
        InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> verifyAssignmentSubmissionLimit.invoke(assignmentService, 1, 2));
        assertTrue(e.getCause() instanceof AssignmentAttemptException);
        assertEquals(TextConstants.LIMIT_OF_SUBMISSIONS_REACHED, e.getCause().getMessage());
    }

    @Test
    public void testVerifySubmissionWaitTimeNull() {
        assertDoesNotThrow(() -> verifySubmissionWaitTime.invoke(assignmentService, null, Collections.emptyList()));
    }

    @Test
    public void testVerifySubmissionWaitTimeZero() {
        assertDoesNotThrow(() -> verifySubmissionWaitTime.invoke(assignmentService, 0F, Collections.emptyList()));
    }

    @Test
    public void testVerifySubmissionWaitTimeAllowed() {
        Submission submission = new Submission();
        submission.setDateSubmitted(Timestamp.from(Instant.now().minus(30, ChronoUnit.MINUTES)));

        assertDoesNotThrow(() -> verifySubmissionWaitTime.invoke(assignmentService, .1F, Collections.singletonList(submission)));
    }

    @Test
    public void testVerifySubmissionWaitTimeNotAllowed() {
        Submission submission = new Submission();
        submission.setDateSubmitted(Timestamp.from(Instant.now().minus(30, ChronoUnit.MINUTES)));

        InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> verifySubmissionWaitTime.invoke(assignmentService, 1F, Collections.singletonList(submission)));
        assertTrue(e.getCause() instanceof AssignmentAttemptException);
        assertEquals(TextConstants.ASSIGNMENT_SUBMISSION_WAIT_TIME_NOT_REACHED, e.getCause().getMessage());
    }

    @Test
    public void testDeleteAssignmentHard() throws EmptyResultDataAccessException, CanvasApiException, AssignmentNotEditedException {
        assignmentService.deleteById(1L, "canvasId");

        verify(allRepositories.assignmentRepository).deleteByAssignmentId(anyLong());
        verify(allRepositories.assignmentRepository, never()).saveAndFlush(any(Assignment.class));
    }

    @Test
    public void testDeleteAssignmentSoft() throws EmptyResultDataAccessException, CanvasApiException, AssignmentNotEditedException {
        when(allRepositories.submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(anyLong())).thenReturn(1L);

        assignmentService.deleteById(1L, "canvasId");

        verify(allRepositories.assignmentRepository, never()).deleteByAssignmentId(anyLong());
        verify(allRepositories.assignmentRepository).saveAndFlush(any(Assignment.class));
    }

}
