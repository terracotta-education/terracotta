package edu.iu.terracotta.service.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.model.dto.ConfigurationDto;

public interface ConfigurationService {

    ConfigurationDto getConfigurations(SecuredInfo securedInfo);

}
