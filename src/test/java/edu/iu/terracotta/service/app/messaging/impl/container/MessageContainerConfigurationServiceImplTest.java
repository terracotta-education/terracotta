package edu.iu.terracotta.service.app.messaging.impl.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainerConfiguration;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.replyto.MessageEmailReplyTo;
import edu.iu.terracotta.dao.model.dto.messaging.container.MessageContainerConfigurationDto;
import edu.iu.terracotta.dao.model.dto.messaging.email.MessageEmailReplyToDto;
import edu.iu.terracotta.dao.model.enums.messaging.MessageContainerUpdatedFields;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;
import edu.iu.terracotta.dao.model.enums.messaging.MessageType;
import edu.iu.terracotta.dao.repository.messaging.container.MessageContainerConfigurationRepository;
import edu.iu.terracotta.service.app.messaging.MessageConfigurationService;
import edu.iu.terracotta.service.app.messaging.MessageEmailReplyToService;

public class MessageContainerConfigurationServiceImplTest extends BaseTest {

    @Mock private MessageContainerConfigurationRepository containerConfigurationRepository;
    @Mock private MessageEmailReplyToService messageEmailReplyToService;
    @Mock private MessageConfigurationService configurationService;

    @InjectMocks private MessageContainerConfigurationServiceImpl containerConfigurationService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    public void testCreate() {
        MessageContainer container = MessageContainer.builder().exposure(exposure).build();

        containerConfigurationService.create(container, ltiUserEntity);

        assertEquals(container, container.getConfiguration().getContainer());
        assertEquals(MessageStatus.UNPUBLISHED, container.getConfiguration().getStatus());
        assertFalse(container.getConfiguration().isToConsentedOnly());
        assertEquals(1, container.getConfiguration().getContainerOrder());
        verify(messageEmailReplyToService).create(container.getConfiguration());
    }

    @Test
    public void testUpdateIncompleteStaysUnpublished() {
        MessageContainer container = MessageContainer.builder().exposure(exposure).messages(new ArrayList<>()).build();
        MessageContainerConfiguration configuration = MessageContainerConfiguration.builder()
            .container(container)
            .status(MessageStatus.UNPUBLISHED)
            .replyTo(new ArrayList<>())
            .build();
        MessageContainerConfigurationDto dto = MessageContainerConfigurationDto.builder()
            .replyTo(List.of())
            .title("Title")
            .toConsentedOnly(true)
            .build();

        containerConfigurationService.update(dto, configuration);

        assertEquals(MessageStatus.UNPUBLISHED, configuration.getStatus());
        assertEquals("Title", configuration.getTitle());
        assertTrue(configuration.isToConsentedOnly());
        verify(messageEmailReplyToService).update(dto, configuration);
    }

    @Test
    public void testUpdateReadyStatusIsPreserved() {
        Message message = mock(Message.class);
        MessageContainer container = MessageContainer.builder().exposure(exposure).messages(List.of(message)).build();
        MessageContainerConfiguration configuration = MessageContainerConfiguration.builder()
            .container(container)
            .status(MessageStatus.QUEUED)
            .replyTo(new ArrayList<>(List.of(mock(MessageEmailReplyTo.class))))
            .build();
        MessageContainerConfigurationDto dto = MessageContainerConfigurationDto.builder()
            .replyTo(List.of(MessageEmailReplyToDto.builder().email("a@b.com").build()))
            .sendAt(Timestamp.from(Instant.now()))
            .sendAtTimezoneOffset(60)
            .status(MessageStatus.QUEUED)
            .title("Ready")
            .type(MessageType.EMAIL)
            .build();

        when(configurationService.calculateStatus(message)).thenReturn(MessageStatus.READY);

        containerConfigurationService.update(dto, configuration);

        assertEquals(MessageStatus.QUEUED, configuration.getStatus());
        verify(messageEmailReplyToService).update(dto, configuration);
    }

