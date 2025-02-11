package edu.iu.terracotta.dao.model.enums.export;

import java.util.Arrays;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum EventPersonalIdentifiers {

    // NOTE: order is important!
    LMS_LOGIN_ID("lms_login_id"),
    LMS_USER_NAME("lms_user_name"),
    LMS_GLOBAL_ID("lms_global_id"),
    LMS_USER_ID("lms_user_id"),
    LMS_USER_GLOBAL_ID("lms_user_global_id");

    public static final String FILENAME = "events.json";

    private String header;

    @Override
    public String toString() {
        return header;
    }

    public static String[] getFields() {
        return Arrays.stream(values())
            .map(EventPersonalIdentifiers::toString)
            .toArray(String[]::new);
    }

}
