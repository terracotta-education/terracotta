package edu.iu.terracotta.service.common;

import java.io.IOException;
import java.security.GeneralSecurityException;

import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.oauth2.APIToken;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.utils.lti.LTI3Request;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

// TODO: document
public interface LMSOAuthService {

    // TODO: maybe don't need this
    public String createOAuthState(SecuredInfo securedInfo);

    public String createOAuthState(LTI3Request lti3Request) throws GeneralSecurityException, IOException;

    public String getAuthorizationRequestURI(PlatformDeployment platformDeployment, String state);

    // TODO: don't need this here
    public Jws<Claims> validateState(String state);

    // TODO: rename to fetchAndSaveAccessToken
    public APIToken requestAccessToken(LtiUserEntity user, String code);

    public APIToken refreshAccessToken(LtiUserEntity user);

    public APIToken getAccessToken(LtiUserEntity user);

    public boolean isAccessTokenAvailable(LtiUserEntity user);
}
