package edu.iu.terracotta.connectors.generic.service.lms;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;

public interface LmsOAuthServiceManager {

    LmsOAuthService<?> getLmsOAuthService(PlatformDeployment platformDeployment) throws TerracottaConnectorException;
    LmsOAuthService<?> getLmsOAuthService(long platformDeploymentId) throws TerracottaConnectorException;

}
