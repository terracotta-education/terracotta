package edu.iu.terracotta.security.pii;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class PiiCipherTest {

    private static final String KEY = key(1);
    private static final String OTHER_KEY = key(2);
    private static final String HASH_KEY = key(3);

    private final PiiCipher piiCipher = new PiiCipher("1", KEY, "", HASH_KEY);

    @Test
    void encryptsAndDecryptsAValue() {
        String stored = piiCipher.encrypt("Ada Lovelace");

        assertTrue(stored.startsWith("pii1:1:"));
        assertFalse(stored.contains("Ada"));
        assertEquals("Ada Lovelace", piiCipher.decrypt(stored));
    }

    @Test
    void encryptsTheSameValueDifferentlyEachTime() {
        assertNotEquals(piiCipher.encrypt("ada@example.com"), piiCipher.encrypt("ada@example.com"));
    }

    @Test
    void handlesNonAsciiValues() {
        assertEquals("José Ñúñez 李", piiCipher.decrypt(piiCipher.encrypt("José Ñúñez 李")));
    }

    @Test
    void leavesNullAlone() {
        assertNull(piiCipher.encrypt(null));
        assertNull(piiCipher.decrypt(null));
        assertNull(piiCipher.hashEmail(null));
    }

    @Test
    void readsLegacyPlainTextAsIs() {
        assertEquals("ada@example.com", piiCipher.decrypt("ada@example.com"));
    }

    @Test
    void doesNotEncryptAValueTwice() {
        String stored = piiCipher.encrypt("ada@example.com");

        assertEquals(stored, piiCipher.encrypt(stored));
    }

    @Test
    void decryptsValuesEncryptedWithARetiredKey() {
        String storedWithOldKey = new PiiCipher("1", KEY, "", HASH_KEY).encrypt("ada@example.com");
        PiiCipher rotated = new PiiCipher("2", OTHER_KEY, "1:" + KEY, HASH_KEY);

        assertEquals("ada@example.com", rotated.decrypt(storedWithOldKey));
        assertTrue(rotated.encrypt("ada@example.com").startsWith("pii1:2:"));
    }

    @Test
    void refusesAValueForAKeyThatIsNotConfigured() {
        String storedWithOtherKey = new PiiCipher("2", OTHER_KEY, "", HASH_KEY).encrypt("ada@example.com");

        assertThrows(IllegalStateException.class, () -> piiCipher.decrypt(storedWithOtherKey));
    }

    @Test
    void refusesATamperedValue() {
        String stored = piiCipher.encrypt("ada@example.com");
        // change one character inside the ciphertext (past the nonce), keeping it valid base64
        int index = stored.lastIndexOf(':') + 1 + 24;
        String tampered = stored.substring(0, index) + (stored.charAt(index) == 'A' ? 'B' : 'A') + stored.substring(index + 1);

        assertThrows(IllegalStateException.class, () -> piiCipher.decrypt(tampered));
    }

    @Test
    void refusesAValueWhoseKeyIdWasSwapped() {
        PiiCipher bothKeys = new PiiCipher("1", KEY, "2:" + OTHER_KEY, HASH_KEY);
        String swapped = bothKeys.encrypt("ada@example.com").replaceFirst("^pii1:1:", "pii1:2:");

        assertThrows(IllegalStateException.class, () -> bothKeys.decrypt(swapped));
    }

    @Test
    void hashesAnEmailIgnoringCaseAndSurroundingSpace() {
        String hash = piiCipher.hashEmail("ada@example.com");

        assertEquals(64, hash.length());
        assertEquals(hash, piiCipher.hashEmail("  Ada@Example.COM "));
        assertNotEquals(hash, piiCipher.hashEmail("grace@example.com"));
    }

    @Test
    void hashesWithItsOwnKey() {
        assertNotEquals(piiCipher.hashEmail("ada@example.com"), new PiiCipher("1", KEY, "", OTHER_KEY).hashEmail("ada@example.com"));
    }

    @Test
    void doesNotHashABlankEmail() {
        assertNull(piiCipher.hashEmail("  "));
    }

    @Test
    void requiresTheKeys() {
        assertThrows(IllegalStateException.class, () -> new PiiCipher("1", "", "", HASH_KEY));
        assertThrows(IllegalStateException.class, () -> new PiiCipher("1", KEY, "", ""));
    }

    @Test
    void requiresThirtyTwoByteBase64Keys() {
        assertThrows(IllegalStateException.class, () -> new PiiCipher("1", Base64.getEncoder().encodeToString(new byte[16]), "", HASH_KEY));
        assertThrows(IllegalStateException.class, () -> new PiiCipher("1", "not base64!", "", HASH_KEY));
    }

    @Test
    void requiresAUsableKeyId() {
        assertThrows(IllegalStateException.class, () -> new PiiCipher("", KEY, "", HASH_KEY));
        assertThrows(IllegalStateException.class, () -> new PiiCipher("a:b", KEY, "", HASH_KEY));
    }

    @Test
    void requiresRetiredKeysAsKeyIdAndKeyPairs() {
        assertThrows(IllegalStateException.class, () -> new PiiCipher("2", OTHER_KEY, KEY, HASH_KEY));
    }

    private static String key(int fill) {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) fill);

        return Base64.getEncoder().encodeToString(key);
    }

}
