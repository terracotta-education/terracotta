package edu.iu.terracotta.service.app.integrations.impl;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.dao.entity.Question;
import edu.iu.terracotta.dao.entity.integrations.Integration;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationUrlIframeInvalidException;
import edu.iu.terracotta.dao.model.dto.integrations.IntegrationDto;
import edu.iu.terracotta.dao.repository.integrations.IntegrationRepository;
import edu.iu.terracotta.service.app.integrations.IntegrationClientService;
import edu.iu.terracotta.service.app.integrations.IntegrationConfigurationService;
import edu.iu.terracotta.service.app.integrations.IntegrationLaunchParameterService;
import edu.iu.terracotta.service.app.integrations.IntegrationService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.LambdaCanBeMethodReference", "PMD.GuardLogStatement", "PMD.PreserveStackTrace"})
public class IntegrationServiceImpl implements IntegrationService {

    @Autowired private IntegrationRepository integrationRepository;
    @Autowired private PlatformDeploymentRepository platformDeploymentRepository;
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

    @Override
    public void validateIntegrationUrlIframe(String url, SecuredInfo securedInfo) throws IntegrationUrlIframeInvalidException {
        PlatformDeployment platformDeployment = platformDeploymentRepository.findByKeyId(securedInfo.getPlatformDeploymentId())
            .orElseThrow(() -> new IntegrationUrlIframeInvalidException("Platform deployment not found for iframe validation."));

        ResponseEntity<String> response;

        try {
            // Use HEAD request to get headers without downloading full content
            response = new RestTemplate().exchange(
                url,
                HttpMethod.HEAD,
                null,
                String.class
            );
        } catch (Exception e) {
            try {
                response = new RestTemplate().exchange(
                url,
                HttpMethod.GET,
                null,
                String.class
            );
            } catch (Exception ex) {
                throw new IntegrationUrlIframeInvalidException(
                    String.format(
                        "Error validating iframe embedding for URL: [%s]. Error: [%s]",
                        url,
                        ex.getMessage()
                    ),
                    ex
                );
            }
        }

        Optional<String> validationErrors = analyzeHeaders(response.getHeaders(), url, platformDeployment.getLocalUrl());

        if (validationErrors.isPresent()) {
            String errorMessage = String.format(
                "The URL: [%s] cannot be embedded in an iframe due to its security headers. Error: [%s]",
                url,
                validationErrors.get()
            );
            log.warn(errorMessage);
            throw new IntegrationUrlIframeInvalidException(errorMessage);
        }
    }

    private Optional<String> analyzeHeaders(HttpHeaders headers, String url, String requestingDomain) {
        String xFrameOptions = headers.getFirst("X-Frame-Options");
        String csp = headers.getFirst("Content-Security-Policy");

        // CSP takes precedence over X-Frame-Options if both are present
        if (Strings.CI.contains(csp, "frame-ancestors")) {
            return analyzeCspFrameAncestors(csp, requestingDomain);
        }

        // Fall back to X-Frame-Options
        if (StringUtils.isNotBlank(xFrameOptions)) {
            return analyzeXFrameOptions(xFrameOptions, url, requestingDomain);
        }

        // No restrictions found - iframe embedding is allowed
        return Optional.empty();
    }

    private Optional<String> analyzeXFrameOptions(String xFrameOptions, String url, String requestingDomain) {
        String value = xFrameOptions.trim().toUpperCase();

        if (Strings.CI.equals(value, "DENY")) {
            return Optional.of("X-Frame-Options: DENY - iframe embedding is blocked");
        }

        if (Strings.CI.equals(value, "SAMEORIGIN")) {
            if (StringUtils.isNotBlank(requestingDomain)) {
                try {
                    boolean sameOrigin = requestingDomain.equalsIgnoreCase(
                        URI.create(url).toURL().getHost()
                    );

                    return sameOrigin ? Optional.empty() : Optional.of("X-Frame-Options: SAMEORIGIN - different origin, embedding blocked");
                } catch (Exception e) {
                    return Optional.empty();
                }
            }

            return Optional.of("X-Frame-Options: SAMEORIGIN - requesting domain not provided for comparison");
        }

        if (Strings.CI.startsWith(value, "ALLOW-FROM")) {
            String allowedOrigin = value.substring("ALLOW-FROM".length()).trim();

            if (Strings.CI.contains(allowedOrigin, requestingDomain)) {
                return Optional.empty();
            }

            return Optional.of("X-Frame-Options: ALLOW-FROM does not match requesting domain");
        }

        // Unknown X-Frame-Options value
        return Optional.of(String.format("Unknown X-Frame-Options value: [%s]", xFrameOptions));
    }

    private Optional<String> analyzeCspFrameAncestors(String csp, String requestingDomain) {
        // Parse frame-ancestors directive from CSP
        String[] directives = csp.split(";");

        for (String directive : directives) {
            directive = directive.trim();

            if (directive.startsWith("frame-ancestors")) {
                String frameAncestors = directive.substring("frame-ancestors".length()).trim();

                if (Strings.CI.equals(frameAncestors, "'none'")) {
                    return Optional.of("CSP frame-ancestors: 'none' - iframe embedding is blocked");
                }

                if (Strings.CI.equals(frameAncestors, "*")) {
                    return Optional.empty();
                }

                if (Strings.CI.equals(frameAncestors, "'self'")) {
                    if (requestingDomain != null) {
                        // Would need to check if same origin - simplified for this example
                        return Optional.of("CSP frame-ancestors: 'self' - only same-origin embedding allowed");
                    }
                }

                // Check if requesting domain is in the allowed list
                if (requestingDomain != null && frameAncestors.contains(requestingDomain)) {
                    return Optional.empty();
                }

                return Optional.of("CSP frame-ancestors: requesting domain not in allowed list");
            }
        }

        // frame-ancestors directive not found in CSP; allow embedding
        return Optional.empty();
    }

}
