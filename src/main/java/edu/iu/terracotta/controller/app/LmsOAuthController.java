package edu.iu.terracotta.controller.app;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/lms/oauth2")
@SuppressWarnings({"PMD.AvoidCatchingThrowable"})
public class LmsOAuthController {

    public static final String SESSION_LMS_OAUTH2_STATE = "lms_oauth2_state";

    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private LmsOAuthServiceManager lmsOAuthServiceManager;
    @Autowired private ApiJwtService apijwtService;

    @GetMapping("/oauth_response")
    public String handleOauthResponse(HttpServletRequest req, Model model) throws GeneralSecurityException, IOException, LmsOAuthException, TerracottaConnectorException {
        String code = req.getParameter("code");
        log.debug("/oauth_response: code={}", code);

        // Verify that state parameter matches session state
        String state = req.getParameter("state");
        log.debug("/oauth_response: state={}", state);

        String error = req.getParameter("error");

        if (error != null) {
            model.addAttribute(TextConstants.ERROR, MessageFormat.format("Error getting LMS API access token: {0}", error));
            return TextConstants.OAUTH2_ERROR;
        }

        String sessionState = (String) req.getSession().getAttribute(SESSION_LMS_OAUTH2_STATE);

        if (sessionState == null || !sessionState.equals(state)) {
            model.addAttribute(TextConstants.ERROR, "Error getting LMS API access token: OAuth2 request doesn't contain the expected state");
            return TextConstants.OAUTH2_ERROR;
        }

        Optional<Jws<Claims>> claims = Optional.empty();

        try {
            claims = apijwtService.validateStateForAPITokenRequest(state);
        } catch (Throwable t) {
            log.error("Failed to validate the state claims", t);
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

        return String.format(
            "redirect:/app/app.html?token=%s",
            apijwtService.buildJwt(platformDeploymentId, userKey, claims.get().getPayload())
        );
    }

}
