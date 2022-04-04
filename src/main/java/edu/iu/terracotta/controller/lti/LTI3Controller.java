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

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.repository.LtiContextRepository;
import edu.iu.terracotta.repository.LtiLinkRepository;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.caliper.CaliperService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.SignatureException;
import edu.iu.terracotta.model.LtiLinkEntity;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.lti.LTIDataService;
import edu.iu.terracotta.service.lti.LTIJWTService;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;
import edu.iu.terracotta.utils.lti.LTI3Request;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.util.List;

/**
 * This LTI 3 redirect controller will retrieve the LTI3 requests and redirect them to the right page.
 * Everything that arrives here is filtered first by the LTI3OAuthProviderProcessingFilter
 */
@Controller
@Scope("session")
@RequestMapping("/lti3")
public class LTI3Controller {

    static final Logger log = LoggerFactory.getLogger(LTI3Controller.class);

    @Autowired
    LTIJWTService ltijwtService;

    @Autowired
    APIJWTService apiJWTService;

    @Autowired
    LtiLinkRepository ltiLinkRepository;

    @Autowired
    LTIDataService ltiDataService;

    @Autowired
    AssignmentService assignmentService;

    @Autowired
    CaliperService caliperService;

    @Autowired
    LtiContextRepository ltiContextRepository;

    @RequestMapping({"", "/"})
    public String home(HttpServletRequest req, Principal principal, Model model) throws DataServiceException, CanvasApiException, ConnectionException {

        //First we will get the state, validate it
        String state = req.getParameter("state");
        //We will use this link to find the content to display.
        String link = req.getParameter("link");
        try {
            Jws<Claims> claims = ltijwtService.validateState(state);
            LTI3Request lti3Request = LTI3Request.getInstance(link);
            // This is just an extra check that we have added, but it is not necessary.
            // Checking that the clientId in the status matches the one coming with the ltiRequest.
            if (!claims.getBody().get("clientId").equals(lti3Request.getAud())) {
                model.addAttribute(TextConstants.ERROR, " Bad Client Id");
                return TextConstants.LTI3ERROR;
            }
            // This is just an extra check that we have added, but it is not necessary.
            // Checking that the deploymentId in the status matches the one coming with the ltiRequest.
            // Note: there may not be an ltiDeploymentId claim if
            // lti_deployment_id was not included in the initial login
            // parameters and the platform has multiple tool deployments
            if (claims.getBody().containsKey("ltiDeploymentId") && claims.getBody().get("ltiDeploymentId") != null
                    && !claims.getBody().get("ltiDeploymentId").equals(lti3Request.getLtiDeploymentId())) {
                model.addAttribute(TextConstants.ERROR, " Bad Deployment Id");
                return TextConstants.LTI3ERROR;
            }
            //We add the request to the model so it can be displayed. But, in a real application, we would start
            // processing it here to generate the right answer.
            if (ltiDataService.getDemoMode()) {
                model.addAttribute("lTI3Request", lti3Request);
                if (link == null) {
                    link = lti3Request.getLtiTargetLinkUrl().substring(lti3Request.getLtiTargetLinkUrl().lastIndexOf("?link=") + 6);
                }
                if (StringUtils.isNotBlank(link)) {
                    List<LtiLinkEntity> linkEntity = ltiLinkRepository.findByLinkKeyAndContext(link, lti3Request.getContext());
                    log.debug("Searching for link " + link + " in the context Key " + lti3Request.getContext().getContextKey() + " And id " + lti3Request.getContext().getContextId());
                    if (linkEntity.size() > 0) {
                        model.addAttribute(TextConstants.HTML_CONTENT, linkEntity.get(0).createHtmlFromLink());
                    } else {
                        model.addAttribute(TextConstants.HTML_CONTENT, "<b> No element was found for that context and linkKey</b>");
                    }
                } else {
                    model.addAttribute(TextConstants.HTML_CONTENT, "<b> No element was requested or it doesn't exists </b>");
                }
                if (lti3Request.getLtiMessageType().equals(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING)) {
                    //Let's create the LtiLinkEntity's in our database
                    //This should be done AFTER the user selects the link in the content selector, and we are doing it before
                    //just to keep it simple. The ideal process would be, the user selects a link, sends it to the platform and
                    // we create the LtiLinkEntity in our code after that.
                    LtiLinkEntity ltiLinkEntity = new LtiLinkEntity("1234", lti3Request.getContext(), "My Test Link");
                    if (ltiLinkRepository.findByLinkKeyAndContext(ltiLinkEntity.getLinkKey(), ltiLinkEntity.getContext()).size() == 0) {
                        ltiLinkRepository.save(ltiLinkEntity);
                    }
                    LtiLinkEntity ltiLinkEntity2 = new LtiLinkEntity("4567", lti3Request.getContext(), "Another Link");
                    if (ltiLinkRepository.findByLinkKeyAndContext(ltiLinkEntity2.getLinkKey(), ltiLinkEntity2.getContext()).size() == 0) {
                        ltiLinkRepository.save(ltiLinkEntity2);
                    }
                    return "lti3DeepLink";
                }
                return "lti3Result";
            } else {
                String oneTimeToken = apiJWTService.buildJwt(
                        true,
                        lti3Request);
                assignmentService.checkAndRestoreAssignmentsInCanvasByContext(lti3Request.getContext().getContextId());
                caliperService.sendToolUseEvent(
                        lti3Request.getMembership(),
                        lti3Request.getLtiCustom().getOrDefault("canvas_user_global_id", "Anonymous").toString(),
                        lti3Request.getLtiCustom().getOrDefault("canvas_course_id", "UnknownCourse").toString(),
                        lti3Request.getLtiCustom().getOrDefault("canvas_user_id", "Anonymous").toString(),
                        lti3Request.getLtiCustom().getOrDefault("canvas_login_id", "Anonymous").toString(),
                        lti3Request.getLtiRoles(),
                        lti3Request.getLtiCustom().getOrDefault("canvas_user_name", "Anonymous").toString());
                String targetLinkUri = URLEncoder.encode(lti3Request.getLtiTargetLinkUrl(), StandardCharsets.UTF_8.toString());
                return "redirect:/app/app.html?token=" + oneTimeToken + "&targetLinkUri=" + targetLinkUri;
            }
        } catch (SignatureException ex) {
            model.addAttribute(TextConstants.ERROR, ex.getMessage());
            return TextConstants.LTI3ERROR;
        } catch (GeneralSecurityException e) {
            model.addAttribute(TextConstants.ERROR, e.getMessage());
            return TextConstants.LTI3ERROR;
        } catch (IOException e) {
            model.addAttribute(TextConstants.ERROR, e.getMessage());
            return TextConstants.LTI3ERROR;
        }
    }


}
