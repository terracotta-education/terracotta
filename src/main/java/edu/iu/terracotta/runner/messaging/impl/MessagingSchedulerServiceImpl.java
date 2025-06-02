package edu.iu.terracotta.runner.messaging.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.model.enums.FeatureType;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;
import edu.iu.terracotta.dao.repository.messaging.container.MessageContainerRepository;
import edu.iu.terracotta.dao.repository.messaging.message.MessageRepository;
import edu.iu.terracotta.runner.messaging.MessagingSchedulerService;
import edu.iu.terracotta.runner.messaging.configuration.model.MessagingScheduleMessage;
import edu.iu.terracotta.runner.messaging.configuration.model.MessagingScheduleResult;
import edu.iu.terracotta.service.app.FeatureService;
import edu.iu.terracotta.service.app.messaging.MessageConversationService;
import edu.iu.terracotta.service.app.messaging.MessageEmailService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class MessagingSchedulerServiceImpl implements MessagingSchedulerService {

    @Autowired private MessageContainerRepository messageContainerRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private FeatureService featureService;
    @Autowired private MessageConversationService messageConversationService;
    @Autowired private MessageEmailService messageEmailService;

    @Override
    @Transactional
    public Optional<MessagingScheduleResult> send() {
        List<Message> messagesMarkedReadyToSend = messageRepository.findAllByContainer_Configuration_StatusAndConfiguration_Status(
            MessageStatus.PUBLISHED,
            MessageStatus.READY
        );

        // set each message status to "processing" if feature is enabled
        messagesMarkedReadyToSend = CollectionUtils.emptyIfNull(messagesMarkedReadyToSend).stream()
            .filter(messageReadyToSend -> featureService.isFeatureEnabled(FeatureType.MESSAGING, messageReadyToSend.getPlatformDeployment().getKeyId()))
            .filter(
                messageMarkedReadyToSend -> {
                    // get the current time for the given offset
                    Instant now = Instant.now().minus(messageMarkedReadyToSend.getSendAtTimestampOffset(), ChronoUnit.MINUTES);

                    // adjust UTC sendAt time by timezone offset to get correct time for comparison
                    Instant adjustedSendAt = messageMarkedReadyToSend.getConfiguration()
                        .getSendAt()
                        .toInstant()
                        .minus(messageMarkedReadyToSend.getSendAtTimestampOffset(), ChronoUnit.MINUTES);

                    return adjustedSendAt.isBefore(now) || adjustedSendAt.equals(now);
                }
            )
            .map(
                messageReadyToSend -> {
                    messageReadyToSend.getConfiguration().setStatus(MessageStatus.PROCESSING);

                    return messageReadyToSend;
                }
            )
            .toList();

        if (CollectionUtils.isEmpty(messagesMarkedReadyToSend)) {
            return Optional.empty();
        }

        messagesMarkedReadyToSend = messageRepository.saveAll(messagesMarkedReadyToSend);

        List<MessagingScheduleMessage> processed = messagesMarkedReadyToSend.stream()
            .map(
                messageReadyToSend -> {
                    log.info("Messaging scheduler processing [{}] message ID: [{}]", messageReadyToSend.getConfiguration().getType(), messageReadyToSend.getId());

                    if (!featureService.isFeatureEnabled(FeatureType.MESSAGING, messageReadyToSend.getPlatformDeployment().getKeyId())) {
                        // messaging feature is not enabled for this organization; mark container as unpublished and do not process message
                        log.warn("Messaging feature is not enabled for platform deployment key ID: [{}]; message ID: [{}] will not be sent",
                            messageReadyToSend.getPlatformDeployment().getKeyId(),
                            messageReadyToSend.getId()
                        );
                        messageReadyToSend.getContainer().getConfiguration().setStatus(MessageStatus.UNPUBLISHED);
                        messageContainerRepository.save(messageReadyToSend.getContainer());

                        return null;
                    }

                    MessagingScheduleMessage processedMessage = MessagingScheduleMessage.builder()
                        .beginAt(Timestamp.valueOf(LocalDateTime.now()))
                        .messageId(messageReadyToSend.getId())
                        .sendAt(messageReadyToSend.getConfiguration().getSendAt())
                        .build();

                    switch (messageReadyToSend.getConfiguration().getType()) {
                        case CONVERSATION:
                            try {
                                messageConversationService.send(messageReadyToSend);
                                messageReadyToSend.getConfiguration().setStatus(MessageStatus.SENT);
                            } catch (Exception e) {
                                processedMessage.addError(e.getMessage());
                                messageReadyToSend.getConfiguration().setStatus(MessageStatus.ERROR);
                                log.error("Messaging scheduler processing conversation message ID: [{}] encountered an error", messageReadyToSend.getId(), e);
                            }

                            break;
                        case EMAIL:
                            try {
                                messageEmailService.send(messageReadyToSend);
                                messageReadyToSend.getConfiguration().setStatus(MessageStatus.SENT);
                            } catch (Exception e) {
                                processedMessage.addError(e.getMessage());
                                messageReadyToSend.getConfiguration().setStatus(MessageStatus.ERROR);
                                log.error("Messaging scheduler processing email message ID: [{}] encountered an error", messageReadyToSend.getId(), e);
                            }

                            break;
                        default:
                            String error = String.format("Messaging scheduler processing email message ID: [%s] encountered an error: Invalid message type: [%s]",
                                    messageReadyToSend.getId(),
                                    messageReadyToSend.getConfiguration().getType());
                            log.error(error);

                            processedMessage.addError(error);
                            messageReadyToSend.getConfiguration().setStatus(MessageStatus.ERROR);
                    }

                    processedMessage.setFinishedAt(Timestamp.valueOf(LocalDateTime.now()));
                    log.info("Messaging scheduler processing message ID: [{}] complete.", messageReadyToSend.getId());

                    return processedMessage;
                }
            )
            .filter(Objects::nonNull)
            .toList();

        messagesMarkedReadyToSend = messageRepository.saveAll(messagesMarkedReadyToSend);

        processContainerStatuses(
            messagesMarkedReadyToSend.stream()
                .filter(message -> MessageStatus.PUBLISHED == message.getContainer().getConfiguration().getStatus())
                .map(Message::getContainer)
                .collect(Collectors.toSet())
        );

        return Optional.of(MessagingScheduleResult.builder().processed(processed).build());
    }

    private void processContainerStatuses(Set<MessageContainer> messageContainers) {
        messageContainers.forEach(
            messageContainer -> {
                boolean isAllSent = messageContainer.getMessages().stream()
                    .allMatch(message -> message.getStatus() == MessageStatus.SENT);

                if (isAllSent) {
                    // all messages sent successfully; update group to SENT status
                    messageContainer.getConfiguration().setStatus(MessageStatus.SENT);
                    messageContainerRepository.save(messageContainer);

                    return;
                }

                boolean isAnyError = messageContainer.getMessages().stream()
                    .anyMatch(message -> message.getStatus() == MessageStatus.ERROR);

                if (isAnyError) {
                    // a messages send error occurred; update group to ERROR status
                    messageContainer.getConfiguration().setStatus(MessageStatus.ERROR);
                    messageContainerRepository.save(messageContainer);
                }
            }
        );
    }

}
