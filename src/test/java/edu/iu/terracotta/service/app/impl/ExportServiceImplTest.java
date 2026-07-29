package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.events.Event;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.log.MessageLog;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.message.MessageConfiguration;
import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRule;
import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRuleSet;
import edu.iu.terracotta.dao.model.enums.FeatureType;
import edu.iu.terracotta.dao.model.enums.LmsType;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.dao.model.enums.export.EventPersonalIdentifiers;
import edu.iu.terracotta.dao.model.enums.export.ExperimentCsv;
import edu.iu.terracotta.dao.model.enums.export.ItemResponsesCsv;
import edu.iu.terracotta.dao.model.enums.export.ItemsCsv;
import edu.iu.terracotta.dao.model.enums.export.MessageConditions;
import edu.iu.terracotta.dao.model.enums.export.MessageContentCsv;
import edu.iu.terracotta.dao.model.enums.export.MessagesCsv;
import edu.iu.terracotta.dao.model.enums.export.OutcomesCsv;
import edu.iu.terracotta.dao.model.enums.export.ParticipantTreatmentCsv;
import edu.iu.terracotta.dao.model.enums.export.ParticipantsCsv;
import edu.iu.terracotta.dao.model.enums.export.ResponseOptionsCsv;
import edu.iu.terracotta.dao.model.enums.export.SubmissionsCsv;
import edu.iu.terracotta.dao.model.enums.messaging.MessageRecipientMatchType;
import edu.iu.terracotta.dao.model.enums.messaging.MessageType;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleComparison;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleOperator;
import edu.iu.terracotta.dao.repository.messaging.container.MessageContainerRepository;
import edu.iu.terracotta.dao.repository.messaging.log.MessageLogRepository;
import edu.iu.terracotta.service.app.FeatureService;
import edu.iu.terracotta.service.app.messaging.MessageContentService;

@SuppressWarnings({"PMD.LooseCoupling"})
public class ExportServiceImplTest extends BaseTest {

    // NOTE: none of these mocks exist on BaseServiceTest/BaseRepositoryTest/BaseModelTest, so declaring
    // them here does not create the field-shadowing/@InjectMocks ambiguity pitfall described for other
    // duplicated-name mocks in this codebase.
    // required so that @InjectMocks has a non-null candidate for this constructor parameter;
    // unstubbed calls fall back to Mockito's default empty-list answer, which is sufficient
    // for every test in this file (none exercise a non-empty message-container scenario).
    @Mock
    private MessageContainerRepository containerRepository;

    @Mock
    private MessageLogRepository messageLogRepository;

    @Mock
    private FeatureService featureService;

    @Mock
    private MessageContentService messageContentService;

    @Mock
    private Message message;

    @Mock
    private MessageContent messageContent;

    @Mock
    private MessageConfiguration messageConfiguration;

    @Mock
    private MessageRecipientRuleSet messageRecipientRuleSet;

    @Mock
    private MessageRecipientRule messageRecipientRule;

    @Mock
    private MessageLog messageLog;

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
            .thenReturn(List.of(participant))
            .thenReturn(List.of());
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

    @Test
    public void testCalculateFinalScoreCachesScoresPerAssessmentAcrossParticipants() {
        List<Participant> consentedParticipants = List.of(participant);
        Map<Long, Map<Long, Float>> scoresByAssessmentIdThenParticipantId = new HashMap<>();
        long participantId = participant.getParticipantId();

        when(submissionService.getScoresFromMultipleSubmissions(consentedParticipants, assessment)).thenReturn(Map.of(participantId, 5F));

        String firstScore = ReflectionTestUtils.invokeMethod(exportService, "calculateFinalScore", participant, assessment, consentedParticipants, scoresByAssessmentIdThenParticipantId);
        String secondScore = ReflectionTestUtils.invokeMethod(exportService, "calculateFinalScore", participant, assessment, consentedParticipants, scoresByAssessmentIdThenParticipantId);

        assertEquals("5.0", firstScore);
        assertEquals("5.0", secondScore);
        // second lookup for the same assessment reuses the cached batch instead of re-querying
        verify(submissionService, times(1)).getScoresFromMultipleSubmissions(consentedParticipants, assessment);
    }

