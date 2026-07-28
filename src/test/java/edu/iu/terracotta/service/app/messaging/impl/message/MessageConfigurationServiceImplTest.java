package edu.iu.terracotta.service.app.messaging.impl.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainerConfiguration;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.message.MessageConfiguration;
import edu.iu.terracotta.dao.entity.messaging.replyto.MessageEmailReplyTo;
import edu.iu.terracotta.dao.model.dto.messaging.message.MessageConfigurationDto;
import edu.iu.terracotta.dao.model.enums.messaging.MessageContainerUpdatedFields;
import edu.iu.terracotta.dao.model.enums.messaging.MessageRecipientMatchType;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;
import edu.iu.terracotta.dao.model.enums.messaging.MessageType;
import edu.iu.terracotta.dao.repository.messaging.message.MessageConfigurationRepository;
import edu.iu.terracotta.service.app.messaging.MessageEmailReplyToService;

@SuppressWarnings("unchecked")
public class MessageConfigurationServiceImplTest extends BaseTest {

    @Mock private MessageConfigurationRepository configurationRepository;
    @Mock private MessageEmailReplyToService messageEmailReplyToService;

    @InjectMocks private MessageConfigurationServiceImpl messageConfigurationService;

    @Mock private Message message;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    private Message readyMessage() {
        MessageContainerConfiguration containerConfiguration = MessageContainerConfiguration.builder()
            .sendAt(Timestamp.from(Instant.now()))
            .sendAtTimezoneOffset(60)
            .build();

        MessageContainer container = MessageContainer.builder()
            .configuration(containerConfiguration)
            .build();

        MessageConfiguration configuration = MessageConfiguration.builder()
            .status(MessageStatus.INCOMPLETE)
            .enabled(true)
            .type(MessageType.CONVERSATION)
            .sendAt(Timestamp.from(Instant.now()))
            .subject("subject")
            .replyTo(new ArrayList<>())
            .build();

        Message message = Message.builder()
            .container(container)
            .configuration(configuration)
            .content(edu.iu.terracotta.dao.entity.messaging.content.MessageContent.builder().html("<p>body</p>").build())
            .build();
        configuration.setMessage(message);

        return message;
    }

    @Test
    public void testCreate() {
        messageConfigurationService.create(message);

        ArgumentCaptor<MessageConfiguration> captor = ArgumentCaptor.forClass(MessageConfiguration.class);
        verify(message).setConfiguration(captor.capture());

        MessageConfiguration configuration = captor.getValue();
        assertEquals(MessageRecipientMatchType.INCLUDE, configuration.getRecipientMatchType());
        assertEquals(MessageStatus.INCOMPLETE, configuration.getStatus());
        assertFalse(configuration.isToConsentedOnly());
        assertEquals(message, configuration.getMessage());
    }

    @Test
    public void testUpdateInheritsSendAtFromContainerWhenDtoSendAtNull() {
        Message message = readyMessage();
        message.getConfiguration().setStatus(MessageStatus.INCOMPLETE);

        MessageConfigurationDto configurationDto = MessageConfigurationDto.builder()
            .enabled(true)
            .matchType(MessageRecipientMatchType.INCLUDE)
            .sendAt(null)
            .subject("new subject")
            .toConsentedOnly(true)
            .type(MessageType.EMAIL)
            .build();

        messageConfigurationService.update(configurationDto, message);

        assertEquals(message.getContainer().getSendAt(), message.getConfiguration().getSendAt());
        assertEquals(message.getContainer().getConfiguration().getSendAtTimezoneOffset(), message.getConfiguration().getSendAtTimezoneOffset());
        assertEquals("new subject", message.getConfiguration().getSubject());
        assertTrue(message.getConfiguration().isToConsentedOnly());
        verify(messageEmailReplyToService).update(configurationDto, message.getConfiguration());
    }

    @Test
    public void testUpdateKeepsDtoSendAtWhenNotNull() {
        Message message = readyMessage();
        Timestamp dtoSendAt = Timestamp.from(Instant.now().plusSeconds(3600));

        MessageConfigurationDto configurationDto = MessageConfigurationDto.builder()
            .enabled(true)
            .matchType(MessageRecipientMatchType.INCLUDE)
            .sendAt(dtoSendAt)
            .sendAtTimezoneOffset(120)
            .subject("subject")
            .type(MessageType.EMAIL)
            .build();

        messageConfigurationService.update(configurationDto, message);

        assertEquals(dtoSendAt, message.getConfiguration().getSendAt());
        assertEquals(120, message.getConfiguration().getSendAtTimezoneOffset());
    }

