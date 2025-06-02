package edu.iu.terracotta.service.app;

import edu.iu.terracotta.dao.model.dto.ConfigurationDto;

public interface ConfigurationService {

    ConfigurationDto getConfigurations(long platformDeploymentKeyId);

}
