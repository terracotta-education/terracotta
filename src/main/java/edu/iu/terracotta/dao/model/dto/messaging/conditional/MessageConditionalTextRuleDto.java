package edu.iu.terracotta.dao.model.dto.messaging.conditional;

import java.util.UUID;

import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleAssignmentDto;
import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleComparisonDto;
import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleOperator;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessageConditionalTextRuleDto {

    private UUID id;
    private UUID ruleSetId;
    private String lmsAssignmentId;
    private MessageRuleAssignmentDto assignment;
    private MessageRuleComparisonDto comparison;
    private String value;
    private MessageRuleOperator operator;

}