    @Test
    public void testUpdateCalculatesStatus() {
        Message message = readyMessage();

        MessageConfigurationDto configurationDto = MessageConfigurationDto.builder()
            .enabled(true)
            .matchType(MessageRecipientMatchType.INCLUDE)
            .sendAt(Timestamp.from(Instant.now()))
            .subject("subject")
            .type(MessageType.CONVERSATION)
            .build();

        messageConfigurationService.update(configurationDto, message);

        assertEquals(MessageStatus.READY, message.getConfiguration().getStatus());
    }

    @Test
    public void testDuplicate() {
        MessageEmailReplyTo replyTo = MessageEmailReplyTo.builder().email("reply@to.com").build();
        MessageConfiguration source = MessageConfiguration.builder()
            .enabled(false)
            .recipientMatchType(MessageRecipientMatchType.EXCLUDE)
            .subject("original subject")
            .toConsentedOnly(true)
            .type(MessageType.EMAIL)
            .replyTo(List.of(replyTo))
            .build();

        Message newMessage = Message.builder().build();

        messageConfigurationService.duplicate(source, newMessage);

        MessageConfiguration duplicated = newMessage.getConfiguration();
        assertEquals(newMessage, duplicated.getMessage());
        assertFalse(duplicated.isEnabled());
        assertEquals(MessageRecipientMatchType.EXCLUDE, duplicated.getRecipientMatchType());
        assertNull(duplicated.getSendAt());
        assertEquals("original subject", duplicated.getSubject());
        assertTrue(duplicated.isToConsentedOnly());
        assertEquals(MessageType.EMAIL, duplicated.getType());
        assertEquals(MessageStatus.INCOMPLETE, duplicated.getStatus());
        verify(messageEmailReplyToService).duplicate(List.of(replyTo), duplicated);
    }

    @Test
    public void testPropogateContainerChangesUpdatesModifiedFields() {
        MessageContainerConfiguration containerConfiguration = MessageContainerConfiguration.builder()
            .type(MessageType.EMAIL)
            .sendAt(Timestamp.from(Instant.now()))
            .sendAtTimezoneOffset(30)
            .toConsentedOnly(true)
            .build();
        containerConfiguration.getModifiedFields().put(MessageContainerUpdatedFields.TYPE, true);
        containerConfiguration.getModifiedFields().put(MessageContainerUpdatedFields.SEND_AT, true);
        containerConfiguration.getModifiedFields().put(MessageContainerUpdatedFields.TO_CONSENTED_ONLY, true);
        containerConfiguration.getModifiedFields().put(MessageContainerUpdatedFields.REPLY_TO, false);

        MessageContainer container = MessageContainer.builder()
            .configuration(containerConfiguration)
            .build();

        MessageConfiguration configuration = MessageConfiguration.builder()
            .type(null)
            .sendAt(null)
            .toConsentedOnly(false)
            .replyTo(new ArrayList<>())
            .build();

        Message message = Message.builder().container(container).configuration(configuration).build();
        container.setMessages(new ArrayList<>(List.of(message)));

        messageConfigurationService.propogateContainerChanges(container);

        assertEquals(1, container.getMessages().size());
        Message updated = container.getMessages().get(0);
        assertEquals(MessageType.EMAIL, updated.getConfiguration().getType());
        assertEquals(containerConfiguration.getSendAt(), updated.getConfiguration().getSendAt());
        assertEquals(30, updated.getConfiguration().getSendAtTimezoneOffset());
        assertTrue(updated.getConfiguration().isToConsentedOnly());
        verify(messageEmailReplyToService, never()).duplicate(any(List.class), any(MessageConfiguration.class));
    }

    @Test
    public void testPropogateContainerChangesReplyToModifiedDuplicatesReplyTo() {
        MessageContainerConfiguration containerConfiguration = MessageContainerConfiguration.builder()
            .type(MessageType.EMAIL)
            .build();
        containerConfiguration.getModifiedFields().put(MessageContainerUpdatedFields.REPLY_TO, true);

        MessageEmailReplyTo containerReplyTo = MessageEmailReplyTo.builder().email("container@reply.com").build();
        containerConfiguration.setReplyTo(List.of(containerReplyTo));

        MessageContainer container = MessageContainer.builder()
            .configuration(containerConfiguration)
            .build();

        MessageConfiguration configuration = MessageConfiguration.builder()
            .type(MessageType.EMAIL)
            .replyTo(new ArrayList<>(List.of(MessageEmailReplyTo.builder().email("old@reply.com").build())))
            .build();

        Message message = Message.builder().container(container).configuration(configuration).build();
        container.setMessages(new ArrayList<>(List.of(message)));

        messageConfigurationService.propogateContainerChanges(container);

        assertTrue(message.getReplyTo().isEmpty());
        verify(messageEmailReplyToService).duplicate(List.of(containerReplyTo), configuration);
    }

    @Test
    public void testToDtoNullConfiguration() {
        assertNull(messageConfigurationService.toDto(null, UUID.randomUUID()));
    }

