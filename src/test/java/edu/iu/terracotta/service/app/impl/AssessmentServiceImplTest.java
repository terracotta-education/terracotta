package edu.iu.terracotta.service.app.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;

import java.util.Collections;
import java.util.Optional;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import edu.iu.terracotta.model.app.Group;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import edu.iu.terracotta.model.app.dto.QuestionDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.repository.AssessmentRepository;
import edu.iu.terracotta.repository.ExposureGroupConditionRepository;
import edu.iu.terracotta.repository.TreatmentRepository;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.app.QuestionService;

import org.apache.commons.lang3.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class AssessmentServiceImplTest {

    @InjectMocks
    private AssessmentServiceImpl assessmentService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private QuestionService questionService;

    @Mock
    private AllRepositories allRepositories;

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private ExposureGroupConditionRepository exposureGroupConditionRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private Condition condition;

    @Mock
    private Assignment assignment;

    @Mock
    private Exposure exposure;

    @Mock
    private Group group;

    @Mock
    private ExposureGroupCondition exposureGroupCondition;

    @Mock
    private Treatment treatment;

    @Mock
    private EntityManager entityManager;

    private Assessment assessment;
    private Assessment newAssessment;

    @BeforeEach
    public void beforeEach() throws DataServiceException, AssessmentNotMatchingException {
        MockitoAnnotations.openMocks(this);

        allRepositories.assessmentRepository = assessmentRepository;
        allRepositories.exposureGroupConditionRepository = exposureGroupConditionRepository;
        allRepositories.treatmentRepository = treatmentRepository;

        assessment = new Assessment();
        assessment.setAssessmentId(3L);
        assessment.setTreatment(treatment);
        assessment.setQuestions(Collections.emptyList());

        newAssessment = new Assessment();
        newAssessment.setAssessmentId(3L);
        newAssessment.setTreatment(treatment);
        newAssessment.setQuestions(Collections.emptyList());

        when(allRepositories.assessmentRepository.findByAssessmentId(anyLong())).thenReturn(assessment);
        when(allRepositories.assessmentRepository.save(any(Assessment.class))).thenReturn(newAssessment);
        when(allRepositories.treatmentRepository.findByTreatmentId(anyLong())).thenReturn(treatment);
        when(allRepositories.treatmentRepository.saveAndFlush(any(Treatment.class))).thenReturn(treatment);
        when(fileStorageService.parseHTMLFiles(anyString())).thenReturn(StringUtils.EMPTY);
        when(allRepositories.exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.of(exposureGroupCondition));
        when(questionService.duplicateQuestionsForAssessment(anyLong(), anyLong())).thenReturn(Collections.singletonList(new QuestionDto()));
        when(condition.getConditionId()).thenReturn(1L);
        when(assignment.getAssignmentId()).thenReturn(1L);
        when(assignment.getExposure()).thenReturn(exposure);
        when(exposure.getExposureId()).thenReturn(1L);
        when(group.getGroupId()).thenReturn(1L);
        when(exposureGroupCondition.getGroup()).thenReturn(group);
        when(treatment.getTreatmentId()).thenReturn(1L);
        when(treatment.getAssessment()).thenReturn(assessment);
        when(treatment.getAssignment()).thenReturn(assignment);
        when(treatment.getCondition()).thenReturn(condition);
    }

    @Test
    public void testDuplicateAssessment() throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException {
        AssessmentDto assessmentDto = assessmentService.duplicateAssessment(1L, 2L);

        assertNotNull(assessmentDto);
        assertEquals(3L, assessmentDto.getAssessmentId());
        assertNull(assessment.getAssessmentId());
    }

    @Test
    public void testDuplicateAssessmentNotFound() throws IdInPostException, ExceedingLimitException, AssessmentNotMatchingException {
        when(allRepositories.assessmentRepository.findByAssessmentId(anyLong())).thenReturn(null);

        Exception exception = assertThrows(DataServiceException.class, () -> { assessmentService.duplicateAssessment(1L, 2L); });

        assertEquals("The assessment with the given ID does not exist", exception.getMessage());
    }

}
