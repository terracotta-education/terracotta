package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.service.app.AdminService;

public class AdminControllerTest extends BaseTest {

    private AdminController adminController;
    @Mock private AdminService adminService;

    private Map<String, String> options;

    @BeforeEach
    public void beforeEach() throws NumberFormatException, TerracottaConnectorException {
        MockitoAnnotations.openMocks(this);

        setup();

        options = Map.of("tokenOverride", "override-token");

        // Constructed manually rather than via @InjectMocks: ApiJwtService is also implemented by the
        // inherited canvasApiJwtService mock (see the ambiguity warning in BaseServiceTest), so
        // constructor-injection-by-type could silently wire the wrong ApiJwtService mock.
        adminController = new AdminController(apiJwtService, adminService);

        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
    }

    @Test
    void resyncTargetUrisUnauthorizedTest() throws Exception {
        when(apiJwtService.isTerracottaAdmin(securedInfo)).thenReturn(false);

        ResponseEntity<Void> ret = adminController.resyncTargetUris(1L, options, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertNull(ret.getBody());
    }

    @Test
    void resyncTargetUrisAcceptedTest() throws Exception {
        when(apiJwtService.isTerracottaAdmin(securedInfo)).thenReturn(true);

        ResponseEntity<Void> ret = adminController.resyncTargetUris(1L, options, httpServletRequest);

        assertEquals(HttpStatus.ACCEPTED, ret.getStatusCode());
        verify(adminService).resyncTargetUris(1L, "override-token");
    }

    @Test
    void resyncTargetUrisAcceptedWhenApiExceptionCaughtTest() throws Exception {
        when(apiJwtService.isTerracottaAdmin(securedInfo)).thenReturn(true);
        doThrow(new ApiException("api error")).when(adminService).resyncTargetUris(anyLong(), anyString());

        ResponseEntity<Void> ret = adminController.resyncTargetUris(1L, options, httpServletRequest);

        assertEquals(HttpStatus.ACCEPTED, ret.getStatusCode());
    }

    @Test
    void resyncTargetUrisAcceptedWhenDataServiceExceptionCaughtTest() throws Exception {
        when(apiJwtService.isTerracottaAdmin(securedInfo)).thenReturn(true);
        doThrow(new DataServiceException("data error")).when(adminService).resyncTargetUris(anyLong(), anyString());

        ResponseEntity<Void> ret = adminController.resyncTargetUris(1L, options, httpServletRequest);

        assertEquals(HttpStatus.ACCEPTED, ret.getStatusCode());
    }

    @Test
    void resyncTargetUrisAcceptedWhenConnectionExceptionCaughtTest() throws Exception {
        when(apiJwtService.isTerracottaAdmin(securedInfo)).thenReturn(true);
        doThrow(new ConnectionException("connection error")).when(adminService).resyncTargetUris(anyLong(), anyString());

        ResponseEntity<Void> ret = adminController.resyncTargetUris(1L, options, httpServletRequest);

        assertEquals(HttpStatus.ACCEPTED, ret.getStatusCode());
    }

    @Test
    void resyncTargetUrisAcceptedWhenIOExceptionCaughtTest() throws Exception {
        when(apiJwtService.isTerracottaAdmin(securedInfo)).thenReturn(true);
        doThrow(new IOException("io error")).when(adminService).resyncTargetUris(anyLong(), anyString());

        ResponseEntity<Void> ret = adminController.resyncTargetUris(1L, options, httpServletRequest);

        assertEquals(HttpStatus.ACCEPTED, ret.getStatusCode());
    }

    @Test
    void resyncTargetUrisPropagatesTerracottaConnectorExceptionTest() throws Exception {
        doThrow(new TerracottaConnectorException("error")).when(apiJwtService).extractValues(httpServletRequest, false);

        assertThrows(TerracottaConnectorException.class, () -> adminController.resyncTargetUris(1L, options, httpServletRequest));
    }

}
