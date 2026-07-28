package edu.iu.terracotta.connectors.brightspace.service.api.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.brightspace.dao.model.enums.jwt.BrightspaceJwtClaim;
import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiOneUseToken;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.enums.jwt.JwtClaim;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.utils.oauth.OAuthUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;

public class BrightspaceApiJwtServiceImplTest extends BaseTest {

    // Self-generated 2048-bit RSA test key pair (PKCS#8 private / X.509 public), used only to sign and
    // verify tokens in this test class. Not used anywhere outside of this test.
    private static final String PRIVATE_KEY_PEM = "-----BEGIN PRIVATE KEY-----MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCVOzs3ZDgaHVz9R9jYfRI8kWGzhRhWSmnfJUgyXCOOsgexj3vRLkzTspL6U3XRN/kljxmyRNG8drVAsnGfZwx2SwOikOWf/OJNvgoZ98iG8NAEvK4alydUzm6x/+F4HYThT1LrBIXoXRCfkDlgyKOB82ZaqfV1mCkRHcEl1nbp1oFDvRwRSxqrR6tksNo9lHx8wtapeaVPfgf+EVZPPMEW2z5DgWtT1fRHMGTTj2QuVRhOzFwtS/9GujYEX4swJ8Lun9J48m2uDI8jM61DQvfvhMmHhzDd3xwMsG3sKpQU8gBxOu9qSqITbPsG8HxSzDJusDUCBwQi7fatM9RRQJYhAgMBAAECggEAB4OwTgySP73ycQrIEkOJkLbfYKJldkYCM8QqjXRoLp/kqoGEEFAtCL5eui/aPb6YvfKXdX1MbUQyF032w7pYmcz2avOVcrKKd9CyOy+9Og63cQoRNLkC/0eBlgFPrgKKhAGyKAVO5YqFQWD79VV/vAebbqhNQUPNYLH8ssuP43Fm6+uITKR1e1xn1GtiPPIwSbtp4qoL47XqruW+l+2X16IrvjrL2k3+fN4opd2GeVka24PSG9AaDEPUIfCn3B7I3FjciaqGjn/fOmkOC2mzs1Vjs8VXWXF1c8OoMe8FSOvcbaOcLkJDXcDomiYyf9V0r31XbiSZY6obx3KjdaQOiwKBgQDRGdYd6p36tKLuftnsLtHp+Qj+jktBEJMZM1rsCpdoi0bjhDO5Xi7JJT9zeB7F3ZEgOK+u9AnbwygyKSV/BUGrFqY14LEbtN20xlT/v2LN1N75IdcBaWfh9BPXeIe9quPgfmMwXI9NIJbix8zGPbOOsYy+w2y8KQSqjK+F3IBt4wKBgQC2s82Xd70gLy2bhjH8XJ7NC9uXxg+8pLpHwQV37whFZ9IrWko1+Y+Tex5vR7svKln16MgjA7CW9Zn5ceE8aiqzWlxHMRbSggr2ITgPWzQEmTDF4c7r38iHXVI8Wb50UB3jbXakMWCU4a39xaOUyrxqPj8FHK3FebyoW9H8uzMrKwKBgQCewbz9jcpQNKGX7hvK5Glf+UJI8wRT7B+i/IjFuezm5Qf3acJTtRxUf8FDDCphFC5BhohsjsCFaBC2tOLRI86N9W5Qbb5KSPIjdInIeyirGjoIcRbq7Xp/5W7DI7H74SmOoqbwavYqGto0aHoi8WnIpykzFPETkxkF3DbPrXCc/QKBgDL7mbNaxnvkrmVn3Sr+7ZFqbay8qEeL8C91vYDqQvSbNBrE51CIg3g0Mxn6elF/D/t/KdobfXuirYTeMmwZdDD4VifHmwbDIyvruSkP2kQuO1381TEdEWg0sm4E3RC4Cp8cPK802C6zi0n1jOBIcqtzE0Lp7dPdVLfmZ4KO+ohhAoGALrdYESRrl+DD8XC45rzLBK+T/WlMDcdaoc40208c5dNhnUHQGV09soQhOBww3NODznn+Gv/tj8rvYlXzSjjX/TYsmyCJ643p7DvRMmWIZXBdVhxgSQL7mnTejWlyG33S5WpJbgB04P08v11Wrbul1+rGOA81g6yDy2mwiPviYQ0=-----END PRIVATE KEY-----";
    private static final String PUBLIC_KEY_PEM = "-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlTs7N2Q4Gh1c/UfY2H0SPJFhs4UYVkpp3yVIMlwjjrIHsY970S5M07KS+lN10Tf5JY8ZskTRvHa1QLJxn2cMdksDopDln/ziTb4KGffIhvDQBLyuGpcnVM5usf/heB2E4U9S6wSF6F0Qn5A5YMijgfNmWqn1dZgpER3BJdZ26daBQ70cEUsaq0erZLDaPZR8fMLWqXmlT34H/hFWTzzBFts+Q4FrU9X0RzBk049kLlUYTsxcLUv/Rro2BF+LMCfC7p/SePJtrgyPIzOtQ0L374TJh4cw3d8cDLBt7CqUFPIAcTrvakqiE2z7BvB8UswybrA1AgcEIu32rTPUUUCWIQIDAQAB-----END PUBLIC KEY-----";

