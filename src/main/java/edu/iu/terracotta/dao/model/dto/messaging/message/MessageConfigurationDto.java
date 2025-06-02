package edu.iu.terracotta.dao.model.dto.messaging.message;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import edu.iu.terracotta.dao.model.dto.messaging.email.MessageEmailReplyToDto;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;
import edu.iu.terracotta.dao.model.enums.messaging.MessageType;
import edu.iu.terracotta.dao.model.enums.messaging.MessageRecipientMatchType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessageConfigurationDto {

    private UUID id;
    private UUID messageId;
    private List<MessageEmailReplyToDto> replyTo;
    private Timestamp sendAt;
    private Integer sendAtTimezoneOffset;
    private MessageStatus status;
    private String subject;
    private boolean toConsentedOnly;
    private MessageType type;
    private boolean enabled;
    private MessageRecipientMatchType matchType;

}
