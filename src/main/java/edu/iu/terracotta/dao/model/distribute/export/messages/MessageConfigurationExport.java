package edu.iu.terracotta.dao.model.distribute.export.messages;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.model.enums.messaging.MessageRecipientMatchType;
import edu.iu.terracotta.dao.model.enums.messaging.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageConfigurationExport {

    private long id;
    private long messageId;
    private String subject;
    private boolean enabled;
    private boolean toConsentedOnly;
    private MessageType type;
    private MessageRecipientMatchType recipientMatchType;
    private List<MessageEmailReplyToExport> replyTos;

}
