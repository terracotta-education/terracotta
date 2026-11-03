package edu.iu.terracotta.connectors.caliper.dao.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;

/**
 * Caliper membership status.
 */
@AllArgsConstructor
public enum Status {

    ACTIVE("Active"),
    INACTIVE("Inactive");

    private final String value;

    @JsonValue
    public String value() {
        return value;
    }

}
