package edu.iu.terracotta.connectors.generic.service.lti.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import edu.iu.terracotta.base.BaseTest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;

public class LtiJwtServiceImplTest extends BaseTest {

    @InjectMocks private LtiJwtServiceImpl ltiJwtService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);

        return keyPairGenerator.generateKeyPair();
    }

    private static String toPublicKeyBase64(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    private static String toPrivateKeyPem(PrivateKey privateKey) {
        return "-----BEGIN PRIVATE KEY-----\n" + Base64.getEncoder().encodeToString(privateKey.getEncoded()) + "\n-----END PRIVATE KEY-----";
    }

    private static String writeJwksFile(File file, String keyId, PublicKey publicKey) throws Exception {
        JWK jwk = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) publicKey).keyID(keyId).build();
        Files.writeString(file.toPath(), new JWKSet(Collections.singletonList(jwk)).toString());

        return file.toURI().toString();
    }

    private static String decodePayload(String jwt) {
        String[] sections = jwt.split("\\.");

        return new String(Base64.getUrlDecoder().decode(sections[1]));
    }

    // validateState

    @Test
    public void testValidateStateSuccess() throws Exception {
        KeyPair keyPair = generateKeyPair();
        when(ltiDataService.getOwnPrivateKey()).thenReturn(toPrivateKeyPem(keyPair.getPrivate()));
        when(ltiDataService.getOwnPublicKey()).thenReturn(toPublicKeyBase64(keyPair.getPublic()));
        when(platformDeployment.getClientId()).thenReturn("client1");
        when(platformDeployment.getOAuth2TokenUrl()).thenReturn("https://example.com/token");

        String state = ltiJwtService.generateTokenRequestJWT(platformDeployment);

        Jws<Claims> result = ltiJwtService.validateState(state);

        assertNotNull(result);
        assertEquals("client1", result.getPayload().getIssuer());
        assertEquals("client1", result.getPayload().getSubject());
    }

    @Test
    public void testValidateStateBadSignatureThrows() throws Exception {
        KeyPair signingKeyPair = generateKeyPair();
        KeyPair otherKeyPair = generateKeyPair();
        when(ltiDataService.getOwnPrivateKey()).thenReturn(toPrivateKeyPem(signingKeyPair.getPrivate()));
        when(platformDeployment.getClientId()).thenReturn("client1");
        when(platformDeployment.getOAuth2TokenUrl()).thenReturn("https://example.com/token");

        String state = ltiJwtService.generateTokenRequestJWT(platformDeployment);

        // the public key returned for validation does not match the private key used to sign
        when(ltiDataService.getOwnPublicKey()).thenReturn(toPublicKeyBase64(otherKeyPair.getPublic()));

        assertThrows(SignatureException.class, () -> ltiJwtService.validateState(state));
    }

    // generateTokenRequestJWT

    @Test
    public void testGenerateTokenRequestJWTUsesOAuth2TokenAudWhenPresent() throws Exception {
        KeyPair keyPair = generateKeyPair();
        when(ltiDataService.getOwnPrivateKey()).thenReturn(toPrivateKeyPem(keyPair.getPrivate()));
        when(platformDeployment.getClientId()).thenReturn("client1");
        when(platformDeployment.getOAuth2TokenUrl()).thenReturn("https://example.com/token");
        when(platformDeployment.getOAuth2TokenAud()).thenReturn("https://example.com/aud");

        String jwt = ltiJwtService.generateTokenRequestJWT(platformDeployment);

        JSONObject payload = new JSONObject(decodePayload(jwt));
        assertEquals("https://example.com/aud", payload.getString("aud"));
    }

    @Test
    public void testGenerateTokenRequestJWTFallsBackToOAuth2TokenUrlWhenAudAbsent() throws Exception {
        KeyPair keyPair = generateKeyPair();
        when(ltiDataService.getOwnPrivateKey()).thenReturn(toPrivateKeyPem(keyPair.getPrivate()));
        when(platformDeployment.getClientId()).thenReturn("client1");
        when(platformDeployment.getOAuth2TokenUrl()).thenReturn("https://example.com/token");
        when(platformDeployment.getOAuth2TokenAud()).thenReturn(null);

        String jwt = ltiJwtService.generateTokenRequestJWT(platformDeployment);

        JSONObject payload = new JSONObject(decodePayload(jwt));
        assertEquals("https://example.com/token", payload.getString("aud"));
    }

    // validateJWT

    private String buildJwt(String kid, String issuer, PrivateKey privateKey) {
        return Jwts.builder()
            .header().add("kid", kid).and()
            .claim("iss", issuer)
            .signWith(privateKey, Jwts.SIG.RS256)
            .compact();
    }

    @Test
    public void testValidateJWTSuccess() throws Exception {
        KeyPair keyPair = generateKeyPair();
        File jwksFile = File.createTempFile("lti-jwt-service-test", ".json");
        jwksFile.deleteOnExit();
        String jwksEndpoint = writeJwksFile(jwksFile, "kid-success", keyPair.getPublic());

        String jwt = buildJwt("kid-success", "issuer-success", keyPair.getPrivate());

        when(platformDeployment.getJwksEndpoint()).thenReturn(jwksEndpoint);
        when(platformDeploymentRepository.findByIssAndClientId(anyString(), anyString())).thenReturn(List.of(platformDeployment));

        Jws<Claims> result = ltiJwtService.validateJWT(jwt, "client1");

        assertNotNull(result);
        assertEquals("issuer-success", result.getPayload().getIssuer());
    }

    @Test
    public void testValidateJWTNoDeploymentFoundThrows() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String jwt = buildJwt("kid-none", "issuer-none", keyPair.getPrivate());

        when(platformDeploymentRepository.findByIssAndClientId(anyString(), anyString())).thenReturn(Collections.emptyList());

        assertThrows(UnsupportedJwtException.class, () -> ltiJwtService.validateJWT(jwt, "client1"));
    }

    @Test
    public void testValidateJWTBlankJwksEndpointThrows() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String jwt = buildJwt("kid-blank", "issuer-blank", keyPair.getPrivate());

        when(platformDeployment.getJwksEndpoint()).thenReturn(null);
        when(platformDeploymentRepository.findByIssAndClientId(anyString(), anyString())).thenReturn(List.of(platformDeployment));

        assertThrows(UnsupportedJwtException.class, () -> ltiJwtService.validateJWT(jwt, "client1"));
    }

    @Test
    public void testValidateJWTKidNotFoundThrows() throws Exception {
        KeyPair keyPair = generateKeyPair();
        File jwksFile = File.createTempFile("lti-jwt-service-test-kid", ".json");
        jwksFile.deleteOnExit();
        // JWKS is served, but does NOT contain the kid referenced by the JWT header
        String jwksEndpoint = writeJwksFile(jwksFile, "some-other-kid", keyPair.getPublic());

        String jwt = buildJwt("kid-missing", "issuer-missing", keyPair.getPrivate());

        when(platformDeployment.getJwksEndpoint()).thenReturn(jwksEndpoint);
        when(platformDeploymentRepository.findByIssAndClientId(anyString(), anyString())).thenReturn(List.of(platformDeployment));

        assertThrows(UnsupportedJwtException.class, () -> ltiJwtService.validateJWT(jwt, "client1"));
    }

    @Test
    public void testValidateJWTInvalidJwksUriThrows() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String jwt = buildJwt("kid-uri", "issuer-uri", keyPair.getPrivate());

        // a space makes this an invalid URI, triggering URISyntaxException inside JwksCache
        when(platformDeployment.getJwksEndpoint()).thenReturn("http://exa mple.com/jwks.json");
        when(platformDeploymentRepository.findByIssAndClientId(anyString(), anyString())).thenReturn(List.of(platformDeployment));

        assertThrows(UnsupportedJwtException.class, () -> ltiJwtService.validateJWT(jwt, "client1"));
    }

    @Test
    public void testValidateJWTJwksFetchIOExceptionThrows() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String jwt = buildJwt("kid-io", "issuer-io", keyPair.getPrivate());

        File missingFile = new File(Files.createTempDirectory("lti-jwt-service-test-io").toFile(), "does-not-exist.json");

        when(platformDeployment.getJwksEndpoint()).thenReturn(missingFile.toURI().toString());
        when(platformDeploymentRepository.findByIssAndClientId(anyString(), anyString())).thenReturn(List.of(platformDeployment));

        assertThrows(UnsupportedJwtException.class, () -> ltiJwtService.validateJWT(jwt, "client1"));
    }

    @Test
    public void testValidateJWTNoJwtSectionsThrows() {
        assertThrows(Exception.class, () -> ltiJwtService.validateJWT("not-a-jwt", "client1"));
    }

    // validateJWT(jwt) - no independently-known clientId (e.g. an LTI notice, which arrives with
    // no accompanying state/session to source one from)

    @Test
    public void testValidateJWTNoClientIdReadsAudAsSingleString() throws Exception {
        KeyPair keyPair = generateKeyPair();
        File jwksFile = File.createTempFile("lti-jwt-service-test-aud-string", ".json");
        jwksFile.deleteOnExit();
        String jwksEndpoint = writeJwksFile(jwksFile, "kid-aud-string", keyPair.getPublic());

        String jwt = Jwts.builder()
            .header().add("kid", "kid-aud-string").and()
            .claim("iss", "issuer-aud-string")
            .claim("aud", "client-from-aud")
            .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
            .compact();

        when(platformDeployment.getJwksEndpoint()).thenReturn(jwksEndpoint);
        when(platformDeploymentRepository.findByIssAndClientId(anyString(), anyString())).thenReturn(List.of(platformDeployment));

        Jws<Claims> result = ltiJwtService.validateJWT(jwt);

        assertNotNull(result);
        assertEquals("issuer-aud-string", result.getPayload().getIssuer());
        verify(platformDeploymentRepository).findByIssAndClientId("issuer-aud-string", "client-from-aud");
    }

    @Test
    public void testValidateJWTNoClientIdReadsFirstAudFromArray() throws Exception {
        KeyPair keyPair = generateKeyPair();
        File jwksFile = File.createTempFile("lti-jwt-service-test-aud-array", ".json");
        jwksFile.deleteOnExit();
        String jwksEndpoint = writeJwksFile(jwksFile, "kid-aud-array", keyPair.getPublic());

        String jwt = Jwts.builder()
            .header().add("kid", "kid-aud-array").and()
            .claim("iss", "issuer-aud-array")
            .audience().add("client-first").add("client-second").and()
            .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
            .compact();

        when(platformDeployment.getJwksEndpoint()).thenReturn(jwksEndpoint);
        when(platformDeploymentRepository.findByIssAndClientId(anyString(), anyString())).thenReturn(List.of(platformDeployment));

        Jws<Claims> result = ltiJwtService.validateJWT(jwt);

        assertNotNull(result);
        verify(platformDeploymentRepository).findByIssAndClientId("issuer-aud-array", "client-first");
    }

    @Test
    public void testValidateJWTNoClientIdMalformedJwtThrows() {
        assertThrows(IllegalStateException.class, () -> ltiJwtService.validateJWT("not-a-jwt"));
    }

}
