package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.app.AnswerEssaySubmission;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import edu.iu.terracotta.model.app.Group;
import edu.iu.terracotta.model.app.Outcome;
import edu.iu.terracotta.model.app.OutcomeScore;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.QuestionMc;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.enumerator.DistributionTypes;
import edu.iu.terracotta.model.app.enumerator.ExposureTypes;
import edu.iu.terracotta.model.app.enumerator.LmsType;
import edu.iu.terracotta.model.app.enumerator.MultipleSubmissionScoringScheme;
import edu.iu.terracotta.model.app.enumerator.ParticipationTypes;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import edu.iu.terracotta.model.app.enumerator.export.ExperimentCsv;
import edu.iu.terracotta.model.app.enumerator.export.ItemResponsesCsv;
import edu.iu.terracotta.model.app.enumerator.export.ItemsCsv;
import edu.iu.terracotta.model.app.enumerator.export.OutcomesCsv;
import edu.iu.terracotta.model.app.enumerator.export.ParticipantTreatmentCsv;
import edu.iu.terracotta.model.app.enumerator.export.ParticipantsCsv;
import edu.iu.terracotta.model.app.enumerator.export.ResponseOptionsCsv;
import edu.iu.terracotta.model.app.enumerator.export.SubmissionsCsv;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.repository.AnswerEssaySubmissionRepository;
import edu.iu.terracotta.repository.AnswerMcRepository;
import edu.iu.terracotta.repository.AssignmentRepository;
import edu.iu.terracotta.repository.ConditionRepository;
import edu.iu.terracotta.repository.EventRepository;
import edu.iu.terracotta.repository.ExperimentRepository;
import edu.iu.terracotta.repository.ExposureGroupConditionRepository;
import edu.iu.terracotta.repository.OutcomeScoreRepository;
import edu.iu.terracotta.repository.ParticipantRepository;
import edu.iu.terracotta.repository.QuestionRepository;
import edu.iu.terracotta.repository.QuestionSubmissionRepository;
import edu.iu.terracotta.repository.SubmissionRepository;
import edu.iu.terracotta.repository.TreatmentRepository;
import edu.iu.terracotta.service.app.OutcomeService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.aws.AWSService;

@Disabled("Test is broken and needs to be updated")
public class ExportServiceImplTest {

    @Spy
    @InjectMocks
    private ExportServiceImpl exportService;

    @Mock private AllRepositories allRepositories;
    @Mock private AnswerEssaySubmissionRepository answerEssaySubmissionRepository;
    @Mock private AnswerMcRepository answerMcRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private ConditionRepository conditionRepository;
    @Mock private EventRepository eventRepository;
    @Mock private ExperimentRepository experimentRepository;
    @Mock private ExposureGroupConditionRepository exposureGroupConditionRepository;
    @Mock private OutcomeScoreRepository outcomeScoreRepository;
    @Mock private ParticipantRepository participantRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionSubmissionRepository questionSubmissionRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private TreatmentRepository treatmentRepository;

    @Mock private AWSService awsService;
    @Mock private OutcomeService outcomeService;
    @Mock private SubmissionService submissionService;

    @Mock private AnswerEssaySubmission answerEssaySubmission;
    @Mock private AnswerMc answerMc;
    @Mock private Assessment assessment;
    @Mock private Assignment assignment;
    @Mock private Condition condition;
    @Mock private Environment env;
    @Mock private Experiment experiment;
    @Mock private Exposure exposure;
    @Mock private ExposureGroupCondition exposureGroupCondition;
    @Mock private Group group;
    @Mock private InputStream inputStream;
    @Mock private LtiContextEntity ltiContextEntity;
    @Mock private Outcome outcome;
    @Mock private OutcomeScore outcomeScore;
    @Mock private Participant participant;
    @Mock private Question question;
    @Mock private QuestionMc questionMc;
    @Mock private QuestionSubmission questionSubmission;
    @Mock SecuredInfo securedInfo;
    @Mock private Submission submission;
    @Mock private Treatment treatment;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        allRepositories.answerEssaySubmissionRepository = answerEssaySubmissionRepository;
        allRepositories.answerMcRepository = answerMcRepository;
        allRepositories.assignmentRepository = assignmentRepository;
        allRepositories.conditionRepository = conditionRepository;
        allRepositories.eventRepository = eventRepository;
        allRepositories.experimentRepository = experimentRepository;
        allRepositories.exposureGroupConditionRepository = exposureGroupConditionRepository;
        allRepositories.outcomeScoreRepository = outcomeScoreRepository;
        allRepositories.participantRepository = participantRepository;
        allRepositories.questionRepository = questionRepository;
        allRepositories.questionSubmissionRepository = questionSubmissionRepository;
        allRepositories.submissionRepository = submissionRepository;
        allRepositories.treatmentRepository = treatmentRepository;

        doReturn('X').when(exportService).mapResponsePosition(anyLong(), anyLong(), anyList());

