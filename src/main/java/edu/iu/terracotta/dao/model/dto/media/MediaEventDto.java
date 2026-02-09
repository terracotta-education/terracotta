package edu.iu.terracotta.dao.model.dto.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.imsglobal.caliper.actions.Action;
import org.imsglobal.caliper.events.EventType;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings({"PMD.LooseCoupling"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaEventDto extends AbstractDto {

    @JsonProperty("@context")
    private String context;

    @Builder.Default private Timestamp eventTime = new Timestamp(Instant.now().toEpochMilli());
    @Builder.Default private EventType type = EventType.MEDIA;

    private String id;
    private String profile;
    private PersonDto actor;
    private Action action;
    private MediaObjectDto object;
    private MediaLocationDto target;
    private String edApp;
    private GroupDto group;
    private MembershipDto membership;
    private SessionDto session;
    private  Map<String, Object> extensions;

}
