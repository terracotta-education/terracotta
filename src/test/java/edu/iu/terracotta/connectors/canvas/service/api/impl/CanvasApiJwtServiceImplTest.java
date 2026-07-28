package edu.iu.terracotta.connectors.canvas.service.api.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.canvas.dao.model.enums.jwt.CanvasJwtClaim;
import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiOneUseToken;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.enums.jwt.JwtClaim;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainerConfiguration;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.message.MessageConfiguration;
import edu.iu.terracotta.dao.repository.messaging.container.MessageContainerConfigurationRepository;
import edu.iu.terracotta.dao.repository.messaging.container.MessageContainerRepository;
import edu.iu.terracotta.dao.repository.messaging.content.MessageContentRepository;
import edu.iu.terracotta.dao.repository.messaging.message.MessageConfigurationRepository;
import edu.iu.terracotta.dao.repository.messaging.message.MessageRepository;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.messaging.MessageConfigurationNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerConfigurationNotFoundException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerNotFoundException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerOwnerNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageContentNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageNotFoundException;
import edu.iu.terracotta.exceptions.messaging.MessageOwnerNotMatchingException;
import edu.iu.terracotta.utils.oauth.OAuthUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import io.jsonwebtoken.MalformedJwtException;

public class CanvasApiJwtServiceImplTest extends BaseTest {

    private static String testPrivateKeyPem;
    private static String testPublicKeyPem;

    @Mock private MessageContainerRepository messageContainerRepository;
    @Mock private MessageContainerConfigurationRepository messageContainerConfigurationRepository;
    @Mock private MessageContentRepository messageContentRepository;
    @Mock private MessageConfigurationRepository messageConfigurationRepository;
    @Mock private MessageRepository messageRepository;

    @InjectMocks private CanvasApiJwtServiceImpl canvasApiJWTService;

    private Map<String, Object> customVars;

