package edu.iu.terracotta.dao.model.dto.messaging.rule;

import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleComparison;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessageRuleComparisonDto {

    private MessageRuleComparison id;
    private String label;
    private boolean requiresValue;

}
