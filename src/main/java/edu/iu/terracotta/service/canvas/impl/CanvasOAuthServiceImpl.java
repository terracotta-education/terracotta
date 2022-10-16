package edu.iu.terracotta.service.canvas.impl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.exceptions.LMSOAuthException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.canvas.CanvasAPIToken;
import edu.iu.terracotta.model.canvas.CanvasAPITokenEntity;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.CanvasAPITokenRepository;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.common.LMSOAuthService;
import edu.iu.terracotta.service.lti.LTIDataService;
import edu.iu.terracotta.utils.lti.LTI3Request;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

@Service
public class CanvasOAuthServiceImpl implements LMSOAuthService<CanvasAPITokenEntity> {

    static final Logger log = LoggerFactory.getLogger(CanvasOAuthServiceImpl.class);

    @Autowired
    CanvasAPITokenRepository canvasAPITokenRepository;

    @Autowired
    LTIDataService ltiDataService;

    @Autowired
    APIJWTService apijwtService;

    @Override
    public boolean isConfigured(PlatformDeployment platformDeployment) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public String createOAuthState(SecuredInfo securedInfo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getAuthorizationRequestURI(PlatformDeployment platformDeployment, String state) {

        // TODO: get client id and auth endpoint from database config
        // TODO: specify all of the necessary scopes
        String clientId = "202570000000000113";
        String url = UriComponentsBuilder.fromUriString("https://terracotta.instructure.com/login/oauth2/auth")
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", getRedirectURI())
                .queryParam("state", state)
                .build()
                .toUriString();
        return url;

    }

    @Override
    public Jws<Claims> validateState(String state) {
        return apijwtService.validateStateForAPITokenRequest(state);
    }

    @Override
    public CanvasAPITokenEntity requestAccessToken(LtiUserEntity user, String code) {
        // TODO: retrieve these from database
        String clientId = "202570000000000113";
        String clientSecret = "orqsVzvJzC8voMswY3nMtpA1yt6pNYJFoGUBJO69gnTLGD1TN9ldB5o4Eb5Bvjhh";
        String canvasOauth2TokenUrl = "https://terracotta.instructure.com/login/oauth2/token";

        // Create x-www-form-urlencoded POST request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "authorization_code");
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("redirect_uri", getRedirectURI());
        map.add("code", code);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        RestTemplate restTemplate = createRestTemplate();
        ResponseEntity<CanvasAPIToken> response = restTemplate.postForEntity(canvasOauth2TokenUrl, request,
                CanvasAPIToken.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            CanvasAPIToken token = response.getBody();

            Optional<CanvasAPITokenEntity> savedToken = canvasAPITokenRepository.findByUser(user);
            savedToken.ifPresent(aToken -> {
                canvasAPITokenRepository.delete(aToken);
            });
            CanvasAPITokenEntity newToken = new CanvasAPITokenEntity();
            newToken.setAccessToken(token.getAccessToken());
            newToken.setCanvasUserId(token.getUser().getId());
            newToken.setCanvasUserName(token.getUser().getName());
            newToken.setExpiresAt(new Timestamp(System.currentTimeMillis() + token.getExpiresIn() * 1000));
            newToken.setRefreshToken(token.getRefreshToken());
            newToken.setUser(user);
            return canvasAPITokenRepository.save(newToken);
        }
        // TODO: change to LMSOAuthException
        throw new RuntimeException(MessageFormat.format("Could not retrieve token from {0}", canvasOauth2TokenUrl));
    }

    @Override
    public CanvasAPITokenEntity refreshAccessToken(LtiUserEntity user) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CanvasAPITokenEntity getAccessToken(LtiUserEntity user) throws LMSOAuthException {

        Optional<CanvasAPITokenEntity> canvasAPIToken = canvasAPITokenRepository.findByUser(user);
        if (!canvasAPIToken.isPresent()) {
            throw new LMSOAuthException(
                    MessageFormat.format("User {0} does not have a Canvas API access token nor refresh token!",
                            user.getUserKey()));
        }

        if (isAccessTokenFresh(canvasAPIToken.get())) {
            return canvasAPIToken.get();
        } else {
            return refreshAccessToken(canvasAPIToken.get());
        }
    }

    @Override
    public boolean isAccessTokenAvailable(LtiUserEntity user) {

        Optional<CanvasAPITokenEntity> canvasAPIToken = canvasAPITokenRepository.findByUser(user);
        if (!canvasAPIToken.isPresent()) {
            return false;
        }

        // if exists, refresh and save the token, return true
        try {
            refreshAccessToken(canvasAPIToken.get());
            return true;
        } catch (Exception e) {
            log.error(MessageFormat.format("Failed to refresh token {0}", canvasAPIToken.get().getTokenId()), e);
            return false;
        }
    }

    @Override
    public String createOAuthState(LTI3Request lti3Request) throws GeneralSecurityException, IOException {
        return apijwtService.generateStateForAPITokenRequest(lti3Request);
    }

    private CanvasAPITokenEntity refreshAccessToken(CanvasAPITokenEntity canvasAPITokenEntity) {

        // TODO: get these from the database
        String clientId = "202570000000000113";
        String clientSecret = "orqsVzvJzC8voMswY3nMtpA1yt6pNYJFoGUBJO69gnTLGD1TN9ldB5o4Eb5Bvjhh";
        String canvasOauth2TokenUrl = "https://terracotta.instructure.com/login/oauth2/token";

        // Create x-www-form-urlencoded POST request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "refresh_token");
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("redirect_uri", getRedirectURI());
        map.add("refresh_token", canvasAPITokenEntity.getRefreshToken());
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        RestTemplate restTemplate = createRestTemplate();
        ResponseEntity<CanvasAPIToken> response = restTemplate.postForEntity(canvasOauth2TokenUrl, request,
                CanvasAPIToken.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            CanvasAPIToken tokenResponse = response.getBody();
            canvasAPITokenEntity.setAccessToken(tokenResponse.getAccessToken());
            canvasAPITokenEntity
                    .setExpiresAt(new Timestamp(System.currentTimeMillis() + tokenResponse.getExpiresIn() * 1000));

            return canvasAPITokenRepository.save(canvasAPITokenEntity);
        }
        throw new RuntimeException(MessageFormat.format("Could not refresh token for user {0}",
                canvasAPITokenEntity.getUser().getUserId()));
    }

    private String getRedirectURI() {
        // TODO: move to interface with default implementation?
        // TODO: use MvcUriComponentsBuilder
        // https://docs.spring.io/spring-framework/docs/5.2.22.RELEASE/spring-framework-reference/web.html#mvc-links-to-controllers
        return ltiDataService.getLocalUrl() + "/lms/oauth2/oauth_response";
    }

    private RestTemplate createRestTemplate() {
        return new RestTemplate(
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
    }

    private boolean isAccessTokenFresh(CanvasAPITokenEntity canvasAPITokenEntity) {
        Timestamp expiresAt = canvasAPITokenEntity.getExpiresAt();
        // Add a buffer of 5 minutes just to be safe
        Instant fiveMinutesFromNow = Instant.now().plus(5, ChronoUnit.MINUTES);
        return expiresAt.after(Timestamp.from(fiveMinutesFromNow));
    }
}
