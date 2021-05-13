/**
 * Copyright 2021 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.model.oauth2.Roles;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import edu.iu.terracotta.service.app.APIDataService;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.lti.LTIDataService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import edu.iu.terracotta.utils.oauth.OAuthUtils;
import edu.iu.terracotta.utils.TextConstants;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PublicKey;
import java.util.Date;
import java.util.List;

/**
 * This manages all the data processing for the LTIRequest (and for LTI in general)
 * Necessary to get appropriate TX handling and service management
 */
@Service
public class APIJWTServiceImpl implements APIJWTService {

    static final Logger log = LoggerFactory.getLogger(APIJWTServiceImpl.class);

    @Autowired
    LTIDataService ltiDataService;

    @Autowired
    APIDataService apiDataService;

    private static final String JWT_REQUEST_HEADER_NAME = "Authorization";
    private static final String JWT_BEARER_TYPE = "Bearer";
    private static final String QUERY_PARAM_NAME = "token";

    String error;

    /**
     * This will check that the state has been signed by us and retrieve the issuer private key.
     * We could add here other checks if we want (like the expiration of the state, nonce used only once, etc...)
     */
    //Here we could add other checks like expiration of the state (not implemented)
    @Override
    public Jws<Claims> validateToken(String token) {
        return Jwts.parser().setSigningKeyResolver(new SigningKeyResolverAdapter() {
            // This is done because each state is signed with a different key based on the issuer... so
            // we don't know the key and we need to check it pre-extracting the claims and finding the kid
            @Override
            public Key resolveSigningKey(JwsHeader header, Claims claims) {
                PublicKey toolPublicKey;
                try {
                    // We are dealing with RS256 encryption, so we have some Oauth utils to manage the keys and
                    // convert them to keys from the string stored in DB. There are for sure other ways to manage this.
                    toolPublicKey = OAuthUtils.loadPublicKey(ltiDataService.getOwnPublicKey());
                } catch (GeneralSecurityException ex) {
                    log.error("Error validating the state. Error generating the tool public key", ex);
                    return null;
                }
                return toolPublicKey;
            }
        }).parseClaimsJws(token);
        // If we are on this point, then the state signature has been validated. We can start other tasks now.
    }




