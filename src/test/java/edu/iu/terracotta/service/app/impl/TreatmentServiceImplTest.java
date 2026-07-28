package edu.iu.terracotta.service.app.impl;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotFoundException;
import edu.iu.terracotta.dao.model.dto.AssessmentDto;
import edu.iu.terracotta.dao.model.dto.TreatmentDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.IdMismatchException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.utils.TextConstants;

import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

public class TreatmentServiceImplTest extends BaseTest {

    private TreatmentServiceImpl treatmentService;

    @Mock private TreatmentDto treatmentDtoToUpdate;

    @BeforeEach
    public void beforeEach() throws DataServiceException, AssessmentNotMatchingException, TreatmentNotMatchingException, QuestionNotMatchingException {
        MockitoAnnotations.openMocks(this);

        setup();

        // apiJwtService below also collides with CanvasApiJwtServiceImpl in BaseServiceTest (see the
        // @InjectMocks pitfall note there), so this class is constructed manually instead of relying
        // on @InjectMocks, which non-deterministically wired the wrong mock and left apiJwtService
        // calls silently unstubbed.
        treatmentService = new TreatmentServiceImpl(
            assignmentRepository,
            conditionRepository,
            ltiUserRepository,
            treatmentRepository,
            apiJwtService,
            assessmentService,
            assignmentTreatmentService
        );
        clearInvocations(assignmentService);

        when(assessmentService.duplicateAssessment(anyLong(), anyLong())).thenReturn(assessment);
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong())).thenReturn(ltiUserEntity);
        when(treatmentRepository.findByCondition_ConditionIdOrderByCondition_ConditionIdAsc(anyLong())).thenReturn(Collections.singletonList(treatment));

        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);

        when(treatmentDtoToUpdate.getAssessmentDto()).thenReturn(assessmentDto);
        when(treatmentDtoToUpdate.getAssignmentDto()).thenReturn(assignmentDto);
        when(treatmentDtoToUpdate.getTreatmentId()).thenReturn(1L);
    }

    @Test
    public void testGetTreatmentsAsStudentUser()
            throws NumberFormatException, AssessmentNotMatchingException, ApiException, TerracottaConnectorException {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);

        List<TreatmentDto> treatmentDtos = treatmentService.getTreatments(0L, false, securedInfo);

        assertNotNull(treatmentDtos);
        assertEquals(1, treatmentDtos.size());
        verify(assignmentTreatmentService, never()).setAssignmentDtoAttrs(any(Assignment.class), anyString(), any(LtiUserEntity.class));
    }

    @Test
    public void testPutTreatment()
        throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, IdMissingException, IdMismatchException, TreatmentNotMatchingException,
        TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, AssignmentNotEditedException,
        NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException, IntegrationClientNotFoundException, IntegrationNotFoundException, ApiException {
        TreatmentDto treatmentDto = treatmentService.putTreatment(treatmentDtoToUpdate, 1L, securedInfo, false);

        assertNotNull(treatmentDto);
        verify(treatmentRepository).save(any(Treatment.class));
    }

    @Test
    public void testPutTreatmentNoIdInPut() throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException {
        when(treatmentDtoToUpdate.getTreatmentId()).thenReturn(null);

        Exception exception = assertThrows(IdMissingException.class, () -> { treatmentService.putTreatment(treatmentDtoToUpdate, 1L, securedInfo, false); });

        assertEquals(TextConstants.ID_MISSING, exception.getMessage());
    }

    @Test
    public void testPutTreatmentIdMismatch() throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException {
        when(treatmentDtoToUpdate.getTreatmentId()).thenReturn(2L);

        Exception exception = assertThrows(IdMismatchException.class, () -> { treatmentService.putTreatment(treatmentDtoToUpdate, 1L, securedInfo, false); });

        assertEquals(TextConstants.ID_MISMATCH_PUT, exception.getMessage());
    }

    @Test
    public void testPutTreatmentNoAssignment() throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException {
        when(treatmentDtoToUpdate.getAssignmentId()).thenReturn(null);

        Exception exception = assertThrows(DataServiceException.class, () -> { treatmentService.putTreatment(treatmentDtoToUpdate, 1L, securedInfo, false); });

        assertEquals(TextConstants.NO_ASSIGNMENT_IN_TREATMENTDTO, exception.getMessage());
    }

    @Test
    public void testPutTreatmentInvalidAssessment() throws IdInPostException, DataServiceException, ExceedingLimitException,
            AssessmentNotMatchingException, TitleValidationException, AssignmentNotEditedException,
            RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, AssignmentNotMatchingException,
            NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException, IntegrationClientNotFoundException, IntegrationNotFoundException {
        doThrow(new AssessmentNotMatchingException(TextConstants.ASSESSMENT_NOT_MATCHING)).when(assessmentService).updateAssessment(anyLong(), any(AssessmentDto.class), anyBoolean());

        Exception exception = assertThrows(DataServiceException.class, () -> { treatmentService.putTreatment(treatmentDtoToUpdate, 1L, securedInfo, false); });

        assertEquals(String.format(TextConstants.UNABLE_TO_UPDATE_TREATMENT, TextConstants.ASSESSMENT_NOT_MATCHING), exception.getMessage());
    }

    @Test
    public void testGetTreatmentsEmpty() throws NumberFormatException, AssessmentNotMatchingException, ApiException, TerracottaConnectorException {
        when(treatmentRepository.findByCondition_ConditionIdOrderByCondition_ConditionIdAsc(anyLong())).thenReturn(Collections.emptyList());

        List<TreatmentDto> treatmentDtos = treatmentService.getTreatments(1L, false, securedInfo);

        assertNotNull(treatmentDtos);
        assertTrue(treatmentDtos.isEmpty());
        verify(assignmentTreatmentService, never()).toTreatmentDto(any(Treatment.class), anyBoolean(), anyBoolean(), any(SecuredInfo.class));
    }

    @Test
    public void testGetTreatmentsAsInstructor() throws NumberFormatException, AssessmentNotMatchingException, ApiException, TerracottaConnectorException {
        List<TreatmentDto> treatmentDtos = treatmentService.getTreatments(1L, true, securedInfo);

        assertNotNull(treatmentDtos);
        assertEquals(1, treatmentDtos.size());
        verify(assignmentTreatmentService).setAssignmentDtoAttrs(anyList(), eq(ltiUserEntity));
        verify(assignmentTreatmentService).toTreatmentDto(treatment, true, true, securedInfo);
    }

    @Test
    public void testGetTreatment() {
        Treatment result = treatmentService.getTreatment(1L);

        assertNotNull(result);
        verify(treatmentRepository).findByTreatmentId(1L);
    }

    @Test
    public void testPostTreatmentHappyPath() throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, TreatmentNotMatchingException {
        when(treatmentDto.getTreatmentId()).thenReturn(null);

        TreatmentDto result = treatmentService.postTreatment(treatmentDto, 5L, securedInfo);

        assertNotNull(result);
        verify(treatmentDto).setConditionId(5L);
        verify(treatmentRepository).save(any(Treatment.class));
    }

    @Test
    public void testPostTreatmentIdInPost() {
        // default (unstubbed) treatmentDto.getTreatmentId() returns a non-null Long, so the "already has an id" branch triggers.
        assertThrows(IdInPostException.class, () -> treatmentService.postTreatment(treatmentDto, 1L, securedInfo));
    }

    @Test
    public void testPostTreatmentNoAssignmentId() {
        when(treatmentDto.getTreatmentId()).thenReturn(null);
        when(treatmentDto.getAssignmentId()).thenReturn(null);

        Exception exception = assertThrows(DataServiceException.class, () -> treatmentService.postTreatment(treatmentDto, 1L, securedInfo));

        assertEquals("Error 129: Unable to create Treatment: The assignmentId is mandatory", exception.getMessage());
    }

    @Test
    public void testPostTreatmentFromDtoAssignmentNotFound() {
        when(treatmentDto.getTreatmentId()).thenReturn(null);
        when(assignmentRepository.findById(anyLong())).thenReturn(java.util.Optional.empty());

        Exception exception = assertThrows(DataServiceException.class, () -> treatmentService.postTreatment(treatmentDto, 1L, securedInfo));

        assertEquals(String.format(TextConstants.UNABLE_TO_CREATE_TREATMENT, TextConstants.NO_ASSIGNMENT_IN_TREATMENTDTO), exception.getMessage());
    }

    @Test
    public void testPostTreatmentFromDtoConditionNotFound() {
        when(treatmentDto.getTreatmentId()).thenReturn(null);
        when(conditionRepository.findById(anyLong())).thenReturn(java.util.Optional.empty());

        Exception exception = assertThrows(DataServiceException.class, () -> treatmentService.postTreatment(treatmentDto, 1L, securedInfo));

        assertEquals(String.format(TextConstants.UNABLE_TO_CREATE_TREATMENT, TextConstants.NO_CONDITION_FOR_TREATMENT), exception.getMessage());
    }

    @Test
    public void testPostTreatmentExceedingLimit() {
        when(treatmentDto.getTreatmentId()).thenReturn(null);
        when(treatmentRepository.existsByAssignment_AssignmentIdAndCondition_ConditionId(anyLong(), anyLong())).thenReturn(true);

        assertThrows(ExceedingLimitException.class, () -> treatmentService.postTreatment(treatmentDto, 1L, securedInfo));
    }

    @Test
    public void testDeleteById() {
        treatmentService.deleteById(1L);

        verify(treatmentRepository).deleteByTreatmentId(1L);
    }

    @Test
    public void testLimitToOneNotExceeding() {
        when(treatmentRepository.existsByAssignment_AssignmentIdAndCondition_ConditionId(anyLong(), anyLong())).thenReturn(false);

        assertDoesNotThrow(() -> treatmentService.limitToOne(1L, 2L));
    }

    @Test
    public void testLimitToOneExceeding() {
        when(treatmentRepository.existsByAssignment_AssignmentIdAndCondition_ConditionId(anyLong(), anyLong())).thenReturn(true);

        Exception exception = assertThrows(ExceedingLimitException.class, () -> treatmentService.limitToOne(1L, 2L));

        assertEquals("Error 141: A treatment for the condition 2 and assignment 1 already exists.", exception.getMessage());
    }

    @Test
    public void testBuildHeaders() {
        HttpHeaders headers = treatmentService.buildHeaders(UriComponentsBuilder.newInstance(), 1L, 2L, 3L);

        assertNotNull(headers);
        assertNotNull(headers.getLocation());
        assertTrue(headers.getLocation().toString().contains("/api/experiments/1/conditions/2/treatments/3"));
    }

}
