package edu.iu.terracotta.connectors.caliper.dao.model.events;

import edu.iu.terracotta.connectors.caliper.dao.model.enums.EventType;
import lombok.experimental.SuperBuilder;

/**
 * A participant starting, restarting or submitting an assessment.
 */
@SuperBuilder
public class AssessmentEvent extends CaliperEvent {

    @Override
    public EventType getType() {
        return EventType.ASSESSMENT;
    }

}