    @InjectMocks private BrightspaceApiJwtServiceImpl brightspaceApiJWTService;

    private Map<String, Object> customVars;

    @BeforeEach
    public void beforeEach() throws GeneralSecurityException {
        MockitoAnnotations.openMocks(this);

        setup();

        when(ltiDataService.getOwnPrivateKey()).thenReturn(PRIVATE_KEY_PEM);
        when(ltiDataService.getOwnPublicKey()).thenReturn(PUBLIC_KEY_PEM);

        // sanity check that the generated test key pair is usable by OAuthUtils before any test runs
        assertNotNull(OAuthUtils.loadPrivateKey(PRIVATE_KEY_PEM));
        assertNotNull(OAuthUtils.loadPublicKey(PUBLIC_KEY_PEM));

        when(lti3Request.getLtiTargetLinkUrl()).thenReturn("");

        LtiContextEntity context = LtiContextEntity.builder()
            .contextId(100L)
            .contextKey("courseKey123")
            .build();
        when(lti3Request.getContext()).thenReturn(context);
        when(lti3Request.getKey()).thenReturn(platformDeployment);

        LtiUserEntity user = new LtiUserEntity("userKey123", null, platformDeployment);
        user.setLmsUserId("guid1234_56789");
        when(lti3Request.getUser()).thenReturn(user);

        when(lti3Request.getNonce()).thenReturn("nonce123");
        when(lti3Request.getLtiRoles()).thenReturn(List.of("Instructor"));

        customVars = new HashMap<>();
        customVars.put(BrightspaceJwtClaim.BRIGHTSPACE_LOGIN_ID.key(), "loginXYZ");
        customVars.put(BrightspaceJwtClaim.BRIGHTSPACE_USER_NAME.key(), "userNameXYZ");
        customVars.put(BrightspaceJwtClaim.BRIGHTSPACE_ASSIGNMENT_ID.key(), "lmsAssignment999");
        customVars.put(JwtClaim.DUE_AT.key(), "2026-01-01T00:00:00Z");
        customVars.put(JwtClaim.LOCK_AT.key(), "2026-02-01T00:00:00Z");
        customVars.put(JwtClaim.UNLOCK_AT.key(), "2026-03-01T00:00:00Z");
        when(lti3Request.getLtiCustom()).thenReturn(customVars);
    }

    private long asLong(Object o) {
        return ((Number) o).longValue();
    }

    private String buildStandardJwt(boolean oneUse, String brightspaceUserId) throws GeneralSecurityException, IOException {
        return brightspaceApiJWTService.buildJwt(
            oneUse,
            List.of("Learner"),
            100L,
            1L,
            "user123",
            5L,
            7L,
            Boolean.TRUE,
            brightspaceUserId,
            "globalGuid123",
            "loginId123",
            "userNameXYZ",
            "courseKey123",
            "lmsAssignment123",
            "2026-01-01T00:00:00Z",
            "2026-02-01T00:00:00Z",
            "2026-03-01T00:00:00Z",
            "nonce123",
            3,
            1
        );
    }

    private String buildStandardJwt(boolean oneUse) throws GeneralSecurityException, IOException {
        return buildStandardJwt(oneUse, "guid1234_56789");
    }

    // builds a well-formed, but already-expired, token signed with the same test key so validateToken's
    // catch(ExpiredJwtException) branch can be exercised deterministically
    private String buildExpiredJwt() throws GeneralSecurityException {
        Date past = DateUtils.addSeconds(new Date(), -3700);

        return Jwts.builder()
            .issuer(ApiJwtService.ISSUER_TERRACOTTA_API)
            .subject("user123")
            .expiration(DateUtils.addSeconds(past, 60))
            .notBefore(past)
            .issuedAt(past)
            .claim(JwtClaim.ONE_USE.key(), false)
            .signWith(OAuthUtils.loadPrivateKey(PRIVATE_KEY_PEM), Jwts.SIG.RS256)
            .compact();
    }

    @Test
    public void testBuildJwtWithNoAllowedAttemptsCustomVariable() throws GeneralSecurityException, IOException, BadTokenException, TerracottaConnectorException {
        String jwt = brightspaceApiJWTService.buildJwt(false, lti3Request);
        Jws<Claims> claims = brightspaceApiJWTService.validateToken(jwt);

        assertFalse(claims.getPayload().containsKey(JwtClaim.ALLOWED_ATTEMPTS.key()));
        assertFalse(claims.getPayload().containsKey(JwtClaim.STUDENT_ATTEMPTS.key()));
    }

