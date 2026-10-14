package edu.iu.terracotta.connectors.generic.service.api.impl;

import java.security.GeneralSecurityException;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;

import edu.iu.terracotta.connectors.generic.dao.model.enums.jwt.JwtClaim;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;
import edu.iu.terracotta.utils.oauth.OAuthUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;

/**
 * The {@code state} sent with an LMS OAuth2 authorization request. The LMS keeps it in the user's
 * session until it redirects back, and Canvas's session lives in a size-limited cookie, so it holds
 * only what LmsOAuthController needs (whose token this is) rather than the whole launch token: a
 * state that pushes the cookie past the browser's limit is silently dropped, and Canvas then
 * reports "Must submit new OAuth2 request".
 */
public final class LmsOAuthState {

    // long enough to log in to the LMS and read its authorization page
    private static final int LIFETIME_SECONDS = 3600;

    private LmsOAuthState() {
    }

    public static String build(String privateKey, String issuer, long platformDeploymentId, String userKey) throws GeneralSecurityException {
        Date now = new Date();

        return Jwts.builder()
            .header()
            .add(LtiStrings.KID, TextConstants.DEFAULT_KID)
            .and()
            .issuer(issuer)
            .expiration(DateUtils.addSeconds(now, LIFETIME_SECONDS))
            .issuedAt(now)
            .claim(JwtClaim.PLATFORM_DEPLOYMENT_ID.key(), platformDeploymentId)
            .claim(JwtClaim.USER_ID.key(), userKey)
            .signWith(OAuthUtils.loadPrivateKey(privateKey), SIG.RS256)
            .compact();
    }

}
