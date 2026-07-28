package edu.iu.terracotta.service.app.messaging.impl.message;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainerConfiguration;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.message.MessageConfiguration;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedText;
import edu.iu.terracotta.dao.model.dto.messaging.content.MessageContentDto;
import edu.iu.terracotta.dao.model.dto.messaging.message.MessageConfigurationDto;
import edu.iu.terracotta.dao.model.dto.messaging.message.MessageDto;
import edu.iu.terracotta.dao.model.dto.messaging.recipient.MessageRecipientRuleSetDto;
import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleAssignmentDto;
import edu.iu.terracotta.dao.model.enums.messaging.MessageStatus;
import edu.iu.terracotta.exceptions.messaging.MessageBodyParseException;
import edu.iu.terracotta.exceptions.messaging.MessagePipedTextFileUploadException;
import edu.iu.terracotta.exceptions.messaging.MessagePipedTextValidationException;
import edu.iu.terracotta.dao.repository.messaging.container.MessageContainerRepository;
import edu.iu.terracotta.dao.repository.messaging.message.MessageRepository;
import edu.iu.terracotta.service.app.messaging.MessageConfigurationService;
import edu.iu.terracotta.service.app.messaging.MessageContentService;
import edu.iu.terracotta.service.app.messaging.MessageEmailReplyToService;
import edu.iu.terracotta.service.app.messaging.MessagePipedTextService;
import edu.iu.terracotta.service.app.messaging.MessageRecipientRuleSetService;
import edu.iu.terracotta.service.app.messaging.MessageRuleAssignmentService;

@SuppressWarnings("unchecked")
public class MessageServiceImplTest extends BaseTest {

    @Mock private MessageContainerRepository containerRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private MessageConfigurationService configurationService;
    @Mock private MessageContentService contentService;
    @Mock private MessageEmailReplyToService messageEmailReplyToService;
    @Mock private MessageRuleAssignmentService messageRuleAssignmentService;
    @Mock private MessageRecipientRuleSetService messageRuleSetService;
    @Mock private MessagePipedTextService pipedTextService;

    @InjectMocks private MessageServiceImpl messageService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    private ExposureGroupCondition egcWithDefault(long id, boolean isDefault) {
        Condition condition = Condition.builder().conditionId(id).defaultCondition(isDefault).build();

        return ExposureGroupCondition.builder().exposureGroupConditionId(id).condition(condition).build();
    }

    private Message realMessage(long egcId) {
        MessageConfiguration configuration = MessageConfiguration.builder()
            .status(MessageStatus.INCOMPLETE)
            .replyTo(new ArrayList<>())
            .build();

        MessageContent content = MessageContent.builder().build();

        return Message.builder()
            .exposureGroupCondition(egcWithDefault(egcId, true))
            .configuration(configuration)
            .content(content)
            .build();
    }

    private MessageContainer realContainer(MessageStatus status) {
        MessageContainerConfiguration containerConfiguration = MessageContainerConfiguration.builder()
            .status(status)
            .build();

        return MessageContainer.builder()
            .configuration(containerConfiguration)
            .owner(ltiUserEntity)
            .messages(new ArrayList<>())
            .build();
    }

    @Test
    public void testCreateSingleVersionUsesDefaultConditionOnly() {
        List<ExposureGroupCondition> egcs = List.of(egcWithDefault(1L, true), egcWithDefault(2L, false));
        when(exposureGroupConditionRepository.findByExposure_ExposureId(anyLong())).thenReturn(egcs);
        MessageContainer container = realContainer(MessageStatus.UNPUBLISHED);

        messageService.create(container, 1L, true);

        assertEquals(1, container.getMessages().size());
        assertEquals(1L, container.getMessages().get(0).getExposureGroupConditionId());
        verify(configurationService).create(any(Message.class));
        verify(messageEmailReplyToService).create((MessageConfiguration) null);
        verify(contentService).create(any(Message.class));
    }

