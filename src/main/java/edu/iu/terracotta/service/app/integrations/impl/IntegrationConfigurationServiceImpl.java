package edu.iu.terracotta.service.app.integrations.impl;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.entity.integrations.Integration;
import edu.iu.terracotta.dao.entity.integrations.IntegrationClient;
import edu.iu.terracotta.dao.entity.integrations.IntegrationConfiguration;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotMatchingException;
import edu.iu.terracotta.dao.model.dto.integrations.IntegrationConfigurationDto;
import edu.iu.terracotta.dao.repository.integrations.IntegrationClientRepository;
import edu.iu.terracotta.dao.repository.integrations.IntegrationConfigurationRepository;
import edu.iu.terracotta.dao.repository.integrations.IntegrationRepository;
import edu.iu.terracotta.service.app.integrations.IntegrationClientService;
import edu.iu.terracotta.service.app.integrations.IntegrationConfigurationService;

@Service
public class IntegrationConfigurationServiceImpl implements IntegrationConfigurationService {

    @Autowired private IntegrationClientRepository integrationClientRepository;
    @Autowired private IntegrationConfigurationRepository integrationConfigurationRepository;
    @Autowired private IntegrationRepository integrationRepository;
    @Autowired private IntegrationClientService integrationClientService;

    @Override
    public IntegrationConfiguration create(Integration integration, UUID clientUuid) throws IntegrationClientNotFoundException {
        IntegrationClient integrationClient = integrationClientRepository.findByUuid(clientUuid)
            .orElseThrow(() -> new IntegrationClientNotFoundException(String.format("No integration client with UUID: [%s] found.", clientUuid)));

        IntegrationConfiguration integrationConfiguration = integrationConfigurationRepository.save(
            IntegrationConfiguration.builder()
                .client(integrationClient)
                .integration(integration)
                .build()
        );

        integration.setConfiguration(integrationConfiguration);
        integrationRepository.save(integration);

        return integrationConfiguration;
    }

    @Override
    public IntegrationConfiguration update(IntegrationConfigurationDto integrationConfigurationDto, Integration integration) throws IntegrationConfigurationNotFoundException, IntegrationConfigurationNotMatchingException, IntegrationClientNotFoundException {
        if (!integration.getConfiguration().getUuid().equals(integrationConfigurationDto.getId())) {
            throw new IntegrationConfigurationNotMatchingException(String.format("Integration configuration with UUID: [%s] does not match the given integration with ID: [%s].", integrationConfigurationDto.getId(), integration.getId()));
        }

        IntegrationConfiguration integrationConfiguration = integrationConfigurationRepository.findById(integration.getConfiguration().getId())
            .orElseThrow(() -> new IntegrationConfigurationNotFoundException(String.format("No integration configuration with ID: [%s] found.", integration.getConfiguration().getId())));

        return integrationConfigurationRepository.save(
            fromDto(integrationConfigurationDto, integrationConfiguration)
        );
    }

    @Override
    public void delete(IntegrationConfiguration integrationConfiguration) {
        if (integrationConfiguration == null) {
            return;
        }

        integrationConfigurationRepository.deleteById(integrationConfiguration.getId());
    }

    @Override
    public void duplicate(IntegrationConfiguration integrationConfiguration, Integration integration) {
        IntegrationConfiguration newIntegrationConfiguration = integrationConfigurationRepository.saveAndFlush(
            IntegrationConfiguration.builder()
                .client(integrationConfiguration.getClient())
                .integration(integration)
                .launchUrl(integrationConfiguration.getLaunchUrl())
                .build()
        );

        integration.setConfiguration(newIntegrationConfiguration);
    }

    @Override
    public IntegrationConfigurationDto toDto(IntegrationConfiguration integrationConfiguration) {
        if (integrationConfiguration == null) {
            return null;
        }

        return IntegrationConfigurationDto.builder()
            .client(
                integrationClientService.toDto(
                    integrationConfiguration.getClient(),
                    integrationConfiguration.getIntegration().getLocalUrl()
                )
            )
            .id(integrationConfiguration.getUuid())
            .launchUrl(integrationConfiguration.getLaunchUrl())
            .build();
    }

    @Override
    public IntegrationConfiguration fromDto(IntegrationConfigurationDto integrationConfigurationDto, IntegrationConfiguration integrationConfiguration) throws IntegrationClientNotFoundException {
        if (integrationConfiguration == null) {
            integrationConfiguration = IntegrationConfiguration.builder().build();
        }

        if (integrationConfigurationDto == null) {
            return integrationConfiguration;
        }

        integrationConfiguration.setClient(integrationClientService.fromDto(integrationConfigurationDto.getClient()));
        integrationConfiguration.setLaunchUrl(integrationConfigurationDto.getLaunchUrl());

        return integrationConfiguration;
    }

}
