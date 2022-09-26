package edu.iu.terracotta.service.app.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import edu.iu.terracotta.model.app.Group;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import edu.iu.terracotta.model.app.dto.QuestionDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.repository.AssessmentRepository;
import edu.iu.terracotta.repository.AssignmentRepository;
import edu.iu.terracotta.repository.ExperimentRepository;
import edu.iu.terracotta.repository.ExposureGroupConditionRepository;
import edu.iu.terracotta.repository.ParticipantRepository;
import edu.iu.terracotta.repository.TreatmentRepository;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.service.app.QuestionService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.lang3.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class AssessmentServiceImplTest {

    @InjectMocks
    private AssessmentServiceImpl assessmentService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ParticipantService participantService;

    @Mock
    private QuestionService questionService;

    @Mock
    private SubmissionService submissionService;

    @Mock
    private AllRepositories allRepositories;

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private ExposureGroupConditionRepository exposureGroupConditionRepository;

    @Mock
    private ParticipantRepository participantRepository;

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

    @Mock
    private SecuredInfo securedInfo;

    @Mock
    private Experiment experiment;

    @Mock
    private Participant participant;

    @Mock
    private Submission submission;

    private AssessmentServiceImpl assessmentServiceSpy;

    private Assessment assessment;
    private Assessment newAssessment;

    @BeforeEach
    public void beforeEach() throws DataServiceException, AssessmentNotMatchingException, GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException {
        MockitoAnnotations.openMocks(this);

        assessmentServiceSpy = spy(assessmentService);

        allRepositories.assessmentRepository = assessmentRepository;
        allRepositories.assignmentRepository = assignmentRepository;
        allRepositories.experimentRepository = experimentRepository;
        allRepositories.exposureGroupConditionRepository = exposureGroupConditionRepository;
        allRepositories.participantRepository = participantRepository;
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
        when(allRepositories.assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(anyLong(), anyString())).thenReturn(assignment);
        when(allRepositories.experimentRepository.findById(anyLong())).thenReturn(Optional.of(experiment));
        when(allRepositories.exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.of(exposureGroupCondition));
        when(allRepositories.exposureGroupConditionRepository.getByGroup_GroupIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.of(exposureGroupCondition));
        when(allRepositories.participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(participant);
        when(allRepositories.treatmentRepository.findByCondition_ConditionIdAndAssignment_AssignmentId(anyLong(), anyLong())).thenReturn(Collections.singletonList(treatment));
        when(allRepositories.treatmentRepository.findByTreatmentId(anyLong())).thenReturn(treatment);
        when(allRepositories.treatmentRepository.saveAndFlush(any(Treatment.class))).thenReturn(treatment);
        when(fileStorageService.parseHTMLFiles(anyString())).thenReturn(StringUtils.EMPTY);
        when(participantService.handleExperimentParticipant(any(Experiment.class), any(SecuredInfo.class))).thenReturn(participant);
        when(questionService.duplicateQuestionsForAssessment(anyLong(), anyLong())).thenReturn(Collections.singletonList(new QuestionDto()));
        when(submissionService.getScoreFromMultipleSubmissions(any(Participant.class), any(Assessment.class))).thenReturn(1F);
        when(submissionService.findByParticipantId(anyLong())).thenReturn(Collections.singletonList(submission));
        when(condition.getConditionId()).thenReturn(1L);
        when(condition.getDefaultCondition()).thenReturn(true);
        when(assignment.getAssignmentId()).thenReturn(1L);
        when(assignment.getExposure()).thenReturn(exposure);
        when(exposure.getExposureId()).thenReturn(1L);
        when(group.getGroupId()).thenReturn(1L);
        when(exposureGroupCondition.getCondition()).thenReturn(condition);
        when(exposureGroupCondition.getGroup()).thenReturn(group);
        when(treatment.getTreatmentId()).thenReturn(1L);
        when(treatment.getAssessment()).thenReturn(assessment);
        when(treatment.getAssignment()).thenReturn(assignment);
        when(treatment.getCondition()).thenReturn(condition);
        when(securedInfo.getUserId()).thenReturn("canvasUserId");
        when(securedInfo.getCanvasAssignmentId()).thenReturn("canvasAssignmentId");
        when(participant.getParticipantId()).thenReturn(1L);
        when(participant.getConsent()).thenReturn(true);
        when(participant.getGroup()).thenReturn(group);
        when(submission.getDateSubmitted()).thenReturn(Timestamp.from(Instant.now()));

        doReturn(assessment).when(assessmentServiceSpy).getAssessmentByGroupId(anyLong(), anyString(), anyLong());
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

    @Test
    public void testViewAssessment() throws ExperimentNotMatchingException, ParticipantNotMatchingException, AssessmentNotMatchingException, GroupNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException {
        AssessmentDto assessmentDto = assessmentService.viewAssessment(1l, securedInfo);

        assertNotNull(assessmentDto);
        assertNotNull(assessmentDto.getRetakeDetails());
        assertEquals(1F, assessmentDto.getRetakeDetails().getKeptScore());
        assertEquals(1, assessmentDto.getRetakeDetails().getSubmissionAttemptsCount());
    }

    @Test
    public void testViewAssessmentNoExperiment() throws IdInPostException, ExceedingLimitException, AssessmentNotMatchingException {
        when(allRepositories.experimentRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(ExperimentNotMatchingException.class, () -> { assessmentService.viewAssessment(1l, securedInfo); });

        assertEquals(TextConstants.EXPERIMENT_NOT_MATCHING, exception.getMessage());
    }

    @Test
    public void testViewAssessmentNoParticipant() throws IdInPostException, ExceedingLimitException, AssessmentNotMatchingException {
        when(allRepositories.participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(null);

        Exception exception = assertThrows(ParticipantNotMatchingException.class, () -> { assessmentService.viewAssessment(1l, securedInfo); });

        assertEquals(TextConstants.PARTICIPANT_NOT_MATCHING, exception.getMessage());
    }

    @Test
    public void testGetAssessmentByGroupId() throws AssessmentNotMatchingException {
        Assessment assessment = assessmentService.getAssessmentByGroupId(1L, "1", 1l);

        assertNotNull(assessment);
    }

    @Test
    public void testGetAssessmentByGroupIdNoAssignment() throws AssessmentNotMatchingException {
        when(allRepositories.assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(anyLong(), anyString())).thenReturn(null);
        Exception exception = assertThrows(AssessmentNotMatchingException.class, () -> { assessmentService.getAssessmentByGroupId(1L, "1", 1l); });

        assertEquals("Error 127: This assignment does not exist in Terracotta for this experiment", exception.getMessage());
    }

    @Test
    public void testGetAssessmentByGroupIdNoExposureGroupCondition() throws AssessmentNotMatchingException {
        when(allRepositories.exposureGroupConditionRepository.getByGroup_GroupIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.empty());
        Exception exception = assertThrows(AssessmentNotMatchingException.class, () -> { assessmentService.getAssessmentByGroupId(1L, "1", 1l); });

        assertEquals("Error 130: This assignment does not have a condition assigned for the participant group.", exception.getMessage());
    }

    @Test
    public void testGetAssessmentByConditionId() throws AssessmentNotMatchingException {
        Assessment assessment = assessmentService.getAssessmentByConditionId(1L, "1", 1l);

        assertNotNull(assessment);
    }

    @Test
    public void testGetAssessmentByConditionIdNoAssignment() throws AssessmentNotMatchingException {
        when(allRepositories.assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(anyLong(), anyString())).thenReturn(null);
        Exception exception = assertThrows(AssessmentNotMatchingException.class, () -> { assessmentService.getAssessmentByConditionId(1L, "1", 1l); });

        assertEquals("Error 127: This assignment does not exist in Terracotta for this experiment", exception.getMessage());
    }

}
