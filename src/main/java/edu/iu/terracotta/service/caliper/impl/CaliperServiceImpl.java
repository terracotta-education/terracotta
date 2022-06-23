package edu.iu.terracotta.service.caliper.impl;

import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.LtiMembershipEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.app.*;
import edu.iu.terracotta.model.app.dto.media.MediaEventDto;
import edu.iu.terracotta.model.app.dto.media.MediaLocationDto;
import edu.iu.terracotta.model.app.dto.media.MediaObjectDto;
import edu.iu.terracotta.model.events.Event;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.caliper.CaliperService;
import edu.iu.terracotta.service.common.Utils;
import edu.iu.terracotta.utils.LtiStrings;
import org.imsglobal.caliper.Envelope;
import org.imsglobal.caliper.Sensor;
import org.imsglobal.caliper.actions.Action;
import org.imsglobal.caliper.clients.HttpClient;
import org.imsglobal.caliper.clients.HttpClientOptions;
import org.imsglobal.caliper.context.JsonldContext;
import org.imsglobal.caliper.context.JsonldStringContext;
import org.imsglobal.caliper.entities.CaliperReferrer;
import org.imsglobal.caliper.entities.EntityType;
import org.imsglobal.caliper.entities.agent.*;
import org.imsglobal.caliper.entities.outcome.Result;
import org.imsglobal.caliper.entities.resource.Attempt;
import org.imsglobal.caliper.entities.resource.MediaLocation;
import org.imsglobal.caliper.entities.session.LtiSession;
import org.imsglobal.caliper.events.*;
import org.imsglobal.caliper.events.MediaEvent.Builder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;

@Service
public class CaliperServiceImpl implements CaliperService {

    private Map<Long, Sensor> sensorMap = new HashMap<>();
    public static final String DATA_VERSION = "http://purl.imsglobal.org/ctx/caliper/v1p2";
    static final Logger log = LoggerFactory.getLogger(CaliperServiceImpl.class);
    private String applicationName;
    private String applicationUrl;
    private final SoftwareApplication softwareApplication;
    private final JsonldContext context;
    private final boolean caliperSend;
    private final boolean caliperDB;

    @Autowired
    AllRepositories allRepositories;

    @Autowired
    AssessmentService assessmentService;

    @Autowired
    SubmissionService submissionService;

    @Autowired
    public CaliperServiceImpl(@Value("${caliper.sensor-id:1}") final String sensorId,
                              @Value("${caliper.client-id:1}") final String clientId,
                              @Value("${caliper.api-key:1}") final String apiKey,
                              @Value("${caliper.connection-timeout:10000}") final int connectionTimeout,
                              @Value("${caliper.content-type:application/json}") final String contentType,
                              @Value("${caliper.host:nohost}") final String host,
                              @Value("${caliper.socket-timeout:10000}") final int socketTimeOut,
                              @Value("${caliper.send:false}") final boolean caliperSendAttribute,
                              @Value("${caliper.store-db:false}") final boolean caliperStoreDBAttribute,
                              @Value("${application.name}") final String applicationNameAttribute,
                              @Value("${application.url}") final String applicationUrlAttribute) {
        applicationName = applicationNameAttribute;
        applicationUrl = applicationUrlAttribute;
        caliperSend = caliperSendAttribute;
        caliperDB = caliperStoreDBAttribute;
        context = JsonldStringContext.create(DATA_VERSION);
        softwareApplication = prepareSoftwareApplication();
        if (caliperSend) {
            Sensor defaultSensor = Sensor.create(sensorId);
            HttpClientOptions httpClientOptions = new HttpClientOptions.OptionsBuilder()
                    .apiKey(apiKey)
                    .connectionTimeout(connectionTimeout)
                    .contentType(contentType)
                    .host(host)
                    .socketTimeout(socketTimeOut)
                    .build();
            HttpClient defaultHttpClient = HttpClient.create(clientId, httpClientOptions);
            defaultSensor.registerClient(defaultHttpClient);
            sensorMap.put(0L, defaultSensor);
        }
    }

    @Override
    public void send(Envelope envelope, PlatformDeployment platformDeployment) {
        Sensor sensor = getSensor(platformDeployment);
        sensor.send(envelope);

    }

