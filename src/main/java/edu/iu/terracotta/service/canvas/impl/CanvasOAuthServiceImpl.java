package edu.iu.terracotta.service.canvas.impl;

import java.net.URI;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import org.springframework.web.client.UnknownContentTypeException;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.exceptions.LMSOAuthException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.canvas.CanvasAPIOAuthSettings;
import edu.iu.terracotta.model.canvas.CanvasAPIToken;
import edu.iu.terracotta.model.canvas.CanvasAPITokenEntity;
import edu.iu.terracotta.repository.CanvasAPIOAuthSettingsRepository;
import edu.iu.terracotta.repository.CanvasAPITokenRepository;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.common.LMSOAuthService;
import edu.iu.terracotta.service.lti.LTIDataService;

@Service
public class CanvasOAuthServiceImpl implements LMSOAuthService<CanvasAPITokenEntity> {

    static final Logger log = LoggerFactory.getLogger(CanvasOAuthServiceImpl.class);

    @Autowired
    CanvasAPITokenRepository canvasAPITokenRepository;

    @Autowired
    CanvasAPIOAuthSettingsRepository canvasAPIOAuthSettingsRepository;

    @Autowired
    LTIDataService ltiDataService;

    @Autowired
    APIJWTService apijwtService;

    @Override
    public boolean isConfigured(PlatformDeployment platformDeployment) {
        Optional<CanvasAPIOAuthSettings> canvasAPIOAuthSettings = canvasAPIOAuthSettingsRepository
                .findByPlatformDeployment(platformDeployment);
        return canvasAPIOAuthSettings.isPresent();
    }

    @Override
    public String getAuthorizationRequestURI(PlatformDeployment platformDeployment, String state)
            throws LMSOAuthException {

        CanvasAPIOAuthSettings canvasAPIOAuthSettings = getCanvasAPIOAuthSettings(platformDeployment);
        String clientId = canvasAPIOAuthSettings.getClientId();
        String url = UriComponentsBuilder.fromUriString(canvasAPIOAuthSettings.getOauth2AuthUrl())
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", getRedirectURI())
                .queryParam("state", state)
                .queryParam("scope", getAllRequiredScopes())
                .encode()
                .build()
                .toUriString();
        return url;

    }

    String getAllRequiredScopes() {
        return String.join(" ", CanvasAPIClientImpl.SCOPES_REQUIRED);
    }

    Set<String> getAllRequiredScopesAsSet() {
        return new HashSet<>(CanvasAPIClientImpl.SCOPES_REQUIRED);
    }

    @Override
    public CanvasAPITokenEntity fetchAndSaveAccessToken(LtiUserEntity user, String code) throws LMSOAuthException {

        CanvasAPIOAuthSettings canvasAPIOAuthSettings = getCanvasAPIOAuthSettings(user.getPlatformDeployment());

        // Create x-www-form-urlencoded POST request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "authorization_code");
        map.add("client_id", canvasAPIOAuthSettings.getClientId());
        map.add("client_secret", canvasAPIOAuthSettings.getClientSecret());
        map.add("redirect_uri", getRedirectURI());
        map.add("code", code);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        CanvasAPIToken token = postToTokenURL(request, user);

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
        newToken.setScopes(getAllRequiredScopes());
        newToken.setUser(user);
        return canvasAPITokenRepository.save(newToken);
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

        // check that token's scopes include all required scopes
        Set<String> allRequiredScopes = this.getAllRequiredScopesAsSet();
        Set<String> tokenScopes = canvasAPIToken.get().getScopesAsSet();
        if (tokenScopes == null || !tokenScopes.containsAll(allRequiredScopes)) {
            allRequiredScopes.removeAll(tokenScopes);
            log.info("Token {} is missing required scopes. Has {} but needs {}", canvasAPIToken.get().getTokenId(),
                    tokenScopes, allRequiredScopes);
            return false; // need to get a new token with all required scopes
        }

        // if exists, refresh and save the token, return true
        try {
            refreshAccessToken(canvasAPIToken.get());
            return true;
        } catch (LMSOAuthException e) {
            log.error(MessageFormat.format("Failed to refresh token {0}", canvasAPIToken.get().getTokenId()), e);
            return false;
        }
    }

