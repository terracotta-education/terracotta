package edu.iu.terracotta.connectors.generic.service.lms.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ApiToken;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.connector.ConnectorService;
import edu.iu.terracotta.connectors.generic.service.lms.LmsOAuthService;
import edu.iu.terracotta.connectors.generic.service.lms.LmsOAuthServiceManager;

@Service
public class LmsOAuthServiceManagerImpl implements LmsOAuthServiceManager {

    @Autowired private ConnectorService<LmsOAuthService<ApiToken>> connectorService;

    private LmsOAuthService<ApiToken> instance(long platformDeploymentId) throws TerracottaConnectorException {
        return connectorService.instance(platformDeploymentId, LmsOAuthService.class);
    }

    @Override
    public LmsOAuthService<?> getLmsOAuthService(PlatformDeployment platformDeployment) throws TerracottaConnectorException {
        return getLmsOAuthService(platformDeployment.getKeyId());
    }

    @Override
    public LmsOAuthService<?> getLmsOAuthService(long platformDeploymentId) throws TerracottaConnectorException {
        return instance(platformDeploymentId);
    }

}
