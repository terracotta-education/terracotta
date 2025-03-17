package edu.iu.terracotta.service.app.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.model.dto.ConfigurationDto;
import edu.iu.terracotta.service.app.ConfigurationService;

@Service
public class ConfigurationServiceImpl implements ConfigurationService {

    @Value("${experiment.export.enabled:true}")
    private boolean experimentExportEnabled;

    @Override
    public ConfigurationDto getConfigurations() {
        return ConfigurationDto.builder()
            .experimentExportEnabled(experimentExportEnabled)
            .build();
    }

}
