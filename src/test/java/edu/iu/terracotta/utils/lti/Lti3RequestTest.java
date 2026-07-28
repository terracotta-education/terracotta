package edu.iu.terracotta.utils.lti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.utils.LtiStrings;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;

@SuppressWarnings({"unchecked"})
public class Lti3RequestTest extends BaseTest {

    private static final String KID = "kid1";
    private static final String ISS = "issuer1";
    private static final String AUD = "client1";

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();
    }

    @AfterEach
    public void afterEach() {
        RequestContextHolder.resetRequestAttributes();
    }

    // ------------------------------------------------------------------
    // test fixtures / helpers
    // ------------------------------------------------------------------

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);

        return keyPairGenerator.generateKeyPair();
    }

    /** writes a real JWKS file to disk (unique per test) and returns its file:// URI */
    private static String writeJwksFile(String prefix, String keyId, RSAPublicKey publicKey) throws Exception {
        JWK jwk = new RSAKey.Builder(publicKey).keyID(keyId).build();
        File jwksFile = File.createTempFile("lti3request-test-" + prefix, ".json");
        jwksFile.deleteOnExit();
        Files.writeString(jwksFile.toPath(), new JWKSet(Collections.singletonList(jwk)).toString());

        return jwksFile.toURI().toString();
    }

    private static String buildJwt(PrivateKey privateKey, String kid, String iss, String aud, String sub, Date iat, Date exp, String nonce, Map<String, Object> extraClaims) {
        JwtBuilder builder = Jwts.builder().header().add("kid", kid).and();

        if (iss != null) {
            builder.issuer(iss);
        }

        if (aud != null) {
            // deliberately NOT using builder.audience().add(aud).and() here: that writes "aud" as a
            // JSON array, but Lti3Request's constructor does a pre-signature-verification raw Jackson
            // parse of the payload and casts jwtClaims.get("aud") directly to String (line ~283) - it
            // does not handle the RFC 7519 array form. Writing "aud" as a bare JSON string keeps that
            // cast valid while still round-tripping correctly through jws.getPayload().getAudience()
            // (JJWT's Claims reader accepts both the single-string and array forms per RFC 7519 4.1.3).
            builder.claim(LtiStrings.AUD, aud);
        }

        if (sub != null) {
            builder.subject(sub);
        }

        if (iat != null) {
            builder.issuedAt(iat);
        }

        if (exp != null) {
            builder.expiration(exp);
        }

        if (nonce != null) {
            builder.claim(LtiStrings.LTI_NONCE, nonce);
        }

        if (extraClaims != null) {
            extraClaims.forEach(builder::claim);
        }

        return builder.signWith(privateKey, Jwts.SIG.RS256).compact();
    }

    private static Map<String, Object> resourceLinkClaims() {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put(LtiStrings.LTI_MESSAGE_TYPE, LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);
        claims.put(LtiStrings.LTI_VERSION, LtiStrings.LTI_VERSION_3);
        claims.put(LtiStrings.LTI_DEPLOYMENT_ID, "deployment-1");
        claims.put(LtiStrings.LTI_ROLES, List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR));
        claims.put(LtiStrings.LTI_LINK, Map.of(LtiStrings.LTI_LINK_ID, "link-1"));

        return claims;
    }

    private static Map<String, Object> deepLinkingClaims() {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put(LtiStrings.LTI_MESSAGE_TYPE, LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);
        claims.put(LtiStrings.LTI_VERSION, LtiStrings.LTI_VERSION_3);
        claims.put(LtiStrings.LTI_DEPLOYMENT_ID, "deployment-1");
        claims.put(
            LtiStrings.DEEP_LINKING_SETTINGS,
            Map.of(
                LtiStrings.DEEP_LINK_RETURN_URL, "https://platform.example/return",
                LtiStrings.DEEP_LINK_ACCEPT_TYPES, List.of("ltiResourceLink"),
                LtiStrings.DEEP_LINK_DOCUMENT_TARGETS, List.of("iframe")
            )
        );

        return claims;
    }

    private void stubJwksLookup(String iss, String aud, String jwksEndpoint) {
        when(ltiDataService.getPlatformDeploymentRepository()).thenReturn(platformDeploymentRepository);
        when(platformDeploymentRepository.findByIssAndClientId(iss, aud)).thenReturn(List.of(platformDeployment));
        when(platformDeployment.getJwksEndpoint()).thenReturn(jwksEndpoint);
    }

    private void stubToolDeploymentFound() {
        when(toolDeployment.getLtiDeploymentId()).thenReturn("deployment-1");
        when(ltiDataService.findOrCreateToolDeployment(anyString(), anyString(), anyString())).thenReturn(toolDeployment);
    }

    private void stubNonceFound(String nonce) {
        when(ltiDataService.getLtiNonceRepository()).thenReturn(ltiNonceRepository);
        when(ltiNonceRepository.deleteByNonce(nonce)).thenReturn(1L);
    }

    private void stubNonceNotFound(String nonce) {
        when(ltiDataService.getLtiNonceRepository()).thenReturn(ltiNonceRepository);
        when(ltiNonceRepository.deleteByNonce(nonce)).thenReturn(0L);
    }

    private Date now() {
        return new Date();
    }

    private Date inOneHour() {
        return new Date(System.currentTimeMillis() + 3_600_000L);
    }

    // ------------------------------------------------------------------
    // constructor: basic argument validation
    // ------------------------------------------------------------------

    @Test
    void testConstructorNullRequestThrowsAssertionError() {
        assertThrows(AssertionError.class, () -> new Lti3Request(null, ltiDataService, true, "link1"));
    }

    @Test
    void testConstructorNullLtiDataServiceThrowsAssertionError() {
        assertThrows(AssertionError.class, () -> new Lti3Request(httpServletRequest, null, true, "link1"));
    }

    // ------------------------------------------------------------------
    // constructor: malformed id_token handling
    // ------------------------------------------------------------------

    @Test
    void testConstructorMalformedJwtPayloadThrowsIllegalStateException() {
        // valid base64url, but the decoded payload is not valid JSON
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString("not valid json".getBytes());
        String jwt = "header." + payload + ".sig";
        when(httpServletRequest.getParameter("id_token")).thenReturn(jwt);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new Lti3Request(httpServletRequest, ltiDataService, true, "link1"));
        assertTrue(ex.getMessage().contains("Request is not a valid LTI3 request."));
    }

    @Test
    void testConstructorNoIdTokenParameterThrowsIllegalStateException() {
        when(httpServletRequest.getParameter("id_token")).thenReturn(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new Lti3Request(httpServletRequest, ltiDataService, true, "link1"));
        assertTrue(ex.getMessage().contains("Request is not a valid LTI3 request."));
    }

    @Test
    void testConstructorNoDotsInIdTokenThrowsIllegalStateException() {
        when(httpServletRequest.getParameter("id_token")).thenReturn("not-a-jwt-at-all");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new Lti3Request(httpServletRequest, ltiDataService, true, "link1"));
        assertTrue(ex.getMessage().contains("Request is not a valid LTI3 request."));
    }

    // ------------------------------------------------------------------
    // constructor: signature verification
    // ------------------------------------------------------------------

    @Test
    void testConstructorBadSignatureThrowsSignatureException() throws Exception {
        KeyPair signingKeyPair = generateKeyPair();
        KeyPair registeredKeyPair = generateKeyPair();
        String jwksEndpoint = writeJwksFile("bad-sig", KID, (RSAPublicKey) registeredKeyPair.getPublic());
        // signed with a DIFFERENT private key than the one published in the JWKS
        String jwt = buildJwt(signingKeyPair.getPrivate(), KID, ISS, AUD, "sub-1", now(), inOneHour(), "nonce-1", resourceLinkClaims());

        when(httpServletRequest.getParameter("id_token")).thenReturn(jwt);
        stubJwksLookup(ISS, AUD, jwksEndpoint);

        assertThrows(SignatureException.class, () -> new Lti3Request(httpServletRequest, ltiDataService, true, "link1"));
    }

    @Test
    void testConstructorBlankJwksEndpointThrowsUnsupportedJwtException() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String jwt = buildJwt(keyPair.getPrivate(), KID, ISS, AUD, "sub-1", now(), inOneHour(), "nonce-1", resourceLinkClaims());

        when(httpServletRequest.getParameter("id_token")).thenReturn(jwt);
        stubJwksLookup(ISS, AUD, null);

        assertThrows(UnsupportedJwtException.class, () -> new Lti3Request(httpServletRequest, ltiDataService, true, "link1"));
    }

    // ------------------------------------------------------------------
    // constructor: message type / version validation
    // ------------------------------------------------------------------

    @Test
    void testConstructorMissingVersionThrowsIllegalStateException() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String jwksEndpoint = writeJwksFile("no-version", KID, (RSAPublicKey) keyPair.getPublic());
        Map<String, Object> claims = resourceLinkClaims();
        claims.remove(LtiStrings.LTI_VERSION);
        String jwt = buildJwt(keyPair.getPrivate(), KID, ISS, AUD, "sub-1", now(), inOneHour(), "nonce-1", claims);

        when(httpServletRequest.getParameter("id_token")).thenReturn(jwt);
        stubJwksLookup(ISS, AUD, jwksEndpoint);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new Lti3Request(httpServletRequest, ltiDataService, true, "link1"));
        assertTrue(ex.getMessage().startsWith("Request is not a valid LTI3 request: ["));
        assertTrue(ex.getMessage().contains("LTI Version = null."));
    }

    // ------------------------------------------------------------------
    // constructor: nonce validation
    // ------------------------------------------------------------------

    @Test
    void testConstructorNonceMismatchThrowsIllegalStateException() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String jwksEndpoint = writeJwksFile("nonce-mismatch", KID, (RSAPublicKey) keyPair.getPublic());
        String jwt = buildJwt(keyPair.getPrivate(), KID, ISS, AUD, "sub-1", now(), inOneHour(), "actual-nonce", resourceLinkClaims());

        when(httpServletRequest.getParameter("id_token")).thenReturn(jwt);
        stubJwksLookup(ISS, AUD, jwksEndpoint);
        stubNonceNotFound("actual-nonce");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new Lti3Request(httpServletRequest, ltiDataService, true, "link1"));
        assertTrue(ex.getMessage().startsWith("Nonce error: ["));
    }

    // ------------------------------------------------------------------
    // constructor: incomplete request (missing mandatory field)
    // ------------------------------------------------------------------

    @Test
    void testConstructorIncompleteRequestMissingSubThrowsIllegalStateException() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String jwksEndpoint = writeJwksFile("missing-sub", KID, (RSAPublicKey) keyPair.getPublic());
        String jwt = buildJwt(keyPair.getPrivate(), KID, ISS, AUD, null, now(), inOneHour(), "nonce-1", resourceLinkClaims());

        when(httpServletRequest.getParameter("id_token")).thenReturn(jwt);
        stubJwksLookup(ISS, AUD, jwksEndpoint);
        stubToolDeploymentFound();
        stubNonceFound("nonce-1");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new Lti3Request(httpServletRequest, ltiDataService, true, "link1"));
        assertTrue(ex.getMessage().startsWith("Request is not a valid LTI3 request: ["));
        assertTrue(ex.getMessage().contains("User (sub) is empty."));
    }

    // ------------------------------------------------------------------
    // constructor: tool deployment resolution
    // ------------------------------------------------------------------

    @Test
    void testConstructorToolDeploymentNotFoundThrowsIllegalStateException() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String jwksEndpoint = writeJwksFile("no-tool-deployment", KID, (RSAPublicKey) keyPair.getPublic());
        String jwt = buildJwt(keyPair.getPrivate(), KID, ISS, AUD, "sub-1", now(), inOneHour(), "nonce-1", resourceLinkClaims());

        when(httpServletRequest.getParameter("id_token")).thenReturn(jwt);
        stubJwksLookup(ISS, AUD, jwksEndpoint);
        stubNonceFound("nonce-1");
        // deliberately NOT stubbing ltiDataService.findOrCreateToolDeployment(...) -> defaults to null
        // for both the internal call in processRequestParameters and the constructor's fallback call

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new Lti3Request(httpServletRequest, ltiDataService, true, "link1"));
        assertTrue(ex.getMessage().contains("Could not find a tool deployment for iss:"));
    }

    // ------------------------------------------------------------------
    // constructor: successful end-to-end launches
    // ------------------------------------------------------------------

    @Test
    void testConstructorResourceLinkUpdateTrueCallsLoadAndUpsertLTIDataInDB() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String jwksEndpoint = writeJwksFile("rl-update-true", KID, (RSAPublicKey) keyPair.getPublic());
        String jwt = buildJwt(keyPair.getPrivate(), KID, ISS, AUD, "sub-1", now(), inOneHour(), "nonce-1", resourceLinkClaims());

        when(httpServletRequest.getParameter("id_token")).thenReturn(jwt);
        stubJwksLookup(ISS, AUD, jwksEndpoint);
        stubToolDeploymentFound();
        stubNonceFound("nonce-1");

        Lti3Request result = new Lti3Request(httpServletRequest, ltiDataService, true, "link1");

        assertEquals(ISS, result.getIss());
        assertEquals(AUD, result.getAud());
        assertEquals("sub-1", result.getSub());
        assertEquals(List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR), result.getLtiRoles());
        assertEquals("true", result.checkCompleteLTIRequest());

        verify(ltiDataService).loadAndUpsertLTIDataInDB(eq(result), eq(toolDeployment), eq("link1"));
        verify(ltiDataService, never()).loadLTIDataFromDB(any(), any());
    }

    @Test
    void testConstructorResourceLinkUpdateFalseCallsLoadLTIDataFromDB() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String jwksEndpoint = writeJwksFile("rl-update-false", KID, (RSAPublicKey) keyPair.getPublic());
        String jwt = buildJwt(keyPair.getPrivate(), KID, ISS, AUD, "sub-1", now(), inOneHour(), "nonce-1", resourceLinkClaims());

        when(httpServletRequest.getParameter("id_token")).thenReturn(jwt);
        stubJwksLookup(ISS, AUD, jwksEndpoint);
        stubToolDeploymentFound();
        stubNonceFound("nonce-1");

        Lti3Request result = new Lti3Request(httpServletRequest, ltiDataService, false, "link1");

        assertEquals("true", result.checkCompleteLTIRequest());
        verify(ltiDataService).loadLTIDataFromDB(eq(result), eq("link1"));
        verify(ltiDataService, never()).loadAndUpsertLTIDataInDB(any(), any(), any());
    }

    @Test
    void testConstructorDeepLinkingUpdateTrueCallsLoadAndUpsertWithNullLink() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String jwksEndpoint = writeJwksFile("dl-update-true", KID, (RSAPublicKey) keyPair.getPublic());
        String jwt = buildJwt(keyPair.getPrivate(), KID, ISS, AUD, "sub-1", now(), inOneHour(), "nonce-1", deepLinkingClaims());

        when(httpServletRequest.getParameter("id_token")).thenReturn(jwt);
        stubJwksLookup(ISS, AUD, jwksEndpoint);
        stubToolDeploymentFound();
        stubNonceFound("nonce-1");

        Lti3Request result = new Lti3Request(httpServletRequest, ltiDataService, true, "link1");

        assertEquals("true", result.checkCompleteDeepLinkingRequest());
        // link argument is null for deep linking, NOT linkId
        verify(ltiDataService).loadAndUpsertLTIDataInDB(eq(result), eq(toolDeployment), isNull());
        verify(ltiDataService, never()).loadLTIDataFromDB(any(), any());
    }

    // ------------------------------------------------------------------
    // getInstance / getInstanceOrDie
    // ------------------------------------------------------------------

    @Test
    void testGetInstanceOrDieReturnsExistingRequestAttribute() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        Lti3Request existing = mock(Lti3Request.class);
        mockRequest.setAttribute(Lti3Request.class.getName(), existing);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));

        Lti3Request result = Lti3Request.getInstanceOrDie("link1");

        assertSame(existing, result);
        verifyNoInteractions(ltiDataService);
    }

    @Test
    void testGetInstanceReturnsExistingRequestAttribute() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        Lti3Request existing = mock(Lti3Request.class);
        mockRequest.setAttribute(Lti3Request.class.getName(), existing);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));

        Lti3Request result = Lti3Request.getInstance("link1");

        assertSame(existing, result);
    }

    @Test
    void testGetInstanceOrDieNoRequestAttributesThrowsIllegalStateException() {
        RequestContextHolder.resetRequestAttributes();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> Lti3Request.getInstanceOrDie("link1"));
        assertTrue(ex.getMessage().contains("cannot get the LTIRequest"));
    }

    @Test
    void testGetInstanceNoRequestAttributesReturnsNull() {
        RequestContextHolder.resetRequestAttributes();

        assertNull(Lti3Request.getInstance("link1"));
    }

    // ------------------------------------------------------------------
    // checkCompleteLTIRequest(boolean objects)
    // ------------------------------------------------------------------

    private Lti3Request objectsMock() {
        Lti3Request req = mock(Lti3Request.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(req, "key", platformDeployment);
        ReflectionTestUtils.setField(req, "context", ltiContextEntity);
        ReflectionTestUtils.setField(req, "link", ltiLinkEntity);
        ReflectionTestUtils.setField(req, "user", ltiUserEntity);

        return req;
    }

    @Test
    void testCheckCompleteLTIRequestObjectsTrueAllPresentReturnsTrue() {
        assertTrue(objectsMock().checkCompleteLTIRequest(true));
    }

    @Test
    void testCheckCompleteLTIRequestObjectsFalseReturnsFalse() {
        assertFalse(objectsMock().checkCompleteLTIRequest(false));
    }

    @Test
    void testCheckCompleteLTIRequestObjectsTrueKeyNullReturnsFalse() {
        Lti3Request req = objectsMock();
        ReflectionTestUtils.setField(req, "key", null);

        assertFalse(req.checkCompleteLTIRequest(true));
    }

    @Test
    void testCheckCompleteLTIRequestObjectsTrueContextNullReturnsFalse() {
        Lti3Request req = objectsMock();
        ReflectionTestUtils.setField(req, "context", null);

        assertFalse(req.checkCompleteLTIRequest(true));
    }

    @Test
    void testCheckCompleteLTIRequestObjectsTrueLinkNullReturnsFalse() {
        Lti3Request req = objectsMock();
        ReflectionTestUtils.setField(req, "link", null);

        assertFalse(req.checkCompleteLTIRequest(true));
    }

    @Test
    void testCheckCompleteLTIRequestObjectsTrueUserNullReturnsFalse() {
        Lti3Request req = objectsMock();
        ReflectionTestUtils.setField(req, "user", null);

        assertFalse(req.checkCompleteLTIRequest(true));
    }

    // ------------------------------------------------------------------
    // checkCompleteLTIRequest() [String]
    // ------------------------------------------------------------------

    private Lti3Request completeLtiRequestFieldsMock() {
        Lti3Request req = mock(Lti3Request.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(req, "ltiDeploymentId", "deployment-1");
        ReflectionTestUtils.setField(req, "ltiResourceLink", Map.of(LtiStrings.LTI_LINK_ID, "link-1"));
        ReflectionTestUtils.setField(req, "ltiLinkId", "link-1");
        ReflectionTestUtils.setField(req, "sub", "user-1");
        ReflectionTestUtils.setField(req, "ltiRoles", List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR));
        ReflectionTestUtils.setField(req, "exp", new Date());
        ReflectionTestUtils.setField(req, "iat", new Date());

        return req;
    }

    @Test
    void testCheckCompleteLTIRequestAllPresentReturnsTrue() {
        assertEquals("true", completeLtiRequestFieldsMock().checkCompleteLTIRequest());
    }

    @Test
    void testCheckCompleteLTIRequestMissingDeploymentId() {
        Lti3Request req = completeLtiRequestFieldsMock();
        ReflectionTestUtils.setField(req, "ltiDeploymentId", null);

        String result = req.checkCompleteLTIRequest();
        assertNotEquals("true", result);
        assertTrue(result.contains("Lti Deployment Id is empty."));
    }

    @Test
    void testCheckCompleteLTIRequestMissingResourceLink() {
        Lti3Request req = completeLtiRequestFieldsMock();
        ReflectionTestUtils.setField(req, "ltiResourceLink", Map.of());

        String result = req.checkCompleteLTIRequest();
        assertNotEquals("true", result);
        assertTrue(result.contains("Lti Resource Link is empty."));
    }

    @Test
    void testCheckCompleteLTIRequestResourceLinkPresentButMissingLinkId() {
        Lti3Request req = completeLtiRequestFieldsMock();
        ReflectionTestUtils.setField(req, "ltiResourceLink", Map.of("other-key", "x"));
        ReflectionTestUtils.setField(req, "ltiLinkId", null);

        String result = req.checkCompleteLTIRequest();
        assertNotEquals("true", result);
        assertTrue(result.contains("Lti Resource Link ID is empty."));
    }

    @Test
    void testCheckCompleteLTIRequestMissingSub() {
        Lti3Request req = completeLtiRequestFieldsMock();
        ReflectionTestUtils.setField(req, "sub", "");

        String result = req.checkCompleteLTIRequest();
        assertNotEquals("true", result);
        assertTrue(result.contains("User (sub) is empty."));
    }

    @Test
    void testCheckCompleteLTIRequestMissingRoles() {
        Lti3Request req = completeLtiRequestFieldsMock();
        ReflectionTestUtils.setField(req, "ltiRoles", null);

        String result = req.checkCompleteLTIRequest();
        assertNotEquals("true", result);
        assertTrue(result.contains("Lti Roles is empty."));
    }

    @Test
    void testCheckCompleteLTIRequestMissingExp() {
        Lti3Request req = completeLtiRequestFieldsMock();
        ReflectionTestUtils.setField(req, "exp", null);

        String result = req.checkCompleteLTIRequest();
        assertNotEquals("true", result);
        assertTrue(result.contains("Exp is empty or invalid."));
    }

    @Test
    void testCheckCompleteLTIRequestMissingIat() {
        Lti3Request req = completeLtiRequestFieldsMock();
        ReflectionTestUtils.setField(req, "iat", null);

        String result = req.checkCompleteLTIRequest();
        assertNotEquals("true", result);
        assertTrue(result.contains("Iat is empty or invalid."));
    }

    // ------------------------------------------------------------------
    // checkCompleteDeepLinkingRequest()
    // ------------------------------------------------------------------

    private Lti3Request completeDeepLinkingFieldsMock() {
        Lti3Request req = mock(Lti3Request.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(req, "ltiDeploymentId", "deployment-1");
        ReflectionTestUtils.setField(req, "sub", "user-1");
        ReflectionTestUtils.setField(req, "exp", new Date());
        ReflectionTestUtils.setField(req, "iat", new Date());
        ReflectionTestUtils.setField(req, "deepLinkingSettings", Map.of("k", "v"));
        ReflectionTestUtils.setField(req, "deepLinkReturnUrl", "https://platform.example/return");
        ReflectionTestUtils.setField(req, "deepLinkAcceptTypes", List.of("ltiResourceLink"));
        ReflectionTestUtils.setField(req, "deepLinkAcceptPresentationDocumentTargets", List.of("iframe"));

        return req;
    }

    @Test
    void testCheckCompleteDeepLinkingRequestAllPresentReturnsTrue() {
        assertEquals("true", completeDeepLinkingFieldsMock().checkCompleteDeepLinkingRequest());
    }

    @Test
    void testCheckCompleteDeepLinkingRequestMissingDeploymentId() {
        Lti3Request req = completeDeepLinkingFieldsMock();
        ReflectionTestUtils.setField(req, "ltiDeploymentId", null);

        String result = req.checkCompleteDeepLinkingRequest();
        assertNotEquals("true", result);
        assertTrue(result.contains("Lti Deployment Id is empty."));
    }

    @Test
    void testCheckCompleteDeepLinkingRequestMissingSub() {
        Lti3Request req = completeDeepLinkingFieldsMock();
        ReflectionTestUtils.setField(req, "sub", null);

        String result = req.checkCompleteDeepLinkingRequest();
        assertNotEquals("true", result);
        assertTrue(result.contains("User (sub) is empty."));
    }

    @Test
    void testCheckCompleteDeepLinkingRequestMissingExp() {
        Lti3Request req = completeDeepLinkingFieldsMock();
        ReflectionTestUtils.setField(req, "exp", null);

        String result = req.checkCompleteDeepLinkingRequest();
        assertNotEquals("true", result);
        assertTrue(result.contains("Exp is empty or invalid."));
    }

    @Test
    void testCheckCompleteDeepLinkingRequestMissingIat() {
        Lti3Request req = completeDeepLinkingFieldsMock();
        ReflectionTestUtils.setField(req, "iat", null);

        String result = req.checkCompleteDeepLinkingRequest();
        assertNotEquals("true", result);
        assertTrue(result.contains("Iat is empty or invalid."));
    }

    @Test
    void testCheckCompleteDeepLinkingRequestMissingSettings() {
        Lti3Request req = completeDeepLinkingFieldsMock();
        ReflectionTestUtils.setField(req, "deepLinkingSettings", Map.of());

        String result = req.checkCompleteDeepLinkingRequest();
        assertNotEquals("true", result);
        assertTrue(result.contains("DeepLinkingSettings is empty or invalid."));
    }

    @Test
    void testCheckCompleteDeepLinkingRequestMissingReturnUrl() {
        Lti3Request req = completeDeepLinkingFieldsMock();
        ReflectionTestUtils.setField(req, "deepLinkReturnUrl", null);

        String result = req.checkCompleteDeepLinkingRequest();
        assertNotEquals("true", result);
        assertTrue(result.contains("deepLinkReturnUrl is empty."));
    }

    @Test
    void testCheckCompleteDeepLinkingRequestMissingAcceptTypes() {
        Lti3Request req = completeDeepLinkingFieldsMock();
        ReflectionTestUtils.setField(req, "deepLinkAcceptTypes", null);

        String result = req.checkCompleteDeepLinkingRequest();
        assertNotEquals("true", result);
        assertTrue(result.contains("deepLink AcceptTypes is empty."));
    }

    @Test
    void testCheckCompleteDeepLinkingRequestMissingDocumentTargets() {
        Lti3Request req = completeDeepLinkingFieldsMock();
        ReflectionTestUtils.setField(req, "deepLinkAcceptPresentationDocumentTargets", null);

        String result = req.checkCompleteDeepLinkingRequest();
        assertNotEquals("true", result);
        assertTrue(result.contains("deepLink AcceptPresentationDocumentTargets is empty."));
    }

    // ------------------------------------------------------------------
    // checkNonce(Jws<Claims> jws)
    // ------------------------------------------------------------------

    private Lti3Request nonceCheckMock() {
        Lti3Request req = mock(Lti3Request.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(req, "ltiDataService", ltiDataService);

        return req;
    }

    private Jws<Claims> jwsWithNonce(String nonce) {
        Jws<Claims> jws = mock(Jws.class);
        Claims claims = mock(Claims.class);
        when(jws.getPayload()).thenReturn(claims);
        when(claims.get(LtiStrings.LTI_NONCE, String.class)).thenReturn(nonce);

        return jws;
    }

    @Test
    void testCheckNonceNullNonceClaimReturnsError() {
        Lti3Request req = nonceCheckMock();

        assertEquals("Nonce = null in the JWT.", req.checkNonce(jwsWithNonce(null)));
    }

    @Test
    void testCheckNonceFoundReturnsTrueAndConsumesNonce() {
        Lti3Request req = nonceCheckMock();
        stubNonceFound("nonce-1");

        String result = req.checkNonce(jwsWithNonce("nonce-1"));

        assertEquals("true", result);
        verify(ltiNonceRepository).deleteByNonce("nonce-1");
    }

    @Test
    void testCheckNonceNotFoundReturnsError() {
        Lti3Request req = nonceCheckMock();
        stubNonceNotFound("nonce-1");

        assertEquals("Unknown or already used nounce.", req.checkNonce(jwsWithNonce("nonce-1")));
    }

    // ------------------------------------------------------------------
    // isLti3Request(Jws<Claims> jws) [static]
    // ------------------------------------------------------------------

    private Jws<Claims> jwsWithVersionAndMessageType(String version, String messageType) {
        Jws<Claims> jws = mock(Jws.class);
        Claims claims = mock(Claims.class);
        when(jws.getPayload()).thenReturn(claims);
        when(claims.get(LtiStrings.LTI_VERSION, String.class)).thenReturn(version);
        when(claims.get(LtiStrings.LTI_MESSAGE_TYPE, String.class)).thenReturn(messageType);

        return jws;
    }

    @Test
    void testIsLti3RequestResourceLinkValid() {
        Jws<Claims> jws = jwsWithVersionAndMessageType(LtiStrings.LTI_VERSION_3, LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);

        assertEquals(LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK, Lti3Request.isLti3Request(jws));
    }

    @Test
    void testIsLti3RequestDeepLinkingValid() {
        Jws<Claims> jws = jwsWithVersionAndMessageType(LtiStrings.LTI_VERSION_3, LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING);

        assertEquals(LtiStrings.LTI_MESSAGE_TYPE_DEEP_LINKING, Lti3Request.isLti3Request(jws));
    }

    @Test
    void testIsLti3RequestNullVersionReturnsError() {
        Jws<Claims> jws = jwsWithVersionAndMessageType(null, LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);

        assertEquals("LTI Version = null. ", Lti3Request.isLti3Request(jws));
    }

    @Test
    void testIsLti3RequestNullMessageTypeReturnsError() {
        Jws<Claims> jws = jwsWithVersionAndMessageType(LtiStrings.LTI_VERSION_3, null);

        assertEquals("LTI Message Type = null. ", Lti3Request.isLti3Request(jws));
    }

    @Test
    void testIsLti3RequestWrongMessageTypeReturnsError() {
        Jws<Claims> jws = jwsWithVersionAndMessageType(LtiStrings.LTI_VERSION_3, "SomethingElse");

        assertEquals("LTI Message Type is not right: SomethingElse. ", Lti3Request.isLti3Request(jws));
    }

    @Test
    void testIsLti3RequestWrongVersionReturnsError() {
        Jws<Claims> jws = jwsWithVersionAndMessageType("2.0", LtiStrings.LTI_MESSAGE_TYPE_RESOURCE_LINK);

        assertEquals("LTI Version is not right: 2.0", Lti3Request.isLti3Request(jws));
    }

    // ------------------------------------------------------------------
    // isRoleAdministrator / isRoleInstructor / isRoleLearner
    // ------------------------------------------------------------------

    private Lti3Request roleMock(List<String> ltiRoles, int userRoleNumber) {
        Lti3Request req = mock(Lti3Request.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(req, "ltiRoles", ltiRoles);
        ReflectionTestUtils.setField(req, "userRoleNumber", userRoleNumber);

        return req;
    }

    @Test
    void testIsRoleAdministratorTrueWhenRoleNumberTwo() {
        assertTrue(roleMock(List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_ADMIN), 2).isRoleAdministrator());
    }

    @Test
    void testIsRoleAdministratorFalseWhenRoleNumberOne() {
        assertFalse(roleMock(List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR), 1).isRoleAdministrator());
    }

    @Test
    void testIsRoleAdministratorFalseWhenRolesNull() {
        assertFalse(roleMock(null, 2).isRoleAdministrator());
    }

    @Test
    void testIsRoleInstructorTrueWhenRoleNumberOne() {
        assertTrue(roleMock(List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR), 1).isRoleInstructor());
    }

    @Test
    void testIsRoleInstructorTrueWhenRoleNumberTwo() {
        assertTrue(roleMock(List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_ADMIN), 2).isRoleInstructor());
    }

    @Test
    void testIsRoleInstructorFalseWhenRoleNumberZero() {
        assertFalse(roleMock(List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_LEARNER), 0).isRoleInstructor());
    }

    @Test
    void testIsRoleInstructorFalseWhenRolesNull() {
        assertFalse(roleMock(null, 1).isRoleInstructor());
    }

    @Test
    void testIsRoleLearnerTrueWhenRolesContainLearner() {
        assertTrue(roleMock(List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_LEARNER), 0).isRoleLearner());
    }

    @Test
    void testIsRoleLearnerFalseWhenRolesDoNotContainLearner() {
        assertFalse(roleMock(List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR), 1).isRoleLearner());
    }

    @Test
    void testIsRoleLearnerFalseWhenRolesAreNull() {
        assertFalse(roleMock(null, 0).isRoleLearner());
    }

    // ------------------------------------------------------------------
    // makeUserRoleNum(List<String> rawUserRoles)
    // ------------------------------------------------------------------

    private Lti3Request plainMock() {
        return mock(Lti3Request.class, CALLS_REAL_METHODS);
    }

    @Test
    void testMakeUserRoleNumNullListReturnsZero() {
        assertEquals(0, plainMock().makeUserRoleNum(null));
    }

    @Test
    void testMakeUserRoleNumAdminReturnsTwo() {
        assertEquals(2, plainMock().makeUserRoleNum(List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_ADMIN)));
    }

    @Test
    void testMakeUserRoleNumInstructorReturnsOne() {
        assertEquals(1, plainMock().makeUserRoleNum(List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR)));
    }

    @Test
    void testMakeUserRoleNumNeitherReturnsZero() {
        assertEquals(0, plainMock().makeUserRoleNum(List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_LEARNER)));
    }

    @Test
    void testMakeUserRoleNumBothAdminAndInstructorReturnsTwo() {
        assertEquals(2, plainMock().makeUserRoleNum(List.of(LtiStrings.LTI_ROLE_MEMBERSHIP_ADMIN, LtiStrings.LTI_ROLE_MEMBERSHIP_INSTRUCTOR)));
    }

}
