package edu.iu.terracotta.connectors.caliper.dao.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;

/**
 * Caliper 1.1 actions, written to and read from JSON by their Caliper value.
 */
@AllArgsConstructor
public enum Action {

    ABANDONED("Abandoned"),
    ACTIVATED("Activated"),
    ADDED("Added"),
    ATTACHED("Attached"),
    BOOKMARKED("Bookmarked"),
    CHANGED_RESOLUTION("ChangedResolution"),
    CHANGED_SIZE("ChangedSize"),
    CHANGED_SPEED("ChangedSpeed"),
    CHANGED_VOLUME("ChangedVolume"),
    CLASSIFIED("Classified"),
    CLOSED_POPOUT("ClosedPopout"),
    COMMENTED("Commented"),
    COMPLETED("Completed"),
    CREATED("Created"),
    DEACTIVATED("Deactivated"),
    DELETED("Deleted"),
    DESCRIBED("Described"),
    DISABLED_CLOSED_CAPTIONING("DisabledClosedCaptioning"),
    DISLIKED("Disliked"),
    ENABLED_CLOSED_CAPTIONING("EnabledClosedCaptioning"),
    ENDED("Ended"),
    ENTERED_FULLSCREEN("EnteredFullScreen"),
    EXITED_FULLSCREEN("ExitedFullScreen"),
    FORWARDED_TO("ForwardedTo"),
    GRADED("Graded"),
    HID("Hid"),
    HIGHLIGHTED("Highlighted"),
    JUMPED_TO("JumpedTo"),
    IDENTIFIED("Identified"),
    LIKED("Liked"),
    LINKED("Linked"),
    LOGGED_IN("LoggedIn"),
    LOGGED_OUT("LoggedOut"),
    MARKED_AS_READ("MarkedAsRead"),
    MARKED_AS_UNREAD("MarkedAsUnread"),
    MODIFIED("Modified"),
    MUTED("Muted"),
    NAVIGATED_TO("NavigatedTo"),
    OPENED_POPOUT("OpenedPopout"),
    PAUSED("Paused"),
    POSTED("Posted"),
    QUESTIONED("Questioned"),
    RANKED("Ranked"),
    RECOMMENDED("Recommended"),
    REMOVED("Removed"),
    REPLIED("Replied"),
    RESET("Reset"),
    RESTARTED("Restarted"),
    RESUMED("Resumed"),
    RETRIEVED("Retrieved"),
    REVIEWED("Reviewed"),
    REWOUND("Rewound"),
    SEARCHED("Searched"),
    SHARED("Shared"),
    SHOWED("Showed"),
    SKIPPED("Skipped"),
    STARTED("Started"),
    SUBMITTED("Submitted"),
    SUBSCRIBED("Subscribed"),
    TAGGED("Tagged"),
    TIMED_OUT("TimedOut"),
    UNMUTED("Unmuted"),
    UNSUBSCRIBED("Unsubscribed"),
    USED("Used"),
    VIEWED("Viewed");

    private final String value;

    @JsonValue
    public String value() {
        return value;
    }

}
