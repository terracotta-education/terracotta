package edu.iu.terracotta.dao.model.enums.messaging.rule;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageRuleComparison {

    EQUALS("equals", true),
    GREATER_THAN("is greater than", true),
    GREATER_THAN_EQUAL("is greater than or equal to", true),
    LESS_THAN("is less than", true),
    LESS_THAN_EQUAL("is less than or equal to", true),
    IS_SUBMITTED("is submitted", false),
    IS_NOT_YET_SUBMITTED("is not yet submitted", false),
    IS_SUBMITTED_BUT_NOT_YET_GRADED("is submitted but not yet graded", false);

    private String label;
    private boolean requiresValue;

}
