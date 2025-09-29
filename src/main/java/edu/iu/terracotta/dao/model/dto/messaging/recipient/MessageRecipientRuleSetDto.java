package edu.iu.terracotta.dao.model.dto.messaging.recipient;

import java.util.List;
import java.util.UUID;

import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleOperator;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessageRecipientRuleSetDto {

    private UUID id;
    private UUID messageId;
    private MessageRuleOperator operator;
    private List<MessageRecipientRuleDto> rules;

}
