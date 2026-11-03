package edu.iu.terracotta.service.caliper.impl;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.caliper.dao.model.enums.Action;
import edu.iu.terracotta.connectors.caliper.dao.model.enums.EntityType;
import edu.iu.terracotta.dao.entity.SubmissionComment;
import edu.iu.terracotta.dao.entity.events.Event;
import edu.iu.terracotta.dao.model.dto.media.MediaEventDto;
import edu.iu.terracotta.dao.model.dto.media.MediaLocationDto;
import edu.iu.terracotta.dao.model.dto.media.MediaObjectDto;
import edu.iu.terracotta.utils.LtiStrings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Compares the Caliper JSON each event produces against JSON recorded from the caliper-java 1.1.2 library
 * this code replaced, so whatever reads these events keeps getting the same documents. Only the random event
 * id and the times taken from the clock are left out of the comparison.
 *
 * After an intentional change to the events, run with -Dcaliper.golden.record=true to record them again from the
 * current code.
 */
public class CaliperJsonCompatibilityTest extends BaseTest {

    private static final Path GOLDEN_DIRECTORY = Path.of("src/test/resources/caliper/golden");
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @InjectMocks
    private CaliperServiceImpl caliperService;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        ReflectionTestUtils.setField(caliperService, "caliperSend", false);
        ReflectionTestUtils.setField(caliperService, "caliperDB", true);
        ReflectionTestUtils.setField(caliperService, "applicationName", "Terracotta");
        ReflectionTestUtils.setField(caliperService, "applicationUrl", "https://terracotta.example.edu/app");
        caliperService.init();

        when(platformDeployment.getBaseUrl()).thenReturn("https://canvas.example.edu");
        when(platformDeployment.getLocalUrl()).thenReturn("https://terracotta.example.edu");
        when(ltiUserEntity.getUserId()).thenReturn(42L);
        when(ltiUserEntity.getUserKey()).thenReturn("lti-user-key");
        when(ltiMembershipEntity.getContext()).thenReturn(ltiContextEntity);
        when(ltiMembershipEntity.getRole()).thenReturn(LtiStrings.ROLE_STUDENT);
        when(ltiContextEntity.getTitle()).thenReturn("Biology 101");
        when(ltiContextEntity.getContextKey()).thenReturn("context-key");
        when(participant.getParticipantId()).thenReturn(7L);
        when(participant.getDropped()).thenReturn(false);
        when(experiment.getClosed()).thenReturn(null);

        when(securedInfo.getLmsUserGlobalId()).thenReturn("global-id");
        when(securedInfo.getLmsCourseId()).thenReturn("101");
        when(securedInfo.getLmsAssignmentId()).thenReturn("202");
        when(securedInfo.getLmsUserId()).thenReturn("303");
        when(securedInfo.getLmsLoginId()).thenReturn("student@example.edu");
        when(securedInfo.getLmsUserName()).thenReturn("Pat Student");
        when(securedInfo.getRoles()).thenReturn(List.of("Learner"));
        when(securedInfo.getNonce()).thenReturn("nonce-1");

        when(assessment.getTitle()).thenReturn("Quiz 1");
        when(assessment.getNumOfSubmissions()).thenReturn(3);
        when(assessment.getVersion()).thenReturn(4);
        when(assessmentSubmissionService.calculateMaxScore(any())).thenReturn(10.0F);
        when(treatment.getTreatmentId()).thenReturn(5L);
        when(treatment.getAssignment()).thenReturn(assignment);
        when(treatment.getCondition()).thenReturn(condition);

