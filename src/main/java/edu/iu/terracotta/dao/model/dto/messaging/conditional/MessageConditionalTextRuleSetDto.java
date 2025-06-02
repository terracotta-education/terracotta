package edu.iu.terracotta.dao.model.dto.messaging.conditional;

import java.util.List;
import java.util.UUID;

import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleOperator;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessageConditionalTextRuleSetDto {

    private UUID id;
    private UUID conditionalTextId;
    private MessageRuleOperator operator;
    private List<MessageConditionalTextRuleDto> rules;

}
