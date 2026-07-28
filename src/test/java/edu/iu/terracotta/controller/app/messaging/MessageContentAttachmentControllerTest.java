package edu.iu.terracotta.controller.app.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.model.dto.messaging.content.MessageContentAttachmentDto;
import edu.iu.terracotta.exceptions.messaging.MessageContainerNotFoundException;
import edu.iu.terracotta.exceptions.messaging.MessageContentNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageNotFoundException;
import edu.iu.terracotta.service.app.messaging.MessageContentAttachmentService;
import jakarta.servlet.http.HttpServletRequest;

public class MessageContentAttachmentControllerTest extends BaseTest {

    private static final long EXPERIMENT_ID = 1L;
    private static final long EXPOSURE_ID = 2L;

    @Mock private MessageContentAttachmentService messageContentAttachmentService;
    @Mock private MessageContent messageContent;

    private MessageContentAttachmentController messageContentAttachmentController;
    private UUID containerUuid;
    private UUID messageUuid;
    private UUID contentUuid;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        // ApiJwtService has multiple type-matching mocks in BaseServiceTest (e.g. canvasApiJwtService
        // also implements it), so @InjectMocks constructor resolution by type alone is unreliable;
        // construct the controller explicitly instead.
        messageContentAttachmentController = new MessageContentAttachmentController(apiJwtService, messageContentAttachmentService);
        containerUuid = UUID.randomUUID();
        messageUuid = UUID.randomUUID();
        contentUuid = UUID.randomUUID();

        when(apiJwtService.extractValues(any(HttpServletRequest.class), eq(false))).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.messagingContentAllowed(securedInfo, messageUuid, contentUuid)).thenReturn(messageContent);
    }

    @Test
    void testGetUnauthorizedWhenNotInstructor() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<MessageContentAttachmentDto>> response = messageContentAttachmentController.get(EXPERIMENT_ID, EXPOSURE_ID, containerUuid, messageUuid, contentUuid, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(apiJwtService, never()).messagingContainerAllowed(any(), org.mockito.ArgumentMatchers.anyLong(), any());
    }

    @Test
    void testGetUnauthorizedWhenContainerNotFound() throws Exception {
        doThrow(new MessageContainerNotFoundException("container not found")).when(apiJwtService).messagingContainerAllowed(securedInfo, EXPOSURE_ID, containerUuid);

        ResponseEntity<List<MessageContentAttachmentDto>> response = messageContentAttachmentController.get(EXPERIMENT_ID, EXPOSURE_ID, containerUuid, messageUuid, contentUuid, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testGetUnauthorizedWhenMessageNotFound() throws Exception {
        doThrow(new MessageNotFoundException("message not found")).when(apiJwtService).messagingAllowed(securedInfo, containerUuid, messageUuid);

        ResponseEntity<List<MessageContentAttachmentDto>> response = messageContentAttachmentController.get(EXPERIMENT_ID, EXPOSURE_ID, containerUuid, messageUuid, contentUuid, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testGetUnauthorizedWhenContentNotMatching() throws Exception {
        when(apiJwtService.messagingContentAllowed(securedInfo, messageUuid, contentUuid)).thenThrow(new MessageContentNotMatchingException("content not matching"));

        ResponseEntity<List<MessageContentAttachmentDto>> response = messageContentAttachmentController.get(EXPERIMENT_ID, EXPOSURE_ID, containerUuid, messageUuid, contentUuid, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testGetReturnsAttachments() throws Exception {
        List<MessageContentAttachmentDto> attachments = List.of(new MessageContentAttachmentDto());
        when(messageContentAttachmentService.get(messageContent)).thenReturn(attachments);

        ResponseEntity<List<MessageContentAttachmentDto>> response = messageContentAttachmentController.get(EXPERIMENT_ID, EXPOSURE_ID, containerUuid, messageUuid, contentUuid, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(attachments, response.getBody());
    }

    @Test
    void testGetReturnsBadRequestOnServiceException() throws Exception {
        when(messageContentAttachmentService.get(messageContent)).thenThrow(new RuntimeException("boom"));

        ResponseEntity<List<MessageContentAttachmentDto>> response = messageContentAttachmentController.get(EXPERIMENT_ID, EXPOSURE_ID, containerUuid, messageUuid, contentUuid, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

}
