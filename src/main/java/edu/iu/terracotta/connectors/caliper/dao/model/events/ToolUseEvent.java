package edu.iu.terracotta.connectors.caliper.dao.model.events;

import edu.iu.terracotta.connectors.caliper.dao.model.enums.EventType;
import lombok.experimental.SuperBuilder;

/**
 * A person launching Terracotta.
 */
@SuperBuilder
public class ToolUseEvent extends CaliperEvent {

    @Override
    public EventType getType() {
        return EventType.TOOL_USE;
    }

}