    @Test
    public void testBuildJwtWithNullAllowedAttemptsCustomVariable() throws GeneralSecurityException, IOException, BadTokenException, TerracottaConnectorException {
        customVars.put(JwtClaim.ALLOWED_ATTEMPTS.key(), null);
        customVars.put(JwtClaim.STUDENT_ATTEMPTS.key(), "3");

        String jwt = brightspaceApiJWTService.buildJwt(false, lti3Request);
        Jws<Claims> claims = brightspaceApiJWTService.validateToken(jwt);

        assertEquals(-1, claims.getPayload().get(JwtClaim.ALLOWED_ATTEMPTS.key()));
        assertEquals(3, claims.getPayload().get(JwtClaim.STUDENT_ATTEMPTS.key()));
    }

    @Test
    public void testBuildJwtWithSomeAllowedAttemptsCustomVariable() throws GeneralSecurityException, IOException, BadTokenException, TerracottaConnectorException {
        customVars.put(JwtClaim.ALLOWED_ATTEMPTS.key(), "3");
        customVars.put(JwtClaim.STUDENT_ATTEMPTS.key(), "1");

        String jwt = brightspaceApiJWTService.buildJwt(false, lti3Request);
        Jws<Claims> claims = brightspaceApiJWTService.validateToken(jwt);

        assertEquals(3, claims.getPayload().get(JwtClaim.ALLOWED_ATTEMPTS.key()));
        assertEquals(1, claims.getPayload().get(JwtClaim.STUDENT_ATTEMPTS.key()));
    }

    @Test
    public void testBuildJwtWithNoStudentAttemptsCustomVariable() throws GeneralSecurityException, IOException, BadTokenException, TerracottaConnectorException {
        customVars.put(JwtClaim.ALLOWED_ATTEMPTS.key(), "3");
        customVars.put(JwtClaim.STUDENT_ATTEMPTS.key(), null);

        String jwt = brightspaceApiJWTService.buildJwt(false, lti3Request);
        Jws<Claims> claims = brightspaceApiJWTService.validateToken(jwt);

        assertEquals(3, claims.getPayload().get(JwtClaim.ALLOWED_ATTEMPTS.key()));
        assertEquals(0, claims.getPayload().get(JwtClaim.STUDENT_ATTEMPTS.key()));
    }

    @Test
    public void testBuildJwtWithNonNumericAllowedAttemptsCustomVariable() throws GeneralSecurityException, IOException, BadTokenException, TerracottaConnectorException {
        customVars.put(JwtClaim.ALLOWED_ATTEMPTS.key(), "not-a-number");

        String jwt = brightspaceApiJWTService.buildJwt(false, lti3Request);
        Jws<Claims> claims = brightspaceApiJWTService.validateToken(jwt);

        // parseInt() swallows the NumberFormatException and returns null; since the raw value is
        // non-null, extractAllowedAttempts() falls through both branches and also returns null
        assertFalse(claims.getPayload().containsKey(JwtClaim.ALLOWED_ATTEMPTS.key()));
    }

    @Test
    public void testBuildJwtFromLti3RequestWithAssignmentExperimentAndConsent() throws GeneralSecurityException, IOException, BadTokenException, TerracottaConnectorException {
        when(lti3Request.getLtiTargetLinkUrl()).thenReturn("https://example.com/launch?assignment=55&experiment=77&consent=true");

        String jwt = brightspaceApiJWTService.buildJwt(false, lti3Request);
        Jws<Claims> claims = brightspaceApiJWTService.validateToken(jwt);
        Claims payload = claims.getPayload();

        assertEquals(55L, asLong(payload.get(JwtClaim.ASSIGNMENT_ID.key())));
        assertEquals(77L, asLong(payload.get(JwtClaim.EXPERIMENT_ID.key())));
        assertEquals(Boolean.TRUE, payload.get(JwtClaim.CONSENT.key()));
        assertEquals(100L, asLong(payload.get(JwtClaim.CONTEXT_ID.key())));
        assertEquals(1L, asLong(payload.get(JwtClaim.PLATFORM_DEPLOYMENT_ID.key())));
        assertEquals(List.of("Instructor"), payload.get(JwtClaim.ROLES.key()));
        assertEquals("userKey123", payload.getSubject());
        assertEquals("guid1234_56789", payload.get(BrightspaceJwtClaim.BRIGHTSPACE_USER_ID.key()));
        assertEquals("userKey123", payload.get(BrightspaceJwtClaim.BRIGHTSPACE_USER_GLOBAL_ID.key()));
        assertEquals("loginXYZ", payload.get(BrightspaceJwtClaim.BRIGHTSPACE_LOGIN_ID.key()));
        assertEquals("userNameXYZ", payload.get(BrightspaceJwtClaim.BRIGHTSPACE_USER_NAME.key()));
        assertEquals("courseKey123", payload.get(BrightspaceJwtClaim.BRIGHTSPACE_COURSE_ID.key()));
        assertEquals("lmsAssignment999", payload.get(JwtClaim.LMS_ASSIGNMENT_ID.key()));
        assertEquals("2026-01-01T00:00:00Z", payload.get(JwtClaim.DUE_AT.key()));
        assertEquals("2026-02-01T00:00:00Z", payload.get(JwtClaim.LOCK_AT.key()));
        assertEquals("2026-03-01T00:00:00Z", payload.get(JwtClaim.UNLOCK_AT.key()));
        assertEquals("nonce123", payload.get(JwtClaim.NONCE.key()));
        assertEquals(BrightspaceJwtClaim.BRIGHTSPACE.key(), payload.get(JwtClaim.LMS_NAME.key()));
    }