    @Test
    public void testCalculateFinalScoreNoScore() {
        List<Participant> consentedParticipants = List.of(participant);
        Map<Long, Map<Long, Float>> scoresByAssessmentIdThenParticipantId = new HashMap<>();

        when(submissionService.getScoresFromMultipleSubmissions(consentedParticipants, assessment)).thenReturn(Collections.emptyMap());

        String score = ReflectionTestUtils.invokeMethod(exportService, "calculateFinalScore", participant, assessment, consentedParticipants, scoresByAssessmentIdThenParticipantId);

        assertEquals("N/A", score);
    }

    // ---------------------------------------------------------------------------------------------
    // calculateAttemptsAllowed / calculateTimeRequiredBetweenAttempts
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testCalculateAttemptsAllowedNullOrZeroIsUnlimited() {
        assertEquals("unlimited", ReflectionTestUtils.invokeMethod(exportService, "calculateAttemptsAllowed", (Integer) null));
        assertEquals("unlimited", ReflectionTestUtils.invokeMethod(exportService, "calculateAttemptsAllowed", 0));
    }

    @Test
    public void testCalculateAttemptsAllowedSpecificValue() {
        assertEquals("3", ReflectionTestUtils.invokeMethod(exportService, "calculateAttemptsAllowed", 3));
    }

    @Test
    public void testCalculateTimeRequiredBetweenAttemptsNullOrZeroIsNa() {
        assertEquals("N/A", ReflectionTestUtils.invokeMethod(exportService, "calculateTimeRequiredBetweenAttempts", (Float) null));
        assertEquals("N/A", ReflectionTestUtils.invokeMethod(exportService, "calculateTimeRequiredBetweenAttempts", 0F));
    }

    @Test
    public void testCalculateTimeRequiredBetweenAttemptsSpecificValue() {
        assertEquals("2.5 hours", ReflectionTestUtils.invokeMethod(exportService, "calculateTimeRequiredBetweenAttempts", 2.5F));
    }

    // ---------------------------------------------------------------------------------------------
    // handleExperimentCsv
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testHandleExperimentCsvBlankFieldsAndNotStarted() throws Exception {
        when(experimentRepository.findByExperimentId(anyLong())).thenReturn(experiment);
        when(conditionRepository.countByExperiment_ExperimentId(anyLong())).thenReturn(3L);

        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleExperimentCsv", 1L, 10L, 5L, files);

        String content = Files.readString(Path.of(files.get(ExperimentCsv.FILENAME)));
        assertTrue(content.contains("N/A"));
    }

    @Test
    public void testHandleExperimentCsvFilledFieldsAndStarted() throws Exception {
        when(experimentRepository.findByExperimentId(anyLong())).thenReturn(experiment);
        when(conditionRepository.countByExperiment_ExperimentId(anyLong())).thenReturn(3L);
        when(experiment.getTitle()).thenReturn("My Experiment");
        when(experiment.getDescription()).thenReturn("Description text");
        when(experiment.isStarted()).thenReturn(true);
        when(experiment.getStarted()).thenReturn(new Timestamp(System.currentTimeMillis()));

        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleExperimentCsv", 1L, 10L, 5L, files);

        String content = Files.readString(Path.of(files.get(ExperimentCsv.FILENAME)));
        assertTrue(content.contains("My Experiment"));
        assertTrue(content.contains("Description text"));
    }

    // ---------------------------------------------------------------------------------------------
    // handleOutcomesCsv
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testHandleOutcomesCsvWritesFallbackRowWhenParticipantHasNoGroup() throws Exception {
        when(participant.getGroup()).thenReturn(null);

        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleOutcomesCsv", 1L, securedInfo, files);

        verify(participantService).refreshParticipantsIfStale(1L);
        String content = Files.readString(Path.of(files.get(OutcomesCsv.FILENAME)));
        assertTrue(content.contains(outcome.getOutcomeId().toString()));
    }

    @Test
    public void testHandleOutcomesCsvWritesConditionRowWhenParticipantGroupIsAssigned() throws Exception {
        ReflectionTestUtils.setField(exportService, "exposureGroupConditions", List.of(exposureGroupCondition));

        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleOutcomesCsv", 1L, securedInfo, files);

        String content = Files.readString(Path.of(files.get(OutcomesCsv.FILENAME)));
        assertTrue(content.contains(condition.getName()));
        assertTrue(content.contains(condition.getConditionId().toString()));
    }

