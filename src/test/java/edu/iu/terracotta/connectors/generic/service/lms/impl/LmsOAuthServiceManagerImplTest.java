package edu.iu.terracotta.connectors.generic.service.lms.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.lms.LmsOAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class LmsOAuthServiceManagerImplTest extends BaseTest {


    @InjectMocks private LmsOAuthServiceManagerImpl lmsOAuthServiceManagerImpl;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();
    }

    @Test
    public void testGetLmsOAuthServiceByPlatformDeploymentSuccess() throws TerracottaConnectorException {
        when(lmsOAuthConnectorService.instance(anyLong(), any(Class.class))).thenReturn(lmsOAuthService);

        LmsOAuthService<?> result = lmsOAuthServiceManagerImpl.getLmsOAuthService(platformDeployment);

        assertEquals(lmsOAuthService, result);
        verify(lmsOAuthConnectorService).instance(platformDeployment.getKeyId(), LmsOAuthService.class);
    }

    @Test
    public void testGetLmsOAuthServiceByPlatformDeploymentFailure() throws TerracottaConnectorException {
        when(lmsOAuthConnectorService.instance(anyLong(), any(Class.class))).thenThrow(new TerracottaConnectorException("Error"));

        assertThrows(TerracottaConnectorException.class, () -> lmsOAuthServiceManagerImpl.getLmsOAuthService(platformDeployment));
    }

    @Test
    public void testGetLmsOAuthServiceByIdSuccess() throws TerracottaConnectorException {
        when(lmsOAuthConnectorService.instance(anyLong(), any(Class.class))).thenReturn(lmsOAuthService);

        LmsOAuthService<?> result = lmsOAuthServiceManagerImpl.getLmsOAuthService(1L);

        assertEquals(lmsOAuthService, result);
        verify(lmsOAuthConnectorService).instance(1L, LmsOAuthService.class);
    }

    @Test
    public void testGetLmsOAuthServiceByIdFailure() throws TerracottaConnectorException {
        when(lmsOAuthConnectorService.instance(anyLong(), any(Class.class))).thenThrow(new TerracottaConnectorException("Error"));

        assertThrows(TerracottaConnectorException.class, () -> lmsOAuthServiceManagerImpl.getLmsOAuthService(1L));
    }

}