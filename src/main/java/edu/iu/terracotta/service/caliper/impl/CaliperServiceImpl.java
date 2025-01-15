package edu.iu.terracotta.service.caliper.impl;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.entity.SubmissionComment;
import edu.iu.terracotta.dao.entity.events.Event;
import edu.iu.terracotta.dao.model.dto.media.MediaEventDto;
import edu.iu.terracotta.dao.model.dto.media.MediaLocationDto;
import edu.iu.terracotta.dao.model.dto.media.MediaObjectDto;
import edu.iu.terracotta.dao.repository.EventRepository;
import edu.iu.terracotta.dao.repository.ExposureGroupConditionRepository;
import edu.iu.terracotta.dao.repository.SubmissionRepository;
import edu.iu.terracotta.service.app.AssessmentSubmissionService;
import edu.iu.terracotta.service.caliper.CaliperService;
import edu.iu.terracotta.utils.CaliperUtils;
import edu.iu.terracotta.utils.LtiStrings;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.imsglobal.caliper.Envelope;
import org.imsglobal.caliper.Sensor;
import org.imsglobal.caliper.actions.Action;
import org.imsglobal.caliper.clients.HttpClient;
import org.imsglobal.caliper.clients.HttpClientOptions;
import org.imsglobal.caliper.context.JsonldContext;
import org.imsglobal.caliper.context.JsonldStringContext;
import org.imsglobal.caliper.entities.CaliperReferrer;
import org.imsglobal.caliper.entities.EntityType;
import org.imsglobal.caliper.entities.agent.CaliperOrganization;
import org.imsglobal.caliper.entities.agent.CourseSection;
import org.imsglobal.caliper.entities.agent.Membership;
import org.imsglobal.caliper.entities.agent.Person;
import org.imsglobal.caliper.entities.agent.Role;
import org.imsglobal.caliper.entities.agent.SoftwareApplication;
import org.imsglobal.caliper.entities.agent.Status;
import org.imsglobal.caliper.entities.outcome.Result;
import org.imsglobal.caliper.entities.resource.Attempt;
import org.imsglobal.caliper.entities.resource.MediaLocation;
import org.imsglobal.caliper.entities.session.LtiSession;
import org.imsglobal.caliper.events.AssessmentEvent;
import org.imsglobal.caliper.events.EventType;
import org.imsglobal.caliper.events.MediaEvent;
import org.imsglobal.caliper.events.MediaEvent.Builder;
import org.imsglobal.caliper.events.ToolUseEvent;
import org.imsglobal.caliper.events.ViewEvent;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class CaliperServiceImpl implements CaliperService {

    public static final String DATA_VERSION = "http://purl.imsglobal.org/ctx/caliper/v1p2";

    @Autowired private EventRepository eventRepository;
    @Autowired private ExposureGroupConditionRepository exposureGroupConditionRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private AssessmentSubmissionService assessmentSubmissionService;

    private final SoftwareApplication softwareApplication;
    private final JsonldContext context;
    private final boolean caliperSend;
    private final boolean caliperDB;

    private Sensor defaultSensor;
    private String applicationName;
    private String applicationUrl;

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

        if (!caliperSend) {
            return;
        }

        defaultSensor = Sensor.create(sensorId);
        HttpClientOptions httpClientOptions = new HttpClientOptions.OptionsBuilder()
            .apiKey(apiKey)
            .connectionTimeout(connectionTimeout)
            .contentType(contentType)
            .host(host)
            .socketTimeout(socketTimeOut)
            .build();
        HttpClient defaultHttpClient = HttpClient.create(clientId, httpClientOptions);
        defaultSensor.registerClient(defaultHttpClient);
    }

    @Override
    public void send(Envelope envelope, PlatformDeployment platformDeployment) {
        Thread thread = new Thread(
            () ->
                {
                    try {
                        Sensor sensor = getSensor(platformDeployment);

                        if (sensor == null) {
                            log.error("No sensor configured for deployment ID: '{}'", platformDeployment.getKeyId());
                            return;
                        }

                        sensor.send(envelope);
                    } catch (Exception e) {
                        log.error("Failed to send event data to caliper: '{}' for deployment ID: '{}' with error: '{}'", platformDeployment.getCaliperHost(), platformDeployment.getKeyId(), e.getMessage());
                    }
                }
        );
        thread.start();
    }

    @Override
    public void sendAssignmentStarted(Submission submission, SecuredInfo securedInfo) {
        LtiSession ltiSession = prepareLtiSession(securedInfo, submission.getParticipant().getLtiMembershipEntity());
        Person actor = prepareActor(submission.getParticipant(), securedInfo.getLmsUserGlobalId());
        CaliperOrganization group = prepareGroup(submission.getParticipant().getLtiMembershipEntity(), securedInfo);
        org.imsglobal.caliper.entities.resource.Assessment assessment = prepareAssessment(submission, securedInfo);
        Attempt attempt = prepareAttempt(submission, actor, assessment);
        String uuid = "urn:uuid:" + UUID.randomUUID();

        Map<String, Object> extenstions = getTerracottaInternalIDs(submission,submission.getParticipant());
        assessment.getExtensions().putAll(extenstions);

        AssessmentEvent assessmentEvent = AssessmentEvent.builder()
            .id(uuid)
            .actor(actor)
            .action(Action.STARTED)
            .edApp(softwareApplication)
            .context(context)
            .eventTime(DateTime.now())
            .membership(prepareMembership(submission.getParticipant(), securedInfo))
            .object(assessment)
            .referrer(prepareReferrer(submission.getParticipant().getLtiMembershipEntity().getUser().getPlatformDeployment()))
            .federatedSession(ltiSession)
            .generated(attempt)
            .group(group)
            .build();
        Envelope envelope = null;

        if (sendEnabled(submission.getParticipant().getLtiMembershipEntity().getUser().getPlatformDeployment())) {
            envelope = new Envelope(getSensor(submission.getParticipant().getLtiMembershipEntity().getUser().getPlatformDeployment()).getId(), DateTime.now(), DATA_VERSION, Collections.singletonList(assessmentEvent));
            send(envelope, submission.getParticipant().getLtiUserEntity().getPlatformDeployment());
        }

        if (!caliperDB) {
            return;
        }

        Event event = new Event();
        event.setCaliperId(uuid);
        event.setEventTime(new Timestamp(DateTime.now().getMillis()));
        event.setActorId(actor.getId());
        event.setActorType(actor.getType().value());
        event.setPlatform_deployment(submission.getParticipant().getLtiMembershipEntity().getUser().getPlatformDeployment().getBaseUrl());
        event.setType(EventType.ASSESSMENT.value());
        event.setProfile("AssessmentProfile");
        event.setAction(Action.STARTED.value());
        event.setGroup(group.getId());
        event.setObjectId(assessment.getId());
        event.setObjectType(EntityType.ASSESSMENT.value());
        event.setReferrerId(submission.getParticipant().getLtiMembershipEntity().getUser().getPlatformDeployment().getBaseUrl());
        event.setReferredType(EntityType.SOFTWARE_APPLICATION.value());
        event.setGeneratedId(attempt.getId());
        event.setGeneratedType(EntityType.ATTEMPT.value());
        event.setMembershipId(submission.getParticipant().getLtiMembershipEntity().getUser().getUserKey());
        event.setMembershipRoles(roleToString(submission.getParticipant().getLtiMembershipEntity().getRole()));
        event.setFederatedSession(ltiSession.getId());
        event.setLtiContextId(submission.getParticipant().getLtiMembershipEntity().getContext().getContextKey());
        event.setParticipant(submission.getParticipant());
        populateJSONColumn(event, envelope, assessmentEvent);
        saveEvent(event);
    }

    @Override
    public void sendAssignmentSubmitted(Submission submission, SecuredInfo securedInfo) {
        Person actor = prepareActor(submission.getParticipant(), securedInfo.getLmsUserGlobalId());
        LtiSession ltiSession = prepareLtiSession(securedInfo, submission.getParticipant().getLtiMembershipEntity());
        CaliperOrganization group = prepareGroup(submission.getParticipant().getLtiMembershipEntity(), securedInfo);
        org.imsglobal.caliper.entities.resource.Assessment assessment = prepareAssessment(submission, securedInfo);
        Attempt attempt = prepareAttempt(submission, actor, assessment);
        String uuid = "urn:uuid:" + UUID.randomUUID();
        Map<String, Object> extensions = getTerracottaInternalIDs(submission, submission.getParticipant());

        assessment.getExtensions().putAll(extensions);

        AssessmentEvent assessmentEvent = AssessmentEvent.builder()
            .id(uuid)
            .actor(actor)
            .action(Action.SUBMITTED)
            .edApp(softwareApplication)
            .context(context)
            .eventTime(DateTime.now())
            .membership(prepareMembership(submission.getParticipant(), securedInfo))
            .object(assessment)
            .referrer(prepareReferrer(submission.getParticipant().getLtiMembershipEntity().getUser().getPlatformDeployment()))
            .federatedSession(ltiSession)
            .generated(attempt)
            .group(group)
            .build();
        Envelope envelope = null;

        if (sendEnabled(submission.getParticipant().getLtiMembershipEntity().getUser().getPlatformDeployment())) {
            envelope = new Envelope(getSensor(submission.getParticipant().getLtiMembershipEntity().getUser().getPlatformDeployment()).getId(), DateTime.now(), DATA_VERSION, Collections.singletonList(assessmentEvent));
            send(envelope, submission.getParticipant().getLtiUserEntity().getPlatformDeployment());
        }

        if (!caliperDB) {
            return;
        }

        Event event = new Event();
        event.setCaliperId(uuid);
        event.setEventTime(new Timestamp(DateTime.now().getMillis()));
        event.setActorId(actor.getId());
        event.setActorType(actor.getType().value());
        event.setPlatform_deployment(submission.getParticipant().getLtiMembershipEntity().getUser().getPlatformDeployment().getBaseUrl());
        event.setType(EventType.ASSESSMENT.value());
        event.setProfile("AssessmentProfile");
        event.setAction(Action.SUBMITTED.value());
        event.setGroup(group.getId());
        event.setObjectId(assessment.getId());
        event.setObjectType(EntityType.ASSESSMENT.value());
        event.setReferrerId(submission.getParticipant().getLtiMembershipEntity().getUser().getPlatformDeployment().getBaseUrl());
        event.setReferredType(EntityType.SOFTWARE_APPLICATION.value());
        event.setGeneratedId(attempt.getId());
        event.setGeneratedType(EntityType.ATTEMPT.value());
        event.setMembershipId(submission.getParticipant().getLtiMembershipEntity().getUser().getUserKey());
        event.setMembershipRoles(roleToString(submission.getParticipant().getLtiMembershipEntity().getRole()));
        event.setFederatedSession(ltiSession.getId());
        event.setLtiContextId(submission.getParticipant().getLtiMembershipEntity().getContext().getContextKey());
        event.setParticipant(submission.getParticipant());
        populateJSONColumn(event, envelope, assessmentEvent);
        saveEvent(event);
    }

    @Override
    public void sendAssignmentRestarted(Submission submission, SecuredInfo securedInfo) {
        Person actor = prepareActor(submission.getParticipant(), securedInfo.getLmsUserGlobalId());
        LtiSession ltiSession = prepareLtiSession(securedInfo, submission.getParticipant().getLtiMembershipEntity());
        CaliperOrganization group = prepareGroup(submission.getParticipant().getLtiMembershipEntity(), securedInfo);
        org.imsglobal.caliper.entities.resource.Assessment assessment = prepareAssessment(submission, securedInfo);
        Attempt attempt = prepareAttempt(submission, actor, assessment);
        String uuid = "urn:uuid:" + UUID.randomUUID();
        Map<String, Object> extensions = getTerracottaInternalIDs(submission, submission.getParticipant());
        assessment.getExtensions().putAll(extensions);

        AssessmentEvent assessmentEvent = AssessmentEvent.builder()
            .id(uuid)
            .actor(actor)
            .action(Action.RESTARTED)
            .edApp(softwareApplication)
            .context(context)
            .eventTime(DateTime.now())
            .membership(prepareMembership(submission.getParticipant(), securedInfo))
            .object(assessment)
            .generated(attempt)
            .referrer(prepareReferrer(submission.getParticipant().getLtiMembershipEntity().getUser().getPlatformDeployment()))
            .federatedSession(ltiSession)
            .group(group)
            .build();
        Envelope envelope = null;

        if (sendEnabled(submission.getParticipant().getLtiMembershipEntity().getUser().getPlatformDeployment())) {
            envelope = new Envelope(getSensor(submission.getParticipant().getLtiMembershipEntity().getUser().getPlatformDeployment()).getId(), DateTime.now(), DATA_VERSION, Collections.singletonList(assessmentEvent));
            send(envelope, submission.getParticipant().getLtiUserEntity().getPlatformDeployment());
        }

        if (!caliperDB) {
            return;
        }

        Event event = new Event();
        event.setCaliperId(uuid);
        event.setEventTime(new Timestamp(DateTime.now().getMillis()));
        event.setActorId(actor.getId());
        event.setActorType(actor.getType().value());
        event.setPlatform_deployment(submission.getParticipant().getLtiMembershipEntity().getUser().getPlatformDeployment().getBaseUrl());
        event.setType(EventType.ASSESSMENT.value());
        event.setProfile("AssessmentProfile");
        event.setAction(Action.RESTARTED.value());
        event.setGroup(group.getId());
        event.setObjectId(assessment.getId());
        event.setObjectType(EntityType.ASSESSMENT.value());
        event.setReferrerId(submission.getParticipant().getLtiMembershipEntity().getUser().getPlatformDeployment().getBaseUrl());
        event.setReferredType(EntityType.SOFTWARE_APPLICATION.value());
        event.setGeneratedId(attempt.getId());
        event.setGeneratedType(EntityType.ATTEMPT.value());
        event.setMembershipId(submission.getParticipant().getLtiMembershipEntity().getUser().getUserKey());
        event.setMembershipRoles(roleToString(submission.getParticipant().getLtiMembershipEntity().getRole()));
        event.setFederatedSession(ltiSession.getId());
        event.setLtiContextId(submission.getParticipant().getLtiMembershipEntity().getContext().getContextKey());
        event.setParticipant(submission.getParticipant());
        populateJSONColumn(event, envelope, assessmentEvent);
        saveEvent(event);
    }

    @Override
    public void sendMediaEvent(MediaEventDto mediaEventDto, Participant participant, SecuredInfo securedInfo, Submission submission, Long questionId) {
        Person actor = prepareActor(participant, securedInfo.getLmsUserGlobalId());
        LtiSession ltiSession = prepareLtiSession(securedInfo, participant.getLtiMembershipEntity());
        CaliperOrganization group = prepareGroup(participant.getLtiMembershipEntity(), securedInfo);
        org.imsglobal.caliper.entities.resource.MediaObject mediaObject = prepareMediaObject(mediaEventDto.getObject(), submission, questionId);
        MediaLocation mediaLocation = prepareMediaLocation(mediaEventDto.getTarget());
        String uuid = "urn:uuid:" + UUID.randomUUID();
        Map<String, Object> extensions = getTerracottaInternalIDs(submission,participant);
        mediaObject.getExtensions().putAll(extensions);

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

        MediaEvent mediaEvent = builder
            .target(mediaLocation)
            .referrer(prepareReferrer(participant.getLtiMembershipEntity().getUser().getPlatformDeployment()))
            .federatedSession(ltiSession)
            .group(group)
            .build();
        Envelope envelope = null;

        if (sendEnabled(participant.getLtiMembershipEntity().getUser().getPlatformDeployment())) {
            envelope = new Envelope(getSensor(participant.getLtiMembershipEntity().getUser().getPlatformDeployment()).getId(), DateTime.now(), DATA_VERSION, Collections.singletonList(mediaEvent));
            send(envelope, submission.getParticipant().getLtiUserEntity().getPlatformDeployment());
        }

        if (!caliperDB) {
            return;
        }

        Event event = new Event();
        event.setCaliperId(uuid);
        event.setEventTime(new Timestamp(mediaEventDto.getEventTime().getMillis()));
        event.setActorId(actor.getId());
        event.setActorType(actor.getType().value());
        event.setPlatform_deployment(participant.getLtiMembershipEntity().getUser().getPlatformDeployment().getBaseUrl());
        event.setType(mediaEventDto.getType().value());
        event.setProfile(mediaEventDto.getProfile());
        event.setAction(mediaEventDto.getAction().value());
        event.setGroup(group.getId());
        event.setObjectId(mediaEventDto.getObject().getId());
        event.setObjectType(mediaEventDto.getObject().getType().value());
        event.setReferrerId(participant.getLtiMembershipEntity().getUser().getPlatformDeployment().getBaseUrl());
        event.setReferredType(EntityType.SOFTWARE_APPLICATION.value());
        event.setTargetId(mediaEventDto.getTarget().getId());
        event.setTargetType(mediaEventDto.getTarget().getType().value());
        event.setMembershipId(participant.getLtiMembershipEntity().getUser().getUserKey());
        event.setMembershipRoles(CaliperUtils.roleToString(participant.getLtiMembershipEntity().getRole()));
        event.setFederatedSession(ltiSession.getId());
        event.setLtiContextId(participant.getLtiMembershipEntity().getContext().getContextKey());
        event.setParticipant(submission.getParticipant());
        populateJSONColumn(event, envelope, mediaEvent);
        saveEvent(event);
    }

    @Override
    public void sendNavigationEvent(Participant participant, String whereTo, SecuredInfo securedInfo) {
        // NO POC, we don't have navigation for the student in the POC
    }

    @Override
    public void sendFeedbackEvent(Participant participant, Assessment assessment, SecuredInfo securedInfo) {
        // NO POC, we don't have navigation for the student in the POC
    }

    // TODO, Think if we want to add the events for each question in the test. (NO POC)

    @Override
    public void sendViewGradeEvent(Submission submission, SecuredInfo securedInfo) {
        Person actor = prepareActor(submission.getParticipant(), securedInfo.getLmsUserGlobalId());
        LtiSession ltiSession = prepareLtiSession(securedInfo, submission.getParticipant().getLtiMembershipEntity());
        CaliperOrganization group = prepareGroup(submission.getParticipant().getLtiMembershipEntity(), securedInfo);
        org.imsglobal.caliper.entities.resource.Assessment assessment = prepareAssessment(submission, securedInfo);
        Attempt attempt = prepareAttempt(submission, actor, assessment);
        Result result = prepareResult(submission, attempt, assessment);
        String uuid = "urn:uuid:" + UUID.randomUUID();
        Map<String, Object> extenstions = getTerracottaInternalIDs(submission, submission.getParticipant());
        result.getExtensions().putAll(extenstions);

        ViewEvent assessmentEvent = ViewEvent.builder()
            .id(uuid)
            .actor(actor)
            .action(Action.VIEWED)
            .edApp(softwareApplication)
            .context(context)
            .eventTime(DateTime.now())
            .membership(prepareMembership(submission.getParticipant(), securedInfo))
            .object(result)
            .referrer(prepareReferrer(submission.getParticipant().getLtiMembershipEntity().getUser().getPlatformDeployment()))
            .federatedSession(ltiSession)
            .group(group)
            .extensions(extenstions)
            .build();
        Envelope envelope = null;

        if (sendEnabled(submission.getParticipant().getLtiMembershipEntity().getUser().getPlatformDeployment())) {
            envelope = new Envelope(getSensor(submission.getParticipant().getLtiMembershipEntity().getUser().getPlatformDeployment()).getId(), DateTime.now(), DATA_VERSION, Collections.singletonList(assessmentEvent));
            send(envelope, submission.getParticipant().getLtiUserEntity().getPlatformDeployment());
        }

        if (!caliperDB) {
            return;
        }

        Event event = new Event();
        event.setCaliperId(uuid);
        event.setEventTime(new Timestamp(DateTime.now().getMillis()));
        event.setActorId(actor.getId());
        event.setActorType(actor.getType().value());
        event.setPlatform_deployment(submission.getParticipant().getLtiMembershipEntity().getUser().getPlatformDeployment().getBaseUrl());
        event.setType(EventType.VIEW.value());
        event.setProfile("GradingProfile");
        event.setAction(Action.VIEWED.value());
        event.setGroup(group.getId());
        event.setObjectId(result.getId());
        event.setObjectType(EntityType.RESULT.value());
        event.setReferrerId(submission.getParticipant().getLtiMembershipEntity().getUser().getPlatformDeployment().getBaseUrl());
        event.setReferredType(EntityType.SOFTWARE_APPLICATION.value());
        event.setMembershipId(submission.getParticipant().getLtiMembershipEntity().getUser().getUserKey());
        event.setMembershipRoles(roleToString(submission.getParticipant().getLtiMembershipEntity().getRole()));
        event.setFederatedSession(ltiSession.getId());
        event.setLtiContextId(submission.getParticipant().getLtiMembershipEntity().getContext().getContextKey());
        event.setParticipant(submission.getParticipant());
        populateJSONColumn(event, envelope, assessmentEvent);
        saveEvent(event);
    }

    @Override
    public void sendToolUseEvent(LtiMembershipEntity membershipEntity, String lmsUserGlobalId, String lmsCourseId, String lmsUserId, String lmsLoginId, List<String> lmsRoles, String lmsUserName) {
        Person actor = prepareActor(membershipEntity, lmsUserGlobalId);
        SecuredInfo securedInfo = new SecuredInfo();
        securedInfo.setLmsUserGlobalId(lmsUserGlobalId);
        securedInfo.setLmsCourseId(lmsCourseId);
        securedInfo.setLmsUserId(lmsUserId);
        securedInfo.setLmsLoginId(lmsLoginId);
        securedInfo.setRoles(lmsRoles);
        securedInfo.setLmsUserName(lmsUserName);
        LtiSession ltiSession = prepareLtiSession(securedInfo, membershipEntity);
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
            envelope = new Envelope(getSensor(membershipEntity.getUser().getPlatformDeployment()).getId(), DateTime.now(), DATA_VERSION, Collections.singletonList(toolUseEvent));
            send(envelope, membershipEntity.getUser().getPlatformDeployment());
        }

        if (!caliperDB) {
            return;
        }

        Event event = new Event();
        event.setCaliperId(uuid);
        event.setEventTime(new Timestamp(DateTime.now().getMillis()));
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
    }

    private void saveEvent(Event event) {
        eventRepository.save(event);
    }

    private Person prepareActor(LtiMembershipEntity ltiMembershipEntity, String lmsGlobalId) {
        return buildActor(ltiMembershipEntity, getExtensions(ltiMembershipEntity, lmsGlobalId));
    }

    private Person prepareActor(Participant participant, String lmsGlobalId) {
        return buildActor(participant.getLtiMembershipEntity(), getExtensions(participant, lmsGlobalId));
    }

    private Map<String, Object> getExtensions(LtiMembershipEntity ltiMembershipEntity, String lmsGlobalId) {
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("lms_global_id", lmsGlobalId);
        extensions.put("lti_id", ltiMembershipEntity.getUser().getUserKey());
        extensions.put("lti_tenant", ltiMembershipEntity.getUser().getPlatformDeployment().getBaseUrl());

        return extensions;
    }

    private Map<String, Object> getExtensions(Participant participant, String lmsGlobalId) {
        Map<String, Object> extensions = new HashMap<>();
        extensions.putAll(getExtensions(participant.getLtiMembershipEntity(), lmsGlobalId));
        extensions.put("terracotta_participant_id", participant.getParticipantId());

        return extensions;
    }

    private Person buildActor(LtiMembershipEntity ltiMembershipEntity, Map<String, Object> extensions) {
        return Person.builder()
            .id(ltiMembershipEntity.getUser().getPlatformDeployment().getLocalUrl() + "/users/" + ltiMembershipEntity.getUser().getUserId())
            .extensions(extensions)
            .type(EntityType.PERSON)
            .build();
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
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("lms_assessment", submission.getParticipant().getLtiUserEntity().getPlatformDeployment().getBaseUrl() + "/courses/" + securedInfo.getLmsCourseId() + "/assignments/" + securedInfo.getLmsAssignmentId());
        int maxAttempts = 0;

        try {
            maxAttempts = submission.getAssessment().getNumOfSubmissions() != null ? submission.getAssessment().getNumOfSubmissions() : 0;
        } catch (Exception ex) {
            log.warn(ex.getMessage());
        }

        return org.imsglobal.caliper.entities.resource.Assessment.builder()
            .name(submission.getAssessment().getTitle())
            .id(buildTerracottaAssessmentId(submission))
            .extensions(extensions)
            .type(EntityType.ASSESSMENT)
            .maxAttempts(maxAttempts)
            .maxScore(assessmentSubmissionService.calculateMaxScore(submission.getAssessment()))
            .version(String.valueOf(submission.getAssessment().getVersion()))
            .build();
    }

    private String buildTerracottaAssessmentId(Submission submission) {
        return submission.getParticipant().getLtiUserEntity().getPlatformDeployment().getLocalUrl() + "/api/experiments/" + submission.getAssessment().getTreatment().getCondition().getExperiment().getExperimentId()
                + "/conditions/" + submission.getAssessment().getTreatment().getCondition().getConditionId()
                + "/treatments/" + submission.getAssessment().getTreatment().getTreatmentId()
                + "/assessments/" + submission.getAssessment().getAssessmentId();
    }

    private org.imsglobal.caliper.entities.resource.MediaObject prepareMediaObject(MediaObjectDto mediaObjectDto, Submission submission, Long questionId) {
        String terracottaAssessmentId = buildTerracottaAssessmentId(submission);
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("terracotta_submission_id", terracottaAssessmentId + "/submissions/" + submission.getSubmissionId());
        extensions.put("terracotta_question_id", terracottaAssessmentId + "/questions/" + questionId);

        return org.imsglobal.caliper.entities.resource.MediaObject.builder()
            .name(mediaObjectDto.getName())
            .id(mediaObjectDto.getId())
            .extensions(extensions)
            .type(mediaObjectDto.getType())
            .mediaType(mediaObjectDto.getMediaType())
            .duration(mediaObjectDto.getDuration())
            .build();
    }

    private MediaLocation prepareMediaLocation(MediaLocationDto target) {
        return MediaLocation.builder()
            .id(target.getId())
            .type(target.getType())
            .currentTime(target.getCurrentTime())
            .build();
    }

    private Attempt prepareAttempt(Submission submission, Person actor, org.imsglobal.caliper.entities.resource.Assessment assessment) {
        return Attempt.builder()
            .id(assessment.getId() + "/submissions/" + submission.getSubmissionId())
            .type(EntityType.ATTEMPT)
            .assignee(actor)
            .assignable(assessment)
            .count(submissionRepository.findByParticipant_ParticipantIdAndAssessment_AssessmentId(submission.getParticipant().getParticipantId(), submission.getAssessment().getAssessmentId()).size())
            .dateCreated(convertTimestamp(submission.getCreatedAt(), false))
            .startedAtTime(convertTimestamp(submission.getCreatedAt(), false))
            .endedAtTime(convertTimestamp(submission.getDateSubmitted(), true)) //To avoid the error if they submit instantaneously for some reason.
            .build();
    }

    private Result prepareResult(Submission submission, Attempt attempt, org.imsglobal.caliper.entities.resource.Assessment assessment) {
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

        return Result.builder()
            .id(assessment.getId() + "/submissions/" + submission.getSubmissionId())
            .type(EntityType.RESULT)
            .attempt(attempt)
            .maxResultScore(assessmentSubmissionService.calculateMaxScore(submission.getAssessment()))
            .resultScore(submission.getTotalAlteredGrade())
            .dateCreated(convertTimestamp(submission.getCreatedAt(), false))
            .comment(comment)
            .build();
    }

    private CaliperOrganization prepareGroup(LtiMembershipEntity participant, SecuredInfo securedInfo) {
        return CourseSection.builder()
            .name(participant.getContext().getTitle())
            .id(participant.getUser().getPlatformDeployment().getBaseUrl() + "/courses/" + securedInfo.getLmsCourseId())
            .type(EntityType.COURSE_OFFERING)
            .build();
    }

    private Membership prepareMembership(Participant participant, SecuredInfo securedInfo) {
        return Membership.builder()
            .id(participant.getLtiUserEntity().getPlatformDeployment().getBaseUrl() + "/courses/" + securedInfo.getLmsCourseId())
            .type(EntityType.MEMBERSHIP)
            .member(prepareActor(participant, securedInfo.getLmsUserGlobalId()))
            .organization(prepareGroup(participant.getLtiMembershipEntity(), securedInfo))
            .status(getStatus(participant.getDropped(), participant.getExperiment().getClosed() != null))
            .roles(Collections.singletonList(roleToCaliperRole(participant.getLtiMembershipEntity().getRole())))
            .build();
    }

    private LtiSession prepareLtiSession(SecuredInfo securedInfo, LtiMembershipEntity ltiMembershipEntity) {
        Map<String, Object> messageParameters = new HashMap<>();
        messageParameters.put("lms_course_id", securedInfo.getLmsCourseId());

        if (!StringUtils.startsWith(securedInfo.getLmsAssignmentId(), "$")) {
            messageParameters.put("lms_assignment_id", securedInfo.getLmsAssignmentId());
        }

        messageParameters.put("lms_user_id", securedInfo.getLmsUserId());
        messageParameters.put("lms_login_id", securedInfo.getLmsLoginId());
        messageParameters.put("lms_user_global_id", securedInfo.getLmsUserGlobalId());
        messageParameters.put("lms_roles", securedInfo.getRoles());
        messageParameters.put("lms_user_name", securedInfo.getLmsUserName());
        messageParameters.put("lti_context_id", ltiMembershipEntity.getContext().getContextKey());

        return LtiSession.builder()
            .id("urn:session_id_localized:" + ltiMembershipEntity.getUser().getPlatformDeployment().getLocalUrl() + "/lti/oauth_nonce/" + securedInfo.getNonce())
            .type(EntityType.LTI_SESSION)
            .messageParameters(messageParameters)
            .build();
    }

    private Status getStatus(boolean dropped, boolean closed) {
        if (closed || dropped) {
            return Status.INACTIVE;
        }

        return Status.ACTIVE;
    }

    private boolean sendEnabled(PlatformDeployment platformDeployment) {
        return BooleanUtils.isTrue(platformDeployment.getCaliperConfiguration()) || caliperSend;
    }

    private Role roleToCaliperRole(int role) {
        switch (role) {
            case 2:
                return Role.ADMINISTRATOR;
            case LtiStrings.ROLE_INSTRUCTOR:
                return Role.INSTRUCTOR;
            case LtiStrings.ROLE_STUDENT:
                return Role.LEARNER;
            default:
                return null;
        }
    }

    private String roleToString(int role) {
        switch (role) {
            case 2:
                return LtiStrings.LTI_ROLE_MEMBERSHIP_ADMIN;
            case LtiStrings.ROLE_INSTRUCTOR:
                return LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR;
            case LtiStrings.ROLE_STUDENT:
                return LtiStrings.LTI_ROLE_LEARNER;
            default:
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

        if (BooleanUtils.isNotTrue(platformDeployment.getCaliperConfiguration())) {
            return defaultSensor;
        }

        sensor = Sensor.create(platformDeployment.getCaliperSensorId());
        sensor.registerClient(HttpClient.create(
            platformDeployment.getClientId(),
            new HttpClientOptions.OptionsBuilder()
                .apiKey(platformDeployment.getCaliperApiKey())
                .connectionTimeout(platformDeployment.getCaliperConnectionTimeout())
                .contentType(platformDeployment.getCaliperContentType())
                .host(platformDeployment.getCaliperHost())
                .socketTimeout(platformDeployment.getCaliperSocketTimeout())
                .build()
            )
        );

        return sensor;
    }

    private void populateJSONColumn(Event event, Envelope envelope, org.imsglobal.caliper.events.Event caliperEvent) {
        if (envelope == null) {
            // If event wasn't sent and we're only saving to the database, just create an envelope when an empty sensor id
            envelope = new Envelope("", DateTime.now(), DATA_VERSION, Collections.singletonList(caliperEvent));
        }

        event.setJson(JsonSerializerClient.serialize(envelope));
    }

    private Map<String, Object> getTerracottaInternalIDs(Submission submission, Participant participant) {
        Map<String, Object> extensions = new HashMap<>();

        if (participant.getGroup() != null) {
            extensions.put("terracotta_condition_id", submission.getAssessment().getTreatment().getCondition().getConditionId());

            Optional<ExposureGroupCondition> groupCondition = exposureGroupConditionRepository.getByGroup_GroupIdAndCondition_ConditionId(participant.getGroup().getGroupId(), submission.getAssessment().getTreatment().getCondition().getConditionId());

            if (groupCondition.isPresent()) {
                extensions.put("terracotta_exposure_id", groupCondition.get().getExposure().getExposureId());
            }
        }

        extensions.put("terracotta_assignment_id", submission.getAssessment().getTreatment().getAssignment().getAssignmentId());
        extensions.put("terracotta_assessment_id", submission.getAssessment().getAssessmentId());
        extensions.put("terracotta_treatment_id", submission.getAssessment().getTreatment().getTreatmentId());

        return extensions;
    }

}
