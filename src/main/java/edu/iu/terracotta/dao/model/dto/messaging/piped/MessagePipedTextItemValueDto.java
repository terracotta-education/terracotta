package edu.iu.terracotta.dao.model.dto.messaging.piped;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class MessagePipedTextItemValueDto {

    private UUID id;
    private UUID pipedTextItemId;
    private long userId;
    private String value;

}
