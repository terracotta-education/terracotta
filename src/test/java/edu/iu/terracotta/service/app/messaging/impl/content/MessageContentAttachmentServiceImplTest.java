package edu.iu.terracotta.service.app.messaging.impl.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsFile;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.messaging.attachment.MessageContentAttachment;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.model.dto.messaging.content.MessageContentAttachmentDto;
import edu.iu.terracotta.dao.repository.messaging.content.MessageContentAttachmentRepository;

public class MessageContentAttachmentServiceImplTest extends BaseTest {

    @Mock private MessageContentAttachmentRepository contentAttachmentRepository;

    private MessageContentAttachmentServiceImpl contentAttachmentService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        // apiClient's mock candidates collide by type (see BaseServiceTest pitfall note), so this
        // class is constructed manually instead of relying on @InjectMocks.
        contentAttachmentService = new MessageContentAttachmentServiceImpl(contentAttachmentRepository, apiClient);
    }

    private MessageContent buildContent(Message message) {
        MessageContent content = MessageContent.builder().message(message).build();
        content.setId(1L);

        return content;
    }

    @Test
    public void testGetMergesExistingAndNewLmsAttachments() throws Exception {
        Message message = mock(Message.class);
        when(message.getOwner()).thenReturn(ltiUserEntity);
        MessageContent content = buildContent(message);

        MessageContentAttachment existing = MessageContentAttachment.builder().lmsId("existing-1").build();
        when(contentAttachmentRepository.findAllByContent_Id(1L)).thenReturn(new ArrayList<>(List.of(existing)));

        LmsFile newFile = LmsFile.builder().id("new-1").displayName("New").filename("new.txt").size(10).url("http://new").build();
        LmsFile duplicateFile = LmsFile.builder().id("existing-1").displayName("Existing").filename("existing.txt").size(5).url("http://existing").build();
        when(apiClient.getFiles(ltiUserEntity)).thenReturn(List.of(newFile, duplicateFile));

        List<MessageContentAttachmentDto> result = contentAttachmentService.get(content);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> "existing-1".equals(dto.getLmsId())));
        assertTrue(result.stream().anyMatch(dto -> "new-1".equals(dto.getLmsId())));
    }

    @Test
    public void testGetSwallowsApiException() throws Exception {
        Message message = mock(Message.class);
        when(message.getOwner()).thenReturn(ltiUserEntity);
        MessageContent content = buildContent(message);

        MessageContentAttachment existing = MessageContentAttachment.builder().lmsId("existing-1").build();
        when(contentAttachmentRepository.findAllByContent_Id(1L)).thenReturn(new ArrayList<>(List.of(existing)));
        when(apiClient.getFiles(ltiUserEntity)).thenThrow(new ApiException("lms down"));

        List<MessageContentAttachmentDto> result = contentAttachmentService.get(content);

        assertEquals(1, result.size());
        assertEquals("existing-1", result.get(0).getLmsId());
    }

    @Test
    public void testGetSwallowsTerracottaConnectorException() throws Exception {
        Message message = mock(Message.class);
        when(message.getOwner()).thenReturn(ltiUserEntity);
        MessageContent content = buildContent(message);

        when(contentAttachmentRepository.findAllByContent_Id(1L)).thenReturn(new ArrayList<>());
        when(apiClient.getFiles(ltiUserEntity)).thenThrow(new TerracottaConnectorException("connector error"));

        List<MessageContentAttachmentDto> result = contentAttachmentService.get(content);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testUpdateReplacesAttachments() {
        MessageContent content = MessageContent.builder().attachments(new ArrayList<>(List.of(MessageContentAttachment.builder().build()))).build();
        MessageContentAttachmentDto dto = MessageContentAttachmentDto.builder().displayName("Attachment").filename("a.txt").lmsId("lms-1").size(1).url("http://a").build();

        contentAttachmentService.update(List.of(dto), content);

        assertEquals(1, content.getAttachments().size());
        assertEquals("Attachment", content.getAttachments().get(0).getDisplayName());
        assertEquals(content, content.getAttachments().get(0).getContent());
    }

    @Test
    public void testUpdateWithEmptyListClearsAttachments() {
        MessageContent content = MessageContent.builder().attachments(new ArrayList<>(List.of(MessageContentAttachment.builder().build()))).build();

        contentAttachmentService.update(List.of(), content);

        assertTrue(content.getAttachments().isEmpty());
    }

    @Test
    public void testDuplicateCopiesAttachments() {
        MessageContentAttachment source = MessageContentAttachment.builder()
            .displayName("Original")
            .filename("original.txt")
            .lmsId("lms-orig")
            .size(42)
            .url("http://original")
            .build();
        MessageContent newContent = MessageContent.builder().build();

        contentAttachmentService.duplicate(List.of(source), newContent);

        assertEquals(1, newContent.getAttachments().size());
        MessageContentAttachment copy = newContent.getAttachments().get(0);
        assertEquals("Original", copy.getDisplayName());
        assertEquals("original.txt", copy.getFilename());
        assertEquals("lms-orig", copy.getLmsId());
        assertEquals(42, copy.getSize());
        assertEquals("http://original", copy.getUrl());
        assertEquals(newContent, copy.getContent());
    }

    @Test
    public void testDuplicateWithEmptyListIsNoOp() {
        MessageContent newContent = MessageContent.builder().build();

        contentAttachmentService.duplicate(List.of(), newContent);

        assertTrue(newContent.getAttachments().isEmpty());
    }

    @Test
    public void testToDtoListNullReturnsEmpty() {
        assertTrue(contentAttachmentService.toDto((List<MessageContentAttachment>) null).isEmpty());
    }

    @Test
    public void testToDtoListMapsAllAttachments() {
        MessageContentAttachment attachment1 = MessageContentAttachment.builder().lmsId("1").build();
        MessageContentAttachment attachment2 = MessageContentAttachment.builder().lmsId("2").build();

        List<MessageContentAttachmentDto> result = contentAttachmentService.toDto(List.of(attachment1, attachment2));

        assertEquals(2, result.size());
        assertTrue(result.stream().map(MessageContentAttachmentDto::getLmsId).sorted(Comparator.naturalOrder()).toList().equals(List.of("1", "2")));
    }

    @Test
    public void testToDtoSingleMapsFields() {
        MessageContentAttachment attachment = MessageContentAttachment.builder()
            .displayName("Display")
            .filename("file.txt")
            .lmsId("lms-1")
            .size(99)
            .url("http://file")
            .build();
        UUID uuid = UUID.randomUUID();
        attachment.setUuid(uuid);

        MessageContentAttachmentDto dto = contentAttachmentService.toDto(attachment);

        assertEquals(uuid, dto.getId());
        assertEquals("Display", dto.getDisplayName());
        assertEquals("file.txt", dto.getFilename());
        assertEquals("lms-1", dto.getLmsId());
        assertEquals(99, dto.getSize());
        assertEquals("http://file", dto.getUrl());
    }

    @Test
    public void testFromDtoSingleNullReturnsEmptyOptional() {
        assertFalse(contentAttachmentService.fromDto((MessageContentAttachmentDto) null, MessageContent.builder().build()).isPresent());
    }

    @Test
    public void testFromDtoSingleMapsFields() {
        MessageContent content = MessageContent.builder().build();
        MessageContentAttachmentDto dto = MessageContentAttachmentDto.builder()
            .displayName("Display")
            .filename("file.txt")
            .lmsId("lms-1")
            .size(99)
            .url("http://file")
            .build();

        Optional<MessageContentAttachment> result = contentAttachmentService.fromDto(dto, content);

        assertTrue(result.isPresent());
        assertEquals("Display", result.get().getDisplayName());
        assertEquals(content, result.get().getContent());
    }

    @Test
    public void testFromDtoListNullReturnsEmpty() {
        assertTrue(contentAttachmentService.fromDto((List<MessageContentAttachmentDto>) null, MessageContent.builder().build()).isEmpty());
    }

    @Test
    public void testFromDtoListFiltersNullEntries() {
        MessageContent content = MessageContent.builder().build();
        MessageContentAttachmentDto dto = MessageContentAttachmentDto.builder().lmsId("lms-1").build();
        List<MessageContentAttachmentDto> dtos = new ArrayList<>();
        dtos.add(dto);
        dtos.add(null);

        List<MessageContentAttachment> result = contentAttachmentService.fromDto(dtos, content);

        assertEquals(1, result.size());
        assertEquals("lms-1", result.get(0).getLmsId());
    }

}
