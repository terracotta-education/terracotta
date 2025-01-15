package edu.iu.terracotta.model.app.integrations.enums;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public enum IntegrationLaunchParameter {

    ANONYMOUS_ID("anonymous_id"),
    ASSIGNMENT_ID("assignment_id"),
    CONDITION_NAME("condition_name"),
    DUE_AT("due_at"),
    EXPERIMENT_ID("experiment_id"),
    LAUNCH_TOKEN("launch_token"),
    REMAINING_ATTEMPTS("remaining_attempts"),
    SUBMISSION_ID("submission_id");

    private String key;

    public String key() {
        return this.key;
    }

}
