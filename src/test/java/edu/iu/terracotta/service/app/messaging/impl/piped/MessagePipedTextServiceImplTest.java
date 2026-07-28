package edu.iu.terracotta.service.app.messaging.impl.piped;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedText;
import edu.iu.terracotta.dao.entity.messaging.piped.MessagePipedTextItem;
import edu.iu.terracotta.dao.model.dto.messaging.piped.MessagePipedTextDto;
import edu.iu.terracotta.dao.model.dto.messaging.piped.MessagePipedTextItemDto;
import edu.iu.terracotta.dao.repository.messaging.piped.PipedTextRepository;
import edu.iu.terracotta.exceptions.messaging.MessagePipedTextFileUploadException;
import edu.iu.terracotta.exceptions.messaging.MessagePipedTextValidationException;
import edu.iu.terracotta.service.app.messaging.MessagePipedTextItemService;

@SuppressWarnings("unchecked")
public class MessagePipedTextServiceImplTest extends BaseTest {

    @Mock private PipedTextRepository pipedTextRepository;
    @Mock private MessagePipedTextItemService pipedTextItemService;

    @InjectMocks private MessagePipedTextServiceImpl pipedTextService;

    private LtiUserEntity owner;
    private Message message;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        owner = mock(LtiUserEntity.class);
        when(owner.getLmsUserId()).thenReturn("owner-lms-id");

