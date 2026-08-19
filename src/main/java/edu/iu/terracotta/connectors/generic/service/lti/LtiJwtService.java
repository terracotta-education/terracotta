package edu.iu.terracotta.connectors.generic.service.lti;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

import java.io.IOException;
import java.security.GeneralSecurityException;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;

public interface LtiJwtService {

    Jws<Claims> validateState(String state);
    Jws<Claims> validateJWT(String jwt, String clientId);

    /**
     * Same signature verification as {@link #validateJWT(String, String)}, for callers that have
     * no independent way to already know the signer's clientId (e.g. an LTI Platform
     * Notification Service notice, which arrives with no accompanying state/session to source it
     * from). The clientId is instead read directly off the JWT's own (as yet unverified) "aud"
     * claim - safe here because it's only ever used to look up which PlatformDeployment's key to
     * verify the signature against; an attacker-supplied clientId simply fails signature
     * verification against the wrong key.
     */
    Jws<Claims> validateJWT(String jwt);
    String generateTokenRequestJWT(PlatformDeployment platformDeployment) throws GeneralSecurityException, IOException;

}
