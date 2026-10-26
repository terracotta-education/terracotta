package edu.iu.terracotta.security.pii;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Encrypts personally identifiable values (names, email addresses) before they're stored, and
 * computes a keyed hash of an email so it can still be matched in queries.
 *
 * Values are encrypted with AES-256-GCM and a random nonce, so the same input encrypts
 * differently every time. A stored value looks like {@code pii1:<keyId>:<base64(nonce + ciphertext)>};
 * carrying the key ID lets the active key be rotated while values encrypted with an older
 * (retired) key stay readable. A value without the prefix is legacy plain text from before the
 * column was encrypted, and is returned as-is until it's re-saved or backfilled.
 */
@Component
public class PiiCipher {

    public static final String PREFIX = "pii1:";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String HMAC = "HmacSHA256";
    private static final int KEY_BYTES = 32;
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();
    private final String activeKeyId;
    private final Map<String, SecretKey> keys = new HashMap<>();
    private final SecretKey hashKey;

    /**
     * @param activeKeyId the ID new values are encrypted under
     * @param activeKey base64 of a 32-byte AES key (e.g. {@code openssl rand -base64 32})
     * @param retiredKeys keys only used to decrypt, as comma-separated {@code keyId:base64Key} pairs
     * @param hashKey base64 of a 32-byte HMAC key, kept separate from the encryption keys
     */
    public PiiCipher(@Value("${pii.encryption.key-id:1}") String activeKeyId,
                     @Value("${pii.encryption.key:}") String activeKey,
                     @Value("${pii.encryption.retired-keys:}") String retiredKeys,
                     @Value("${pii.hash.key:}") String hashKey) {
        if (StringUtils.isBlank(activeKeyId) || activeKeyId.contains(":")) {
            throw new IllegalStateException("pii.encryption.key-id must be set and can't contain ':'");
        }

        this.activeKeyId = activeKeyId;
        keys.put(activeKeyId, aesKey("pii.encryption.key", activeKey));

        for (String retired : StringUtils.split(StringUtils.defaultString(retiredKeys), ',')) {
            String[] idAndKey = StringUtils.split(retired.trim(), ":", 2);

            if (idAndKey.length != 2) {
                throw new IllegalStateException("pii.encryption.retired-keys entries must be keyId:base64Key");
            }

            keys.putIfAbsent(idAndKey[0], aesKey("pii.encryption.retired-keys", idAndKey[1]));
        }

        this.hashKey = new SecretKeySpec(decodeKey("pii.hash.key", hashKey), HMAC);
    }

    public String encrypt(String plainText) {
        if (plainText == null || isEncrypted(plainText)) {
            return plainText;
        }

        try {
            byte[] nonce = new byte[NONCE_BYTES];
            random.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keys.get(activeKeyId), new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(aad(activeKeyId));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] payload = ByteBuffer.allocate(nonce.length + cipherText.length)
                .put(nonce)
                .put(cipherText)
                .array();

            return PREFIX + activeKeyId + ":" + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to encrypt a PII value", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null || !isEncrypted(stored)) {
            return stored;
        }

        String[] keyIdAndPayload = StringUtils.split(stored.substring(PREFIX.length()), ":", 2);
        SecretKey key = keyIdAndPayload.length == 2 ? keys.get(keyIdAndPayload[0]) : null;

        if (key == null) {
            throw new IllegalStateException("No PII encryption key is configured for this value's key ID");
        }

        try {
            byte[] payload = Base64.getDecoder().decode(keyIdAndPayload[1]);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, payload, 0, NONCE_BYTES));
            cipher.updateAAD(aad(keyIdAndPayload[0]));

            return new String(cipher.doFinal(payload, NONCE_BYTES, payload.length - NONCE_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Unable to decrypt a PII value", e);
        }
    }

    /**
     * A keyed hash of an email address, for matching it in queries without storing it in plain
     * text. Case and surrounding whitespace are ignored, like MySQL's default collation did when
     * the plain column was compared.
     */
    public String hashEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return null;
        }

        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(hashKey);

            return HexFormat.of().formatHex(mac.doFinal(email.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to hash a PII value", e);
        }
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    // binds each ciphertext to the key ID written next to it, so the ID can't be swapped
    private static byte[] aad(String keyId) {
        return (PREFIX + keyId).getBytes(StandardCharsets.UTF_8);
    }

    private static SecretKey aesKey(String property, String base64) {
        return new SecretKeySpec(decodeKey(property, base64), "AES");
    }

    private static byte[] decodeKey(String property, String base64) {
        if (StringUtils.isBlank(base64)) {
            throw new IllegalStateException(property + " must be set to a base64-encoded 32-byte key (e.g. openssl rand -base64 32)");
        }

        byte[] key;

        try {
            key = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(property + " isn't valid base64", e);
        }

        if (key.length != KEY_BYTES) {
            throw new IllegalStateException(property + " must decode to 32 bytes");
        }

        return key;
    }

}
