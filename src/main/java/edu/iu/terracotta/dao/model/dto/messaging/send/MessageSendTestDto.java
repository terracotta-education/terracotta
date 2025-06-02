package edu.iu.terracotta.dao.model.dto.messaging.send;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessageSendTestDto {

    private String message;
    private String subject;
    private String to;

}