    /**
     * This JWT will contain the token request
     */
    @Override
    public String buildJwt(boolean oneUse, List<String> roles, Long contextId, Long platformDeploymentId, String userId) throws GeneralSecurityException, IOException {

        int length = 3600;
        //We only allow 30 seconds (surely we can low that) for the one time token, because that one must be traded
        // immediately
        if (oneUse){
            length = 300; //TODO, change this test value to 30
        }
        Date date = new Date();
        Key toolPrivateKey = OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey());
        JwtBuilder builder = Jwts.builder()
                .setHeaderParam("kid", TextConstants.DEFAULT_KID)
                .setHeaderParam("typ", "JWT")
                .setIssuer("TERRACOTTA")
                .setSubject(userId) // The clientId
                .setAudience(ltiDataService.getLocalUrl())  //We send here the authToken url.
                .setExpiration(DateUtils.addSeconds(date, length)) //a java.util.Date
                .setNotBefore(date) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .claim("contextId", contextId)  //This is an specific claim to ask for tokens.
                .claim("platformDeploymentId", platformDeploymentId)  //This is an specific claim to ask for tokens.
                .claim("userId", userId)  //This is an specific claim to ask for tokens.
                .claim("roles", roles)
                .claim("oneUse", oneUse)  //This is an specific claim to ask for tokens.
                .signWith(SignatureAlgorithm.RS256, toolPrivateKey);  //We sign it with our own private key. The platform has the public one.
        String token = builder.compact();
        if (oneUse){
            apiDataService.addOneUseToken(token);
        }
        log.debug("Token Request: \n {} \n", token);
        return token;
    }

    @Override
    public String refreshToken(String token) throws GeneralSecurityException, IOException, BadTokenException {
        int length = 3600;
        Jws<Claims> tokenClaims = validateToken(token);
        if (tokenClaims.getBody().get("oneUse").equals("true")){
            throw new BadTokenException("Trying to refresh an one use token");
        }
        Date date = new Date();
        Key toolPrivateKey = OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey());
        JwtBuilder builder = Jwts.builder()
                .setHeaderParam("kid", tokenClaims.getHeader().getKeyId())
                .setHeaderParam("typ", "JWT")
                .setIssuer(tokenClaims.getBody().getIssuer())
                .setSubject(tokenClaims.getBody().getSubject()) // The clientId
                .setAudience(tokenClaims.getBody().getAudience())  //We send here the authToken url.
                .setExpiration(DateUtils.addDays(date, length)) //a java.util.Date
                .setNotBefore(date) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .claim("something",tokenClaims.getBody().get("something"))  //This is an specific claim to ask for tokens.
                .claim("roles", tokenClaims.getBody().get("roles"))  //This is an specific claim to ask for tokens.
                .claim("oneUse", false)  //This is an specific claim to ask for tokens.
                .signWith(SignatureAlgorithm.RS256, toolPrivateKey);  //We sign it with our own private key. The platform has the public one.
        String newToken = builder.compact();
        log.debug("Token Request: \n {} \n", newToken);
        return newToken;
    }

    @Override
    public String extractJwtStringValue(HttpServletRequest request, boolean allowQueryParam) {
        String rawHeaderValue = StringUtils.trimAllWhitespace(request.getHeader(JWT_REQUEST_HEADER_NAME));
        if (rawHeaderValue == null) {
            if (allowQueryParam) {
                String param = StringUtils.trimAllWhitespace(request.getParameter(QUERY_PARAM_NAME));
                return param;
            }
        }
        if (rawHeaderValue == null) {
            return null;
        }
        // very similar to BearerTokenExtractor.java in Spring spring-security-oauth2
        if (isBearerToken(rawHeaderValue)) {
            String jwtValue = rawHeaderValue.substring(JWT_BEARER_TYPE.length()).trim();
            return jwtValue;
        }
        return null;
    }

    @Override
    public SecurityInfo extractValues(HttpServletRequest request, boolean allowQueryParam) {
        String token = extractJwtStringValue(request,allowQueryParam);
        Jws<Claims> claims = validateToken(token);
        if (claims != null) {
          SecurityInfo securityInfo = new SecurityInfo();
          securityInfo.setUserId(claims.getBody().get("userId").toString());
          securityInfo.setPlatformDeploymentId(Long.valueOf((Integer) claims.getBody().get("platformDeploymentId")));
          securityInfo.setContextId(Long.valueOf((Integer) claims.getBody().get("contextId")));
          securityInfo.setRoles((List<String>) claims.getBody().get("roles"));
          return securityInfo;
        } else {
          return null;
        }
    }

    @Override
    public boolean isAdmin(SecurityInfo securityInfo){
        return securityInfo.getRoles().contains(Roles.ADMIN);
    }

    @Override
    public boolean isInstructor(SecurityInfo securityInfo){
        return (securityInfo.getRoles().contains(Roles.INSTRUCTOR) || securityInfo.getRoles().contains(Roles.MEMBERSHIP_INSTRUCTOR));
    }

    @Override
    public boolean isLearner(SecurityInfo securityInfo){
        return (securityInfo.getRoles().contains(Roles.LEARNER) || securityInfo.getRoles().contains(Roles.MEMBERSHIP_LEARNER));
    }

    @Override
    public boolean isGeneral(SecurityInfo securityInfo){
        return securityInfo.getRoles().contains(Roles.GENERAL);
    }

    @Override
    public boolean isInstructorOrHigher(SecurityInfo securityInfo){
        return (isInstructor(securityInfo) || isAdmin(securityInfo));
    }

    @Override
    public boolean isLearnerOrHigher(SecurityInfo securityInfo){
        return (isLearner(securityInfo) || isInstructorOrHigher(securityInfo));
    }

    private boolean isBearerToken(String rawHeaderValue) {
        return rawHeaderValue.toLowerCase().startsWith(JWT_BEARER_TYPE.toLowerCase());
    }

}
