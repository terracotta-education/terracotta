package edu.iu.terracotta.dao.model.enums.export;

import java.util.Arrays;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum MessageContentCsv {

    // NOTE: order is important!
    MESSAGE_ID("message_id"),
    COURSE_ID("course_id"),
    EXPERIMENT_ID("experiment_id"),
    BASE_MESSAGE("base_message");

    public static final String FILENAME = "message_content.csv";

    private String header;

    @Override
    public String toString() {
        return header;
    }

    public static String[] getHeaderRow() {
        return Arrays.stream(values())
            .map(MessageContentCsv::toString)
            .toArray(String[]::new);
    }

}
