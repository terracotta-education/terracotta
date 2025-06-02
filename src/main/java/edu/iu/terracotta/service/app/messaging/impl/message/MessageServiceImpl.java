package edu.iu.terracotta.service.app.messaging.impl.message;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.model.dto.messaging.message.MessageDto;
import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleAssignmentDto;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;
import edu.iu.terracotta.dao.repository.ExposureGroupConditionRepository;
import edu.iu.terracotta.dao.repository.messaging.container.MessageContainerRepository;
import edu.iu.terracotta.dao.repository.messaging.message.MessageRepository;
import edu.iu.terracotta.exceptions.messaging.MessageBodyParseException;
import edu.iu.terracotta.exceptions.messaging.MessagePipedTextFileUploadException;
import edu.iu.terracotta.exceptions.messaging.MessagePipedTextValidationException;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.messaging.MessageConfigurationService;
import edu.iu.terracotta.service.app.messaging.MessageContentService;
import edu.iu.terracotta.service.app.messaging.MessageEmailReplyToService;
import edu.iu.terracotta.service.app.messaging.MessageRuleAssignmentService;
import edu.iu.terracotta.service.app.messaging.MessageRecipientRuleSetService;
import edu.iu.terracotta.service.app.messaging.MessageService;
import edu.iu.terracotta.service.app.messaging.MessagePipedTextService;
import io.jsonwebtoken.lang.Collections;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement", "PMD.LambdaCanBeMethodReference"})
public class MessageServiceImpl implements MessageService {

    @Autowired private ExposureGroupConditionRepository exposureGroupConditionRepository;
    @Autowired private MessageContainerRepository containerRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private AssignmentService assignmentService;
    @Autowired private MessageConfigurationService configurationService;
    @Autowired private MessageContentService contentService;
    @Autowired private MessageEmailReplyToService messageEmailReplyToService;
    @Autowired private MessageRuleAssignmentService messageRuleAssignmentService;
    @Autowired private MessageRecipientRuleSetService messageRuleSetService;
    @Autowired private MessagePipedTextService pipedTextService;

    @Override
    public void create(MessageContainer container, long exposureId, boolean single) {
        List<ExposureGroupCondition> exposureGroupConditions = exposureGroupConditionRepository.findByExposure_ExposureId(exposureId);

        // create messages
        List<Message> messages;

        if (single) {
            // single-version message; use egc with default condition
            messages = exposureGroupConditions.stream()
                .filter(exposureGroupCondition -> BooleanUtils.isTrue(exposureGroupCondition.getCondition().getDefaultCondition()))
                .map(exposureGroupCondition ->
                    Message.builder()
                        .container(container)
                        .exposureGroupCondition(exposureGroupCondition)
                        .build()
                )
                .toList();
        } else {
            // multiple-version message; create for each egc
            messages = exposureGroupConditions.stream()
                .map(exposureGroupCondition ->
                    Message.builder()
                        .container(container)
                        .exposureGroupCondition(exposureGroupCondition)
                        .build()
                )
                .toList();
        }

        container.setMessages(
            messages.stream()
                .map(
                    message -> {
                        // create message configurations
                        configurationService.create(message);

                        // create the default reply-to address for the message configuration
                        messageEmailReplyToService.create(message.getConfiguration());

                        // create message content
                        contentService.create(message);

                        return message;
                    }
                )
                .toList()
        );
    }

    @Override
    public void update(MessageDto messageDto, long exposureId, MessageContainer container, Message message) {
        if (message.getExposureGroupConditionId() != messageDto.getExposureGroupConditionId()) {
            ExposureGroupCondition exposureGroupCondition = exposureGroupConditionRepository.findById(messageDto.getExposureGroupConditionId())
                .orElseThrow(() -> new IllegalArgumentException(String.format("No exposure group condition with ID: [%s] found.", messageDto.getExposureGroupConditionId())));
            message.setExposureGroupCondition(exposureGroupCondition);
        }

        contentService.update(messageDto.getContent(), message);
        configurationService.update(messageDto.getConfiguration(), message);
        messageRuleSetService.update(messageDto.getRuleSets(), message);

        if (MessageStatus.READY != message.getStatus() && MessageStatus.PUBLISHED == container.getStatus()) {
            // if the message is not ready, but the container is published, set the container status to unpublished
            container.getConfiguration().setStatus(MessageStatus.UNPUBLISHED);
            containerRepository.save(container);
        }

        message = messageRepository.save(message);
        updatePlaceholders(message, true);
    }

