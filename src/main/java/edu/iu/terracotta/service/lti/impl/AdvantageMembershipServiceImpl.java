package edu.iu.terracotta.service.lti.impl;

import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.helper.ExceptionMessageGenerator;
import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.membership.CourseUser;
import edu.iu.terracotta.model.membership.CourseUsers;
import edu.iu.terracotta.model.oauth2.LTIToken;
import edu.iu.terracotta.service.lti.AdvantageConnectorHelper;
import edu.iu.terracotta.service.lti.AdvantageMembershipService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@SuppressWarnings({"rawtypes", "PMD.GuardLogStatement"})
public class AdvantageMembershipServiceImpl implements AdvantageMembershipService {

    @Autowired private AdvantageConnectorHelper advantageConnectorHelper;
    @Autowired private ExceptionMessageGenerator exceptionMessageGenerator;

    @Value("${app.token.logging.enabled:true}")
    private boolean tokenLoggingEnabled;

    @Override
    public LTIToken getToken(PlatformDeployment platformDeployment) throws ConnectionException {
        return advantageConnectorHelper.getToken(platformDeployment, "https://purl.imsglobal.org/spec/lti-nrps/scope/contextmembership.readonly");
    }

    @Override
    public CourseUsers callMembershipService(LTIToken ltiToken, LtiContextEntity context) throws ConnectionException {
        if (tokenLoggingEnabled) {
            log.debug(TextConstants.TOKEN + ltiToken.getAccess_token());
        }

        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            HttpEntity request = advantageConnectorHelper.createTokenizedRequestEntity(ltiToken);
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

            courseUsers = new CourseUsers();
            courseUsers.getCourseUserList().addAll(courseUserList);

            return courseUsers;
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't get the membership");
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }

}
