package edu.iu.terracotta.service.app.messaging;

import java.util.List;

import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalTextRuleSet;
import edu.iu.terracotta.dao.model.dto.messaging.conditional.MessageConditionalTextRuleSetDto;

public interface MessageConditionalTextRuleSetService {

    void create(MessageConditionalTextRuleSetDto conditionalTextRuleSetDto, MessageConditionalText conditionalText);
    void create(List<MessageConditionalTextRuleSetDto> conditionalTextRuleSetDtos, MessageConditionalText conditionalText);
    void update(MessageConditionalTextRuleSetDto conditionalTextRuleSetDto, MessageConditionalTextRuleSet conditionalTextRuleSet);
    void update(List<MessageConditionalTextRuleSetDto> conditionalTextRuleSetDtos, MessageConditionalText conditionalText);
    void duplicate(List<MessageConditionalTextRuleSet> conditionalTextRuleSets, MessageConditionalText conditionalText);
    List<MessageConditionalTextRuleSetDto> toDto(List<MessageConditionalTextRuleSet> conditionalTextRuleSets);
    MessageConditionalTextRuleSetDto toDto(MessageConditionalTextRuleSet conditionalTextRuleSet);
    MessageConditionalTextRuleSet fromDto(MessageConditionalTextRuleSetDto conditionalTextRuleSetDto, MessageConditionalTextRuleSet conditionalTextRuleSet);
    MessageConditionalTextRuleSet fromDto(MessageConditionalTextRuleSetDto conditionalTextRuleSetDto, MessageConditionalTextRuleSet conditionalTextRuleSet, boolean includeRules);
    List<MessageConditionalTextRuleSet> fromDto(List<MessageConditionalTextRuleSetDto> conditionalTextRuleSetDtos, MessageConditionalText conditionalText, boolean includeRules);

}
