package edu.iu.terracotta.runner.messaging.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainerConfiguration;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.message.MessageConfiguration;
import edu.iu.terracotta.dao.model.enums.FeatureType;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;
import edu.iu.terracotta.dao.model.enums.messaging.MessageType;
import edu.iu.terracotta.dao.repository.messaging.container.MessageContainerRepository;
import edu.iu.terracotta.dao.repository.messaging.message.MessageRepository;
import edu.iu.terracotta.exceptions.messaging.MessageSendConversationException;
import edu.iu.terracotta.exceptions.messaging.MessageSendEmailException;
import edu.iu.terracotta.runner.messaging.model.MessagingScheduleResult;
import edu.iu.terracotta.service.app.FeatureService;
import edu.iu.terracotta.service.app.messaging.MessageConversationService;
import edu.iu.terracotta.service.app.messaging.MessageEmailService;

public class MessagingSchedulerServiceImplTest extends BaseTest {

    private static final long KEY_ID = 42L;

    @Mock private MessageContainerRepository messageContainerRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private FeatureService featureService;
    @Mock private MessageConversationService messageConversationService;
    @Mock private MessageEmailService messageEmailService;

    private MessagingSchedulerServiceImpl messagingSchedulerService;
    private AtomicLong nextId;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        messagingSchedulerService = new MessagingSchedulerServiceImpl(
            messageContainerRepository,
            messageRepository,
            featureService,
            messageConversationService,
            messageEmailService
        );

        nextId = new AtomicLong(1L);

