package edu.iu.terracotta.connectors.brightspace.service.lti.advantage.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.model.lms.membership.CourseUser;
import edu.iu.terracotta.connectors.generic.dao.model.lms.membership.CourseUsers;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.dao.model.lti.enums.LtiAgsScope;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.exceptions.helper.ExceptionMessageGenerator;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageConnectorHelper;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageMembershipService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@TerracottaConnector(LmsConnector.BRIGHTSPACE)
@SuppressWarnings({"rawtypes", "PMD.GuardLogStatement"})
public class BrightspaceAdvantageMembershipServiceImpl implements AdvantageMembershipService {

    @Autowired private AdvantageConnectorHelper advantageConnectorHelper;
    @Autowired private ExceptionMessageGenerator exceptionMessageGenerator;

    @Value("${app.token.logging.enabled:true}")
    private boolean tokenLoggingEnabled;

    @Override
    public LtiToken getToken(PlatformDeployment platformDeployment) throws ConnectionException, TerracottaConnectorException {
        return advantageConnectorHelper.getToken(platformDeployment, LtiAgsScope.NRPS_MEMBERSHIP_READONLY.key());
    }

    @Override
    public CourseUsers callMembershipService(LtiToken ltiToken, LtiContextEntity context) throws ConnectionException, TerracottaConnectorException {
        if (tokenLoggingEnabled) {
            log.debug("{}{}", TextConstants.TOKEN, ltiToken.getAccess_token());
        }

        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            HttpEntity request = advantageConnectorHelper.createTokenizedRequestEntityWithAccept(ltiToken, LtiAgsScope.NRPS_MEMBERSHIP_JSON_ACCEPT.key());
            ResponseEntity<CourseUsers> membershipGetResponse = restTemplate.exchange(context.getContext_memberships_url(), HttpMethod.GET, request, CourseUsers.class);

            if (!membershipGetResponse.getStatusCode().is2xxSuccessful()) {
                String exceptionMsg = "Can't get the membership";
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }

            CourseUsers courseUsers = membershipGetResponse.getBody();
            List<CourseUser> courseUserList = new ArrayList<>(Objects.requireNonNull(courseUsers).getCourseUserList());
            String nextPage = advantageConnectorHelper.nextPage(membershipGetResponse.getHeaders());

            while (nextPage != null) {
                ResponseEntity<CourseUsers> responseForNextPage = restTemplate.exchange(nextPage, HttpMethod.GET, request, CourseUsers.class);
                List<CourseUser> nextCourseUsersList = Objects.requireNonNull(responseForNextPage.getBody()).getCourseUserList();
                courseUserList.addAll(nextCourseUsersList);
                nextPage = advantageConnectorHelper.nextPage(responseForNextPage.getHeaders());
            }

            return CourseUsers.builder()
                .courseUserList(courseUserList)
                .build();
        } catch (Exception e) {
            String exceptionMsg = "Can't get the membership";
            log.error(exceptionMsg, e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg, e));
        }
    }

}
