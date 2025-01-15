package edu.iu.terracotta.service.app.integrations.impl;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.exceptions.integrations.IntegrationConfigurationNotFoundException;
import edu.iu.terracotta.exceptions.integrations.IntegrationConfigurationNotMatchingException;
import edu.iu.terracotta.exceptions.integrations.IntegrationNotFoundException;
import edu.iu.terracotta.exceptions.integrations.IntegrationNotMatchingException;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.integrations.Integration;
import edu.iu.terracotta.model.app.integrations.dto.IntegrationDto;
import edu.iu.terracotta.repository.integrations.IntegrationRepository;
import edu.iu.terracotta.service.app.integrations.IntegrationClientService;
import edu.iu.terracotta.service.app.integrations.IntegrationConfigurationService;
import edu.iu.terracotta.service.app.integrations.IntegrationLaunchParameterService;
import edu.iu.terracotta.service.app.integrations.IntegrationService;

@Service
@SuppressWarnings({"PMD.LambdaCanBeMethodReference"})
public class IntegrationServiceImpl implements IntegrationService {

    @Autowired private IntegrationRepository integrationRepository;
    @Autowired private IntegrationClientService integrationClientService;
    @Autowired private IntegrationConfigurationService integrationConfigurationService;
    @Autowired private IntegrationLaunchParameterService integrationLaunchParameterService;

    @Override
    public Integration create(Question question, UUID clientUuid) throws IntegrationClientNotFoundException {
        // create the integration
        Integration integration = integrationRepository.save(
            Integration.builder()
                .question(question)
                .build()
        );

        // create the configuration
        integrationConfigurationService.create(integration, clientUuid);

        return integration;
    }

    @Override
    public Integration update(IntegrationDto integrationDto, Question question) throws IntegrationNotFoundException, IntegrationNotMatchingException, IntegrationConfigurationNotFoundException, IntegrationConfigurationNotMatchingException, IntegrationClientNotFoundException {
        Integration integration = integrationRepository.findById(question.getIntegration().getId())
            .orElseThrow(() -> new IntegrationNotFoundException(String.format("No integration with ID: [%s] found.", question.getIntegration().getId())));

        if (!integration.getQuestion().getQuestionId().equals(question.getQuestionId())) {
            throw new IntegrationNotMatchingException(String.format("Integration ID: [%s] not associated with question ID: [%s]", integration.getId(), question.getQuestionId()));
        }

        integration = integrationRepository.save(
            fromDto(integrationDto, integration)
        );

        integration.setConfiguration(
            integrationConfigurationService.update(integrationDto.getConfiguration(), integration)
        );

        return integration;
    }

    @Override
    public void delete(Integration integration) {
        if (integration == null) {
            return;
        }

        integrationConfigurationService.delete(integration.getConfiguration());
        integrationRepository.deleteById(integration.getId());
    }

    @Override
    public void duplicate(Integration integration, Question question) {
        // duplicate integration
        Integration newIntegration = integrationRepository.saveAndFlush(Integration.builder()
            .question(question)
            .build()
        );

        // duplicate integration configuration
        integrationConfigurationService.duplicate(integration.getConfiguration(), newIntegration);
        integrationRepository.save(integration);

        question.setIntegration(newIntegration);
    }

    @Override
    public Integration findByUuid(UUID uuid) throws IntegrationNotFoundException {
        return integrationRepository.findByUuid(uuid)
            .orElseThrow(() -> new IntegrationNotFoundException(String.format("No integration with UUID: [%s] found.", uuid)));
    }

    @Override
    public List<IntegrationDto> toDto(List<Integration> integrations) {
        if (CollectionUtils.isEmpty(integrations)) {
            return Collections.emptyList();
        }

        return integrations.stream()
            .map(integration -> toDto(integration))
            .toList();
    }

    @Override
    public IntegrationDto toDto(Integration integration) {
        if (integration == null) {
            return null;
        }

        return IntegrationDto.builder()
            .clients(integrationClientService.toDto(integrationClientService.getAll(), integration.getLocalUrl()))
            .configuration(integrationConfigurationService.toDto(integration.getConfiguration()))
            .id(integration.getUuid())
            .previewUrl(integrationLaunchParameterService.buildPreviewQueryString(integration))
            .questionId(integration.getQuestion().getQuestionId())
            .build();
    }

    @Override
    public Integration fromDto(IntegrationDto integrationDto, Integration integration) {
        if (integration == null) {
            integration = Integration.builder().build();
        }

        if (integrationDto == null) {
            return integration;
        }

        return integration;
    }

}
