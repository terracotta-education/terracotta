package edu.iu.terracotta.service.common;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

import edu.iu.terracotta.exceptions.LMSOAuthException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.oauth2.APIToken;
import edu.iu.terracotta.utils.lti.LTI3Request;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

// TODO: document
public interface LMSOAuthService<T extends APIToken> {

    public boolean isConfigured(PlatformDeployment platformDeployment);

    public String getAuthorizationRequestURI(PlatformDeployment platformDeployment, String state)
            throws LMSOAuthException;

    public T fetchAndSaveAccessToken(LtiUserEntity user, String code) throws LMSOAuthException;

    public T getAccessToken(LtiUserEntity user) throws LMSOAuthException;

    public boolean isAccessTokenAvailable(LtiUserEntity user);
}
