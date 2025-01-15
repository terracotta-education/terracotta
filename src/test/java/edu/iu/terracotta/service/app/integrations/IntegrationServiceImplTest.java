package edu.iu.terracotta.service.app.integrations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.integrations.Integration;
import edu.iu.terracotta.dao.entity.integrations.IntegrationConfiguration;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenInvalidException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.dao.model.dto.integrations.IntegrationConfigurationDto;
import edu.iu.terracotta.dao.model.dto.integrations.IntegrationDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.service.app.integrations.impl.IntegrationServiceImpl;

public class IntegrationServiceImplTest extends BaseTest {

    @InjectMocks private IntegrationServiceImpl integrationService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        clearInvocations(
            integration,
            integrationRepository,
            integrationConfigurationService,
            question
        );
        setup();
    }

    @Test
    void testCreate() throws IntegrationClientNotFoundException {
        Integration ret = integrationService.create(question, UUID.randomUUID());

        assertNotNull(ret);
        verify(integrationRepository).save(any(Integration.class));
        verify(integrationConfigurationService).create(any(Integration.class), any(UUID.class));
    }

    @Test
    void testUpdate()
        throws IntegrationNotFoundException, IntegrationNotMatchingException, IntegrationConfigurationNotFoundException, IntegrationConfigurationNotMatchingException, IntegrationClientNotFoundException {
        Integration ret = integrationService.update(integrationDto, question);

        assertNotNull(ret);
        verify(integrationRepository).save(any(Integration.class));
        verify(integration).setConfiguration(any(IntegrationConfiguration.class));
        verify(integrationConfigurationService).update(any(IntegrationConfigurationDto.class), any(Integration.class));
    }

    @Test
    public void testUpdateIntegrationNotFoundException() throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException {
        when(integrationRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(IntegrationNotFoundException.class, () -> { integrationService.update(integrationDto, question); });
    }

    @Test
    public void testUpdateIntegrationNotMatchingException() throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException {
        when(question.getQuestionId()).thenReturn(1l, 2l);

        assertThrows(IntegrationNotMatchingException.class, () -> { integrationService.update(integrationDto, question); });
    }

    @Test
    void testDelete() {
        integrationService.delete(integration);

        verify(integrationConfigurationService).delete(any(IntegrationConfiguration.class));
        verify(integrationRepository).deleteById(anyLong());
    }

    @Test
    void testDeleteIntegrationNUll() {
        integrationService.delete(null);

        verify(integrationConfigurationService, never()).delete(any(IntegrationConfiguration.class));
        verify(integrationRepository, never()).deleteById(anyLong());
    }

    @Test
    void testDuplicate() {
        integrationService.duplicate(integration, question);

        verify(integrationRepository).saveAndFlush(any(Integration.class));
        verify(integrationRepository).save(any(Integration.class));
        verify(integrationConfigurationService).duplicate(any(IntegrationConfiguration.class), any(Integration.class));
        verify(question).setIntegration(any(Integration.class));
    }

    @Test
    void testFindByUuid() throws IntegrationNotFoundException {
        Integration ret = integrationService.findByUuid(UUID.randomUUID());

        assertNotNull(ret);
    }

    @Test
    public void testFindByUuidIntegrationNotFoundException() throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException {
        when(integrationRepository.findByUuid(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(IntegrationNotFoundException.class, () -> { integrationService.findByUuid(any(UUID.class)); });
    }

    @Test
    void testToDtoList() {
        List<IntegrationDto> ret = integrationService.toDto(Collections.singletonList(integration));

        assertNotNull(ret);
        assertEquals(1, ret.size());
    }

    @Test
    void testToDtoListEmptyInput() {
        List<IntegrationDto> ret = integrationService.toDto(Collections.emptyList());

        assertNotNull(ret);
        assertEquals(0, ret.size());
    }

    @Test
    void testToDto() {
        IntegrationDto ret = integrationService.toDto(integration);

        assertNotNull(ret);
    }

    @Test
    void testFromDto() {
        Integration ret = integrationService.fromDto(integrationDto, integration);

        assertNotNull(ret);
    }

    @Test
    void testFromDtoDtoNull() {
        Integration ret = integrationService.fromDto(null, integration);

        assertNotNull(ret);
    }

    @Test
    void testFromDtoNull() {
        Integration ret = integrationService.fromDto(integrationDto, null);

        assertNotNull(ret);
    }

}
