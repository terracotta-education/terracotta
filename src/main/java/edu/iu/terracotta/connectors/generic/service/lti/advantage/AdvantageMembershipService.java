package edu.iu.terracotta.connectors.generic.service.lti.advantage;

import java.util.List;
import java.util.UUID;

import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatch;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.model.lms.membership.CourseUsers;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;

@TerracottaConnector(LmsConnector.GENERIC)
public interface AdvantageMembershipService {

    LtiToken getToken(PlatformDeployment platformDeployment) throws ConnectionException, TerracottaConnectorException;
    CourseUsers callMembershipService(LtiToken ltiToken, LtiContextEntity context, UUID batchId) throws ConnectionException, TerracottaConnectorException;
    CourseUsers callMembershipService(LtiToken ltiToken, LtiContextEntity context, UUID batchId, boolean onlyStudents) throws ConnectionException, TerracottaConnectorException;
    List<LmsUserBatch> getAllLmsUsers(LtiToken ltiToken, LtiContextEntity context) throws ConnectionException, TerracottaConnectorException;

}