    @Test
    public void testHandleOutcomesCsvSkipsParticipantRefreshWhenNoOutcomes() throws Exception {
        when(outcomeRepository.findByExposure_Experiment_ExperimentId(anyLong(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(participant.getGroup()).thenReturn(null);

        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleOutcomesCsv", 1L, securedInfo, files);

        verify(participantService, never()).refreshParticipantsIfStale(anyLong());
        assertTrue(files.containsKey(OutcomesCsv.FILENAME));
    }

    // ---------------------------------------------------------------------------------------------
    // handleParticipantTreatmentCsv
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testHandleParticipantTreatmentCsvWritesRowAndAppendsOnSecondCall() throws Exception {
        Long participantId = participant.getParticipantId();
        when(submissionService.getScoresFromMultipleSubmissions(anyList(), any(Assessment.class))).thenReturn(Map.of(participantId, 4.5F));
        ReflectionTestUtils.setField(exportService, "exposureGroupConditions", List.of(exposureGroupCondition));
        ReflectionTestUtils.setField(exportService, "assignments", List.of(assignment));
        ReflectionTestUtils.setField(exportService, "treatments", List.of(treatment));

        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleParticipantTreatmentCsv", List.of(participant), files);
        String firstPath = files.get(ParticipantTreatmentCsv.FILENAME);
        ReflectionTestUtils.invokeMethod(exportService, "handleParticipantTreatmentCsv", List.of(participant), files);

        // second call must append to the same file rather than creating a new one / rewriting the header
        assertEquals(firstPath, files.get(ParticipantTreatmentCsv.FILENAME));
        String content = Files.readString(Path.of(firstPath));
        assertTrue(content.contains("4.5"));
        assertTrue(content.contains("unlimited"));
        assertEquals(3, content.lines().count());
    }

    @Test
    public void testHandleParticipantTreatmentCsvWritesNoRowsWhenParticipantNotConsented() throws Exception {
        when(participant.getConsent()).thenReturn(false);

        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleParticipantTreatmentCsv", List.of(participant), files);

        String content = Files.readString(Path.of(files.get(ParticipantTreatmentCsv.FILENAME)));
        assertEquals(1, content.lines().count());
    }

    // ---------------------------------------------------------------------------------------------
    // handleParticpantsCsv
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testHandleParticpantsCsvGroupAssignedUsesExperimentParticipationType() throws Exception {
        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleParticpantsCsv", List.of(participant), files);

        String content = Files.readString(Path.of(files.get(ParticipantsCsv.FILENAME)));
        assertTrue(content.contains("AUTO"));
    }

    @Test
    public void testHandleParticpantsCsvNoGroupUsesConsentedButNotAssigned() throws Exception {
        when(participant.getGroup()).thenReturn(null);

        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleParticpantsCsv", List.of(participant), files);

        String content = Files.readString(Path.of(files.get(ParticipantsCsv.FILENAME)));
        assertTrue(content.contains("CONSENTED_BUT_NOT_ASSIGNED"));
    }

    @Test
    public void testHandleParticpantsCsvFiltersOutNonConsentedParticipants() throws Exception {
        when(participant.getConsent()).thenReturn(false);

        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleParticpantsCsv", List.of(participant), files);

        String content = Files.readString(Path.of(files.get(ParticipantsCsv.FILENAME)));
        assertEquals(1, content.lines().count());
    }

    // ---------------------------------------------------------------------------------------------
    // handleSubmissionsCsv / handleItemsCsv / handleResponseOptionsCsv
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testHandleSubmissionsCsvWritesRowForConsentedNonTestParticipant() throws Exception {
        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleSubmissionsCsv", 1L, files);

        String content = Files.readString(Path.of(files.get(SubmissionsCsv.FILENAME)));
        assertTrue(content.contains(submission.getSubmissionId().toString()));
    }

    @Test
    public void testHandleItemsCsvWritesRowForEachQuestion() throws Exception {
        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleItemsCsv", 1L, files);

        String content = Files.readString(Path.of(files.get(ItemsCsv.FILENAME)));
        assertTrue(content.contains(question.getQuestionId().toString()));
    }

    @Test
    public void testHandleResponseOptionsCsvWritesRowForEachAnswer() throws Exception {
        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleResponseOptionsCsv", 1L, files);

        String content = Files.readString(Path.of(files.get(ResponseOptionsCsv.FILENAME)));
        assertTrue(content.contains(answerMc.getAnswerMcId().toString()));
    }

    // ---------------------------------------------------------------------------------------------
    // handleItemResponsesCsv
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testHandleItemResponsesCsvMcWithAnswerSubmission() throws Exception {
        when(questionSubmission.getQuestion()).thenReturn(questionMc);
        when(questionSubmission.getAnswerMcSubmissionOptions()).thenReturn(Collections.emptyList());
        when(answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(List.of(answerMcSubmission));
        when(answerMc.getHtml()).thenReturn("<p>answer</p>");

        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleItemResponsesCsv", 1L, files);

        String content = Files.readString(Path.of(files.get(ItemResponsesCsv.FILENAME)));
        assertTrue(content.contains("<p>answer</p>"));
        assertTrue(content.contains("TRUE"));
    }

    @Test
    public void testHandleItemResponsesCsvMcWithNoAnswerSubmissionStaysNa() throws Exception {
        when(questionSubmission.getQuestion()).thenReturn(questionMc);
        when(answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.emptyList());

        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleItemResponsesCsv", 1L, files);

        String content = Files.readString(Path.of(files.get(ItemResponsesCsv.FILENAME)));
        assertTrue(content.contains("N/A"));
    }

    @Test
    public void testHandleItemResponsesCsvEssayWithAnswerSubmission() throws Exception {
        when(answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(List.of(answerEssaySubmission));
        when(answerEssaySubmission.getResponse()).thenReturn("my essay response");

        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleItemResponsesCsv", 1L, files);

        String content = Files.readString(Path.of(files.get(ItemResponsesCsv.FILENAME)));
        assertTrue(content.contains("my essay response"));
    }

    @Test
    public void testHandleItemResponsesCsvEssayWithNoAnswerSubmissionStaysNa() throws Exception {
        when(answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.emptyList());

        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleItemResponsesCsv", 1L, files);

        assertTrue(files.containsKey(ItemResponsesCsv.FILENAME));
    }

    @Test
    public void testHandleItemResponsesCsvDefaultQuestionTypeStaysNa() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.PAGE_BREAK);

        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleItemResponsesCsv", 1L, files);

        String content = Files.readString(Path.of(files.get(ItemResponsesCsv.FILENAME)));
        assertTrue(content.contains("N/A"));
    }

    // ---------------------------------------------------------------------------------------------
    // handleMessagesCsv
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testHandleMessagesCsvReturnsEarlyWhenNoMessageLogs() throws Exception {
        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleMessagesCsv", 1L, "course-1", files);

        String content = Files.readString(Path.of(files.get(MessagesCsv.FILENAME)));
        assertEquals(1, content.lines().count());
    }

    @Test
    public void testHandleMessagesCsvWritesRowForMatchedParticipant() throws Exception {
        when(ltiUserEntity.getUserId()).thenReturn(42L);
        when(messageLog.getRecipient()).thenReturn(ltiUserEntity);
        when(messageLog.getMessage()).thenReturn(message);
        when(messageLog.getId()).thenReturn(10L);
        when(messageLog.getConditionId()).thenReturn(1L);
        when(messageLog.getConditionName()).thenReturn("Condition A");
        when(messageLog.getMessageSubject()).thenReturn("Subject line");
        when(messageLog.getCreatedAt()).thenReturn(new Timestamp(System.currentTimeMillis()));
        when(message.getId()).thenReturn(5L);
        when(message.getConfiguration()).thenReturn(messageConfiguration);
        when(messageConfiguration.getType()).thenReturn(MessageType.EMAIL);
        ReflectionTestUtils.setField(exportService, "messageLogs", List.of(messageLog));

        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleMessagesCsv", 1L, "course-1", files);

        String content = Files.readString(Path.of(files.get(MessagesCsv.FILENAME)));
        assertTrue(content.contains("Subject line"));
        assertTrue(content.contains("INCLUDED"));
    }

    @Test
    public void testHandleMessagesCsvSkipsUnmatchedRecipient() throws Exception {
        LtiUserEntity unknownUser = mock(LtiUserEntity.class);
        when(unknownUser.getUserId()).thenReturn(999L);
        when(messageLog.getRecipient()).thenReturn(unknownUser);
        ReflectionTestUtils.setField(exportService, "messageLogs", List.of(messageLog));

        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleMessagesCsv", 1L, "course-1", files);

        String content = Files.readString(Path.of(files.get(MessagesCsv.FILENAME)));
        assertEquals(1, content.lines().count());
    }

    // ---------------------------------------------------------------------------------------------
    // handleMessageContentCsv
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testHandleMessageContentCsvReturnsEarlyWhenNoMessages() throws Exception {
        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleMessageContentCsv", 1L, "course-1", files);

        String content = Files.readString(Path.of(files.get(MessageContentCsv.FILENAME)));
        assertEquals(1, content.lines().count());
    }

    @Test
    public void testHandleMessageContentCsvWritesRowForEachMessage() throws Exception {
        when(message.getId()).thenReturn(7L);
        when(message.getContent()).thenReturn(messageContent);
        when(messageContent.getHtml()).thenReturn("<p>body</p>");
        when(messageContentService.prepareBodyHtmlForExport("<p>body</p>")).thenReturn("sanitized body");
        ReflectionTestUtils.setField(exportService, "messages", List.of(message));

        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleMessageContentCsv", 1L, "course-1", files);

        String content = Files.readString(Path.of(files.get(MessageContentCsv.FILENAME)));
        assertTrue(content.contains("sanitized body"));
    }

    // ---------------------------------------------------------------------------------------------
    // handleMessageConditionsCsv
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testHandleMessageConditionsCsvReturnsEarlyWhenNoMessages() throws Exception {
        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleMessageConditionsCsv", 1L, "course-1", files);

        String content = Files.readString(Path.of(files.get(MessageConditions.FILENAME)));
        assertEquals(1, content.lines().count());
    }

    @Test
    public void testHandleMessageConditionsCsvWritesRowForEachRule() throws Exception {
        when(message.getId()).thenReturn(7L);
        when(message.getRuleSets()).thenReturn(List.of(messageRecipientRuleSet));
        when(message.getConfiguration()).thenReturn(messageConfiguration);
        when(messageConfiguration.getRecipientMatchType()).thenReturn(MessageRecipientMatchType.INCLUDE);
        when(messageRecipientRuleSet.getId()).thenReturn(3L);
        when(messageRecipientRuleSet.getOperator()).thenReturn(MessageRuleOperator.AND);
        when(messageRecipientRuleSet.getRules()).thenReturn(List.of(messageRecipientRule));
        when(messageRecipientRule.getId()).thenReturn(9L);
        when(messageRecipientRule.getOperator()).thenReturn(MessageRuleOperator.OR);
        when(messageRecipientRule.getLmsAssignmentId()).thenReturn("assign-1");
        when(messageRecipientRule.getComparison()).thenReturn(MessageRuleComparison.EQUALS);
        when(messageRecipientRule.getValue()).thenReturn("42");
        ReflectionTestUtils.setField(exportService, "messages", List.of(message));

        Map<String, String> files = new HashMap<>();
        ReflectionTestUtils.invokeMethod(exportService, "handleMessageConditionsCsv", 1L, "course-1", files);

        String content = Files.readString(Path.of(files.get(MessageConditions.FILENAME)));
        assertTrue(content.contains("assign-1"));
        assertTrue(content.contains("42"));
    }

    // ---------------------------------------------------------------------------------------------
    // getJsonFiles / removePersonalIdentifiersFromEvent
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testGetJsonFilesWritesSingleEvent() throws Exception {
        Map<String, String> files = new HashMap<>();
        exportService.getJsonFiles(1L, files);

        String content = Files.readString(Path.of(files.get(EventPersonalIdentifiers.FILENAME)));
        assertTrue(content.contains("ToolUseEvent"));
    }

    @Test
    public void testGetJsonFilesWritesCommaBetweenMultipleEvents() throws Exception {
        Event secondEvent = mock(Event.class);
        when(secondEvent.getParticipant()).thenReturn(participant);
        when(secondEvent.getJson()).thenReturn("{\"a\":1}");
        when(eventRepository.findByParticipant_Experiment_ExperimentId(anyLong(), any(Pageable.class)))
            .thenReturn(new PageImpl<Event>(List.of(event, secondEvent)))
            .thenReturn(new PageImpl<Event>(Collections.emptyList()));

        Map<String, String> files = new HashMap<>();
        exportService.getJsonFiles(1L, files);

        String content = Files.readString(Path.of(files.get(EventPersonalIdentifiers.FILENAME)));
        // both events' (re-serialized) JSON must be present, separated by the comma line the
        // "isFirstElement" check writes before every element after the first
        assertTrue(content.contains("ToolUseEvent"));
        assertTrue(content.contains("{\"a\":1}"));
        assertTrue(content.contains(","));
    }

    @Test
    public void testGetJsonFilesHandlesMalformedEventJsonGracefully() throws Exception {
        when(event.getJson()).thenReturn("{not valid json");

        Map<String, String> files = new HashMap<>();
        exportService.getJsonFiles(1L, files);

        String content = Files.readString(Path.of(files.get(EventPersonalIdentifiers.FILENAME)));
        assertTrue(content.contains("{}"));
    }

    // ---------------------------------------------------------------------------------------------
    // isEventExportAllowed
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testIsEventExportAllowed() {
        ReflectionTestUtils.setField(exportService, "eventsOutputEnabled", true);
        ReflectionTestUtils.setField(exportService, "consentedParticipantsCount", 10L);
        ReflectionTestUtils.setField(exportService, "eventsOutputParticipantThreshold", 400);
        assertTrue((boolean) ReflectionTestUtils.invokeMethod(exportService, "isEventExportAllowed"));

        ReflectionTestUtils.setField(exportService, "eventsOutputEnabled", false);
        assertFalse((boolean) ReflectionTestUtils.invokeMethod(exportService, "isEventExportAllowed"));

        ReflectionTestUtils.setField(exportService, "eventsOutputEnabled", true);
        ReflectionTestUtils.setField(exportService, "consentedParticipantsCount", 1000L);
        assertFalse((boolean) ReflectionTestUtils.invokeMethod(exportService, "isEventExportAllowed"));
    }

    // ---------------------------------------------------------------------------------------------
    // getReadMeFile
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testGetReadMeFileSkippedWhenDisabled() throws Exception {
        doCallRealMethod().when(exportService).getReadMeFile(anyMap());
        ReflectionTestUtils.setField(exportService, "exportReadmeEnabled", false);

        Map<String, String> files = new HashMap<>();
        exportService.getReadMeFile(files);

        assertTrue(files.isEmpty());
        verify(awsService, never()).readFileFromS3Bucket(anyString(), anyString());
    }

    @Test
    public void testGetReadMeFileDownloadsReadmeWhenEnabled() throws Exception {
        doCallRealMethod().when(exportService).getReadMeFile(anyMap());
        ReflectionTestUtils.setField(exportService, "exportReadmeEnabled", true);
        when(inputStream.read()).thenReturn(-1);
        when(inputStream.read(any(byte[].class))).thenReturn(-1);
        when(inputStream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(-1);
        when(environment.getProperty("aws.bucket-name")).thenReturn("bucket");
        when(environment.getProperty("aws.object-key")).thenReturn("readme.txt");

        Map<String, String> files = new HashMap<>();
        exportService.getReadMeFile(files);

        assertTrue(files.containsKey("readme.txt"));
        verify(awsService).readFileFromS3Bucket("bucket", "readme.txt");
    }

    // ---------------------------------------------------------------------------------------------
    // prepareData
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testPrepareDataSkipsAssignmentTreatmentLookupWhenNoAssignments() throws Exception {
        when(assignmentRepository.findByExposure_Experiment_ExperimentId(anyLong())).thenReturn(List.of());

        ReflectionTestUtils.invokeMethod(exportService, "prepareData", 1L, securedInfo);

        verify(ltiUserRepository, never()).findFirstByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong());
        verify(assignmentTreatmentService, never()).setAssignmentDtoAttrs(any(Assignment.class), anyString(), any(LtiUserEntity.class));
    }

    @Test
    public void testPrepareDataCallsAssignmentTreatmentServiceWhenAssignmentsExist() throws Exception {
        when(assignmentRepository.findByExposure_Experiment_ExperimentId(anyLong())).thenReturn(List.of(assignment));
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong())).thenReturn(ltiUserEntity);

        ReflectionTestUtils.invokeMethod(exportService, "prepareData", 1L, securedInfo);

        verify(assignmentTreatmentService).setAssignmentDtoAttrs(assignment, securedInfo.getLmsCourseId(), ltiUserEntity);
        assertEquals(List.of(assignment), ReflectionTestUtils.getField(exportService, "assignments"));
    }

    @Test
    public void testPrepareDataLogsWarningWhenAssignmentTreatmentServiceThrows() throws Exception {
        when(assignmentRepository.findByExposure_Experiment_ExperimentId(anyLong())).thenReturn(List.of(assignment));
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong())).thenReturn(ltiUserEntity);
        doThrow(new ApiException("boom")).when(assignmentTreatmentService).setAssignmentDtoAttrs(any(Assignment.class), anyString(), any(LtiUserEntity.class));

