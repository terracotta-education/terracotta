package edu.iu.terracotta.runner.messaging.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessagingScheduleMessage {

    private long messageId;
    private Timestamp sendAt;
    private Timestamp beginAt;
    private Timestamp finishedAt;
    private List<String> errors;

    public void addError(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }

        errors.add(error);
    }

}