    @Test
    public void testBuildJwtFromLti3RequestOneUseSavesTokenAndUsesShortExpiration() throws GeneralSecurityException, IOException, BadTokenException, TerracottaConnectorException {
        String jwt = brightspaceApiJWTService.buildJwt(true, lti3Request);
        Jws<Claims> claims = brightspaceApiJWTService.validateToken(jwt);

        assertEquals(Boolean.TRUE, claims.getPayload().get(JwtClaim.ONE_USE.key()));
        assertEquals(300, (claims.getPayload().getExpiration().getTime() - claims.getPayload().getIssuedAt().getTime()) / 1000);
        verify(apiOneUseTokenRepository).save(any(ApiOneUseToken.class));
    }

    @Test
    public void testBuildJwtFromLti3RequestNotOneUseDoesNotSaveTokenAndUsesLongExpiration() throws GeneralSecurityException, IOException, BadTokenException, TerracottaConnectorException {
        String jwt = brightspaceApiJWTService.buildJwt(false, lti3Request);
        Jws<Claims> claims = brightspaceApiJWTService.validateToken(jwt);

        assertEquals(Boolean.FALSE, claims.getPayload().get(JwtClaim.ONE_USE.key()));
        assertEquals(3600, (claims.getPayload().getExpiration().getTime() - claims.getPayload().getIssuedAt().getTime()) / 1000);
        verify(apiOneUseTokenRepository, never()).save(any());
    }

    @Test
    public void testBuildJwtDirectOverloadProducesExpectedClaims() throws GeneralSecurityException, IOException, BadTokenException, TerracottaConnectorException {
        String jwt = buildStandardJwt(false);
        Jws<Claims> claims = brightspaceApiJWTService.validateToken(jwt);
        Claims payload = claims.getPayload();

        assertEquals(ApiJwtService.ISSUER_TERRACOTTA_API, payload.getIssuer());
        assertEquals("user123", payload.getSubject());
        assertTrue(payload.getAudience().contains("http://lti.url"));
        assertEquals(100L, asLong(payload.get(JwtClaim.CONTEXT_ID.key())));
        assertEquals(1L, asLong(payload.get(JwtClaim.PLATFORM_DEPLOYMENT_ID.key())));
        assertEquals(List.of("Learner"), payload.get(JwtClaim.ROLES.key()));
        assertEquals(5L, asLong(payload.get(JwtClaim.ASSIGNMENT_ID.key())));
        assertEquals(7L, asLong(payload.get(JwtClaim.EXPERIMENT_ID.key())));
        assertEquals(Boolean.TRUE, payload.get(JwtClaim.CONSENT.key()));
        assertEquals("guid1234_56789", payload.get(BrightspaceJwtClaim.BRIGHTSPACE_USER_ID.key()));
        assertEquals("globalGuid123", payload.get(BrightspaceJwtClaim.BRIGHTSPACE_USER_GLOBAL_ID.key()));
        assertEquals("loginId123", payload.get(BrightspaceJwtClaim.BRIGHTSPACE_LOGIN_ID.key()));
        assertEquals("userNameXYZ", payload.get(BrightspaceJwtClaim.BRIGHTSPACE_USER_NAME.key()));
        assertEquals("courseKey123", payload.get(BrightspaceJwtClaim.BRIGHTSPACE_COURSE_ID.key()));
        assertEquals("lmsAssignment123", payload.get(JwtClaim.LMS_ASSIGNMENT_ID.key()));
        assertEquals(3, payload.get(JwtClaim.ALLOWED_ATTEMPTS.key()));
        assertEquals(1, payload.get(JwtClaim.STUDENT_ATTEMPTS.key()));
        assertEquals(BrightspaceJwtClaim.BRIGHTSPACE.key(), payload.get(JwtClaim.LMS_NAME.key()));
    }

