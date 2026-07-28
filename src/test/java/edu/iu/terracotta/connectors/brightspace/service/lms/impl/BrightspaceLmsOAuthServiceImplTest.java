package edu.iu.terracotta.connectors.brightspace.service.lms.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockConstruction;
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
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownContentTypeException;

import edu.iu.terracotta.connectors.brightspace.dao.model.api.BrightspaceApiToken;
import edu.iu.terracotta.connectors.brightspace.dao.model.api.BrightspaceApiUser;
import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiOAuthSettings;
import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiTokenEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.repository.api.ApiOAuthSettingsRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.api.ApiTokenRepository;
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;
import edu.iu.terracotta.connectors.generic.service.api.ApiScopeService;

public class BrightspaceLmsOAuthServiceImplTest {

    @Mock private ApiTokenRepository apiTokenRepository;
    @Mock private ApiOAuthSettingsRepository apiOAuthSettingsRepository;
    @Mock private ApiScopeService apiScopeService;
    @Mock private RestTemplate restTemplate;

    private PlatformDeployment platformDeployment;
    private LtiUserEntity user;
    private ApiOAuthSettings apiOAuthSettings;

    private BrightspaceLmsOAuthServiceImpl brightspaceLmsOAuthService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        brightspaceLmsOAuthService = Mockito.spy(new BrightspaceLmsOAuthServiceImpl(apiTokenRepository, apiOAuthSettingsRepository, apiScopeService));
        doReturn(restTemplate).when(brightspaceLmsOAuthService).createRestTemplate();

        platformDeployment = PlatformDeployment.builder()
            .keyId(1L)
            .localUrl("https://terracotta.example.com")
            .build();

        user = LtiUserEntity.builder()
            .userId(1L)
            .platformDeployment(platformDeployment)
            .build();

        apiOAuthSettings = ApiOAuthSettings.builder()
            .clientId("clientId")
            .clientSecret("clientSecret")
            .oauth2AuthUrl("https://brightspace.example.com/authorize")
            .oauth2TokenUrl("https://brightspace.example.com/token")
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

    private BrightspaceApiToken refreshedToken() {
        BrightspaceApiToken token = new BrightspaceApiToken();
        token.setAccessToken("new-access-token");
        token.setRefreshToken("new-refresh-token");
        token.setExpiresIn(3600);

        return token;
    }

    private BrightspaceApiUser whoamiUser() {
        BrightspaceApiUser brightspaceApiUser = new BrightspaceApiUser();
        brightspaceApiUser.setIdentifier("d2l-user-1");
        brightspaceApiUser.setUniqueName("jdoe");

        return brightspaceApiUser;
    }

