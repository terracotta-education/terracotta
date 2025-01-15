package edu.iu.terracotta.connectors.generic.service.lms;

import org.springframework.web.client.RestTemplate;

import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;
import edu.iu.terracotta.dao.exceptions.FeatureNotFoundException;

/**
 * Interface for an LMS that provides OAuth2 API tokens for authorizing with its
 * API. Initially implemented to get LMS OAuth2 API tokens.
 */
@TerracottaConnector(LmsConnector.GENERIC)
public interface LmsOAuthService<T> {

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
    String getAuthorizationRequestURI(PlatformDeployment platformDeployment, String state) throws LmsOAuthException, FeatureNotFoundException;

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
    T fetchAndSaveAccessToken(LtiUserEntity user, String code) throws LmsOAuthException, FeatureNotFoundException;

    /**
     * Get the access token that is saved for this user. Or, if the access token is
     * expired, refresh the access token and return it.
     *
     * @param user
     * @return
     * @throws LMSOAuthException if user has not saved access token or if refreshing
     *                           the token fails
     */
    T getAccessToken(LtiUserEntity user) throws LmsOAuthException;

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