    @Test
    public void testBuildJwtFromClaimsAlwaysUsesOneUseTrue() throws GeneralSecurityException, IOException, BadTokenException, TerracottaConnectorException {
        Claims mockClaims = mock(Claims.class);
        when(mockClaims.get(JwtClaim.ROLES.key(), List.class)).thenReturn(List.of("Learner"));
        when(mockClaims.get(JwtClaim.CONTEXT_ID.key(), Long.class)).thenReturn(200L);
        when(mockClaims.get(JwtClaim.ASSIGNMENT_ID.key(), Long.class)).thenReturn(9L);
        when(mockClaims.get(JwtClaim.EXPERIMENT_ID.key(), Long.class)).thenReturn(11L);
        when(mockClaims.get(JwtClaim.CONSENT.key(), Boolean.class)).thenReturn(true);
        when(mockClaims.get(BrightspaceJwtClaim.BRIGHTSPACE_USER_ID.key(), String.class)).thenReturn("bUser1");
        when(mockClaims.get(BrightspaceJwtClaim.BRIGHTSPACE_USER_GLOBAL_ID.key(), String.class)).thenReturn("bGlobal1");
        when(mockClaims.get(BrightspaceJwtClaim.BRIGHTSPACE_LOGIN_ID.key(), String.class)).thenReturn("bLogin1");
        when(mockClaims.get(BrightspaceJwtClaim.BRIGHTSPACE_USER_NAME.key(), String.class)).thenReturn("bName1");
        when(mockClaims.get(BrightspaceJwtClaim.BRIGHTSPACE_COURSE_ID.key(), String.class)).thenReturn("bCourse1");
        when(mockClaims.get(JwtClaim.LMS_ASSIGNMENT_ID.key(), String.class)).thenReturn("lmsAssign1");
        when(mockClaims.get(JwtClaim.DUE_AT.key(), String.class)).thenReturn("2026-01-01");
        when(mockClaims.get(JwtClaim.LOCK_AT.key(), String.class)).thenReturn("2026-02-01");
        when(mockClaims.get(JwtClaim.UNLOCK_AT.key(), String.class)).thenReturn("2026-03-01");
        when(mockClaims.get(JwtClaim.NONCE.key(), String.class)).thenReturn("nonceABC");
        when(mockClaims.get(JwtClaim.ALLOWED_ATTEMPTS.key(), Integer.class)).thenReturn(5);
        when(mockClaims.get(JwtClaim.STUDENT_ATTEMPTS.key(), Integer.class)).thenReturn(2);

        String jwt = brightspaceApiJWTService.buildJwt(1L, "userKeyABC", mockClaims);
        Jws<Claims> claims = brightspaceApiJWTService.validateToken(jwt);
        Claims payload = claims.getPayload();

        assertEquals(Boolean.TRUE, payload.get(JwtClaim.ONE_USE.key()));
        assertEquals("userKeyABC", payload.getSubject());
        assertEquals(200L, asLong(payload.get(JwtClaim.CONTEXT_ID.key())));
        assertEquals(9L, asLong(payload.get(JwtClaim.ASSIGNMENT_ID.key())));
        assertEquals(11L, asLong(payload.get(JwtClaim.EXPERIMENT_ID.key())));
        assertEquals("bUser1", payload.get(BrightspaceJwtClaim.BRIGHTSPACE_USER_ID.key()));
        assertEquals("bCourse1", payload.get(BrightspaceJwtClaim.BRIGHTSPACE_COURSE_ID.key()));
        assertEquals("nonceABC", payload.get(JwtClaim.NONCE.key()));
    }

    @Test
    public void testGenerateStateForAPITokenRequestUsesLmsOAuthIssuer() throws GeneralSecurityException, IOException, BadTokenException, TerracottaConnectorException {
        String state = brightspaceApiJWTService.generateStateForAPITokenRequest(lti3Request);
        Jws<Claims> claims = brightspaceApiJWTService.validateToken(state);

        assertEquals(JwtClaim.ISSUER_LMS_OAUTH_API_TOKEN_REQUEST.key(), claims.getPayload().getIssuer());
    }

    @Test
    public void testValidateStateForAPITokenRequestWithMatchingIssuerReturnsClaims() throws GeneralSecurityException, IOException, BadTokenException, TerracottaConnectorException {
        String state = brightspaceApiJWTService.generateStateForAPITokenRequest(lti3Request);

        assertTrue(brightspaceApiJWTService.validateStateForAPITokenRequest(state).isPresent());
    }

    @Test
    public void testValidateStateForAPITokenRequestWithNonMatchingIssuerReturnsEmpty() throws GeneralSecurityException, IOException, BadTokenException, TerracottaConnectorException {
        String jwt = buildStandardJwt(false);

        assertTrue(brightspaceApiJWTService.validateStateForAPITokenRequest(jwt).isEmpty());
    }

    @Test
    public void testValidateStateForAPITokenRequestWithInvalidTokenReturnsEmpty() throws GeneralSecurityException, IOException, BadTokenException, TerracottaConnectorException {
        String expired = buildExpiredJwt();

        assertTrue(brightspaceApiJWTService.validateStateForAPITokenRequest(expired).isEmpty());
    }

    @Test
    public void testValidateTokenWithValidSignedTokenReturnsClaims() throws GeneralSecurityException, IOException, BadTokenException, TerracottaConnectorException {
        String jwt = buildStandardJwt(false);

        Jws<Claims> claims = brightspaceApiJWTService.validateToken(jwt);

        assertNotNull(claims);
        assertEquals("user123", claims.getPayload().getSubject());
    }

