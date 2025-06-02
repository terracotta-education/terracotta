package edu.iu.terracotta.dao.model.distribute.export.messages;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
public class MessageContainerConfigurationExport {

    private long id;
    private long containerId;
    private int containerOrder;
    private String title;
    private boolean toConsentedOnly;
    private MessageType type;
    private List<MessageEmailReplyToExport> replyTos;

}
