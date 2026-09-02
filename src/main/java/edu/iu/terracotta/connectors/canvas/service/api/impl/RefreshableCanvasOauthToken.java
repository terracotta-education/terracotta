package edu.iu.terracotta.connectors.canvas.service.api.impl;

import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.ksu.canvas.oauth.OauthToken;
import lombok.extern.slf4j.Slf4j;

/**
 * Unlike NonRefreshableOauthToken, this actually fetches a new access token when the Canvas SDK's
 * RefreshingRestClient calls refresh() after an InvalidOauthTokenException - e.g. when a long-running,
 * multi-page API call outlives the token's remaining lifetime, or when Canvas has invalidated the
 * token out-of-band (revoked access, rotated dev key, etc.) before our own cached expiry says it's due.
 * Passes the rejected access token back to the supplier so it can force an actual refresh_token call
 * instead of trusting a locally-cached expiry that Canvas has already disagreed with.
 */
@Slf4j
public class RefreshableCanvasOauthToken implements OauthToken {

    private static final long serialVersionUID = 1L;

    private final AccessTokenSupplier accessTokenSupplier;
    private String accessToken;

    public RefreshableCanvasOauthToken(AccessTokenSupplier accessTokenSupplier, String accessToken) {
        this.accessTokenSupplier = accessTokenSupplier;
        this.accessToken = accessToken;
    }

    @Override
    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public void refresh() {
        try {
            accessToken = accessTokenSupplier.get(accessToken);
        } catch (ApiException e) {
            throw new IllegalStateException("Failed to refresh Canvas API access token", e);
        }
    }

    @FunctionalInterface
    public interface AccessTokenSupplier {
        String get(String rejectedAccessToken) throws ApiException;
    }

}
