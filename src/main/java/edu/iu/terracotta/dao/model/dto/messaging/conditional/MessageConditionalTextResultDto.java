package edu.iu.terracotta.dao.model.dto.messaging.conditional;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessageConditionalTextResultDto {

    private UUID id;
    private UUID conditionalTextId;
    private String html;

}