    @BeforeAll
    public static void generateTestKeyPair() throws GeneralSecurityException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        testPrivateKeyPem = "-----BEGIN PRIVATE KEY-----"
            + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
            + "-----END PRIVATE KEY-----";
        testPublicKeyPem = "-----BEGIN PUBLIC KEY-----"
            + Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())
            + "-----END PUBLIC KEY-----";
    }

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        when(lti3Request.getLtiTargetLinkUrl()).thenReturn("");
        when(lti3Request.getContext()).thenReturn(new LtiContextEntity());
        when(lti3Request.getKey()).thenReturn(platformDeployment);
        when(lti3Request.getUser()).thenReturn(new LtiUserEntity("userKey", null, platformDeployment));

        customVars = new HashMap<>();
        customVars.put("canvas_user_id", "123");
        customVars.put("canvas_user_global_id", "202570000000000141");
        customVars.put("canvas_login_id", "teststudent@iu.edu");
        customVars.put("canvas_user_name", "teststudent@iu.edu");
        customVars.put("canvas_course_id", "1154");
        customVars.put("canvas_assignment_id", ".assignment.id");
        customVars.put("due_at", ".assignment.dueAt.iso8601");
        customVars.put("lock_at", ".assignment.lockAt.iso8601");
        customVars.put("unlock_at", ".assignment.unlockAt.iso8601");

        when(lti3Request.getLtiCustom()).thenReturn(customVars);
        when(ltiDataService.getOwnPrivateKey()).thenReturn(testPrivateKeyPem);
        when(ltiDataService.getOwnPublicKey()).thenReturn(testPublicKeyPem);
    }

    // helper: builds a real RS256-signed JWT using the generated test key pair, mirroring the
    // sibling ApiJwtServiceImplTest/BrightspaceApiJwtServiceImplTest helpers so expiry/signature
    // logic is exercised against a real (test) token instead of a mock
    private String buildSignedToken(String issuer, Map<String, Object> claims, Date expiration) throws GeneralSecurityException {
        Date now = new Date();
        var builder = Jwts.builder()
            .issuer(issuer)
            .subject("user123")
            .issuedAt(now)
            .expiration(expiration);

        if (claims != null) {
            claims.forEach(builder::claim);
        }

        return builder
            .signWith(OAuthUtils.loadPrivateKey(testPrivateKeyPem), SIG.RS256)
            .compact();
    }

    private String buildValidToken(Map<String, Object> claims) throws GeneralSecurityException {
        return buildSignedToken(ApiJwtService.ISSUER_TERRACOTTA_API, claims, new Date(System.currentTimeMillis() + 60_000L));
    }

    private String buildExpiredToken(Map<String, Object> claims) throws GeneralSecurityException {
        return buildSignedToken(ApiJwtService.ISSUER_TERRACOTTA_API, claims, new Date(System.currentTimeMillis() - 60_000L));
    }

    private long asLong(Object o) {
        return ((Number) o).longValue();
    }

    private String buildFullJwt(boolean oneUse) throws GeneralSecurityException, IOException {
        return canvasApiJWTService.buildJwt(
            oneUse,
            List.of("Instructor"),
            10L,
            1L,
            "user123",
            5L,
            7L,
            Boolean.TRUE,
            "canvasUser1",
            "canvasGlobal1",
            "canvasLogin1",
            "canvasName1",
            "canvasCourse1",
            "lmsAssign1",
            "2026-01-01T00:00:00Z",
            "2026-02-01T00:00:00Z",
            "2026-03-01T00:00:00Z",
            "nonce1",
            3,
            1
        );
    }

    // Test the case where the LTI tool launch is not in an assignment context
    @Test
    public void testBuildJwtWithNoAllowedAttemptsCustomVariable() throws GeneralSecurityException, IOException, TerracottaConnectorException {
        // Value is unreplaced when it is unavailable
        customVars.put("allowed_attempts", "$Canvas.assignment.allowedAttempts");
        customVars.put("student_attempts", "$Canvas.assignment.submission.studentAttempts");

        String jwt = canvasApiJWTService.buildJwt(false, lti3Request);
        Map<String, Object> claims = canvasApiJWTService.unsecureToken(jwt, platformDeployment);

        assertFalse(claims.containsKey("allowedAttempts"));
        assertFalse(claims.containsKey("studentAttempts"));
    }

    // assignment context, test with allowed_attempts = null
    @Test
    public void testBuildJwtWithNullAllowedAttemptsCustomVariable() throws GeneralSecurityException, IOException, TerracottaConnectorException {

        customVars.put("allowed_attempts", null);
        customVars.put("student_attempts", "3");

        String jwt = canvasApiJWTService.buildJwt(false, lti3Request);
        Map<String, Object> claims = canvasApiJWTService.unsecureToken(jwt, platformDeployment);

        // populate allowedAttempts with -1 to indicate that there are unlimited
        // attempts
        assertEquals(-1, claims.get("allowedAttempts"));
        assertEquals(3, claims.get("studentAttempts"));
    }

    // assignment context, test with allowed_attempts = 3
    @Test
    public void testBuildJwtWithSomeAllowedAttemptsCustomVariable() throws GeneralSecurityException, IOException, TerracottaConnectorException {

        customVars.put("allowed_attempts", "3");
        customVars.put("student_attempts", "1");

        String jwt = canvasApiJWTService.buildJwt(false, lti3Request);
        Map<String, Object> claims = canvasApiJWTService.unsecureToken(jwt, platformDeployment);

        assertEquals(3, claims.get("allowedAttempts"));
        assertEquals(1, claims.get("studentAttempts"));
    }

    // assignment context, test with allowed_attempts = 3, student_attempts = null
    @Test
    public void testBuildJwtWithNoStudentAttemptsCustomVariable() throws GeneralSecurityException, IOException, TerracottaConnectorException {

        customVars.put("allowed_attempts", "3");
        customVars.put("student_attempts", null);

        String jwt = canvasApiJWTService.buildJwt(false, lti3Request);
        Map<String, Object> claims = canvasApiJWTService.unsecureToken(jwt, platformDeployment);

        assertEquals(3, claims.get("allowedAttempts"));
        assertEquals(0, claims.get("studentAttempts"));
    }

    /* ***************** validateToken ***************** */

    @Test
    public void testValidateTokenValidReturnsClaims() throws GeneralSecurityException {
        String token = buildValidToken(Map.of("someClaim", "someValue"));

        Jws<Claims> result = canvasApiJWTService.validateToken(token);

        assertNotNull(result);
        assertEquals("someValue", result.getPayload().get("someClaim"));
    }

    @Test
    public void testValidateTokenExpiredReturnsNull() throws GeneralSecurityException {
        String token = buildExpiredToken(Map.of("someClaim", "someValue"));

        assertNull(canvasApiJWTService.validateToken(token));
    }

    @Test
    public void testValidateTokenBadPublicKeyThrowsJwtException() throws GeneralSecurityException {
        // valid base64 (OAuthUtils.loadPublicKey strips PEM markers unconditionally), but not a
        // well-formed X.509 SubjectPublicKeyInfo, so key parsing fails with a GeneralSecurityException
        // that the Locator swallows (logs + returns a null Key), which makes JJWT signature
        // verification fail with a JwtException.
        when(ltiDataService.getOwnPublicKey()).thenReturn("aGVsbG93b3JsZA==");
        String token = buildValidToken(Map.of("someClaim", "someValue"));

        assertThrows(JwtException.class, () -> canvasApiJWTService.validateToken(token));
    }

    @Test
    public void testValidateTokenMalformedTokenThrowsException() {
        assertThrows(MalformedJwtException.class, () -> canvasApiJWTService.validateToken("not-a-jwt-token-at-all"));
    }

    /* ***************** validateFileToken ***************** */

    @Test
    public void testValidateFileTokenMatchingFileIdReturnsTrue() throws GeneralSecurityException {
        String token = canvasApiJWTService.buildFileToken("file123", "http://localhost");

        assertTrue(canvasApiJWTService.validateFileToken(token, "file123"));
    }

    @Test
    public void testValidateFileTokenNonMatchingFileIdReturnsFalse() throws GeneralSecurityException {
        String token = canvasApiJWTService.buildFileToken("file123", "http://localhost");

        assertFalse(canvasApiJWTService.validateFileToken(token, "otherFile"));
    }

    @Test
    public void testValidateFileTokenExpiredTokenReturnsFalse() throws GeneralSecurityException {
        String token = buildExpiredToken(Map.of("fileId", "file123"));

        assertFalse(canvasApiJWTService.validateFileToken(token, "file123"));
    }

    /* ***************** unsecureToken ***************** */

    @Test
    public void testUnsecureTokenInvalidPayloadThrowsIllegalStateException() throws GeneralSecurityException {
        String token = canvasApiJWTService.buildFileToken("file123", "http://localhost");
        // the payload segment of a real signed JWT is a JSON object, so mangling it with extra
        // characters forces JsonMapper#readValue to fail and hit the catch(JacksonException) branch
        String[] sections = token.split("\\.");
        String corruptedToken = sections[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString("not-json".getBytes()) + "." + sections[2];

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> canvasApiJWTService.unsecureToken(corruptedToken, platformDeployment)
        );
        assertEquals("Request is not a valid LTI3 request.", ex.getMessage());
    }

    /* ***************** buildJwt(long, String, Claims) ***************** */

    @Test
    public void testBuildJwtFromClaimsOverloadMapsAllFieldsAndAlwaysOneUse() throws Exception {
        Claims mockClaims = mock(Claims.class);
        when(mockClaims.get(JwtClaim.ROLES.key(), List.class)).thenReturn(List.of("Learner"));
        when(mockClaims.get(JwtClaim.CONTEXT_ID.key(), Long.class)).thenReturn(200L);
        when(mockClaims.get(JwtClaim.ASSIGNMENT_ID.key(), Long.class)).thenReturn(9L);
        when(mockClaims.get(JwtClaim.EXPERIMENT_ID.key(), Long.class)).thenReturn(11L);
        when(mockClaims.get(JwtClaim.CONSENT.key(), Boolean.class)).thenReturn(true);
        when(mockClaims.get(CanvasJwtClaim.CANVAS_USER_ID.key(), String.class)).thenReturn("cUser1");
        when(mockClaims.get(CanvasJwtClaim.CANVAS_USER_GLOBAL_ID.key(), String.class)).thenReturn("cGlobal1");
        when(mockClaims.get(CanvasJwtClaim.CANVAS_LOGIN_ID.key(), String.class)).thenReturn("cLogin1");
        when(mockClaims.get(CanvasJwtClaim.CANVAS_USER_NAME.key(), String.class)).thenReturn("cName1");
        when(mockClaims.get(CanvasJwtClaim.CANVAS_COURSE_ID.key(), String.class)).thenReturn("cCourse1");
        when(mockClaims.get(JwtClaim.LMS_ASSIGNMENT_ID.key(), String.class)).thenReturn("lmsAssign1");
        when(mockClaims.get(JwtClaim.DUE_AT.key(), String.class)).thenReturn("2026-01-01T00:00:00Z");
        when(mockClaims.get(JwtClaim.LOCK_AT.key(), String.class)).thenReturn("2026-02-01T00:00:00Z");
        when(mockClaims.get(JwtClaim.UNLOCK_AT.key(), String.class)).thenReturn("2026-03-01T00:00:00Z");
        when(mockClaims.get(JwtClaim.NONCE.key(), String.class)).thenReturn("nonceABC");
        when(mockClaims.get(JwtClaim.ALLOWED_ATTEMPTS.key(), Integer.class)).thenReturn(5);
        when(mockClaims.get(JwtClaim.STUDENT_ATTEMPTS.key(), Integer.class)).thenReturn(2);

        String jwt = canvasApiJWTService.buildJwt(1L, "userKeyABC", mockClaims);
        Jws<Claims> claims = canvasApiJWTService.validateToken(jwt);
        Claims payload = claims.getPayload();

        assertEquals(Boolean.TRUE, payload.get(JwtClaim.ONE_USE.key()));
        assertEquals("userKeyABC", payload.getSubject());
        assertEquals(200L, asLong(payload.get(JwtClaim.CONTEXT_ID.key())));
        assertEquals("cUser1", payload.get(CanvasJwtClaim.CANVAS_USER_ID.key()));
        assertEquals("cCourse1", payload.get(CanvasJwtClaim.CANVAS_COURSE_ID.key()));
        assertEquals("nonceABC", payload.get(JwtClaim.NONCE.key()));
        verify(apiOneUseTokenRepository).save(any(ApiOneUseToken.class));
    }

    /* ***************** buildJwt(20-arg overload) ***************** */

    @Test
    public void testBuildJwtDirectOverloadProducesExpectedClaims() throws Exception {
        String jwt = buildFullJwt(false);
        Jws<Claims> claims = canvasApiJWTService.validateToken(jwt);
        Claims payload = claims.getPayload();

        assertEquals(ApiJwtService.ISSUER_TERRACOTTA_API, payload.getIssuer());
        assertEquals("user123", payload.getSubject());
        assertTrue(payload.getAudience().contains("http://lti.url"));
        assertEquals(10L, asLong(payload.get(JwtClaim.CONTEXT_ID.key())));
        assertEquals(1L, asLong(payload.get(JwtClaim.PLATFORM_DEPLOYMENT_ID.key())));
        assertEquals(List.of("Instructor"), payload.get(JwtClaim.ROLES.key()));
        assertEquals(5L, asLong(payload.get(JwtClaim.ASSIGNMENT_ID.key())));
        assertEquals(7L, asLong(payload.get(JwtClaim.EXPERIMENT_ID.key())));
        assertEquals(Boolean.TRUE, payload.get(JwtClaim.CONSENT.key()));
        assertEquals("canvasUser1", payload.get(CanvasJwtClaim.CANVAS_USER_ID.key()));
        assertEquals("canvasGlobal1", payload.get(CanvasJwtClaim.CANVAS_USER_GLOBAL_ID.key()));
        assertEquals("canvasLogin1", payload.get(CanvasJwtClaim.CANVAS_LOGIN_ID.key()));
        assertEquals("canvasName1", payload.get(CanvasJwtClaim.CANVAS_USER_NAME.key()));
        assertEquals("canvasCourse1", payload.get(CanvasJwtClaim.CANVAS_COURSE_ID.key()));
        assertEquals("lmsAssign1", payload.get(JwtClaim.LMS_ASSIGNMENT_ID.key()));
        assertEquals(3, payload.get(JwtClaim.ALLOWED_ATTEMPTS.key()));
        assertEquals(1, payload.get(JwtClaim.STUDENT_ATTEMPTS.key()));
        assertEquals(CanvasJwtClaim.CANVAS.key(), payload.get(JwtClaim.LMS_NAME.key()));
    }

    @Test
    public void testBuildJwtOneUseSavesTokenAndUsesShortExpiration() throws Exception {
        String jwt = buildFullJwt(true);
        Jws<Claims> claims = canvasApiJWTService.validateToken(jwt);

        assertEquals(Boolean.TRUE, claims.getPayload().get(JwtClaim.ONE_USE.key()));
        assertEquals(300, (claims.getPayload().getExpiration().getTime() - claims.getPayload().getIssuedAt().getTime()) / 1000);
        verify(apiOneUseTokenRepository).save(any(ApiOneUseToken.class));
    }

    @Test
    public void testBuildJwtNotOneUseDoesNotSaveTokenAndUsesLongExpiration() throws Exception {
        String jwt = buildFullJwt(false);
        Jws<Claims> claims = canvasApiJWTService.validateToken(jwt);

        assertEquals(Boolean.FALSE, claims.getPayload().get(JwtClaim.ONE_USE.key()));
        assertEquals(3600, (claims.getPayload().getExpiration().getTime() - claims.getPayload().getIssuedAt().getTime()) / 1000);
        verify(apiOneUseTokenRepository, never()).save(any());
    }

    @Test
    public void testBuildJwtWithUnknownPlatformDeploymentUsesDefaultLocalUrl() throws Exception {
        when(platformDeploymentRepository.findById(999L)).thenReturn(Optional.empty());

        String jwt = canvasApiJWTService.buildJwt(
            false, List.of("Instructor"), 10L, 999L, "user123", 5L, 7L, true,
            "canvasUser1", "canvasGlobal1", "canvasLogin1", "canvasName1", "canvasCourse1",
            "lmsAssign1", "2026-01-01T00:00:00Z", "2026-02-01T00:00:00Z", "2026-03-01T00:00:00Z",
            "nonce1", 3, 1
        );
        Jws<Claims> claims = canvasApiJWTService.validateToken(jwt);

        assertTrue(claims.getPayload().getAudience().contains(PlatformDeployment.LOCAL_URL));
    }

    /* ***************** buildJwt(oneUse, Lti3Request) ***************** */

    @Test
    public void testBuildJwtFromLti3RequestWithAssignmentExperimentAndConsent() throws Exception {
        when(lti3Request.getLtiTargetLinkUrl()).thenReturn("https://example.com/launch?assignment=55&experiment=77&consent=true");

        String jwt = canvasApiJWTService.buildJwt(false, lti3Request);
        Jws<Claims> claims = canvasApiJWTService.validateToken(jwt);
        Claims payload = claims.getPayload();

        assertEquals(55L, asLong(payload.get(JwtClaim.ASSIGNMENT_ID.key())));
        assertEquals(77L, asLong(payload.get(JwtClaim.EXPERIMENT_ID.key())));
        assertEquals(Boolean.TRUE, payload.get(JwtClaim.CONSENT.key()));
    }

    @Test
    public void testBuildJwtWithNonNumericAllowedAttemptsCustomVariable() throws Exception {
        customVars.put("allowed_attempts", "not-a-number");

        String jwt = canvasApiJWTService.buildJwt(false, lti3Request);
        Map<String, Object> claims = canvasApiJWTService.unsecureToken(jwt, platformDeployment);

        // parseInt() swallows the NumberFormatException and returns null; since the raw value is
        // non-null, extractAllowedAttempts() falls through both branches and also returns null
        assertFalse(claims.containsKey("allowedAttempts"));
    }

    /* ***************** generateStateForAPITokenRequest ***************** */

    @Test
    public void testGenerateStateForAPITokenRequestUsesLmsOAuthIssuer() throws Exception {
        String state = canvasApiJWTService.generateStateForAPITokenRequest(lti3Request);
        Jws<Claims> claims = canvasApiJWTService.validateToken(state);

        assertEquals(JwtClaim.ISSUER_LMS_OAUTH_API_TOKEN_REQUEST.key(), claims.getPayload().getIssuer());
    }

    /* ***************** validateStateForAPITokenRequest ***************** */

    @Test
    public void testValidateStateForAPITokenRequestMatchingIssuerReturnsClaims() throws Exception {
        String state = canvasApiJWTService.generateStateForAPITokenRequest(lti3Request);

        Optional<Jws<Claims>> result = canvasApiJWTService.validateStateForAPITokenRequest(state);

        assertTrue(result.isPresent());
        assertEquals(JwtClaim.ISSUER_LMS_OAUTH_API_TOKEN_REQUEST.key(), result.get().getPayload().getIssuer());
    }

    @Test
    public void testValidateStateForAPITokenRequestWrongIssuerReturnsEmpty() throws Exception {
        String jwt = buildFullJwt(false);

        assertTrue(canvasApiJWTService.validateStateForAPITokenRequest(jwt).isEmpty());
    }

    @Test
    public void testValidateStateForAPITokenRequestExpiredReturnsEmpty() throws Exception {
        String expired = buildExpiredToken(null);

        assertTrue(canvasApiJWTService.validateStateForAPITokenRequest(expired).isEmpty());
    }

    /* ***************** buildFileToken ***************** */

    @Test
    public void testBuildFileTokenBuildsValidatableToken() throws Exception {
        String token = canvasApiJWTService.buildFileToken("file123", "http://localhost");

        assertNotNull(token);

        Jws<Claims> claims = canvasApiJWTService.validateToken(token);
        assertNotNull(claims);
        assertEquals("file123", claims.getPayload().get(JwtClaim.FILE_ID.key()));
        assertEquals(JwtClaim.TERRACOTTA.key(), claims.getPayload().getIssuer());
        assertEquals(JwtClaim.NO_USER.key(), claims.getPayload().getSubject());
    }

    @Test
    public void testBuildFileTokenBadPrivateKeyThrowsGeneralSecurityException() {
        when(ltiDataService.getOwnPrivateKey()).thenReturn("not-a-valid-key");

        assertThrows(GeneralSecurityException.class, () -> canvasApiJWTService.buildFileToken("file123", "http://localhost"));
    }

    /* ***************** refreshToken ***************** */

    @Test
    public void testRefreshTokenInvalidTokenThrowsBadTokenException() throws Exception {
        String expired = buildExpiredToken(Map.of(JwtClaim.ONE_USE.key(), false));

        BadTokenException ex = assertThrows(BadTokenException.class, () -> canvasApiJWTService.refreshToken(expired));
        assertEquals("Token is invalid.", ex.getMessage());
    }

    @Test
    public void testRefreshTokenOneUseTokenThrowsBadTokenException() throws Exception {
        String oneUseToken = buildFullJwt(true);

        BadTokenException ex = assertThrows(BadTokenException.class, () -> canvasApiJWTService.refreshToken(oneUseToken));
        assertEquals("Trying to refresh an one use token", ex.getMessage());
    }

    @Test
    public void testRefreshTokenValidNonOneUseTokenSucceedsAndPreservesClaims() throws Exception {
        String original = buildFullJwt(false);
        Jws<Claims> originalClaims = canvasApiJWTService.validateToken(original);

        String refreshed = canvasApiJWTService.refreshToken(original);
        Jws<Claims> refreshedClaims = canvasApiJWTService.validateToken(refreshed);
        Claims payload = refreshedClaims.getPayload();

        assertEquals(Boolean.FALSE, payload.get(JwtClaim.ONE_USE.key()));
        assertEquals(originalClaims.getHeader().getKeyId(), refreshedClaims.getHeader().getKeyId());
        assertEquals(originalClaims.getPayload().getIssuer(), payload.getIssuer());
        assertEquals(originalClaims.getPayload().getSubject(), payload.getSubject());
        assertEquals("2026-01-01T00:00:00Z", payload.get(JwtClaim.DUE_AT.key()));
        assertEquals("2026-02-01T00:00:00Z", payload.get(JwtClaim.LOCK_AT.key()));
        assertEquals("2026-03-01T00:00:00Z", payload.get(JwtClaim.UNLOCK_AT.key()));
        assertEquals(CanvasJwtClaim.CANVAS.key(), payload.get(JwtClaim.LMS_NAME.key()));
    }

    // BUG: refreshToken() computes `length = 3600` (intended as seconds, matching the non-one-use
    // expiration elsewhere in this class) but passes it to DateUtils.addDays() instead of
    // DateUtils.addSeconds(), so a refreshed token actually expires 3600 DAYS out rather than
    // 3600 seconds - a ~311,040,000x longer lifetime than intended.
    @Test
    public void testRefreshTokenExpiresInOneHour() throws Exception {
        String original = buildFullJwt(false);

        String refreshed = canvasApiJWTService.refreshToken(original);
        Jws<Claims> claims = canvasApiJWTService.validateToken(refreshed);
        long deltaSeconds = (claims.getPayload().getExpiration().getTime() - claims.getPayload().getIssuedAt().getTime()) / 1000;

        assertEquals(3600L, deltaSeconds);
    }

    /* ***************** extractJwtStringValue ***************** */

    @Test
    public void testExtractJwtStringValueBearerHeaderReturnsToken() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer abc123");

        assertEquals("abc123", canvasApiJWTService.extractJwtStringValue(httpServletRequest, true));
    }

    @Test
    public void testExtractJwtStringValueLowercaseBearerHeaderReturnsToken() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn("bearer abc123");

        assertEquals("abc123", canvasApiJWTService.extractJwtStringValue(httpServletRequest, true));
    }

    @Test
    public void testExtractJwtStringValueNonBearerHeaderReturnsNull() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Basic abc123");

        assertNull(canvasApiJWTService.extractJwtStringValue(httpServletRequest, true));
    }

    @Test
    public void testExtractJwtStringValueNoHeaderQueryParamAllowedReturnsParam() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
        when(httpServletRequest.getParameter("token")).thenReturn("queryToken123");

        assertEquals("queryToken123", canvasApiJWTService.extractJwtStringValue(httpServletRequest, true));
    }

    @Test
    public void testExtractJwtStringValueNoHeaderQueryParamDisallowedReturnsNull() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        assertNull(canvasApiJWTService.extractJwtStringValue(httpServletRequest, false));
        verify(httpServletRequest, never()).getParameter(any());
    }

    @Test
    public void testExtractJwtStringValueNoHeaderNoQueryParamReturnsNull() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
        when(httpServletRequest.getParameter("token")).thenReturn(null);

        assertNull(canvasApiJWTService.extractJwtStringValue(httpServletRequest, true));
    }

    /* ***************** extractValues(HttpServletRequest, boolean) ***************** */

    @Test
    public void testExtractValuesFromRequestDelegatesToTokenExtraction() throws Exception {
        String jwt = buildFullJwt(false);
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer " + jwt);

        SecuredInfo result = canvasApiJWTService.extractValues(httpServletRequest, true);

        assertNotNull(result);
        assertEquals("user123", result.getUserId());
    }

    /* ***************** extractValues(String) ***************** */

    @Test
    public void testExtractValuesFullMapping() throws Exception {
        String jwt = buildFullJwt(false);

        SecuredInfo result = canvasApiJWTService.extractValues(jwt);

        assertNotNull(result);
        assertEquals("user123", result.getUserId());
        assertEquals(1L, result.getPlatformDeploymentId());
        assertEquals(10L, result.getContextId());
        assertEquals(List.of("Instructor"), result.getRoles());
        assertEquals("canvasUser1", result.getLmsUserId());
        assertEquals("canvasGlobal1", result.getLmsUserGlobalId());
        assertEquals("canvasLogin1", result.getLmsLoginId());
        assertEquals(CanvasJwtClaim.CANVAS.key(), result.getLmsName());
        assertEquals("canvasName1", result.getLmsUserName());
        assertEquals("canvasCourse1", result.getLmsCourseId());
        assertEquals("lmsAssign1", result.getLmsAssignmentId());
        assertNotNull(result.getDueAt());
        assertNotNull(result.getLockAt());
        assertNotNull(result.getUnlockAt());
        assertEquals("nonce1", result.getNonce());
        assertEquals(Boolean.TRUE, result.getConsent());
        assertEquals(3, result.getAllowedAttempts());
        assertEquals(1, result.getStudentAttempts());
    }

    @Test
    public void testExtractValuesInvalidTimestampReturnsNullForThatField() throws Exception {
        String jwt = canvasApiJWTService.buildJwt(
            false, List.of("Instructor"), 10L, 1L, "user123", 5L, 7L, true,
            "canvasUser1", "canvasGlobal1", "canvasLogin1", "canvasName1", "canvasCourse1",
            "lmsAssign1", "not-a-timestamp", "not-a-timestamp", "not-a-timestamp",
            "nonce1", 3, 1
        );

        SecuredInfo result = canvasApiJWTService.extractValues(jwt);

        assertNull(result.getDueAt());
        assertNull(result.getLockAt());
        assertNull(result.getUnlockAt());
    }

    @Test
    public void testExtractValuesMissingLmsNameDefaultsToCanvas() throws Exception {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaim.USER_ID.key(), "manualUser");
        claims.put(JwtClaim.PLATFORM_DEPLOYMENT_ID.key(), 1);
        claims.put(JwtClaim.CONTEXT_ID.key(), 10);
        claims.put(JwtClaim.ROLES.key(), List.of("Instructor"));
        claims.put(CanvasJwtClaim.CANVAS_USER_ID.key(), "canvasUser1");
        claims.put(CanvasJwtClaim.CANVAS_USER_GLOBAL_ID.key(), "canvasGlobal1");
        claims.put(CanvasJwtClaim.CANVAS_LOGIN_ID.key(), "canvasLogin1");
        claims.put(CanvasJwtClaim.CANVAS_USER_NAME.key(), "canvasName1");
        claims.put(CanvasJwtClaim.CANVAS_COURSE_ID.key(), "canvasCourse1");
        claims.put(JwtClaim.LMS_ASSIGNMENT_ID.key(), "lmsAssign1");
        claims.put(JwtClaim.DUE_AT.key(), "2026-01-01T00:00:00Z");
        claims.put(JwtClaim.LOCK_AT.key(), "2026-02-01T00:00:00Z");
        claims.put(JwtClaim.UNLOCK_AT.key(), "2026-03-01T00:00:00Z");
        claims.put(JwtClaim.NONCE.key(), "nonce1");
        claims.put(JwtClaim.CONSENT.key(), true);
        claims.put(JwtClaim.ALLOWED_ATTEMPTS.key(), 3);
        claims.put(JwtClaim.STUDENT_ATTEMPTS.key(), 1);

        String jwt = buildValidToken(claims);

        SecuredInfo result = canvasApiJWTService.extractValues(jwt);

        assertEquals(CanvasJwtClaim.CANVAS.key(), result.getLmsName());
    }

    @Test
    public void testExtractValuesInvalidTokenReturnsNull() throws Exception {
        String expired = buildExpiredToken(null);

        assertNull(canvasApiJWTService.extractValues(expired));
    }

    /* ***************** getTimedToken(HttpServletRequest) ***************** */

    @Test
    public void testGetTimedTokenFromRequestDelegates() throws Exception {
        String oneUseToken = buildFullJwt(true);
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
        when(httpServletRequest.getParameter("token")).thenReturn(oneUseToken);

        ResponseEntity<String> response = canvasApiJWTService.getTimedToken(httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /* ***************** getTimedToken(String) ***************** */

    @Test
    public void testGetTimedTokenOneUseTokenReturnsOkWithNewToken() throws Exception {
        String oneUseToken = buildFullJwt(true);

        ResponseEntity<String> response = canvasApiJWTService.getTimedToken(oneUseToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Jws<Claims> newClaims = canvasApiJWTService.validateToken(response.getBody());
        assertEquals(Boolean.FALSE, newClaims.getPayload().get(JwtClaim.ONE_USE.key()));
        assertEquals("user123", newClaims.getPayload().getSubject());
    }

    @Test
    public void testGetTimedTokenOneUseTokenWithNullAssignmentAndExperimentIds() throws Exception {
        String oneUseToken = canvasApiJWTService.buildJwt(
            true, List.of("Instructor"), 10L, 1L, "user123", null, null, true,
            "canvasUser1", "canvasGlobal1", "canvasLogin1", "canvasName1", "canvasCourse1",
            "lmsAssign1", "2026-01-01T00:00:00Z", "2026-02-01T00:00:00Z", "2026-03-01T00:00:00Z",
            "nonce1", 3, 1
        );

        ResponseEntity<String> response = canvasApiJWTService.getTimedToken(oneUseToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testGetTimedTokenNonOneUseTokenReturnsUnauthorized() throws Exception {
        String jwt = buildFullJwt(false);

        ResponseEntity<String> response = canvasApiJWTService.getTimedToken(jwt);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Token passed was not a one time valid token", response.getBody());
    }

    @Test
    public void testGetTimedTokenBuildJwtFailureReturnsInternalServerError() throws Exception {
        String oneUseToken = buildFullJwt(true);
        when(ltiDataService.getOwnPrivateKey()).thenReturn("invalid-key-data");

        ResponseEntity<String> response = canvasApiJWTService.getTimedToken(oneUseToken);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().startsWith("Error generating token"));
    }

    @Test
    public void testGetTimedTokenInvalidTokenReturnsNull() throws Exception {
        String expired = buildExpiredToken(Map.of(JwtClaim.ONE_USE.key(), true));

        assertNull(canvasApiJWTService.getTimedToken(expired));
    }

    /* ***************** role checks (all unimplemented) ***************** */

    @Test
    public void testRoleCheckMethodsThrowUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.isAdmin(null));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.isTerracottaAdmin(null));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.isInstructor(null));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.isInstructorOrHigher(null));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.isLearner(null));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.isLearnerOrHigher(null));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.isGeneral(null));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.isTestStudent(null));
    }

    /* ***************** allowed-check methods (all unimplemented except messaging*Allowed below) ***************** */

    @Test
    public void testUnimplementedAllowedMethodsThrowUnsupportedOperationException() {
        UUID uuid = UUID.randomUUID();

        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.experimentAllowed(null, 1L));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.experimentLocked(1L, false));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.conditionsLocked(1L, false));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.conditionAllowed(null, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.participantAllowed(null, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.exposureAllowed(null, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.groupAllowed(null, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.assignmentAllowed(null, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.assignmentAllowed(null, 1L, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.treatmentAllowed(null, 1L, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.assessmentAllowed(null, 1L, 1L, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.questionAllowed(null, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.answerAllowed(null, 1L, 1L, "type", 1L));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.submissionAllowed(null, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.questionSubmissionAllowed(null, 1L, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.submissionCommentAllowed(null, 1L, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.questionSubmissionCommentAllowed(null, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.outcomeAllowed(null, 1L, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.outcomeScoreAllowed(null, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.answerSubmissionAllowed(null, 1L, "type", 1L));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.integrationAllowed(1L, uuid));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.experimentImportAllowed(null, uuid));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.messagingRuleSetAllowed(null, uuid, uuid));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.messagingRuleAllowed(null, uuid, uuid));
        assertThrows(UnsupportedOperationException.class, () -> canvasApiJWTService.messagingConditionalTextAllowed(null, uuid, uuid));
    }

    /* ***************** messagingContainerAllowed ***************** */

    @Test
    public void testMessagingContainerAllowedOwnerMismatchThrows() {
        UUID uuid = UUID.randomUUID();
        when(messageContainerRepository.existsByUuidAndOwner_LmsUserId(uuid, "1")).thenReturn(false);

        assertThrows(MessageContainerOwnerNotMatchingException.class, () -> canvasApiJWTService.messagingContainerAllowed(securedInfo, 5L, uuid));
    }

    @Test
    public void testMessagingContainerAllowedExposureMismatchThrows() {
        UUID uuid = UUID.randomUUID();
        when(messageContainerRepository.existsByUuidAndOwner_LmsUserId(uuid, "1")).thenReturn(true);
        when(messageContainerRepository.existsByExposure_ExposureId(5L)).thenReturn(false);

        assertThrows(MessageContainerNotMatchingException.class, () -> canvasApiJWTService.messagingContainerAllowed(securedInfo, 5L, uuid));
    }

    @Test
    public void testMessagingContainerAllowedNotFoundThrows() {
        UUID uuid = UUID.randomUUID();
        when(messageContainerRepository.existsByUuidAndOwner_LmsUserId(uuid, "1")).thenReturn(true);
        when(messageContainerRepository.existsByExposure_ExposureId(5L)).thenReturn(true);
        when(messageContainerRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThrows(MessageContainerNotFoundException.class, () -> canvasApiJWTService.messagingContainerAllowed(securedInfo, 5L, uuid));
    }

    @Test
    public void testMessagingContainerAllowedFound() throws Exception {
        UUID uuid = UUID.randomUUID();
        MessageContainer container = new MessageContainer();
        when(messageContainerRepository.existsByUuidAndOwner_LmsUserId(uuid, "1")).thenReturn(true);
        when(messageContainerRepository.existsByExposure_ExposureId(5L)).thenReturn(true);
        when(messageContainerRepository.findByUuid(uuid)).thenReturn(Optional.of(container));

        MessageContainer result = canvasApiJWTService.messagingContainerAllowed(securedInfo, 5L, uuid);

        assertSame(container, result);
    }

    /* ***************** messagingContainerConfigurationAllowed ***************** */

    @Test
    public void testMessagingContainerConfigurationAllowedFound() throws Exception {
        UUID containerUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        MessageContainerConfiguration configuration = new MessageContainerConfiguration();
        when(messageContainerConfigurationRepository.findByUuidAndContainer_Uuid(uuid, containerUuid)).thenReturn(Optional.of(configuration));

        MessageContainerConfiguration result = canvasApiJWTService.messagingContainerConfigurationAllowed(securedInfo, containerUuid, uuid);

        assertSame(configuration, result);
    }

    @Test
    public void testMessagingContainerConfigurationAllowedNotFoundThrows() {
        UUID containerUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        when(messageContainerConfigurationRepository.findByUuidAndContainer_Uuid(uuid, containerUuid)).thenReturn(Optional.empty());

        assertThrows(MessageContainerConfigurationNotFoundException.class, () -> canvasApiJWTService.messagingContainerConfigurationAllowed(securedInfo, containerUuid, uuid));
    }

    /* ***************** messagingAllowed ***************** */

    @Test
    public void testMessagingAllowedOwnerMismatchThrows() {
        UUID containerUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        when(messageRepository.existsByUuidAndContainer_UuidAndContainer_Owner_LmsUserId(uuid, containerUuid, "1")).thenReturn(false);

        assertThrows(MessageOwnerNotMatchingException.class, () -> canvasApiJWTService.messagingAllowed(securedInfo, containerUuid, uuid));
    }

    @Test
    public void testMessagingAllowedNotFoundThrows() {
        UUID containerUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        when(messageRepository.existsByUuidAndContainer_UuidAndContainer_Owner_LmsUserId(uuid, containerUuid, "1")).thenReturn(true);
        when(messageRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThrows(MessageNotFoundException.class, () -> canvasApiJWTService.messagingAllowed(securedInfo, containerUuid, uuid));
    }

    @Test
    public void testMessagingAllowedFound() throws Exception {
        UUID containerUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        Message messageEntity = new Message();
        when(messageRepository.existsByUuidAndContainer_UuidAndContainer_Owner_LmsUserId(uuid, containerUuid, "1")).thenReturn(true);
        when(messageRepository.findByUuid(uuid)).thenReturn(Optional.of(messageEntity));

        Message result = canvasApiJWTService.messagingAllowed(securedInfo, containerUuid, uuid);

        assertSame(messageEntity, result);
    }

    /* ***************** messagingContentAllowed ***************** */

    @Test
    public void testMessagingContentAllowedFound() throws Exception {
        UUID messageUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        MessageContent content = new MessageContent();
        when(messageContentRepository.findByUuidAndMessage_Uuid(uuid, messageUuid)).thenReturn(Optional.of(content));

        MessageContent result = canvasApiJWTService.messagingContentAllowed(securedInfo, messageUuid, uuid);

        assertSame(content, result);
    }

    @Test
    public void testMessagingContentAllowedNotFoundThrows() {
        UUID messageUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        when(messageContentRepository.findByUuidAndMessage_Uuid(uuid, messageUuid)).thenReturn(Optional.empty());

        assertThrows(MessageContentNotMatchingException.class, () -> canvasApiJWTService.messagingContentAllowed(securedInfo, messageUuid, uuid));
    }

    /* ***************** messagingConfigurationAllowed ***************** */

    @Test
    public void testMessagingConfigurationAllowedFound() throws Exception {
        UUID messageUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        MessageConfiguration configuration = new MessageConfiguration();
        when(messageConfigurationRepository.findByUuidAndMessage_Uuid(uuid, messageUuid)).thenReturn(Optional.of(configuration));

        MessageConfiguration result = canvasApiJWTService.messagingConfigurationAllowed(securedInfo, messageUuid, uuid);

        assertSame(configuration, result);
    }

    @Test
    public void testMessagingConfigurationAllowedNotFoundThrows() {
        UUID messageUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        when(messageConfigurationRepository.findByUuidAndMessage_Uuid(uuid, messageUuid)).thenReturn(Optional.empty());

        assertThrows(MessageConfigurationNotMatchingException.class, () -> canvasApiJWTService.messagingConfigurationAllowed(securedInfo, messageUuid, uuid));
    }

}
