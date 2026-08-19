package edu.iu.terracotta.controller.lti;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiDeepLink;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiLinkEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiLinkRepository;
import edu.iu.terracotta.connectors.canvas.service.lti.advantage.CanvasAdvantageNoticeService;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.connectors.generic.service.lms.LmsOAuthService;
import edu.iu.terracotta.connectors.generic.service.lms.LmsOAuthServiceManager;
import edu.iu.terracotta.connectors.generic.service.lti.LtiDataService;
import edu.iu.terracotta.connectors.generic.service.lti.LtiJwtService;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageDeepLinkService;
import edu.iu.terracotta.dao.entity.ObsoleteAssignment;
import edu.iu.terracotta.dao.exceptions.FeatureNotFoundException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.service.caliper.CaliperService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;
import edu.iu.terracotta.utils.lti.Lti3Request;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.util.List;

/**
 * This LTI 3 redirect controller will retrieve the LTI3 requests and redirect them to the right page.
 * Everything that arrives here is filtered first by the LTI3OAuthProviderProcessingFilter
 */
@Slf4j
@Controller
@RequestMapping("/lti3")
@RequiredArgsConstructor
@SuppressWarnings({"PMD.GuardLogStatement"})
public class Lti3Controller {

    private final LtiLinkRepository ltiLinkRepository;
    private final ApiJwtService apiJwtService;
    private final AdvantageDeepLinkService advantageDeepLinkService;
    private final CaliperService caliperService;
    private final LtiDataService ltiDataService;
    private final LtiJwtService ltiJwtService;
    private final LmsOAuthServiceManager lmsOAuthServiceManager;
    private final CanvasAdvantageNoticeService canvasAdvantageNoticeService;

    @RequestMapping({"", "/"})
    public String home(HttpServletRequest req, Principal principal, Model model) throws DataServiceException, ApiException, ConnectionException, LmsOAuthException, TerracottaConnectorException {
        // First we will get the state, validate it
        String state = req.getParameter("state");
        // We will use this link to find the content to display.
        String link = req.getParameter("link");

        try {
            Jws<Claims> claims = ltiJwtService.validateState(state);
            Lti3Request lti3Request = Lti3Request.getInstance(link);

            // check if the request is for an obsolete assignment; redirect immediately if true
            if (Strings.CI.endsWith(lti3Request.getLtiTargetLinkUrl(), ObsoleteAssignment.URL)) {
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

            // best-effort, fire-and-forget: registers this tool as an LTI Platform Notification
            // Service course-copy handler with Canvas, if not already done for this deployment -
            // see CanvasAdvantageNoticeServiceImpl and NoticeController. A no-op after the first
            // successful launch for a given deployment (see ToolDeployment.noticeHandlerRegistered),
            // and never blocks or fails the launch itself either way.
            if (LmsConnector.CANVAS == lti3Request.getToolDeployment().getPlatformDeployment().getLmsConnector()) {
                canvasAdvantageNoticeService.ensureNoticeHandlerRegistered(lti3Request.getToolDeployment());
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

            if (LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING.equals(lti3Request.getLtiMessageType())) {
                LtiDeepLink ltiDeepLink = advantageDeepLinkService.generateLtiDeepLink(
                    lti3Request,
                    req,
                    state
                );

                return String.format(
                    "redirect:/app/deepLink.html?id=%s",
                    ltiDeepLink.getUuid()
                );
            }

            String redirectUrl = "redirect:/app/app.html?token=" + oneTimeToken;

            // Check if we need to get API token from instructor to use LMS API
            if (lti3Request.isRoleInstructor()) {
                String oauth2APITokenRedirectURL = getOAuth2APITokenRedirectURL(lti3Request.getKey(), lti3Request.getUser(), lti3Request);

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

    private String getOAuth2APITokenRedirectURL(PlatformDeployment platformDeployment, LtiUserEntity user, Lti3Request lti3Request)
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

        // if LMS OAuth settings are configured but user doesn't have an access token, get one. Create and return
        // authorization url. The state is a self-contained signed JWT (see ApiJwtService#validateStateForAPITokenRequest),
        // so its own signature is enough to validate it later in LmsOAuthController - no session needed to track it.
        String state = apiJwtService.generateStateForAPITokenRequest(lti3Request);

        try {
            return lmsOAuthService.getAuthorizationRequestURI(platformDeployment, state);
        } catch (FeatureNotFoundException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

}
