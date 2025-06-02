package edu.iu.terracotta.dao.model.enums.export;

import java.util.Arrays;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum MessageConditions {

    // NOTE: order is important!
    MESSAGE_ID("message_id"),
    COURSE_ID("course_id"),
    EXPERIMENT_ID("experiment_id"),
    RULESET_ID("ruleset_id"),
    RULESET_OPERATOR("ruleset_operator"),
    RULE_ID("rule_id"),
    RULE_OPERATOR("rule_operator"),
    RULE_LMS_ASSIGNMENT_ID("rule_lms_assignment_id"),
    RULE_COMPARISON("rule_comparison"),
    RULE_VALUE("rule_value"),
    FILTER("filter");

    public static final String FILENAME = "message_conditions.csv";

    private String header;

    @Override
    public String toString() {
        return header;
    }

    public static String[] getHeaderRow() {
        return Arrays.stream(values())
            .map(MessageConditions::toString)
            .toArray(String[]::new);
    }

}
