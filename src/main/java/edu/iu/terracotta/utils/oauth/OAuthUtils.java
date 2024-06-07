package edu.iu.terracotta.utils.oauth;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * OAuth handling utils
 */
public final class OAuthUtils {

    private OAuthUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static RSAPublicKey loadPublicKey(String key) throws GeneralSecurityException {
        String publicKeyContent = key.replace("\\n", "").replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
        KeyFactory kf = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent));

        return (RSAPublicKey) kf.generatePublic(keySpecX509);
    }

    public static PrivateKey loadPrivateKey(String privateKeyPem) throws GeneralSecurityException {
        // PKCS#8 format
        final String pemPrivateStart = "-----BEGIN PRIVATE KEY-----";
        final String pemPrivateEnd = "-----END PRIVATE KEY-----";

        if (privateKeyPem.contains(pemPrivateStart)) { // PKCS#8 format
            privateKeyPem = privateKeyPem.replace(pemPrivateStart, "").replace(pemPrivateEnd, "");
            privateKeyPem = privateKeyPem.replaceAll("\\s", "");
            byte[] pkcs8EncodedKey = Base64.getDecoder().decode(privateKeyPem);
            KeyFactory factory = KeyFactory.getInstance("RSA");

            return factory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8EncodedKey));
        }

        throw new GeneralSecurityException("Not supported format of a private key");
    }

}
