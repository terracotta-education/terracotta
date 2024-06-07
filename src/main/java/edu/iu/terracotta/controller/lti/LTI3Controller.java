package edu.iu.terracotta.controller.lti;

import edu.iu.terracotta.controller.app.LMSOAuthController;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.LMSOAuthException;
import edu.iu.terracotta.repository.LtiLinkRepository;
import edu.iu.terracotta.service.caliper.CaliperService;
import edu.iu.terracotta.service.common.LMSOAuthService;
import edu.iu.terracotta.service.common.LMSOAuthServiceManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import edu.iu.terracotta.model.LtiLinkEntity;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.lti.LTIDataService;
import edu.iu.terracotta.service.lti.LTIJWTService;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;
import edu.iu.terracotta.utils.lti.LTI3Request;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

/**
 * This LTI 3 redirect controller will retrieve the LTI3 requests and redirect them to the right page.
 * Everything that arrives here is filtered first by the LTI3OAuthProviderProcessingFilter
 */
@Slf4j
@Controller
@Scope("session")
@RequestMapping("/lti3")
@SuppressWarnings({"PMD.GuardLogStatement"})
public class LTI3Controller {

    @Autowired  private LTIJWTService ltijwtService;
    @Autowired private APIJWTService apiJWTService;
    @Autowired private LtiLinkRepository ltiLinkRepository;
    @Autowired private LTIDataService ltiDataService;
    @Autowired private CaliperService caliperService;
    @Autowired private LMSOAuthServiceManager lmsoAuthServiceManager;

