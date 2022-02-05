package edu.iu.terracotta.model.app.dto.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.imsglobal.caliper.actions.Action;
import org.imsglobal.caliper.entities.resource.MediaLocation;
import org.imsglobal.caliper.events.Event;
import org.imsglobal.caliper.events.EventType;
import org.joda.time.DateTime;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaEventDto extends AbstractDto {

    @JsonProperty("@context")
    private String context;

    private String id;

    private EventType type= EventType.MEDIA;

    private String profile;

    private PersonDto actor;

    private Action action;

    private MediaObjectDto object;

    private MediaLocationDto location;

    private final DateTime eventTime = new DateTime();

    private String edApp;

    private GroupDto group;

    private MembershipDto membership;

    private SessionDto session;

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public PersonDto getActor() {
        return actor;
    }

    public void setActor(PersonDto actor) {
        this.actor = actor;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public MediaObjectDto getObject() {
        return object;
    }

    public void setObject(MediaObjectDto object) {
        this.object = object;
    }

    public MediaLocationDto getLocation() {
        return location;
    }

    public void setLocation(MediaLocationDto location) {
        this.location = location;
    }

    public DateTime getEventTime() {
        return eventTime;
    }

    public String getEdApp() {
        return edApp;
    }

    public void setEdApp(String edApp) {
        this.edApp = edApp;
    }

    public GroupDto getGroup() {
        return group;
    }

    public void setGroup(GroupDto group) {
        this.group = group;
    }

    public MembershipDto getMembership() {
        return membership;
    }

    public void setMembership(MembershipDto membership) {
        this.membership = membership;
    }

    public SessionDto getSession() {
        return session;
    }

    public void setSession(SessionDto session) {
        this.session = session;
    }
}
