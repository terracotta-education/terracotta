package edu.iu.terracotta.connectors.generic.service.lms.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.lms.LmsOAuthService;

public class LmsOAuthServiceManagerImplTest extends BaseTest {

    private LmsOAuthServiceManagerImpl lmsOAuthServiceManager;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();

        // constructed manually rather than via @InjectMocks: the constructor takes a raw
        // ConnectorService<LmsOAuthService<ApiToken>>, and other ConnectorService<?> mocks of
        // different generic parameterizations exist in BaseServiceTest, which would make
        // @InjectMocks resolution by (erased) type ambiguous.
        lmsOAuthServiceManager = new LmsOAuthServiceManagerImpl(lmsOAuthConnectorService);
    }

    @Test
    public void testGetLmsOAuthServiceByPlatformDeploymentDelegatesUsingKeyId() throws TerracottaConnectorException {
        when(platformDeployment.getKeyId()).thenReturn(42L);
        when(lmsOAuthConnectorService.instance(42L, LmsOAuthService.class)).thenReturn(lmsOAuthService);

        LmsOAuthService<?> result = lmsOAuthServiceManager.getLmsOAuthService(platformDeployment);

        assertEquals(lmsOAuthService, result);
        verify(lmsOAuthConnectorService).instance(42L, LmsOAuthService.class);
    }

    @Test
    public void testGetLmsOAuthServiceByIdDelegatesDirectly() throws TerracottaConnectorException {
        when(lmsOAuthConnectorService.instance(7L, LmsOAuthService.class)).thenReturn(lmsOAuthService);

        LmsOAuthService<?> result = lmsOAuthServiceManager.getLmsOAuthService(7L);

        assertEquals(lmsOAuthService, result);
        verify(lmsOAuthConnectorService).instance(7L, LmsOAuthService.class);
    }

    @Test
    public void testGetLmsOAuthServiceByPlatformDeploymentPropagatesConnectorException() throws TerracottaConnectorException {
        when(platformDeployment.getKeyId()).thenReturn(1L);
        when(lmsOAuthConnectorService.instance(1L, LmsOAuthService.class)).thenThrow(new TerracottaConnectorException("boom"));

        assertThrows(TerracottaConnectorException.class, () -> lmsOAuthServiceManager.getLmsOAuthService(platformDeployment));
    }

    @Test
    public void testGetLmsOAuthServiceByIdPropagatesConnectorException() throws TerracottaConnectorException {
        when(lmsOAuthConnectorService.instance(9L, LmsOAuthService.class)).thenThrow(new TerracottaConnectorException("boom"));

        assertThrows(TerracottaConnectorException.class, () -> lmsOAuthServiceManager.getLmsOAuthService(9L));
    }

}
