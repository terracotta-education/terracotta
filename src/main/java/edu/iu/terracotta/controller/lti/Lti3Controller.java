package edu.iu.terracotta.controller.lti;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiLinkEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiLinkRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.connectors.generic.service.lms.LmsOAuthService;
import edu.iu.terracotta.connectors.generic.service.lms.LmsOAuthServiceManager;
import edu.iu.terracotta.connectors.generic.service.lti.LtiDataService;
import edu.iu.terracotta.connectors.generic.service.lti.LtiJwtService;
import edu.iu.terracotta.controller.app.LmsOAuthController;
import edu.iu.terracotta.dao.entity.ObsoleteAssignment;
import edu.iu.terracotta.dao.exceptions.FeatureNotFoundException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.service.caliper.CaliperService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;
import edu.iu.terracotta.utils.lti.Lti3Request;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
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
import java.net.URLEncoder;
import java.nio.charset.Charset;
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
public class Lti3Controller {

    @Autowired  private LtiJwtService ltijwtService;
    @Autowired private ApiJwtService apiJwtService;
    @Autowired private LtiLinkRepository ltiLinkRepository;
    @Autowired private LtiDataService ltiDataService;
    @Autowired private CaliperService caliperService;
    @Autowired private LmsOAuthServiceManager lmsOAuthServiceManager;

    @RequestMapping({"", "/"})
    public String home(HttpServletRequest req, Principal principal, Model model) throws DataServiceException, ApiException, ConnectionException, LmsOAuthException, TerracottaConnectorException {
        // First we will get the state, validate it
        String state = req.getParameter("state");
        // We will use this link to find the content to display.
        String link = req.getParameter("link");

        try {
            Jws<Claims> claims = ltijwtService.validateState(state);
            Lti3Request lti3Request = Lti3Request.getInstance(link);

            // check if the request is for an obsolete assignment; redirect immediately if true
            if (Strings.CS.endsWith(lti3Request.getLtiTargetLinkUrl(), ObsoleteAssignment.URL)) {
                return String.format("redirect:/%s", ObsoleteAssignment.URL);
            }

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

            String oneTimeToken = apiJwtService.buildJwt(true, lti3Request);
            caliperService.sendToolUseEvent(
                lti3Request.getMembership(),
                lti3Request.getLtiCustom().getOrDefault("lms_user_global_id", "Anonymous").toString(),
                lti3Request.getLtiCustom().getOrDefault("lms_course_id", "UnknownCourse").toString(),
                lti3Request.getLtiCustom().getOrDefault("lms_user_id", "Anonymous").toString(),
                lti3Request.getLtiCustom().getOrDefault("lms_login_id", "Anonymous").toString(),
                lti3Request.getLtiRoles(),
                lti3Request.getLtiCustom().getOrDefault("lms_user_name", "Anonymous").toString()
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

            String redirectUrl = "redirect:/app/app.html?token=" + oneTimeToken;

            // Check if we need to get API token from instructor to use LMS API
            if (lti3Request.isRoleInstructor()) {
                String oauth2APITokenRedirectURL = getOAuth2APITokenRedirectURL(req, lti3Request.getKey(), lti3Request.getUser(), lti3Request);

                if (oauth2APITokenRedirectURL != null) {
                    redirectUrl += "&lms_api_oauth_url=" + URLEncoder.encode(oauth2APITokenRedirectURL, Charset.defaultCharset());
                }
            }

            return redirectUrl;
        } catch (SignatureException | GeneralSecurityException | IOException e) {
            model.addAttribute(TextConstants.ERROR, e.getMessage());
            return TextConstants.LTI3ERROR;
        }
    }

    private String getOAuth2APITokenRedirectURL(HttpServletRequest req, PlatformDeployment platformDeployment, LtiUserEntity user, Lti3Request lti3Request)
            throws GeneralSecurityException, IOException, LmsOAuthException, TerracottaConnectorException {
        LmsOAuthService<?> lmsOAuthService = null;

        try {
            // check if API Token settings exist for this PlatformDeployment
            lmsOAuthService = lmsOAuthServiceManager.getLmsOAuthService(platformDeployment);
        } catch (TerracottaConnectorException e) {
            log.error(e.getMessage(), e);
            return null;
        }

        if (!lmsOAuthService.isConfigured(platformDeployment)) {
            return null;
        }

        if (lmsOAuthService.isAccessTokenAvailable(user)) {
            return null;
        }

        // if LMS OAuth settings are configured but user doesn't have an access token, get one. Create and return authorization url.
        String state = apiJwtService.generateStateForAPITokenRequest(lti3Request);
        HttpSession session = req.getSession();
        session.setAttribute(LmsOAuthController.SESSION_LMS_OAUTH2_STATE, state);

        try {
            return lmsOAuthService.getAuthorizationRequestURI(platformDeployment, state);
        } catch (FeatureNotFoundException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

}
