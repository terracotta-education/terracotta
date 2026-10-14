package edu.iu.terracotta.controller.app;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.connectors.generic.service.lms.LmsOAuthService;
import edu.iu.terracotta.connectors.generic.service.lms.LmsOAuthServiceManager;
import edu.iu.terracotta.dao.exceptions.FeatureNotFoundException;
import edu.iu.terracotta.utils.TextConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/lms/oauth2")
@SuppressWarnings({"PMD.AvoidCatchingThrowable"})
public class LmsOAuthController {

    private final LtiUserRepository ltiUserRepository;
    private final LmsOAuthServiceManager lmsOAuthServiceManager;
    private final ApiJwtService apijwtService;

    @GetMapping("/oauth_response")
    public String handleOauthResponse(HttpServletRequest req, Model model) throws GeneralSecurityException, IOException, LmsOAuthException, TerracottaConnectorException {
        String code = req.getParameter("code");
        log.debug("/oauth_response: code={}", code);

        // The state is a self-contained signed JWT (see ApiJwtService#generateStateForAPITokenRequest), so its own
        // signature - validated just below - is enough to confirm it is one we actually issued; no session needed.
        String state = req.getParameter("state");
        log.debug("/oauth_response: state={}", state);

        String error = req.getParameter("error");

        if (error != null) {
            model.addAttribute(TextConstants.ERROR, MessageFormat.format("Error getting LMS API access token: {0}", error));
            return TextConstants.OAUTH2_ERROR;
        }

        if (state == null || state.isBlank()) {
            // the LMS always returns a state (with a code or an error), so this is someone opening
            // or refreshing this address directly, not a return from authorization
            log.warn("Ignoring a /oauth_response request with no OAuth2 state (code {}); it isn't a return from LMS authorization", code == null ? "missing" : "present");
            model.addAttribute(TextConstants.ERROR, "Error getting LMS API access token: \"OAuth2 request doesn't contain the expected state\"");
            return TextConstants.OAUTH2_ERROR;
        }

        Optional<Jws<Claims>> claims = Optional.empty();

        try {
            claims = apijwtService.validateStateForAPITokenRequest(state);
        } catch (Throwable t) {
            // a forged, tampered or expired state - the reason is enough, the stack trace adds nothing
            log.warn("Couldn't validate the OAuth2 state on /oauth_response: {}", t.getMessage());
            model.addAttribute(TextConstants.ERROR, "Error getting LMS API access token: Could not validate the OAuth2 request state");
            return TextConstants.OAUTH2_ERROR;
        }

        if (claims.isEmpty()) {
            model.addAttribute(TextConstants.ERROR, "Error getting LMS API access token: \"OAuth2 request doesn't contain the expected state\"");
            return TextConstants.OAUTH2_ERROR;
        }

        long platformDeploymentId = claims.get().getPayload().get("platformDeploymentId", Long.class);
        LmsOAuthService<?> lmsoAuthService = lmsOAuthServiceManager.getLmsOAuthService(platformDeploymentId);

        String userKey = claims.get().getPayload().get("userId", String.class);
        LtiUserEntity user = ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(userKey, platformDeploymentId);

        try {
            lmsoAuthService.fetchAndSaveAccessToken(user, code);
        } catch (LmsOAuthException | FeatureNotFoundException e) {
            model.addAttribute(TextConstants.ERROR, MessageFormat.format("Error getting LMS API access token: {0}", e.getMessage()));
            return TextConstants.OAUTH2_ERROR;
        }

        // delivered via the response body rather than a redirect's query string, same as
        // Lti3Controller#home and for the same reason - see lti3Launch.html
        model.addAttribute("token", apijwtService.buildJwt(platformDeploymentId, userKey, claims.get().getPayload()));
        model.addAttribute("lmsApiOAuthUrl", null);
        // authorization normally runs in a window the frame opened; the page closes it and the
        // frame, which polls for the new token, carries on
        model.addAttribute("authorizationWindow", true);

        return "lti3Launch";
    }

}
