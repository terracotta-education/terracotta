package edu.iu.terracotta.dao.model.enums.messaging.rule;

import java.util.List;

public enum MessageRuleAssignmentGradingType {

    gpa_scale,
    letter_grade,
    pass_fail,
    percent,
    points,
    unknown;

    public List<MessageRuleComparison> getAllowedComparisons() {
        switch (this) {
            case gpa_scale:
            case percent:
            case points:
                return List.of(
                    MessageRuleComparison.EQUALS,
                    MessageRuleComparison.GREATER_THAN,
                    MessageRuleComparison.GREATER_THAN_EQUAL,
                    MessageRuleComparison.LESS_THAN,
                    MessageRuleComparison.LESS_THAN_EQUAL,
                    MessageRuleComparison.IS_SUBMITTED,
                    MessageRuleComparison.IS_NOT_YET_SUBMITTED,
                    MessageRuleComparison.IS_SUBMITTED_BUT_NOT_YET_GRADED
                );
            case letter_grade:
            case pass_fail:
            case unknown:
            default:
                return List.of(
                    MessageRuleComparison.IS_SUBMITTED,
                    MessageRuleComparison.IS_NOT_YET_SUBMITTED,
                    MessageRuleComparison.IS_SUBMITTED_BUT_NOT_YET_GRADED
                );
        }
    }
}
