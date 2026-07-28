package edu.iu.terracotta.utils.oauth;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class OAuthUtils {

    // Key material comes from static config (per platform deployment) and never changes at
    // runtime, but loadPublicKey/loadPrivateKey are on the hot path for every authenticated
    // request and LTI launch - cache the parsed key objects instead of re-running
    // KeyFactory/Base64 decoding on every call.
    private static final Map<String, RSAPublicKey> PUBLIC_KEY_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, PrivateKey> PRIVATE_KEY_CACHE = new ConcurrentHashMap<>();

    public static RSAPublicKey loadPublicKey(String key) throws GeneralSecurityException {
        RSAPublicKey cached = PUBLIC_KEY_CACHE.get(key);

        if (cached != null) {
            return cached;
        }

        String publicKeyContent = key.replace("\\n", "").replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
        KeyFactory kf = KeyFactory.getInstance("RSA");

        try {
            X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent));
            RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(keySpecX509);
            PUBLIC_KEY_CACHE.put(key, publicKey);

            return publicKey;
        } catch (IllegalArgumentException e) {
            throw new GeneralSecurityException("Invalid public key encoding", e);
        }
    }

    public static PrivateKey loadPrivateKey(String privateKeyPem) throws GeneralSecurityException {
        PrivateKey cached = PRIVATE_KEY_CACHE.get(privateKeyPem);

        if (cached != null) {
            return cached;
        }

        // PKCS#8 format
        String pemPrivateStart = "-----BEGIN PRIVATE KEY-----";
        String pemPrivateEnd = "-----END PRIVATE KEY-----";

        if (privateKeyPem.contains(pemPrivateStart)) { // PKCS#8 format
            String strippedPem = privateKeyPem.replace(pemPrivateStart, "").replace(pemPrivateEnd, "").replaceAll("\\s", "");
            KeyFactory factory = KeyFactory.getInstance("RSA");

            try {
                byte[] pkcs8EncodedKey = Base64.getDecoder().decode(strippedPem);
                PrivateKey privateKey = factory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8EncodedKey));
                PRIVATE_KEY_CACHE.put(privateKeyPem, privateKey);

                return privateKey;
            } catch (IllegalArgumentException e) {
                throw new GeneralSecurityException("Invalid private key encoding", e);
            }
        }

        throw new GeneralSecurityException("Unsupported format of a private key");
    }

}