    @Test
    public void testCreateMultiVersionUsesAllConditions() {
        List<ExposureGroupCondition> egcs = List.of(egcWithDefault(1L, true), egcWithDefault(2L, false));
        when(exposureGroupConditionRepository.findByExposure_ExposureId(anyLong())).thenReturn(egcs);
        MessageContainer container = realContainer(MessageStatus.UNPUBLISHED);

        messageService.create(container, 1L, false);

        assertEquals(2, container.getMessages().size());
        verify(configurationService, times(2)).create(any(Message.class));
        verify(contentService, times(2)).create(any(Message.class));
    }

    @Test
    public void testUpdateSameExposureGroupConditionSkipsLookup() {
        Message message = realMessage(1L);
        MessageContainer container = realContainer(MessageStatus.UNPUBLISHED);
        MessageDto messageDto = MessageDto.builder().exposureGroupConditionId(1L).build();
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        messageService.update(messageDto, 1L, container, message);

        verify(exposureGroupConditionRepository, never()).findById(anyLong());
    }

    @Test
    public void testUpdateDifferentExposureGroupConditionFoundUpdatesMessage() {
        Message message = realMessage(1L);
        MessageContainer container = realContainer(MessageStatus.UNPUBLISHED);
        MessageDto messageDto = MessageDto.builder().exposureGroupConditionId(2L).build();
        ExposureGroupCondition newEgc = egcWithDefault(2L, false);
        when(exposureGroupConditionRepository.findById(2L)).thenReturn(Optional.of(newEgc));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        messageService.update(messageDto, 1L, container, message);

        assertEquals(2L, message.getExposureGroupConditionId());
    }

    @Test
    public void testUpdateDifferentExposureGroupConditionNotFoundThrows() {
        Message message = realMessage(1L);
        MessageContainer container = realContainer(MessageStatus.UNPUBLISHED);
        MessageDto messageDto = MessageDto.builder().exposureGroupConditionId(99L).build();
        when(exposureGroupConditionRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> messageService.update(messageDto, 1L, container, message)
        );

        assertTrue(exception.getMessage().contains("99"));
    }

    @Test
    public void testUpdateUnpublishesContainerWhenMessageNotReady() {
        Message message = realMessage(1L);
        message.getConfiguration().setStatus(MessageStatus.INCOMPLETE);
        MessageContainer container = realContainer(MessageStatus.PUBLISHED);
        MessageDto messageDto = MessageDto.builder().exposureGroupConditionId(1L).build();
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        messageService.update(messageDto, 1L, container, message);

        assertEquals(MessageStatus.UNPUBLISHED, container.getConfiguration().getStatus());
        verify(containerRepository).save(container);
    }

    @Test
    public void testUpdateDoesNotUnpublishWhenMessageReady() {
        Message message = realMessage(1L);
        message.getConfiguration().setStatus(MessageStatus.READY);
        MessageContainer container = realContainer(MessageStatus.PUBLISHED);
        MessageDto messageDto = MessageDto.builder().exposureGroupConditionId(1L).build();
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        messageService.update(messageDto, 1L, container, message);

        assertEquals(MessageStatus.PUBLISHED, container.getConfiguration().getStatus());
        verify(containerRepository, never()).save(any(MessageContainer.class));
    }

    @Test
    public void testUpdateSavesMessageAndUpdatesPlaceholders() throws MessageBodyParseException {
        Message message = realMessage(1L);
        MessageContainer container = realContainer(MessageStatus.UNPUBLISHED);
        MessageDto messageDto = MessageDto.builder().exposureGroupConditionId(1L).build();
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        messageService.update(messageDto, 1L, container, message);

        verify(messageRepository).save(message);
        verify(contentService).updatePlaceholders(message.getContent(), true);
        verify(contentService).update(messageDto.getContent(), message);
        verify(configurationService).update(messageDto.getConfiguration(), message);
        verify(messageRuleSetService).update(messageDto.getRuleSets(), message);
    }

