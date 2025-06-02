package edu.iu.terracotta.service.app.messaging;

import java.util.List;

import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedText;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItem;
import edu.iu.terracotta.dao.model.dto.messaging.piped.MessagePipedTextItemDto;

public interface MessagePipedTextItemService {

    void create(List<MessagePipedTextItemDto> pipedTextItemDtos, MessagePipedText pipedText);
    void update(MessagePipedTextItemDto pipedTextItemDto, MessagePipedTextItem pipedTextItem);
    void upsert(List<MessagePipedTextItemDto> pipedTextItemDtos, MessagePipedText pipedText);
    void duplicate(List<MessagePipedTextItem> pipedTextItems, MessagePipedText pipedText);
    MessagePipedTextItem fromDto(MessagePipedTextItemDto pipedTextItemDto, MessagePipedTextItem pipedTextItem);
    MessagePipedTextItem fromDto(MessagePipedTextItemDto pipedTextItemDto, MessagePipedTextItem pipedTextItem, boolean includeItemValues);
    List<MessagePipedTextItem> fromDto(List<MessagePipedTextItemDto> pipedTextItemDtos, MessagePipedText pipedText);
    List<MessagePipedTextItem> fromDto(List<MessagePipedTextItemDto> pipedTextItemDtos, MessagePipedText pipedText, boolean includeItemValues);
    MessagePipedTextItemDto toDto(MessagePipedTextItem pipedTextItem);
    List<MessagePipedTextItemDto> toDto(List<MessagePipedTextItem> pipedTextItems);

}
