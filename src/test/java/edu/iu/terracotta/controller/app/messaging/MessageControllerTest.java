package edu.iu.terracotta.controller.app.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.messaging.content.MessageContentDto;
import edu.iu.terracotta.dao.model.dto.messaging.message.MessageDto;
import edu.iu.terracotta.dao.model.dto.messaging.preview.MessagePreviewDto;
import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleAssignmentDto;
import edu.iu.terracotta.dao.model.dto.messaging.send.MessageSendTestDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.messaging.MessageBodyParseException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerNotFoundException;
import edu.iu.terracotta.exceptions.messaging.MessageContentNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageNotFoundException;
import edu.iu.terracotta.exceptions.messaging.MessagePipedTextFileUploadException;
import edu.iu.terracotta.exceptions.messaging.MessageSendEmailException;
import edu.iu.terracotta.service.app.messaging.MessageContentService;
import edu.iu.terracotta.service.app.messaging.MessageEmailService;
import edu.iu.terracotta.service.app.messaging.MessagePreviewService;
import edu.iu.terracotta.service.app.messaging.MessageService;

public class MessageControllerTest extends BaseTest {

    @Mock private MessageContentService contentService;
    @Mock private MessageEmailService messageEmailService;
    @Mock private MessagePreviewService previewService;
    @Mock private MessageService messageService;

    @Mock private MessageContainer messageContainer;
    @Mock private Message message;
    @Mock private MessageContent messageContent;

    private MessageController messageController;