    @Override
    public void sendAssignmentStarted(Submission submission, SecuredInfo securedInfo) {
        DateTime time = DateTime.now();
        Participant participant = submission.getParticipant();
        LtiMembershipEntity membershipEntity = participant.getLtiMembershipEntity();
        LtiSession ltiSession = prepareLtiSession(securedInfo, membershipEntity.getContext().getContextKey());
        Person actor = prepareActor(participant, securedInfo.getCanvasUserGlobalId());
        CaliperOrganization group = prepareGroup(membershipEntity, securedInfo);
        org.imsglobal.caliper.entities.resource.Assessment assessment = prepareAssessment(submission, securedInfo);
        Attempt attempt = prepareAttempt(submission, actor, assessment);
        String uuid = "urn:uuid:" + UUID.randomUUID();

        Map<String, Object> extenstions = getTerracottaInternalIDs(submission,participant);
        assessment.getExtensions().putAll(extenstions);


        AssessmentEvent assessmentEvent = AssessmentEvent.builder()
                .id(uuid)
                .actor(actor)
                .action(Action.STARTED)
                .edApp(softwareApplication)
                .context(context)
                .eventTime(DateTime.now())
                .membership(prepareMembership(participant, securedInfo))
                .object(assessment)
                .referrer(prepareReferrer(membershipEntity.getUser().getPlatformDeployment()))
                .federatedSession(ltiSession)
                .generated(attempt)
                .group(group)
                .build();
        Envelope envelope = null;
        if (sendEnabled(membershipEntity.getUser().getPlatformDeployment())) {
            log.debug("Caliper event being generated: Assessment Starting Use");

            envelope = new Envelope(getSensor(membershipEntity.getUser().getPlatformDeployment()).getId(), DateTime.now(), DATA_VERSION, Collections.singletonList(assessmentEvent));
            send(envelope, submission.getParticipant().getLtiUserEntity().getPlatformDeployment());
            log.debug("Caliper event sent");
        }
        if (caliperDB) {
            log.debug("Caliper event to DB: Assessment Started");
            Event event = new Event();
            event.setCaliperId(uuid);
            event.setEventTime(new Timestamp(time.getMillis()));
            event.setActorId(actor.getId());
            event.setActorType(actor.getType().value());
            event.setPlatform_deployment(membershipEntity.getUser().getPlatformDeployment().getBaseUrl());
            event.setType(EventType.ASSESSMENT.value());
            event.setProfile("AssessmentProfile");
            event.setAction(Action.STARTED.value());
            event.setGroup(group.getId());
            event.setObjectId(assessment.getId());
            event.setObjectType(EntityType.ASSESSMENT.value());
            event.setReferrerId(membershipEntity.getUser().getPlatformDeployment().getBaseUrl());
            event.setReferredType(EntityType.SOFTWARE_APPLICATION.value());
            event.setGeneratedId(attempt.getId());
            event.setGeneratedType(EntityType.ATTEMPT.value());
            event.setMembershipId(membershipEntity.getUser().getUserKey());
            event.setMembershipRoles(roleToString(membershipEntity.getRole()));
            event.setFederatedSession(ltiSession.getId());
            event.setLtiContextId(membershipEntity.getContext().getContextKey());
            event.setParticipant(submission.getParticipant());
            populateJSONColumn(event, envelope, assessmentEvent);
            saveEvent(event);
            log.debug("Event Saved");
        }
    }

    @Override
    public void sendAssignmentSubmitted(Submission submission, SecuredInfo securedInfo) {
        DateTime time = DateTime.now();
        Participant participant = submission.getParticipant();
        LtiMembershipEntity membershipEntity = participant.getLtiMembershipEntity();
        Person actor = prepareActor(participant, securedInfo.getCanvasUserGlobalId());
        LtiSession ltiSession = prepareLtiSession(securedInfo, membershipEntity.getContext().getContextKey());
        CaliperOrganization group = prepareGroup(membershipEntity, securedInfo);
        org.imsglobal.caliper.entities.resource.Assessment assessment = prepareAssessment(submission, securedInfo);
        Attempt attempt = prepareAttempt(submission, actor, assessment);
        String uuid = "urn:uuid:" + UUID.randomUUID();

        Map<String, Object> extenstions = getTerracottaInternalIDs(submission,participant);

        assessment.getExtensions().putAll(extenstions);

        AssessmentEvent assessmentEvent = AssessmentEvent.builder()
                .id(uuid)
                .actor(actor)
                .action(Action.SUBMITTED)
                .edApp(softwareApplication)
                .context(context)
                .eventTime(DateTime.now())
                .membership(prepareMembership(participant, securedInfo))
                .object(assessment)
                .referrer(prepareReferrer(membershipEntity.getUser().getPlatformDeployment()))
                .federatedSession(ltiSession)
                .generated(attempt)
                .group(group)
                .build();
        Envelope envelope = null;
        if (sendEnabled(membershipEntity.getUser().getPlatformDeployment())) {
            log.debug("Caliper event being generated: Assessment Submitted Use");

            envelope = new Envelope(getSensor(membershipEntity.getUser().getPlatformDeployment()).getId(), DateTime.now(), DATA_VERSION, Collections.singletonList(assessmentEvent));
            send(envelope, submission.getParticipant().getLtiUserEntity().getPlatformDeployment());
            log.debug("Caliper event sent");
        }
        if (caliperDB) {
            log.debug("Caliper event to DB: Assessment Started");
            Event event = new Event();
            event.setCaliperId(uuid);
            event.setEventTime(new Timestamp(time.getMillis()));
            event.setActorId(actor.getId());
            event.setActorType(actor.getType().value());
            event.setPlatform_deployment(membershipEntity.getUser().getPlatformDeployment().getBaseUrl());
            event.setType(EventType.ASSESSMENT.value());
            event.setProfile("AssessmentProfile");
            event.setAction(Action.SUBMITTED.value());
            event.setGroup(group.getId());
            event.setObjectId(assessment.getId());
            event.setObjectType(EntityType.ASSESSMENT.value());
            event.setReferrerId(membershipEntity.getUser().getPlatformDeployment().getBaseUrl());
            event.setReferredType(EntityType.SOFTWARE_APPLICATION.value());
            event.setGeneratedId(attempt.getId());
            event.setGeneratedType(EntityType.ATTEMPT.value());
            event.setMembershipId(membershipEntity.getUser().getUserKey());
            event.setMembershipRoles(roleToString(membershipEntity.getRole()));
            event.setFederatedSession(ltiSession.getId());
            event.setLtiContextId(membershipEntity.getContext().getContextKey());
            event.setParticipant(submission.getParticipant());
            populateJSONColumn(event, envelope, assessmentEvent);
            saveEvent(event);
            log.debug("Event Saved");
        }
    }

