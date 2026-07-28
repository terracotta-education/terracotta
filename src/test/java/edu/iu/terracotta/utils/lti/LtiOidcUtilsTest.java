package edu.iu.terracotta.utils.lti;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.LoginInitiationDto;
import edu.iu.terracotta.connectors.generic.service.lti.LtiDataService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

/**
 * {@link LtiOidcUtils} is a {@code final} utility class with a private constructor and a single
 * static method that builds/signs a real JWT. LtiDataService is mocked (it's an interface with no
 * usable real implementation here); PlatformDeployment and LoginInitiationDto are real,
 * builder-constructed domain objects since both are simple Lombok POJOs.
 */
public class LtiOidcUtilsTest {

    @Mock private LtiDataService ltiDataService;

    private KeyPair keyPair;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();

        when(ltiDataService.getOwnPrivateKey()).thenReturn(toPrivateKeyPem(keyPair.getPrivate()));
    }

    private static String toPrivateKeyPem(PrivateKey privateKey) {
        return "-----BEGIN PRIVATE KEY-----\n" + Base64.getEncoder().encodeToString(privateKey.getEncoded()) + "\n-----END PRIVATE KEY-----";
    }

    // private constructor

    @Test
    public void testPrivateConstructorThrowsIllegalStateException() throws Exception {
        Constructor<LtiOidcUtils> constructor = LtiOidcUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);

        assertTrue(exception.getCause() instanceof IllegalStateException);
        assertEquals("Utility class", exception.getCause().getMessage());
    }

    // generateState

    @Test
    public void testGenerateStateClaims() throws Exception {
        PlatformDeployment platformDeployment = PlatformDeployment.builder()
            .iss("https://platform.example.com")
            .clientId("platform-client-id")
            .build();

        Map<String, String> authRequestMap = new HashMap<>();
        authRequestMap.put("nonce", "nonce-value-123");

        LoginInitiationDto loginInitiationDto = LoginInitiationDto.builder()
            .iss("https://login-iss.example.com")
            .loginHint("login-hint-1")
            .ltiMessageHint("message-hint-1")
            .targetLinkUri("https://target.example.com/link")
            .clientId("dto-client-id")
            .deploymentId("dto-deployment-id")
            .build();

        String clientIdValue = "explicit-client-id";
        String deploymentIdValue = "explicit-deployment-id";

        String state = LtiOidcUtils.generateState(ltiDataService, platformDeployment, authRequestMap, loginInitiationDto, clientIdValue, deploymentIdValue, false);

        Jws<Claims> parsed = Jwts.parser().verifyWith(keyPair.getPublic()).build().parseSignedClaims(state);
        Claims claims = parsed.getPayload();

        assertEquals("ltiStarter", claims.getIssuer());
        assertEquals(platformDeployment.getIss(), claims.getSubject());
        assertTrue(claims.getAudience().contains(platformDeployment.getClientId()));
        assertEquals("nonce-value-123", claims.getId());
        assertEquals(loginInitiationDto.getIss(), claims.get("original_iss", String.class));
        assertEquals(loginInitiationDto.getLoginHint(), claims.get("loginHint", String.class));
        assertEquals(loginInitiationDto.getLtiMessageHint(), claims.get("ltiMessageHint", String.class));
        assertEquals(loginInitiationDto.getTargetLinkUri(), claims.get("targetLinkUri", String.class));
        // these come from the method's explicit clientIdValue/deploymentIdValue params, NOT from
        // loginInitiationDto.getClientId()/getDeploymentId() - assert the two are distinguishable.
        assertEquals(clientIdValue, claims.get("clientId", String.class));
        assertEquals(deploymentIdValue, claims.get("ltiDeploymentId", String.class));
        assertEquals("/oidc/login_initiations", claims.get("controller", String.class));

        assertEquals(claims.getIssuedAt(), claims.getNotBefore());

        long expirySeconds = (claims.getExpiration().getTime() - claims.getNotBefore().getTime()) / 1000;
        assertTrue(Math.abs(expirySeconds - 3600) <= 2, "expected expiration ~3600s after notBefore, was " + expirySeconds + "s");
    }

    @Test
    public void testGenerateStateVerboseLoggingTrueDoesNotThrow() {
        PlatformDeployment platformDeployment = PlatformDeployment.builder()
            .iss("https://platform.example.com")
            .clientId("platform-client-id")
            .build();

        Map<String, String> authRequestMap = new HashMap<>();
        authRequestMap.put("nonce", "nonce-value-456");

        LoginInitiationDto loginInitiationDto = LoginInitiationDto.builder()
            .iss("https://login-iss.example.com")
            .loginHint("login-hint-2")
            .ltiMessageHint("message-hint-2")
            .targetLinkUri("https://target.example.com/link2")
            .build();

        assertDoesNotThrow(() -> LtiOidcUtils.generateState(ltiDataService, platformDeployment, authRequestMap, loginInitiationDto, "client-id", "deployment-id", true));
    }

    @Test
    public void testGenerateStateVerboseLoggingFalseDoesNotThrow() {
        PlatformDeployment platformDeployment = PlatformDeployment.builder()
            .iss("https://platform.example.com")
            .clientId("platform-client-id")
            .build();

        Map<String, String> authRequestMap = new HashMap<>();
        authRequestMap.put("nonce", "nonce-value-789");

        LoginInitiationDto loginInitiationDto = LoginInitiationDto.builder()
            .iss("https://login-iss.example.com")
            .loginHint("login-hint-3")
            .ltiMessageHint("message-hint-3")
            .targetLinkUri("https://target.example.com/link3")
            .build();

        assertDoesNotThrow(() -> LtiOidcUtils.generateState(ltiDataService, platformDeployment, authRequestMap, loginInitiationDto, "client-id", "deployment-id", false));
    }

}
