package edu.iu.terracotta.connectors.brightspace.configuration.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.brightspace.configuration.BrightspaceConfigurationService;
import lombok.Getter;

@Getter
@Service
public class BrightspaceConfigurationServiceImpl implements BrightspaceConfigurationService {

    @Value("${brightspace.api.request.log.enabled:false}")
    private boolean apiRequestLogEnabled;

}
