package edu.iu.terracotta.service.app.messaging.impl.container;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainerConfiguration;
import edu.iu.terracotta.dao.model.dto.messaging.container.MessageContainerConfigurationDto;
import edu.iu.terracotta.dao.model.enums.messaging.MessageContainerUpdatedFields;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;
import edu.iu.terracotta.dao.repository.messaging.container.MessageContainerConfigurationRepository;
import edu.iu.terracotta.service.app.messaging.MessageEmailReplyToService;
import io.micrometer.common.util.StringUtils;
import edu.iu.terracotta.service.app.ComponentUtils;
import edu.iu.terracotta.service.app.messaging.MessageConfigurationService;
import edu.iu.terracotta.service.app.messaging.MessageContainerConfigurationService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class MessageContainerConfigurationServiceImpl implements MessageContainerConfigurationService {

    @Autowired private MessageContainerConfigurationRepository containerConfigurationRepository;
    @Autowired private MessageEmailReplyToService messageEmailReplyToService;
    @Autowired private MessageConfigurationService configurationService;
    @Autowired private ComponentUtils componentUtils;

    @Override
    public void create(MessageContainer container, LtiUserEntity owner) {
        MessageContainerConfiguration containerConfiguration = MessageContainerConfiguration.builder()
                .container(container)
                .containerOrder(
                    componentUtils.calculateNextOrder(container.getExposureId(), owner)
                )
                .status(MessageStatus.UNPUBLISHED)
                .toConsentedOnly(false)
                .build();

        messageEmailReplyToService.create(containerConfiguration);
        container.setConfiguration(containerConfiguration);
    }

    @Override
    public void update(MessageContainerConfigurationDto containerConfigurationDto, MessageContainerConfiguration containerConfiguration) {
        fromDto(containerConfigurationDto, containerConfiguration);
        MessageStatus status = calculateStatus(containerConfiguration);
        containerConfiguration.setStatus(status);

        messageEmailReplyToService.update(containerConfigurationDto, containerConfiguration);
    }

    @Override
    public void duplicate(MessageContainerConfiguration containerConfiguration, MessageContainer container) {
        MessageContainerConfiguration newMessageContainerConfiguration = MessageContainerConfiguration.builder()
            .container(container)
            .containerOrder(
                componentUtils.calculateNextOrder(container.getExposureId(), container.getOwner())
            )
            .sendAt(containerConfiguration.getSendAt())
            .sendAtTimezoneOffset(containerConfiguration.getSendAtTimezoneOffset())
            .status(MessageStatus.UNPUBLISHED)
            .title(String.format("Copy of %s", containerConfiguration.getTitle()))
            .toConsentedOnly(containerConfiguration.isToConsentedOnly())
            .type(containerConfiguration.getType())
            .build();

        messageEmailReplyToService.duplicate(containerConfiguration.getReplyTo(), newMessageContainerConfiguration);
        container.setConfiguration(newMessageContainerConfiguration);
    }

    @Override
    public MessageContainerConfigurationDto toDto(MessageContainerConfiguration containerConfiguration) {
        if (containerConfiguration == null) {
            return null;
        }

        // verify container status
        MessageStatus status = calculateStatus(containerConfiguration);

        if (status != containerConfiguration.getStatus()) {
            // if the status has changed, update it. do not auto set to PUBLISHED
            containerConfiguration.setStatus(status);
            containerConfiguration = containerConfigurationRepository.save(containerConfiguration);
        }

        return MessageContainerConfigurationDto.builder()
            .containerId(containerConfiguration.getContainer().getUuid())
            .id(containerConfiguration.getUuid())
            .order(containerConfiguration.getContainerOrder())
            .replyTo(
                messageEmailReplyToService.toDto(containerConfiguration.getReplyTo())
            )
            .sendAt(containerConfiguration.getSendAt())
            .sendAtTimezoneOffset(containerConfiguration.getSendAtTimezoneOffset())
            .status(containerConfiguration.getStatus())
            .title(containerConfiguration.getTitle())
            .type(containerConfiguration.getType())
            .toConsentedOnly(containerConfiguration.isToConsentedOnly())
            .build();
    }

    @Override
    public MessageContainerConfiguration fromDto(MessageContainerConfigurationDto containerConfigurationDto, MessageContainerConfiguration containerConfiguration) {
        if (containerConfiguration == null) {
            containerConfiguration = MessageContainerConfiguration.builder().build();
        }

        if (containerConfigurationDto == null) {
            return containerConfiguration;
        }

        calculateUpdatedFields(containerConfigurationDto, containerConfiguration);

        containerConfiguration.setContainerOrder(containerConfigurationDto.getOrder());
        containerConfiguration.setSendAt(containerConfigurationDto.getSendAt());
        containerConfiguration.setSendAtTimezoneOffset(containerConfigurationDto.getSendAtTimezoneOffset());
        containerConfiguration.setStatus(containerConfigurationDto.getStatus());
        containerConfiguration.setTitle(containerConfigurationDto.getTitle());
        containerConfiguration.setToConsentedOnly(containerConfigurationDto.isToConsentedOnly());
        containerConfiguration.setType(containerConfigurationDto.getType());

        return containerConfiguration;
    }

    private void calculateUpdatedFields(MessageContainerConfigurationDto containerConfigurationDto, MessageContainerConfiguration containerConfiguration) {
        if (containerConfigurationDto.getType() != null && containerConfigurationDto.getType() != containerConfiguration.getType()) {
            containerConfiguration.getModifiedFields().put(MessageContainerUpdatedFields.TYPE, true);
        }

        if ((containerConfigurationDto.getSendAt() != null && containerConfiguration.getSendAt() == null) ||
                (containerConfigurationDto.getSendAt() != null && containerConfiguration.getSendAt() != null && containerConfigurationDto.getSendAt().getTime() != containerConfiguration.getSendAt().getTime())
        ) {
            containerConfiguration.getModifiedFields().put(MessageContainerUpdatedFields.SEND_AT, true);
        }

        if (containerConfigurationDto.getSendAtTimezoneOffset() != null && !containerConfigurationDto.getSendAtTimezoneOffset().equals(containerConfiguration.getSendAtTimezoneOffset())) {
            containerConfiguration.getModifiedFields().put(MessageContainerUpdatedFields.SEND_AT_TIMEZONE_OFFSET, true);
        }

        if (containerConfigurationDto.isToConsentedOnly() != containerConfiguration.isToConsentedOnly()) {
            containerConfiguration.getModifiedFields().put(MessageContainerUpdatedFields.TO_CONSENTED_ONLY, true);
        }

        if (containerConfigurationDto.getReplyTo().size() != containerConfiguration.getReplyTo().size() ||
                containerConfigurationDto.getReplyTo().stream().anyMatch(replyTo -> replyTo.getId() == null)
        ) {
            // if any replyTo has no id, then we are creating new replyTo addresses
            containerConfiguration.getModifiedFields().put(MessageContainerUpdatedFields.REPLY_TO, true);
        }
    }

    /**
     * Message is ready to send if all are true:
     * - sendAt is not null
     * - type is not null
     * - replyTo is not empty
     * - title is not empty
     *
     * @param containerConfiguration
     * @return
     */
    private MessageStatus calculateStatus(MessageContainerConfiguration containerConfiguration) {
        if (containerConfiguration.getStatus() == MessageStatus.SENT || containerConfiguration.getStatus() == MessageStatus.DELETED) {
            // message container has already been sent or deleted; no need to calculate status
            return containerConfiguration.getStatus();
        }

        if (containerConfiguration.getSendAt() == null) {
            return MessageStatus.UNPUBLISHED;
        }

        if (containerConfiguration.getType() == null) {
            return MessageStatus.UNPUBLISHED;
        }

        if (CollectionUtils.isEmpty(containerConfiguration.getReplyTo())) {
            return MessageStatus.UNPUBLISHED;
        }

        if (StringUtils.isEmpty(containerConfiguration.getTitle())) {
            return MessageStatus.UNPUBLISHED;
        }

        if (!containerConfiguration.getContainer().getMessages().stream().allMatch(message -> List.of(MessageStatus.READY, MessageStatus.SENT, MessageStatus.DISABLED).contains(configurationService.calculateStatus(message)))) {
            // if all messages in the container are not ready, disabled, or sent, then the container is not ready; mark as unpublished
            return MessageStatus.UNPUBLISHED;
        }

        return containerConfiguration.getStatus();
    }

}