    @Override
    public void sendAssignmentRestarted(Submission submission, SecuredInfo securedInfo) {
        DateTime time = DateTime.now();
        Participant participant = submission.getParticipant();
        LtiMembershipEntity membershipEntity = participant.getLtiMembershipEntity();
        Person actor = prepareActor(participant, securedInfo.getCanvasUserGlobalId());
        LtiSession ltiSession = prepareLtiSession(securedInfo, membershipEntity.getContext().getContextKey());
        CaliperOrganization group = prepareGroup(membershipEntity, securedInfo);
        org.imsglobal.caliper.entities.resource.Assessment assessment = prepareAssessment(submission, securedInfo);
        Attempt attempt = prepareAttempt(submission, actor, assessment);
        String uuid = "urn:uuid:" + UUID.randomUUID();
        Map<String, Object> extenstions = getTerracottaInternalIDs(submission,participant);
        assessment.getExtensions().putAll(extenstions);

        AssessmentEvent assessmentEvent = AssessmentEvent.builder()
                .id(uuid)
                .actor(actor)
                .action(Action.RESTARTED)
                .edApp(softwareApplication)
                .context(context)
                .eventTime(DateTime.now())
                .membership(prepareMembership(participant, securedInfo))
                .object(assessment)
                .generated(attempt)
                .referrer(prepareReferrer(membershipEntity.getUser().getPlatformDeployment()))
                .federatedSession(ltiSession)
                .group(group)
                .build();
        Envelope envelope = null;
        if (sendEnabled(membershipEntity.getUser().getPlatformDeployment())) {
            log.debug("Caliper event being generated: Assessment Starting Use");

            envelope = new Envelope(getSensor(membershipEntity.getUser().getPlatformDeployment()).getId(), DateTime.now(), DATA_VERSION, Collections.singletonList(assessmentEvent));
            send(envelope, submission.getParticipant().getLtiUserEntity().getPlatformDeployment());
            log.debug("Caliper event sent");
        }
        if (caliperDB) {
            log.debug("Caliper event to DB: Assessment Started");
            Event event = new Event();
            event.setCaliperId(uuid);
            event.setEventTime(new Timestamp(time.getMillis()));
            event.setActorId(actor.getId());
            event.setActorType(actor.getType().value());
            event.setPlatform_deployment(membershipEntity.getUser().getPlatformDeployment().getBaseUrl());
            event.setType(EventType.ASSESSMENT.value());
            event.setProfile("AssessmentProfile");
            event.setAction(Action.RESTARTED.value());
            event.setGroup(group.getId());
            event.setObjectId(assessment.getId());
            event.setObjectType(EntityType.ASSESSMENT.value());
            event.setReferrerId(membershipEntity.getUser().getPlatformDeployment().getBaseUrl());
            event.setReferredType(EntityType.SOFTWARE_APPLICATION.value());
            event.setGeneratedId(attempt.getId());
            event.setGeneratedType(EntityType.ATTEMPT.value());
            event.setMembershipId(membershipEntity.getUser().getUserKey());
            event.setMembershipRoles(roleToString(membershipEntity.getRole()));
            event.setFederatedSession(ltiSession.getId());
            event.setLtiContextId(membershipEntity.getContext().getContextKey());
            event.setParticipant(submission.getParticipant());
            populateJSONColumn(event, envelope, assessmentEvent);
            saveEvent(event);
            log.debug("Event Saved");
        }
    }

