package edu.iu.terracotta.security.pii;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class PiiStringConverterTest {

    private final PiiCipher piiCipher = new PiiCipher("1", key(1), "", key(2));

    @Test
    void encryptsOnSaveAndDecryptsOnRead() {
        PiiStringConverter converter = new PiiStringConverter(piiCipher, false);

        String stored = converter.convertToDatabaseColumn("Ada Lovelace");

        assertTrue(PiiCipher.isEncrypted(stored));
        assertEquals("Ada Lovelace", converter.convertToEntityAttribute(stored));
    }

    @Test
    void savesPlainTextWhileDecryptingEverything() {
        PiiStringConverter converter = new PiiStringConverter(piiCipher, true);

        assertEquals("Ada Lovelace", converter.convertToDatabaseColumn("Ada Lovelace"));
        // still reads values that haven't been decrypted yet
        assertEquals("Ada Lovelace", converter.convertToEntityAttribute(piiCipher.encrypt("Ada Lovelace")));
    }

    @Test
    void leavesNullAlone() {
        assertNull(new PiiStringConverter(piiCipher, false).convertToDatabaseColumn(null));
        assertNull(new PiiStringConverter(piiCipher, true).convertToDatabaseColumn(null));
    }

    private static String key(int fill) {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) fill);

        return Base64.getEncoder().encodeToString(key);
    }

}
