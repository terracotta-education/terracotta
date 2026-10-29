package edu.iu.terracotta.connectors.caliper.dao.model.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import edu.iu.terracotta.connectors.caliper.dao.model.entities.CaliperEntity;
import edu.iu.terracotta.connectors.caliper.dao.model.entities.CourseSection;
import edu.iu.terracotta.connectors.caliper.dao.model.entities.LtiSession;
import edu.iu.terracotta.connectors.caliper.dao.model.entities.Membership;
import edu.iu.terracotta.connectors.caliper.dao.model.entities.Person;
import edu.iu.terracotta.connectors.caliper.dao.model.entities.SoftwareApplication;
import edu.iu.terracotta.connectors.caliper.dao.model.enums.Action;
import edu.iu.terracotta.connectors.caliper.dao.model.enums.EventType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;

/**
 * The fields every Caliper event Terracotta sends can carry. Fields left unset are left out of the JSON.
 */
@Getter
@SuperBuilder
@JsonPropertyOrder({"@context", "id", "type", "actor", "action", "object", "target", "generated", "referrer", "eventTime", "edApp", "group", "membership", "federatedSession", "extensions"})
public abstract class CaliperEvent {

    @JsonProperty("@context") private final String context;
    private final String id;
    private final Person actor;
    private final Action action;
    private final CaliperEntity object;
    private final CaliperEntity target;
    private final CaliperEntity generated;
    private final SoftwareApplication referrer;
    private final Instant eventTime;
    private final SoftwareApplication edApp;
    private final CourseSection group;
    private final Membership membership;
    private final LtiSession federatedSession;
    private final Map<String, Object> extensions;

    @JsonProperty("type")
    public abstract EventType getType();

}
