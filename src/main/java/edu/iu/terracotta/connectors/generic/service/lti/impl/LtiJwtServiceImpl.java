package edu.iu.terracotta.connectors.generic.service.lti.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.Jwts.SIG;
import lombok.extern.slf4j.Slf4j;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.connectors.generic.service.lti.LtiDataService;
import edu.iu.terracotta.connectors.generic.service.lti.LtiJwtService;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;
import edu.iu.terracotta.utils.oauth.OAuthUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PublicKey;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * This manages all the data processing for the LTIRequest (and for LTI in general)
 * Necessary to get appropriate TX handling and service management
 */
@Slf4j
@Service
public class LtiJwtServiceImpl implements LtiJwtService {

    @Autowired private PlatformDeploymentRepository platformDeploymentRepository;
    @Autowired private LtiDataService ltiDataService;

    @Value("${app.token.logging.enabled:true}")
    private boolean tokenLoggingEnabled;

    /**
     * This will check that the state has been signed by us and retrieve the issuer private key.
     * We could add here other checks if we want (like the expiration of the state, nonce used only once, etc...)
     */
    //Here we could add other checks like expiration of the state (not implemented)
    @Override
    public Jws<Claims> validateState(String state) {
        return Jwts.parser().keyLocator(
            // This is done because each state is signed with a different key based on the issuer... so
            // we don't know the key and we need to check it pre-extracting the claims and finding the kid
            new Locator<Key>() {
                @Override
                public Key locate(Header header) {
                    if (header instanceof JwsHeader) {
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

                    return null;
                }
            }
        )
        .build()
        .parseSignedClaims(state);

        // If we are on this point, then the state signature has been validated. We can start other tasks now.
    }

    /**
     * We will just check that it is a valid signed JWT from the issuer. The logic later will decide if we
     * want to do what is asking or not. I'm not checking permissions here, that will happen later.
     * We could do other checks here, like comparing some values with the state
     * that just make us sure about the JWT being valid...
     */
    @Override
    public Jws<Claims> validateJWT(String jwt, String clientId) {
        return Jwts.parser().keyLocator(
            // This is done because each state is signed with a different key based on the issuer... so
            // we don't know the key and we need to check it pre-extracting the claims and finding the kid
            new Locator<Key>() {
                @Override
                public Key locate(Header header) {
                    if (header instanceof JwsHeader) {
                        JwsHeader jwsHeader = (JwsHeader) header;
                        PlatformDeployment platformDeployment;

                        try {
                            // We are dealing with RS256 encryption, so we have some Oauth utils to manage the keys and
                            // convert them to keys from the string stored in DB. There are for sure other ways to manage this.

                            String[] jwtSections = jwt.split("\\.");
                            String jwtPayload = new String(Base64.getUrlDecoder().decode(jwtSections[1]));
                            Map<String, Object> jwtClaims = null;

                            try {
                                jwtClaims = new ObjectMapper().readValue(jwtPayload, new TypeReference<Map<String,Object>>() {});
                            } catch (JsonProcessingException e) {
                                throw new IllegalStateException("Request is not a valid LTI3 request.", e);
                            }

                            String issuer = (String) jwtClaims.get(LtiStrings.ISS);
                            platformDeployment = platformDeploymentRepository.findByIssAndClientId(issuer, clientId).get(0);
                        } catch (IndexOutOfBoundsException ex) {
                            log.error("kid not found in header", ex);
                            return null;
                        }

                        // If the platform has a JWK Set endpoint... we try that.
                        if (StringUtils.isNoneEmpty(platformDeployment.getJwksEndpoint())) {
                            try {
                                JWKSet publicKeys = JWKSet.load(new URI(platformDeployment.getJwksEndpoint()).toURL());
                                JWK jwk = publicKeys.getKeyByKeyId(jwsHeader.getKeyId());
                                return ((AsymmetricJWK) jwk).toPublicKey();
                            } catch (JOSEException | ParseException | IOException ex) {
                                log.error("Error getting the iss public key", ex);
                                return null;
                            } catch (NullPointerException ex) {
                                log.error("Kid not found in header", ex);
                                return null;
                            } catch (URISyntaxException e) {
                                log.error("The platform configuration must contain a Jwks endpoint");
                                return null;
                            }
                        }

                        // If not, we get the key stored in our configuration
                        log.error("The platform configuration must contain a valid JWKS");
                        return null;
                    }

                    return null;
                }
            }
        )
        .build()
        .parseSignedClaims(jwt);
    }

    /**
     * This JWT will contain the token request
     */
    @Override
    public String generateTokenRequestJWT(PlatformDeployment platformDeployment) throws GeneralSecurityException, IOException {
        Date date = new Date();
        String aud;

        // D2L needs a different aud, maybe others too
        if (platformDeployment.getOAuth2TokenAud() != null) {
            aud = platformDeployment.getOAuth2TokenAud();
        } else {
            aud = platformDeployment.getOAuth2TokenUrl();
        }

        String state = Jwts.builder()
            .header()
            .add(LtiStrings.KID, TextConstants.DEFAULT_KID)
            .add(LtiStrings.TYP, LtiStrings.JWT)
            .and()
            .issuer(platformDeployment.getClientId())  // D2L needs the issuer to be the clientId
            .subject(platformDeployment.getClientId()) // The clientId
            .audience()
            .add(aud)  //We send here the authToken url.
            .and()
            .expiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
            .notBefore(date) //a java.util.Date
            .issuedAt(date) // for example, now
            .claim(LtiStrings.JTI, UUID.randomUUID().toString())  //This is an specific claim to ask for tokens.
            .signWith(OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey()), SIG.RS256)  //We sign it with our own private key. The platform has the public one.
            .compact();

        if (tokenLoggingEnabled) {
            log.debug("Token Request: \n {} \n", state);
        }

        return state;
    }

}
