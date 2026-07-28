package edu.iu.terracotta.controller.app.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
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

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.dao.model.dto.messaging.container.MessageContainerDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerNotFoundException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerOwnerNotMatchingException;
import edu.iu.terracotta.service.app.messaging.MessageContainerService;

public class MessageContainerControllerTest extends BaseTest {

    private static final long EXPERIMENT_ID = 1L;
    private static final long EXPOSURE_ID = 1L;
    private static final UUID UUID_VALUE = UUID.randomUUID();

    @Mock private MessageContainerService messageContainerService;
    @Mock private MessageContainerDto messageContainerDto;
    @Mock private MessageContainer messageContainer;

    private MessageContainerController messageContainerController;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        // Constructed manually rather than via @InjectMocks: ApiJwtService is also implemented by the
        // inherited canvasApiJwtService mock (see the ambiguity warning in BaseServiceTest), so
        // constructor-injection-by-type could silently wire the wrong ApiJwtService mock.
        messageContainerController = new MessageContainerController(apiJwtService, messageContainerService);

        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
    }

    @Test
    void getAllTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(messageContainerService.getAll(anyLong(), anyLong(), any(SecuredInfo.class))).thenReturn(List.of(messageContainerDto));

        ResponseEntity<List<MessageContainerDto>> response = messageContainerController.getAll(EXPERIMENT_ID, EXPOSURE_ID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getAllUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<MessageContainerDto>> response = messageContainerController.getAll(EXPERIMENT_ID, EXPOSURE_ID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getAllBadTokenTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        doThrow(new ExperimentNotMatchingException("error")).when(apiJwtService).experimentAllowed(any(SecuredInfo.class), anyLong());

        ResponseEntity<List<MessageContainerDto>> response = messageContainerController.getAll(EXPERIMENT_ID, EXPOSURE_ID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void postTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.exposureAllowed(any(SecuredInfo.class), anyLong(), anyLong())).thenReturn(exposure);
        when(messageContainerService.create(any(Exposure.class), anyBoolean(), any(SecuredInfo.class))).thenReturn(messageContainerDto);

        ResponseEntity<MessageContainerDto> response = messageContainerController.post(EXPERIMENT_ID, EXPOSURE_ID, false, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messageContainerDto, response.getBody());
    }

    @Test
    void postUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<MessageContainerDto> response = messageContainerController.post(EXPERIMENT_ID, EXPOSURE_ID, false, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void postExposureNotMatchingTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        doThrow(new ExposureNotMatchingException("error")).when(apiJwtService).exposureAllowed(any(SecuredInfo.class), anyLong(), anyLong());

        ResponseEntity<MessageContainerDto> response = messageContainerController.post(EXPERIMENT_ID, EXPOSURE_ID, false, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void putTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.messagingContainerAllowed(any(SecuredInfo.class), anyLong(), any(UUID.class))).thenReturn(messageContainer);
        when(messageContainerDto.getExposureId()).thenReturn(EXPOSURE_ID);
        when(messageContainerService.update(any(MessageContainerDto.class), any(MessageContainer.class))).thenReturn(messageContainerDto);

        ResponseEntity<MessageContainerDto> response = messageContainerController.put(EXPERIMENT_ID, EXPOSURE_ID, UUID_VALUE, messageContainerDto, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messageContainerDto, response.getBody());
    }

    // dto.getExposureId() differs from the path exposureId, so put() internally re-enters move() before
    // calling update() - see MessageContainerController.put()/move() note in the final report.
    @Test
    void putTriggersMoveWhenExposureChangedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.messagingContainerAllowed(any(SecuredInfo.class), anyLong(), any(UUID.class))).thenReturn(messageContainer);
        when(apiJwtService.exposureAllowed(any(SecuredInfo.class), anyLong(), anyLong())).thenReturn(exposure);
        when(messageContainerDto.getExposureId()).thenReturn(2L);
        when(messageContainerService.move(any(Exposure.class), any(MessageContainer.class))).thenReturn(messageContainerDto);
        when(messageContainerService.update(any(MessageContainerDto.class), any(MessageContainer.class))).thenReturn(messageContainerDto);

        ResponseEntity<MessageContainerDto> response = messageContainerController.put(EXPERIMENT_ID, EXPOSURE_ID, UUID_VALUE, messageContainerDto, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void putUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<MessageContainerDto> response = messageContainerController.put(EXPERIMENT_ID, EXPOSURE_ID, UUID_VALUE, messageContainerDto, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void putNotFoundTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        doThrow(new MessageContainerNotFoundException("error")).when(apiJwtService).messagingContainerAllowed(any(SecuredInfo.class), anyLong(), any(UUID.class));

        ResponseEntity<MessageContainerDto> response = messageContainerController.put(EXPERIMENT_ID, EXPOSURE_ID, UUID_VALUE, messageContainerDto, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void putBadRequestTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.messagingContainerAllowed(any(SecuredInfo.class), anyLong(), any(UUID.class))).thenReturn(messageContainer);
        when(messageContainerDto.getExposureId()).thenReturn(EXPOSURE_ID);
        when(messageContainerService.update(any(MessageContainerDto.class), any(MessageContainer.class))).thenThrow(new RuntimeException("boom"));

        ResponseEntity<MessageContainerDto> response = messageContainerController.put(EXPERIMENT_ID, EXPOSURE_ID, UUID_VALUE, messageContainerDto, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void putAllTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(messageContainerDto.getId()).thenReturn(UUID_VALUE);
        when(apiJwtService.messagingContainerAllowed(any(SecuredInfo.class), anyLong(), any(UUID.class))).thenReturn(messageContainer);
        when(messageContainerService.updateAll(anyList(), anyList())).thenReturn(List.of(messageContainerDto));

        ResponseEntity<List<MessageContainerDto>> response = messageContainerController.putAll(EXPERIMENT_ID, EXPOSURE_ID, List.of(messageContainerDto), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void putAllUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<MessageContainerDto>> response = messageContainerController.putAll(EXPERIMENT_ID, EXPOSURE_ID, List.of(messageContainerDto), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void putAllNotMatchingTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(messageContainerDto.getId()).thenReturn(UUID_VALUE);
        doThrow(new MessageContainerOwnerNotMatchingException("error")).when(apiJwtService).messagingContainerAllowed(any(SecuredInfo.class), anyLong(), any(UUID.class));

        ResponseEntity<List<MessageContainerDto>> response = messageContainerController.putAll(EXPERIMENT_ID, EXPOSURE_ID, List.of(messageContainerDto), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void putAllBadRequestTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(messageContainerDto.getId()).thenReturn(UUID_VALUE);
        when(apiJwtService.messagingContainerAllowed(any(SecuredInfo.class), anyLong(), any(UUID.class))).thenReturn(messageContainer);
        when(messageContainerService.updateAll(anyList(), anyList())).thenThrow(new RuntimeException("boom"));

        ResponseEntity<List<MessageContainerDto>> response = messageContainerController.putAll(EXPERIMENT_ID, EXPOSURE_ID, List.of(messageContainerDto), httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void deleteTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.messagingContainerAllowed(any(SecuredInfo.class), anyLong(), any(UUID.class))).thenReturn(messageContainer);
        when(messageContainerService.delete(any(MessageContainer.class))).thenReturn(messageContainerDto);

        ResponseEntity<MessageContainerDto> response = messageContainerController.delete(EXPERIMENT_ID, EXPOSURE_ID, UUID_VALUE, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messageContainerDto, response.getBody());
    }

    @Test
    void deleteUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<MessageContainerDto> response = messageContainerController.delete(EXPERIMENT_ID, EXPOSURE_ID, UUID_VALUE, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void deleteNotMatchingTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        doThrow(new MessageContainerNotMatchingException("error")).when(apiJwtService).messagingContainerAllowed(any(SecuredInfo.class), anyLong(), any(UUID.class));

        ResponseEntity<MessageContainerDto> response = messageContainerController.delete(EXPERIMENT_ID, EXPOSURE_ID, UUID_VALUE, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void deleteServiceExceptionReturnsBadRequestTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.messagingContainerAllowed(any(SecuredInfo.class), anyLong(), any(UUID.class))).thenReturn(messageContainer);
        when(messageContainerService.delete(any(MessageContainer.class))).thenThrow(new RuntimeException("boom"));

        ResponseEntity<MessageContainerDto> response = messageContainerController.delete(EXPERIMENT_ID, EXPOSURE_ID, UUID_VALUE, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void moveTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.messagingContainerAllowed(any(SecuredInfo.class), anyLong(), any(UUID.class))).thenReturn(messageContainer);
        when(apiJwtService.exposureAllowed(any(SecuredInfo.class), anyLong(), anyLong())).thenReturn(exposure);
        when(messageContainerService.move(any(Exposure.class), any(MessageContainer.class))).thenReturn(messageContainerDto);

        ResponseEntity<MessageContainerDto> response = messageContainerController.move(EXPERIMENT_ID, EXPOSURE_ID, UUID_VALUE, messageContainerDto, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messageContainerDto, response.getBody());
    }

    @Test
    void moveUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<MessageContainerDto> response = messageContainerController.move(EXPERIMENT_ID, EXPOSURE_ID, UUID_VALUE, messageContainerDto, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void moveBadTokenTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        doThrow(new BadTokenException("error")).when(apiJwtService).experimentAllowed(any(SecuredInfo.class), anyLong());

        ResponseEntity<MessageContainerDto> response = messageContainerController.move(EXPERIMENT_ID, EXPOSURE_ID, UUID_VALUE, messageContainerDto, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void moveBadRequestTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.messagingContainerAllowed(any(SecuredInfo.class), anyLong(), any(UUID.class))).thenReturn(messageContainer);
        when(apiJwtService.exposureAllowed(any(SecuredInfo.class), anyLong(), anyLong())).thenReturn(exposure);
        when(messageContainerService.move(any(Exposure.class), any(MessageContainer.class))).thenThrow(new RuntimeException("boom"));

        ResponseEntity<MessageContainerDto> response = messageContainerController.move(EXPERIMENT_ID, EXPOSURE_ID, UUID_VALUE, messageContainerDto, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void duplicateTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.messagingContainerAllowed(any(SecuredInfo.class), anyLong(), any(UUID.class))).thenReturn(messageContainer);
        when(apiJwtService.exposureAllowed(any(SecuredInfo.class), anyLong(), anyLong())).thenReturn(exposure);
        when(messageContainerService.duplicate(any(MessageContainer.class), any(Exposure.class))).thenReturn(messageContainerDto);

        ResponseEntity<MessageContainerDto> response = messageContainerController.duplicate(EXPERIMENT_ID, EXPOSURE_ID, UUID_VALUE, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messageContainerDto, response.getBody());
    }

    @Test
    void duplicateUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<MessageContainerDto> response = messageContainerController.duplicate(EXPERIMENT_ID, EXPOSURE_ID, UUID_VALUE, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void duplicateBadRequestTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.messagingContainerAllowed(any(SecuredInfo.class), anyLong(), any(UUID.class))).thenReturn(messageContainer);
        when(apiJwtService.exposureAllowed(any(SecuredInfo.class), anyLong(), anyLong())).thenReturn(exposure);
        when(messageContainerService.duplicate(any(MessageContainer.class), any(Exposure.class))).thenThrow(new RuntimeException("boom"));

        ResponseEntity<MessageContainerDto> response = messageContainerController.duplicate(EXPERIMENT_ID, EXPOSURE_ID, UUID_VALUE, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

}
