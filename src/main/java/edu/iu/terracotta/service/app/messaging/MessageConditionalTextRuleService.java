package edu.iu.terracotta.service.app.messaging;

import java.util.List;

import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextRule;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextRuleSet;
import edu.iu.terracotta.dao.model.dto.messaging.conditional.MessageConditionalTextRuleDto;

public interface MessageConditionalTextRuleService {

    void create(MessageConditionalTextRuleDto conditionalTextRuleDto, MessageConditionalTextRuleSet conditionalTextRuleSet);
    void create(List<MessageConditionalTextRuleDto> conditionalTextRuleDtos, MessageConditionalTextRuleSet conditionalTextRuleSet);
    void update(MessageConditionalTextRuleDto conditionalTextRuleDto, MessageConditionalTextRule conditionalTextRule);
    void update(List<MessageConditionalTextRuleDto> conditionalTextRuleDtos, MessageConditionalTextRuleSet conditionalTextRuleSet);
    void duplicate(List<MessageConditionalTextRule> conditionalTextRules, MessageConditionalTextRuleSet conditionalTextRuleSet);
    List<MessageConditionalTextRuleDto> toDto(List<MessageConditionalTextRule> conditionalTextRules);
    MessageConditionalTextRuleDto toDto(MessageConditionalTextRule conditionalTextRule);
    MessageConditionalTextRule fromDto(MessageConditionalTextRuleDto conditionalTextRuleDto, MessageConditionalTextRule conditionalTextRule);
    List<MessageConditionalTextRule> fromDto(List<MessageConditionalTextRuleDto> conditionalTextRuleDtos, MessageConditionalTextRuleSet conditionalTextRuleSet);

}
