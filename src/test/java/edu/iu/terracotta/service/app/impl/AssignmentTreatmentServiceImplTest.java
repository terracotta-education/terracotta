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

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
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

    @InjectMocks private AssignmentTreatmentServiceImpl assignmentTreatmentService;

    @BeforeEach
    public void beforeEach() throws AssessmentNotMatchingException, AssignmentAttemptException, NumberFormatException, IdInPostException, DataServiceException, ExceedingLimitException, TreatmentNotMatchingException, QuestionNotMatchingException {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    public void testDuplicateTreatment() throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException, TreatmentNotMatchingException, QuestionNotMatchingException, ApiException, TerracottaConnectorException {
        TreatmentDto treatmentDto = assignmentTreatmentService.duplicateTreatment(1L, assignment, securedInfo);

        assertNotNull(treatmentDto);
        assertEquals(1L, treatmentDto.getTreatmentId());
    }

    @Test
    public void testDuplicateTreatmentNoAssessmentFound() throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException, TreatmentNotMatchingException, QuestionNotMatchingException, ApiException, TerracottaConnectorException {
        when(treatment.getAssessment()).thenReturn(null);
        TreatmentDto treatmentDto = assignmentTreatmentService.duplicateTreatment(1L, assignment, securedInfo);

        assertNotNull(treatmentDto);
        assertEquals(1L, treatmentDto.getTreatmentId());
        assertNull(treatmentDto.getAssessmentDto());
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
}
