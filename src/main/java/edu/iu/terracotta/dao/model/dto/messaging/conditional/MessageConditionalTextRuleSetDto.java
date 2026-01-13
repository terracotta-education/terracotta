package edu.iu.terracotta.dao.model.dto.messaging.conditional;

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
public class MessageConditionalTextRuleSetDto {

    private UUID id;
    private UUID conditionalTextId;
    private MessageRuleOperator operator;
    private List<MessageConditionalTextRuleDto> rules;

}
