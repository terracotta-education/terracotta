package edu.iu.terracotta.connectors.canvas.dao.model.extended.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkflowState {

    DELETED("deleted"),
    GRADED("graded"),
    PENDING_REVIEW("pending_review"),
    SUBMITTED("submitted"),
    UNSUBMITTED("unsubmitted");

    private String state;

    public String state() {
        return state;
    }

}
