package edu.iu.terracotta.dao.model.enums.export;

import java.util.Arrays;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum MessagesCsv {

    // NOTE: order is important!
    MESSAGE_ID("message_id"),
    MESSAGE_TYPE("message_type"),
    COURSE_ID("course_id"),
    EXPERIMENT_ID("experiment_id"),
    CONDITION_ID("condition_id"),
    CONDITION_NAME("condition_name"),
    SUBJECT("subject"),
    PARTICIPANT_ID("participant_id"),
    PARTICIPANT_STATUS("participant_status"),
    SENT_AT("sent_at");

    public static final String FILENAME = "messages.csv";

    private String header;

    @Override
    public String toString() {
        return header;
    }

    public static String[] getHeaderRow() {
        return Arrays.stream(values())
            .map(MessagesCsv::toString)
            .toArray(String[]::new);
    }

}
