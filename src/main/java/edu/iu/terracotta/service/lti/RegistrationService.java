package edu.iu.terracotta.service.lti;

import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.model.lti.dto.ToolRegistrationDTO;

public interface RegistrationService {

    String callDynamicRegistration(String token, ToolRegistrationDTO toolRegistrationDTO, String endpoint) throws ConnectionException;

}
