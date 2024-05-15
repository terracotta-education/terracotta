package edu.iu.terracotta.controller.app;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.iu.terracotta.exceptions.LMSOAuthException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.repository.LtiUserRepository;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.common.LMSOAuthService;
import edu.iu.terracotta.service.common.LMSOAuthServiceManager;
import edu.iu.terracotta.utils.TextConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/lms/oauth2")
@SuppressWarnings({"unchecked", "PMD.AvoidCatchingThrowable"})
public class LMSOAuthController {

    public static final String SESSION_LMS_OAUTH2_STATE = "lms_oauth2_state";

    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private LMSOAuthServiceManager lmsoAuthServiceManager;
    @Autowired private APIJWTService apijwtService;

    @GetMapping("/oauth_response")
    public String handleOauthResponse(HttpServletRequest req, Model model) throws GeneralSecurityException, IOException, LMSOAuthException {
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
            String errMessage = "OAuth2 request doesn't contain the expected state";
            model.addAttribute(TextConstants.ERROR, MessageFormat.format("Error getting LMS API access token: {0}", errMessage));
            return TextConstants.OAUTH2_ERROR;
        }

        Optional<Jws<Claims>> claims = Optional.empty();

        try {
            claims = apijwtService.validateStateForAPITokenRequest(state);
        } catch (Throwable t) {
            log.error("Failed to validate the state claims", t);
            String errMessage = "Could not validate the OAuth2 request state";
            model.addAttribute(TextConstants.ERROR, MessageFormat.format("Error getting LMS API access token: {0}", errMessage));
            return TextConstants.OAUTH2_ERROR;
        }

        if (claims.isEmpty()) {
            String errMessage = "OAuth2 request doesn't contain the expected state";
            model.addAttribute(TextConstants.ERROR, MessageFormat.format("Error getting LMS API access token: {0}", errMessage));
            return TextConstants.OAUTH2_ERROR;
        }

        long platformDeploymentId = claims.get().getPayload().get("platformDeploymentId", Long.class);
        LMSOAuthService<?> lmsoAuthService = lmsoAuthServiceManager.getLMSOAuthService(platformDeploymentId);

        String userKey = claims.get().getPayload().get("userId", String.class);
        LtiUserEntity user = ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(userKey, platformDeploymentId);

        try {
            lmsoAuthService.fetchAndSaveAccessToken(user, code);
        } catch (LMSOAuthException e) {
            model.addAttribute(TextConstants.ERROR, MessageFormat.format("Error getting LMS API access token: {0}", e.getMessage()));
            return TextConstants.OAUTH2_ERROR;
        }

        String oneTimeToken = createOneTimeToken(platformDeploymentId, userKey, claims.get().getPayload());

        return "redirect:/app/app.html?token=" + oneTimeToken;
    }

    private String createOneTimeToken(long platformDeploymentId, String userKey, Claims claims) throws GeneralSecurityException, IOException {
        return apijwtService.buildJwt(
                true,
                claims.get("roles", List.class),
                claims.get("contextId", Long.class),
                platformDeploymentId,
                userKey,
                claims.get("assignmentId", Long.class),
                claims.get("experimentId", Long.class),
                claims.get("consent", Boolean.class),
                claims.get("canvasUserId", String.class),
                claims.get("canvasUserGlobalId", String.class),
                claims.get("canvasLoginId", String.class),
                claims.get("canvasUserName", String.class),
                claims.get("canvasCourseId", String.class),
                claims.get("canvasAssignmentId", String.class),
                claims.get("dueAt", String.class),
                claims.get("lockAt", String.class),
                claims.get("unlockAt", String.class),
                claims.get("nonce", String.class),
                claims.get("allowedAttempts", Integer.class),
                claims.get("studentAttempts", Integer.class));
    }

}
