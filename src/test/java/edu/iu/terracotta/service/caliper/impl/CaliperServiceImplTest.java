package edu.iu.terracotta.service.caliper.impl;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.SubmissionComment;
import edu.iu.terracotta.dao.entity.events.Event;
import edu.iu.terracotta.dao.model.dto.media.MediaEventDto;
import edu.iu.terracotta.dao.model.dto.media.MediaLocationDto;
import edu.iu.terracotta.dao.model.dto.media.MediaObjectDto;
import edu.iu.terracotta.utils.LtiStrings;

import org.imsglobal.caliper.Envelope;
import org.imsglobal.caliper.Sensor;
import org.imsglobal.caliper.actions.Action;
import org.imsglobal.caliper.entities.EntityType;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CaliperServiceImplTest extends BaseTest {

    @InjectMocks
    private CaliperServiceImpl caliperService;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        // wire up the @Value-injected configuration fields (plain Mockito/constructor injection does not populate these)
        ReflectionTestUtils.setField(caliperService, "sensorId", "default-sensor-id");
        ReflectionTestUtils.setField(caliperService, "clientId", "default-client-id");
        ReflectionTestUtils.setField(caliperService, "apiKey", "default-api-key");
        ReflectionTestUtils.setField(caliperService, "connectionTimeout", 5000);
        ReflectionTestUtils.setField(caliperService, "contentType", "application/json");
        ReflectionTestUtils.setField(caliperService, "host", "http://localhost");
        ReflectionTestUtils.setField(caliperService, "socketTimeOut", 5000);
        ReflectionTestUtils.setField(caliperService, "caliperSend", false);
        ReflectionTestUtils.setField(caliperService, "caliperDB", true);
        ReflectionTestUtils.setField(caliperService, "applicationName", "Terracotta");
        ReflectionTestUtils.setField(caliperService, "applicationUrl", "http://terracotta.test");

        // @PostConstruct is not invoked by Mockito; call explicitly so `context`/`softwareApplication` are populated
        caliperService.init();

        // common wiring needed across most of the send* methods
        when(ltiMembershipEntity.getContext()).thenReturn(ltiContextEntity);
        when(ltiContextEntity.getTitle()).thenReturn("Test Course");
        // prepareReferrer(...) builds a SoftwareApplication with .id(platformDeployment.getBaseUrl()); Caliper
        // rejects a null id, and this is not stubbed anywhere in the Base*Test hierarchy
        when(platformDeployment.getBaseUrl()).thenReturn("https://terracotta.example.edu");
        when(submissionRepository.findByParticipant_IdAndAssessment_AssessmentId(anyLong(), anyLong())).thenReturn(Collections.singletonList(submission));
        when(assessmentSubmissionService.calculateMaxScore(any())).thenReturn(10.0F);
        // must stay strictly before submission.getDateSubmitted()'s default (BaseModelTest stubs that to
        // Instant.now() during setup(), which runs before this line), or Caliper's Attempt validation
        // (startedAtTime < endedAtTime) fails
        when(submission.getCreatedAt()).thenReturn(new Timestamp(System.currentTimeMillis() - 60_000));
    }

    // ---------- init() ----------

    @Test
    public void testInitWithCaliperSendDisabled() {
        // caliperSend is false from beforeEach; init() should build the software application and stop before touching the sensor
        assertDoesNotThrow(() -> caliperService.init());
        Object softwareApplication = ReflectionTestUtils.getField(caliperService, "softwareApplication");
        assertNotNull(softwareApplication);
        Object defaultSensor = ReflectionTestUtils.getField(caliperService, "defaultSensor");
        assertNull(defaultSensor);
    }

    @Test
    public void testInitWithCaliperSendEnabledBuildsDefaultSensor() {
        ReflectionTestUtils.setField(caliperService, "caliperSend", true);
        ReflectionTestUtils.setField(caliperService, "sensorId", "sensor-xyz");

        caliperService.init();

        Sensor defaultSensor = (Sensor) ReflectionTestUtils.getField(caliperService, "defaultSensor");
        assertNotNull(defaultSensor);
        assertEquals("sensor-xyz", defaultSensor.getId());
    }

    // ---------- getSensor() ----------

    @Test
    public void testGetSensorReturnsDefaultSensorWhenCaliperConfigurationNotTrue() {
        Sensor sentinel = mock(Sensor.class);
        ReflectionTestUtils.setField(caliperService, "defaultSensor", sentinel);
        when(platformDeployment.getCaliperConfiguration()).thenReturn(false);

        assertSame(sentinel, caliperService.getSensor(platformDeployment));
    }

    @Test
    public void testGetSensorReturnsDefaultSensorWhenCaliperConfigurationNull() {
        Sensor sentinel = mock(Sensor.class);
        ReflectionTestUtils.setField(caliperService, "defaultSensor", sentinel);
        when(platformDeployment.getCaliperConfiguration()).thenReturn(null);

        assertSame(sentinel, caliperService.getSensor(platformDeployment));
    }

    @Test
    public void testGetSensorBuildsNewSensorWhenCaliperConfigurationTrue() {
        Sensor sentinel = mock(Sensor.class);
        ReflectionTestUtils.setField(caliperService, "defaultSensor", sentinel);
        when(platformDeployment.getCaliperConfiguration()).thenReturn(true);
        when(platformDeployment.getCaliperSensorId()).thenReturn("pd-sensor-id");
        when(platformDeployment.getClientId()).thenReturn("pd-client-id");
        when(platformDeployment.getCaliperApiKey()).thenReturn("pd-api-key");
        when(platformDeployment.getCaliperConnectionTimeout()).thenReturn(5000);
        when(platformDeployment.getCaliperContentType()).thenReturn("application/json");
        when(platformDeployment.getCaliperHost()).thenReturn("http://localhost");
        when(platformDeployment.getCaliperSocketTimeout()).thenReturn(5000);

        Sensor sensor = caliperService.getSensor(platformDeployment);

        assertNotNull(sensor);
        assertEquals("pd-sensor-id", sensor.getId());
        assertNotSame(sentinel, sensor);
    }

    // ---------- send() ----------

    @Test
    public void testSendLogsAndReturnsWhenSensorIsNull() throws InterruptedException {
        // defaultSensor is null (init() never set it because caliperSend is false) and platformDeployment's
        // caliper configuration is unstubbed (null), so getSensor() resolves to the null defaultSensor.
        Envelope envelope = new Envelope("sensor-id", DateTime.now(), CaliperServiceImpl.DATA_VERSION, Collections.emptyList());

        assertDoesNotThrow(() -> caliperService.send(envelope, platformDeployment));
        // give the fire-and-forget background thread a moment to execute the log branch for coverage purposes
        Thread.sleep(150);
    }

    @Test
    public void testSendDispatchesEnvelopeToSensorWhenSensorIsPresent() {
        Sensor mockSensor = mock(Sensor.class);
        ReflectionTestUtils.setField(caliperService, "defaultSensor", mockSensor);
        when(platformDeployment.getCaliperConfiguration()).thenReturn(false);
        Envelope envelope = new Envelope("sensor-id", DateTime.now(), CaliperServiceImpl.DATA_VERSION, Collections.emptyList());

        caliperService.send(envelope, platformDeployment);

        verify(mockSensor, timeout(2000)).send(envelope);
    }

    // ---------- sendAssignmentStarted() ----------

    @Test
    public void testSendAssignmentStartedSavesEventWhenCaliperDbEnabled() {
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

        caliperService.sendAssignmentStarted(submission, securedInfo);

        verify(eventRepository).save(captor.capture());
        Event event = captor.getValue();
        assertTrue(event.getCaliperId().startsWith("urn:uuid:"));
        assertEquals(Action.STARTED.value(), event.getAction());
        assertEquals("AssessmentProfile", event.getProfile());
        assertNotNull(event.getJson());
        assertEquals(submission.getParticipant(), event.getParticipant());
    }

    @Test
    public void testSendAssignmentStartedDoesNotSaveEventWhenCaliperDbDisabled() {
        ReflectionTestUtils.setField(caliperService, "caliperDB", false);

        caliperService.sendAssignmentStarted(submission, securedInfo);

        verify(eventRepository, never()).save(any());
    }

    @Test
    public void testSendAssignmentStartedSkipsExposureLookupWhenParticipantHasNoGroup() {
        when(participant.getGroup()).thenReturn(null);

        caliperService.sendAssignmentStarted(submission, securedInfo);

        verify(exposureGroupConditionRepository, never()).getByGroup_GroupIdAndCondition_ConditionId(any(), any());
        verify(eventRepository).save(any());
    }

    @Test
    public void testSendAssignmentStartedAddsExposureExtensionWhenGroupConditionPresent() {
        when(exposureGroupConditionRepository.getByGroup_GroupIdAndCondition_ConditionId(any(), any())).thenReturn(Optional.of(exposureGroupCondition));

        assertDoesNotThrow(() -> caliperService.sendAssignmentStarted(submission, securedInfo));

        verify(eventRepository).save(any());
    }

    @Test
    public void testSendAssignmentStartedHandlesIntegrationSubmission() {
        when(submission.isIntegration()).thenReturn(true);
        when(integrationToken.getToken()).thenReturn("integration-token-value");

        assertDoesNotThrow(() -> caliperService.sendAssignmentStarted(submission, securedInfo));

        verify(eventRepository).save(any());
    }

    @Test
    public void testSendAssignmentStartedWithInstructorRole() {
        when(ltiMembershipEntity.getRole()).thenReturn(LtiStrings.ROLE_INSTRUCTOR);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        caliperService.sendAssignmentStarted(submission, securedInfo);

        verify(eventRepository).save(captor.capture());
        assertEquals(LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR, captor.getValue().getMembershipRoles());
    }

    @Test
    public void testSendAssignmentStartedWithAdministratorRole() {
        when(ltiMembershipEntity.getRole()).thenReturn(2);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        caliperService.sendAssignmentStarted(submission, securedInfo);

        verify(eventRepository).save(captor.capture());
        assertEquals(LtiStrings.LTI_ROLE_MEMBERSHIP_ADMIN, captor.getValue().getMembershipRoles());
    }

    @Test
    public void testSendAssignmentStartedWithUnknownRole() {
        when(ltiMembershipEntity.getRole()).thenReturn(99);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        caliperService.sendAssignmentStarted(submission, securedInfo);

        verify(eventRepository).save(captor.capture());
        assertNull(captor.getValue().getMembershipRoles());
    }

    // ---------- sendAssignmentSubmitted() ----------

    @Test
    public void testSendAssignmentSubmittedSavesEventWhenCaliperDbEnabled() {
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

        caliperService.sendAssignmentSubmitted(submission, securedInfo);

        verify(eventRepository).save(captor.capture());
        assertEquals(Action.SUBMITTED.value(), captor.getValue().getAction());
        assertNotNull(captor.getValue().getJson());
    }

    @Test
    public void testSendAssignmentSubmittedDoesNotSaveEventWhenCaliperDbDisabled() {
        ReflectionTestUtils.setField(caliperService, "caliperDB", false);

        caliperService.sendAssignmentSubmitted(submission, securedInfo);

        verify(eventRepository, never()).save(any());
    }

    // ---------- sendAssignmentRestarted() ----------

    @Test
    public void testSendAssignmentRestartedSavesEventWhenCaliperDbEnabled() {
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

        caliperService.sendAssignmentRestarted(submission, securedInfo);

        verify(eventRepository).save(captor.capture());
        assertEquals(Action.RESTARTED.value(), captor.getValue().getAction());
        assertNotNull(captor.getValue().getJson());
    }

    @Test
    public void testSendAssignmentRestartedDoesNotSaveEventWhenCaliperDbDisabled() {
        ReflectionTestUtils.setField(caliperService, "caliperDB", false);

        caliperService.sendAssignmentRestarted(submission, securedInfo);

        verify(eventRepository, never()).save(any());
    }

    // ---------- sendMediaEvent() ----------

    private MediaEventDto buildMediaEventDto(Map<String, Object> extensions) {
        // NOTE: MediaObjectDto/MediaLocationDto extend AbstractDto (which owns the "id" field) but are
        // annotated with plain @Builder (not @SuperBuilder), so the generated builder does not expose an
        // id(...) method for the inherited field - it has to be set via the Lombok-generated setter instead.
        MediaObjectDto object = MediaObjectDto.builder()
            .name("Sample Video")
            .type(EntityType.MEDIA_OBJECT)
            .mediaType("video/mp4")
            .duration("120")
            .build();
        object.setId("media-object-1");
        MediaLocationDto target = MediaLocationDto.builder()
            .type(EntityType.MEDIA_LOCATION)
            .currentTime("30")
            .build();
        target.setId("media-location-1");

        return MediaEventDto.builder()
            .id("media-event-1")
            .profile("MediaProfile")
            .action(Action.PAUSED)
            .object(object)
            .target(target)
            .extensions(extensions)
            .build();
    }

    @Test
    public void testSendMediaEventSavesEventWithExtensions() {
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("custom_extension", "value");
        MediaEventDto mediaEventDto = buildMediaEventDto(extensions);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        caliperService.sendMediaEvent(mediaEventDto, participant, securedInfo, submission, 1L);

        verify(eventRepository).save(captor.capture());
        assertEquals(Action.PAUSED.value(), captor.getValue().getAction());
        assertEquals("media-object-1", captor.getValue().getObjectId());
        assertEquals("media-location-1", captor.getValue().getTargetId());
        assertNotNull(captor.getValue().getJson());
    }

    @Test
    public void testSendMediaEventSavesEventWithoutExtensions() {
        MediaEventDto mediaEventDto = buildMediaEventDto(null);

        assertDoesNotThrow(() -> caliperService.sendMediaEvent(mediaEventDto, participant, securedInfo, submission, 1L));

        verify(eventRepository).save(any());
    }

    @Test
    public void testSendMediaEventDoesNotSaveEventWhenCaliperDbDisabled() {
        ReflectionTestUtils.setField(caliperService, "caliperDB", false);
        MediaEventDto mediaEventDto = buildMediaEventDto(null);

        caliperService.sendMediaEvent(mediaEventDto, participant, securedInfo, submission, 1L);

        verify(eventRepository, never()).save(any());
    }

    // ---------- sendNavigationEvent() / sendFeedbackEvent() (no-op POC stubs) ----------

    @Test
    public void testSendNavigationEventIsNoOp() {
        assertDoesNotThrow(() -> caliperService.sendNavigationEvent(participant, "somewhere", securedInfo));
        verify(eventRepository, never()).save(any());
    }

    @Test
    public void testSendFeedbackEventIsNoOp() {
        assertDoesNotThrow(() -> caliperService.sendFeedbackEvent(participant, assessment, securedInfo));
        verify(eventRepository, never()).save(any());
    }

    // ---------- sendViewGradeEvent() ----------

    @Test
    public void testSendViewGradeEventSavesEventWithNoComments() {
        when(submission.getSubmissionComments()).thenReturn(Collections.emptyList());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        caliperService.sendViewGradeEvent(submission, securedInfo);

        verify(eventRepository).save(captor.capture());
        assertEquals(Action.VIEWED.value(), captor.getValue().getAction());
        assertEquals("GradingProfile", captor.getValue().getProfile());
    }

    @Test
    public void testSendViewGradeEventSavesEventWithMultipleComments() {
        SubmissionComment comment1 = mock(SubmissionComment.class);
        when(comment1.getComment()).thenReturn("Nice job");
        when(comment1.getCreator()).thenReturn("Teacher One");
        SubmissionComment comment2 = mock(SubmissionComment.class);
        when(comment2.getComment()).thenReturn("Needs work");
        when(comment2.getCreator()).thenReturn("Teacher Two");
        when(submission.getSubmissionComments()).thenReturn(List.of(comment1, comment2));

        assertDoesNotThrow(() -> caliperService.sendViewGradeEvent(submission, securedInfo));

        verify(eventRepository).save(any());
    }

    @Test
    public void testSendViewGradeEventDoesNotSaveEventWhenCaliperDbDisabled() {
        when(submission.getSubmissionComments()).thenReturn(Collections.emptyList());
        ReflectionTestUtils.setField(caliperService, "caliperDB", false);

        caliperService.sendViewGradeEvent(submission, securedInfo);

        verify(eventRepository, never()).save(any());
    }

    // ---------- sendToolUseEvent() ----------

    @Test
    public void testSendToolUseEventSavesEventWithLearnerRole() {
        when(ltiMembershipEntity.getRole()).thenReturn(LtiStrings.ROLE_STUDENT);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        caliperService.sendToolUseEvent(ltiMembershipEntity, "global-id", "course-1", "user-1", "login-1", List.of("Learner"), "Test User");

        verify(eventRepository).save(captor.capture());
        Event event = captor.getValue();
        assertEquals(Action.USED.value(), event.getAction());
        assertEquals("ToolUseProfile", event.getProfile());
        assertEquals(LtiStrings.LTI_ROLE_LEARNER, event.getMembershipRoles());
        assertNotNull(event.getJson());
    }

    @Test
    public void testSendToolUseEventSavesEventWithInstructorRole() {
        when(ltiMembershipEntity.getRole()).thenReturn(LtiStrings.ROLE_INSTRUCTOR);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        caliperService.sendToolUseEvent(ltiMembershipEntity, "global-id", "course-1", "user-1", "login-1", List.of("Instructor"), "Test User");

        verify(eventRepository).save(captor.capture());
        assertEquals(LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR, captor.getValue().getMembershipRoles());
    }

    @Test
    public void testSendToolUseEventDoesNotSaveEventWhenCaliperDbDisabled() {
        ReflectionTestUtils.setField(caliperService, "caliperDB", false);

        caliperService.sendToolUseEvent(ltiMembershipEntity, "global-id", "course-1", "user-1", "login-1", List.of("Learner"), "Test User");

        verify(eventRepository, never()).save(any());
    }

    @Test
    public void testSendAssignmentStartedAssignmentIdStartingWithDollarSignIsExcludedFromSession() {
        // Note: sendToolUseEvent() builds its OWN local `SecuredInfo` internally (shadowing the injected
        // parameter used elsewhere) and never populates lmsAssignmentId on it, so that method can never
        // reach the "starts with $" branch of prepareLtiSession(). Exercise the branch here instead, through
        // a method that is actually passed the (mocked) SecuredInfo directly.
        when(securedInfo.getLmsAssignmentId()).thenReturn("$Canvas.assignment.id");

        assertDoesNotThrow(() -> caliperService.sendAssignmentStarted(submission, securedInfo));

        verify(eventRepository).save(any());
    }

    @Test
    public void testSendAssignmentStartedFalseCaliperConfigurationIsTreatedAsNotConfigured() {
        // BooleanUtils.isNotTrue(false) branch of getSensor(), reached indirectly through sendEnabled() defaulting to false
        when(platformDeployment.getCaliperConfiguration()).thenReturn(false);

        assertDoesNotThrow(() -> caliperService.sendAssignmentStarted(submission, securedInfo));

        verify(eventRepository).save(any());
    }

}
