package edu.iu.terracotta.service.canvas.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.UnknownContentTypeException;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.BaseTest;
import edu.iu.terracotta.exceptions.LMSOAuthException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.canvas.CanvasAPIOAuthSettings;
import edu.iu.terracotta.model.canvas.CanvasAPIToken;
import edu.iu.terracotta.model.canvas.CanvasAPITokenEntity;
import edu.iu.terracotta.model.canvas.CanvasAPIUser;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.lti.LTIDataService;

public class CanvasOAuthServiceImplTest extends BaseTest {

    @Spy
    @InjectMocks
    private CanvasOAuthServiceImpl canvasOAuthService;

    @Mock private LTIDataService ltiDataService;
    @Mock private APIJWTService apijwtService;

    @Mock
    private PlatformDeployment platformDeployment;

    private CanvasAPIOAuthSettings canvasAPIOAuthSettings;
    private String clientId = "222220000000000111";
    private CanvasAPIToken token;
    private CanvasAPITokenEntity tokenEntity;
    private LtiUserEntity user;
    private String localUrl = "https://dev.terracotta.education";

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        canvasAPIOAuthSettings = new CanvasAPIOAuthSettings();
        canvasAPIOAuthSettings.setClientId(clientId);
        canvasAPIOAuthSettings.setOauth2AuthUrl("https://terracotta.instructure.com/login/oauth2/auth");
        canvasAPIOAuthSettings.setClientSecret("c8f5245e-72c9-41a3-9bdb-19ebc28a0612");
        canvasAPIOAuthSettings.setOauth2TokenUrl("https://terracotta.instructure.com/login/oauth2/token");

        token = new CanvasAPIToken();
        token.setAccessToken("1/fFAGRNJru1FTz70BzhT3Zg");
        token.setExpiresIn(3600);
        token.setRefreshToken("tIh2YBWGiC0GgGRglT9Ylwv2MnTvy8csfGyfK2PqZmkFYYqYZ0wui4tzI7uBwnN2");
        token.setUser(new CanvasAPIUser(42, "Jimi Hendrix"));

        user = new LtiUserEntity("userKey", new Date(), platformDeployment);
        user.setUserId(1032L);

        tokenEntity = new CanvasAPITokenEntity();
        tokenEntity.setAccessToken(token.getAccessToken());
        tokenEntity.setRefreshToken(token.getRefreshToken());
        tokenEntity.setUser(user);

