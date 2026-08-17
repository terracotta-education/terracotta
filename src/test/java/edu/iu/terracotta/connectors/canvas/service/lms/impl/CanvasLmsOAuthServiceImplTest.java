package edu.iu.terracotta.connectors.canvas.service.lms.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownContentTypeException;

import edu.iu.terracotta.connectors.canvas.dao.model.api.CanvasApiToken;
import edu.iu.terracotta.connectors.canvas.dao.model.api.CanvasApiUser;
import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiOAuthSettings;
import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiTokenEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.repository.api.ApiOAuthSettingsRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.api.ApiTokenRepository;
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;
import edu.iu.terracotta.connectors.generic.service.api.ApiScopeService;
import edu.iu.terracotta.dao.exceptions.FeatureNotFoundException;

public class CanvasLmsOAuthServiceImplTest {

    @Mock private ApiTokenRepository apiTokenRepository;
    @Mock private ApiOAuthSettingsRepository apiOAuthSettingsRepository;
    @Mock private ApiScopeService apiScopeService;
    @Mock private RestTemplate restTemplate;

    private PlatformDeployment platformDeployment;
    private LtiUserEntity user;
    private ApiOAuthSettings apiOAuthSettings;

    private CanvasLmsOAuthServiceImpl canvasLmsOAuthService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        canvasLmsOAuthService = Mockito.spy(new CanvasLmsOAuthServiceImpl(apiTokenRepository, apiOAuthSettingsRepository, apiScopeService));
        doReturn(restTemplate).when(canvasLmsOAuthService).createRestTemplate();

        platformDeployment = PlatformDeployment.builder()
            .keyId(1L)
            .localUrl("https://terracotta.example.com")
            .lmsConnector(LmsConnector.CANVAS)
            .build();

        user = LtiUserEntity.builder()
            .userId(1L)
            .userKey("user-key")
            .platformDeployment(platformDeployment)
            .build();

        apiOAuthSettings = ApiOAuthSettings.builder()
            .clientId("clientId")
            .clientSecret("clientSecret")
            .oauth2AuthUrl("https://canvas.example.com/auth")
            .oauth2TokenUrl("https://canvas.example.com/token")
            .platformDeployment(platformDeployment)
            .build();

