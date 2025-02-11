package edu.iu.terracotta.service.app.integrations;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.integrations.IntegrationConfiguration;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotMatchingException;
import edu.iu.terracotta.dao.model.dto.integrations.IntegrationConfigurationDto;
import edu.iu.terracotta.service.app.integrations.impl.IntegrationConfigurationServiceImpl;

public class IntegrationConfigurationServiceImplTest extends BaseTest {

    @InjectMocks private IntegrationConfigurationServiceImpl integrationConfigurationService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        clearInvocations(integrationConfigurationRepository);
        setup();
    }

    @Test
    void testCreate() throws IntegrationClientNotFoundException {
        IntegrationConfiguration ret = integrationConfigurationService.create(integration, UUID.randomUUID());

        assertNotNull(ret);
    }

    @Test
    public void testCreateIntegrationClientNotFoundException() throws IntegrationConfigurationNotMatchingException {
        when(integrationClientRepository.findByUuid(any(UUID.class))).thenReturn(Optional.empty());
        assertThrows(IntegrationClientNotFoundException.class, () -> { integrationConfigurationService.create(integration, UUID.randomUUID()); });
    }

    @Test
    void testUpdate() throws IntegrationConfigurationNotFoundException, IntegrationConfigurationNotMatchingException, IntegrationClientNotFoundException {
        UUID uuid = UUID.randomUUID();
        when(integrationConfiguration.getUuid()).thenReturn(uuid);
        when(integrationConfigurationDto.getId()).thenReturn(uuid);

        IntegrationConfiguration ret = integrationConfigurationService.update(integrationConfigurationDto, integration);

        assertNotNull(ret);
    }

    @Test
    public void testUpdateIntegrationConfigurationNotMatchingException() throws IntegrationConfigurationNotMatchingException {
        assertThrows(IntegrationConfigurationNotMatchingException.class, () -> { integrationConfigurationService.update(integrationConfigurationDto, integration); });
    }

    @Test
    public void testUpdateIntegrationConfigurationNotFoundException() throws IntegrationConfigurationNotFoundException {
        UUID uuid = UUID.randomUUID();
        when(integrationConfiguration.getUuid()).thenReturn(uuid);
        when(integrationConfigurationDto.getId()).thenReturn(uuid);
        when(integrationConfigurationRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(IntegrationConfigurationNotFoundException.class, () -> { integrationConfigurationService.update(integrationConfigurationDto, integration); });
    }

    @Test
    void testDelete() {
        integrationConfigurationService.delete(integrationConfiguration);

        verify(integrationConfigurationRepository).deleteById(anyLong());
    }

    @Test
    void testDeleteNull() {
        integrationConfigurationService.delete(null);

        verify(integrationConfigurationRepository, never()).deleteById(anyLong());
    }

    @Test
    void testDuplicate() {
        integrationConfigurationService.duplicate(integrationConfiguration, integration);

        verify(integrationConfigurationRepository).saveAndFlush(any(IntegrationConfiguration.class));
        verify(integration).setConfiguration(any(IntegrationConfiguration.class));
    }

    @Test
    void testToDto() {
        IntegrationConfigurationDto ret = integrationConfigurationService.toDto(integrationConfiguration);

        assertNotNull(ret);
    }

    @Test
    void testToDtoNull() {
        IntegrationConfigurationDto ret = integrationConfigurationService.toDto(null);

        assertNull(ret);
    }

    @Test
    void testFromDto() throws IntegrationClientNotFoundException {
        IntegrationConfiguration ret = integrationConfigurationService.fromDto(integrationConfigurationDto, integrationConfiguration);

        assertNotNull(ret);
    }

    @Test
    void testFromDtoNull() throws IntegrationClientNotFoundException {
        IntegrationConfiguration ret = integrationConfigurationService.fromDto(integrationConfigurationDto, null);

        assertNotNull(ret);
    }

    @Test
    void testFromDtoDtoNull() throws IntegrationClientNotFoundException {
        IntegrationConfiguration ret = integrationConfigurationService.fromDto(null, integrationConfiguration);

        assertNotNull(ret);
    }

}
