package edu.iu.terracotta.connectors.canvas.service.lti.advantage.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.canvas.dao.model.lti.advantage.CanvasNoticeHandlerDto;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;

public class CanvasAdvantageNoticeServiceImplTest extends BaseTest {

    @InjectMocks private CanvasAdvantageNoticeServiceImpl canvasAdvantageNoticeService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        when(toolDeployment.getPlatformDeployment()).thenReturn(platformDeployment);
        when(platformDeployment.getBaseUrl()).thenReturn("https://canvas.example.com");
        when(platformDeployment.getLocalUrl()).thenReturn("https://terracotta.example.com");
    }

    @Test
    public void testEnsureNoticeHandlerRegisteredNoOpWhenAlreadyRegistered() throws ConnectionException {
        when(toolDeployment.isNoticeHandlerRegistered()).thenReturn(true);

        canvasAdvantageNoticeService.ensureNoticeHandlerRegistered(toolDeployment);

        verify(advantageConnectorHelper, never()).getToken(any(), anyString());
        verify(toolDeploymentRepository, never()).save(any());
    }

    @Test
    public void testEnsureNoticeHandlerRegisteredSkipsWhenDeploymentIdHasNoNumericPrefix() throws ConnectionException {
        when(toolDeployment.isNoticeHandlerRegistered()).thenReturn(false);
        when(toolDeployment.getLtiDeploymentId()).thenReturn("not-a-numeric-prefix:abc123");

        canvasAdvantageNoticeService.ensureNoticeHandlerRegistered(toolDeployment);

        verify(advantageConnectorHelper, never()).getToken(any(), anyString());
        verify(toolDeploymentRepository, never()).save(any());
    }

    @Test
    public void testEnsureNoticeHandlerRegisteredSkipsWhenDeploymentIdBlank() throws ConnectionException {
        when(toolDeployment.isNoticeHandlerRegistered()).thenReturn(false);
        when(toolDeployment.getLtiDeploymentId()).thenReturn(null);

        canvasAdvantageNoticeService.ensureNoticeHandlerRegistered(toolDeployment);

        verify(advantageConnectorHelper, never()).getToken(any(), anyString());
        verify(toolDeploymentRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEnsureNoticeHandlerRegisteredSuccessCallsCorrectUrlAndBodyAndMarksRegistered() throws ConnectionException {
        when(toolDeployment.isNoticeHandlerRegistered()).thenReturn(false);
        when(toolDeployment.getLtiDeploymentId()).thenReturn("37:b82229c6e10bcb87beb1f1b287faee560ddc3109");
        when(toolDeployment.getDeploymentId()).thenReturn(1L);
        when(advantageConnectorHelper.getToken(platformDeployment, "https://purl.imsglobal.org/spec/lti/scope/noticehandlers")).thenReturn(ltiToken);
        when(ltiToken.getAccess_token()).thenReturn("access-token-value");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(CanvasNoticeHandlerDto.class)))
            .thenReturn(new ResponseEntity<>(CanvasNoticeHandlerDto.builder().build(), org.springframework.http.HttpStatus.OK));

        canvasAdvantageNoticeService.ensureNoticeHandlerRegistered(toolDeployment);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpEntity<CanvasNoticeHandlerDto>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.PUT), entityCaptor.capture(), eq(CanvasNoticeHandlerDto.class));

        assertEquals("https://canvas.example.com/api/lti/notice-handlers/37", urlCaptor.getValue());

        CanvasNoticeHandlerDto body = entityCaptor.getValue().getBody();
        assertEquals("LtiContextCopyNotice", body.getNotice_type());
        assertEquals("https://terracotta.example.com/notice", body.getHandler());
        assertEquals("Bearer access-token-value", entityCaptor.getValue().getHeaders().getFirst("Authorization"));

        verify(toolDeployment).setNoticeHandlerRegistered(true);
        verify(toolDeploymentRepository).save(toolDeployment);
    }

    @Test
    public void testEnsureNoticeHandlerRegisteredCapturesErrorAndLeavesUnregistered() throws ConnectionException {
        when(toolDeployment.isNoticeHandlerRegistered()).thenReturn(false);
        when(toolDeployment.getLtiDeploymentId()).thenReturn("37:b82229c6e10bcb87beb1f1b287faee560ddc3109");
        doThrow(new RuntimeException("connection failed")).when(advantageConnectorHelper).getToken(any(), anyString());

        canvasAdvantageNoticeService.ensureNoticeHandlerRegistered(toolDeployment);

        verify(toolDeployment, never()).setNoticeHandlerRegistered(true);
        verify(toolDeploymentRepository, never()).save(any());
    }

}