    @Test
    public void testUpdatePreservesSentStatusRegardlessOfFields() {
        MessageContainer container = MessageContainer.builder().exposure(exposure).messages(new ArrayList<>()).build();
        MessageContainerConfiguration configuration = MessageContainerConfiguration.builder()
            .container(container)
            .status(MessageStatus.SENT)
            .replyTo(new ArrayList<>())
            .build();
        MessageContainerConfigurationDto dto = MessageContainerConfigurationDto.builder()
            .replyTo(List.of())
            .status(MessageStatus.SENT)
            .build();

        containerConfigurationService.update(dto, configuration);

        assertEquals(MessageStatus.SENT, configuration.getStatus());
    }

    @Test
    public void testDuplicate() {
        MessageEmailReplyTo replyTo = mock(MessageEmailReplyTo.class);
        MessageContainerConfiguration source = MessageContainerConfiguration.builder()
            .sendAt(Timestamp.from(Instant.now()))
            .sendAtTimezoneOffset(30)
            .status(MessageStatus.PUBLISHED)
            .title("Original")
            .toConsentedOnly(true)
            .type(MessageType.EMAIL)
            .replyTo(List.of(replyTo))
            .build();
        MessageContainer newContainer = MessageContainer.builder().exposure(exposure).owner(ltiUserEntity).build();

        containerConfigurationService.duplicate(source, newContainer);

        MessageContainerConfiguration newConfiguration = newContainer.getConfiguration();
        assertEquals(newContainer, newConfiguration.getContainer());
        assertEquals("Copy of Original", newConfiguration.getTitle());
        assertEquals(MessageStatus.UNPUBLISHED, newConfiguration.getStatus());
        assertEquals(source.getSendAt(), newConfiguration.getSendAt());
        assertEquals(source.getSendAtTimezoneOffset(), newConfiguration.getSendAtTimezoneOffset());
        assertTrue(newConfiguration.isToConsentedOnly());
        assertEquals(MessageType.EMAIL, newConfiguration.getType());
        verify(messageEmailReplyToService).duplicate(eq(source.getReplyTo()), any(MessageContainerConfiguration.class));
    }

    @Test
    public void testToDtoNullReturnsNull() {
        assertNull(containerConfigurationService.toDto(null));
    }

    @Test
    public void testToDtoSavesWhenStatusChanges() {
        MessageContainer container = MessageContainer.builder().exposure(exposure).build();
        container.setUuid(UUID.randomUUID());
        MessageContainerConfiguration configuration = MessageContainerConfiguration.builder()
            .container(container)
            .status(MessageStatus.QUEUED)
            .replyTo(new ArrayList<>())
            .build();
        configuration.setUuid(UUID.randomUUID());

        when(containerConfigurationRepository.save(configuration)).thenReturn(configuration);
        when(messageEmailReplyToService.toDto(anyList())).thenReturn(List.of());

        MessageContainerConfigurationDto dto = containerConfigurationService.toDto(configuration);

        assertEquals(MessageStatus.UNPUBLISHED, dto.getStatus());
        verify(containerConfigurationRepository).save(configuration);
    }

    @Test
    public void testToDtoDoesNotSaveWhenStatusUnchanged() {
        MessageContainer container = MessageContainer.builder().exposure(exposure).build();
        container.setUuid(UUID.randomUUID());
        MessageContainerConfiguration configuration = MessageContainerConfiguration.builder()
            .container(container)
            .status(MessageStatus.UNPUBLISHED)
            .replyTo(new ArrayList<>())
            .build();
        configuration.setUuid(UUID.randomUUID());

        when(messageEmailReplyToService.toDto(anyList())).thenReturn(List.of());

        MessageContainerConfigurationDto dto = containerConfigurationService.toDto(configuration);

        assertEquals(MessageStatus.UNPUBLISHED, dto.getStatus());
        verify(containerConfigurationRepository, never()).save(any(MessageContainerConfiguration.class));
    }