    @Override
    public void sendMediaEvent(MediaEventDto mediaEventDto, Participant participant, SecuredInfo securedInfo,
                               Submission submission, Long questionId) {
        LtiMembershipEntity membershipEntity = participant.getLtiMembershipEntity();
        Person actor = prepareActor(participant, securedInfo.getCanvasUserGlobalId());
        LtiSession ltiSession = prepareLtiSession(securedInfo, membershipEntity.getContext().getContextKey());
        CaliperOrganization group = prepareGroup(membershipEntity, securedInfo);
        org.imsglobal.caliper.entities.resource.MediaObject mediaObject = prepareMediaObject(mediaEventDto.getObject(),
                submission, securedInfo, questionId);
        MediaLocation mediaLocation = prepareMediaLocation(mediaEventDto.getTarget());
        String uuid = "urn:uuid:" + UUID.randomUUID();
        Map<String, Object> extenstions = getTerracottaInternalIDs(submission,participant);

        mediaObject.getExtensions().putAll(extenstions);

        Builder<?> builder = MediaEvent.builder()
                .id(uuid)
                .actor(actor)
                .action(mediaEventDto.getAction())
                .edApp(softwareApplication)
                .context(context)
                .eventTime(mediaEventDto.getEventTime())
                .membership(prepareMembership(participant, securedInfo))
                .object(mediaObject);
        if (mediaEventDto.getExtensions() != null) {
            builder.extensions(mediaEventDto.getExtensions());
        }
        org.imsglobal.caliper.events.MediaEvent mediaEvent = builder
                .target(mediaLocation)
                .referrer(prepareReferrer(membershipEntity.getUser().getPlatformDeployment()))
                .federatedSession(ltiSession)
                .group(group)
                .build();
        Envelope envelope = null;
        if (sendEnabled(membershipEntity.getUser().getPlatformDeployment())) {
            log.debug("Caliper event being generated: MediaEvent Starting Use");

            envelope = new Envelope(getSensor(membershipEntity.getUser().getPlatformDeployment()).getId(),
                    DateTime.now(), DATA_VERSION, Collections.singletonList(mediaEvent));
            send(envelope, submission.getParticipant().getLtiUserEntity().getPlatformDeployment());
            log.debug("Caliper event sent");
        }
        if (caliperDB) {
            log.debug("Caliper event to DB: Media Event Started");
            Event event = new Event();
            event.setCaliperId(uuid);
            event.setEventTime(new Timestamp(mediaEventDto.getEventTime().getMillis()));
            event.setActorId(actor.getId());
            event.setActorType(actor.getType().value());
            event.setPlatform_deployment(membershipEntity.getUser().getPlatformDeployment().getBaseUrl());
            event.setType(mediaEventDto.getType().value());
            event.setProfile(mediaEventDto.getProfile());
            event.setAction(mediaEventDto.getAction().value());


            event.setGroup(group.getId());
            event.setObjectId(mediaEventDto.getObject().getId());
            event.setObjectType(mediaEventDto.getObject().getType().value());
            event.setReferrerId(membershipEntity.getUser().getPlatformDeployment().getBaseUrl());
            event.setReferredType(EntityType.SOFTWARE_APPLICATION.value());
            event.setTargetId(mediaEventDto.getTarget().getId());
            event.setTargetType(mediaEventDto.getTarget().getType().value());
            event.setMembershipId(membershipEntity.getUser().getUserKey());
            event.setMembershipRoles(Utils.roleToString(membershipEntity.getRole()));
            event.setFederatedSession(ltiSession.getId());
            event.setLtiContextId(membershipEntity.getContext().getContextKey());
            event.setParticipant(submission.getParticipant());
            populateJSONColumn(event, envelope, mediaEvent);
            saveEvent(event);
            log.debug("Event Saved");
        }
    }

    @Override
    public void sendNavigationEvent(Participant participant, String whereTo, SecuredInfo securedInfo) {
        //NO POC, we don't have navigation for the student in the POC
    }

    @Override
    public void sendFeedbackEvent(Participant participant, Assessment assessment, SecuredInfo securedInfo) {
        //NO POC, we don't have navigation for the student in the POC
    }

    //TODO, Think if we want to add the events for each question in the test. (NO POC)


