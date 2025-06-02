package edu.iu.terracotta.service.app.messaging;

import java.util.List;

import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.model.dto.messaging.conditional.MessageConditionalTextDto;

public interface MessageConditionalTextService {

    void create(MessageConditionalTextDto conditionalTextDto, MessageContent content);
    MessageConditionalTextDto post(MessageConditionalTextDto conditionalTextDto, MessageContent content);
    void update(MessageConditionalTextDto conditionalTextDto, MessageConditionalText conditionalText);
    MessageConditionalTextDto put(MessageConditionalTextDto conditionalTextDto, MessageConditionalText conditionalText);
    void duplicate(List<MessageConditionalText> conditionalTexts, MessageContent content);
    void duplicate(MessageConditionalText conditionalText, MessageContent content);
    void delete(MessageConditionalText conditionalText);
    void upsert(List<MessageConditionalTextDto> conditionalTextDtos, MessageContent content);
    List<MessageConditionalTextDto> toDto(List<MessageConditionalText> conditionalTexts);
    MessageConditionalTextDto toDto(MessageConditionalText conditionalText);
    MessageConditionalText fromDto(MessageConditionalTextDto conditionalTextDto, MessageConditionalText conditionalText);
    MessageConditionalText fromDto(MessageConditionalTextDto conditionalTextDto, MessageConditionalText conditionalText, boolean includeResult, boolean includeRuleSets);
    List<MessageConditionalText> fromDto(List<MessageConditionalTextDto> conditionalTextDtos, MessageContent content, boolean includeResult, boolean includeRuleSets);

}
