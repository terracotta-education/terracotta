package edu.iu.terracotta.service.app.messaging.impl.container;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.model.dto.messaging.container.MessageContainerDto;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;
import edu.iu.terracotta.dao.repository.ExposureGroupConditionRepository;
import edu.iu.terracotta.dao.repository.messaging.container.MessageContainerRepository;
import edu.iu.terracotta.exceptions.messaging.MessageBodyParseException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerMoveException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerNotFoundException;
import edu.iu.terracotta.service.app.ComponentUtils;
import edu.iu.terracotta.service.app.messaging.MessageConfigurationService;
import edu.iu.terracotta.service.app.messaging.MessageContainerConfigurationService;
import edu.iu.terracotta.service.app.messaging.MessageContainerService;
import edu.iu.terracotta.service.app.messaging.MessageEmailReplyToService;
import edu.iu.terracotta.service.app.messaging.MessageService;
import io.jsonwebtoken.lang.Collections;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement", "PMD.LambdaCanBeMethodReference"})
public class MessageContainerServiceImpl implements MessageContainerService {

    @Autowired private ExposureGroupConditionRepository exposureGroupConditionRepository;
    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private MessageContainerRepository containerRepository;
    @Autowired private MessageConfigurationService configurationService;
    @Autowired private MessageContainerConfigurationService containerConfigurationService;
    @Autowired private MessageEmailReplyToService messageEmailReplyToService;
    @Autowired private MessageService messageService;
    @Autowired private ComponentUtils componentUtils;

    @Override
    public List<MessageContainerDto> getAll(long experimentId, long exposureId, SecuredInfo securedInfo) {
        return toDto(
            containerRepository.findAllByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndOwner_LmsUserIdAndConfiguration_StatusInOrderByConfiguration_ContainerOrderAsc(
                experimentId,
                exposureId,
                securedInfo.getLmsUserId(),
                List.of(
                    MessageStatus.PUBLISHED,
                    MessageStatus.UNPUBLISHED,
                    MessageStatus.ERROR,
                    MessageStatus.INCOMPLETE,
                    MessageStatus.PROCESSING,
                    MessageStatus.SENT,
                    MessageStatus.QUEUED
                )
            )
        );
    }

    @Override
    public MessageContainerDto create(Exposure exposure, boolean single, SecuredInfo securedInfo) {
        LtiUserEntity owner = ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());

        MessageContainer container = MessageContainer.builder()
            .exposure(exposure)
            .owner(owner)
            .build();

        containerConfigurationService.create(container, owner);
        messageService.create(container, exposure.getExposureId(), single);

        return toDto(containerRepository.save(container));
    }

    @Override
    public MessageContainerDto update(MessageContainerDto containerDto, MessageContainer container) {
        containerConfigurationService.update(containerDto.getConfiguration(), container.getConfiguration());
        configurationService.propogateContainerChanges(container);
        messageEmailReplyToService.update(containerDto.getConfiguration(), container.getConfiguration());

        return toDto(containerRepository.save(container));
    }

    @Override
    public List<MessageContainerDto> updateAll(List<MessageContainerDto> containerDtos, List<MessageContainer> containers) {
        return containerDtos.stream()
            .map(
                containerDto -> {
                    try {
                        return update(
                            containerDto,
                            containers.stream()
                                .filter(container -> container.getUuid().equals(containerDto.getId()))
                                .findFirst()
                                .orElseThrow(() -> new MessageContainerNotFoundException(String.format("No message container with UUID: [%s] found.", containerDto.getId())))
                        );
                    } catch (MessageContainerNotFoundException e) {
                        log.error(e.getMessage(), e);
                        return null;
                    }
                }
            )
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    public MessageContainerDto delete(MessageContainer container) {
        // mark the container as deleted
        container.getConfiguration().setStatus(MessageStatus.DELETED);
        messageService.delete(container);

        return toDto(containerRepository.save(container));
    }

    @Override
    public MessageContainerDto move(Exposure toExposure, MessageContainer container) {
        List<ExposureGroupCondition> newExposureGroupConditions = exposureGroupConditionRepository.findByExposure_ExposureId(toExposure.getExposureId());

        container.setExposure(toExposure);

        // update container order for new exposure
        container.getConfiguration().setContainerOrder(componentUtils.calculateNextOrder(toExposure.getExposureId(), container.getOwner()));

        container.setMessages(
            container.getMessages().stream()
                .map(
                    oldMessage -> {
                        try {
                            // set the message to the new exposureGroupCondition with the same condition as the old exposureGroupCondition
                            oldMessage.setExposureGroupCondition(
                                newExposureGroupConditions.stream()
                                    .filter(exposureGroupCondition -> exposureGroupCondition.getCondition().getConditionId().equals(oldMessage.getConditionId()))
                                    .findFirst()
                                    .orElseThrow(
                                        () -> new MessageContainerMoveException(
                                            String.format(
                                                "No exposureGroupCondition found with condition ID: [%s]",
                                                oldMessage.getConditionId()
                                            )
                                        )
                                )
                            );
                        } catch (MessageContainerMoveException e) {
                            log.error(e.getMessage(), e);
                            return null;
                        }

                        return oldMessage;
                    }
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toList()) // need modifiable list
        );

        return toDto(containerRepository.save(container));
    }

    @Override
    public MessageContainerDto duplicate(MessageContainer container, Exposure exposure) throws MessageBodyParseException {
        MessageContainer newMessageContainer = MessageContainer.builder()
            .exposure(exposure)
            .owner(container.getOwner())
            .build();

        containerConfigurationService.duplicate(container.getConfiguration(), newMessageContainer);
        messageService.duplicate(
            container.getMessages().stream().toList(),
            newMessageContainer
        );
        newMessageContainer = containerRepository.save(newMessageContainer);

        return toDto(newMessageContainer);
    }

    @Override
    public List<MessageContainerDto> toDto(List<MessageContainer> containers) {
        if (CollectionUtils.isEmpty(containers)) {
            return Collections.emptyList();
        }

        return containers.stream()
            .filter(container -> isDisplayable(container))
            .map(container -> toDto(container))
            .toList();
    }

    @Override
    public MessageContainerDto toDto(MessageContainer container) {
        if (container == null) {
            return null;
        }

        return MessageContainerDto.builder()
            .configuration(containerConfigurationService.toDto(container.getConfiguration()))
            .exposureId(container.getExposure().getExposureId())
            .id(container.getUuid())
            .messages(messageService.toDto(container.getMessages()))
            .myFilesUrl(
                String.format(
                    MessageContainerDto.MY_FILES_URL,
                    container.getOwner().getPlatformDeployment().getBaseUrl(),
                    container.getOwner().getLmsUserId()
                )
            )
            .ownerEmail(container.getOwner().getEmail())
            .ownerId(container.getOwner().getUserId())
            .build();
    }

    private boolean isDisplayable(MessageContainer container) {
        return MessageStatus.displayable().contains(container.getConfiguration().getStatus());
    }

}