    @Override
    public void sendViewGradeEvent(Submission submission, SecuredInfo securedInfo) {
        DateTime time = DateTime.now();
        Participant participant = submission.getParticipant();
        LtiMembershipEntity membershipEntity = participant.getLtiMembershipEntity();
        Person actor = prepareActor(participant, securedInfo.getCanvasUserGlobalId());
        LtiSession ltiSession = prepareLtiSession(securedInfo, membershipEntity.getContext().getContextKey());
        CaliperOrganization group = prepareGroup(membershipEntity, securedInfo);
        org.imsglobal.caliper.entities.resource.Assessment assessment = prepareAssessment(submission, securedInfo);
        Attempt attempt = prepareAttempt(submission, actor, assessment);
        Result result = prepareResult(submission, attempt, assessment);

        String uuid = "urn:uuid:" + UUID.randomUUID();

        Map<String, Object> extenstions = getTerracottaInternalIDs(submission,participant);

        result.getExtensions().putAll(extenstions);

        ViewEvent assessmentEvent = ViewEvent.builder()
                .id(uuid)
                .actor(actor)
                .action(Action.VIEWED)
                .edApp(softwareApplication)
                .context(context)
                .eventTime(DateTime.now())
                .membership(prepareMembership(participant, securedInfo))
                .object(result)
                .referrer(prepareReferrer(membershipEntity.getUser().getPlatformDeployment()))
                .federatedSession(ltiSession)
                .group(group)
                .extensions(extenstions)
                .build();
        Envelope envelope = null;
        if (sendEnabled(membershipEntity.getUser().getPlatformDeployment())) {
            log.debug("Caliper event being generated: Assessment Starting Use");

            envelope = new Envelope(getSensor(membershipEntity.getUser().getPlatformDeployment()).getId(), DateTime.now(), DATA_VERSION, Collections.singletonList(assessmentEvent));
            send(envelope, submission.getParticipant().getLtiUserEntity().getPlatformDeployment());
            log.debug("Caliper event sent");
        }
        if (caliperDB) {
            log.debug("Caliper event to DB: Assessment Started");
            Event event = new Event();
            event.setCaliperId(uuid);
            event.setEventTime(new Timestamp(time.getMillis()));
            event.setActorId(actor.getId());
            event.setActorType(actor.getType().value());
            event.setPlatform_deployment(membershipEntity.getUser().getPlatformDeployment().getBaseUrl());
            event.setType(EventType.VIEW.value());
            event.setProfile("GradingProfile");
            event.setAction(Action.VIEWED.value());
            event.setGroup(group.getId());
            event.setObjectId(result.getId());
            event.setObjectType(EntityType.RESULT.value());
            event.setReferrerId(membershipEntity.getUser().getPlatformDeployment().getBaseUrl());
            event.setReferredType(EntityType.SOFTWARE_APPLICATION.value());
            event.setMembershipId(membershipEntity.getUser().getUserKey());
            event.setMembershipRoles(roleToString(membershipEntity.getRole()));
            event.setFederatedSession(ltiSession.getId());
            event.setLtiContextId(membershipEntity.getContext().getContextKey());
            event.setParticipant(submission.getParticipant());
            populateJSONColumn(event, envelope, assessmentEvent);
            saveEvent(event);
            log.debug("Event Saved");
        }
    }

    @Override
    public void sendToolUseEvent(LtiMembershipEntity membershipEntity,
                                 String canvasUserGlobalId,
                                 String canvasCourseId,
                                 String canvasUserId,
                                 String canvasLoginId,
                                 List<String> canvasRoles,
                                 String canvasUserName
    ) {
        DateTime time = DateTime.now();
        Person actor = prepareActor(membershipEntity, canvasUserGlobalId);
        SecuredInfo securedInfo = new SecuredInfo();
        securedInfo.setCanvasUserGlobalId(canvasUserGlobalId);
        securedInfo.setCanvasCourseId(canvasCourseId);
        securedInfo.setCanvasUserId(canvasUserId);
        securedInfo.setCanvasLoginId(canvasLoginId);
        securedInfo.setRoles(canvasRoles);
        securedInfo.setCanvasUserName(canvasUserName);
        LtiSession ltiSession = prepareLtiSession(securedInfo, membershipEntity.getContext().getContextKey());
        CaliperOrganization group = prepareGroup(membershipEntity, securedInfo);
        String uuid = "urn:uuid:" + UUID.randomUUID();
        ToolUseEvent toolUseEvent = ToolUseEvent.builder()
                .id(uuid)
                .actor(actor)
                .action(Action.USED)
                .edApp(softwareApplication)
                .context(context)
                .eventTime(DateTime.now())
                .object(softwareApplication)
                .referrer(prepareReferrer(membershipEntity.getUser().getPlatformDeployment()))
                .federatedSession(ltiSession)
                .group(group)
                .build();
        Envelope envelope = null;
        if (sendEnabled(membershipEntity.getUser().getPlatformDeployment())) {
            log.debug("Caliper event being generated: Tool Use");
            envelope = new Envelope(getSensor(membershipEntity.getUser().getPlatformDeployment()).getId(), DateTime.now(), DATA_VERSION, Collections.singletonList(toolUseEvent));
            send(envelope, membershipEntity.getUser().getPlatformDeployment());
            log.debug("Caliper event sent");
        }
        if (caliperDB) {
            log.debug("Caliper event to DB: Tool Use");
            Event event = new Event();
            event.setCaliperId(uuid);
            event.setEventTime(new Timestamp(time.getMillis()));
            event.setActorId(actor.getId());
            event.setActorType(actor.getType().value());
            event.setPlatform_deployment(membershipEntity.getUser().getPlatformDeployment().getBaseUrl());
            event.setType(EventType.TOOL_USE.value());
            event.setProfile("ToolUseProfile");
            event.setAction(Action.USED.value());
            event.setGroup(group.getId());
            event.setObjectId(softwareApplication.getId());
            event.setObjectType(softwareApplication.getType().value());
            event.setReferrerId(membershipEntity.getUser().getPlatformDeployment().getBaseUrl());
            event.setReferredType(EntityType.SOFTWARE_APPLICATION.value());
            event.setMembershipId(membershipEntity.getUser().getUserKey());
            event.setMembershipRoles(roleToString(membershipEntity.getRole()));
            event.setFederatedSession(ltiSession.getId());
            event.setLtiContextId(membershipEntity.getContext().getContextKey());
            populateJSONColumn(event, envelope, toolUseEvent);
            saveEvent(event);
            log.debug("Event Saved");
        }

    }

