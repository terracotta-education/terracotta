package edu.iu.terracotta.connectors.generic.service.lti;

import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.ToolRegistrationDto;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;

public interface RegistrationService {

    String callDynamicRegistration(String token, ToolRegistrationDto toolRegistrationDto, String endpoint) throws ConnectionException;

}