    @Test
    public void testValidateTokenWithExpiredTokenReturnsNull() throws GeneralSecurityException {
        String expired = buildExpiredJwt();

        assertNull(brightspaceApiJWTService.validateToken(expired));
    }

    // BUG: validateToken() only catches ExpiredJwtException. Any other parsing failure (malformed
    // compact JWT, bad signature, unresolvable key, etc.) propagates as an unchecked jjwt exception
    // instead of returning null like the expired-token case does.
    @Test
    public void testValidateTokenWithMalformedTokenThrowsMalformedJwtException() {
        assertThrows(MalformedJwtException.class, () -> brightspaceApiJWTService.validateToken("not-a-jwt-token-at-all"));
    }

    @Test
    public void testValidateFileTokenWithMatchingFileIdReturnsTrue() throws GeneralSecurityException {
        String token = brightspaceApiJWTService.buildFileToken("file123", "https://example.com");

        assertTrue(brightspaceApiJWTService.validateFileToken(token, "file123"));
    }

    @Test
    public void testValidateFileTokenWithNonMatchingFileIdReturnsFalse() throws GeneralSecurityException {
        String token = brightspaceApiJWTService.buildFileToken("file123", "https://example.com");

        assertFalse(brightspaceApiJWTService.validateFileToken(token, "otherFile"));
    }

    @Test
    public void testValidateFileTokenWithInvalidTokenReturnsFalse() throws GeneralSecurityException {
        String expired = buildExpiredJwt();

        assertFalse(brightspaceApiJWTService.validateFileToken(expired, "file123"));
    }

