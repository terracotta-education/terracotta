package edu.iu.terracotta.service.app.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExposureNotMatchingException;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AssignmentServiceImplTest extends BaseTest {

    @Spy
    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    @Mock private LtiUserEntity instructorUser;

    private Date dueDate = new Date();

    @BeforeEach
    public void beforeEach() throws AssessmentNotMatchingException, AssignmentAttemptException, NumberFormatException, IdInPostException, DataServiceException, ExceedingLimitException, TreatmentNotMatchingException, QuestionNotMatchingException, ApiException, TerracottaConnectorException {
        MockitoAnnotations.openMocks(this);

        setup();
        clearInvocations(assignmentRepository, apiClient);

        when(assessmentRepository.findByTreatment_Assignment_AssignmentId(anyLong())).thenReturn(Collections.singletonList(assessment));
        when(assignmentRepository.findByExposure_ExposureIdAndSoftDeleted(anyLong(), anyBoolean())).thenReturn(Collections.singletonList(assignment));
        when(submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(anyLong())).thenReturn(0L);
        when(treatmentRepository.findByAssignment_AssignmentId(anyLong())).thenReturn(Collections.emptyList());
        when(ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong())).thenReturn(instructorUser);

        when(assessmentService.getAssessmentForParticipant(any(Participant.class), any(SecuredInfo.class))).thenReturn(assessment);
        doNothing().when(assessmentService).verifySubmissionLimit(anyInt(), anyInt());
        doNothing().when(assessmentService).verifySubmissionWaitTime(anyFloat(), anyList());
        when(apiClient.listAssignment(eq(instructorUser), anyString(), anyString())).thenReturn(Optional.empty());
        when(apiClient.createLmsAssignment(any(LtiUserEntity.class), any(Assignment.class), anyString())).thenReturn(lmsAssignment);
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

    /*@Test
    public void duplicateAssignmentTest() throws DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
                                                AssignmentNotCreatedException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, NumberFormatException, ExceedingLimitException, TreatmentNotMatchingException, QuestionNotMatchingException, ApiException, TerracottaConnectorException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(assignmentDto.getAssignmentId()).thenReturn(null);

        AssignmentDto retVal = assignmentService.duplicateAssignment(1L, securedInfo);

        assertNotNull(retVal);
        verify(assignmentRepository).save(any(Assignment.class));
    }

    @Test
    public void duplicateAssignmentTestWithTreatments() throws DataServiceException, IdInPostException, TitleValidationException, AssessmentNotMatchingException,
                                                AssignmentNotCreatedException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException,
                                                NumberFormatException, ExceedingLimitException, TreatmentNotMatchingException, QuestionNotMatchingException, ApiException, TerracottaConnectorException {
        when(treatmentRepository.findByAssignment_AssignmentId(anyLong())).thenReturn(Collections.singletonList(treatment));
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(assignmentDto.getAssignmentId()).thenReturn(null);

        AssignmentDto retVal = assignmentService.duplicateAssignment(0L, securedInfo);

        assertNotNull(retVal);
    }*/

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
        when(assignmentRepository.findByExposure_ExposureIdAndSoftDeleted(anyLong(), anyBoolean())).thenReturn(Collections.emptyList());

        List<AssignmentDto> retVal = assignmentService.getAssignments(0L, false, false, securedInfo);

        assertNotNull(retVal);
        assertEquals(0, retVal.size());
    }

    @Test
    public void testDeleteAssignmentHard() throws EmptyResultDataAccessException, AssignmentNotEditedException, ApiException, TerracottaConnectorException {
        assignmentService.deleteById(1L, securedInfo);

        verify(assignmentRepository).deleteByAssignmentId(anyLong());
        verify(assignmentRepository, never()).saveAndFlush(any(Assignment.class));
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

}