        when(platformDeployment.getLocalUrl()).thenReturn(localUrl);
    }

    @Test
    public void testIsConfiguredWhenNotConfigured() {
        assertFalse(canvasOAuthService.isConfigured(platformDeployment));
    }

    @Test
    public void testIsConfiguredWhenIsConfigured() {
        when(canvasAPIOAuthSettingsRepository.findByPlatformDeployment(eq(platformDeployment)))
                .thenReturn(Optional.of(canvasAPIOAuthSettings));
        assertTrue(canvasOAuthService.isConfigured(platformDeployment));
    }

    @Test
    public void testGetAuthorizationRequestURI() throws LMSOAuthException, MalformedURLException, URISyntaxException {
        when(canvasAPIOAuthSettingsRepository.findByPlatformDeployment(eq(platformDeployment))).thenReturn(Optional.of(canvasAPIOAuthSettings));

        String state = "abc123";
        String url = canvasOAuthService.getAuthorizationRequestURI(platformDeployment, state);
        URL parsedURL = new URI(url).toURL();
        assertTrue(parsedURL.getQuery().contains(encodeQueryParam("client_id", clientId)));
        assertTrue(parsedURL.getQuery().contains(encodeQueryParam("response_type", "code")));
        assertTrue(parsedURL.getQuery().contains(encodeQueryParam("redirect_uri",localUrl + "/lms/oauth2/oauth_response")));
        assertTrue(parsedURL.getQuery().contains(encodeQueryParam("state", state)));
        assertTrue(parsedURL.getQuery().contains(encodeQueryParam("scope", canvasOAuthService.getAllRequiredScopes())));
    }

    @Test
    public void testFetchAndSaveAccessToken() throws LMSOAuthException {
        when(canvasAPIOAuthSettingsRepository.findByPlatformDeployment(eq(platformDeployment))).thenReturn(Optional.of(canvasAPIOAuthSettings));
        when(canvasOAuthService.createRestTemplate()).thenReturn(restTemplate);
        when(restTemplate.postForEntity(anyString(), any(), eq(CanvasAPIToken.class))).thenReturn(new ResponseEntity<CanvasAPIToken>(token, HttpStatus.OK));
        when(canvasAPITokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CanvasAPITokenEntity result = canvasOAuthService.fetchAndSaveAccessToken(user, "code");

        assertEquals(token.getAccessToken(), result.getAccessToken());
        assertEquals(token.getRefreshToken(), result.getRefreshToken());
        assertEquals(canvasOAuthService.getAllRequiredScopes(), result.getScopes());
        assertEquals(token.getUser().getId(), result.getCanvasUserId());
        assertEquals(token.getUser().getName(), result.getCanvasUserName());
        long actualExpiresEpochMillis = result.getExpiresAt().toInstant().toEpochMilli();
        long expectedExpiresEpochMillis = Instant.now().plus(token.getExpiresIn(), ChronoUnit.SECONDS).toEpochMilli();
        assertTrue(Math.abs(actualExpiresEpochMillis - expectedExpiresEpochMillis) < 1000, MessageFormat.format("Expected {0} to be within 1000ms of {1}", actualExpiresEpochMillis, expectedExpiresEpochMillis));
    }

    @Test
    public void testFetchAndSaveAccessTokenThrowsExceptionWhenNot2XX() throws LMSOAuthException {
        when(canvasAPIOAuthSettingsRepository.findByPlatformDeployment(eq(platformDeployment))).thenReturn(Optional.of(canvasAPIOAuthSettings));
        when(canvasOAuthService.createRestTemplate()).thenReturn(restTemplate);
        when(restTemplate.postForEntity(anyString(), any(), eq(CanvasAPIToken.class))).thenReturn(new ResponseEntity<>(null, HttpStatus.BAD_REQUEST));

        try {
            canvasOAuthService.fetchAndSaveAccessToken(user, "code");
            fail("Should have thrown an exception");
        } catch (LMSOAuthException e ) {
            assertTrue(e.getMessage().contains("Could not fetch token"));
        }
    }

    @Test
    public void testFetchAndSaveAccessTokenThrowsExceptionWhen302() throws LMSOAuthException, URISyntaxException {
        when(canvasAPIOAuthSettingsRepository.findByPlatformDeployment(eq(platformDeployment))).thenReturn(Optional.of(canvasAPIOAuthSettings));
        when(canvasOAuthService.createRestTemplate()).thenReturn(restTemplate);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new URI("http://www.example.com/token-response?error=a_error_code&error_description=a_description"));
        when(restTemplate.postForEntity(anyString(), any(), eq(CanvasAPIToken.class))).thenThrow(
            new UnknownContentTypeException(null, null, HttpStatus.FOUND.value(), "Not Found", headers, null)
        );

        try {
            canvasOAuthService.fetchAndSaveAccessToken(user, "code");
            fail("Should have thrown an exception");
        } catch (LMSOAuthException e ) {
            assertTrue(e.getMessage().contains("a_error_code"));
            assertTrue(e.getMessage().contains("a_description"));
        }
    }

    @Test
    public void testGetAccessTokenReturnsTokenIfNotExpired() throws LMSOAuthException {
        when(canvasAPITokenRepository.findByUser(eq(user))).thenReturn(Optional.of(tokenEntity));
        tokenEntity.setExpiresAt(Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)));

        CanvasAPITokenEntity result = canvasOAuthService.getAccessToken(user);

        assertSame(tokenEntity, result);
    }

    @Test
    public void testGetAccessTokenRefreshesTokenIfExpired() throws LMSOAuthException {
        when(canvasAPITokenRepository.findByUser(eq(user))).thenReturn(Optional.of(tokenEntity));

        tokenEntity.setAccessToken("old-access-token");
        tokenEntity.setExpiresAt(Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)));

        when(canvasAPIOAuthSettingsRepository.findByPlatformDeployment(eq(platformDeployment))).thenReturn(Optional.of(canvasAPIOAuthSettings));
        when(canvasOAuthService.createRestTemplate()).thenReturn(restTemplate);
        when(restTemplate.postForEntity(anyString(), any(), eq(CanvasAPIToken.class))).thenReturn(new ResponseEntity<CanvasAPIToken>(token, HttpStatus.OK));
        when(canvasAPITokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CanvasAPITokenEntity result = canvasOAuthService.getAccessToken(user);

        assertSame(tokenEntity, result);
        assertEquals(token.getAccessToken(), result.getAccessToken());
        long actualExpiresEpochMillis = result.getExpiresAt().toInstant().toEpochMilli();
        long expectedExpiresEpochMillis = Instant.now().plus(token.getExpiresIn(), ChronoUnit.SECONDS).toEpochMilli();
        assertTrue(Math.abs(actualExpiresEpochMillis - expectedExpiresEpochMillis) < 1000, MessageFormat.format("Expected {0} to be within 1000ms of {1}", actualExpiresEpochMillis, expectedExpiresEpochMillis));
    }

    // Token expires in 4 minutes and the buffer for unexpired is 5 minutes.
    @Test
    public void testGetAccessTokenRefreshesTokenIfAlmostExpired() throws LMSOAuthException {
        when(canvasAPITokenRepository.findByUser(eq(user))).thenReturn(Optional.of(tokenEntity));

        tokenEntity.setAccessToken("old-access-token");
        tokenEntity.setExpiresAt(Timestamp.from(Instant.now().plus(4, ChronoUnit.MINUTES)));
        // Technically not expired, but will expire in 4 minutes, less than the 5 minute buffer, so should trigger refresh
        assertTrue(tokenEntity.getExpiresAt().toInstant().isAfter(Instant.now()));

        // Expect to refresh token
        when(canvasAPIOAuthSettingsRepository.findByPlatformDeployment(eq(platformDeployment))).thenReturn(Optional.of(canvasAPIOAuthSettings));
        when(canvasOAuthService.createRestTemplate()).thenReturn(restTemplate);
        when(restTemplate.postForEntity(anyString(), any(), eq(CanvasAPIToken.class))).thenReturn(new ResponseEntity<CanvasAPIToken>(token, HttpStatus.OK));
        when(canvasAPITokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CanvasAPITokenEntity result = canvasOAuthService.getAccessToken(user);

        assertSame(tokenEntity, result);
        // refresh should update the access token and expires at timestamp
        assertEquals(token.getAccessToken(), result.getAccessToken());
        long actualExpiresEpochMillis = result.getExpiresAt().toInstant().toEpochMilli();
        long expectedExpiresEpochMillis = Instant.now().plus(token.getExpiresIn(), ChronoUnit.SECONDS).toEpochMilli();
        assertTrue(Math.abs(actualExpiresEpochMillis - expectedExpiresEpochMillis) < 1000, MessageFormat.format("Expected {0} to be within 1000ms of {1}", actualExpiresEpochMillis, expectedExpiresEpochMillis));
    }

    @Test
    public void testIsAccessTokenAvailableReturnsFalseIfNoToken() {
        when(canvasAPITokenRepository.findByUser(eq(user))).thenReturn(Optional.empty());

        boolean result = canvasOAuthService.isAccessTokenAvailable(user);

        assertFalse(result);
    }

    @Test
    public void testIsAccessTokenAvailableReturnsFalseIfMissingScopes() {
        when(canvasAPITokenRepository.findByUser(eq(user))).thenReturn(Optional.of(tokenEntity));

        // Only include the first required scope
        tokenEntity.setScopes("");
        tokenEntity.setExpiresAt(Timestamp.from(Instant.now().plusSeconds(60)));

        boolean result = canvasOAuthService.isAccessTokenAvailable(user);

        assertFalse(result);
        verify(canvasAPIOAuthSettingsRepository, never()).findByPlatformDeployment(platformDeployment);
        verify(canvasOAuthService, never()).createRestTemplate();

    }

    @Test
    public void testIsAccessTokenAvailableRefreshesTokenIfAllScopes() {
        when(canvasAPITokenRepository.findByUser(eq(user))).thenReturn(Optional.of(tokenEntity));

        // Only include the first required scope
        tokenEntity.setAccessToken("old-access-token");
        tokenEntity.setScopes(String.join(" ", CANVAS_API_SCOPE));
        tokenEntity.setExpiresAt(Timestamp.from(Instant.now()));
        token.setAccessToken("new-access-token");

        // Should try to refresh token
        when(canvasAPIOAuthSettingsRepository.findByPlatformDeployment(eq(platformDeployment))).thenReturn(Optional.of(canvasAPIOAuthSettings));
        when(canvasOAuthService.createRestTemplate()).thenReturn(restTemplate);
        when(restTemplate.postForEntity(anyString(), any(), eq(CanvasAPIToken.class))).thenReturn(new ResponseEntity<CanvasAPIToken>(token, HttpStatus.OK));
        when(canvasAPITokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = canvasOAuthService.isAccessTokenAvailable(user);

        assertTrue(result);
        verify(canvasAPITokenRepository).save(tokenEntity);
        assertEquals("new-access-token", tokenEntity.getAccessToken());
    }

    @Test
    public void testIsAccessTokenAvailableReturnsFalseIfRefreshTokenFails() {
        when(canvasAPITokenRepository.findByUser(eq(user))).thenReturn(Optional.of(tokenEntity));

        // Only include the first required scope
        tokenEntity.setScopes(String.join(" ", CANVAS_API_SCOPE));
        tokenEntity.setExpiresAt(Timestamp.from(Instant.now()));

        // Should try to refresh token
        when(canvasAPIOAuthSettingsRepository.findByPlatformDeployment(eq(platformDeployment))).thenReturn(Optional.of(canvasAPIOAuthSettings));
        when(canvasOAuthService.createRestTemplate()).thenReturn(restTemplate);
        when(restTemplate.postForEntity(anyString(), any(), eq(CanvasAPIToken.class))).thenReturn(new ResponseEntity<CanvasAPIToken>(token, HttpStatus.BAD_REQUEST));

        boolean result = canvasOAuthService.isAccessTokenAvailable(user);

        assertFalse(result);
        verify(canvasAPITokenRepository, never()).save(tokenEntity);
    }

    private String encodeQueryParam(String name, String value) {
        return UriComponentsBuilder
            .newInstance()
            .queryParam(name, value)
            .encode()
            .build()
            .toString()
            .substring(1); // strip off leading "?"
    }

}