        message = mock(Message.class);
        when(message.getId()).thenReturn(1L);
    }

    private MessageContent buildContent() {
        MessageContainer container = MessageContainer.builder().owner(owner).build();
        Message message = Message.builder().container(container).build();
        MessageContent content = MessageContent.builder().message(message).build();
        content.setUuid(UUID.randomUUID());

        return content;
    }

    private Message mockCsvMessage(MessageContent content) {
        Message message = mock(Message.class);
        when(message.getContent()).thenReturn(content);
        when(message.getId()).thenReturn(1L);
        when(message.getPlatformDeployment()).thenReturn(platformDeployment);

        return message;
    }

    private MultipartFile csvFile(String fileName, String contentType, String content) {
        return new MockMultipartFile("file", fileName, contentType, content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testCreateSuccess() {
        MessageContent content = buildContent();
        MessagePipedTextDto dto = MessagePipedTextDto.builder()
            .id(UUID.randomUUID())
            .fileName("file.csv")
            .items(List.of())
            .build();

        pipedTextService.create(dto, content);

        assertNotNull(content.getPipedText());
        assertEquals("file.csv", content.getPipedText().getFileName());
        assertEquals(content, content.getPipedText().getContent());
        verify(pipedTextItemService).create(dto.getItems(), content.getPipedText());
    }

    @Test
    public void testUpdateSuccess() {
        MessageContent content = buildContent();
        MessagePipedText pipedText = MessagePipedText.builder().content(content).items(new ArrayList<>()).build();
        MessagePipedTextDto dto = MessagePipedTextDto.builder()
            .id(UUID.randomUUID())
            .fileName("updated.csv")
            .items(List.of())
            .build();

        pipedTextService.update(dto, pipedText);

        assertEquals("updated.csv", pipedText.getFileName());
        verify(pipedTextItemService).upsert(dto.getItems(), pipedText);
    }

    @Test
    public void testUpsertNullDtoNullsPipedText() {
        MessageContent content = buildContent();
        content.setPipedText(MessagePipedText.builder().build());

        pipedTextService.upsert(null, content);

        assertNull(content.getPipedText());
    }

    @Test
    public void testUpsertNullIdCreatesNew() {
        MessageContent content = buildContent();
        MessagePipedTextDto dto = MessagePipedTextDto.builder()
            .fileName("new.csv")
            .items(List.of())
            .build();

        pipedTextService.upsert(dto, content);

        assertNotNull(content.getPipedText());
        assertEquals("new.csv", content.getPipedText().getFileName());
        verify(pipedTextItemService).create(dto.getItems(), content.getPipedText());
    }

    @Test
    public void testUpsertExistingIdFoundUpdatesInPlace() {
        MessageContent content = buildContent();
        MessagePipedText existing = MessagePipedText.builder().content(content).items(new ArrayList<>()).build();
        UUID id = UUID.randomUUID();
        MessagePipedTextDto dto = MessagePipedTextDto.builder()
            .id(id)
            .fileName("existing.csv")
            .items(List.of())
            .build();

        when(pipedTextRepository.findByUuidAndContent_UuidAndContent_Message_Container_Owner_LmsUserId(id, content.getUuid(), "owner-lms-id"))
            .thenReturn(Optional.of(existing));

        pipedTextService.upsert(dto, content);

        assertEquals("existing.csv", existing.getFileName());
        verify(pipedTextItemService).upsert(dto.getItems(), existing);
        verify(pipedTextItemService, never()).create(any(List.class), any(MessagePipedText.class));
    }

    @Test
    public void testUpsertExistingIdNotFoundCreatesNew() {
        MessageContent content = buildContent();
        UUID id = UUID.randomUUID();
        MessagePipedTextDto dto = MessagePipedTextDto.builder()
            .id(id)
            .fileName("missing.csv")
            .items(List.of())
            .build();

        when(pipedTextRepository.findByUuidAndContent_UuidAndContent_Message_Container_Owner_LmsUserId(id, content.getUuid(), "owner-lms-id"))
            .thenReturn(Optional.empty());

        pipedTextService.upsert(dto, content);

        assertNotNull(content.getPipedText());
        assertEquals("missing.csv", content.getPipedText().getFileName());
        verify(pipedTextItemService).create(dto.getItems(), content.getPipedText());
    }

    @Test
    public void testProcessPipedTextCsvFileSuccess() throws Exception {
        MessageContent content = MessageContent.builder().build();
        Message message = mockCsvMessage(content);
        MultipartFile file = csvFile("piped.csv", "text/csv", "ID,Course name\nuser1,CourseA\nuser2,CourseB\n");

        when(ltiUserRepository.findFirstByLmsUserIdAndPlatformDeployment("user1", platformDeployment)).thenReturn(Optional.of(ltiUserEntity));
        when(ltiUserRepository.findFirstByLmsUserIdAndPlatformDeployment("user2", platformDeployment)).thenReturn(Optional.of(ltiUserEntity));

        MessagePipedText result = pipedTextService.processPipedTextCsvFile(message, file);

        assertEquals("piped.csv", result.getFileName());
        assertEquals(2, result.getItems().size());
        assertEquals("ID", result.getItems().get(0).getKey());
        assertEquals("Course name", result.getItems().get(1).getKey());
        assertEquals(2, result.getItems().get(0).getValues().size());
        assertEquals("user1", result.getItems().get(0).getValues().get(0).getValue());
        assertEquals("user2", result.getItems().get(0).getValues().get(1).getValue());
        assertEquals("CourseA", result.getItems().get(1).getValues().get(0).getValue());
        assertEquals("CourseB", result.getItems().get(1).getValues().get(1).getValue());
    }

    @Test
    public void testProcessPipedTextCsvFileReusesExistingPipedText() throws Exception {
        MessagePipedText existing = MessagePipedText.builder().items(new ArrayList<>()).build();
        MessageContent content = MessageContent.builder().pipedText(existing).build();
        Message message = mockCsvMessage(content);
        MultipartFile file = csvFile("piped.csv", "text/csv", "ID\nuser1\n");

        when(ltiUserRepository.findFirstByLmsUserIdAndPlatformDeployment("user1", platformDeployment)).thenReturn(Optional.of(ltiUserEntity));

        MessagePipedText result = pipedTextService.processPipedTextCsvFile(message, file);

        assertSame(existing, result);
    }

    @Test
    public void testProcessPipedTextCsvFileSkipsBlankIdRow() throws Exception {
        MessageContent content = MessageContent.builder().build();
        Message message = mockCsvMessage(content);
        MultipartFile file = csvFile("piped.csv", "text/csv", "ID,Course name\n,CourseA\nuser2,CourseB\n");

        when(ltiUserRepository.findFirstByLmsUserIdAndPlatformDeployment("user2", platformDeployment)).thenReturn(Optional.of(ltiUserEntity));

        MessagePipedText result = pipedTextService.processPipedTextCsvFile(message, file);

        assertEquals(1, result.getItems().get(0).getValues().size());
        assertEquals("user2", result.getItems().get(0).getValues().get(0).getValue());
    }

    @Test
    public void testProcessPipedTextCsvFileInvalidContentType() {
        MessageContent content = MessageContent.builder().build();
        Message message = mockCsvMessage(content);
        MultipartFile file = csvFile("piped.txt", "text/plain", "ID\nuser1\n");

        assertThrows(MessagePipedTextFileUploadException.class, () -> pipedTextService.processPipedTextCsvFile(message, file));
    }

    @Test
    public void testProcessPipedTextCsvFileColumnCountMismatch() {
        MessageContent content = MessageContent.builder().build();
        Message message = mockCsvMessage(content);
        MultipartFile file = csvFile("piped.csv", "text/csv", "ID,Course name\nuser1\n");

        assertThrows(MessagePipedTextFileUploadException.class, () -> pipedTextService.processPipedTextCsvFile(message, file));
    }

    @Test
    public void testProcessPipedTextCsvFileUserNotFound() {
        MessageContent content = MessageContent.builder().build();
        Message message = mockCsvMessage(content);
        MultipartFile file = csvFile("piped.csv", "text/csv", "ID,Course name\nuser1,CourseA\n");

        when(ltiUserRepository.findFirstByLmsUserIdAndPlatformDeployment("user1", platformDeployment)).thenReturn(Optional.empty());

        assertThrows(MessagePipedTextFileUploadException.class, () -> pipedTextService.processPipedTextCsvFile(message, file));
    }

    @Test
    public void testProcessPipedTextCsvFileWrapsIOException() throws IOException {
        MessageContent content = MessageContent.builder().build();
        Message message = mockCsvMessage(content);
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("boom"));

        MessagePipedTextFileUploadException exception = assertThrows(
            MessagePipedTextFileUploadException.class,
            () -> pipedTextService.processPipedTextCsvFile(message, file)
        );

        assertEquals("boom", exception.getMessage());
    }

    @Test
    public void testValidatePipedTextFileSuccess() {
        MultipartFile file = csvFile("piped.csv", "text/csv", "ID,Course name\n");

        assertDoesNotThrow(() -> pipedTextService.validatePipedTextFile(message, file));
    }

    @Test
    public void testValidatePipedTextFileMissingRequiredHeader() {
        MultipartFile file = csvFile("piped.csv", "text/csv", "Name,Email\n");

        MessagePipedTextValidationException exception = assertThrows(
            MessagePipedTextValidationException.class,
            () -> pipedTextService.validatePipedTextFile(message, file)
        );

        assertTrue(exception.getMessage().contains("Expected required headers: [ID]."));
    }

    @Test
    public void testValidatePipedTextFileBlankHeader() {
        MultipartFile file = csvFile("piped.csv", "text/csv", "ID,,Email\n");

        MessagePipedTextValidationException exception = assertThrows(
            MessagePipedTextValidationException.class,
            () -> pipedTextService.validatePipedTextFile(message, file)
        );

        assertTrue(exception.getMessage().contains("File contains a blank column header."));
    }

    @Test
    public void testValidatePipedTextFileDuplicateHeaders() {
        MultipartFile file = csvFile("piped.csv", "text/csv", "ID,ID\n");

        MessagePipedTextValidationException exception = assertThrows(
            MessagePipedTextValidationException.class,
            () -> pipedTextService.validatePipedTextFile(message, file)
        );

        assertTrue(exception.getMessage().contains("Duplicate headers found: [ID]."));
    }

    @Test
    public void testValidatePipedTextFileEmptyFileHitsGenericCatch() {
        MultipartFile file = csvFile("piped.csv", "text/csv", "");

        // an empty file yields null headers; Arrays.stream(null) NPEs internally,
        // which is caught by the generic Exception branch rather than surfacing
        // the "expected at least 1 column" validation error.
        MessagePipedTextValidationException exception = assertThrows(
            MessagePipedTextValidationException.class,
            () -> pipedTextService.validatePipedTextFile(message, file)
        );

        assertTrue(exception.getMessage().contains("Error validating piped text file: 'piped.csv'"));
    }

    @Test
    public void testValidatePipedTextFileWrapsIOException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("boom"));
        when(file.getOriginalFilename()).thenReturn("piped.csv");

        MessagePipedTextValidationException exception = assertThrows(
            MessagePipedTextValidationException.class,
            () -> pipedTextService.validatePipedTextFile(message, file)
        );

        assertTrue(exception.getMessage().contains("Error validating piped text file: 'piped.csv'"));
    }

    @Test
    public void testDuplicateNullPipedTextIsNoop() {
        MessageContent content = buildContent();

        pipedTextService.duplicate(null, content);

        assertNull(content.getPipedText());
        verify(pipedTextItemService, never()).duplicate(any(List.class), any(MessagePipedText.class));
    }

    @Test
    public void testDuplicateSuccess() {
        MessageContent sourceContent = buildContent();
        MessagePipedText source = MessagePipedText.builder()
            .content(sourceContent)
            .fileName("source.csv")
            .items(List.of(MessagePipedTextItem.builder().key("key1").build()))
            .build();
        source.setUuid(UUID.randomUUID());

        MessageContent targetContent = buildContent();

        pipedTextService.duplicate(source, targetContent);

        assertNotNull(targetContent.getPipedText());
        assertEquals("source.csv", targetContent.getPipedText().getFileName());
        assertTrue(targetContent.getPipedText().getUuid() != null);
        verify(pipedTextItemService).duplicate(source.getItems(), targetContent.getPipedText());
    }

    @Test
    public void testFromDtoTwoArgDelegatesWithoutItems() {
        MessagePipedText pipedText = MessagePipedText.builder().build();
        UUID id = UUID.randomUUID();
        MessagePipedTextDto dto = MessagePipedTextDto.builder().id(id).fileName("f.csv").build();

        MessagePipedText result = pipedTextService.fromDto(dto, pipedText);

        assertEquals(id, result.getUuid());
        assertEquals("f.csv", result.getFileName());
        verify(pipedTextItemService, never()).fromDto(any(List.class), any(MessagePipedText.class), anyBoolean());
    }

    @Test
    public void testFromDtoNullDto() {
        MessagePipedText pipedText = MessagePipedText.builder().build();

        MessagePipedText result = pipedTextService.fromDto(null, pipedText);

        assertSame(pipedText, result);
    }

    @Test
    public void testFromDtoIncludeItemsTrue() {
        MessagePipedText pipedText = MessagePipedText.builder().items(new ArrayList<>()).build();
        UUID id = UUID.randomUUID();
        MessagePipedTextDto dto = MessagePipedTextDto.builder().id(id).fileName("f.csv").items(List.of(MessagePipedTextItemDto.builder().build())).build();
        List<MessagePipedTextItem> items = List.of(MessagePipedTextItem.builder().key("key1").build());

        when(pipedTextItemService.fromDto(dto.getItems(), pipedText, true)).thenReturn(items);

        MessagePipedText result = pipedTextService.fromDto(dto, pipedText, true);

        assertEquals(items, result.getItems());
    }

    @Test
    public void testToDtoNull() {
        assertNull(pipedTextService.toDto(null));
    }

    @Test
    public void testToDtoSuccess() {
        MessageContent content = buildContent();
        MessagePipedText pipedText = MessagePipedText.builder()
            .content(content)
            .fileName("f.csv")
            .items(List.of(MessagePipedTextItem.builder().key("key1").build()))
            .build();
        UUID id = UUID.randomUUID();
        pipedText.setUuid(id);
        List<MessagePipedTextItemDto> itemDtos = List.of(MessagePipedTextItemDto.builder().key("key1").build());

        when(pipedTextItemService.toDto(pipedText.getItems())).thenReturn(itemDtos);

        MessagePipedTextDto result = pipedTextService.toDto(pipedText);

        assertEquals(id, result.getId());
        assertEquals(content.getUuid(), result.getContentId());
        assertEquals("f.csv", result.getFileName());
        assertEquals(itemDtos, result.getItems());
    }

}
