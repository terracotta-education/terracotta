package edu.iu.terracotta.connectors.caliper.dao.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;

/**
 * The Caliper membership roles Terracotta maps LTI roles to.
 */
@AllArgsConstructor
public enum Role {

    ADMINISTRATOR("Administrator"),
    INSTRUCTOR("Instructor"),
    LEARNER("Learner");

    private final String value;

    @JsonValue
    public String value() {
        return value;
    }

}
