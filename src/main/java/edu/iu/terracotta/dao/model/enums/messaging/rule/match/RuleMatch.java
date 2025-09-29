package edu.iu.terracotta.dao.model.enums.messaging.rule.match;

import java.util.UUID;

import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleComparison;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleOperator;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RuleMatch {

    private UUID ruleUuid;
    private MessageRuleComparison comparison;
    private MessageRuleOperator operator;
    private String value;

    @Builder.Default private boolean match = false;

}
