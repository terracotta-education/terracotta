package edu.iu.terracotta.service.app.messaging;

import java.util.UUID;

import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.message.MessageConfiguration;
import edu.iu.terracotta.dao.model.dto.messaging.message.MessageConfigurationDto;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;

public interface MessageConfigurationService {

    void create(Message message);
    void update(MessageConfigurationDto configurationDto, Message message);
    void duplicate(MessageConfiguration configuration, Message message);
    void propogateContainerChanges(MessageContainer container);
    MessageConfigurationDto toDto(MessageConfiguration configuration, UUID messageUuid);
    MessageConfiguration fromDto(MessageConfigurationDto configurationDto, MessageConfiguration configuration);

    /**
     * Message is ready to send if all are true:
     *
     * 1. content body is not blank
     * 2. sendAt is not blank
     * 3. subject is not blank
     * 4. type is not blank
     * 5. if email type, no replyTo(s) are blank
     *
     * @param message
     * @return
     */
    MessageStatus calculateStatus(Message message);

}