    @Test
    public void testFromDtoNullConfigurationCreatesNew() {
        MessageContainerConfigurationDto dto = MessageContainerConfigurationDto.builder()
            .order(3)
            .replyTo(List.of())
            .status(MessageStatus.UNPUBLISHED)
            .title("New Title")
            .toConsentedOnly(true)
            .type(MessageType.CONVERSATION)
            .build();

        MessageContainerConfiguration result = containerConfigurationService.fromDto(dto, null);

        assertEquals(3, result.getContainerOrder());
        assertEquals("New Title", result.getTitle());
        assertTrue(result.isToConsentedOnly());
        assertEquals(MessageType.CONVERSATION, result.getType());
        assertEquals(MessageStatus.UNPUBLISHED, result.getStatus());
    }

    @Test
    public void testFromDtoNullDtoReturnsConfigurationUnchanged() {
        MessageContainerConfiguration configuration = MessageContainerConfiguration.builder()
            .title("Unchanged")
            .build();

        MessageContainerConfiguration result = containerConfigurationService.fromDto(null, configuration);

        assertEquals(configuration, result);
        assertEquals("Unchanged", result.getTitle());
    }

    @Test
    public void testFromDtoMarksAllModifiedFieldsWhenChanged() {
        MessageContainerConfiguration configuration = MessageContainerConfiguration.builder()
            .sendAt(null)
            .sendAtTimezoneOffset(0)
            .toConsentedOnly(false)
            .type(MessageType.NONE)
            .replyTo(new ArrayList<>())
            .build();
        MessageContainerConfigurationDto dto = MessageContainerConfigurationDto.builder()
            .replyTo(List.of(MessageEmailReplyToDto.builder().build()))
            .sendAt(Timestamp.from(Instant.now()))
            .sendAtTimezoneOffset(120)
            .status(MessageStatus.UNPUBLISHED)
            .toConsentedOnly(true)
            .type(MessageType.EMAIL)
            .build();

        containerConfigurationService.fromDto(dto, configuration);

        assertTrue(configuration.getModifiedFields().get(MessageContainerUpdatedFields.TYPE));
        assertTrue(configuration.getModifiedFields().get(MessageContainerUpdatedFields.SEND_AT));
        assertTrue(configuration.getModifiedFields().get(MessageContainerUpdatedFields.SEND_AT_TIMEZONE_OFFSET));
        assertTrue(configuration.getModifiedFields().get(MessageContainerUpdatedFields.TO_CONSENTED_ONLY));
        assertTrue(configuration.getModifiedFields().get(MessageContainerUpdatedFields.REPLY_TO));
    }

    @Test
    public void testFromDtoNoModifiedFieldsWhenUnchanged() {
        Timestamp sendAt = Timestamp.from(Instant.now());
        MessageEmailReplyTo existingReplyTo = mock(MessageEmailReplyTo.class);
        MessageContainerConfiguration configuration = MessageContainerConfiguration.builder()
            .sendAt(sendAt)
            .sendAtTimezoneOffset(60)
            .toConsentedOnly(true)
            .type(MessageType.EMAIL)
            .replyTo(new ArrayList<>(List.of(existingReplyTo)))
            .build();
        MessageContainerConfigurationDto dto = MessageContainerConfigurationDto.builder()
            .replyTo(List.of(MessageEmailReplyToDto.builder().id(UUID.randomUUID()).build()))
            .sendAt(sendAt)
            .sendAtTimezoneOffset(60)
            .status(MessageStatus.UNPUBLISHED)
            .toConsentedOnly(true)
            .type(MessageType.EMAIL)
            .build();

        containerConfigurationService.fromDto(dto, configuration);

        assertFalse(configuration.getModifiedFields().get(MessageContainerUpdatedFields.TYPE));
        assertFalse(configuration.getModifiedFields().get(MessageContainerUpdatedFields.SEND_AT));
        assertFalse(configuration.getModifiedFields().get(MessageContainerUpdatedFields.SEND_AT_TIMEZONE_OFFSET));
        assertFalse(configuration.getModifiedFields().get(MessageContainerUpdatedFields.TO_CONSENTED_ONLY));
        assertFalse(configuration.getModifiedFields().get(MessageContainerUpdatedFields.REPLY_TO));
    }

}
