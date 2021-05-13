package edu.iu.terracotta.service.lti;

import edu.iu.terracotta.model.PlatformDeployment;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

import java.io.IOException;
import java.security.GeneralSecurityException;

public interface LTIJWTService {
    //Here we could add other checks like expiration of the state (not implemented)
    Jws<Claims> validateState(String state);

    Jws<Claims> validateJWT(String jwt, String clientId);

    String generateTokenRequestJWT(PlatformDeployment platformDeployment) throws GeneralSecurityException, IOException;
}