    @Test
    public void testPutReturnsDto() {
        Message message = realMessage(1L);
        MessageContainer container = realContainer(MessageStatus.UNPUBLISHED);
        container.setUuid(UUID.randomUUID());
        message.setContainer(container);
        MessageDto messageDto = MessageDto.builder().exposureGroupConditionId(1L).build();
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageDto result = messageService.put(messageDto, 1L, container, message);

        assertEquals(container.getUuid(), result.getContainerId());
        verify(messageRepository).save(message);
    }

    @Test
    public void testDuplicateListClearsAndRepopulatesContainer() throws MessageBodyParseException {
        MessageContainer container = realContainer(MessageStatus.UNPUBLISHED);
        container.getMessages().add(realMessage(1L));

        Message source1 = realMessage(1L);
        Message source2 = realMessage(2L);

        messageService.duplicate(List.of(source1, source2), container);

        assertEquals(2, container.getMessages().size());
        verify(configurationService, times(2)).duplicate(any(MessageConfiguration.class), any(Message.class));
        verify(contentService, times(2)).duplicate(any(MessageContent.class), any(Message.class));
        verify(messageRuleSetService, times(2)).duplicate(any(List.class), any(Message.class));
    }

    @Test
    public void testDuplicateListPropagatesContentException() throws MessageBodyParseException {
        MessageContainer container = realContainer(MessageStatus.UNPUBLISHED);
        Message source = realMessage(1L);
        doThrow(new MessageBodyParseException("bad body")).when(contentService).duplicate(any(MessageContent.class), any(Message.class));

        assertThrows(MessageBodyParseException.class, () -> messageService.duplicate(List.of(source), container));
    }

    @Test
    public void testDuplicateSingleAddsNewMessageToContainer() throws MessageBodyParseException {
        MessageContainer container = realContainer(MessageStatus.UNPUBLISHED);
        Message source = realMessage(1L);

        messageService.duplicate(source, container);

        assertEquals(1, container.getMessages().size());
        Message duplicated = container.getMessages().get(0);
        assertEquals(container, duplicated.getContainer());
        assertEquals(source.getExposureGroupCondition(), duplicated.getExposureGroupCondition());
        verify(configurationService).duplicate(source.getConfiguration(), duplicated);
        verify(contentService).duplicate(source.getContent(), duplicated);
        verify(messageRuleSetService).duplicate(source.getRuleSets(), duplicated);
    }

    @Test
    public void testDeleteMarksAllMessagesDeleted() {
        MessageContainer container = realContainer(MessageStatus.PUBLISHED);
        container.getMessages().add(realMessage(1L));
        container.getMessages().add(realMessage(2L));

        messageService.delete(container);

        assertEquals(2, container.getMessages().size());
        assertTrue(container.getMessages().stream().allMatch(message -> message.getStatus() == MessageStatus.DELETED));
    }

    @Test
    public void testToDtoListEmptyReturnsEmptyList() {
        assertTrue(messageService.toDto((List<Message>) null).isEmpty());
        assertTrue(messageService.toDto(List.<Message>of()).isEmpty());
    }

    @Test
    public void testToDtoListMapsEachMessage() {
        Message message1 = realMessage(1L);
        message1.setContainer(realContainer(MessageStatus.UNPUBLISHED));
        Message message2 = realMessage(2L);
        message2.setContainer(realContainer(MessageStatus.UNPUBLISHED));

        List<MessageDto> dtos = messageService.toDto(List.of(message1, message2));

        assertEquals(2, dtos.size());
    }

    @Test
    public void testToDtoSingleNullReturnsNull() {
        assertNull(messageService.toDto((Message) null));
    }