    private void saveEvent(Event event) {
        allRepositories.eventRepository.save(event);
    }


    private Person prepareActor(LtiMembershipEntity ltiMembershipEntity, String canvasGlobalId) {

        Map<String, Object> extensions = getExtensions(ltiMembershipEntity, canvasGlobalId);
        return buildActor(ltiMembershipEntity, extensions);
    }

    private Person prepareActor(Participant participant, String canvasGlobalId) {

        Map<String, Object> extensions = getExtensions(participant, canvasGlobalId);
        return buildActor(participant.getLtiMembershipEntity(), extensions);
    }

    private Map<String, Object> getExtensions(LtiMembershipEntity ltiMembershipEntity, String canvasGlobalId) {

        Map<String, Object> extensions = new HashMap<>();
        extensions.put("canvas_global_id", canvasGlobalId);
        extensions.put("lti_id", ltiMembershipEntity.getUser().getUserKey());
        extensions.put("lti_tenant", ltiMembershipEntity.getUser().getPlatformDeployment().getBaseUrl());
        return extensions;
    }

    private Map<String, Object> getExtensions(Participant participant, String canvasGlobalId) {

        Map<String, Object> extensions = new HashMap<>();
        extensions.putAll(getExtensions(participant.getLtiMembershipEntity(), canvasGlobalId));
        extensions.put("terracotta_participant_id", participant.getParticipantId());
        return extensions;
    }

    private Person buildActor(LtiMembershipEntity ltiMembershipEntity, Map<String, Object> extensions) {
        Person actor = Person.builder()
                .id(applicationUrl + "/users/" + ltiMembershipEntity.getUser().getUserId())
                .extensions(extensions)
                .type(EntityType.PERSON)
                .build();
        return actor;
    }

    private SoftwareApplication prepareSoftwareApplication() {
        return SoftwareApplication.builder()
                .name(applicationName)
                .id(applicationUrl)
                .build();
    }

    private CaliperReferrer prepareReferrer(PlatformDeployment platformDeployment) {
        return SoftwareApplication.builder()
                .id(platformDeployment.getBaseUrl())
                .type(EntityType.SOFTWARE_APPLICATION)
                .build();
    }

    private org.imsglobal.caliper.entities.resource.Assessment prepareAssessment(Submission submission, SecuredInfo securedInfo) {
        String terracottaAssessmentId = buildTerracottaAssessmentId(submission);
        String canvasAssessmentId = submission.getParticipant().getLtiUserEntity().getPlatformDeployment().getBaseUrl()
                + "/courses/" + securedInfo.getCanvasCourseId()
                + "/assignments/" + securedInfo.getCanvasAssignmentId();
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("canvas_assessment", canvasAssessmentId);
        int maxAttempts = 0;
        try {
            maxAttempts = submission.getAssessment().getNumOfSubmissions();
        } catch (Exception ex) {
            //Do nothing...
        }
        org.imsglobal.caliper.entities.resource.Assessment assessment = org.imsglobal.caliper.entities.resource.Assessment.builder()
                .name(submission.getAssessment().getTitle())
                .id(terracottaAssessmentId)
                .extensions(extensions)
                .type(EntityType.ASSESSMENT)
                .maxAttempts(maxAttempts)
                .maxScore(assessmentService.calculateMaxScore(submission.getAssessment()))
                .version("" + submission.getAssessment().getVersion())
                .build();
        return assessment;
    }

    private String buildTerracottaAssessmentId(Submission submission) {
        String terracottaAssessmentId = applicationUrl + "/api/experiments/" + submission.getAssessment().getTreatment().getCondition().getExperiment().getExperimentId()
                + "/conditions/" + submission.getAssessment().getTreatment().getCondition().getConditionId()
                + "/treatments/" + submission.getAssessment().getTreatment().getTreatmentId()
                + "/assessments/" + submission.getAssessment().getAssessmentId();
        return terracottaAssessmentId;
    }

    private org.imsglobal.caliper.entities.resource.MediaObject prepareMediaObject(MediaObjectDto mediaObjectDto,
                                                                                   Submission submission, SecuredInfo securedInfo, Long questionId) {
        String terracottaAssessmentId = buildTerracottaAssessmentId(submission);
        String submissionId = terracottaAssessmentId + "/submissions/" + submission.getSubmissionId();
        String questionIdUrl = terracottaAssessmentId + "/questions/" + questionId;
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("terracotta_submission_id", submissionId);
        extensions.put("terracotta_question_id", questionIdUrl);
        org.imsglobal.caliper.entities.resource.MediaObject mediaObject = org.imsglobal.caliper.entities.resource.MediaObject.builder()
                .name(mediaObjectDto.getName())
                .id(mediaObjectDto.getId())
                .extensions(extensions)
                .type(mediaObjectDto.getType())
                .mediaType(mediaObjectDto.getMediaType())
                .duration(mediaObjectDto.getDuration())
                .build();
        return mediaObject;
    }