        when(answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.singletonList(answerEssaySubmission));
        when(answerMcRepository.findByQuestion_Assessment_Treatment_Condition_Experiment_ExperimentId(anyLong(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(answerMc)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(assignmentRepository.findByExposure_ExposureIdAndSoftDeleted(1L, false)).thenReturn(Collections.singletonList(assignment));
        when(conditionRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(condition));
        when(eventRepository.findByParticipant_Experiment_ExperimentId(anyLong())).thenReturn(Collections.emptyList());
        when(experimentRepository.findByExperimentId(anyLong())).thenReturn(experiment);
        when(exposureGroupConditionRepository.findByGroup_GroupId(anyLong())).thenReturn(Collections.singletonList(exposureGroupCondition));
        when(exposureGroupConditionRepository.getByGroup_GroupIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(outcomeScoreRepository.findByOutcome_Exposure_Experiment_ExperimentId(anyLong(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(outcomeScore)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(participantRepository.findByExperiment_ExperimentId(anyLong(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(participant)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(questionRepository.findByAssessment_Treatment_Condition_Experiment_ExperimentId(anyLong(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(question)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(questionSubmissionRepository.findBySubmission_Participant_Experiment_ExperimentId(anyLong(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(questionSubmission)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(submissionRepository.findByParticipant_Experiment_ExperimentId(anyLong(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(submission)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(treatmentRepository.findByCondition_ConditionIdAndAssignment_AssignmentId(anyLong(), anyLong())).thenReturn(Collections.singletonList(treatment));

        when(awsService.readFileFromS3Bucket(anyString(), anyString())).thenReturn(inputStream);
        when(outcomeService.findAllByExperiment(anyLong())).thenReturn(Collections.singletonList(outcome));
        when(submissionService.getScoreFromMultipleSubmissions(any(Participant.class), any(Assessment.class))).thenReturn(null);

        when(answerEssaySubmission.getQuestionSubmission()).thenReturn(questionSubmission);
        when(answerMc.getAnswerMcId()).thenReturn(1L);
        when(answerMc.getCorrect()).thenReturn(true);
        when(answerMc.getQuestion()).thenReturn(questionMc);
        when(assessment.getTreatment()).thenReturn(treatment);
        when(assessment.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.MOST_RECENT);
        when(assignment.getAssignmentId()).thenReturn(1L);
        when(assignment.getTitle()).thenReturn("assignment1");
        when(condition.getConditionId()).thenReturn(1L);
        when(env.getProperty(anyString())).thenReturn("aws_string");
        when(experiment.getCreatedAt()).thenReturn(new Timestamp(System.currentTimeMillis()));
        when(experiment.getExperimentId()).thenReturn(1L);
        when(experiment.getExposureType()).thenReturn(ExposureTypes.BETWEEN);
        when(experiment.getLtiContextEntity()).thenReturn(ltiContextEntity);
        when(experiment.getDistributionType()).thenReturn(DistributionTypes.CUSTOM);
        when(experiment.getParticipationType()).thenReturn(ParticipationTypes.AUTO);
        when(exposure.getExposureId()).thenReturn(1L);
        when(exposureGroupCondition.getExposure()).thenReturn(exposure);
        when(exposureGroupCondition.getCondition()).thenReturn(condition);
        when(ltiContextEntity.getContextId()).thenReturn(1L);
        when(outcome.getExposure()).thenReturn(exposure);
        when(outcome.getLmsType()).thenReturn(LmsType.discussion_topic);
        when(outcome.getMaxPoints()).thenReturn(1F);
        when(outcome.getOutcomeId()).thenReturn(1L);
        when(outcomeScore.getOutcome()).thenReturn(outcome);
        when(outcomeScore.getParticipant()).thenReturn(participant);
        when(participant.getConsent()).thenReturn(true);
        when(participant.getExperiment()).thenReturn(experiment);
        when(participant.getDateGiven()).thenReturn(Timestamp.from(Instant.now()));
        when(participant.getGroup()).thenReturn(group);
        when(participant.getParticipantId()).thenReturn(1L);
        when(question.getAssessment()).thenReturn(assessment);
        when(question.getPoints()).thenReturn(1F);
        when(question.getQuestionId()).thenReturn(1L);
        when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);
        when(questionMc.isRandomizeAnswers()).thenReturn(true);
        when(questionSubmission.getAlteredGrade()).thenReturn(1F);
        when(questionSubmission.getCalculatedPoints()).thenReturn(1F);
        when(questionSubmission.getQuestion()).thenReturn(question);
        when(questionSubmission.getQuestionSubmissionId()).thenReturn(1L);
        when(questionSubmission.getSubmission()).thenReturn(submission);
        when(submission.getAlteredCalculatedGrade()).thenReturn(1F);
        when(submission.getAssessment()).thenReturn(assessment);
        when(submission.getCalculatedGrade()).thenReturn(1F);
        when(submission.getDateSubmitted()).thenReturn(Timestamp.from(Instant.now()));
        when(submission.getParticipant()).thenReturn(participant);
        when(submission.getSubmissionId()).thenReturn(1L);
        when(submission.getTotalAlteredGrade()).thenReturn(1F);
        when(treatment.getAssessment()).thenReturn(assessment);
        when(treatment.getAssignment()).thenReturn(assignment);
        when(treatment.getCondition()).thenReturn(condition);
        when(treatment.getTreatmentId()).thenReturn(1L);
    }

    @Test
    void testGetCsvFiles() throws CanvasApiException, ParticipantNotUpdatedException, IOException, ExperimentNotMatchingException, OutcomeNotMatchingException {
        ReflectionTestUtils.setField(exportService, "exportBatchSize", 1);

        Map<String, String> files = exportService.getFiles(1L, securedInfo);

        assertNotNull(files);
        assertEquals(10, files.size());
        assertTrue(files.containsKey(ExperimentCsv.FILENAME));
        assertTrue(files.containsKey(ItemResponsesCsv.FILENAME));
        assertTrue(files.containsKey(ItemsCsv.FILENAME));
        assertTrue(files.containsKey(OutcomesCsv.FILENAME));
        assertTrue(files.containsKey(ParticipantTreatmentCsv.FILENAME));
        assertTrue(files.containsKey(ParticipantsCsv.FILENAME));
        assertTrue(files.containsKey(ResponseOptionsCsv.FILENAME));
        assertTrue(files.containsKey(SubmissionsCsv.FILENAME));
        assertTrue(files.containsKey("events.json"));
    }

}
