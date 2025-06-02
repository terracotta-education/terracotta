package edu.iu.terracotta.dao.model.dto.messaging.email;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessageEmailReplyToDto {

    private UUID id;
    private UUID containerConfigurationId;
    private UUID messageConfigurationId;
    private String email;

}
