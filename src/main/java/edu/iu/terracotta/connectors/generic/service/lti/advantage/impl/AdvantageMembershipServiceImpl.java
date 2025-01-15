package edu.iu.terracotta.connectors.generic.service.lti.advantage.impl;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.membership.CourseUsers;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.connector.ConnectorService;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageMembershipService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Slf4j
@Primary
@Service
public class AdvantageMembershipServiceImpl implements AdvantageMembershipService {

    @Autowired private ConnectorService<AdvantageMembershipService> connectorService;

    private AdvantageMembershipService instance(LtiContextEntity ltiContextEntity) throws TerracottaConnectorException {
        return instance(ltiContextEntity.getToolDeployment().getPlatformDeployment());
    }

    private AdvantageMembershipService instance(PlatformDeployment platformDeployment) throws TerracottaConnectorException {
        return connectorService.instance(platformDeployment, AdvantageMembershipService.class);
    }

    @Override
    public LtiToken getToken(PlatformDeployment platformDeployment) throws ConnectionException, TerracottaConnectorException {
        return instance(platformDeployment).getToken(platformDeployment);
    }

    @Override
    public CourseUsers callMembershipService(LtiToken ltiToken, LtiContextEntity ltiContextEntity) throws ConnectionException, TerracottaConnectorException {
        return instance(ltiContextEntity).callMembershipService(ltiToken, ltiContextEntity);
    }

}
