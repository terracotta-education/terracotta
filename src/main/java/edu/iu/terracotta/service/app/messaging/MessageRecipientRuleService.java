package edu.iu.terracotta.service.app.messaging;

import java.util.List;

import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRule;
import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRuleSet;
import edu.iu.terracotta.dao.model.dto.messaging.recipient.MessageRecipientRuleDto;

public interface MessageRecipientRuleService {

    void create(MessageRecipientRuleDto recipientRuleDto, MessageRecipientRuleSet recipientRuleSet);
    void create(List<MessageRecipientRuleDto> recipientRuleDtos, MessageRecipientRuleSet recipientRuleSet);
    void update(MessageRecipientRuleDto recipientRuleDto, MessageRecipientRule recipientRule);
    void update(List<MessageRecipientRuleDto> recipientRuleDto, MessageRecipientRuleSet recipientRuleSet);
    void duplicate(List<MessageRecipientRule> recipientRules, MessageRecipientRuleSet recipientRuleSet);
    List<MessageRecipientRuleDto> toDto(List<MessageRecipientRule> recipientRules);
    MessageRecipientRuleDto toDto(MessageRecipientRule recipientRule);
    MessageRecipientRule fromDto(MessageRecipientRuleDto recipientRuleDto, MessageRecipientRule recipientRule);

}
