package edu.iu.terracotta.controller.lti;


import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.membership.CourseUsers;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiContextRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.ToolDeploymentRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageMembershipService;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Optional;

/**
 * This LTI 3 redirect controller will retrieve the LTI3 requests and redirect them to the right page.
 * Everything that arrives here is filtered first by the LTI3OAuthProviderProcessingFilter
 */
@Controller
@Scope("session")
@RequestMapping("/membership")
public class MembershipController {

    @Autowired private LtiContextRepository ltiContextRepository;
    @Autowired private ToolDeploymentRepository toolDeploymentRepository;
    @Autowired private AdvantageMembershipService advantageMembershipService;

    @RequestMapping({"", "/"})
    public String membershipGet(HttpServletRequest req, Principal principal, Model model) throws ConnectionException, TerracottaConnectorException {

        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        HttpSession session = req.getSession();

        if (session.getAttribute(LtiStrings.LTI_SESSION_TOOL_DEPLOYMENT_ID) == null) {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, true);
        }

        model.addAttribute(TextConstants.NO_SESSION_VALUES, false);
        Long deployment = (Long) session.getAttribute(LtiStrings.LTI_SESSION_TOOL_DEPLOYMENT_ID);
        String contextId = (String) session.getAttribute(LtiStrings.LTI_SESSION_CONTEXT_ID);
        //We find the right deployment:
        Optional<ToolDeployment> toolDeployment = toolDeploymentRepository.findById(deployment);

        if (toolDeployment.isPresent()) {
            //Get the context in the query
            LtiContextEntity context = ltiContextRepository.findByContextKeyAndToolDeployment(contextId, toolDeployment.get());

            //Call the membership service to get the users on the context
            // 1. Get the token
            LtiToken ltiToken = advantageMembershipService.getToken(toolDeployment.get().getPlatformDeployment());

            // 2. Call the service
            CourseUsers courseUsers = advantageMembershipService.callMembershipService(ltiToken, context);

            // 3. update the model
            model.addAttribute(TextConstants.RESULTS, courseUsers.getCourseUserList());
        }

        return "ltiAdvMembershipMain";
    }


}