        when(submission.getSubmissionId()).thenReturn(11L);
        when(submission.getCreatedAt()).thenReturn(Timestamp.from(Instant.parse("2026-10-01T12:00:00Z")));
        when(submission.getDateSubmitted()).thenReturn(Timestamp.from(Instant.parse("2026-10-01T12:30:00Z")));
        when(submission.getTotalAlteredGrade()).thenReturn(7.25F);
        when(submission.isIntegration()).thenReturn(false);
        when(submissionRepository.findByParticipant_IdAndAssessment_AssessmentId(anyLong(), anyLong())).thenReturn(List.of(submission, submission));
        when(exposureGroupConditionRepository.getByGroup_GroupIdAndCondition_ConditionId(any(), any())).thenReturn(Optional.of(exposureGroupCondition));
    }

    @Test
    public void testAssignmentStarted() throws IOException {
        caliperService.sendAssignmentStarted(submission, securedInfo);

        assertMatchesGolden("assignment-started");
    }

    @Test
    public void testAssignmentSubmittedByInstructorForIntegration() throws IOException {
        when(ltiMembershipEntity.getRole()).thenReturn(LtiStrings.ROLE_INSTRUCTOR);
        when(submission.isIntegration()).thenReturn(true);
        when(integrationToken.getToken()).thenReturn("launch-token");
        when(participant.getDropped()).thenReturn(true);

        caliperService.sendAssignmentSubmitted(submission, securedInfo);

        assertMatchesGolden("assignment-submitted-integration");
    }

    @Test
    public void testAssignmentRestartedWithUnknownRoleAndNoGroup() throws IOException {
        when(ltiMembershipEntity.getRole()).thenReturn(99);
        when(participant.getGroup()).thenReturn(null);
        when(securedInfo.getLmsAssignmentId()).thenReturn("$Canvas.assignment.id");
        when(assessment.getNumOfSubmissions()).thenReturn(null);
        when(assessmentSubmissionService.calculateMaxScore(any())).thenReturn(12.345F);

        caliperService.sendAssignmentRestarted(submission, securedInfo);

        assertMatchesGolden("assignment-restarted");
    }

    @Test
    public void testMediaEventWithExtensions() throws IOException {
        Map<String, Object> extensions = new LinkedHashMap<>();
        extensions.put("playback_rate", 1.5);
        extensions.put("source", "player");

        caliperService.sendMediaEvent(buildMediaEventDto(Action.PAUSED, extensions), participant, securedInfo, submission, 9L);

        assertMatchesGolden("media-paused");
    }

    @Test
    public void testMediaEventWithoutExtensions() throws IOException {
        caliperService.sendMediaEvent(buildMediaEventDto(Action.CHANGED_SPEED, null), participant, securedInfo, submission, 9L);

        assertMatchesGolden("media-changed-speed");
    }

    @Test
    public void testViewGradeWithComments() throws IOException {
        SubmissionComment first = mock(SubmissionComment.class);
        when(first.getComment()).thenReturn("Nice work");
        when(first.getCreator()).thenReturn("Teacher One");
        SubmissionComment second = mock(SubmissionComment.class);
        when(second.getComment()).thenReturn("See me");
        when(second.getCreator()).thenReturn("Teacher Two");
        when(submission.getSubmissionComments()).thenReturn(List.of(first, second));

        caliperService.sendViewGradeEvent(submission, securedInfo);

        assertMatchesGolden("view-grade-comments");
    }

    @Test
    public void testViewGradeWithoutComments() throws IOException {
        when(submission.getSubmissionComments()).thenReturn(Collections.emptyList());
        when(submission.getTotalAlteredGrade()).thenReturn(0F);

        caliperService.sendViewGradeEvent(submission, securedInfo);

        assertMatchesGolden("view-grade-zero");
    }

    @Test
    public void testToolUse() throws IOException {
        caliperService.sendToolUseEvent(ltiMembershipEntity, "global-id", "101", "303", "student@example.edu", List.of("Learner", "Student"), "Pat Student");

        assertMatchesGolden("tool-use");
    }

    private MediaEventDto buildMediaEventDto(Action action, Map<String, Object> extensions) {
        MediaObjectDto object = MediaObjectDto.builder()
            .name("Lecture video")
            .type(EntityType.VIDEO_OBJECT)
            .mediaType("video/mp4")
            .duration("PT2M")
            .build();
        object.setId("https://media.example.edu/videos/1");

        MediaLocationDto target = MediaLocationDto.builder()
            .type(EntityType.MEDIA_LOCATION)
            .currentTime("PT30S")
            .build();
        target.setId("https://media.example.edu/videos/1#t=30");

        return MediaEventDto.builder()
            .id("media-event-1")
            .profile("MediaProfile")
            .action(action)
            .eventTime(Timestamp.from(Instant.parse("2026-10-01T12:05:00Z")))
            .object(object)
            .target(target)
            .extensions(extensions)
            .build();
    }

    private void assertMatchesGolden(String name) throws IOException {
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        JsonNode actual = withoutClockValues(MAPPER.readTree(captor.getValue().getJson()));
        Path golden = GOLDEN_DIRECTORY.resolve(name + ".json");

        if (Boolean.getBoolean("caliper.golden.record")) {
            Files.createDirectories(GOLDEN_DIRECTORY);
            Files.writeString(golden, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(actual) + "\n", StandardCharsets.UTF_8);
            return;
        }

        assertEquals(MAPPER.readTree(Files.readString(golden)), actual, name);
    }

    // the event id is random, and the send time and most event times come from the clock
    private JsonNode withoutClockValues(JsonNode envelope) {
        ((ObjectNode) envelope).put("sendTime", "<now>");
        ObjectNode event = (ObjectNode) envelope.get("data").get(0);
        event.put("id", event.get("id").asString().startsWith("urn:uuid:") ? "urn:uuid:<random>" : event.get("id").asString());

        if (!"MediaEvent".equals(event.get("type").asString())) {
            event.put("eventTime", "<now>");
        }

        return envelope;
    }

}
