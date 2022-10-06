package edu.iu.terracotta.service.app.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.Optional;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.repository.AssessmentRepository;
import edu.iu.terracotta.repository.AssignmentRepository;
import edu.iu.terracotta.repository.ExposureRepository;
import edu.iu.terracotta.repository.SubmissionRepository;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.ExposureService;
import edu.iu.terracotta.service.app.TreatmentService;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;

import javax.persistence.EntityManager;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentMoveException;
import edu.iu.terracotta.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.AssignmentDto;
import edu.iu.terracotta.model.app.dto.TreatmentDto;
import edu.iu.terracotta.model.app.enumerator.MultipleSubmissionScoringScheme;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.PlatformDeploymentRepository;
import edu.iu.terracotta.repository.TreatmentRepository;
import edu.iu.terracotta.utils.TextConstants;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AssignmentServiceImplTest {

    @Spy
    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    @Mock private AllRepositories allRepositories;
    @Mock private AssessmentRepository assessmentRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private ExposureRepository exposureRepository;
    @Mock private PlatformDeploymentRepository platformDeploymentRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private TreatmentRepository treatmentRepository;

    @Mock private AssessmentService assessmentService;
    @Mock private CanvasAPIClient canvasAPIClient;
    @Mock ExposureService exposureService;
    @Mock private TreatmentService treatmentService;

    @Mock private Assessment assessment;
    @Mock private Assignment assignment;
    @Mock private AssignmentDto assignmentDto;
    @Mock private AssignmentExtended assignmentExtended;
    @Mock private EntityManager entityManager;
    @Mock private Experiment experiment;
    @Mock private Exposure exposure;
    @Mock private PlatformDeployment platformDeployment;
    @Mock private Treatment treatment;
    @Mock private TreatmentDto treatmentDto;

    private Date dueDate = new Date();

    @BeforeEach
    public void beforeEach() throws NoSuchMethodException, SecurityException, DataServiceException, AssessmentNotMatchingException, CanvasApiException, NumberFormatException, IdInPostException, ExceedingLimitException, TreatmentNotMatchingException, AssignmentNotCreatedException, AssignmentAttemptException {
        MockitoAnnotations.openMocks(this);

        clearInvocations(assignmentRepository, canvasAPIClient);

        allRepositories.assessmentRepository = assessmentRepository;
        allRepositories.assignmentRepository = assignmentRepository;
        allRepositories.exposureRepository = exposureRepository;
        allRepositories.platformDeploymentRepository = platformDeploymentRepository;
        allRepositories.submissionRepository = submissionRepository;
        allRepositories.treatmentRepository = treatmentRepository;

        when(assignmentRepository.getOne(anyLong())).thenReturn(assignment);
        when(assessmentRepository.findByTreatment_Assignment_AssignmentId(anyLong())).thenReturn(Collections.singletonList(assessment));
        when(assignmentRepository.findByAssignmentId(anyLong())).thenReturn(assignment);
        when(assignmentRepository.findByExposure_ExposureIdAndSoftDeleted(anyLong(), anyBoolean())).thenReturn(Collections.singletonList(assignment));
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(assignment);
        when(exposureRepository.findById(anyLong())).thenReturn(Optional.of(exposure));
        when(platformDeploymentRepository.getOne(anyLong())).thenReturn(platformDeployment);
        when(submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(anyLong())).thenReturn(0L);
        when(treatmentRepository.findByAssignment_AssignmentId(anyLong())).thenReturn(Collections.emptyList());

        when(assessmentService.getAssessmentForParticipant(any(Participant.class), any(SecuredInfo.class))).thenReturn(assessment);
        doNothing().when(assessmentService).verifySubmissionLimit(anyInt(), anyInt());
        doNothing().when(assessmentService).verifySubmissionWaitTime(anyFloat(), anyList());
        when(canvasAPIClient.listAssignment(anyString(), anyInt(), any(PlatformDeployment.class))).thenReturn(Optional.empty());
        when(exposureService.getExposure(anyLong())).thenReturn(exposure);
        when(treatmentService.duplicateTreatment(anyLong(), any(Assignment.class), anyString(), anyLong())).thenReturn(treatmentDto);

        when(assignment.getAssignmentId()).thenReturn(1l);
        when(assignment.getDueDate()).thenReturn(dueDate);
        when(assignment.getExposure()).thenReturn(exposure);
        when(assignment.getLmsAssignmentId()).thenReturn("1");
        when(assignment.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.MOST_RECENT);
        when(assignment.isPublished()).thenReturn(true);
        when(assignmentDto.getExposureId()).thenReturn(1L);
        when(assignmentDto.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.MOST_RECENT.toString());
        when(assignmentExtended.isPublished()).thenReturn(true);
        when(assignmentExtended.getDueAt()).thenReturn(dueDate);
        when(experiment.getPlatformDeployment()).thenReturn(new PlatformDeployment());
        when(exposure.getExperiment()).thenReturn(experiment);
        when(exposure.getExposureId()).thenReturn(1L);

        doNothing().when(assignmentService).createAssignmentInCanvas(any(Assignment.class), anyLong(), anyString());
    }

    @Test
    public void duplicateAssignmentTest() throws DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
                                                AssignmentNotCreatedException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, NumberFormatException, CanvasApiException, ExceedingLimitException, TreatmentNotMatchingException {
        AssignmentDto assignmentDto = assignmentService.duplicateAssignment(0L, "0", 0l);

        assertNotNull(assignmentDto);
        verify(assignmentRepository).save(any(Assignment.class));
    }

    @Test
    public void duplicateAssignmentTestWithTreatments() throws DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
                                                AssignmentNotCreatedException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, NumberFormatException, CanvasApiException, ExceedingLimitException, TreatmentNotMatchingException {
        when(treatmentRepository.findByAssignment_AssignmentId(anyLong())).thenReturn(Collections.singletonList(treatment));
        AssignmentDto assignmentDto = assignmentService.duplicateAssignment(0L, "0", 0l);

        assertNotNull(assignmentDto);
        assertEquals(1l, assignmentDto.getAssignmentId());
        assertEquals(1, assignmentDto.getTreatments().size());
    }

    @Test
    public void testDuplicateAssessmentNotFound() throws IdInPostException, AssessmentNotMatchingException {
        when(assignmentRepository.findByAssignmentId(anyLong())).thenReturn(null);

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
        assertTrue(assignmentDtos.get(0).isPublished());
    }

    @Test
    public void testGetAssignmentsNoAssignmentsFound() throws AssessmentNotMatchingException, CanvasApiException {
        when(assignmentRepository.findByExposure_ExposureIdAndSoftDeleted(anyLong(), anyBoolean())).thenReturn(Collections.emptyList());
        List<AssignmentDto> assignmentDtos = assignmentService.getAssignments(0L, "0", 0l, false, false);

        assertNotNull(assignmentDtos);
        assertEquals(0, assignmentDtos.size());
    }

    @Test
    public void testDeleteAssignmentHard() throws EmptyResultDataAccessException, CanvasApiException, AssignmentNotEditedException {
        assignmentService.deleteById(1L, "canvasId");

        verify(assignmentRepository).deleteByAssignmentId(anyLong());
        verify(assignmentRepository, never()).saveAndFlush(any(Assignment.class));
    }

    @Test
    public void testDeleteAssignmentSoft() throws EmptyResultDataAccessException, CanvasApiException, AssignmentNotEditedException {
        when(submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(anyLong())).thenReturn(1L);

        assignmentService.deleteById(1L, "canvasId");

        verify(assignmentRepository, never()).deleteByAssignmentId(anyLong());
        verify(assignmentRepository).saveAndFlush(any(Assignment.class));
    }

    @Test
    public void testMoveAssignment() throws NumberFormatException, DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
            AssignmentNotCreatedException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, CanvasApiException,
            ExceedingLimitException, TreatmentNotMatchingException, ExposureNotMatchingException, AssignmentMoveException, AssignmentNotEditedException {
        when(assignmentDto.getAssignmentId()).thenReturn(null);

        AssignmentDto newAssignmentDto = assignmentService.moveAssignment(2l, assignmentDto, 1L, 2l, "1", 1L);

        assertNotNull(newAssignmentDto);
        verify(assignmentRepository).save(any(Assignment.class));
        verify(assignmentRepository).saveAndFlush(any(Assignment.class));
    }

    @Test
    public void testMoveAssignmentExposuresMatch() throws IdInPostException, AssessmentNotMatchingException {
        Exception exception = assertThrows(AssignmentMoveException.class, () -> { assignmentService.moveAssignment(2l, assignmentDto, 1L, 1l, "1", 1L); });

        assertEquals(TextConstants.UNABLE_TO_MOVE_ASSIGNMENT_EXPOSURE_SAME, exception.getMessage());
    }

    @Test
    public void testMoveAssignmentNoTargetExposureMatch() throws IdInPostException, AssessmentNotMatchingException {
        when(exposureService.getExposure(anyLong())).thenReturn(null);
        Exception exception = assertThrows(ExposureNotMatchingException.class, () -> { assignmentService.moveAssignment(2l, assignmentDto, 1L, 2l, "1", 1L); });

        assertEquals(TextConstants.EXPOSURE_NOT_MATCHING, exception.getMessage());
    }

}
