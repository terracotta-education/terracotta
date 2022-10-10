package edu.iu.terracotta.service.app.impl;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.IdMismatchException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import edu.iu.terracotta.model.app.dto.AssignmentDto;
import edu.iu.terracotta.model.app.dto.TreatmentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.repository.AssignmentRepository;
import edu.iu.terracotta.repository.ConditionRepository;
import edu.iu.terracotta.repository.TreatmentRepository;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.utils.TextConstants;

public class TreatmentServiceImplTest {

    @InjectMocks
    private TreatmentServiceImpl treatmentService;

    @Mock private AllRepositories allRepositories;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private ConditionRepository conditionRepository;
    @Mock private TreatmentRepository treatmentRepository;

    @Mock private AssessmentService assessmentService;
    @Mock private AssignmentService assignmentService;
    @Mock private EntityManager entityManager;

    @Mock private Assessment assessment;
    @Mock private AssessmentDto assessmentDto;
    @Mock private Assignment assignment;
    @Mock private AssignmentDto assignmentDto;
    @Mock private Condition condition;
    @Mock private SecuredInfo securedInfo;
    @Mock private Treatment treatment;
    @Mock private TreatmentDto treatmentDtoToUpdate;


    @BeforeEach
    public void beforeEach() throws DataServiceException, AssessmentNotMatchingException, CanvasApiException, TreatmentNotMatchingException, QuestionNotMatchingException {
        MockitoAnnotations.openMocks(this);

        clearInvocations(assignmentService);

        allRepositories.assignmentRepository = assignmentRepository;
        allRepositories.conditionRepository = conditionRepository;
        allRepositories.treatmentRepository = treatmentRepository;

        when(assessmentService.duplicateAssessment(anyLong(), anyLong())).thenReturn(assessment);
        when(assignmentRepository.findById(anyLong())).thenReturn(Optional.of(assignment));
        when(conditionRepository.findById(anyLong())).thenReturn(Optional.of(condition));
        when(treatmentRepository.findByTreatmentId(anyLong())).thenReturn(treatment);
        when(treatmentRepository.findByCondition_ConditionId(anyLong())).thenReturn(Collections.singletonList(treatment));
        when(treatmentRepository.save(any(Treatment.class))).thenReturn(treatment);

        when(assessment.getAssessmentId()).thenReturn(1L);
        when(assessmentDto.getAssessmentId()).thenReturn(1L);
        when(assignment.getAssignmentId()).thenReturn(1L);
        when(assignmentDto.getAssignmentId()).thenReturn(1L);
        when(condition.getConditionId()).thenReturn(1L);
        when(securedInfo.getCanvasCourseId()).thenReturn("canvasCourseId");
        when(treatment.getAssessment()).thenReturn(assessment);
        when(treatment.getAssignment()).thenReturn(assignment);
        when(treatment.getCondition()).thenReturn(condition);
        when(treatment.getTreatmentId()).thenReturn(1l);
        when(treatmentDtoToUpdate.getAssessmentDto()).thenReturn(assessmentDto);
        when(treatmentDtoToUpdate.getAssignmentDto()).thenReturn(assignmentDto);
        when(treatmentDtoToUpdate.getTreatmentId()).thenReturn(1L);

    }

    @Test
    public void testDuplicateTreatment() throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException, CanvasApiException, TreatmentNotMatchingException, QuestionNotMatchingException {
        TreatmentDto treatmentDto = treatmentService.duplicateTreatment(1L, "0", 0L);

        assertNotNull(treatmentDto);
        assertEquals(1L, treatmentDto.getTreatmentId());
    }

    @Test
    public void testDuplicateTreatmentNoAssessmentFound() throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException, CanvasApiException, TreatmentNotMatchingException, QuestionNotMatchingException {
        when(treatment.getAssessment()).thenReturn(null);
        TreatmentDto treatmentDto = treatmentService.duplicateTreatment(1L, "0", 0L);

        assertNotNull(treatmentDto);
        assertEquals(1L, treatmentDto.getTreatmentId());
        assertNull(treatmentDto.getAssessmentDto());
    }

    @Test
    public void testDuplicateTreatmentNotFound() throws IdInPostException, ExceedingLimitException, AssessmentNotMatchingException {
        when(allRepositories.treatmentRepository.findByTreatmentId(anyLong())).thenReturn(null);

        Exception exception = assertThrows(DataServiceException.class, () -> { treatmentService.duplicateTreatment(1L, "0", 0L); });

        assertEquals("The treatment with the given ID does not exist", exception.getMessage());
    }

    @Test
    public void testGetTreatments() throws NumberFormatException, AssessmentNotMatchingException, CanvasApiException {
        List<TreatmentDto> treatmentDtos = treatmentService.getTreatments(0L, "0", 0l, false);

        assertNotNull(treatmentDtos);
        assertEquals(1, treatmentDtos.size());
        verify(assignmentService).setAssignmentDtoAttrs(any(Assignment.class), anyString(), anyLong());
    }

    @Test
    public void testPutTreatment() throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, IdMissingException, IdMismatchException, TreatmentNotMatchingException, TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, CanvasApiException, AssignmentNotEditedException, NegativePointsException, QuestionNotMatchingException {
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
            AssessmentNotMatchingException, TitleValidationException, CanvasApiException, AssignmentNotEditedException,
            RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, AssignmentNotMatchingException, NegativePointsException, QuestionNotMatchingException {
        doThrow(new AssessmentNotMatchingException(TextConstants.ASSESSMENT_NOT_MATCHING)).when(assessmentService).updateAssessment(anyLong(), any(AssessmentDto.class), anyBoolean());

        Exception exception = assertThrows(DataServiceException.class, () -> { treatmentService.putTreatment(treatmentDtoToUpdate, 1L, securedInfo, false); });

        assertEquals(String.format(TextConstants.UNABLE_TO_UPDATE_TREATMENT, TextConstants.ASSESSMENT_NOT_MATCHING), exception.getMessage());
    }

    @Test
    public void testPutTreatmentInvalidAssignment() throws IdInPostException, DataServiceException, ExceedingLimitException,
            AssessmentNotMatchingException, TitleValidationException, CanvasApiException, AssignmentNotEditedException,
            RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, AssignmentNotMatchingException {
        when(assignmentService.updateAssignment(anyLong(), any(AssignmentDto.class), anyString())).thenThrow(new AssignmentNotMatchingException(TextConstants.ASSIGNMENT_NOT_MATCHING));

        Exception exception = assertThrows(DataServiceException.class, () -> { treatmentService.putTreatment(treatmentDtoToUpdate, 1L, securedInfo, false); });

        assertEquals(String.format(TextConstants.UNABLE_TO_UPDATE_TREATMENT, TextConstants.ASSIGNMENT_NOT_MATCHING), exception.getMessage());
    }

}
