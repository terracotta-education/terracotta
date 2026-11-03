package edu.iu.terracotta.connectors.caliper.dao.model.events;

import edu.iu.terracotta.connectors.caliper.dao.model.enums.Action;
import edu.iu.terracotta.connectors.caliper.dao.model.enums.EventType;
import lombok.experimental.SuperBuilder;

import java.util.EnumSet;
import java.util.Set;

/**
 * A participant interacting with a video. The action comes from the browser, so it's checked against the ones
 * Caliper allows for media events.
 */
@SuperBuilder
public class MediaEvent extends CaliperEvent {

    public static final Set<Action> SUPPORTED_ACTIONS = EnumSet.of(
        Action.STARTED,
        Action.ENDED,
        Action.PAUSED,
        Action.RESUMED,
        Action.RESTARTED,
        Action.FORWARDED_TO,
        Action.JUMPED_TO,
        Action.REWOUND,
        Action.CHANGED_RESOLUTION,
        Action.CHANGED_SIZE,
        Action.CHANGED_SPEED,
        Action.CHANGED_VOLUME,
        Action.DISABLED_CLOSED_CAPTIONING,
        Action.ENABLED_CLOSED_CAPTIONING,
        Action.EXITED_FULLSCREEN,
        Action.ENTERED_FULLSCREEN,
        Action.OPENED_POPOUT,
        Action.CLOSED_POPOUT,
        Action.MUTED,
        Action.UNMUTED
    );

    @Override
    public EventType getType() {
        return EventType.MEDIA;
    }

    public static void checkAction(Action action) {
        if (action == null) {
            throw new IllegalArgumentException("an action must be specified");
        }

        if (!SUPPORTED_ACTIONS.contains(action)) {
            throw new IllegalArgumentException(String.format("%s action is not supported", action));
        }
    }

}