    private CanvasAPITokenEntity refreshAccessToken(CanvasAPITokenEntity canvasAPITokenEntity)
            throws LMSOAuthException {

        CanvasAPIOAuthSettings canvasAPIOAuthSettings = getCanvasAPIOAuthSettings(
                canvasAPITokenEntity.getUser().getPlatformDeployment());

        // Create x-www-form-urlencoded POST request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "refresh_token");
        map.add("client_id", canvasAPIOAuthSettings.getClientId());
        map.add("client_secret", canvasAPIOAuthSettings.getClientSecret());
        map.add("redirect_uri", getRedirectURI());
        map.add("refresh_token", canvasAPITokenEntity.getRefreshToken());
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        CanvasAPIToken tokenResponse = postToTokenURL(request, canvasAPITokenEntity.getUser());
        canvasAPITokenEntity.setAccessToken(tokenResponse.getAccessToken());
        canvasAPITokenEntity
                .setExpiresAt(new Timestamp(System.currentTimeMillis() + tokenResponse.getExpiresIn() * 1000));

        return canvasAPITokenRepository.save(canvasAPITokenEntity);
    }

    private CanvasAPIToken postToTokenURL(HttpEntity<MultiValueMap<String, String>> request,
            LtiUserEntity user) throws LMSOAuthException {

        CanvasAPIOAuthSettings canvasAPIOAuthSettings = getCanvasAPIOAuthSettings(user.getPlatformDeployment());

        RestTemplate restTemplate = createRestTemplate();
        try {
            ResponseEntity<CanvasAPIToken> response = restTemplate.postForEntity(
                    canvasAPIOAuthSettings.getOauth2TokenUrl(),
                    request,
                    CanvasAPIToken.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (UnknownContentTypeException unknownContentTypeException) {
            if (unknownContentTypeException.getRawStatusCode() == 302) {

                URI location = unknownContentTypeException.getResponseHeaders().getLocation();
                String queryParameters = location.getQuery();
                Map<String, String> paramMap = new HashMap<>();
                for (String queryParam : queryParameters.split("&")) {
                    String[] nameValue = queryParam.split("=");
                    paramMap.put(nameValue[0], nameValue[1]);
                }
                throw new LMSOAuthException(
                        MessageFormat.format("Error getting access token: error: {0}, error_description: {1}",
                                paramMap.get("error"), paramMap.get("error_description")),
                        unknownContentTypeException);
            } else {
                throw new LMSOAuthException(unknownContentTypeException.getResponseBodyAsString(),
                        unknownContentTypeException);
            }
        }
        throw new LMSOAuthException(MessageFormat.format("Could not fetch token for user {0}",
                user.getUserId()));
    }

    private String getRedirectURI() {
        return ltiDataService.getLocalUrl() + "/lms/oauth2/oauth_response";
    }

    RestTemplate createRestTemplate() {
        return new RestTemplate(
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
    }

    private boolean isAccessTokenFresh(CanvasAPITokenEntity canvasAPITokenEntity) {
        Timestamp expiresAt = canvasAPITokenEntity.getExpiresAt();
        // Add a buffer of 5 minutes just to be safe
        Instant fiveMinutesFromNow = Instant.now().plus(5, ChronoUnit.MINUTES);
        return expiresAt.after(Timestamp.from(fiveMinutesFromNow));
    }

    private CanvasAPIOAuthSettings getCanvasAPIOAuthSettings(PlatformDeployment platformDeployment)
            throws LMSOAuthException {
        CanvasAPIOAuthSettings canvasAPIOAuthSettings = canvasAPIOAuthSettingsRepository
                .findByPlatformDeployment(platformDeployment)
                .orElseThrow(() -> {
                    return new LMSOAuthException("Could not find settings for Canvas API OAuth");
                });
        return canvasAPIOAuthSettings;
    }
}
