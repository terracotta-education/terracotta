package edu.iu.terracotta.controller.app;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.connectors.generic.service.lms.LmsOAuthService;
import edu.iu.terracotta.connectors.generic.service.lms.LmsOAuthServiceManager;
import edu.iu.terracotta.security.app.roles.InstructorOrHigher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Lets the authorization page in the LMS frame learn when the instructor has finished authorizing
 * Terracotta in the separate window it opened, since that window can't reliably message the frame.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping(value = LmsAuthorizationController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class LmsAuthorizationController {

    public static final String REQUEST_ROOT = "api/lms/authorization";

    private final ApiJwtService apijwtService;
    private final LmsOAuthServiceManager lmsOAuthServiceManager;
    private final LtiUserRepository ltiUserRepository;

    @GetMapping
    @InstructorOrHigher
    public ResponseEntity<Map<String, Boolean>> getAuthorization(HttpServletRequest req) throws TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        LtiUserEntity user = ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        LmsOAuthService<?> lmsOAuthService = lmsOAuthServiceManager.getLmsOAuthService(securedInfo.getPlatformDeploymentId());

        boolean authorized = user != null
            && (!lmsOAuthService.isConfigured(user.getPlatformDeployment()) || lmsOAuthService.isAccessTokenAvailable(user));

        return new ResponseEntity<>(Map.of("authorized", authorized), HttpStatus.OK);
    }

}
