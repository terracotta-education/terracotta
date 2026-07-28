package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.dao.model.dto.ConfigurationDto;
import edu.iu.terracotta.dao.model.enums.FeatureType;
import edu.iu.terracotta.service.app.FeatureService;

public class ConfigurationServiceImplTest extends BaseTest {

    @InjectMocks private ConfigurationServiceImpl configurationService;

    @Mock private FeatureService featureService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        when(securedInfo.getPlatformDeploymentId()).thenReturn(1L);
        when(platformDeploymentRepository.findById(anyLong())).thenReturn(Optional.of(platformDeployment));
        when(platformDeployment.getLmsConnector()).thenReturn(LmsConnector.CANVAS);
    }

    @Test
    public void testGetConfigurations() {
        when(featureService.isFeatureEnabled(FeatureType.MESSAGING, 1L)).thenReturn(true);

        ConfigurationDto dto = configurationService.getConfigurations(securedInfo);

        assertNotNull(dto);
        assertEquals(LmsConnector.CANVAS, dto.getLms());
        assertEquals(LmsConnector.CANVAS.title(), dto.getLmsTitle());
        assertTrue(dto.isMessagingEnabled());
    }

    @Test
    public void testGetConfigurationsMessagingDisabled() {
        when(featureService.isFeatureEnabled(FeatureType.MESSAGING, 1L)).thenReturn(false);

        ConfigurationDto dto = configurationService.getConfigurations(securedInfo);

        assertNotNull(dto);
        assertEquals(false, dto.isMessagingEnabled());
    }

    @Test
    public void testGetConfigurationsPlatformDeploymentNotFound() {
        when(platformDeploymentRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> configurationService.getConfigurations(securedInfo));
    }

}
