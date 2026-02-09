package edu.iu.terracotta.connectors.brightspace.service.lms.impl;

import java.net.URI;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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

import edu.iu.terracotta.connectors.brightspace.dao.model.api.BrightspaceApiToken;
import edu.iu.terracotta.connectors.brightspace.dao.model.api.BrightspaceApiUser;
import edu.iu.terracotta.connectors.brightspace.io.model.enums.api.BrightspaceUrl;
import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiOAuthSettings;
import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiTokenEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.repository.api.ApiOAuthSettingsRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.api.ApiTokenRepository;
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;
import edu.iu.terracotta.connectors.generic.service.api.ApiScopeService;
import edu.iu.terracotta.connectors.generic.service.lms.LmsOAuthService;
import edu.iu.terracotta.dao.exceptions.FeatureNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@TerracottaConnector(LmsConnector.BRIGHTSPACE)
@SuppressWarnings({"PMD.GuardLogStatement", "PMD.LooseCoupling"})
public class BrightspaceLmsOAuthServiceImpl implements LmsOAuthService<ApiTokenEntity> {

    @Autowired private ApiTokenRepository apiTokenRepository;
    @Autowired private ApiOAuthSettingsRepository apiOAuthSettingsRepository;
    @Autowired private ApiScopeService apiScopeService;

    @Value("${brightspace.api.lp.version:1.52}")
    private String brightspaceApiLpVersion;

    @Override
    public boolean isConfigured(PlatformDeployment platformDeployment) {
        return apiOAuthSettingsRepository.findByPlatformDeployment(platformDeployment).isPresent();
    }

    @Override
    public String getAuthorizationRequestURI(PlatformDeployment platformDeployment, String state) throws LmsOAuthException, FeatureNotFoundException {
        ApiOAuthSettings apiOAuthSettings = getApiOAuthSettings(platformDeployment);

        return UriComponentsBuilder.fromUriString(apiOAuthSettings.getOauth2AuthUrl())
            .queryParam("client_id", apiOAuthSettings.getClientId())
            .queryParam("response_type", "code")
            .queryParam("redirect_uri", getRedirectURI(platformDeployment.getLocalUrl()))
            .queryParam("state", state)
            .queryParam("scope", apiScopeService.getNecessaryScopes(platformDeployment.getKeyId(), " "))
            .encode()
            .build()
            .toUriString();
    }

    @Override
    public ApiTokenEntity fetchAndSaveAccessToken(LtiUserEntity user, String code) throws LmsOAuthException, FeatureNotFoundException {
        ApiOAuthSettings apiOAuthSettings = getApiOAuthSettings(user.getPlatformDeployment());

        // Create x-www-form-urlencoded POST request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "authorization_code");
        map.add("client_id", apiOAuthSettings.getClientId());
        map.add("client_secret", apiOAuthSettings.getClientSecret());
        map.add("redirect_uri", getRedirectURI(user.getPlatformDeployment().getLocalUrl()));
        map.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        BrightspaceApiToken token = postToTokenURL(request, user);
        BrightspaceApiUser brightspaceApiUser = postToWhoami(user, token);
        Optional<ApiTokenEntity> savedToken = apiTokenRepository.findByUser(user);

        savedToken.ifPresent(aToken -> {
            apiTokenRepository.delete(aToken);
        });

        ApiTokenEntity newTokenEntity = ApiTokenEntity.builder()
            .accessToken(token.getAccessToken())
            .expiresAt(new Timestamp(Instant.now().toEpochMilli() + token.getExpiresIn() * 1000))
            .lmsConnector(apiOAuthSettings.getPlatformDeployment().getLmsConnector())
            .lmsUserId(brightspaceApiUser.getIdentifier())
            .lmsUserName(brightspaceApiUser.getUniqueName())
            .refreshToken(token.getRefreshToken())
            .scopes(
                apiScopeService.getNecessaryScopes(user.getPlatformDeployment().getKeyId(), " ")
            )
            .user(user)
            .build();

        return apiTokenRepository.save(newTokenEntity);
    }

    @Override
    public ApiTokenEntity getAccessToken(LtiUserEntity user) throws LmsOAuthException {
        Optional<ApiTokenEntity> brightspaceApiTokenEntity = apiTokenRepository.findByUser(user);

        if (brightspaceApiTokenEntity.isEmpty()) {
            throw new LmsOAuthException(MessageFormat.format("User {0} does not have a Brightspace API access token nor refresh token!", user.getUserKey()));
        }

        if (isAccessTokenFresh(brightspaceApiTokenEntity.get())) {
            return brightspaceApiTokenEntity.get();
        }

        return refreshAccessToken(brightspaceApiTokenEntity.get());
    }

    @Override
    public boolean isAccessTokenAvailable(LtiUserEntity user) {
        Optional<ApiTokenEntity> brightspaceApiTokenEntity = apiTokenRepository.findByUser(user);

        if (brightspaceApiTokenEntity.isEmpty()) {
            return false;
        }

        // check that token's scopes include all necessary scopes (default required and feature-required)
        Set<String> allNecessaryScopes = apiScopeService.getNecessaryScopes(user.getPlatformDeployment().getKeyId());
        Set<String> tokenScopes = brightspaceApiTokenEntity.get().getScopesAsSet();

        if (tokenScopes == null || !tokenScopes.containsAll(allNecessaryScopes)) {
            allNecessaryScopes.removeAll(tokenScopes);
            log.info("Token [{}] is missing necessary scopes. Has [{}] but needs [{}]", brightspaceApiTokenEntity.get().getTokenId(), tokenScopes, allNecessaryScopes);

            return false; // need to get a new token with all necessary scopes
        }

        // if exists, refresh and save the token, return true
        try {
            refreshAccessToken(brightspaceApiTokenEntity.get());

            return true;
        } catch (LmsOAuthException e) {
            log.error(MessageFormat.format("Failed to refresh token {0}", brightspaceApiTokenEntity.get().getTokenId()), e);

            return false;
        }
    }