    // postToWhoami() always builds its own `new RestTemplate()` rather than delegating to the
    // overridable createRestTemplate(), so the whoami HTTP call can only be intercepted in a unit
    // test by mocking construction of RestTemplate itself.
    private MockedConstruction<RestTemplate> mockWhoamiResponse(ResponseEntity<BrightspaceApiUser> response) {
        return mockConstruction(
            RestTemplate.class,
            (mock, context) -> when(mock.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(BrightspaceApiUser.class))).thenReturn(response)
        );
    }

    private MockedConstruction<RestTemplate> mockWhoamiThrows(RuntimeException exception) {
        return mockConstruction(
            RestTemplate.class,
            (mock, context) -> when(mock.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(BrightspaceApiUser.class))).thenThrow(exception)
        );
    }

    private UnknownContentTypeException redirectException(Class<?> targetType, String query) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("https://brightspace.example.com/redirect?" + query));

        return new UnknownContentTypeException(targetType, MediaType.TEXT_HTML, 302, "Found", headers, new byte[0]);
    }

    private UnknownContentTypeException nonRedirectException(Class<?> targetType, int status, String body) {
        return new UnknownContentTypeException(targetType, MediaType.TEXT_HTML, status, "Error", new HttpHeaders(), body.getBytes(StandardCharsets.UTF_8));
    }

    private HttpClientErrorException httpStatusCodeException(int status, String body) {
        return new HttpClientErrorException(HttpStatusCode.valueOf(status), "Error", new HttpHeaders(), body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    @Test
    public void testGetAccessTokenReturnsFreshTokenWithoutRefreshing() throws LmsOAuthException {
        ApiTokenEntity fresh = freshToken();
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.of(fresh));

        ApiTokenEntity result = brightspaceLmsOAuthService.getAccessToken(user);

        assertEquals("fresh-access-token", result.getAccessToken());
        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), any());
        verify(apiTokenRepository, never()).save(any(ApiTokenEntity.class));
    }

    @Test
    public void testGetAccessTokenRefreshesStaleToken() throws LmsOAuthException {
        ApiTokenEntity stale = staleToken();
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.of(stale));
        when(apiTokenRepository.save(any(ApiTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenReturn(ResponseEntity.ok(refreshedToken()));

        ApiTokenEntity result = brightspaceLmsOAuthService.getAccessToken(user);

        assertEquals("new-access-token", result.getAccessToken());
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), any());
        verify(apiTokenRepository, times(1)).save(any(ApiTokenEntity.class));
    }

    @Test
    public void testGetAccessTokenThrowsWhenNoTokenExists() {
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThrows(LmsOAuthException.class, () -> brightspaceLmsOAuthService.getAccessToken(user));
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

            return ResponseEntity.ok(refreshedToken());
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
                    return brightspaceLmsOAuthService.getAccessToken(user);
                }),
                executor.submit(() -> {
                    ready.countDown();
                    go.await();
                    return brightspaceLmsOAuthService.getAccessToken(user);
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
        // refreshed via Brightspace; the other should have reused the result once it got the lock
        assertEquals(1, refreshCount.get());
        verify(restTemplate, times(1)).postForEntity(anyString(), any(HttpEntity.class), any());
    }

    @Test
    public void testIsConfiguredReturnsTrueWhenSettingsPresent() {
        assertTrue(brightspaceLmsOAuthService.isConfigured(platformDeployment));
    }

    @Test
    public void testIsConfiguredReturnsFalseWhenSettingsAbsent() {
        PlatformDeployment other = PlatformDeployment.builder().keyId(2L).build();
        when(apiOAuthSettingsRepository.findByPlatformDeployment(other)).thenReturn(Optional.empty());

        assertFalse(brightspaceLmsOAuthService.isConfigured(other));
    }

    @Test
    public void testGetAuthorizationRequestURIBuildsExpectedURI() throws Exception {
        when(apiScopeService.getNecessaryScopes(1L, " ")).thenReturn("scope1 scope2");

        String uri = brightspaceLmsOAuthService.getAuthorizationRequestURI(platformDeployment, "xyz-state");

        assertTrue(uri.startsWith("https://brightspace.example.com/authorize?"));
        assertTrue(uri.contains("client_id=clientId"));
        assertTrue(uri.contains("response_type=code"));
        assertTrue(uri.contains("state=xyz-state"));
        assertTrue(uri.contains("redirect_uri="));
        assertTrue(uri.contains("scope=scope1%20scope2"));
    }

    @Test
    public void testGetAuthorizationRequestURIThrowsWhenSettingsMissing() {
        PlatformDeployment other = PlatformDeployment.builder().keyId(2L).build();
        when(apiOAuthSettingsRepository.findByPlatformDeployment(other)).thenReturn(Optional.empty());

        assertThrows(LmsOAuthException.class, () -> brightspaceLmsOAuthService.getAuthorizationRequestURI(other, "xyz-state"));
    }

    @Test
    public void testFetchAndSaveAccessTokenSuccessDeletesExistingToken() throws Exception {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenReturn(ResponseEntity.ok(refreshedToken()));
        when(apiScopeService.getNecessaryScopes(1L, " ")).thenReturn("scope1 scope2");

        ApiTokenEntity existing = freshToken();
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.of(existing));
        when(apiTokenRepository.save(any(ApiTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiTokenEntity result;

        try (MockedConstruction<RestTemplate> _ = mockWhoamiResponse(ResponseEntity.ok(whoamiUser()))) {
            result = brightspaceLmsOAuthService.fetchAndSaveAccessToken(user, "auth-code");
        }

        assertEquals("new-access-token", result.getAccessToken());
        assertEquals("new-refresh-token", result.getRefreshToken());
        assertEquals("d2l-user-1", result.getLmsUserId());
        assertEquals("jdoe", result.getLmsUserName());
        assertEquals("scope1 scope2", result.getScopes());
        verify(apiTokenRepository, times(1)).delete(existing);
        verify(apiTokenRepository, times(1)).save(any(ApiTokenEntity.class));
    }

    @Test
    public void testFetchAndSaveAccessTokenSuccessNoExistingToken() throws Exception {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenReturn(ResponseEntity.ok(refreshedToken()));
        when(apiScopeService.getNecessaryScopes(1L, " ")).thenReturn("scope1 scope2");
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.empty());
        when(apiTokenRepository.save(any(ApiTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiTokenEntity result;

        try (MockedConstruction<RestTemplate> _ = mockWhoamiResponse(ResponseEntity.ok(whoamiUser()))) {
            result = brightspaceLmsOAuthService.fetchAndSaveAccessToken(user, "auth-code");
        }

        assertEquals("new-access-token", result.getAccessToken());
        verify(apiTokenRepository, never()).delete(any(ApiTokenEntity.class));
    }

    @Test
    public void testFetchAndSaveAccessTokenThrowsWhenSettingsMissing() {
        PlatformDeployment other = PlatformDeployment.builder().keyId(2L).localUrl("https://terracotta.example.com").build();
        LtiUserEntity otherUser = LtiUserEntity.builder().userId(2L).platformDeployment(other).build();
        when(apiOAuthSettingsRepository.findByPlatformDeployment(other)).thenReturn(Optional.empty());

        assertThrows(LmsOAuthException.class, () -> brightspaceLmsOAuthService.fetchAndSaveAccessToken(otherUser, "auth-code"));
    }

    @Test
    public void testFetchAndSaveAccessTokenThrowsWhenTokenResponseNonSuccessStatus() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenReturn(ResponseEntity.status(400).body(new BrightspaceApiToken()));

        assertThrows(LmsOAuthException.class, () -> brightspaceLmsOAuthService.fetchAndSaveAccessToken(user, "auth-code"));
    }

    @Test
    public void testFetchAndSaveAccessTokenThrowsOnTokenRedirectError() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
            .thenThrow(redirectException(BrightspaceApiToken.class, "error=invalid_grant&error_description=expired_code"));

        LmsOAuthException ex = assertThrows(LmsOAuthException.class, () -> brightspaceLmsOAuthService.fetchAndSaveAccessToken(user, "auth-code"));

        assertTrue(ex.getMessage().contains("invalid_grant"));
        assertTrue(ex.getMessage().contains("expired_code"));
    }

    @Test
    public void testFetchAndSaveAccessTokenThrowsOnTokenNonRedirectError() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
            .thenThrow(nonRedirectException(BrightspaceApiToken.class, 400, "bad token request"));

        LmsOAuthException ex = assertThrows(LmsOAuthException.class, () -> brightspaceLmsOAuthService.fetchAndSaveAccessToken(user, "auth-code"));

        assertEquals("bad token request", ex.getMessage());
    }

    @Test
    public void testFetchAndSaveAccessTokenThrowsWhenWhoamiNonSuccessStatus() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenReturn(ResponseEntity.ok(refreshedToken()));

        try (MockedConstruction<RestTemplate> _ = mockWhoamiResponse(ResponseEntity.status(500).body(new BrightspaceApiUser()))) {
            assertThrows(LmsOAuthException.class, () -> brightspaceLmsOAuthService.fetchAndSaveAccessToken(user, "auth-code"));
        }
    }

    @Test
    public void testFetchAndSaveAccessTokenThrowsOnWhoamiRedirectError() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenReturn(ResponseEntity.ok(refreshedToken()));

        try (MockedConstruction<RestTemplate> _ = mockWhoamiThrows(redirectException(BrightspaceApiUser.class, "error=access_denied&error_description=no_whoami"))) {
            LmsOAuthException ex = assertThrows(LmsOAuthException.class, () -> brightspaceLmsOAuthService.fetchAndSaveAccessToken(user, "auth-code"));

            assertTrue(ex.getMessage().contains("access_denied"));
            assertTrue(ex.getMessage().contains("no_whoami"));
        }
    }

    @Test
    public void testFetchAndSaveAccessTokenThrowsOnWhoamiNonRedirectError() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenReturn(ResponseEntity.ok(refreshedToken()));

        try (MockedConstruction<RestTemplate> _ = mockWhoamiThrows(nonRedirectException(BrightspaceApiUser.class, 400, "bad whoami request"))) {
            LmsOAuthException ex = assertThrows(LmsOAuthException.class, () -> brightspaceLmsOAuthService.fetchAndSaveAccessToken(user, "auth-code"));

            assertEquals("bad whoami request", ex.getMessage());
        }
    }

    @Test
    public void testFetchAndSaveAccessTokenThrowsOnTokenHttpStatusCodeException() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
            .thenThrow(httpStatusCodeException(400, "token http status code error"));

        LmsOAuthException ex = assertThrows(LmsOAuthException.class, () -> brightspaceLmsOAuthService.fetchAndSaveAccessToken(user, "auth-code"));

        assertEquals("token http status code error", ex.getMessage());
    }

    @Test
    public void testFetchAndSaveAccessTokenThrowsOnWhoamiHttpStatusCodeException() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenReturn(ResponseEntity.ok(refreshedToken()));

        try (MockedConstruction<RestTemplate> _ = mockWhoamiThrows(httpStatusCodeException(401, "whoami http status code error"))) {
            LmsOAuthException ex = assertThrows(LmsOAuthException.class, () -> brightspaceLmsOAuthService.fetchAndSaveAccessToken(user, "auth-code"));

            assertEquals("whoami http status code error", ex.getMessage());
        }
    }

    @Test
    public void testGetAccessTokenRefreshThrowsWhenNonSuccessStatus() {
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.of(staleToken()));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenReturn(ResponseEntity.status(400).body(new BrightspaceApiToken()));

        assertThrows(LmsOAuthException.class, () -> brightspaceLmsOAuthService.getAccessToken(user));
    }

    @Test
    public void testGetAccessTokenRefreshThrowsOnRedirectError() {
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.of(staleToken()));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
            .thenThrow(redirectException(BrightspaceApiToken.class, "error=invalid_grant&error_description=expired_refresh_token"));

        LmsOAuthException ex = assertThrows(LmsOAuthException.class, () -> brightspaceLmsOAuthService.getAccessToken(user));

        assertTrue(ex.getMessage().contains("invalid_grant"));
        assertTrue(ex.getMessage().contains("expired_refresh_token"));
    }

    @Test
    public void testGetAccessTokenRefreshThrowsOnNonRedirectError() {
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.of(staleToken()));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
            .thenThrow(nonRedirectException(BrightspaceApiToken.class, 401, "unauthorized refresh"));

        LmsOAuthException ex = assertThrows(LmsOAuthException.class, () -> brightspaceLmsOAuthService.getAccessToken(user));

        assertEquals("unauthorized refresh", ex.getMessage());
    }

    @Test
    public void testIsAccessTokenAvailableReturnsFalseWhenNoToken() {
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.empty());

        assertFalse(brightspaceLmsOAuthService.isAccessTokenAvailable(user));
        verify(apiScopeService, never()).getNecessaryScopes(anyLong());
    }

    @Test
    public void testIsAccessTokenAvailableReturnsFalseWhenScopesMissing() {
        ApiTokenEntity token = ApiTokenEntity.builder()
            .accessToken("stale-access-token")
            .refreshToken("refresh-token")
            .expiresAt(Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .scopes("scope1")
            .user(user)
            .build();
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.of(token));
        // production code mutates this set in place (allNecessaryScopes.removeAll(tokenScopes)), so an
        // immutable Set.of() here would throw UnsupportedOperationException instead of exercising the branch
        when(apiScopeService.getNecessaryScopes(1L)).thenReturn(new HashSet<>(Set.of("scope1", "scope2")));

        assertFalse(brightspaceLmsOAuthService.isAccessTokenAvailable(user));
        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), any());
    }

    @Test
    public void testIsAccessTokenAvailableReturnsTrueWhenRefreshSucceeds() {
        ApiTokenEntity token = ApiTokenEntity.builder()
            .accessToken("stale-access-token")
            .refreshToken("refresh-token")
            .expiresAt(Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .scopes("scope1 scope2")
            .user(user)
            .build();
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.of(token));
        when(apiScopeService.getNecessaryScopes(1L)).thenReturn(Set.of("scope1", "scope2"));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenReturn(ResponseEntity.ok(refreshedToken()));
        when(apiTokenRepository.save(any(ApiTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertTrue(brightspaceLmsOAuthService.isAccessTokenAvailable(user));
    }

    @Test
    public void testIsAccessTokenAvailableReturnsFalseWhenRefreshFails() {
        ApiTokenEntity token = ApiTokenEntity.builder()
            .accessToken("stale-access-token")
            .refreshToken("refresh-token")
            .expiresAt(Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .scopes("scope1 scope2")
            .user(user)
            .build();
        when(apiTokenRepository.findByUser(user)).thenReturn(Optional.of(token));
        when(apiScopeService.getNecessaryScopes(1L)).thenReturn(Set.of("scope1", "scope2"));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any()))
            .thenThrow(nonRedirectException(BrightspaceApiToken.class, 400, "refresh failed"));

        assertFalse(brightspaceLmsOAuthService.isAccessTokenAvailable(user));
    }

    @Test
    public void testCreateRestTemplateReturnsRestTemplateInstance() {
        BrightspaceLmsOAuthServiceImpl plain = new BrightspaceLmsOAuthServiceImpl(apiTokenRepository, apiOAuthSettingsRepository, apiScopeService);

        assertNotNull(plain.createRestTemplate());
    }

}
