package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AssignmentDto;
import edu.iu.terracotta.dao.model.dto.TreatmentDto;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;

public class AssignmentTreatmentServiceImplTest extends BaseTest {

    private AssignmentTreatmentServiceImpl assignmentTreatmentService;

    @BeforeEach
    public void beforeEach() throws AssessmentNotMatchingException, AssignmentAttemptException, NumberFormatException, IdInPostException, DataServiceException, ExceedingLimitException, TreatmentNotMatchingException, QuestionNotMatchingException {
        MockitoAnnotations.openMocks(this);

        setup();

        // apiClient below also collides with CanvasApiClientImpl in BaseServiceTest (see the @InjectMocks
        // pitfall note there), so this class is constructed manually instead of relying on @InjectMocks,
        // which non-deterministically wired the wrong mock and left apiClient calls silently unstubbed.
        assignmentTreatmentService = new AssignmentTreatmentServiceImpl(
            assessmentRepository,
            ltiUserRepository,
            submissionRepository,
            treatmentRepository,
            assessmentService,
            apiClient
        );
        ReflectionTestUtils.setField(assignmentTreatmentService, "entityManager", entityManager);
    }

    @Test
    public void testDuplicateTreatmentNotFound() throws IdInPostException, ExceedingLimitException, AssessmentNotMatchingException {
        when(treatmentRepository.findByTreatmentId(anyLong())).thenReturn(null);

        Exception exception = assertThrows(DataServiceException.class, () -> { assignmentTreatmentService.duplicateTreatment(1L, assignment, securedInfo); });

        assertEquals("The treatment with the given ID does not exist", exception.getMessage());
    }

    @Test
    public void testToAssignmentDtoWithTreatment() throws AssessmentNotMatchingException {
        AssignmentDto retVal = assignmentTreatmentService.toAssignmentDto(assignment, false, true, securedInfo);

        assertNotNull(retVal);
        assertEquals(1, retVal.getTreatments().size());
    }

    @Test
    public void testDuplicateTreatmentTwoArgOverloadNotFound() {
        when(treatmentRepository.findByTreatmentId(anyLong())).thenReturn(null);

        Exception exception = assertThrows(DataServiceException.class, () -> { assignmentTreatmentService.duplicateTreatment(1L, securedInfo); });

        assertEquals("The treatment with the given ID does not exist", exception.getMessage());
    }

    @Test
    public void testDuplicateTreatmentTwoArgOverloadSuccessNoAssignmentNoAssessment()
            throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException,
                ApiException, TreatmentNotMatchingException, QuestionNotMatchingException,
                TerracottaConnectorException {
        TreatmentDto retVal = assignmentTreatmentService.duplicateTreatment(1L, securedInfo);

        assertNotNull(retVal);
        verify(treatment, never()).setAssignment(any(Assignment.class));
        verify(treatmentRepository, never()).saveAndFlush(any());
    }

    @Test
    public void testDuplicateTreatmentSuccessWithAssignmentAndExistingAssessment()
            throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException,
                ApiException, TreatmentNotMatchingException, QuestionNotMatchingException,
                TerracottaConnectorException {
        when(assessmentRepository.findByTreatment_TreatmentId(anyLong())).thenReturn(List.of(assessment));
        when(assessmentService.duplicateAssessment(anyLong(), any(Treatment.class), any(Assignment.class))).thenReturn(assessment);
        when(assessmentService.toDto(any(Assessment.class), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), any())).thenReturn(assessmentDto);

        TreatmentDto retVal = assignmentTreatmentService.duplicateTreatment(1L, assignment, securedInfo);

        assertNotNull(retVal);
        assertNotNull(retVal.getAssessmentDto());
        verify(treatment).setAssignment(assignment);
        verify(treatmentRepository).saveAndFlush(any(Treatment.class));
    }

    @Test
    public void testSetAssignmentDtoAttrsSingleAssignmentEmpty() throws NumberFormatException, ApiException, TerracottaConnectorException {
        when(apiClient.listAssignment(any(), anyString(), any(Assignment.class))).thenReturn(Optional.empty());

        assignmentTreatmentService.setAssignmentDtoAttrs(assignment, "1", ltiUserEntity);

        verify(assignment, never()).setPublished(anyBoolean());
    }

    @Test
    public void testSetAssignmentDtoAttrsSingleAssignmentSuccess() throws NumberFormatException, ApiException, TerracottaConnectorException {
        Date dueAt = new Date();
        when(apiClient.listAssignment(any(), anyString(), any(Assignment.class))).thenReturn(Optional.of(lmsAssignment));
        when(lmsAssignment.isPublished()).thenReturn(true);
        when(lmsAssignment.getDueAt()).thenReturn(dueAt);

        assignmentTreatmentService.setAssignmentDtoAttrs(assignment, "1", ltiUserEntity);

        verify(assignment).setPublished(true);
        verify(assignment).setDueDate(dueAt);
    }

    @Test
    public void testSetAssignmentDtoAttrsListEmpty() throws ApiException, TerracottaConnectorException {
        assignmentTreatmentService.setAssignmentDtoAttrs(Collections.emptyList(), ltiUserEntity);

        verify(apiClient, never()).listAssignments(any(LtiUserEntity.class), any(Experiment.class));
    }

    @Test
    public void testSetAssignmentDtoAttrsListMatchesAndSkipsUnmatched() throws ApiException, TerracottaConnectorException {
        // default: lmsAssignment.getId() == "1" == assignment.getLmsAssignmentId() -> matched
        Assignment unmatchedAssignment = mock(Assignment.class);
        when(unmatchedAssignment.getLmsAssignmentId()).thenReturn("does-not-exist");

        assignmentTreatmentService.setAssignmentDtoAttrs(List.of(assignment, unmatchedAssignment), ltiUserEntity);

        verify(assignment).setPublished(true);
        verify(unmatchedAssignment, never()).setPublished(anyBoolean());
    }

    @Test
    public void testToAssignmentDtoListSuccess() throws AssessmentNotMatchingException {
        List<AssignmentDto> retVal = assignmentTreatmentService.toAssignmentDto(List.of(assignment), false, false, securedInfo);

        assertEquals(1, retVal.size());
    }

    @Test
    public void testToAssignmentDtoListSwallowsException() throws AssessmentNotMatchingException {
        when(assessmentService.toDto(any(Assessment.class), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), any())).thenThrow(new AssessmentNotMatchingException("nope"));

        List<AssignmentDto> retVal = assignmentTreatmentService.toAssignmentDto(List.of(assignment), false, true, securedInfo);

        assertTrue(retVal.isEmpty());
    }
}
