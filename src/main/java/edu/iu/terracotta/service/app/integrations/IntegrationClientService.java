package edu.iu.terracotta.service.app.integrations;

import java.util.List;

import edu.iu.terracotta.dao.entity.integrations.IntegrationClient;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.dao.model.dto.integrations.IntegrationClientDto;

public interface IntegrationClientService {

    List<IntegrationClient> getAll();
    List<IntegrationClientDto> toDto(List<IntegrationClient> integrationClients, String localUrl);
    IntegrationClientDto toDto(IntegrationClient integrationClient, String localUrl);
    IntegrationClient fromDto(IntegrationClientDto integrationClientDto) throws IntegrationClientNotFoundException;

}
