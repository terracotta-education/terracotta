package edu.iu.terracotta.connectors.brightspace.configuration.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class BrightspaceConfigurationServiceImplTest {

    private BrightspaceConfigurationServiceImpl brightspaceConfigurationService;

    @BeforeEach
    public void beforeEach() {
        brightspaceConfigurationService = new BrightspaceConfigurationServiceImpl();
    }

    @Test
    public void testIsApiRequestLogEnabledDefaultsToFalse() {
        // no @Value injection happens in a plain construction (no Spring context), matching the
        // "brightspace.api.request.log.enabled:false" default the field falls back to
        assertFalse(brightspaceConfigurationService.isApiRequestLogEnabled());
    }

    @Test
    public void testIsApiRequestLogEnabledReturnsTrueWhenConfigured() {
        ReflectionTestUtils.setField(brightspaceConfigurationService, "apiRequestLogEnabled", true);

        assertTrue(brightspaceConfigurationService.isApiRequestLogEnabled());
    }

    @Test
    public void testIsApiRequestLogEnabledReturnsFalseWhenExplicitlyDisabled() {
        ReflectionTestUtils.setField(brightspaceConfigurationService, "apiRequestLogEnabled", false);

        assertFalse(brightspaceConfigurationService.isApiRequestLogEnabled());
    }

}
