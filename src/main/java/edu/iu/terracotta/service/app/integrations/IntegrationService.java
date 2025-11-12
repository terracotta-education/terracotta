package edu.iu.terracotta.service.app.integrations;

import java.util.List;
import java.util.UUID;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.Question;
import edu.iu.terracotta.dao.entity.integrations.Integration;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationUrlIframeInvalidException;
import edu.iu.terracotta.dao.model.dto.integrations.IntegrationDto;

public interface IntegrationService {

    Integration create(Question question, UUID clientUuid) throws IntegrationClientNotFoundException;
    Integration update(IntegrationDto integrationDto, Question question)
        throws IntegrationNotFoundException, IntegrationNotMatchingException, IntegrationConfigurationNotFoundException, IntegrationConfigurationNotMatchingException, IntegrationClientNotFoundException;
    void delete(Integration integration);
    void duplicate(Integration integration, Question question);
    Integration findByUuid(UUID uuid) throws IntegrationNotFoundException;
    List<IntegrationDto> toDto(List<Integration> integrations);
    IntegrationDto toDto(Integration integration);
    Integration fromDto(IntegrationDto integrationDto, Integration integration);
    void validateIntegrationUrlIframe(String url, SecuredInfo securedInfo) throws IntegrationUrlIframeInvalidException;

}