    private MediaLocation prepareMediaLocation(MediaLocationDto target) {
        MediaLocation mediaLocation = MediaLocation.builder()
                .id(target.getId())
                .type(target.getType())
                .currentTime(target.getCurrentTime())
                .build();
        return mediaLocation;
    }

    private Attempt prepareAttempt(Submission submission, Person actor, org.imsglobal.caliper.entities.resource.Assessment assessment) {

        String terracottaSubmissionId = assessment.getId()
                + "/submissions/" + submission.getSubmissionId();

        Attempt attempt = Attempt.builder()
                .id(terracottaSubmissionId)
                .type(EntityType.ATTEMPT)
                .assignee(actor)
                .assignable(assessment)
                .count(submissionService.findByParticipantIdAndAssessmentId(submission.getParticipant().getParticipantId(), submission.getAssessment().getAssessmentId()).size())
                .dateCreated(convertTimestamp(submission.getCreatedAt(), false))
                .startedAtTime(convertTimestamp(submission.getCreatedAt(), false))
                .endedAtTime(convertTimestamp(submission.getDateSubmitted(), true)) //To avoid the error if they submit instantaneously for some reason.
                .build();
        return attempt;
    }

    private Result prepareResult(Submission submission, Attempt attempt, org.imsglobal.caliper.entities.resource.Assessment assessment) {

        String terracottaSubmissionId = assessment.getId()
                + "/submissions/" + submission.getSubmissionId();
        String comment = null;
        boolean firstComment = true;
        for (SubmissionComment submissionComment : submission.getSubmissionComments()) {
            if (firstComment) {
                comment = submissionComment.getComment() + ":[" + submissionComment.getCreator();
                firstComment = false;
            } else {
                comment = "\n" + submissionComment.getComment() + ":[" + submissionComment.getCreator();
            }
        }
        Result result = Result.builder()
                .id(terracottaSubmissionId)
                .type(EntityType.RESULT)
                .attempt(attempt)
                .maxResultScore(assessmentService.calculateMaxScore(submission.getAssessment()))
                .resultScore(submission.getTotalAlteredGrade())
                .dateCreated(convertTimestamp(submission.getCreatedAt(), false))
                .comment(comment)
                .build();
        return result;
    }

    private CaliperOrganization prepareGroup(LtiMembershipEntity participant, SecuredInfo securedInfo) {
        LtiContextEntity contextEntity = participant.getContext();
        String canvasCourseId = participant.getUser().getPlatformDeployment().getBaseUrl()
                + "/courses/" + securedInfo.getCanvasCourseId();
        return CourseSection.builder()
                .name(contextEntity.getTitle())
                .id(canvasCourseId)
                .type(EntityType.COURSE_OFFERING).build();
    }

    private Membership prepareMembership(Participant participant, SecuredInfo securedInfo) {
        String canvasCourseId = participant.getLtiUserEntity().getPlatformDeployment().getBaseUrl()
                + "/courses/" + securedInfo.getCanvasCourseId();
        return Membership.builder()
                .id(canvasCourseId)
                .type(EntityType.MEMBERSHIP)
                .member(prepareActor(participant, securedInfo.getCanvasUserGlobalId()))
                .organization(prepareGroup(participant.getLtiMembershipEntity(), securedInfo))
                .status(getStatus(participant.getDropped(), participant.getExperiment().getClosed() != null))
                .roles(Collections.singletonList(roleToCaliperRole(participant.getLtiMembershipEntity().getRole()))).build();
    }

    private LtiSession prepareLtiSession(SecuredInfo securedInfo, String contextId) {
        Map<String, Object> messageParameters = new HashMap<>();
        messageParameters.put("canvas_course_id", securedInfo.getCanvasCourseId());
        if (securedInfo.getCanvasAssignmentId() != null && !securedInfo.getCanvasAssignmentId().startsWith("$")) {
            messageParameters.put("canvas_assignment_id", securedInfo.getCanvasAssignmentId());
        }
        messageParameters.put("canvas_user_id", securedInfo.getCanvasUserId());
        messageParameters.put("canvas_login_id", securedInfo.getCanvasLoginId());
        messageParameters.put("canvas_user_global_id", securedInfo.getCanvasUserGlobalId());
        messageParameters.put("canvas_roles", securedInfo.getRoles());
        messageParameters.put("canvas_user_name", securedInfo.getCanvasUserName());
        messageParameters.put("lti_context_id", contextId);
        return LtiSession.builder()
                .id("urn:session_id_localized:" + applicationUrl + "/lti/oauth_nonce/" + securedInfo.getNonce())
                .type(EntityType.LTI_SESSION)
                .messageParameters(messageParameters)
                .build();
    }


    private Status getStatus(boolean dropped, boolean closed) {
        if (closed || dropped) {
            return Status.INACTIVE;
        } else {
            return Status.ACTIVE;
        }
    }

