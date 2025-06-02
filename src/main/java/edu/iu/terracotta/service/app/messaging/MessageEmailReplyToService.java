package edu.iu.terracotta.service.app.messaging;

import java.util.List;

import edu.iu.terracotta.dao.entity.messaging.container.MessageContainerConfiguration;
import edu.iu.terracotta.dao.entity.messaging.message.MessageConfiguration;
import edu.iu.terracotta.dao.entity.messaging.replyto.MessageEmailReplyTo;
import edu.iu.terracotta.dao.model.dto.messaging.container.MessageContainerConfigurationDto;
import edu.iu.terracotta.dao.model.dto.messaging.email.MessageEmailReplyToDto;
import edu.iu.terracotta.dao.model.dto.messaging.message.MessageConfigurationDto;

public interface MessageEmailReplyToService {

    void create(MessageContainerConfiguration configuration);
    void create(MessageConfiguration configuration);
    void update(MessageContainerConfigurationDto containerConfigurationDto, MessageContainerConfiguration containerConfiguration);
    void update(MessageConfigurationDto configurationDto, MessageConfiguration configuration);
    void duplicate(List<MessageEmailReplyTo> emailReplyTos, MessageContainerConfiguration containerConfiguration);
    void duplicate(List<MessageEmailReplyTo> emailReplyTos, MessageConfiguration configuration);
    List<MessageEmailReplyToDto> toDto(List<MessageEmailReplyTo> emailReplyTos);
    MessageEmailReplyToDto toDto(MessageEmailReplyTo emailReplyTo);

}
