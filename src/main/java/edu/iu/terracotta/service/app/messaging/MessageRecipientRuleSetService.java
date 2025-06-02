package edu.iu.terracotta.service.app.messaging;

import java.util.List;

import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRuleSet;
import edu.iu.terracotta.dao.model.dto.messaging.recipient.MessageRecipientRuleSetDto;

public interface MessageRecipientRuleSetService {

    void create(MessageRecipientRuleSetDto recipientRuleSetDto, Message message);
    void update(MessageRecipientRuleSetDto recipientRuleSetDto, MessageRecipientRuleSet recipientRuleSet);
    void update(List<MessageRecipientRuleSetDto> recipientRuleSetDtos, Message message);
    void duplicate(List<MessageRecipientRuleSet> recipientRuleSets, Message message);
    List<MessageRecipientRuleSetDto> toDto(List<MessageRecipientRuleSet> recipientRuleSets);
    MessageRecipientRuleSetDto toDto(MessageRecipientRuleSet recipientRuleSet);
    MessageRecipientRuleSet fromDto(MessageRecipientRuleSetDto recipientRuleSetDto, MessageRecipientRuleSet recipientRuleSet);

}
