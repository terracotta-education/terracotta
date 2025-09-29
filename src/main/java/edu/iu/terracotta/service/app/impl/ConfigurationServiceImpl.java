package edu.iu.terracotta.service.app.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.model.dto.ConfigurationDto;
import edu.iu.terracotta.dao.model.enums.FeatureType;
import edu.iu.terracotta.service.app.ConfigurationService;
import edu.iu.terracotta.service.app.FeatureService;

@Service
public class ConfigurationServiceImpl implements ConfigurationService {

    @Autowired private FeatureService featureService;

    @Value("${experiment.export.enabled:true}")
    private boolean experimentExportEnabled;

    @Override
    public ConfigurationDto getConfigurations(long platformDeploymentKeyId) {
        return ConfigurationDto.builder()
            .experimentExportEnabled(experimentExportEnabled)
            .messagingEnabled(featureService.isFeatureEnabled(FeatureType.MESSAGING, platformDeploymentKeyId))
            .build();
    }

}
