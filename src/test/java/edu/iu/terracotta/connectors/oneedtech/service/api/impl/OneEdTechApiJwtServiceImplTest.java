package edu.iu.terracotta.connectors.oneedtech.service.api.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiOneUseToken;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class OneEdTechApiJwtServiceImplTest extends BaseTest {

    private static KeyPair keyPair;
    private static String privateKeyPem;
    private static String publicKeyPem;

    @InjectMocks private OneEdTechApiJwtServiceImpl oneEdTechApiJwtService;

    private Map<String, Object> customVars;

    @BeforeAll
    public static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        privateKeyPem = "-----BEGIN PRIVATE KEY-----" + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()) + "-----END PRIVATE KEY-----";
        publicKeyPem = "-----BEGIN PUBLIC KEY-----" + Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()) + "-----END PUBLIC KEY-----";
    }

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        when(ltiDataService.getOwnPrivateKey()).thenReturn(privateKeyPem);
        when(ltiDataService.getOwnPublicKey()).thenReturn(publicKeyPem);

        when(lti3Request.getLtiTargetLinkUrl()).thenReturn("");
        when(lti3Request.getContext()).thenReturn(new LtiContextEntity());
        when(lti3Request.getKey()).thenReturn(platformDeployment);
        when(lti3Request.getUser()).thenReturn(new LtiUserEntity("userKey", null, platformDeployment));
        when(lti3Request.getNonce()).thenReturn("nonce-123");

        customVars = new HashMap<>();
        customVars.put("oneEdTech_user_id", "123");
        customVars.put("oneEdTech_user_global_id", "202570000000000141");
        customVars.put("oneEdTech_login_id", "teststudent@iu.edu");
        customVars.put("oneEdTech_user_name", "teststudent@iu.edu");
        customVars.put("oneEdTech_course_id", "1154");
        customVars.put("oneEdTech_assignment_id", ".assignment.id");
        customVars.put("due_at", ".assignment.dueAt.iso8601");
        customVars.put("lock_at", ".assignment.lockAt.iso8601");
        customVars.put("unlock_at", ".assignment.unlockAt.iso8601");

        when(lti3Request.getLtiCustom()).thenReturn(customVars);
    }

    // helper: builds a fully signed, non-expired token via the class under test's own buildJwt method
    private String buildValidToken(boolean oneUse) throws GeneralSecurityException, java.io.IOException {
        return oneEdTechApiJwtService.buildJwt(
            oneUse,
            List.of("Learner"),
            1L,
            1L,
            "user-1",
            10L,
            20L,
            true,
            "oneEdTechUser1",
            "oneEdTechUserGlobal1",
            "oneEdTechLogin1",
            "oneEdTechName1",
            "oneEdTechCourse1",
            "oneEdTechAssignment1",
            "2024-05-01T10:00:00",
            "2024-05-10T10:00:00",
            "2024-04-01T10:00:00",
            "nonce-abc",
            5,
            2
        );
    }

    // helper: hand-crafts an already-expired, but validly-signed, token (validateToken can only ever return
    // null via the ExpiredJwtException branch, so tests that rely on a null Jws<Claims> need this)
    private String buildExpiredToken(boolean oneUse) {
        Date past = new Date(System.currentTimeMillis() - 60_000L);
        Date evenMorePast = new Date(System.currentTimeMillis() - 120_000L);

        return Jwts.builder()
            .header()
            .add(LtiStrings.KID, TextConstants.DEFAULT_KID)
            .add(LtiStrings.TYP, LtiStrings.JWT)
            .and()
            .issuer(ApiJwtService.ISSUER_TERRACOTTA_API)
            .subject("user-1")
            .audience()
            .add("http://lti.url")
            .and()
            .expiration(past)
            .notBefore(evenMorePast)
            .issuedAt(evenMorePast)
            .claim("oneUse", oneUse)
            .claim("contextId", 1L)
            .claim("platformDeploymentId", 1L)
            .claim("userId", "user-1")
            .signWith(keyPair.getPrivate(), SIG.RS256)
            .compact();
    }

    // ======================= validateToken =======================

    @Test
    public void testValidateTokenValidSignatureReturnsClaims() throws Exception {
        String token = buildValidToken(false);

        Jws<Claims> claims = oneEdTechApiJwtService.validateToken(token);

        assertNotNull(claims);
        assertEquals("user-1", claims.getPayload().getSubject());
    }

    @Test
    public void testValidateTokenExpiredReturnsNull() {
        String token = buildExpiredToken(false);

        assertNull(oneEdTechApiJwtService.validateToken(token));
    }

    @Test
    public void testValidateTokenBadPublicKeyThrows() throws Exception {
        String token = buildValidToken(false);
        when(ltiDataService.getOwnPublicKey()).thenReturn("not-a-valid-key");

        assertThrows(RuntimeException.class, () -> oneEdTechApiJwtService.validateToken(token));
    }

    // ======================= validateFileToken =======================

    @Test
    public void testValidateFileTokenMatchingIdReturnsTrue() throws GeneralSecurityException {
        String token = oneEdTechApiJwtService.buildFileToken("file-1", "http://lti.url");

        assertTrue(oneEdTechApiJwtService.validateFileToken(token, "file-1"));
    }

    @Test
    public void testValidateFileTokenNonMatchingIdReturnsFalse() throws GeneralSecurityException {
        String token = oneEdTechApiJwtService.buildFileToken("file-1", "http://lti.url");

        assertFalse(oneEdTechApiJwtService.validateFileToken(token, "file-2"));
    }

    @Test
    public void testValidateFileTokenExpiredReturnsFalse() {
        String token = buildExpiredToken(false);

        assertFalse(oneEdTechApiJwtService.validateFileToken(token, "file-1"));
    }

    // ======================= unsecureToken =======================
    // NOTE: unlike CanvasApiJwtServiceImpl#unsecureToken, this implementation does NOT split the compact
    // JWT on "." before base64-decoding it. Base64.getDecoder() (standard alphabet) rejects "." as an
    // illegal character, so calling unsecureToken with any real 3-part compact JWT (as every production
    // caller does, e.g. FileStorageServiceImpl/AssignmentServiceImpl/ParticipantServiceImpl) throws an
    // uncaught IllegalArgumentException instead of returning the decoded claims map. This looks like a bug.

    @Test
    public void testUnsecureTokenValidBase64JsonReturnsMap() {
        String encoded = Base64.getEncoder().encodeToString("{\"lti_assignment_id\":\"123\"}".getBytes());

        Map<String, Object> result = oneEdTechApiJwtService.unsecureToken(encoded, platformDeployment);

        assertEquals("123", result.get("lti_assignment_id"));
    }

    @Test
    public void testUnsecureTokenInvalidJsonThrowsIllegalStateException() {
        String encoded = Base64.getEncoder().encodeToString("not-json".getBytes());

        assertThrows(IllegalStateException.class, () -> oneEdTechApiJwtService.unsecureToken(encoded, platformDeployment));
    }

    @Test
    public void testUnsecureTokenRealCompactJwtThrowsIllegalArgumentException() throws Exception {
        // documents the bug described above: a real signed token (which contains "." separators) blows up
        String token = buildValidToken(false);

        assertThrows(IllegalArgumentException.class, () -> oneEdTechApiJwtService.unsecureToken(token, platformDeployment));
    }

    // ======================= buildJwt(long, String, Claims) =======================

    @Test
    public void testBuildJwtFromClaimsDelegatesToFullBuildJwt() throws Exception {
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("roles", List.of("Instructor"));
        claimsMap.put("contextId", 2L);
        claimsMap.put("assignmentId", 30L);
        claimsMap.put("experimentId", 40L);
        claimsMap.put("consent", Boolean.TRUE);
        claimsMap.put("oneEdTechUserId", "u1");
        claimsMap.put("oneEdTechUserGlobalId", "g1");
        claimsMap.put("oneEdTechLoginId", "l1");
        claimsMap.put("oneEdTechUserName", "n1");
        claimsMap.put("oneEdTechCourseId", "c1");
        claimsMap.put("oneEdTechAssignmentId", "a1");
        claimsMap.put("dueAt", "2024-05-01T10:00:00");
        claimsMap.put("lockAt", "2024-05-10T10:00:00");
        claimsMap.put("unlockAt", "2024-04-01T10:00:00");
        claimsMap.put("nonce", "nonce-xyz");
        claimsMap.put("allowedAttempts", 3);
        claimsMap.put("studentAttempts", 1);
        Claims claims = Jwts.claims().add(claimsMap).build();

        String token = oneEdTechApiJwtService.buildJwt(1L, "user-key", claims);

        Jws<Claims> parsed = oneEdTechApiJwtService.validateToken(token);
        assertEquals("user-key", parsed.getPayload().getSubject());
        assertEquals("nonce-xyz", parsed.getPayload().get("nonce"));
        assertEquals(3, parsed.getPayload().get("allowedAttempts"));
    }

    // ======================= buildJwt(oneUse, roles, ...) =======================

    @Test
    public void testBuildJwtOneUseSavesOneUseToken() throws Exception {
        buildValidToken(true);

        verify(apiOneUseTokenRepository).save(any(ApiOneUseToken.class));
    }

    @Test
    public void testBuildJwtNotOneUseDoesNotSaveToken() throws Exception {
        buildValidToken(false);

        verify(apiOneUseTokenRepository, never()).save(any());
    }

    @Test
    public void testBuildJwtUnknownPlatformDeploymentFallsBackToDefaultLocalUrl() throws Exception {
        when(platformDeploymentRepository.findById(999L)).thenReturn(Optional.empty());

        String token = oneEdTechApiJwtService.buildJwt(
            false, List.of("Learner"), 1L, 999L, "user-1", null, null, false,
            "u", "g", "l", "n", "c", "a", "", "", "", "nonce", null, null
        );

        Jws<Claims> parsed = oneEdTechApiJwtService.validateToken(token);
        assertTrue(parsed.getPayload().getAudience().contains(PlatformDeployment.LOCAL_URL));
    }

    @Test
    public void testBuildJwtRoundTripContainsAllClaims() throws Exception {
        String token = buildValidToken(false);

        Jws<Claims> parsed = oneEdTechApiJwtService.validateToken(token);
        Claims payload = parsed.getPayload();

        assertEquals(ApiJwtService.ISSUER_TERRACOTTA_API, payload.getIssuer());
        assertEquals("user-1", payload.getSubject());
        assertEquals(List.of("Learner"), payload.get("roles"));
        assertEquals(1, payload.get("contextId"));
        assertEquals(1, payload.get("platformDeploymentId"));
        assertEquals(10, payload.get("assignmentId"));
        assertEquals(20, payload.get("experimentId"));
        assertEquals(Boolean.TRUE, payload.get("consent"));
        assertEquals("oneEdTechUser1", payload.get("oneEdTechUserId"));
        assertEquals("oneEdTechUserGlobal1", payload.get("oneEdTechUserGlobalId"));
        assertEquals("oneEdTechLogin1", payload.get("oneEdTechLoginId"));
        assertEquals("oneEdTechName1", payload.get("oneEdTechUserName"));
        assertEquals("oneEdTechCourse1", payload.get("oneEdTechCourseId"));
        assertEquals("oneEdTechAssignment1", payload.get("oneEdTechAssignmentId"));
        assertEquals("nonce-abc", payload.get("nonce"));
        assertEquals(5, payload.get("allowedAttempts"));
        assertEquals(2, payload.get("studentAttempts"));
        assertEquals(Boolean.FALSE, payload.get("oneUse"));
    }

    // ======================= buildJwt(oneUse, lti3Request) =======================

    @Test
    public void testBuildJwtFromLti3RequestParsesQueryParams() throws Exception {
        when(lti3Request.getLtiTargetLinkUrl()).thenReturn("http://example.com/launch?assignment=5&consent=true&experiment=7");
        when(lti3Request.getLtiRoles()).thenReturn(List.of("Learner"));

        String token = oneEdTechApiJwtService.buildJwt(false, lti3Request);

        Jws<Claims> parsed = oneEdTechApiJwtService.validateToken(token);
        assertEquals(5, parsed.getPayload().get("assignmentId"));
        assertEquals(7, parsed.getPayload().get("experimentId"));
        assertEquals(Boolean.TRUE, parsed.getPayload().get("consent"));
        assertEquals("123", parsed.getPayload().get("oneEdTechUserId"));
        assertEquals("1154", parsed.getPayload().get("oneEdTechCourseId"));
    }

    @Test
    public void testBuildJwtFromLti3RequestBlankAssignmentAndExperimentAreNull() throws Exception {
        when(lti3Request.getLtiTargetLinkUrl()).thenReturn("http://example.com/launch");
        when(lti3Request.getLtiRoles()).thenReturn(List.of("Learner"));

        String token = oneEdTechApiJwtService.buildJwt(false, lti3Request);

        Jws<Claims> parsed = oneEdTechApiJwtService.validateToken(token);
        assertNull(parsed.getPayload().get("assignmentId"));
        assertNull(parsed.getPayload().get("experimentId"));
        assertEquals(Boolean.FALSE, parsed.getPayload().get("consent"));
    }

    @Test
    public void testBuildJwtFromLti3RequestNoAllowedAttemptsCustomVariable() throws Exception {
        customVars.put("allowed_attempts", "$OneEdTech.assignment.allowedAttempts");
        customVars.put("student_attempts", "$OneEdTech.assignment.submission.studentAttempts");

        String token = oneEdTechApiJwtService.buildJwt(false, lti3Request);

        Jws<Claims> parsed = oneEdTechApiJwtService.validateToken(token);
        assertNull(parsed.getPayload().get("allowedAttempts"));
        assertNull(parsed.getPayload().get("studentAttempts"));
    }

    @Test
    public void testBuildJwtFromLti3RequestNullAllowedAttemptsCustomVariable() throws Exception {
        customVars.put("allowed_attempts", null);
        customVars.put("student_attempts", "3");

        String token = oneEdTechApiJwtService.buildJwt(false, lti3Request);

        Jws<Claims> parsed = oneEdTechApiJwtService.validateToken(token);
        // populate allowedAttempts with -1 to indicate unlimited attempts
        assertEquals(-1, parsed.getPayload().get("allowedAttempts"));
        assertEquals(3, parsed.getPayload().get("studentAttempts"));
    }

    @Test
    public void testBuildJwtFromLti3RequestNumericAllowedAttemptsCustomVariable() throws Exception {
        customVars.put("allowed_attempts", "3");
        customVars.put("student_attempts", "1");

        String token = oneEdTechApiJwtService.buildJwt(false, lti3Request);

        Jws<Claims> parsed = oneEdTechApiJwtService.validateToken(token);
        assertEquals(3, parsed.getPayload().get("allowedAttempts"));
        assertEquals(1, parsed.getPayload().get("studentAttempts"));
    }

    @Test
    public void testBuildJwtFromLti3RequestNullStudentAttemptsCustomVariable() throws Exception {
        customVars.put("allowed_attempts", "3");
        customVars.put("student_attempts", null);

        String token = oneEdTechApiJwtService.buildJwt(false, lti3Request);

        Jws<Claims> parsed = oneEdTechApiJwtService.validateToken(token);
        assertEquals(3, parsed.getPayload().get("allowedAttempts"));
        assertEquals(0, parsed.getPayload().get("studentAttempts"));
    }

    // ======================= generateStateForAPITokenRequest / validateStateForAPITokenRequest =======================

    @Test
    public void testGenerateStateForAPITokenRequestUsesOAuthIssuer() throws Exception {
        String state = oneEdTechApiJwtService.generateStateForAPITokenRequest(lti3Request);

        Jws<Claims> parsed = oneEdTechApiJwtService.validateToken(state);
        assertEquals("lmsOAuthAPITokenRequest", parsed.getPayload().getIssuer());
    }

    @Test
    public void testValidateStateForAPITokenRequestMatchingIssuerReturnsClaims() throws Exception {
        String state = oneEdTechApiJwtService.generateStateForAPITokenRequest(lti3Request);

        Optional<Jws<Claims>> result = oneEdTechApiJwtService.validateStateForAPITokenRequest(state);

        assertTrue(result.isPresent());
    }

    @Test
    public void testValidateStateForAPITokenRequestWrongIssuerReturnsEmpty() throws Exception {
        String token = buildValidToken(false); // issuer is ISSUER_TERRACOTTA_API, not the oauth-state issuer

        Optional<Jws<Claims>> result = oneEdTechApiJwtService.validateStateForAPITokenRequest(token);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testValidateStateForAPITokenRequestExpiredReturnsEmpty() {
        String token = buildExpiredToken(false);

        Optional<Jws<Claims>> result = oneEdTechApiJwtService.validateStateForAPITokenRequest(token);

        assertTrue(result.isEmpty());
    }

    // ======================= buildFileToken =======================

    @Test
    public void testBuildFileTokenContainsFileIdClaim() throws GeneralSecurityException {
        String token = oneEdTechApiJwtService.buildFileToken("file-42", "http://lti.url");

        Jws<Claims> parsed = oneEdTechApiJwtService.validateToken(token);
        assertEquals("file-42", parsed.getPayload().get("fileId"));
        assertEquals("no_user", parsed.getPayload().getSubject());
        assertEquals("TERRACOTTA", parsed.getPayload().getIssuer());
    }

    // ======================= refreshToken =======================

    @Test
    public void testRefreshTokenValidTokenReturnsNewNonOneUseToken() throws Exception {
        String original = buildValidToken(false);

        String refreshed = oneEdTechApiJwtService.refreshToken(original);

        Jws<Claims> parsed = oneEdTechApiJwtService.validateToken(refreshed);
        assertEquals(Boolean.FALSE, parsed.getPayload().get("oneUse"));
        assertEquals("user-1", parsed.getPayload().getSubject());
        assertEquals("nonce-abc", parsed.getPayload().get("nonce"));
    }

    @Test
    public void testRefreshTokenOneUseTokenThrowsBadTokenException() throws Exception {
        String original = buildValidToken(true);

        assertThrows(BadTokenException.class, () -> oneEdTechApiJwtService.refreshToken(original));
    }

    @Test
    public void testRefreshTokenInvalidTokenThrowsBadTokenException() {
        String expired = buildExpiredToken(false);

        assertThrows(BadTokenException.class, () -> oneEdTechApiJwtService.refreshToken(expired));
    }

    // ======================= extractJwtStringValue =======================

    @Test
    public void testExtractJwtStringValueBearerHeaderReturnsToken() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer abc.def.ghi");

        assertEquals("abc.def.ghi", oneEdTechApiJwtService.extractJwtStringValue(httpServletRequest, false));
    }

    @Test
    public void testExtractJwtStringValueNonBearerHeaderReturnsNull() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Basic abc.def.ghi");

        assertNull(oneEdTechApiJwtService.extractJwtStringValue(httpServletRequest, false));
    }

    @Test
    public void testExtractJwtStringValueNoHeaderQueryParamAllowedReturnsQueryParam() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
        when(httpServletRequest.getParameter("token")).thenReturn("query-token");

        assertEquals("query-token", oneEdTechApiJwtService.extractJwtStringValue(httpServletRequest, true));
    }

    @Test
    public void testExtractJwtStringValueNoHeaderQueryParamNotAllowedReturnsNull() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        assertNull(oneEdTechApiJwtService.extractJwtStringValue(httpServletRequest, false));
    }

    // ======================= extractValues(HttpServletRequest, boolean) =======================

    @Test
    public void testExtractValuesFromRequestPopulatesSecuredInfo() throws Exception {
        String token = buildValidToken(false);
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer " + token);

        SecuredInfo securedInfoResult = oneEdTechApiJwtService.extractValues(httpServletRequest, false);

        assertNotNull(securedInfoResult);
        assertEquals("user-1", securedInfoResult.getUserId());
        assertEquals(1L, securedInfoResult.getPlatformDeploymentId());
        assertEquals(1L, securedInfoResult.getContextId());
        assertEquals(List.of("Learner"), securedInfoResult.getRoles());
        assertEquals("oneEdTechUser1", securedInfoResult.getLmsUserId());
        assertEquals("oneEdTechUserGlobal1", securedInfoResult.getLmsUserGlobalId());
        assertEquals("oneEdTechLogin1", securedInfoResult.getLmsLoginId());
        assertEquals("oneEdTechName1", securedInfoResult.getLmsUserName());
        assertEquals("oneEdTechCourse1", securedInfoResult.getLmsCourseId());
        assertEquals("oneEdTechAssignment1", securedInfoResult.getLmsAssignmentId());
        assertEquals("nonce-abc", securedInfoResult.getNonce());
        assertEquals(Boolean.TRUE, securedInfoResult.getConsent());
        assertEquals(5, securedInfoResult.getAllowedAttempts());
        assertEquals(2, securedInfoResult.getStudentAttempts());
        assertNotNull(securedInfoResult.getDueAt());
        assertNotNull(securedInfoResult.getLockAt());
        assertNotNull(securedInfoResult.getUnlockAt());
    }

    @Test
    public void testExtractValuesFromRequestUnparseableDatesResultInNullTimestamps() throws Exception {
        String token = oneEdTechApiJwtService.buildJwt(
            false, List.of("Learner"), 1L, 1L, "user-1", 10L, 20L, true,
            "u", "g", "l", "n", "c", "a",
            "not-a-date", "not-a-date", "not-a-date", "nonce", 1, 1
        );
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer " + token);

        SecuredInfo securedInfoResult = oneEdTechApiJwtService.extractValues(httpServletRequest, false);

        assertNotNull(securedInfoResult);
        assertNull(securedInfoResult.getDueAt());
        assertNull(securedInfoResult.getLockAt());
        assertNull(securedInfoResult.getUnlockAt());
    }

    @Test
    public void testExtractValuesFromRequestExpiredTokenReturnsNull() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer " + buildExpiredToken(false));

        assertNull(oneEdTechApiJwtService.extractValues(httpServletRequest, false));
    }

    @Test
    public void testExtractValuesFromRequestNoTokenThrowsIllegalArgumentException() {
        // documents a real edge case: when no token can be extracted at all, extractJwtStringValue returns
        // null and validateToken(null) is not guarded against, so parsing throws instead of the method
        // gracefully returning null the way it does for an expired/absent-claims token.
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> oneEdTechApiJwtService.extractValues(httpServletRequest, false));
    }

    // ======================= getTimedToken(HttpServletRequest) / getTimedToken(String) =======================

    @Test
    public void testGetTimedTokenFromRequestDelegatesToStringOverload() throws Exception {
        String oneUseToken = buildValidToken(true);
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer " + oneUseToken);

        ResponseEntity<String> response = oneEdTechApiJwtService.getTimedToken(httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Jws<Claims> parsed = oneEdTechApiJwtService.validateToken(response.getBody());
        assertEquals(Boolean.FALSE, parsed.getPayload().get("oneUse"));
    }

    @Test
    public void testGetTimedTokenOneUseTokenReturnsNewTokenWithOkStatus() throws Exception {
        String oneUseToken = buildValidToken(true);

        ResponseEntity<String> response = oneEdTechApiJwtService.getTimedToken(oneUseToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testGetTimedTokenNotOneUseTokenReturnsUnauthorized() throws Exception {
        String notOneUseToken = buildValidToken(false);

        ResponseEntity<String> response = oneEdTechApiJwtService.getTimedToken(notOneUseToken);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void testGetTimedTokenBuildJwtFailureReturnsInternalServerError() throws Exception {
        String oneUseToken = buildValidToken(true);
        // corrupt the private key AFTER the token above was signed with the valid one, so that
        // validateToken (which uses the still-valid public key) succeeds but the internal re-build fails
        when(ltiDataService.getOwnPrivateKey()).thenReturn("not-a-valid-private-key");

        ResponseEntity<String> response = oneEdTechApiJwtService.getTimedToken(oneUseToken);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Error generating token"));
    }

    @Test
    public void testGetTimedTokenExpiredTokenReturnsNull() throws Exception {
        String expired = buildExpiredToken(true);

        assertNull(oneEdTechApiJwtService.getTimedToken(expired));
    }

    // ======================= extractValues(String) =======================

    @Test
    public void testExtractValuesFromTokenPopulatesMinimalSecuredInfo() throws Exception {
        String token = buildValidToken(false);

        SecuredInfo securedInfoResult = oneEdTechApiJwtService.extractValues(token);

        assertNotNull(securedInfoResult);
        assertEquals("user-1", securedInfoResult.getUserId());
        assertEquals(1L, securedInfoResult.getPlatformDeploymentId());
        assertEquals(1L, securedInfoResult.getContextId());
        assertEquals(List.of("Learner"), securedInfoResult.getRoles());
        assertEquals("user-1", securedInfoResult.getLmsUserId());
        assertEquals("user-1", securedInfoResult.getLmsUserGlobalId());
        assertEquals("user-1", securedInfoResult.getLmsLoginId());
        assertEquals("user-1", securedInfoResult.getLmsUserName());
        assertEquals("ONE_ED_TECH", securedInfoResult.getLmsName());
        assertEquals("nonce-abc", securedInfoResult.getNonce());
    }

    @Test
    public void testExtractValuesFromTokenExpiredReturnsNull() throws Exception {
        String expired = buildExpiredToken(false);

        assertNull(oneEdTechApiJwtService.extractValues(expired));
    }

    // ======================= unimplemented ApiJwtService methods =======================
    // OneEdTechApiJwtServiceImpl only implements the token/JWT-handling half of ApiJwtService; every
    // authorization-check method below is intentionally unimplemented for this connector and always
    // throws UnsupportedOperationException. These assertions pin down that contract so a future partial
    // implementation is a deliberate, visible change rather than a silent behavior shift.

    @Test
    public void testUnimplementedMethodsThrowUnsupportedOperationException() throws Exception {
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.isAdmin(securedInfo));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.isTerracottaAdmin(securedInfo));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.isInstructor(securedInfo));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.isInstructorOrHigher(securedInfo));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.isLearner(securedInfo));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.isLearnerOrHigher(securedInfo));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.isGeneral(securedInfo));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.isTestStudent(securedInfo));

        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.experimentAllowed(securedInfo, 1L));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.experimentLocked(1L, false));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.conditionsLocked(1L, false));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.conditionAllowed(securedInfo, 1L, 2L));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.participantAllowed(securedInfo, 1L, 2L));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.exposureAllowed(securedInfo, 1L, 2L));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.groupAllowed(securedInfo, 1L, 2L));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.assignmentAllowed(securedInfo, 1L, 2L));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.assignmentAllowed(securedInfo, 1L, 2L, 3L));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.treatmentAllowed(securedInfo, 1L, 2L, 3L));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.assessmentAllowed(securedInfo, 1L, 2L, 3L, 4L));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.questionAllowed(securedInfo, 1L, 2L));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.answerAllowed(securedInfo, 1L, 2L, "MC", 3L));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.submissionAllowed(securedInfo, 1L, 2L));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.questionSubmissionAllowed(securedInfo, 1L, 2L, 3L));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.submissionCommentAllowed(securedInfo, 1L, 2L, 3L));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.questionSubmissionCommentAllowed(securedInfo, 1L, 2L));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.outcomeAllowed(securedInfo, 1L, 2L, 3L));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.outcomeScoreAllowed(securedInfo, 1L, 2L));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.answerSubmissionAllowed(securedInfo, 1L, "MC", 2L));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.integrationAllowed(1L, UUID.randomUUID()));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.experimentImportAllowed(securedInfo, UUID.randomUUID()));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.messagingContainerAllowed(securedInfo, 1L, UUID.randomUUID()));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.messagingContainerConfigurationAllowed(securedInfo, UUID.randomUUID(), UUID.randomUUID()));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.messagingAllowed(securedInfo, UUID.randomUUID(), UUID.randomUUID()));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.messagingContentAllowed(securedInfo, UUID.randomUUID(), UUID.randomUUID()));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.messagingConfigurationAllowed(securedInfo, UUID.randomUUID(), UUID.randomUUID()));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.messagingRuleSetAllowed(securedInfo, UUID.randomUUID(), UUID.randomUUID()));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.messagingRuleAllowed(securedInfo, UUID.randomUUID(), UUID.randomUUID()));
        assertThrows(UnsupportedOperationException.class, () -> oneEdTechApiJwtService.messagingConditionalTextAllowed(securedInfo, UUID.randomUUID(), UUID.randomUUID()));
    }

}
