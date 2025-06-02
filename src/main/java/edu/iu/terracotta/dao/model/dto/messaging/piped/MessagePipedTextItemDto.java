package edu.iu.terracotta.dao.model.dto.messaging.piped;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessagePipedTextItemDto {

    private UUID id;
    private UUID pipedTextId;
    private String key;
    private List<MessagePipedTextItemValueDto> values;

}
