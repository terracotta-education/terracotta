package edu.iu.terracotta.connectors.generic.service.connector.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.canvas.service.api.impl.CanvasApiClientImpl;
import edu.iu.terracotta.connectors.canvas.service.lms.impl.CanvasLmsOAuthServiceImpl;
import edu.iu.terracotta.connectors.canvas.service.lms.impl.CanvasLmsUtilsImpl;
import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ConnectorServiceImplTest extends BaseTest {

    // named distinctly from BaseServiceTest's inherited canvasLmsOAuthService field (same type,
    // CanvasLmsOAuthServiceImpl): a same-name local @Mock here would shadow it and be a latent
    // @InjectMocks ambiguity risk if this class ever gained a constructor/field dependency on that
    // type directly (see the @InjectMocks pitfall note in BaseServiceTest)
    @Mock private CanvasLmsOAuthServiceImpl canvasClientOAuthService;
    @Mock private CanvasLmsUtilsImpl canvasClientLmsUtils;

    // no other ApplicationContext mock exists anywhere in the BaseModelTest/BaseRepositoryTest/
    // BaseServiceTest hierarchy, so @InjectMocks constructor injection can wire this unambiguously
    @Mock private ApplicationContext applicationContext;

    @Mock private ApiClient brightspaceApiClient;
    @Mock private ApiClient oneEdTechApiClient;
    @Mock private ApiClient genericApiClient;

    @InjectMocks private ConnectorServiceImpl<ApiClient> connectorService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();

        Map<LmsConnector, Map<String, Object>> connectorServiceMap = new HashMap<>();
        Map<String, Object> canvasMap = new HashMap<>();
        canvasMap.put(ApiClient.class.getSimpleName(), new CanvasApiClientImpl(canvasClientOAuthService, canvasClientLmsUtils));
        connectorServiceMap.put(LmsConnector.CANVAS, canvasMap);
        ReflectionTestUtils.setField(connectorService, "connectorServiceMap", connectorServiceMap);
    }

    // registers an additional connector-type entry into the already-injected map so each
    // dispatch test can exercise a distinct LmsConnector branch without disturbing the
    // CANVAS-only baseline other tests (e.g. the "not found" tests) rely on
    @SuppressWarnings("unchecked")
    private void registerConnector(LmsConnector lmsConnector, ApiClient apiClient) {
        Map<LmsConnector, Map<String, Object>> connectorServiceMap =
            (Map<LmsConnector, Map<String, Object>>) ReflectionTestUtils.getField(connectorService, "connectorServiceMap");
        Map<String, Object> typeMap = new HashMap<>();
        typeMap.put(ApiClient.class.getSimpleName(), apiClient);
        connectorServiceMap.put(lmsConnector, typeMap);
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

    @Test
    public void testInstanceByPlatformDeploymentBrightspaceDispatch() throws TerracottaConnectorException {
        registerConnector(LmsConnector.BRIGHTSPACE, brightspaceApiClient);
        when(platformDeployment.getLmsConnector()).thenReturn(LmsConnector.BRIGHTSPACE);

        ApiClient ret = connectorService.instance(platformDeployment, ApiClient.class);

        assertEquals(brightspaceApiClient, ret);
    }

    @Test
    public void testInstanceByPlatformDeploymentOneEdTechDispatch() throws TerracottaConnectorException {
        registerConnector(LmsConnector.ONE_ED_TECH, oneEdTechApiClient);
        when(platformDeployment.getLmsConnector()).thenReturn(LmsConnector.ONE_ED_TECH);

        ApiClient ret = connectorService.instance(platformDeployment, ApiClient.class);

        assertEquals(oneEdTechApiClient, ret);
    }

    @Test
    public void testInstanceByPlatformDeploymentGenericDispatch() throws TerracottaConnectorException {
        registerConnector(LmsConnector.GENERIC, genericApiClient);
        when(platformDeployment.getLmsConnector()).thenReturn(LmsConnector.GENERIC);

        ApiClient ret = connectorService.instance(platformDeployment, ApiClient.class);

        assertEquals(genericApiClient, ret);
    }

    @Test
    public void testInstanceByPlatformDeploymentCanvasDispatch() throws TerracottaConnectorException {
        when(platformDeployment.getLmsConnector()).thenReturn(LmsConnector.CANVAS);

        ApiClient ret = connectorService.instance(platformDeployment, ApiClient.class);

        assertNotNull(ret);
    }

    @Test
    public void testInstanceServiceTypeNotFoundInConnectorMapWrapsCause() {
        // CANVAS is registered in the map (see beforeEach) but only under the "ApiClient" key,
        // so requesting a different service type should fall into the internal "not found in
        // map" branch (as opposed to a missing-connector NPE) and still wrap it as a cause
        when(platformDeployment.getLmsConnector()).thenReturn(LmsConnector.CANVAS);

        TerracottaConnectorException exception = assertThrows(
            TerracottaConnectorException.class,
            () -> connectorService.instance(platformDeployment, ApiJwtService.class)
        );

        assertNotNull(exception.getCause());
    }

    @Test
    public void testCreateConnectorMapRegistersBeansFoundByApplicationContext() {
        // CanvasApiClientImpl is @TerracottaConnector(CANVAS) and implements ApiClient, which is
        // itself @TerracottaConnector(GENERIC) - a real class (not a mock) is used here since
        // createConnectorMap reflects on the bean's actual declared type/interfaces.
        when(applicationContext.getBeanNamesForAnnotation(TerracottaConnector.class)).thenReturn(new String[] {"canvasApiClientImpl"});
        doReturn(CanvasApiClientImpl.class).when(applicationContext).getType("canvasApiClientImpl");
        when(applicationContext.getBean("canvasApiClientImpl")).thenReturn(genericApiClient);

        connectorService.createConnectorMap();

        @SuppressWarnings("unchecked")
        Map<LmsConnector, Map<String, Object>> populatedMap =
            (Map<LmsConnector, Map<String, Object>>) ReflectionTestUtils.getField(connectorService, "connectorServiceMap");

        assertNotNull(populatedMap);
        assertTrue(populatedMap.containsKey(LmsConnector.CANVAS));
        assertEquals(genericApiClient, populatedMap.get(LmsConnector.CANVAS).get(ApiClient.class.getSimpleName()));
    }

    @Test
    public void testCreateConnectorMapSkipsBeanWithUnresolvableType() {
        when(applicationContext.getBeanNamesForAnnotation(TerracottaConnector.class)).thenReturn(new String[] {"someLazyBean"});
        doReturn(null).when(applicationContext).getType("someLazyBean");

        connectorService.createConnectorMap();

        @SuppressWarnings("unchecked")
        Map<LmsConnector, Map<String, Object>> populatedMap =
            (Map<LmsConnector, Map<String, Object>>) ReflectionTestUtils.getField(connectorService, "connectorServiceMap");

        assertTrue(populatedMap.get(LmsConnector.CANVAS).isEmpty());
    }

}