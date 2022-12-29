package edu.iu.terracotta.model.app.dto.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.imsglobal.caliper.actions.Action;
import org.imsglobal.caliper.events.EventType;
import org.joda.time.DateTime;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
    private MediaLocationDto target;
    private final DateTime eventTime = new DateTime();
    private String edApp;
    private GroupDto group;
    private MembershipDto membership;
    private SessionDto session;
    private  Map<String, Object> extensions;

}