        ReflectionTestUtils.invokeMethod(exportService, "prepareData", 1L, securedInfo);

        verify(assignmentTreatmentService).setAssignmentDtoAttrs(assignment, securedInfo.getLmsCourseId(), ltiUserEntity);
    }

    // ---------------------------------------------------------------------------------------------
    // getFiles (end-to-end orchestration)
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testGetFilesHappyPathWithMessagingAndEventsEnabled() throws Exception {
        when(experimentRepository.findByExperimentId(anyLong())).thenReturn(experiment);
        when(conditionRepository.countByExperiment_ExperimentId(anyLong())).thenReturn(2L);
        when(featureService.isFeatureEnabled(eq(FeatureType.MESSAGING), anyLong())).thenReturn(true);

        Map<String, String> files = exportService.getFiles(1L, securedInfo);

        assertTrue(files.containsKey(ExperimentCsv.FILENAME));
        assertTrue(files.containsKey(ParticipantTreatmentCsv.FILENAME));
        assertTrue(files.containsKey(ParticipantsCsv.FILENAME));
        assertTrue(files.containsKey(OutcomesCsv.FILENAME));
        assertTrue(files.containsKey(SubmissionsCsv.FILENAME));
        assertTrue(files.containsKey(ItemsCsv.FILENAME));
        assertTrue(files.containsKey(ItemResponsesCsv.FILENAME));
        assertTrue(files.containsKey(ResponseOptionsCsv.FILENAME));
        assertTrue(files.containsKey(MessagesCsv.FILENAME));
        assertTrue(files.containsKey(MessageContentCsv.FILENAME));
        assertTrue(files.containsKey(MessageConditions.FILENAME));
        assertTrue(files.containsKey(EventPersonalIdentifiers.FILENAME));
        verify(featureService).isFeatureEnabled(FeatureType.MESSAGING, securedInfo.getPlatformDeploymentId());
    }

    @Test
    public void testGetFilesSkipsMessagingAndEventsWhenDisabled() throws Exception {
        when(experimentRepository.findByExperimentId(anyLong())).thenReturn(experiment);
        when(conditionRepository.countByExperiment_ExperimentId(anyLong())).thenReturn(2L);
        when(featureService.isFeatureEnabled(eq(FeatureType.MESSAGING), anyLong())).thenReturn(false);
        ReflectionTestUtils.setField(exportService, "eventsOutputEnabled", false);

        Map<String, String> files = exportService.getFiles(1L, securedInfo);

        assertTrue(files.containsKey(ExperimentCsv.FILENAME));
        assertFalse(files.containsKey(MessagesCsv.FILENAME));
        assertFalse(files.containsKey(MessageContentCsv.FILENAME));
        assertFalse(files.containsKey(MessageConditions.FILENAME));
        assertFalse(files.containsKey(EventPersonalIdentifiers.FILENAME));
    }

}
