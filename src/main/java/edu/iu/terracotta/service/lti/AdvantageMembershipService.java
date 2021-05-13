package edu.iu.terracotta.service.lti;

import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.membership.CourseUsers;
import edu.iu.terracotta.model.oauth2.LTIToken;

public interface AdvantageMembershipService {
    //Asking for a token with the right scope.
    LTIToken getToken(PlatformDeployment platformDeployment) throws ConnectionException;

    //Calling the membership service and getting a paginated result of users.
    CourseUsers callMembershipService(LTIToken LTIToken, LtiContextEntity context) throws ConnectionException;
}
