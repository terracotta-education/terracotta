package edu.iu.terracotta.utils.oauth;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OAuthUtilsTest {

    private KeyPair keyPair;

    @BeforeEach
    public void beforeEach() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();
    }

    // loadPublicKey

    @Test
    public void testLoadPublicKeySuccessWithProperPemFormat() throws Exception {
        String pem = "-----BEGIN PUBLIC KEY-----\n" + Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()) + "\n-----END PUBLIC KEY-----";

        RSAPublicKey result = OAuthUtils.loadPublicKey(pem);

        assertNotNull(result);
        assertArrayEquals(keyPair.getPublic().getEncoded(), result.getEncoded());
    }

    @Test
    public void testLoadPublicKeySuccessWithMixedEscapedAndRealNewlines() throws Exception {
        String base64Key = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        // no PEM markers here, but a mix of literal "\n" escape sequences and real newline/whitespace characters
        String mixedWhitespaceKey = "\\n" + base64Key.substring(0, base64Key.length() / 2) + "\n \t" + "\\n" + base64Key.substring(base64Key.length() / 2) + "\\n";

        RSAPublicKey result = OAuthUtils.loadPublicKey(mixedWhitespaceKey);

        assertNotNull(result);
        assertArrayEquals(keyPair.getPublic().getEncoded(), result.getEncoded());
    }

    @Test
    public void testLoadPublicKeyMalformedBase64ThrowsGeneralSecurityException() {
        String garbage = "-----BEGIN PUBLIC KEY-----\nnot*valid$base64!!\n-----END PUBLIC KEY-----";

        GeneralSecurityException exception = assertThrows(GeneralSecurityException.class, () -> OAuthUtils.loadPublicKey(garbage));

        assertEquals("Invalid public key encoding", exception.getMessage());
    }

    // loadPrivateKey

    @Test
    public void testLoadPrivateKeySuccessWithProperPemFormat() throws Exception {
        String pem = "-----BEGIN PRIVATE KEY-----\n" + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----";

        PrivateKey result = OAuthUtils.loadPrivateKey(pem);

        assertNotNull(result);
        assertArrayEquals(keyPair.getPrivate().getEncoded(), result.getEncoded());
    }

    @Test
    public void testLoadPrivateKeyMissingBeginMarkerThrowsGeneralSecurityException() {
        String noMarker = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        GeneralSecurityException exception = assertThrows(GeneralSecurityException.class, () -> OAuthUtils.loadPrivateKey(noMarker));

        assertEquals("Unsupported format of a private key", exception.getMessage());
    }

    @Test
    public void testLoadPrivateKeyMalformedBase64ThrowsGeneralSecurityException() {
        String garbage = "-----BEGIN PRIVATE KEY-----\nnot*valid$base64!!\n-----END PRIVATE KEY-----";

        GeneralSecurityException exception = assertThrows(GeneralSecurityException.class, () -> OAuthUtils.loadPrivateKey(garbage));

        assertEquals("Invalid private key encoding", exception.getMessage());
    }

}
