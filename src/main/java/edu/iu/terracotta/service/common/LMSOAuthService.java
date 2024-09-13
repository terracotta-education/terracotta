package edu.iu.terracotta.service.common;

import org.springframework.web.client.RestTemplate;

import edu.iu.terracotta.exceptions.LMSOAuthException;
import edu.iu.terracotta.exceptions.app.FeatureNotFoundException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.oauth2.APIToken;

/**
 * Interface for an LMS that provides OAuth2 API tokens for authorizing with its
 * API. Initially implemented to get Canvas OAuth2 API tokens.
 */
public interface LMSOAuthService<T extends APIToken> {

    /**
     * Returns true if this {@link PlatformDeployment} is configured for getting
     * OAuth2 API tokens.
     *
     * @param platformDeployment
     * @return
     */
    boolean isConfigured(PlatformDeployment platformDeployment);

    /**
     * Return the authorization request URI that user agent will be redirect to to
     * authorize token grant.
     *
     * @param platformDeployment
     * @param state
     * @return
     * @throws LMSOAuthException if not configured or is missing configuration for
     *                           OAuth2 API tokens
     * @throws FeatureNotFoundException
     */
    String getAuthorizationRequestURI(PlatformDeployment platformDeployment, String state) throws LMSOAuthException, FeatureNotFoundException;

    /**
     * Exchange the code for an access token and refresh token. Save and return the
     * persisted access token.
     *
     * @param user
     * @param code
     * @return
     * @throws LMSOAuthException if fails to fetch access token from LMS
     * @throws FeatureNotFoundException
     */
    T fetchAndSaveAccessToken(LtiUserEntity user, String code) throws LMSOAuthException, FeatureNotFoundException;

    /**
     * Get the access token that is saved for this user. Or, if the access token is
     * expired, refresh the access token and return it.
     *
     * @param user
     * @return
     * @throws LMSOAuthException if user has not saved access token or if refreshing
     *                           the token fails
     */
    T getAccessToken(LtiUserEntity user) throws LMSOAuthException;

    /**
     * Return true if the user has an access token. This method should verify that
     * the token hasn't been revoked, for example, by refreshing the token (even if
     * the token is not expired).
     *
     * @param user
     * @return
     */
    boolean isAccessTokenAvailable(LtiUserEntity user);

    RestTemplate createRestTemplate();

}