    @Override
    public MessageDto put(MessageDto messageDto, long exposureId, MessageContainer container, Message message) {
        update(messageDto, exposureId, container, message);

        return toDto(message);
    }

    @Override
    public void duplicate(List<Message> messages, MessageContainer container) throws MessageBodyParseException {
        container.getMessages().clear();

        for (Message message : messages)  {
            duplicate(message, container);
        }
    }

    @Override
    public void duplicate(Message message, MessageContainer container) throws MessageBodyParseException {
        Message newMessage = Message.builder()
                .container(container)
                .exposureGroupCondition(message.getExposureGroupCondition())
                .build();

        configurationService.duplicate(message.getConfiguration(), newMessage);
        contentService.duplicate(message.getContent(), newMessage);
        messageRuleSetService.duplicate(message.getRuleSets(), newMessage);

        container.getMessages().add(newMessage);
    }

     @Override
    public void delete(MessageContainer container) {
        List<Message> deletedMessages = container.getMessages().stream()
            .map(
                message -> {
                    // mark the message as deleted
                    message.getConfiguration().setStatus(MessageStatus.DELETED);

                    return message;
                }
            )
            .toList();

        container.getMessages().clear();
        container.getMessages().addAll(deletedMessages);
    }

    @Override
    public List<MessageDto> toDto(List<Message> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return Collections.emptyList();
        }

        return messages.stream()
            .map(message -> toDto(message))
            .toList();
    }

    @Override
    public MessageDto toDto(Message message) {
        if (message ==  null) {
            return null;
        }

        return MessageDto.builder()
            .conditionId(message.getConditionId())
            .configuration(
                configurationService.toDto(message.getConfiguration(), message.getUuid())
            )
            .containerId(message.getContainer().getUuid())
            .content(
                contentService.toDto(message.getContent())
            )
            .created(message.getCreatedAt())
            .exposureGroupConditionId(message.getExposureGroupConditionId())
            .id(message.getUuid())
            .ownerEmail(message.getOwner().getEmail())
            .ruleSets(
                messageRuleSetService.toDto(message.getRuleSets())
            )
            .build();
    }

    @Override
    public List<MessageRuleAssignmentDto> getAssignments(SecuredInfo securedInfo) throws ApiException, TerracottaConnectorException {
        return messageRuleAssignmentService.toDto(
            assignmentService.getAllAssignmentsForLmsCourse(securedInfo)
        );
    }

    @Override
    public void updatePlaceholders(Message message, boolean save) {
        try {
            contentService.updatePlaceholders(message.getContent(), save);
        } catch (Exception e) {
            log.error("Error updating placeholders for message with ID: [{}]", message.getId(), e);
        }
    }

    @Override
    public MessageDto processPipedTextCsvFile(Message message, MultipartFile file) {
        try {
            // validate the piped text file
            pipedTextService.validatePipedTextFile(message, file);

            // process the piped text CSV file and create PipedText entity
            message.getContent().setPipedText(pipedTextService.processPipedTextCsvFile(message, file));
        } catch (MessagePipedTextValidationException | MessagePipedTextFileUploadException e) {
            log.error("Error processing piped text file for message with ID: [{}].", message.getId(), e);
            return MessageDto.builder()
                .validationErrors(Arrays.asList(StringUtils.splitByWholeSeparator(e.getMessage(), "::")))
                .build();
        }

        // update the piped text placeholders in the content
        updatePlaceholders(message, false);

        return toDto(message);
    }

}