    @Test
    public void testToDtoSingleMapsFields() {
        Message message = realMessage(1L);
        MessageContainer container = realContainer(MessageStatus.UNPUBLISHED);
        container.setUuid(UUID.randomUUID());
        message.setContainer(container);
        message.setUuid(UUID.randomUUID());

        MessageConfigurationDto configurationDto = MessageConfigurationDto.builder().build();
        MessageContentDto contentDto = MessageContentDto.builder().build();
        List<MessageRecipientRuleSetDto> ruleSetDtos = List.of();

        when(configurationService.toDto(message.getConfiguration(), message.getUuid())).thenReturn(configurationDto);
        when(contentService.toDto(message.getContent())).thenReturn(contentDto);
        when(messageRuleSetService.toDto(message.getRuleSets())).thenReturn(ruleSetDtos);
        when(ltiUserEntity.getEmail()).thenReturn("owner@example.com");

        MessageDto dto = messageService.toDto(message);

        assertEquals(message.getUuid(), dto.getId());
        assertEquals(container.getUuid(), dto.getContainerId());
        assertEquals(configurationDto, dto.getConfiguration());
        assertEquals(contentDto, dto.getContent());
        assertEquals(ruleSetDtos, dto.getRuleSets());
        assertEquals("owner@example.com", dto.getOwnerEmail());
        assertEquals(message.getConditionId(), dto.getConditionId());
        assertEquals(message.getExposureGroupConditionId(), dto.getExposureGroupConditionId());
    }

    @Test
    public void testGetAssignmentsReturnsMappedDtos() throws Exception {
        List<MessageRuleAssignmentDto> expected = List.of(MessageRuleAssignmentDto.builder().lmsId("1").build());
        when(messageRuleAssignmentService.toDto(List.of(lmsAssignment))).thenReturn(expected);

        List<MessageRuleAssignmentDto> result = messageService.getAssignments(securedInfo);

        assertEquals(expected, result);
    }

    @Test
    public void testGetAssignmentsPropagatesApiException() throws Exception {
        when(assignmentService.getAllAssignmentsForLmsCourse(securedInfo)).thenThrow(new ApiException("boom"));

        assertThrows(ApiException.class, () -> messageService.getAssignments(securedInfo));
    }

    @Test
    public void testUpdatePlaceholdersCallsContentService() throws MessageBodyParseException {
        Message message = realMessage(1L);

        messageService.updatePlaceholders(message, true);

        verify(contentService).updatePlaceholders(message.getContent(), true);
    }

    @Test
    public void testUpdatePlaceholdersSwallowsException() throws MessageBodyParseException {
        Message message = realMessage(1L);
        doThrow(new MessageBodyParseException("bad")).when(contentService).updatePlaceholders(any(MessageContent.class), anyBoolean());

        assertDoesNotThrow(() -> messageService.updatePlaceholders(message, false));
    }

    @Test
    public void testProcessPipedTextCsvFileSuccess() throws Exception {
        Message message = realMessage(1L);
        MessageContainer container = realContainer(MessageStatus.UNPUBLISHED);
        message.setContainer(container);
        MessagePipedText pipedText = MessagePipedText.builder().build();
        when(pipedTextService.processPipedTextCsvFile(message, multipartFile)).thenReturn(pipedText);

        MessageDto dto = messageService.processPipedTextCsvFile(message, multipartFile);

        assertNull(dto.getValidationErrors());
        assertEquals(pipedText, message.getContent().getPipedText());
        verify(contentService).updatePlaceholders(message.getContent(), false);
    }

    @Test
    public void testProcessPipedTextCsvFileValidationErrorReturnsErrorsWithoutUpdatingPlaceholders() throws Exception {
        Message message = realMessage(1L);
        doThrow(new MessagePipedTextValidationException(List.of("error one", "error two")))
            .when(pipedTextService).validatePipedTextFile(message, multipartFile);

        MessageDto dto = messageService.processPipedTextCsvFile(message, multipartFile);

        assertEquals(List.of("error one", "error two"), dto.getValidationErrors());
        verify(contentService, never()).updatePlaceholders(any(MessageContent.class), anyBoolean());
    }

    @Test
    public void testProcessPipedTextCsvFileUploadErrorReturnsErrors() throws Exception {
        Message message = realMessage(1L);
        doThrow(new MessagePipedTextFileUploadException("upload failed"))
            .when(pipedTextService).validatePipedTextFile(message, multipartFile);

        MessageDto dto = messageService.processPipedTextCsvFile(message, multipartFile);

        assertEquals(List.of("upload failed"), dto.getValidationErrors());
    }

}
