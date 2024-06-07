package edu.iu.terracotta.utils.lti;

import edu.iu.terracotta.service.lti.LTIDataService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import lombok.extern.slf4j.Slf4j;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.lti.dto.LoginInitiationDTO;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;
import edu.iu.terracotta.utils.oauth.OAuthUtils;
import org.apache.commons.lang3.time.DateUtils;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.Map;

@Slf4j
public final class LtiOidcUtils {

    private LtiOidcUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * The state will be returned when the tool makes the final call to us, so it is useful to send information
     * to our own tool, to know about the request.
     */
    public static String generateState(LTIDataService ltiDataService, PlatformDeployment platformDeployment, Map<String, String> authRequestMap, LoginInitiationDTO loginInitiationDTO, String clientIdValue, String deploymentIdValue, boolean verboseLogging) throws GeneralSecurityException, IOException {
        Date date = new Date();
        String state = Jwts.builder()
            .header()
            .add(LtiStrings.KID, TextConstants.DEFAULT_KID)
            .add(LtiStrings.TYP, LtiStrings.JWT)
            .and()
            .issuer("ltiStarter")  // This is our own identifier, to know that we are the issuer.
            .subject(platformDeployment.getIss()) // We store here the platform issuer to check that matches with the issuer received later
            .audience()
            .add(platformDeployment.getClientId())  // We send here the clientId to check it later.
            .and()
            .expiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
            .notBefore(date) // a java.util.Date
            .issuedAt(date) // for example, now
            .id(authRequestMap.get("nonce")) // just a nonce... we don't use it by the moment, but it could be good if we store information about the requests in DB.
            .claim("original_iss", loginInitiationDTO.getIss())  // All this claims are the information received in the OIDC initiation and some other useful things.
            .claim("loginHint", loginInitiationDTO.getLoginHint())
            .claim("ltiMessageHint", loginInitiationDTO.getLtiMessageHint())
            .claim("targetLinkUri", loginInitiationDTO.getTargetLinkUri())
            .claim("clientId", clientIdValue)
            .claim("ltiDeploymentId", deploymentIdValue)
            .claim("controller", "/oidc/login_initiations")
            .signWith(OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey()), SIG.RS256)  // We sign it
            .compact();

        if (verboseLogging) {
            log.debug("State: \n {} \n", state);
        }

        return state;
    }

}