        when(messageRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageContainerRepository.save(any(MessageContainer.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private MessageContainer buildContainer(MessageStatus status) {
        return MessageContainer.builder()
            .configuration(MessageContainerConfiguration.builder().status(status).build())
            .build();
    }

    /**
     * Builds a real Message/MessageConfiguration graph (so status mutations made by production code
     * are visible through the entity's own transient getters), spying only getPlatformDeployment()
     * since exercising the real exposureGroupCondition -> condition -> experiment chain isn't needed here.
     */
    private Message buildMessage(MessageContainer container, MessageStatus status, MessageType type, Timestamp sendAt, int offsetMinutes, long keyId) {
        MessageConfiguration configuration = MessageConfiguration.builder()
            .status(status)
            .type(type)
            .sendAt(sendAt)
            .sendAtTimezoneOffset(offsetMinutes)
            .build();

        Message message = Message.builder()
            .configuration(configuration)
            .container(container)
            .build();
        message.setId(nextId.getAndIncrement());

        Message spyMessage = spy(message);
        doReturn(PlatformDeployment.builder().keyId(keyId).build()).when(spyMessage).getPlatformDeployment();

        container.getMessages().add(spyMessage);

        return spyMessage;
    }

    @Test
    public void testSendReturnsEmptyWhenFeatureDisabled() {
        MessageContainer container = buildContainer(MessageStatus.PUBLISHED);
        Message message = buildMessage(container, MessageStatus.READY, MessageType.CONVERSATION, Timestamp.from(Instant.now().minusSeconds(600)), 0, KEY_ID);

        when(messageRepository.findAllByContainer_Configuration_StatusAndConfiguration_Status(MessageStatus.PUBLISHED, MessageStatus.READY))
            .thenReturn(List.of(message));
        when(featureService.isFeatureEnabled(FeatureType.MESSAGING, KEY_ID)).thenReturn(false);

        Optional<MessagingScheduleResult> result = messagingSchedulerService.send();

        assertTrue(result.isEmpty());
        verify(messageRepository, never()).saveAll(anyList());
        verify(messageContainerRepository, never()).save(any());
        verifyNoInteractions(messageConversationService, messageEmailService);
    }

    @Test
    public void testSendReturnsEmptyWhenOutsideTimeWindow() {
        MessageContainer container = buildContainer(MessageStatus.PUBLISHED);
        // sendAt in the future; not yet due to be sent
        Message message = buildMessage(container, MessageStatus.READY, MessageType.CONVERSATION, Timestamp.from(Instant.now().plusSeconds(600)), 0, KEY_ID);

        when(messageRepository.findAllByContainer_Configuration_StatusAndConfiguration_Status(MessageStatus.PUBLISHED, MessageStatus.READY))
            .thenReturn(List.of(message));
        when(featureService.isFeatureEnabled(FeatureType.MESSAGING, KEY_ID)).thenReturn(true);

        Optional<MessagingScheduleResult> result = messagingSchedulerService.send();

        assertTrue(result.isEmpty());
        verify(messageRepository, never()).saveAll(anyList());
        verify(messageContainerRepository, never()).save(any());
    }

    @Test
    public void testSendIncludesMessageWhenAdjustedSendAtExactlyEqualsNow() throws Exception {
        Instant fixedNow = Instant.parse("2026-01-01T00:00:00Z");

        MessageContainer container = buildContainer(MessageStatus.PUBLISHED);
        Message message = buildMessage(container, MessageStatus.READY, MessageType.CONVERSATION, Timestamp.from(fixedNow), 0, KEY_ID);

        when(messageRepository.findAllByContainer_Configuration_StatusAndConfiguration_Status(MessageStatus.PUBLISHED, MessageStatus.READY))
            .thenReturn(List.of(message));
        when(featureService.isFeatureEnabled(FeatureType.MESSAGING, KEY_ID)).thenReturn(true);

        Optional<MessagingScheduleResult> result;

        try (MockedStatic<Instant> instantMock = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            instantMock.when(Instant::now).thenReturn(fixedNow);

            result = messagingSchedulerService.send();
        }

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getProcessed().size());
        assertEquals(MessageStatus.SENT, message.getConfiguration().getStatus());
        verify(messageConversationService, times(1)).send(message);
    }

    @Test
    public void testSendHappyPathConversationMarksContainerSent() throws Exception {
        MessageContainer container = buildContainer(MessageStatus.PUBLISHED);
        Message message = buildMessage(container, MessageStatus.READY, MessageType.CONVERSATION, Timestamp.from(Instant.now().minusSeconds(600)), 0, KEY_ID);

        when(messageRepository.findAllByContainer_Configuration_StatusAndConfiguration_Status(MessageStatus.PUBLISHED, MessageStatus.READY))
            .thenReturn(List.of(message));
        when(featureService.isFeatureEnabled(FeatureType.MESSAGING, KEY_ID)).thenReturn(true);

        Optional<MessagingScheduleResult> result = messagingSchedulerService.send();

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getProcessed().size());
        assertTrue(result.get().getProcessed().get(0).getErrors() == null || result.get().getProcessed().get(0).getErrors().isEmpty());
        assertEquals(MessageStatus.SENT, message.getConfiguration().getStatus());
        assertEquals(MessageStatus.SENT, container.getConfiguration().getStatus());
        verify(messageConversationService, times(1)).send(message);
        verifyNoInteractions(messageEmailService);
        verify(messageContainerRepository, times(1)).save(container);
    }

    @Test
    public void testSendHappyPathEmailMarksContainerSent() throws Exception {
        MessageContainer container = buildContainer(MessageStatus.PUBLISHED);
        Message message = buildMessage(container, MessageStatus.READY, MessageType.EMAIL, Timestamp.from(Instant.now().minusSeconds(600)), 0, KEY_ID);

        when(messageRepository.findAllByContainer_Configuration_StatusAndConfiguration_Status(MessageStatus.PUBLISHED, MessageStatus.READY))
            .thenReturn(List.of(message));
        when(featureService.isFeatureEnabled(FeatureType.MESSAGING, KEY_ID)).thenReturn(true);

        Optional<MessagingScheduleResult> result = messagingSchedulerService.send();

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getProcessed().size());
        assertEquals(MessageStatus.SENT, message.getConfiguration().getStatus());
        assertEquals(MessageStatus.SENT, container.getConfiguration().getStatus());
        verify(messageEmailService, times(1)).send(message);
        verifyNoInteractions(messageConversationService);
        verify(messageContainerRepository, times(1)).save(container);
    }

