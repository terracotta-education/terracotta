package edu.iu.terracotta.service.app.impl;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import edu.iu.terracotta.model.app.dto.TreatmentDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.repository.TreatmentRepository;
import edu.iu.terracotta.service.app.AssessmentService;

public class TreatmentServiceImplTest {

    @InjectMocks
    private TreatmentServiceImpl treatmentService;

    @Mock
    private AssessmentService assessmentService;

    @Mock
    private AllRepositories allRepositories;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private Condition condition;

    @Mock
    private Assignment assignment;

    @Mock
    private Assessment assessment;

    @Mock
    private EntityManager entityManager;

    private Treatment treatment;
    private Treatment newTreatment;

    @BeforeEach
    public void beforeEach() throws DataServiceException, AssessmentNotMatchingException {
        MockitoAnnotations.openMocks(this);

        allRepositories.treatmentRepository = treatmentRepository;

        treatment = new Treatment();
        treatment.setTreatmentId(1L);
        treatment.setAssessment(assessment);

        newTreatment = new Treatment();
        newTreatment.setTreatmentId(2L);
        newTreatment.setCondition(condition);
        newTreatment.setAssignment(assignment);

        when(allRepositories.treatmentRepository.findByTreatmentId(anyLong())).thenReturn(treatment);
        when(allRepositories.treatmentRepository.save(any(Treatment.class))).thenReturn(newTreatment);
        when(assessmentService.duplicateAssessment(anyLong(), anyLong())).thenReturn(new AssessmentDto());
        when(condition.getConditionId()).thenReturn(1L);
        when(assignment.getAssignmentId()).thenReturn(1L);
        when(assessment.getAssessmentId()).thenReturn(1L);
    }

    @Test
    public void testDuplicateTreatment() throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException {
        TreatmentDto treatmentDto = treatmentService.duplicateTreatment(1L);

        assertNotNull(treatmentDto);
        assertEquals(2L, treatmentDto.getTreatmentId());
        assertNull(treatment.getTreatmentId());
    }

    @Test
    public void testDuplicateTreatmentNoAssessmentFound() throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException {
        treatment.setAssessment(null);
        TreatmentDto treatmentDto = treatmentService.duplicateTreatment(1L);

        assertNotNull(treatmentDto);
        assertEquals(2L, treatmentDto.getTreatmentId());
        assertNull(treatmentDto.getAssessmentDto());
        assertNull(treatment.getTreatmentId());
    }

    @Test
    public void testDuplicateTreatmentNotFound() throws IdInPostException, ExceedingLimitException, AssessmentNotMatchingException {
        when(allRepositories.treatmentRepository.findByTreatmentId(anyLong())).thenReturn(null);

        Exception exception = assertThrows(DataServiceException.class, () -> { treatmentService.duplicateTreatment(1L); });

        assertEquals("The treatment with the given ID does not exist", exception.getMessage());
    }

}
