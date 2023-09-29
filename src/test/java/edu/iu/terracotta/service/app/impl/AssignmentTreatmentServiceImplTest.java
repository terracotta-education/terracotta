package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.BaseTest;
import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.app.dto.AssignmentDto;
import edu.iu.terracotta.model.app.dto.TreatmentDto;

public class AssignmentTreatmentServiceImplTest extends BaseTest {

    @InjectMocks private AssignmentTreatmentServiceImpl assignmentTreatmentService;

    @BeforeEach
    public void beforeEach() throws AssessmentNotMatchingException, AssignmentAttemptException, CanvasApiException, NumberFormatException, IdInPostException, DataServiceException, ExceedingLimitException, TreatmentNotMatchingException, QuestionNotMatchingException {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    public void testDuplicateTreatment() throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException, CanvasApiException, TreatmentNotMatchingException, QuestionNotMatchingException {
        TreatmentDto treatmentDto = assignmentTreatmentService.duplicateTreatment(1L, assignment, securedInfo);

        assertNotNull(treatmentDto);
        assertEquals(1L, treatmentDto.getTreatmentId());
    }

    @Test
    public void testDuplicateTreatmentNoAssessmentFound() throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException, CanvasApiException, TreatmentNotMatchingException, QuestionNotMatchingException {
        when(treatment.getAssessment()).thenReturn(null);
        TreatmentDto treatmentDto = assignmentTreatmentService.duplicateTreatment(1L, assignment, securedInfo);

        assertNotNull(treatmentDto);
        assertEquals(1L, treatmentDto.getTreatmentId());
        assertNull(treatmentDto.getAssessmentDto());
    }

    @Test
    public void testDuplicateTreatmentNotFound() throws IdInPostException, ExceedingLimitException, AssessmentNotMatchingException {
        when(allRepositories.treatmentRepository.findByTreatmentId(anyLong())).thenReturn(null);

        Exception exception = assertThrows(DataServiceException.class, () -> { assignmentTreatmentService.duplicateTreatment(1L, assignment, securedInfo); });

        assertEquals("The treatment with the given ID does not exist", exception.getMessage());
    }

    @Test
    public void testToAssignmentDtoWithTreatment() throws AssessmentNotMatchingException {
        AssignmentDto retVal = assignmentTreatmentService.toAssignmentDto(assignment, false, true);

        assertNotNull(retVal);
        assertEquals(1, retVal.getTreatments().size());
    }
}
