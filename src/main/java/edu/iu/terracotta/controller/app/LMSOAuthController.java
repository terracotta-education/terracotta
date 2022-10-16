package edu.iu.terracotta.controller.app;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.repository.LtiUserRepository;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.common.LMSOAuthService;
import edu.iu.terracotta.service.common.LMSOAuthServiceManager;
import edu.iu.terracotta.service.lti.LTIDataService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

@Controller
@RequestMapping("/lms/oauth2")
public class LMSOAuthController {

    public static final String SESSION_LMS_OAUTH2_STATE = "lms_oauth2_state";

    private static final Logger log = LoggerFactory.getLogger(LMSOAuthController.class);

    @Autowired
    LTIDataService ltiDataService;

    @Autowired
    LMSOAuthServiceManager lmsoAuthServiceManager;

    @Autowired
    APIJWTService apijwtService;

    @Autowired
    LtiUserRepository ltiUserRepository;

    @GetMapping(value = "/oauth_response")
    public String handleOauthResponse(HttpServletRequest req) throws GeneralSecurityException, IOException {

        String code = req.getParameter("code");
        log.debug("/oauth_response: code={}", code);

        // Verify that state parameter matches session state
        String state = req.getParameter("state");
        log.debug("/oauth_response: state={}", state);

        String sessionState = (String) req.getSession().getAttribute(SESSION_LMS_OAUTH2_STATE);
        if (sessionState == null) {
            throw new IllegalStateException("OAuth2 request doesn't contains the expected state");
        }
        if (!sessionState.equals(state)) {
            throw new IllegalStateException("OAuth2 request doesn't contains the expected state");
        }
        Jws<Claims> claims = apijwtService.validateStateForAPITokenRequest(state);
        long platformDeploymentId = claims.getBody().get("platformDeploymentId", Long.class);
        LMSOAuthService<?> lmsoAuthService = lmsoAuthServiceManager.getLMSOAuthService(platformDeploymentId);

        String userKey = claims.getBody().get("userId", String.class);
        LtiUserEntity user = ltiUserRepository.findByUserKey(userKey);

        lmsoAuthService.requestAccessToken(user, code);

        String oneTimeToken = createOneTimeToken(platformDeploymentId, userKey, claims.getBody());
        return "redirect:/app/app.html?token=" + oneTimeToken;

    }

    private String createOneTimeToken(long platformDeploymentId, String userKey, Claims claims)
            throws GeneralSecurityException, IOException {
        String oneTimeToken = apijwtService.buildJwt(
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
                claims.get("nonce", String.class));
        return oneTimeToken;
    }
}