    @Test
    public void testToDtoSavesWhenStatusChanged() {
        Message message = readyMessage();
        message.getConfiguration().setStatus(MessageStatus.INCOMPLETE);
        when(configurationRepository.save(any(MessageConfiguration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageConfigurationDto dto = messageConfigurationService.toDto(message.getConfiguration(), message.getUuid());

        assertEquals(MessageStatus.READY, dto.getStatus());
        verify(configurationRepository, times(1)).save(any(MessageConfiguration.class));
    }

    @Test
    public void testToDtoDoesNotSaveWhenStatusUnchanged() {
        Message message = readyMessage();
        message.getConfiguration().setStatus(MessageStatus.READY);

        MessageConfigurationDto dto = messageConfigurationService.toDto(message.getConfiguration(), message.getUuid());

        assertEquals(MessageStatus.READY, dto.getStatus());
        verify(configurationRepository, never()).save(any(MessageConfiguration.class));
    }

    @Test
    public void testFromDto() {
        MessageConfiguration configuration = MessageConfiguration.builder().build();
        Timestamp sendAt = Timestamp.from(Instant.now());

        MessageConfigurationDto dto = MessageConfigurationDto.builder()
            .enabled(true)
            .matchType(MessageRecipientMatchType.EXCLUDE)
            .sendAt(sendAt)
            .sendAtTimezoneOffset(45)
            .subject("subj")
            .toConsentedOnly(true)
            .type(MessageType.CONVERSATION)
            .build();

        MessageConfiguration result = messageConfigurationService.fromDto(dto, configuration);

        assertEquals(configuration, result);
        assertTrue(result.isEnabled());
        assertEquals(MessageRecipientMatchType.EXCLUDE, result.getRecipientMatchType());
        assertEquals(sendAt, result.getSendAt());
        assertEquals(45, result.getSendAtTimezoneOffset());
        assertEquals("subj", result.getSubject());
        assertTrue(result.isToConsentedOnly());
        assertEquals(MessageType.CONVERSATION, result.getType());
    }

    @Test
    public void testCalculateStatusAlreadySentIsUnchanged() {
        Message message = readyMessage();
        message.getConfiguration().setStatus(MessageStatus.SENT);

        assertEquals(MessageStatus.SENT, messageConfigurationService.calculateStatus(message));
    }

    @Test
    public void testCalculateStatusAlreadyDeletedIsUnchanged() {
        Message message = readyMessage();
        message.getConfiguration().setStatus(MessageStatus.DELETED);

        assertEquals(MessageStatus.DELETED, messageConfigurationService.calculateStatus(message));
    }

    @Test
    public void testCalculateStatusNotEnabledIsDisabled() {
        Message message = readyMessage();
        message.getConfiguration().setEnabled(false);

        assertEquals(MessageStatus.DISABLED, messageConfigurationService.calculateStatus(message));
    }

    @Test
    public void testCalculateStatusNoSendAtIsIncomplete() {
        Message message = readyMessage();
        message.getConfiguration().setSendAt(null);

        assertEquals(MessageStatus.INCOMPLETE, messageConfigurationService.calculateStatus(message));
    }

    @Test
    public void testCalculateStatusNoTypeIsIncomplete() {
        Message message = readyMessage();
        message.getConfiguration().setType(null);

        assertEquals(MessageStatus.INCOMPLETE, messageConfigurationService.calculateStatus(message));
    }

    @Test
    public void testCalculateStatusBlankSubjectIsIncomplete() {
        Message message = readyMessage();
        message.getConfiguration().setSubject(null);

        assertEquals(MessageStatus.INCOMPLETE, messageConfigurationService.calculateStatus(message));
    }

    @Test
    public void testCalculateStatusBlankBodyIsIncomplete() {
        Message message = readyMessage();
        message.getContent().setHtml(null);

        assertEquals(MessageStatus.INCOMPLETE, messageConfigurationService.calculateStatus(message));
    }

    @Test
    public void testCalculateStatusEmailWithBlankReplyToIsIncomplete() {
        Message message = readyMessage();
        message.getConfiguration().setType(MessageType.EMAIL);
        message.getConfiguration().setReplyTo(List.of(MessageEmailReplyTo.builder().email(" ").build()));

        assertEquals(MessageStatus.INCOMPLETE, messageConfigurationService.calculateStatus(message));
    }

    @Test
    public void testCalculateStatusEmailWithValidReplyToIsReady() {
        Message message = readyMessage();
        message.getConfiguration().setType(MessageType.EMAIL);
        message.getConfiguration().setReplyTo(List.of(MessageEmailReplyTo.builder().email("a@b.com").build()));

        assertEquals(MessageStatus.READY, messageConfigurationService.calculateStatus(message));
    }

    @Test
    public void testCalculateStatusAllCompleteIsReady() {
        Message message = readyMessage();

        assertEquals(MessageStatus.READY, messageConfigurationService.calculateStatus(message));
    }

}
