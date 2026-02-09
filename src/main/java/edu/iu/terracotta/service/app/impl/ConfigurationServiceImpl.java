package edu.iu.terracotta.service.app.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.dao.model.dto.ConfigurationDto;
import edu.iu.terracotta.dao.model.enums.FeatureType;
import edu.iu.terracotta.service.app.ConfigurationService;
import edu.iu.terracotta.service.app.FeatureService;

@Service
public class ConfigurationServiceImpl implements ConfigurationService {

    @Autowired private FeatureService featureService;
    @Autowired private PlatformDeploymentRepository platformDeploymentRepository;

    @Value("${experiment.export.enabled:true}")
    private boolean experimentExportEnabled;

    @Override
    public ConfigurationDto getConfigurations(SecuredInfo securedInfo) {
        PlatformDeployment platformDeployment = platformDeploymentRepository.findById(securedInfo.getPlatformDeploymentId())
            .orElseThrow(() -> new RuntimeException(String.format("Platform deployment not found for ID: [%s]", securedInfo.getPlatformDeploymentId())));

        return ConfigurationDto.builder()
            .experimentExportEnabled(experimentExportEnabled)
            .lms(platformDeployment.getLmsConnector())
            .lmsTitle(platformDeployment.getLmsConnector().title())
            .messagingEnabled(featureService.isFeatureEnabled(FeatureType.MESSAGING, securedInfo.getPlatformDeploymentId()))
            .build();
    }

}
