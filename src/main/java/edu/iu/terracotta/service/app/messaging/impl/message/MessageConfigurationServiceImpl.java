package edu.iu.terracotta.service.app.messaging.impl.message;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.message.MessageConfiguration;
import edu.iu.terracotta.dao.model.dto.messaging.message.MessageConfigurationDto;
import edu.iu.terracotta.dao.model.enums.messaging.MessageContainerUpdatedFields;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;
import edu.iu.terracotta.dao.model.enums.messaging.MessageType;
import edu.iu.terracotta.dao.model.enums.messaging.MessageRecipientMatchType;
import edu.iu.terracotta.dao.repository.messaging.message.MessageConfigurationRepository;
import edu.iu.terracotta.service.app.messaging.MessageConfigurationService;
import edu.iu.terracotta.service.app.messaging.MessageEmailReplyToService;

@Service
public class MessageConfigurationServiceImpl implements MessageConfigurationService {

    @Autowired private MessageConfigurationRepository configurationRepository;
    @Autowired private MessageEmailReplyToService messageEmailReplyToService;

    @Override
    public void create(Message message) {
        message.setConfiguration(
            MessageConfiguration.builder()
                .message(message)
                .recipientMatchType(MessageRecipientMatchType.INCLUDE)
                .status(MessageStatus.INCOMPLETE)
                .toConsentedOnly(false)
                .build()
        );
    }

    @Override
    public void update(MessageConfigurationDto configurationDto, Message message) {
        fromDto(
            configurationDto,
            message.getConfiguration()
        );

        // set message status
        message.getConfiguration().setStatus(calculateStatus(message));

        // set sendAt to container sendAt if not set
        if (message.getSendAt() == null) {
            message.getConfiguration().setSendAt(message.getContainer().getSendAt());
            message.getConfiguration().setSendAtTimezoneOffset(message.getContainer().getSendAtTimezoneOffset());
        }

        messageEmailReplyToService.update(configurationDto, message.getConfiguration());
    }

    @Override
    public void duplicate(MessageConfiguration configuration, Message message) {
        MessageConfiguration newMessageConfiguration = MessageConfiguration.builder()
            .enabled(configuration.isEnabled())
            .message(message)
            .recipientMatchType(configuration.getRecipientMatchType())
            .sendAt(null)
            .status(MessageStatus.INCOMPLETE)
            .subject(configuration.getSubject())
            .toConsentedOnly(configuration.isToConsentedOnly())
            .type(configuration.getType())
            .build();

        messageEmailReplyToService.duplicate(configuration.getReplyTo(), newMessageConfiguration);

        message.setConfiguration(newMessageConfiguration);
    }

    @Override
    public void propogateContainerChanges(MessageContainer container) {
        // update all messages with modified container configurations
        List<Message> messagesToUpdate = container.getMessages().stream().toList();
        container.getMessages().clear();
        container.getMessages().addAll(
            messagesToUpdate.stream()
                .map(
                    message -> {
                        if (message.getType() == null || container.getConfiguration().getModifiedFields().get(MessageContainerUpdatedFields.TYPE)) {
                            message.getConfiguration().setType(container.getConfiguration().getType());
                        }

                        if (message.getSendAt() == null || container.getConfiguration().getModifiedFields().get(MessageContainerUpdatedFields.SEND_AT) ||
                                container.getConfiguration().getModifiedFields().get(MessageContainerUpdatedFields.SEND_AT_TIMEZONE_OFFSET)) {
                            message.getConfiguration().setSendAt(container.getConfiguration().getSendAt());
                            message.getConfiguration().setSendAtTimezoneOffset(container.getConfiguration().getSendAtTimezoneOffset());
                        }

                        if (container.getConfiguration().getModifiedFields().get(MessageContainerUpdatedFields.TO_CONSENTED_ONLY)) {
                            message.getConfiguration().setToConsentedOnly(container.getConfiguration().isToConsentedOnly());
                        }

                        if (container.getConfiguration().getModifiedFields().get(MessageContainerUpdatedFields.REPLY_TO)) {
                            message.getReplyTo().clear();
                            messageEmailReplyToService.duplicate(
                                container.getReplyTo(),
                                message.getConfiguration()
                            );
                        }
                        return message;
                    }
                )
                .collect(Collectors.toList()) // need modifiable list
        );
    }

    @Override
    public MessageConfigurationDto toDto(MessageConfiguration configuration, UUID messageUuid) {
        if (configuration == null) {
            return null;
        }

        // verify message status
        MessageStatus status = calculateStatus(configuration.getMessage());

        if (status != configuration.getStatus()) {
            // if the status has changed, update it
            configuration.setStatus(status);
            configuration = configurationRepository.save(configuration);
        }

        return MessageConfigurationDto.builder()
            .enabled(configuration.isEnabled())
            .id(configuration.getUuid())
            .matchType(configuration.getRecipientMatchType())
            .messageId(messageUuid)
            .replyTo(
                messageEmailReplyToService.toDto(
                    configuration.getReplyTo()
                )
            )
            .sendAt(configuration.getSendAt())
            .sendAtTimezoneOffset(configuration.getSendAtTimezoneOffset())
            .status(configuration.getStatus())
            .subject(configuration.getSubject())
            .toConsentedOnly(configuration.isToConsentedOnly())
            .type(configuration.getType())
            .build();
    }

    @Override
    public MessageConfiguration fromDto(MessageConfigurationDto configurationDto, MessageConfiguration configuration) {
        configuration.setEnabled(configurationDto.isEnabled());
        configuration.setRecipientMatchType(configurationDto.getMatchType());
        configuration.setSendAt(configurationDto.getSendAt());
        configuration.setSendAtTimezoneOffset(configurationDto.getSendAtTimezoneOffset());
        configuration.setSubject(configurationDto.getSubject());
        configuration.setToConsentedOnly(configurationDto.isToConsentedOnly());
        configuration.setType(configurationDto.getType());

        return configuration;
    }

    @Override
    public MessageStatus calculateStatus(Message message) {
        if (message.getStatus() == MessageStatus.SENT || message.getStatus() == MessageStatus.DELETED) {
            // message has already been sent or deleted; no need to calculate status
            return message.getStatus();
        }

        if (!message.isEnabled()) {
            // message is not enabled; skip calculations for container readiness
            return MessageStatus.DISABLED;
        }

        if (message.getConfiguration().getSendAt() == null) {
            // message is not ready to send; sendAt is not set
            return MessageStatus.INCOMPLETE;
        }

        if (message.getConfiguration().getType() == null) {
            return MessageStatus.INCOMPLETE;
        }

        if (StringUtils.isAnyBlank(
                message.getContent().getHtml(),
                message.getConfiguration().getSubject()
            )
        ) {
            return MessageStatus.INCOMPLETE;
        }

        if (message.getType() == MessageType.EMAIL && !message.getReplyTo().stream().allMatch(replyTo -> StringUtils.isNotBlank(replyTo.getEmail()))) {
            return MessageStatus.INCOMPLETE;
        }

        return MessageStatus.READY;
    }

}
