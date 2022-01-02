/**
 * Copyright 2021 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.iu.terracotta.controller.lti;


import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.repository.LtiContextRepository;
import edu.iu.terracotta.repository.ToolDeploymentRepository;
import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.ToolDeployment;
import edu.iu.terracotta.model.membership.CourseUsers;
import edu.iu.terracotta.model.oauth2.LTIToken;
import edu.iu.terracotta.service.lti.AdvantageMembershipService;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
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

    static final Logger log = LoggerFactory.getLogger(MembershipController.class);

    @Autowired
    LtiContextRepository ltiContextRepository;

    @Autowired
    ToolDeploymentRepository toolDeploymentRepository;

    @Autowired
    AdvantageMembershipService advantageMembershipService;

    @SuppressWarnings("SameReturnValue")
    @RequestMapping({"", "/"})
    public String membershipGet(HttpServletRequest req, Principal principal, Model model) throws ConnectionException {

        //To keep this endpoint secured, we will only allow access to the course/platform stored in the session.
        //LTI Advantage services doesn't need a session to access to the membership, but we implemented this control here
        // to avoid access to all the courses and platforms.
        HttpSession session = req.getSession();
        if (session.getAttribute(LtiStrings.LTI_SESSION_TOOL_DEPLOYMENT_ID) != null) {
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
                LTIToken LTIToken = advantageMembershipService.getToken(toolDeployment.get().getPlatformDeployment());

                // 2. Call the service
                CourseUsers courseUsers = advantageMembershipService.callMembershipService(LTIToken, context);

                // 3. update the model
                model.addAttribute(TextConstants.RESULTS, courseUsers.getCourseUserList());
            }
        } else {
            model.addAttribute(TextConstants.NO_SESSION_VALUES, true);
        }
        return "ltiAdvMembershipMain";
    }


}
