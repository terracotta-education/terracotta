package edu.iu.terracotta.service.app.messaging;

import java.util.List;

import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItem;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItemValue;
import edu.iu.terracotta.dao.model.dto.messaging.piped.MessagePipedTextItemValueDto;

public interface MessagePipedTextItemValueService {

    void create(MessagePipedTextItemValueDto pipedTextItemValueDto, MessagePipedTextItem pipedTextItem);
    void create(List<MessagePipedTextItemValueDto> pipedTextItemValueDtos, MessagePipedTextItem pipedTextItem);
    void update(MessagePipedTextItemValueDto pipedTextItemValueDto, MessagePipedTextItemValue pipedTextItem);
    void update(List<MessagePipedTextItemValueDto> pipedTextItemValueDtos, MessagePipedTextItem pipedTextItem);
    void upsert(List<MessagePipedTextItemValueDto> pipedTextItemDtos, MessagePipedTextItem pipedTextItem);
    void duplicate(List<MessagePipedTextItemValue> pipedTextItemValues, MessagePipedTextItem pipedTextItem);
    MessagePipedTextItemValue fromDto(MessagePipedTextItemValueDto pipedTextItemValueDto, MessagePipedTextItemValue pipedTextItemValue);
    List<MessagePipedTextItemValue> fromDto(List<MessagePipedTextItemValueDto> pipedTextItemValueDtos, MessagePipedTextItem pipedTextItem);
    MessagePipedTextItemValueDto toDto(MessagePipedTextItemValue pipedTextItemValue);
    List<MessagePipedTextItemValueDto> toDto(List<MessagePipedTextItemValue> pipedTextItemValues);

}
