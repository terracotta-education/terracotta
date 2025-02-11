package edu.iu.terracotta.connectors.generic.service.lti;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

import java.io.IOException;
import java.security.GeneralSecurityException;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;

public interface LtiJwtService {

    Jws<Claims> validateState(String state);
    Jws<Claims> validateJWT(String jwt, String clientId);
    String generateTokenRequestJWT(PlatformDeployment platformDeployment) throws GeneralSecurityException, IOException;

}
