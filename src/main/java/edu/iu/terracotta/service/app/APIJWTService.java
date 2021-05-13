package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public interface APIJWTService {
    //Here we could add other checks like expiration of the state (not implemented)
    Jws<Claims> validateToken(String token);

    String buildJwt(boolean oneUse, List<String> roles, Long contextId, Long platformDeploymentId, String userId) throws GeneralSecurityException, IOException;

    String refreshToken(String token) throws GeneralSecurityException, IOException, BadTokenException;

    String extractJwtStringValue(HttpServletRequest request, boolean allowQueryParam);

    SecurityInfo extractValues(HttpServletRequest request, boolean allowQueryParam);

    boolean isAdmin(SecurityInfo securityInfo);

    boolean isInstructor(SecurityInfo securityInfo);

    boolean isInstructorOrHigher(SecurityInfo securityInfo);

    boolean isLearner(SecurityInfo securityInfo);

    boolean isLearnerOrHigher(SecurityInfo securityInfo);

    boolean isGeneral(SecurityInfo securityInfo);
}
