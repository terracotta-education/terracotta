package edu.iu.terracotta.service.app.integrations;

import java.util.UUID;

import edu.iu.terracotta.dao.entity.integrations.Integration;
import edu.iu.terracotta.dao.entity.integrations.IntegrationConfiguration;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotMatchingException;
import edu.iu.terracotta.dao.model.dto.integrations.IntegrationConfigurationDto;

public interface IntegrationConfigurationService {

    IntegrationConfiguration create(Integration integration, UUID clientUuid) throws IntegrationClientNotFoundException;
    IntegrationConfiguration update(IntegrationConfigurationDto integrationConfigurationDto, Integration integration) throws IntegrationConfigurationNotFoundException, IntegrationConfigurationNotMatchingException, IntegrationClientNotFoundException;
    void delete(IntegrationConfiguration integrationConfiguration);
    void duplicate(IntegrationConfiguration integrationConfiguration, Integration integration);
    IntegrationConfigurationDto toDto(IntegrationConfiguration integrationConfiguration);
    IntegrationConfiguration fromDto(IntegrationConfigurationDto integrationConfigurationDto, IntegrationConfiguration integrationConfiguration) throws IntegrationClientNotFoundException;

}