    @Test
    public void testSendConversationThrowsSetsErrorStatusAndErrorContainer() throws Exception {
        MessageContainer container = buildContainer(MessageStatus.PUBLISHED);
        Message message = buildMessage(container, MessageStatus.READY, MessageType.CONVERSATION, Timestamp.from(Instant.now().minusSeconds(600)), 0, KEY_ID);

        when(messageRepository.findAllByContainer_Configuration_StatusAndConfiguration_Status(MessageStatus.PUBLISHED, MessageStatus.READY))
            .thenReturn(List.of(message));
        when(featureService.isFeatureEnabled(FeatureType.MESSAGING, KEY_ID)).thenReturn(true);
        doThrow(new MessageSendConversationException("conversation boom", null)).when(messageConversationService).send(message);

        Optional<MessagingScheduleResult> result = messagingSchedulerService.send();

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getProcessed().size());
        assertEquals(1, result.get().getProcessed().get(0).getErrors().size());
        assertEquals("conversation boom", result.get().getProcessed().get(0).getErrors().get(0));
        assertEquals(MessageStatus.ERROR, message.getConfiguration().getStatus());
        assertEquals(MessageStatus.ERROR, container.getConfiguration().getStatus());
        verify(messageContainerRepository, times(1)).save(container);
    }

    @Test
    public void testSendEmailThrowsSetsErrorStatusAndErrorContainer() throws Exception {
        MessageContainer container = buildContainer(MessageStatus.PUBLISHED);
        Message message = buildMessage(container, MessageStatus.READY, MessageType.EMAIL, Timestamp.from(Instant.now().minusSeconds(600)), 0, KEY_ID);

        when(messageRepository.findAllByContainer_Configuration_StatusAndConfiguration_Status(MessageStatus.PUBLISHED, MessageStatus.READY))
            .thenReturn(List.of(message));
        when(featureService.isFeatureEnabled(FeatureType.MESSAGING, KEY_ID)).thenReturn(true);
        doThrow(new MessageSendEmailException("email boom", null)).when(messageEmailService).send(message);

        Optional<MessagingScheduleResult> result = messagingSchedulerService.send();

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getProcessed().get(0).getErrors().size());
        assertEquals("email boom", result.get().getProcessed().get(0).getErrors().get(0));
        assertEquals(MessageStatus.ERROR, message.getConfiguration().getStatus());
        assertEquals(MessageStatus.ERROR, container.getConfiguration().getStatus());
    }

    @Test
    public void testSendUnknownMessageTypeSetsErrorStatus() {
        MessageContainer container = buildContainer(MessageStatus.PUBLISHED);
        // MessageType.NONE is the default configuration type and falls through to the switch's default branch
        Message message = buildMessage(container, MessageStatus.READY, MessageType.NONE, Timestamp.from(Instant.now().minusSeconds(600)), 0, KEY_ID);

        when(messageRepository.findAllByContainer_Configuration_StatusAndConfiguration_Status(MessageStatus.PUBLISHED, MessageStatus.READY))
            .thenReturn(List.of(message));
        when(featureService.isFeatureEnabled(FeatureType.MESSAGING, KEY_ID)).thenReturn(true);

        Optional<MessagingScheduleResult> result = messagingSchedulerService.send();

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getProcessed().get(0).getErrors().size());
        assertTrue(result.get().getProcessed().get(0).getErrors().get(0).contains("Invalid message type"));
        assertEquals(MessageStatus.ERROR, message.getConfiguration().getStatus());
        assertEquals(MessageStatus.ERROR, container.getConfiguration().getStatus());
        verifyNoInteractions(messageConversationService, messageEmailService);
    }

    @Test
    public void testSendFeatureDisabledOnRecheckExcludesMessageAndUnpublishesContainer() {
        MessageContainer container = buildContainer(MessageStatus.PUBLISHED);
        Message message = buildMessage(container, MessageStatus.READY, MessageType.CONVERSATION, Timestamp.from(Instant.now().minusSeconds(600)), 0, KEY_ID);

        when(messageRepository.findAllByContainer_Configuration_StatusAndConfiguration_Status(MessageStatus.PUBLISHED, MessageStatus.READY))
            .thenReturn(List.of(message));
        // first call (pre-filter) enabled, second call (defense-in-depth recheck) disabled
        when(featureService.isFeatureEnabled(eq(FeatureType.MESSAGING), eq(KEY_ID))).thenReturn(true, false);

        Optional<MessagingScheduleResult> result = messagingSchedulerService.send();

        assertTrue(result.isPresent());
        assertTrue(result.get().getProcessed().isEmpty());
        assertEquals(MessageStatus.UNPUBLISHED, container.getConfiguration().getStatus());
        verify(messageContainerRepository, times(1)).save(container);
        verifyNoInteractions(messageConversationService, messageEmailService);
    }

    @Test
    public void testProcessContainerStatusesMixedStatusDoesNotSaveContainer() {
        MessageContainer container = buildContainer(MessageStatus.PUBLISHED);
        Message sentMessage = buildMessage(container, MessageStatus.READY, MessageType.CONVERSATION, Timestamp.from(Instant.now().minusSeconds(600)), 0, KEY_ID);
        // second message in the same container is not due yet (never returned by the repository query);
        // it stays attached to the container with a non-terminal status
        buildMessage(container, MessageStatus.PROCESSING, MessageType.CONVERSATION, Timestamp.from(Instant.now().plusSeconds(600)), 0, KEY_ID);

        when(messageRepository.findAllByContainer_Configuration_StatusAndConfiguration_Status(MessageStatus.PUBLISHED, MessageStatus.READY))
            .thenReturn(List.of(sentMessage));
        when(featureService.isFeatureEnabled(FeatureType.MESSAGING, KEY_ID)).thenReturn(true);

        Optional<MessagingScheduleResult> result = messagingSchedulerService.send();

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getProcessed().size());
        assertEquals(MessageStatus.SENT, sentMessage.getConfiguration().getStatus());
        assertEquals(MessageStatus.PUBLISHED, container.getConfiguration().getStatus());
        assertFalse(container.getMessages().stream().allMatch(m -> m.getStatus() == MessageStatus.SENT));
        verify(messageContainerRepository, never()).save(any());
    }

}
