package edu.iu.terracotta.service.caliper.impl;

import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.LtiMembershipEntity;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.events.Event;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.caliper.CaliperService;
import edu.iu.terracotta.utils.LtiStrings;
import org.apache.commons.lang3.StringUtils;
import org.imsglobal.caliper.Envelope;
import org.imsglobal.caliper.Sensor;
import org.imsglobal.caliper.actions.Action;
import org.imsglobal.caliper.clients.HttpClient;
import org.imsglobal.caliper.clients.HttpClientOptions;
import org.imsglobal.caliper.context.JsonldContext;
import org.imsglobal.caliper.context.JsonldStringContext;
import org.imsglobal.caliper.entities.CaliperEntity;
import org.imsglobal.caliper.entities.CaliperGeneratable;
import org.imsglobal.caliper.entities.EntityType;
import org.imsglobal.caliper.entities.agent.CaliperOrganization;
import org.imsglobal.caliper.entities.agent.CourseSection;
import org.imsglobal.caliper.entities.agent.Person;
import org.imsglobal.caliper.entities.agent.SoftwareApplication;
import org.imsglobal.caliper.events.EventType;
import org.imsglobal.caliper.events.ToolUseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.joda.time.DateTime;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.UUID;

@Service
public class CaliperServiceImpl implements CaliperService {

    private Sensor sensor;
    private HttpClient httpClient;
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
    public CaliperServiceImpl(@Value("${caliper.sensor-id:1}")final String sensorId,
                              @Value("${caliper.client-id:1}") final String clientId,
                              @Value("${caliper.api-key:1}") final String apiKey,
                              @Value("${caliper.connection-timeout:10000}") final int connectionTimeout,
                              @Value("${caliper.content-type:application/json}") final String contentType,
                              @Value("${caliper.host:nohost}") final String host,
                              @Value("${caliper.socket-timeout:10000}") final int socketTimeOut,
                              @Value("${caliper.send:false}") final boolean caliperSendAttribute,
                              @Value("${caliper.store-db:false}") final boolean caliperStoreDBAttribute,
                              @Value("${application.name}") final String applicationNameAttribute,
                              @Value("${application.url}") final String applicationUrlAttribute){
        applicationName = applicationNameAttribute;
        applicationUrl = applicationUrlAttribute;
        caliperSend = caliperSendAttribute;
        caliperDB = caliperStoreDBAttribute;
        context = JsonldStringContext.create(DATA_VERSION);
        softwareApplication = prepareSoftwareApplication();
        if (caliperSend) {
            sensor = Sensor.create(sensorId);
            HttpClientOptions httpClientOptions = new HttpClientOptions.OptionsBuilder()
                    .apiKey(apiKey)
                    .connectionTimeout(connectionTimeout)
                    .contentType(contentType)
                    .host(host)
                    .socketTimeout(socketTimeOut)
                    .build();
            httpClient = HttpClient.create(clientId, httpClientOptions);
            sensor.registerClient(httpClient);
        }
    }

    @Override
    public void send(Envelope envelope) {
        sensor.send(envelope);
    }

    @Override
    public void sendAssignmentStarted(Participant participant, Assessment assessment) {

    }

    @Override
    public void sendAssignmentSubmitted(Participant participant, Assessment assessment) {

    }

    @Override
    public void sendAssignmentRestarted(Participant participant, Assessment assessment) {

    }

    @Override
    public void sendNavigationEvent(Participant participant, String whereTo) {

    }

    @Override
    public void sendFeedbackEvent(Participant participant, Assessment assessment) {

    }

    @Override
    public void sendViewGradeEvent(Participant participant, Assessment assessment) {

    }

    @Override
    public void sendToolUseEvent(LtiMembershipEntity membershipEntity) {
        DateTime time = DateTime.now();
        Person actor = prepareActor(membershipEntity);
        CaliperOrganization group = prepareGroup(membershipEntity);
        String uuid = "urn:uuid:" + UUID.randomUUID();
        if (caliperSend) {
            log.debug("Caliper event being generated: Tool Use");
            ToolUseEvent toolUseEvent = ToolUseEvent.builder()
                    .id(uuid)
                    .actor(actor)
                    .action(Action.USED)
                    .edApp(softwareApplication)
                    .context(context)
                    .eventTime(DateTime.now())
                    .object(softwareApplication)
                    .group(group)
                    .build();
            Envelope envelope = new Envelope(sensor.getId(), DateTime.now(), DATA_VERSION, Collections.singletonList(toolUseEvent));
            send(envelope);
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
            event.setMembershipId(membershipEntity.getUser().getUserKey());
            event.setMembershipRoles(roleToString(membershipEntity.getRole()));
            saveEvent(event);
            log.debug("Event Saved");
        }

    }

    private void saveEvent(Event event){
        allRepositories.eventRepository.save(event);
    }


    private Person prepareActor(LtiMembershipEntity participant){
        Person actor = Person.builder()
                .id(participant.getUser().getUserKey())
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
    private CaliperEntity prepareObject(Object object, String type){

        return null;
    }

    private CaliperOrganization prepareGroup(LtiMembershipEntity participant){
        LtiContextEntity contextEntity = participant.getContext();
        return CourseSection.builder()
                .name(contextEntity.getTitle())
                .id(courseUrl(contextEntity.getContext_memberships_url()))
                .type(EntityType.COURSE_SECTION).build();
    }

    private CaliperGeneratable prepareGenerated(){
        return null;
    }


    private String courseUrl(String membershipUrl){
            return StringUtils.removeEnd(membershipUrl, "/names_and_roles");
    }


    private String roleToString(int role) {

        if (role == 2) {
            return LtiStrings.LTI_ROLE_MEMBERSHIP_ADMIN;
        } else if (role == LtiStrings.ROLE_INSTRUCTOR) {
            return LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR;
        } else if (role == LtiStrings.ROLE_STUDENT){
            return LtiStrings.LTI_ROLE_LEARNER;
        } else {
            return null;
        }

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

    public Sensor getSensor() {
        return sensor;
    }

    public void setSensor(Sensor sensor) {
        this.sensor = sensor;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public SoftwareApplication getSoftwareApplication() {
        return softwareApplication;
    }

    public JsonldContext getContext() {
        return context;
    }
}
