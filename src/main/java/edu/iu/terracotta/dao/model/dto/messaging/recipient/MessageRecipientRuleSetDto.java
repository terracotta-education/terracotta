package edu.iu.terracotta.dao.model.dto.messaging.recipient;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.model.enums.messaging.rule.MessageRuleOperator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageRecipientRuleSetDto {

    private UUID id;
    private UUID messageId;
    private MessageRuleOperator operator;
    private List<MessageRecipientRuleDto> rules;

}
