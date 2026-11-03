package edu.iu.terracotta.connectors.caliper.dao.model.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.caliper.dao.model.enums.EventType;
import lombok.experimental.SuperBuilder;

/**
 * A participant viewing their grade.
 *
 * The object, the participant's Result, is left out of the JSON. caliper-java 1.1.2, which Terracotta used
 * before, only serialized a digital resource as a view event's object and silently dropped a Result, so no
 * view event Terracotta has sent or stored has ever included it. It's left out here to keep the events the
 * same; remove the ignore to start sending it.
 */
@SuperBuilder
@JsonIgnoreProperties({"object"})
public class ViewEvent extends CaliperEvent {

    @Override
    public EventType getType() {
        return EventType.VIEW;
    }

}