    private final long experimentId = 1L;
    private final long exposureId = 1L;
    private final UUID containerUuid = UUID.randomUUID();
    private final UUID messageUuid = UUID.randomUUID();
    private final UUID uuid = UUID.randomUUID();

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        // Constructed manually rather than via @InjectMocks: ApiJwtService is also implemented by the
        // inherited canvasApiJwtService mock (see the ambiguity warning in BaseServiceTest), so
        // constructor-injection-by-type could silently wire the wrong ApiJwtService mock.
        messageController = new MessageController(apiJwtService, contentService, messageEmailService, previewService, messageService);
    }

    private void stubAllowedForMessage() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.experimentAllowed(eq(securedInfo), anyLong())).thenReturn(experiment);
        when(apiJwtService.exposureAllowed(eq(securedInfo), anyLong(), anyLong())).thenReturn(exposure);
        when(apiJwtService.messagingContainerAllowed(eq(securedInfo), anyLong(), eq(containerUuid))).thenReturn(messageContainer);
        when(apiJwtService.messagingAllowed(eq(securedInfo), eq(containerUuid), eq(uuid))).thenReturn(message);
    }

    private void stubAllowedForContent() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.experimentAllowed(eq(securedInfo), anyLong())).thenReturn(experiment);
        when(apiJwtService.exposureAllowed(eq(securedInfo), anyLong(), anyLong())).thenReturn(exposure);
        when(apiJwtService.messagingContainerAllowed(eq(securedInfo), anyLong(), eq(containerUuid))).thenReturn(messageContainer);
        when(apiJwtService.messagingAllowed(eq(securedInfo), eq(containerUuid), eq(messageUuid))).thenReturn(message);
        when(apiJwtService.messagingContentAllowed(eq(securedInfo), eq(messageUuid), eq(uuid))).thenReturn(messageContent);
    }

    @Test
    void putTest() throws Exception {
        stubAllowedForMessage();
        MessageDto returned = new MessageDto();
        when(messageService.put(any(MessageDto.class), anyLong(), eq(messageContainer), eq(message))).thenReturn(returned);

        ResponseEntity<MessageDto> response = messageController.put(experimentId, exposureId, containerUuid, uuid, new MessageDto(), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(returned, response.getBody());
    }

    @Test
    void putUnauthorizedTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<MessageDto> response = messageController.put(experimentId, exposureId, containerUuid, uuid, new MessageDto(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void putAllowedCheckFailsTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.experimentAllowed(eq(securedInfo), anyLong())).thenThrow(new ExperimentNotMatchingException("no match"));

        ResponseEntity<MessageDto> response = messageController.put(experimentId, exposureId, containerUuid, uuid, new MessageDto(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void putServiceThrowsTest() throws Exception {
        stubAllowedForMessage();
        when(messageService.put(any(MessageDto.class), anyLong(), eq(messageContainer), eq(message))).thenThrow(new RuntimeException("boom"));

        ResponseEntity<MessageDto> response = messageController.put(experimentId, exposureId, containerUuid, uuid, new MessageDto(), httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void putPropagatesUncaughtExtractValuesExceptionTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenThrow(new TerracottaConnectorException("bad token"));

        assertThrows(TerracottaConnectorException.class, () -> messageController.put(experimentId, exposureId, containerUuid, uuid, new MessageDto(), httpServletRequest));
    }

    @Test
    void getAssignmentsTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        List<MessageRuleAssignmentDto> returned = List.of(new MessageRuleAssignmentDto());
        when(messageService.getAssignments(securedInfo)).thenReturn(returned);

        ResponseEntity<List<MessageRuleAssignmentDto>> response = messageController.getAssignments(experimentId, exposureId, containerUuid, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(returned, response.getBody());
    }

    @Test
    void getAssignmentsUnauthorizedTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<MessageRuleAssignmentDto>> response = messageController.getAssignments(experimentId, exposureId, containerUuid, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getAssignmentsServiceThrowsTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(messageService.getAssignments(securedInfo)).thenThrow(new DataServiceException("boom"));

        ResponseEntity<List<MessageRuleAssignmentDto>> response = messageController.getAssignments(experimentId, exposureId, containerUuid, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void previewTest() throws Exception {
        stubAllowedForMessage();
        MessagePreviewDto returned = new MessagePreviewDto();
        when(previewService.preview(any(MessagePreviewDto.class), eq(message))).thenReturn(returned);

        ResponseEntity<MessagePreviewDto> response = messageController.preview(experimentId, exposureId, containerUuid, uuid, new MessagePreviewDto(), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(returned, response.getBody());
    }

    @Test
    void previewUnauthorizedTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<MessagePreviewDto> response = messageController.preview(experimentId, exposureId, containerUuid, uuid, new MessagePreviewDto(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void previewAllowedCheckFailsTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.experimentAllowed(eq(securedInfo), anyLong())).thenReturn(experiment);
        when(apiJwtService.exposureAllowed(eq(securedInfo), anyLong(), anyLong())).thenReturn(exposure);
        when(apiJwtService.messagingContainerAllowed(eq(securedInfo), anyLong(), eq(containerUuid))).thenThrow(new MessageContainerNotFoundException("not found"));

        ResponseEntity<MessagePreviewDto> response = messageController.preview(experimentId, exposureId, containerUuid, uuid, new MessagePreviewDto(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void previewServiceThrowsTest() throws Exception {
        stubAllowedForMessage();
        when(previewService.preview(any(MessagePreviewDto.class), eq(message))).thenThrow(new MessageBodyParseException("boom"));

        ResponseEntity<MessagePreviewDto> response = messageController.preview(experimentId, exposureId, containerUuid, uuid, new MessagePreviewDto(), httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void sendTestTest() throws Exception {
        stubAllowedForMessage();
        doNothing().when(messageEmailService).sendTest(eq(message), any(MessageSendTestDto.class));

        ResponseEntity<Void> response = messageController.sendTest(experimentId, exposureId, containerUuid, uuid, new MessageSendTestDto(), httpServletRequest);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    void sendTestUnauthorizedTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = messageController.sendTest(experimentId, exposureId, containerUuid, uuid, new MessageSendTestDto(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void sendTestAllowedCheckFailsTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.experimentAllowed(eq(securedInfo), anyLong())).thenReturn(experiment);
        when(apiJwtService.exposureAllowed(eq(securedInfo), anyLong(), anyLong())).thenReturn(exposure);
        when(apiJwtService.messagingContainerAllowed(eq(securedInfo), anyLong(), eq(containerUuid))).thenReturn(messageContainer);
        when(apiJwtService.messagingAllowed(eq(securedInfo), eq(containerUuid), eq(uuid))).thenThrow(new MessageNotFoundException("not found"));

        ResponseEntity<Void> response = messageController.sendTest(experimentId, exposureId, containerUuid, uuid, new MessageSendTestDto(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void sendTestServiceThrowsTest() throws Exception {
        stubAllowedForMessage();
        doThrow(new MessageSendEmailException("boom", new RuntimeException())).when(messageEmailService).sendTest(eq(message), any(MessageSendTestDto.class));

        ResponseEntity<Void> response = messageController.sendTest(experimentId, exposureId, containerUuid, uuid, new MessageSendTestDto(), httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void pipedTextCsvTest() throws Exception {
        stubAllowedForContent();
        MessageDto returned = new MessageDto();
        when(messageService.processPipedTextCsvFile(eq(message), any(MultipartFile.class))).thenReturn(returned);

        ResponseEntity<MessageDto> response = messageController.pipedTextCsv(experimentId, exposureId, containerUuid, messageUuid, uuid, multipartFile, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(returned, response.getBody());
    }

    @Test
    void pipedTextCsvUnauthorizedTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<MessageDto> response = messageController.pipedTextCsv(experimentId, exposureId, containerUuid, messageUuid, uuid, multipartFile, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void pipedTextCsvAllowedCheckFailsTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.experimentAllowed(eq(securedInfo), anyLong())).thenReturn(experiment);
        when(apiJwtService.exposureAllowed(eq(securedInfo), anyLong(), anyLong())).thenReturn(exposure);
        when(apiJwtService.messagingContainerAllowed(eq(securedInfo), anyLong(), eq(containerUuid))).thenReturn(messageContainer);
        when(apiJwtService.messagingAllowed(eq(securedInfo), eq(containerUuid), eq(messageUuid))).thenReturn(message);
        when(apiJwtService.messagingContentAllowed(eq(securedInfo), eq(messageUuid), eq(uuid))).thenThrow(new MessageContentNotMatchingException("no match"));

        ResponseEntity<MessageDto> response = messageController.pipedTextCsv(experimentId, exposureId, containerUuid, messageUuid, uuid, multipartFile, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void pipedTextCsvServiceThrowsTest() throws Exception {
        stubAllowedForContent();
        when(messageService.processPipedTextCsvFile(eq(message), any(MultipartFile.class))).thenThrow(new MessagePipedTextFileUploadException("boom"));

        ResponseEntity<MessageDto> response = messageController.pipedTextCsv(experimentId, exposureId, containerUuid, messageUuid, uuid, multipartFile, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void updatePlaceholdersTest() throws Exception {
        stubAllowedForContent();
        MessageContentDto returned = new MessageContentDto();
        when(contentService.updatePlaceholders(eq(messageContent), any(MessageContentDto.class))).thenReturn(returned);

        ResponseEntity<MessageContentDto> response = messageController.updatePlaceholders(experimentId, exposureId, containerUuid, messageUuid, uuid, new MessageContentDto(), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(returned, response.getBody());
    }

    @Test
    void updatePlaceholdersUnauthorizedTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<MessageContentDto> response = messageController.updatePlaceholders(experimentId, exposureId, containerUuid, messageUuid, uuid, new MessageContentDto(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void updatePlaceholdersAllowedCheckFailsTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.experimentAllowed(eq(securedInfo), anyLong())).thenReturn(experiment);
        when(apiJwtService.exposureAllowed(eq(securedInfo), anyLong(), anyLong())).thenReturn(exposure);
        when(apiJwtService.messagingContainerAllowed(eq(securedInfo), anyLong(), eq(containerUuid))).thenReturn(messageContainer);
        when(apiJwtService.messagingAllowed(eq(securedInfo), eq(containerUuid), eq(messageUuid))).thenReturn(message);
        when(apiJwtService.messagingContentAllowed(eq(securedInfo), eq(messageUuid), eq(uuid))).thenThrow(new MessageContentNotMatchingException("no match"));

        ResponseEntity<MessageContentDto> response = messageController.updatePlaceholders(experimentId, exposureId, containerUuid, messageUuid, uuid, new MessageContentDto(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void updatePlaceholdersServiceThrowsTest() throws Exception {
        stubAllowedForContent();
        when(contentService.updatePlaceholders(eq(messageContent), any(MessageContentDto.class))).thenThrow(new MessageBodyParseException("boom"));

        ResponseEntity<MessageContentDto> response = messageController.updatePlaceholders(experimentId, exposureId, containerUuid, messageUuid, uuid, new MessageContentDto(), httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

}
