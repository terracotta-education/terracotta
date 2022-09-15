package edu.iu.terracotta.service.app.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;

import java.util.Collections;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.dto.AssignmentDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.repository.AssignmentRepository;
import edu.iu.terracotta.repository.SubmissionRepository;
import edu.iu.terracotta.repository.TreatmentRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

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
    private TreatmentRepository treatmentRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Exposure exposure;

    private Assignment assignment;
    private Assignment newAssignment;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        allRepositories.assignmentRepository = assignmentRepository;
        allRepositories.submissionRepository = submissionRepository;
        allRepositories.treatmentRepository = treatmentRepository;

        assignment = new Assignment();
        assignment.setAssignmentId(1L);

        newAssignment = new Assignment();
        newAssignment.setAssignmentId(2L);
        newAssignment.setExposure(exposure);

        when(allRepositories.assignmentRepository.findByAssignmentId(anyLong())).thenReturn(assignment);
        when(allRepositories.assignmentRepository.save(any(Assignment.class))).thenReturn(newAssignment);
        when(allRepositories.submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(anyLong())).thenReturn(0L);
        when(allRepositories.treatmentRepository.findByAssignment_AssignmentId(anyLong())).thenReturn(Collections.emptyList());
        when(exposure.getExposureId()).thenReturn(1L);
    }

    @Test
    public void duplicateAssignmentTest() throws DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
                                                AssignmentNotCreatedException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException {
        AssignmentDto assignmentDto = assignmentService.duplicateAssignment(0L);

        assertNotNull(assignmentDto);
        assertEquals(2L, assignmentDto.getAssignmentId());
        assertNull(assignment.getAssignmentId());
    }

    @Test
    public void testDuplicateAssessmentNotFound() throws IdInPostException, AssessmentNotMatchingException {
        when(allRepositories.assignmentRepository.findByAssignmentId(anyLong())).thenReturn(null);

        Exception exception = assertThrows(DataServiceException.class, () -> { assignmentService.duplicateAssignment(1L); });

        assertEquals("The assignment with the given ID does not exist", exception.getMessage());
    }

}
