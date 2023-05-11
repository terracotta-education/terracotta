package edu.iu.terracotta.service.lti;

import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.membership.CourseUsers;
import edu.iu.terracotta.model.oauth2.LTIToken;

public interface AdvantageMembershipService {

    LTIToken getToken(PlatformDeployment platformDeployment) throws ConnectionException;

    CourseUsers callMembershipService(LTIToken ltiToken, LtiContextEntity context) throws ConnectionException;

}
