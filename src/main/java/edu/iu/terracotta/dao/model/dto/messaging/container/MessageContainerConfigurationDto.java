package edu.iu.terracotta.dao.model.dto.messaging.container;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import edu.iu.terracotta.dao.model.dto.messaging.email.MessageEmailReplyToDto;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;
import edu.iu.terracotta.dao.model.enums.messaging.MessageType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessageContainerConfigurationDto {

    private UUID id;
    private UUID containerId;
    private MessageStatus status;
    private String title;
    private boolean toConsentedOnly;
    private List<MessageEmailReplyToDto> replyTo;
    private Timestamp sendAt;
    private Integer sendAtTimezoneOffset;
    private MessageType type;
    private int order;

}
