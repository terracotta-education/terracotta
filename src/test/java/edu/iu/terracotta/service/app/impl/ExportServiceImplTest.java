package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
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
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import edu.iu.terracotta.model.app.enumerator.export.ExperimentCsv;
import edu.iu.terracotta.model.app.enumerator.export.ItemResponsesCsv;
import edu.iu.terracotta.model.app.enumerator.export.ItemsCsv;
import edu.iu.terracotta.model.app.enumerator.export.OutcomesCsv;
import edu.iu.terracotta.model.app.enumerator.export.ParticipantTreatmentCsv;
import edu.iu.terracotta.model.app.enumerator.export.ParticipantsCsv;
import edu.iu.terracotta.model.app.enumerator.export.ResponseOptionsCsv;
import edu.iu.terracotta.model.app.enumerator.export.SubmissionsCsv;
import edu.iu.terracotta.model.events.Event;

public class ExportServiceImplTest extends BaseTest {

    @Spy
    @InjectMocks
    private ExportServiceImpl exportService;

    @BeforeEach
    public void beforeEach() throws IOException {
        MockitoAnnotations.openMocks(this);

        setup();

        ReflectionTestUtils.setField(exportService, "exportBatchSize", 50);
        ReflectionTestUtils.setField(exportService, "eventsOutputEnabled", true);
        ReflectionTestUtils.setField(exportService, "eventsOutputParticipantThreshold", 400);

        doNothing().when(exportService).getReadMeFile(anyMap());
        doReturn('X').when(exportService).mapResponsePosition(anyLong(), anyLong(), anyList());

        when(answerMcRepository.findByQuestion_Assessment_Treatment_Condition_Experiment_ExperimentId(anyLong(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(answerMc)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(eventRepository.findByParticipant_Experiment_ExperimentId(anyLong(), any(Pageable.class)))
            .thenReturn(new PageImpl<Event>(Collections.singletonList(event)))
            .thenReturn(new PageImpl<Event>(Collections.emptyList()));
        when(exposureGroupConditionRepository.getByGroup_GroupIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(outcomeScoreRepository.findByOutcome_Exposure_Experiment_ExperimentId(anyLong(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(outcomeScore)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(outcomeRepository.findByExposure_Experiment_ExperimentId(anyLong(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.singletonList(outcome)))
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

        when(awsService.readFileFromS3Bucket(anyString(), anyString())).thenReturn(inputStream);
        when(submissionService.getScoreFromMultipleSubmissions(any(Participant.class), any(Assessment.class))).thenReturn(null);

        when(environment.getProperty(anyString())).thenReturn("aws_string");
        when(outcome.getLmsType()).thenReturn(LmsType.discussion_topic);
    }

    @Test
    void testGetCsvFiles() throws CanvasApiException, ParticipantNotUpdatedException, IOException, ExperimentNotMatchingException, OutcomeNotMatchingException {
        Map<String, String> files = exportService.getFiles(1L, securedInfo);

        assertNotNull(files);
        assertEquals(9, files.size()); // NOTE: no README file here
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

    @Test
    void testGetCsvFilesMCQuestionType() throws CanvasApiException, ParticipantNotUpdatedException, IOException, ExperimentNotMatchingException, OutcomeNotMatchingException {
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);

        Map<String, String> files = exportService.getFiles(1L, securedInfo);

        assertNotNull(files);
        assertEquals(9, files.size()); // NOTE: no README file here
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

    @Test
    void testGetCsvFilesEventsNotEnabled() throws CanvasApiException, ParticipantNotUpdatedException, IOException, ExperimentNotMatchingException, OutcomeNotMatchingException {
        ReflectionTestUtils.setField(exportService, "eventsOutputEnabled", false);
        Map<String, String> files = exportService.getFiles(1L, securedInfo);

        assertNotNull(files);
        assertEquals(8, files.size()); // NOTE: no README file here
        assertTrue(files.containsKey(ExperimentCsv.FILENAME));
        assertTrue(files.containsKey(ItemResponsesCsv.FILENAME));
        assertTrue(files.containsKey(ItemsCsv.FILENAME));
        assertTrue(files.containsKey(OutcomesCsv.FILENAME));
        assertTrue(files.containsKey(ParticipantTreatmentCsv.FILENAME));
        assertTrue(files.containsKey(ParticipantsCsv.FILENAME));
        assertTrue(files.containsKey(ResponseOptionsCsv.FILENAME));
        assertTrue(files.containsKey(SubmissionsCsv.FILENAME));
        assertFalse(files.containsKey("events.json"));
    }

    @Test
    void testGetCsvFilesEventMaxThresholdExceeded() throws CanvasApiException, ParticipantNotUpdatedException, IOException, ExperimentNotMatchingException, OutcomeNotMatchingException {
        ReflectionTestUtils.setField(exportService, "eventsOutputParticipantThreshold", 0);
        Map<String, String> files = exportService.getFiles(1L, securedInfo);

        assertNotNull(files);
        assertEquals(8, files.size()); // NOTE: no README file here
        assertTrue(files.containsKey(ExperimentCsv.FILENAME));
        assertTrue(files.containsKey(ItemResponsesCsv.FILENAME));
        assertTrue(files.containsKey(ItemsCsv.FILENAME));
        assertTrue(files.containsKey(OutcomesCsv.FILENAME));
        assertTrue(files.containsKey(ParticipantTreatmentCsv.FILENAME));
        assertTrue(files.containsKey(ParticipantsCsv.FILENAME));
        assertTrue(files.containsKey(ResponseOptionsCsv.FILENAME));
        assertTrue(files.containsKey(SubmissionsCsv.FILENAME));
        assertFalse(files.containsKey("events.json"));
    }

    @Test
    public void testMapResponsePosition() {
        doCallRealMethod().when(exportService).mapResponsePosition(anyLong(), anyLong(), anyList());
        char retVal = exportService.mapResponsePosition(1L, 1L, Collections.singletonList(answerMcSubmissionOption));

        assertEquals('A', retVal);
    }

    @Test
    public void testMapResponsePositionNoAnswerList() {
        doCallRealMethod().when(exportService).mapResponsePosition(anyLong(), anyLong());
        char retVal = exportService.mapResponsePosition(1L, 1L);

        assertEquals('X', retVal);
    }

    
}
