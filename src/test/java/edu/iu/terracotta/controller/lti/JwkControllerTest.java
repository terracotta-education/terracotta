package edu.iu.terracotta.controller.lti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import edu.iu.terracotta.base.BaseTest;

public class JwkControllerTest extends BaseTest {

    @InjectMocks private JwkController jwkController;

    private RSAPublicKey rsaPublicKey;
    private Model model;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        model = new ExtendedModelMap();

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        rsaPublicKey = (RSAPublicKey) keyPair.getPublic();

        // OAuthUtils.loadPublicKey only strips literal "\n" (backslash-n) sequences, not real
        // newline characters, so the key is built as a single line to avoid tripping over that.
        String pem = "-----BEGIN PUBLIC KEY-----" + Base64.getEncoder().encodeToString(rsaPublicKey.getEncoded()) + "-----END PUBLIC KEY-----";
        when(ltiDataService.getOwnPublicKey()).thenReturn(pem);
    }

    @Test
    void testJwkReturnsToolPublicKeyAsJwks() throws GeneralSecurityException {
        String json = jwkController.jwk(httpServletRequest, model);

        assertTrue(json.contains("\"kty\":\"RSA\""), json);
        assertTrue(json.contains("\"use\":\"sig\""), json);
        assertTrue(json.contains("\"alg\":\"RS256\""), json);
        assertTrue(json.contains("\"kid\":\"OWNKEY\""), json);

        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(extractField(json, "\"n\":\"")));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(extractField(json, "\"e\":\"")));

        assertEquals(rsaPublicKey.getModulus(), modulus);
        assertEquals(rsaPublicKey.getPublicExponent(), exponent);
    }

    @Test
    void testJwkInvalidPublicKeyThrowsGeneralSecurityException() {
        when(ltiDataService.getOwnPublicKey()).thenReturn("not-a-valid-key");

        assertThrows(GeneralSecurityException.class, () -> jwkController.jwk(httpServletRequest, model));
    }

    private String extractField(String json, String prefix) {
        int start = json.indexOf(prefix) + prefix.length();
        int end = json.indexOf('"', start);

        return json.substring(start, end);
    }

}