    private ApiTokenEntity refreshAccessToken(ApiTokenEntity brightspaceApiTokenEntity) throws LmsOAuthException {
        ApiOAuthSettings brightspaceAPIOAuthSettings = getApiOAuthSettings(brightspaceApiTokenEntity.getUser().getPlatformDeployment());

        // Create x-www-form-urlencoded POST request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "refresh_token");
        map.add("client_id", brightspaceAPIOAuthSettings.getClientId());
        map.add("client_secret", brightspaceAPIOAuthSettings.getClientSecret());
        map.add("redirect_uri", getRedirectURI(brightspaceApiTokenEntity.getUser().getPlatformDeployment().getLocalUrl()));
        map.add("refresh_token", brightspaceApiTokenEntity.getRefreshToken());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        BrightspaceApiToken tokenResponse = postToTokenURL(request, brightspaceApiTokenEntity.getUser());
        brightspaceApiTokenEntity.setAccessToken(tokenResponse.getAccessToken());
        brightspaceApiTokenEntity.setRefreshToken(tokenResponse.getRefreshToken());
        brightspaceApiTokenEntity.setExpiresAt(new Timestamp(Instant.now().toEpochMilli() + tokenResponse.getExpiresIn() * 1000));

        return apiTokenRepository.save(brightspaceApiTokenEntity);
    }

    private BrightspaceApiToken postToTokenURL(HttpEntity<MultiValueMap<String, String>> request, LtiUserEntity user) throws LmsOAuthException {
        ApiOAuthSettings apiOAuthSettings = getApiOAuthSettings(user.getPlatformDeployment());
        RestTemplate restTemplate = createRestTemplate();

        try {
            ResponseEntity<BrightspaceApiToken> response = restTemplate.postForEntity(
                apiOAuthSettings.getOauth2TokenUrl(),
                request,
                BrightspaceApiToken.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (UnknownContentTypeException unknownContentTypeException) {
            if (unknownContentTypeException.getStatusCode().value() == 302) {

                URI location = unknownContentTypeException.getResponseHeaders().getLocation();
                String queryParameters = location.getQuery();
                Map<String, String> paramMap = new HashMap<>();

                for (String queryParam : queryParameters.split("&")) {
                    String[] nameValue = queryParam.split("=");
                    paramMap.put(nameValue[0], nameValue[1]);
                }

                throw new LmsOAuthException(
                        MessageFormat.format("Error getting access token: error: {0}, error_description: {1}",
                                paramMap.get("error"), paramMap.get("error_description")),
                        unknownContentTypeException);
            } else {
                throw new LmsOAuthException(unknownContentTypeException.getResponseBodyAsString(),
                        unknownContentTypeException);
            }
        }

        throw new LmsOAuthException(MessageFormat.format("Could not fetch token for user {0}", user.getUserId()));
    }

    private BrightspaceApiUser postToWhoami(LtiUserEntity user, BrightspaceApiToken token) throws LmsOAuthException {
        RestTemplate restTemplate = new RestTemplate();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token.getAccessToken());

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<BrightspaceApiUser> response = restTemplate.exchange(
                String.format(
                    BrightspaceUrl.WHOAMI.url(),
                    user.getPlatformDeployment().getBaseUrl(),
                    brightspaceApiLpVersion
                ),
                HttpMethod.GET,
                entity,
                BrightspaceApiUser.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (UnknownContentTypeException unknownContentTypeException) {
            if (unknownContentTypeException.getStatusCode().value() == 302) {

                URI location = unknownContentTypeException.getResponseHeaders().getLocation();
                String queryParameters = location.getQuery();
                Map<String, String> paramMap = new HashMap<>();

                for (String queryParam : queryParameters.split("&")) {
                    String[] nameValue = queryParam.split("=");
                    paramMap.put(nameValue[0], nameValue[1]);
                }

                throw new LmsOAuthException(
                    MessageFormat.format(
                        "Error getting whoami user: error: {0}, error_description: {1}",
                        paramMap.get("error"),
                        paramMap.get("error_description")
                    ),
                    unknownContentTypeException
                );
            } else {
                throw new LmsOAuthException(unknownContentTypeException.getResponseBodyAsString(), unknownContentTypeException);
            }
        }

        throw new LmsOAuthException(MessageFormat.format("Could not fetch whoami for user {0}", user.getUserId()));
    }

    private String getRedirectURI(String localUrl) {
        return String.format("%s/lms/oauth2/oauth_response", localUrl);
    }

    @Override
    public RestTemplate createRestTemplate() {
        return new RestTemplate(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
    }

    private boolean isAccessTokenFresh(ApiTokenEntity brightspaceApiTokenEntity) {
        Timestamp expiresAt = brightspaceApiTokenEntity.getExpiresAt();
        // Add a buffer of 5 minutes just to be safe
        Instant fiveMinutesFromNow = Instant.now().plus(5, ChronoUnit.MINUTES);

        return expiresAt.after(Timestamp.from(fiveMinutesFromNow));
    }

    private ApiOAuthSettings getApiOAuthSettings(PlatformDeployment platformDeployment) throws LmsOAuthException {
        return apiOAuthSettingsRepository
            .findByPlatformDeployment(platformDeployment)
            .orElseThrow(() -> {
                return new LmsOAuthException("Could not find settings for Brightspace API OAuth");
            });
    }

}
