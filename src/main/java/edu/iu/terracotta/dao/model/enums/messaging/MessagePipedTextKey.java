package edu.iu.terracotta.dao.model.enums.messaging;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessagePipedTextKey {

    STUDENT_NAME("Student name"),
    ID("ID"),
    COURSE_NAME("Course name"),
    EMAIL_ADDRESS("Email address");

    private String key;

    public String key() {
        return key;
    }

}