        when(apiOAuthSettingsRepository.findByPlatformDeployment(platformDeployment)).thenReturn(Optional.of(apiOAuthSettings));
    }

    private ApiTokenEntity freshToken() {
        return ApiTokenEntity.builder()
            .accessToken("fresh-access-token")
            .refreshToken("refresh-token")
            .expiresAt(Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .user(user)
            .build();
    }

    private ApiTokenEntity staleToken() {
        return ApiTokenEntity.builder()
            .accessToken("stale-access-token")
            .refreshToken("refresh-token")
            .expiresAt(Timestamp.from(Instant.now().minus(1, ChronoUnit.MINUTES)))
            .user(user)
            .build();
    }

    @Test
    public void testGetAccessTokenReturnsFreshTokenWithoutRefreshing() throws LmsOAuthException {
        ApiTokenEntity fresh = freshToken();
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.of(fresh));

        ApiTokenEntity result = canvasLmsOAuthService.getAccessToken(user);

        assertEquals("fresh-access-token", result.getAccessToken());
        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), any());
        verify(apiTokenRepository, never()).save(any(ApiTokenEntity.class));
    }

    @Test
    public void testGetAccessTokenRefreshesStaleToken() throws LmsOAuthException {
        ApiTokenEntity stale = staleToken();
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.of(stale));
        when(apiTokenRepository.save(any(ApiTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CanvasApiToken refreshedToken = CanvasApiToken.builder()
            .accessToken("new-access-token")
            .refreshToken("new-refresh-token")
            .expiresIn(3600)
            .build();
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenReturn(ResponseEntity.ok(refreshedToken));

        ApiTokenEntity result = canvasLmsOAuthService.getAccessToken(user);

        assertEquals("new-access-token", result.getAccessToken());
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), any());
        verify(apiTokenRepository, times(1)).save(any(ApiTokenEntity.class));
    }

    @Test
    public void testGetAccessTokenThrowsWhenNoTokenExists() {
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThrows(LmsOAuthException.class, () -> canvasLmsOAuthService.getAccessToken(user));
    }

    @Test
    public void testGetAccessTokenConcurrentCallsOnlyRefreshOnce() throws Exception {
        // a single mutable ApiTokenEntity backs every findByUser() call, so once one thread's
        // refresh saves a fresh expiry, the other thread's double-check under the lock sees it
        ApiTokenEntity token = staleToken();
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.of(token));

        AtomicInteger refreshCount = new AtomicInteger(0);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenAnswer(invocation -> {
            refreshCount.incrementAndGet();
            // simulate a slow network round trip so both threads are stale before either refreshes
            Thread.sleep(200);

            return ResponseEntity.ok(
                CanvasApiToken.builder()
                    .accessToken("new-access-token")
                    .refreshToken("new-refresh-token")
                    .expiresIn(3600)
                    .build()
            );
        });
        when(apiTokenRepository.save(any(ApiTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        try {
            var futures = java.util.List.of(
                executor.submit(() -> {
                    ready.countDown();
                    go.await();
                    return canvasLmsOAuthService.getAccessToken(user);
                }),
                executor.submit(() -> {
                    ready.countDown();
                    go.await();
                    return canvasLmsOAuthService.getAccessToken(user);
                })
            );

            ready.await();
            go.countDown();

            for (var future : futures) {
                ApiTokenEntity result = future.get(5, TimeUnit.SECONDS);
                assertEquals("new-access-token", result.getAccessToken());
            }
        } finally {
            executor.shutdown();
        }

        // both callers saw a stale token concurrently, but only one should have actually
        // refreshed via Canvas; the other should have reused the result once it got the lock
        assertEquals(1, refreshCount.get());
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), any());
    }

    private CanvasApiToken canvasApiTokenWithUser() {
        return CanvasApiToken.builder()
            .accessToken("new-access-token")
            .refreshToken("new-refresh-token")
            .expiresIn(3600)
            .user(
                CanvasApiUser.builder()
                    .id(42L)
                    .name("Jane Doe")
                    .build()
            )
            .build();
    }

    @Test
    public void testIsConfiguredReturnsTrueWhenSettingsExist() {
        assertTrue(canvasLmsOAuthService.isConfigured(platformDeployment));
    }

    @Test
    public void testIsConfiguredReturnsFalseWhenSettingsMissing() {
        PlatformDeployment other = PlatformDeployment.builder().keyId(2L).build();

        assertFalse(canvasLmsOAuthService.isConfigured(other));
    }

    @Test
    public void testGetAuthorizationRequestURIBuildsExpectedUri() throws LmsOAuthException, FeatureNotFoundException {
        when(apiScopeService.getNecessaryScopes(1L, " ")).thenReturn("scope1 scope2");

        String uri = canvasLmsOAuthService.getAuthorizationRequestURI(platformDeployment, "state-value");

        assertTrue(uri.startsWith("https://canvas.example.com/auth"));
        assertTrue(uri.contains("client_id=clientId"));
        assertTrue(uri.contains("response_type=code"));
        assertTrue(uri.contains("state=state-value"));
        assertTrue(uri.contains("redirect_uri="));
        assertTrue(uri.contains("scope=scope1"));
    }

    @Test
    public void testGetAuthorizationRequestURIThrowsWhenSettingsMissing() {
        PlatformDeployment other = PlatformDeployment.builder().keyId(2L).localUrl("https://terracotta.example.com").build();

        assertThrows(LmsOAuthException.class, () -> canvasLmsOAuthService.getAuthorizationRequestURI(other, "state-value"));
    }

    @Test
    public void testFetchAndSaveAccessTokenDeletesExistingTokenAndSavesNew() throws LmsOAuthException, FeatureNotFoundException {
        ApiTokenEntity existing = freshToken();
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.of(existing));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenReturn(ResponseEntity.ok(canvasApiTokenWithUser()));
        when(apiScopeService.getNecessaryScopes(1L, " ")).thenReturn("scope1 scope2");
        when(apiTokenRepository.save(any(ApiTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiTokenEntity result = canvasLmsOAuthService.fetchAndSaveAccessToken(user, "auth-code");

        assertEquals("new-access-token", result.getAccessToken());
        assertEquals("new-refresh-token", result.getRefreshToken());
        assertEquals("42", result.getLmsUserId());
        assertEquals("Jane Doe", result.getLmsUserName());
        assertEquals("scope1 scope2", result.getScopes());
        assertEquals(LmsConnector.CANVAS, result.getLmsConnector());
        verify(apiTokenRepository, times(1)).delete(existing);
        verify(apiTokenRepository, times(1)).save(any(ApiTokenEntity.class));
    }

    @Test
    public void testFetchAndSaveAccessTokenWhenNoExistingTokenDoesNotDelete() throws LmsOAuthException, FeatureNotFoundException {
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.empty());
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenReturn(ResponseEntity.ok(canvasApiTokenWithUser()));
        when(apiScopeService.getNecessaryScopes(1L, " ")).thenReturn("scope1 scope2");
        when(apiTokenRepository.save(any(ApiTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiTokenEntity result = canvasLmsOAuthService.fetchAndSaveAccessToken(user, "auth-code");

        assertEquals("new-access-token", result.getAccessToken());
        verify(apiTokenRepository, never()).delete(any(ApiTokenEntity.class));
    }

    @Test
    public void testFetchAndSaveAccessTokenThrowsWhenSettingsMissing() {
        LtiUserEntity otherUser = LtiUserEntity.builder()
            .userId(2L)
            .platformDeployment(PlatformDeployment.builder().keyId(2L).localUrl("https://terracotta.example.com").build())
            .build();

        assertThrows(LmsOAuthException.class, () -> canvasLmsOAuthService.fetchAndSaveAccessToken(otherUser, "auth-code"));
    }

    @Test
    public void testFetchAndSaveAccessTokenThrowsWhenTokenResponseNotSuccessful() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class))).thenReturn(new ResponseEntity<CanvasApiToken>(HttpStatus.BAD_REQUEST));

        assertThrows(LmsOAuthException.class, () -> canvasLmsOAuthService.fetchAndSaveAccessToken(user, "auth-code"));
    }

    @Test
    public void testFetchAndSaveAccessTokenThrowsWithErrorDetailsOn302Redirect() {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("https://canvas.example.com/login?error=invalid_grant&error_description=Refresh+token+not+found"));
        UnknownContentTypeException redirectException = new UnknownContentTypeException(
            CanvasApiToken.class,
            MediaType.TEXT_HTML,
            HttpStatus.FOUND,
            "Found",
            headers,
            new byte[0]
        );
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenThrow(redirectException);

        LmsOAuthException thrown = assertThrows(LmsOAuthException.class, () -> canvasLmsOAuthService.fetchAndSaveAccessToken(user, "auth-code"));

        assertTrue(thrown.getMessage().contains("invalid_grant"));
        assertTrue(thrown.getMessage().contains("Refresh+token+not+found"));
        assertEquals(redirectException, thrown.getCause());
    }

    @Test
    public void testFetchAndSaveAccessTokenThrowsWithBodyOnNonRedirectUnknownContentType() {
        byte[] body = "server exploded".getBytes(StandardCharsets.UTF_8);
        UnknownContentTypeException serverException = new UnknownContentTypeException(
            CanvasApiToken.class,
            MediaType.TEXT_PLAIN,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            new HttpHeaders(),
            body
        );
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenThrow(serverException);

        LmsOAuthException thrown = assertThrows(LmsOAuthException.class, () -> canvasLmsOAuthService.fetchAndSaveAccessToken(user, "auth-code"));

        assertEquals(serverException.getResponseBodyAsString(), thrown.getMessage());
        assertEquals(serverException, thrown.getCause());
    }

    @Test
    public void testGetAccessTokenThrowsWhenRefreshFails() {
        ApiTokenEntity stale = staleToken();
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.of(stale));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class))).thenReturn(new ResponseEntity<CanvasApiToken>(HttpStatus.BAD_REQUEST));

        assertThrows(LmsOAuthException.class, () -> canvasLmsOAuthService.getAccessToken(user));
        verify(apiTokenRepository, never()).save(any(ApiTokenEntity.class));
    }

    @Test
    public void testIsAccessTokenAvailableReturnsFalseWhenNoTokenSaved() {
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.empty());

        assertFalse(canvasLmsOAuthService.isAccessTokenAvailable(user));
        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), any());
    }

    @Test
    public void testIsAccessTokenAvailableReturnsFalseWhenScopesMissing() {
        ApiTokenEntity token = freshToken();
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.of(token));
        when(apiScopeService.getNecessaryScopes(1L)).thenReturn(new HashSet<>(Set.of("scope1")));

        assertFalse(canvasLmsOAuthService.isAccessTokenAvailable(user));
        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), any());
    }

    @Test
    public void testIsAccessTokenAvailableReturnsTrueWithoutRefreshingWhenTokenFresh() {
        ApiTokenEntity token = ApiTokenEntity.builder()
            .accessToken("fresh-access-token")
            .refreshToken("refresh-token")
            .expiresAt(Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .scopes("scope1 scope2")
            .user(user)
            .build();
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.of(token));
        when(apiScopeService.getNecessaryScopes(1L)).thenReturn(Set.of("scope1", "scope2"));

        assertTrue(canvasLmsOAuthService.isAccessTokenAvailable(user));
        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), any());
        verify(apiTokenRepository, never()).save(any(ApiTokenEntity.class));
    }

    @Test
    public void testIsAccessTokenAvailableRefreshesAndReturnsTrueWhenScopesPresent() {
        ApiTokenEntity token = ApiTokenEntity.builder()
            .accessToken("stale-access-token")
            .refreshToken("refresh-token")
            .expiresAt(Timestamp.from(Instant.now().minus(1, ChronoUnit.MINUTES)))
            .scopes("scope1 scope2")
            .user(user)
            .build();
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.of(token));
        when(apiScopeService.getNecessaryScopes(1L)).thenReturn(Set.of("scope1", "scope2"));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenReturn(
            ResponseEntity.ok(
                CanvasApiToken.builder()
                    .accessToken("new-access-token")
                    .refreshToken("new-refresh-token")
                    .expiresIn(3600)
                    .build()
            )
        );
        when(apiTokenRepository.save(any(ApiTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertTrue(canvasLmsOAuthService.isAccessTokenAvailable(user));
        verify(apiTokenRepository, times(1)).save(any(ApiTokenEntity.class));
    }

    @Test
    public void testIsAccessTokenAvailableReturnsFalseWhenRefreshFails() {
        ApiTokenEntity token = ApiTokenEntity.builder()
            .accessToken("stale-access-token")
            .refreshToken("refresh-token")
            .expiresAt(Timestamp.from(Instant.now().minus(1, ChronoUnit.MINUTES)))
            .scopes("scope1 scope2")
            .user(user)
            .build();
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.of(token));
        when(apiScopeService.getNecessaryScopes(1L)).thenReturn(Set.of("scope1", "scope2"));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class))).thenReturn(new ResponseEntity<CanvasApiToken>(HttpStatus.BAD_REQUEST));

        assertFalse(canvasLmsOAuthService.isAccessTokenAvailable(user));
        verify(apiTokenRepository, never()).save(any(ApiTokenEntity.class));
    }

    @Test
    public void testCreateRestTemplateReturnsRestTemplateInstance() {
        CanvasLmsOAuthServiceImpl realService = new CanvasLmsOAuthServiceImpl(apiTokenRepository, apiOAuthSettingsRepository, apiScopeService);

        RestTemplate result = realService.createRestTemplate();

        assertNotNull(result);
    }

}
