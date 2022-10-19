package edu.iu.terracotta.service.common;

import java.io.IOException;
import java.security.GeneralSecurityException;

import edu.iu.terracotta.exceptions.LMSOAuthException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.oauth2.APIToken;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.utils.lti.LTI3Request;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

// TODO: document
public interface LMSOAuthService<T extends APIToken> {

    public boolean isConfigured(PlatformDeployment platformDeployment);

    // TODO: maybe don't need this
    public String createOAuthState(SecuredInfo securedInfo);

    public String createOAuthState(LTI3Request lti3Request) throws GeneralSecurityException, IOException;

    public String getAuthorizationRequestURI(PlatformDeployment platformDeployment, String state)
            throws LMSOAuthException;

    // TODO: don't need this here
    public Jws<Claims> validateState(String state);

    public T fetchAndSaveAccessToken(LtiUserEntity user, String code) throws LMSOAuthException;

    public T refreshAccessToken(LtiUserEntity user);

    public T getAccessToken(LtiUserEntity user) throws LMSOAuthException;

    public boolean isAccessTokenAvailable(LtiUserEntity user);
}
