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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.BaseTest;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.enumerator.LmsType;
import edu.iu.terracotta.model.app.enumerator.MultipleSubmissionScoringScheme;
import edu.iu.terracotta.model.app.enumerator.export.ExperimentCsv;
import edu.iu.terracotta.model.app.enumerator.export.ItemResponsesCsv;
import edu.iu.terracotta.model.app.enumerator.export.ItemsCsv;
import edu.iu.terracotta.model.app.enumerator.export.OutcomesCsv;
import edu.iu.terracotta.model.app.enumerator.export.ParticipantTreatmentCsv;
import edu.iu.terracotta.model.app.enumerator.export.ParticipantsCsv;
import edu.iu.terracotta.model.app.enumerator.export.ResponseOptionsCsv;
import edu.iu.terracotta.model.app.enumerator.export.SubmissionsCsv;

@Disabled("Test is broken and needs to be updated")
public class ExportServiceImplTest extends BaseTest {

    @Spy
    @InjectMocks
    private ExportServiceImpl exportService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        doReturn('X').when(exportService).mapResponsePosition(anyLong(), anyLong(), anyList());

        when(answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.singletonList(answerEssaySubmission));
        when(answerMcRepository.findByQuestion_Assessment_Treatment_Condition_Experiment_ExperimentId(anyLong(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(answerMc)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(assignmentRepository.findByExposure_ExposureIdAndSoftDeleted(1L, false)).thenReturn(Collections.singletonList(assignment));
        when(conditionRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(condition));
        when(eventRepository.findByParticipant_Experiment_ExperimentId(anyLong())).thenReturn(Collections.emptyList());
        when(exposureGroupConditionRepository.findByGroup_GroupId(anyLong())).thenReturn(Collections.singletonList(exposureGroupCondition));
        when(exposureGroupConditionRepository.getByGroup_GroupIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(outcomeRepository.findByExposure_Experiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(outcome));
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
        when(submissionService.getScoreFromMultipleSubmissions(any(Participant.class), any(Assessment.class))).thenReturn(null);

        when(assessment.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.MOST_RECENT);
        when(environment.getProperty(anyString())).thenReturn("aws_string");
        when(experiment.getCreatedAt()).thenReturn(new Timestamp(System.currentTimeMillis()));


        when(outcome.getLmsType()).thenReturn(LmsType.discussion_topic);
        when(participant.getDateGiven()).thenReturn(Timestamp.from(Instant.now()));
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
