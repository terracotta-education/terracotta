package edu.iu.terracotta.service.app.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

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
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import edu.iu.terracotta.model.app.Group;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import edu.iu.terracotta.model.app.dto.QuestionDto;
import edu.iu.terracotta.model.app.enumerator.MultipleSubmissionScoringScheme;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.repository.AssessmentRepository;
import edu.iu.terracotta.repository.AssignmentRepository;
import edu.iu.terracotta.repository.ExperimentRepository;
import edu.iu.terracotta.repository.ExposureGroupConditionRepository;
import edu.iu.terracotta.repository.ParticipantRepository;
import edu.iu.terracotta.repository.QuestionRepository;
import edu.iu.terracotta.repository.TreatmentRepository;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.service.app.QuestionService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.lang3.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AssessmentServiceImplTest {

    @Spy
    @InjectMocks
    private AssessmentServiceImpl assessmentService;

    @Mock private FileStorageService fileStorageService;
    @Mock private ParticipantService participantService;
    @Mock private QuestionService questionService;
    @Mock private SubmissionService submissionService;

    @Mock private AllRepositories allRepositories;
    @Mock private AssessmentRepository assessmentRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private ExperimentRepository experimentRepository;
    @Mock private ExposureGroupConditionRepository exposureGroupConditionRepository;
    @Mock private ParticipantRepository participantRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private TreatmentRepository treatmentRepository;

    @Mock private Assessment assessment;
    @Mock private AssessmentDto assessmentDto;
    @Mock private Assignment assignment;
    @Mock private Condition condition;
    @Mock private EntityManager entityManager;
    @Mock private Experiment experiment;
    @Mock private Exposure exposure;
    @Mock private ExposureGroupCondition exposureGroupCondition;
    @Mock private Group group;
    @Mock private Participant participant;
    @Mock private Question question;
    @Mock private QuestionDto questionDto;
    @Mock private SecuredInfo securedInfo;
    @Mock private Submission submission;
    @Mock private Treatment treatment;

    @BeforeEach
    public void beforeEach() throws DataServiceException, AssessmentNotMatchingException, GroupNotMatchingException, ParticipantNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, IdInPostException {
        MockitoAnnotations.openMocks(this);

        clearInvocations(questionService);

        allRepositories.assessmentRepository = assessmentRepository;
        allRepositories.assignmentRepository = assignmentRepository;
        allRepositories.experimentRepository = experimentRepository;
        allRepositories.exposureGroupConditionRepository = exposureGroupConditionRepository;
        allRepositories.participantRepository = participantRepository;
        allRepositories.questionRepository = questionRepository;
        allRepositories.treatmentRepository = treatmentRepository;

        when(assessmentRepository.findByAssessmentId(anyLong())).thenReturn(assessment);
        when(assessmentRepository.save(any(Assessment.class))).thenReturn(assessment);
        when(assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(anyLong(), anyString())).thenReturn(assignment);
        when(experimentRepository.findById(anyLong())).thenReturn(Optional.of(experiment));
        when(exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.of(exposureGroupCondition));
        when(exposureGroupConditionRepository.getByGroup_GroupIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.of(exposureGroupCondition));
        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(participant);
        when(questionRepository.findByAssessment_AssessmentIdOrderByQuestionOrder(anyLong())).thenReturn(Collections.emptyList());
        when(treatmentRepository.findByCondition_ConditionIdAndAssignment_AssignmentId(anyLong(), anyLong())).thenReturn(Collections.singletonList(treatment));
        when(treatmentRepository.findByTreatmentId(anyLong())).thenReturn(treatment);
        when(treatmentRepository.saveAndFlush(any(Treatment.class))).thenReturn(treatment);

        when(fileStorageService.parseHTMLFiles(anyString())).thenReturn(StringUtils.EMPTY);
        when(participantService.handleExperimentParticipant(any(Experiment.class), any(SecuredInfo.class))).thenReturn(participant);
        when(questionService.duplicateQuestionsForAssessment(anyLong(), anyLong())).thenReturn(Collections.singletonList(new QuestionDto()));
        when(questionService.findAllByAssessmentId(anyLong())).thenReturn(Collections.singletonList(question));
        when(questionService.getQuestion(anyLong())).thenReturn(question);
        when(questionService.postQuestion(any(QuestionDto.class), anyLong(), anyBoolean())).thenReturn(questionDto);
        when(questionService.save(any(Question.class))).thenReturn(question);
        when(submissionService.findByParticipantId(anyLong())).thenReturn(Collections.singletonList(submission));
        when(submissionService.getScoreFromMultipleSubmissions(any(Participant.class), any(Assessment.class))).thenReturn(1F);

        when(assessment.getAssessmentId()).thenReturn(1l);
        when(assessment.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.MOST_RECENT);
        when(assessment.getQuestions()).thenReturn(Collections.emptyList());
        when(assessment.getTreatment()).thenReturn(treatment);
        when(assignment.getAssignmentId()).thenReturn(1L);
        when(assignment.getExposure()).thenReturn(exposure);
        when(assessmentDto.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.MOST_RECENT.toString());
        when(assessmentDto.getQuestions()).thenReturn(Collections.singletonList(questionDto));
        when(assessmentDto.getTitle()).thenReturn("title");
        when(condition.getConditionId()).thenReturn(1L);
        when(condition.getDefaultCondition()).thenReturn(true);
        when(exposure.getExposureId()).thenReturn(1L);
        when(exposureGroupCondition.getCondition()).thenReturn(condition);
        when(exposureGroupCondition.getGroup()).thenReturn(group);
        when(group.getGroupId()).thenReturn(1L);
        when(participant.getConsent()).thenReturn(true);
        when(participant.getGroup()).thenReturn(group);
        when(participant.getParticipantId()).thenReturn(1L);
        when(treatment.getAssessment()).thenReturn(assessment);
        when(treatment.getAssignment()).thenReturn(assignment);
        when(treatment.getCondition()).thenReturn(condition);
        when(treatment.getTreatmentId()).thenReturn(1L);
        when(securedInfo.getCanvasAssignmentId()).thenReturn("canvasAssignmentId");
        when(securedInfo.getUserId()).thenReturn("canvasUserId");
        when(submission.getDateSubmitted()).thenReturn(Timestamp.from(Instant.now()));
    }

    @Test
    public void testDuplicateAssessment() throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, TreatmentNotMatchingException {
        AssessmentDto assessmentDto = assessmentService.duplicateAssessment(1L, 2L);

        assertNotNull(assessmentDto);
        assertEquals(1L, assessmentDto.getAssessmentId());
    }

    @Test
    public void testDuplicateAssessmentNotFound() throws IdInPostException, ExceedingLimitException, AssessmentNotMatchingException {
        when(assessmentRepository.findByAssessmentId(anyLong())).thenReturn(null);

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
        when(experimentRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(ExperimentNotMatchingException.class, () -> { assessmentService.viewAssessment(1l, securedInfo); });

        assertEquals(TextConstants.EXPERIMENT_NOT_MATCHING, exception.getMessage());
    }

    @Test
    public void testGetAssessmentByGroupId() throws AssessmentNotMatchingException {
        Assessment assessment = assessmentService.getAssessmentByGroupId(1L, "1", 1l);

        assertNotNull(assessment);
    }

    @Test
    public void testGetAssessmentByGroupIdNoAssignment() throws AssessmentNotMatchingException {
        when(assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(anyLong(), anyString())).thenReturn(null);
        Exception exception = assertThrows(AssessmentNotMatchingException.class, () -> { assessmentService.getAssessmentByGroupId(1L, "1", 1L); });

        assertEquals("Error 127: This assignment does not exist in Terracotta for this experiment", exception.getMessage());
    }

    @Test
    public void testGetAssessmentByGroupIdNoExposureGroupCondition() throws AssessmentNotMatchingException {
        when(exposureGroupConditionRepository.getByGroup_GroupIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.empty());
        Exception exception = assertThrows(AssessmentNotMatchingException.class, () -> { assessmentService.getAssessmentByGroupId(1L, "1", 1L); });

        assertEquals("Error 130: This assignment does not have a condition assigned for the participant group.", exception.getMessage());
    }

    @Test
    public void testGetAssessmentByConditionId() throws AssessmentNotMatchingException {
        Assessment assessment = assessmentService.getAssessmentByConditionId(1L, "1", 1l);

        assertNotNull(assessment);
    }

    @Test
    public void testGetAssessmentByConditionIdNoAssignment() throws AssessmentNotMatchingException {
        when(assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(anyLong(), anyString())).thenReturn(null);
        Exception exception = assertThrows(AssessmentNotMatchingException.class, () -> { assessmentService.getAssessmentByConditionId(1L, "1", 1l); });

        assertEquals("Error 127: This assignment does not exist in Terracotta for this experiment", exception.getMessage());
    }

    @Test
    public void testUpdateAssessmentWithNewQuestion()
        throws TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException,
        AssessmentNotMatchingException, IdInPostException, DataServiceException, NegativePointsException, QuestionNotMatchingException {
            when(questionService.findAllByAssessmentId(anyLong())).thenReturn(Collections.emptyList());
        when(questionDto.getQuestionId()).thenReturn(null);
        assessmentService.updateAssessment(1L, assessmentDto, true);

        verify(questionService).postQuestion(any(QuestionDto.class), anyLong(), anyBoolean());
        verify(questionService, never()).getQuestion(anyLong());
        verify(questionService, never()).updateQuestion(anyMap());
        verify(questionService, never()).deleteById(anyLong());
    }

    @Test
    public void testUpdateAssessmentWithExistingQuestion()
        throws TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException,
        AssessmentNotMatchingException, IdInPostException, DataServiceException, NegativePointsException, QuestionNotMatchingException {
        when(questionDto.getQuestionId()).thenReturn(1L);
        assessmentService.updateAssessment(1L, assessmentDto, true);

        verify(questionService, never()).postQuestion(any(QuestionDto.class), anyLong(), anyBoolean());
        verify(questionService).getQuestion(anyLong());
        verify(questionService).updateQuestion(anyMap());
        verify(questionService, never()).deleteById(anyLong());
    }

    @Test
    public void testUpdateAssessmentWithQuestionNotFound() throws QuestionNotMatchingException {
        when(questionService.getQuestion(anyLong())).thenReturn(null);
        Exception exception = assertThrows(QuestionNotMatchingException.class, () -> { assessmentService.updateAssessment(1L, assessmentDto, true); });

        assertEquals(TextConstants.QUESTION_NOT_MATCHING, exception.getMessage());
    }

    @Test
    public void testUpdateAssessmentWithDeletedQuestion()
        throws TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException,
        AssessmentNotMatchingException, IdInPostException, DataServiceException, NegativePointsException, QuestionNotMatchingException {
        when(assessmentDto.getQuestions()).thenReturn(Collections.emptyList());
        assessmentService.updateAssessment(1L, assessmentDto, true);

        verify(questionService, never()).postQuestion(any(QuestionDto.class), anyLong(), anyBoolean());
        verify(questionService, never()).getQuestion(anyLong());
        verify(questionService, never()).updateQuestion(anyMap());
        verify(questionService).deleteById(anyLong());
    }

    @Test
    public void testUpdateAssessmentWithDeletedQuestionNoExistingFound()
        throws TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException,
        AssessmentNotMatchingException, IdInPostException, DataServiceException, NegativePointsException, QuestionNotMatchingException {
        when(assessmentDto.getQuestions()).thenReturn(Collections.emptyList());
        when(questionService.findAllByAssessmentId(anyLong())).thenReturn(Collections.emptyList());
        assessmentService.updateAssessment(1L, assessmentDto, true);

        verify(questionService, never()).postQuestion(any(QuestionDto.class), anyLong(), anyBoolean());
        verify(questionService, never()).getQuestion(anyLong());
        verify(questionService, never()).updateQuestion(anyMap());
        verify(questionService, never()).deleteById(anyLong());
    }

}
