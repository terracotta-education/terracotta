package edu.iu.terracotta.dao.model.dto.messaging.piped;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessagePipedTextDto {

    private UUID id;
    private UUID contentId;
    private String fileName;
    private List<MessagePipedTextItemDto> items;

}