    private boolean sendEnabled(PlatformDeployment platformDeployment) {
        if (platformDeployment.getCaliperConfiguration() != null) {
            if (platformDeployment.getCaliperConfiguration()) {
                return true;
            } else {
                return false;
            }
        } else {
            return caliperSend;
        }
    }


    private Role roleToCaliperRole(int role) {

        if (role == 2) {
            return Role.ADMINISTRATOR;
        } else if (role == LtiStrings.ROLE_INSTRUCTOR) {
            return Role.INSTRUCTOR;
        } else if (role == LtiStrings.ROLE_STUDENT) {
            return Role.LEARNER;
        } else {
            return null;
        }
    }

    private String roleToString(int role) {

        if (role == 2) {
            return LtiStrings.LTI_ROLE_MEMBERSHIP_ADMIN;
        } else if (role == LtiStrings.ROLE_INSTRUCTOR) {
            return LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR;
        } else if (role == LtiStrings.ROLE_STUDENT) {
            return LtiStrings.LTI_ROLE_LEARNER;
        } else {
            return null;
        }

    }

    private DateTime convertTimestamp(Timestamp timestamp, boolean plusOne) {
        DateTime date;
        try {
            date = new DateTime(timestamp.getTime());
            if (plusOne) {
                date.plus(1);
            }
        } catch (Exception e) {
            date = null;
        }
        return date;
    }

    public Sensor getSensor(PlatformDeployment platformDeployment) {
        Sensor sensor = null;
        if ((platformDeployment.getCaliperConfiguration() != null) && (platformDeployment.getCaliperConfiguration())) {
            if (!sensorMap.containsKey(platformDeployment.getKeyId())) {
                sensor = Sensor.create(platformDeployment.getCaliperSensorId());
                HttpClientOptions httpClientOptions = new HttpClientOptions.OptionsBuilder()
                        .apiKey(platformDeployment.getCaliperApiKey())
                        .connectionTimeout(platformDeployment.getCaliperConnectionTimeout())
                        .contentType(platformDeployment.getCaliperContentType())
                        .host(platformDeployment.getCaliperHost())
                        .socketTimeout(platformDeployment.getCaliperSocketTimeOut())
                        .build();
                HttpClient httpClient = HttpClient.create(platformDeployment.getClientId(), httpClientOptions);
                sensor.registerClient(httpClient);
                sensorMap.put(platformDeployment.getKeyId(), sensor);
                return sensor;
            } else {
                return sensorMap.get(platformDeployment.getKeyId());
            }
        } else {
            return sensorMap.get(0L);
        }
    }

    private void populateJSONColumn(Event event, Envelope envelope, org.imsglobal.caliper.events.Event caliperEvent) {
        if (envelope == null) {
            // If event wasn't sent and we're only saving to the database, just create an envelope when an empty sensor id
            envelope = new Envelope("", DateTime.now(), DATA_VERSION, Collections.singletonList(caliperEvent));
        }
        String jsonEnvelope = JSONSerializerClient.serialize(envelope);
        log.debug("json envelop {}", jsonEnvelope);
        event.setJson(jsonEnvelope);
    }

    //public CaliperReferrer prepareReferrer(PlatformDeployment platformDeployment){
    //return null;
    //}


    //GETTERS and SETTERS

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationUrl() {
        return applicationUrl;
    }

    public void setApplicationUrl(String applicationUrl) {
        this.applicationUrl = applicationUrl;
    }

    public boolean isCaliperSend() {
        return caliperSend;
    }

    public boolean isCaliperDB() {
        return caliperDB;
    }


    public Map<Long, Sensor> getSensorMap() {
        return sensorMap;
    }

    public void setSensorMap(Map<Long, Sensor> sensorMap) {
        this.sensorMap = sensorMap;
    }

    public SoftwareApplication getSoftwareApplication() {
        return softwareApplication;
    }

    public JsonldContext getContext() {
        return context;
    }


    private Map<String, Object> getTerracottaInternalIDs(Submission submission, Participant participant) {
        Map<String, Object> extenstions = new HashMap<>();

        Long assignmentId = submission.getAssessment().getTreatment().getAssignment().getAssignmentId();
        Long assessmentId = submission.getAssessment().getAssessmentId();
        Long treatmentId = submission.getAssessment().getTreatment().getTreatmentId();

        if (participant.getGroup() != null) {
            Long groupId = participant.getGroup().getGroupId();

            Long conditionId = submission.getAssessment().getTreatment().getCondition().getConditionId();
            extenstions.put("terracotta_condition_id", conditionId);

            Optional<ExposureGroupCondition> groupCondition = allRepositories.exposureGroupConditionRepository
                    .getByGroup_GroupIdAndCondition_ConditionId(groupId, conditionId);

            if (groupCondition.isPresent()) {
                extenstions.put("terracotta_exposure_id", groupCondition.get().getExposure().getExposureId());
            }
        }

        extenstions.put("terracotta_assignment_id", assignmentId);
        extenstions.put("terracotta_assessment_id", assessmentId);
        extenstions.put("terracotta_treatment_id", treatmentId);
        return extenstions;
    }
}