    @RequestMapping({"", "/"})
    public String home(HttpServletRequest req, Principal principal, Model model) throws DataServiceException, CanvasApiException, ConnectionException, LMSOAuthException {
        // First we will get the state, validate it
        String state = req.getParameter("state");
        // We will use this link to find the content to display.
        String link = req.getParameter("link");

        try {
            Jws<Claims> claims = ltijwtService.validateState(state);
            LTI3Request lti3Request = LTI3Request.getInstance(link);

            // This is just an extra check that we have added, but it is not necessary.
            // Checking that the clientId in the status matches the one coming with the ltiRequest.
            if (!claims.getPayload().get("clientId").equals(lti3Request.getAud())) {
                model.addAttribute(TextConstants.ERROR, " Bad Client Id");

                return TextConstants.LTI3ERROR;
            }

            // This is just an extra check that we have added, but it is not necessary.
            // Checking that the deploymentId in the status matches the one coming with the ltiRequest.
            // Note: there may not be an ltiDeploymentId claim if
            // lti_deployment_id was not included in the initial login
            // parameters and the platform has multiple tool deployments
            if (claims.getPayload().containsKey("ltiDeploymentId") && claims.getPayload().get("ltiDeploymentId") != null
                    && !claims.getPayload().get("ltiDeploymentId").equals(lti3Request.getLtiDeploymentId())) {
                model.addAttribute(TextConstants.ERROR, " Bad Deployment Id");

                return TextConstants.LTI3ERROR;
            }

            // We add the request to the model so it can be displayed. But, in a real application, we would start processing it here to generate the right answer.
            if (ltiDataService.getDemoMode()) {
                model.addAttribute("lTI3Request", lti3Request);

                if (link == null) {
                    link = lti3Request.getLtiTargetLinkUrl().substring(lti3Request.getLtiTargetLinkUrl().lastIndexOf("?link=") + 6);
                }

                if (StringUtils.isNotBlank(link)) {
                    List<LtiLinkEntity> linkEntity = ltiLinkRepository.findByLinkKeyAndContext(link, lti3Request.getContext());
                    log.debug("Searching for link " + link + " in the context Key " + lti3Request.getContext().getContextKey() + " And id " + lti3Request.getContext().getContextId());

                    if (CollectionUtils.isNotEmpty(linkEntity)) {
                        model.addAttribute(TextConstants.HTML_CONTENT, linkEntity.get(0).createHtmlFromLink());
                    } else {
                        model.addAttribute(TextConstants.HTML_CONTENT, "<b>No element was found for that context and linkKey</b>");
                    }
                } else {
                    model.addAttribute(TextConstants.HTML_CONTENT, "<b>No element was requested or it doesn't exists</b>");
                }

                if (LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING.equals(lti3Request.getLtiMessageType())) {
                    // Let's create the LtiLinkEntity's in our database
                    // This should be done AFTER the user selects the link in the content selector, and we are doing it before
                    // just to keep it simple. The ideal process would be, the user selects a link, sends it to the platform and
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
            }

            String oneTimeToken = apiJWTService.buildJwt(true, lti3Request);
            caliperService.sendToolUseEvent(
                lti3Request.getMembership(),
                lti3Request.getLtiCustom().getOrDefault("canvas_user_global_id", "Anonymous").toString(),
                lti3Request.getLtiCustom().getOrDefault("canvas_course_id", "UnknownCourse").toString(),
                lti3Request.getLtiCustom().getOrDefault("canvas_user_id", "Anonymous").toString(),
                lti3Request.getLtiCustom().getOrDefault("canvas_login_id", "Anonymous").toString(),
                lti3Request.getLtiRoles(),
                lti3Request.getLtiCustom().getOrDefault("canvas_user_name", "Anonymous").toString()
            );

            // Check for platform_redirect_url to determine if this is a first-party interaction request
            try {
                List<NameValuePair> targetLinkQueryParams = new URIBuilder(lti3Request.getLtiTargetLinkUrl()).getQueryParams();
                Optional<NameValuePair> platformRedirectUrl = targetLinkQueryParams.stream()
                    .filter(nv -> "platform_redirect_url".equals(nv.getName()))
                    .findFirst();

                if (platformRedirectUrl.isPresent()) {
                    model.addAttribute("targetLinkUri", lti3Request.getLtiTargetLinkUrl());
                    return "redirect:/app/firstParty.html";
                }
            } catch (URISyntaxException ex) {
                model.addAttribute(TextConstants.ERROR, ex.getMessage());
                return TextConstants.LTI3ERROR;
            }

            // Check if we need to get API token from instructor to use LMS API
            if (lti3Request.isRoleInstructor()) {
                String oauth2APITokenRedirectURL = getOAuth2APITokenRedirectURL(req, lti3Request.getKey(), lti3Request.getUser(), lti3Request);

                if (oauth2APITokenRedirectURL != null) {
                    model.addAttribute("lms_api_oauth_url", oauth2APITokenRedirectURL);
                }
            }

            return "redirect:/app/app.html?token=" + oneTimeToken;
        } catch (SignatureException | GeneralSecurityException | IOException e) {
            model.addAttribute(TextConstants.ERROR, e.getMessage());
            return TextConstants.LTI3ERROR;
        }
    }

    private String getOAuth2APITokenRedirectURL(HttpServletRequest req, PlatformDeployment platformDeployment, LtiUserEntity user, LTI3Request lti3Request)
            throws GeneralSecurityException, IOException, LMSOAuthException {
        // check if API Token settings exist for this PlatformDeployment
        LMSOAuthService<?> lmsOAuthService = lmsoAuthServiceManager.getLMSOAuthService(platformDeployment);

        if (lmsOAuthService == null) {
            return null;
        }

        if (lmsOAuthService.isAccessTokenAvailable(user)) {
            return null;
        }

        // if LMS OAuth settings are configured but user doesn't have an access token,
        // we'll need to get one. Create and return authorization url.
        String state = apiJWTService.generateStateForAPITokenRequest(lti3Request);
        HttpSession session = req.getSession();
        session.setAttribute(LMSOAuthController.SESSION_LMS_OAUTH2_STATE, state);

        return lmsOAuthService.getAuthorizationRequestURI(platformDeployment, state);
    }

}
