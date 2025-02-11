package edu.iu.terracotta.connectors.generic.service.connector.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.canvas.service.api.impl.CanvasApiClientImpl;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ConnectorServiceImplTest extends BaseTest {

    @InjectMocks private ConnectorServiceImpl<ApiClient> connectorService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();

        Map<LmsConnector, Map<String, Object>> connectorServiceMap = new HashMap<>();
        Map<String, Object> canvasMap = new HashMap<>();
        canvasMap.put(ApiClient.class.getSimpleName(), new CanvasApiClientImpl());
        connectorServiceMap.put(LmsConnector.CANVAS, canvasMap);
        ReflectionTestUtils.setField(connectorService, "connectorServiceMap", connectorServiceMap);
    }

    @Test
    public void testInstanceByIdSuccess() throws TerracottaConnectorException {
        ApiClient ret = connectorService.instance(1L, ApiClient.class);

        assertNotNull(ret);
        verify(platformDeploymentRepository).findById(1L);
    }

    @Test
    public void testInstanceByIdNotFound() {
        when(platformDeploymentRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(TerracottaConnectorException.class, () -> connectorService.instance(1L, ApiClient.class));
    }

    @Test
    public void testInstanceByOptionalSuccess() throws TerracottaConnectorException {
        ApiClient ret = connectorService.instance(Optional.of(platformDeployment), ApiClient.class);

        assertNotNull(ret);
    }

    @Test
    public void testInstanceByOptionalNotFound() {
        assertThrows(TerracottaConnectorException.class, () -> connectorService.instance(Optional.empty(), ApiClient.class));
    }

    @Test
    public void testInstanceByPlatformDeploymentSuccess() throws TerracottaConnectorException {
        ApiClient ret = connectorService.instance(platformDeployment, ApiClient.class);

        assertNotNull(ret);
    }

    @Test
    public void testInstanceByPlatformDeploymentNotFound() {
        when(platformDeployment.getLmsConnector()).thenReturn(LmsConnector.GENERIC);

        assertThrows(TerracottaConnectorException.class, () -> connectorService.instance(platformDeployment, ApiClient.class));
    }

}