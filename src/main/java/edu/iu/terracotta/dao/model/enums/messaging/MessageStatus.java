package edu.iu.terracotta.dao.model.enums.messaging;

import java.util.List;

public enum MessageStatus {
    CANCELLED,
    COPIED,
    CREATED,
    DELETED,
    DISABLED,
    EDITED,
    ERROR,
    INCOMPLETE,
    PROCESSING,
    PUBLISHED,
    QUEUED,
    READY,
    SENT,
    UNPUBLISHED;

    public static List<MessageStatus> displayable() {
        return List.of(
            COPIED,
            CREATED,
            EDITED,
            INCOMPLETE,
            PROCESSING,
            PUBLISHED,
            QUEUED,
            READY,
            SENT,
            UNPUBLISHED
        );
    }

}
