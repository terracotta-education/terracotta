package edu.iu.terracotta.service.app.impl;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

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

public class AssignmentServiceImplTest {

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    @Mock
    private AllRepositories allRepositories;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private CanvasAPIClient canvasAPIClient;

    @Mock
    private Exposure exposure;

    @Mock
    private Experiment experiment;

    private Assignment assignment;

    @BeforeEach
    public void beforeEach() throws DataServiceException, AssessmentNotMatchingException, CanvasApiException {
        MockitoAnnotations.openMocks(this);

        clearInvocations(assignmentRepository);
        allRepositories.assignmentRepository = assignmentRepository;
        allRepositories.submissionRepository = submissionRepository;

        assignment = new Assignment();
        assignment.setAssignmentId(1L);
        assignment.setLmsAssignmentId("1");
        assignment.setExposure(exposure);

        when(allRepositories.assignmentRepository.getOne(anyLong())).thenReturn(assignment);
        when(allRepositories.submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(anyLong())).thenReturn(0L);
        when(canvasAPIClient.listAssignment(anyString(), anyInt(), any(PlatformDeployment.class))).thenReturn(Optional.empty());
        when(exposure.getExperiment()).thenReturn(experiment);
        when(experiment.getPlatformDeployment()).thenReturn(new PlatformDeployment());
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