    // BUG: unlike CanvasApiJwtServiceImpl#unsecureToken (which splits the compact JWT and base64url
    // decodes the payload segment before handing it to the JSON mapper), Brightspace's unsecureToken()
    // passes the raw compact token straight to JsonMapper#readValue. A real signed JWT is never valid
    // JSON, so this always throws instead of returning the decoded claims map.
    @Test
    public void testUnsecureTokenWithRealCompactJwtThrowsIllegalStateException() throws GeneralSecurityException, IOException, BadTokenException, TerracottaConnectorException {
        String jwt = buildStandardJwt(false);

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> brightspaceApiJWTService.unsecureToken(jwt, platformDeployment)
        );
        assertEquals("Request is not a valid LTI3 request.", ex.getMessage());
    }

    @Test
    public void testUnsecureTokenWithPlainJsonInputParsesSuccessfully() {
        Map<String, Object> result = brightspaceApiJWTService.unsecureToken("{\"foo\":\"bar\"}", platformDeployment);

        assertEquals("bar", result.get("foo"));
    }

    @Test
    public void testBuildFileTokenCreatesValidatableToken() throws GeneralSecurityException {
        String token = brightspaceApiJWTService.buildFileToken("file123", "https://example.com");
        Jws<Claims> claims = brightspaceApiJWTService.validateToken(token);

        assertEquals(JwtClaim.TERRACOTTA.key(), claims.getPayload().getIssuer());
        assertEquals(JwtClaim.NO_USER.key(), claims.getPayload().getSubject());
        assertEquals("file123", claims.getPayload().get(JwtClaim.FILE_ID.key()));
        assertTrue(claims.getPayload().getAudience().contains("https://example.com"));
    }

    @Test
    public void testRefreshTokenWithInvalidTokenThrowsBadTokenException() throws GeneralSecurityException {
        String expired = buildExpiredJwt();

        BadTokenException ex = assertThrows(BadTokenException.class, () -> brightspaceApiJWTService.refreshToken(expired));
        assertEquals("Token is invalid.", ex.getMessage());
    }

    @Test
    public void testRefreshTokenWithOneUseTokenThrowsBadTokenException() throws GeneralSecurityException, IOException {
        String oneUseToken = buildStandardJwt(true);

        BadTokenException ex = assertThrows(BadTokenException.class, () -> brightspaceApiJWTService.refreshToken(oneUseToken));
        assertEquals("Trying to refresh an one use token", ex.getMessage());
    }

    @Test
    public void testRefreshTokenSuccessClearsDueLockUnlockAtAndFlipsOneUse() throws GeneralSecurityException, IOException, BadTokenException {
        String original = buildStandardJwt(false);
        Jws<Claims> originalClaims = brightspaceApiJWTService.validateToken(original);

        String refreshed = brightspaceApiJWTService.refreshToken(original);
        Jws<Claims> refreshedClaims = brightspaceApiJWTService.validateToken(refreshed);
        Claims payload = refreshedClaims.getPayload();

        assertEquals(Boolean.FALSE, payload.get(JwtClaim.ONE_USE.key()));
        assertNull(payload.get(JwtClaim.DUE_AT.key()));
        assertNull(payload.get(JwtClaim.LOCK_AT.key()));
        assertNull(payload.get(JwtClaim.UNLOCK_AT.key()));
        assertEquals(originalClaims.getHeader().getKeyId(), refreshedClaims.getHeader().getKeyId());
        assertEquals(originalClaims.getPayload().getIssuer(), payload.getIssuer());
        assertEquals(originalClaims.getPayload().getSubject(), payload.getSubject());
        assertEquals(originalClaims.getPayload().get(JwtClaim.CONTEXT_ID.key()), payload.get(JwtClaim.CONTEXT_ID.key()));
        assertEquals("guid1234_56789", payload.get(BrightspaceJwtClaim.BRIGHTSPACE_USER_ID.key()));
        assertEquals(BrightspaceJwtClaim.BRIGHTSPACE.key(), payload.get(JwtClaim.LMS_NAME.key()));
    }

    @Test
    public void testExtractJwtStringValueWithBearerHeaderReturnsToken() {
        when(httpServletRequest.getHeader(JwtClaim.JWT_REQUEST_HEADER_NAME.key())).thenReturn("Bearer abc123");

        assertEquals("abc123", brightspaceApiJWTService.extractJwtStringValue(httpServletRequest, false));
    }

    @Test
    public void testExtractJwtStringValueWithLowercaseBearerHeaderReturnsToken() {
        when(httpServletRequest.getHeader(JwtClaim.JWT_REQUEST_HEADER_NAME.key())).thenReturn("bearer abc123");

        assertEquals("abc123", brightspaceApiJWTService.extractJwtStringValue(httpServletRequest, false));
    }

    @Test
    public void testExtractJwtStringValueWithNoHeaderAndQueryParamAllowedReturnsParam() {
        when(httpServletRequest.getHeader(JwtClaim.JWT_REQUEST_HEADER_NAME.key())).thenReturn(null);
        when(httpServletRequest.getParameter(JwtClaim.QUERY_PARAM_NAME.key())).thenReturn("paramToken123");

        assertEquals("paramToken123", brightspaceApiJWTService.extractJwtStringValue(httpServletRequest, true));
    }

    @Test
    public void testExtractJwtStringValueWithNoHeaderAndQueryParamNotAllowedReturnsNull() {
        when(httpServletRequest.getHeader(JwtClaim.JWT_REQUEST_HEADER_NAME.key())).thenReturn(null);

        assertNull(brightspaceApiJWTService.extractJwtStringValue(httpServletRequest, false));
    }

    @Test
    public void testExtractJwtStringValueWithNonBearerHeaderReturnsNull() {
        when(httpServletRequest.getHeader(JwtClaim.JWT_REQUEST_HEADER_NAME.key())).thenReturn("Basic abc123");

        assertNull(brightspaceApiJWTService.extractJwtStringValue(httpServletRequest, false));
    }

    @Test
    public void testExtractValuesWithValidTokenMapsFields() throws GeneralSecurityException, IOException {
        String jwt = buildStandardJwt(false);

        SecuredInfo securedInfo = brightspaceApiJWTService.extractValues(jwt);

        assertNotNull(securedInfo);
        assertEquals(1L, securedInfo.getPlatformDeploymentId());
        assertEquals(100L, securedInfo.getContextId());
        assertEquals("user123", securedInfo.getUserId());
        assertEquals(List.of("Learner"), securedInfo.getRoles());
        assertEquals("56789", securedInfo.getLmsUserId());
        assertEquals("globalGuid123", securedInfo.getLmsUserGlobalId());
        assertEquals("loginId123", securedInfo.getLmsLoginId());
        assertEquals("userNameXYZ", securedInfo.getLmsUserName());
        assertEquals("courseKey123", securedInfo.getLmsCourseId());
        assertEquals("lmsAssignment123", securedInfo.getLmsAssignmentId());
        assertEquals("nonce123", securedInfo.getNonce());
        assertEquals(Boolean.TRUE, securedInfo.getConsent());
        assertEquals(3, securedInfo.getAllowedAttempts());
        assertEquals(1, securedInfo.getStudentAttempts());
        assertEquals(BrightspaceJwtClaim.BRIGHTSPACE.key(), securedInfo.getLmsName());
        // BUG: extractValues() unconditionally hardcodes dueAt/lockAt/unlockAt to null, discarding the
        // due_at/lock_at/unlock_at claims that buildJwt() actually placed into the token (Canvas's
        // equivalent method decodes them via extractTimestamp() instead of dropping them).
        assertNull(securedInfo.getDueAt());
        assertNull(securedInfo.getLockAt());
        assertNull(securedInfo.getUnlockAt());
    }

    @Test
    public void testExtractValuesWithBrightspaceUserIdWithoutUnderscoreLmsUserIdIsNull() throws GeneralSecurityException, IOException {
        String jwt = buildStandardJwt(false, "noUnderscoreId");

        SecuredInfo securedInfo = brightspaceApiJWTService.extractValues(jwt);

        assertNull(securedInfo.getLmsUserId());
    }

    @Test
    public void testExtractValuesWithInvalidTokenReturnsNull() throws GeneralSecurityException {
        String expired = buildExpiredJwt();

        assertNull(brightspaceApiJWTService.extractValues(expired));
    }

    @Test
    public void testExtractValuesFromRequestDelegatesToTokenExtraction() throws GeneralSecurityException, IOException {
        String jwt = buildStandardJwt(false);
        when(httpServletRequest.getHeader(JwtClaim.JWT_REQUEST_HEADER_NAME.key())).thenReturn("Bearer " + jwt);

        SecuredInfo securedInfo = brightspaceApiJWTService.extractValues(httpServletRequest, false);

        assertNotNull(securedInfo);
        assertEquals("user123", securedInfo.getUserId());
        assertEquals("courseKey123", securedInfo.getLmsCourseId());
    }

    @Test
    public void testGetTimedTokenFromRequestDelegates() throws GeneralSecurityException, IOException, TerracottaConnectorException {
        String jwt = buildStandardJwt(false);
        when(httpServletRequest.getHeader(JwtClaim.JWT_REQUEST_HEADER_NAME.key())).thenReturn(null);
        when(httpServletRequest.getParameter(JwtClaim.QUERY_PARAM_NAME.key())).thenReturn(jwt);

        ResponseEntity<String> response = brightspaceApiJWTService.getTimedToken(httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void testGetTimedTokenWithOneUseTokenReturnsOkWithNewToken() throws GeneralSecurityException, IOException, TerracottaConnectorException {
        String oneUseToken = buildStandardJwt(true);

        ResponseEntity<String> response = brightspaceApiJWTService.getTimedToken(oneUseToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Jws<Claims> newClaims = brightspaceApiJWTService.validateToken(response.getBody());
        assertEquals(Boolean.FALSE, newClaims.getPayload().get(JwtClaim.ONE_USE.key()));
        assertEquals("user123", newClaims.getPayload().getSubject());
    }

    @Test
    public void testGetTimedTokenWithNonOneUseTokenReturnsUnauthorized() throws GeneralSecurityException, IOException, TerracottaConnectorException {
        String jwt = buildStandardJwt(false);

        ResponseEntity<String> response = brightspaceApiJWTService.getTimedToken(jwt);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Token passed was not a one time valid token", response.getBody());
    }

    @Test
    public void testGetTimedTokenWhenBuildJwtFailsReturnsInternalServerError() throws GeneralSecurityException, IOException, TerracottaConnectorException {
        String oneUseToken = buildStandardJwt(true);
        when(ltiDataService.getOwnPrivateKey()).thenReturn("invalid-key-data");

        ResponseEntity<String> response = brightspaceApiJWTService.getTimedToken(oneUseToken);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().startsWith("Error generating token"));
    }

    @Test
    public void testGetTimedTokenWithInvalidTokenReturnsNull() throws GeneralSecurityException, TerracottaConnectorException {
        String expired = buildExpiredJwt();

        assertNull(brightspaceApiJWTService.getTimedToken(expired));
    }

    @Test
    public void testUnsupportedOperationMethodsAllThrowUnsupportedOperationException() {
        UUID uuid = UUID.randomUUID();

        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.isAdmin(null));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.isTerracottaAdmin(null));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.isInstructor(null));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.isInstructorOrHigher(null));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.isLearner(null));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.isLearnerOrHigher(null));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.isGeneral(null));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.isTestStudent(null));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.experimentAllowed(null, 1L));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.experimentLocked(1L, false));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.conditionsLocked(1L, false));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.conditionAllowed(null, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.participantAllowed(null, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.exposureAllowed(null, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.groupAllowed(null, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.assignmentAllowed(null, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.assignmentAllowed(null, 1L, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.treatmentAllowed(null, 1L, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.assessmentAllowed(null, 1L, 1L, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.questionAllowed(null, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.answerAllowed(null, 1L, 1L, "type", 1L));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.submissionAllowed(null, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.questionSubmissionAllowed(null, 1L, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.submissionCommentAllowed(null, 1L, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.questionSubmissionCommentAllowed(null, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.outcomeAllowed(null, 1L, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.outcomeScoreAllowed(null, 1L, 1L));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.answerSubmissionAllowed(null, 1L, "type", 1L));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.integrationAllowed(1L, uuid));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.experimentImportAllowed(null, uuid));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.messagingContainerAllowed(null, 1L, uuid));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.messagingContainerConfigurationAllowed(null, uuid, uuid));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.messagingAllowed(null, uuid, uuid));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.messagingContentAllowed(null, uuid, uuid));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.messagingConfigurationAllowed(null, uuid, uuid));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.messagingRuleSetAllowed(null, uuid, uuid));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.messagingRuleAllowed(null, uuid, uuid));
        assertThrows(UnsupportedOperationException.class, () -> brightspaceApiJWTService.messagingConditionalTextAllowed(null, uuid, uuid));
    }

}
