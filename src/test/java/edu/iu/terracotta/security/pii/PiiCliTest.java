package edu.iu.terracotta.security.pii;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PiiCliTest {

    private static final Map<String, String> ENV = Map.of(
        "PII_ENCRYPTION_KEY", key(1),
        "PII_HASH_KEY", key(2)
    );

    private final PiiCipher piiCipher = new PiiCipher("1", key(1), "", key(2));

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @Test
    void encryptsEachLineOfInput() throws IOException {
        assertEquals(0, run(ENV, "encrypt", "first-secret\nsecond-secret\n"));

        List<String> lines = output();
        assertEquals(2, lines.size());
        assertEquals("first-secret", piiCipher.decrypt(lines.get(0)));
        assertEquals("second-secret", piiCipher.decrypt(lines.get(1)));
        assertTrue(PiiCipher.isEncrypted(lines.get(0)));
    }

    @Test
    void decryptsAStoredValue() throws IOException {
        assertEquals(0, run(ENV, "decrypt", piiCipher.encrypt("the-secret") + "\n"));

        assertEquals(List.of("the-secret"), output());
    }

    @Test
    void hashesAnEmailTheWayTheAppDoes() throws IOException {
        assertEquals(0, run(ENV, "hash-email", "Ada@Example.com\n"));

        assertEquals(List.of(piiCipher.hashEmail("ada@example.com")), output());
    }

    @Test
    void usesTheConfiguredKeyId() throws IOException {
        Map<String, String> env = new HashMap<>(ENV);
        env.put("PII_ENCRYPTION_KEY_ID", "7");

        assertEquals(0, run(env, "encrypt", "the-secret\n"));

        assertTrue(output().get(0).startsWith("pii1:7:"));
    }

    @Test
    void reportsAValueItCannotDecryptWithoutEchoingIt() throws IOException {
        String fromAnotherKey = new PiiCipher("1", key(9), "", key(2)).encrypt("the-secret");

        assertEquals(1, run(ENV, "decrypt", fromAnotherKey + "\n"));

        assertFalse(err.toString(StandardCharsets.UTF_8).contains(fromAnotherKey));
        assertFalse(err.toString(StandardCharsets.UTF_8).contains("the-secret"));
    }

    @Test
    void namesTheMissingKeyByItsEnvironmentVariable() throws IOException {
        assertEquals(2, run(Map.of("PII_HASH_KEY", key(2)), "encrypt", "the-secret\n"));

        assertTrue(err.toString(StandardCharsets.UTF_8).contains("PII_ENCRYPTION_KEY"));
        assertEquals(List.of(), output());
    }

    @Test
    void printsUsageForAnUnknownCommand() throws IOException {
        assertEquals(2, run(ENV, "shout", "the-secret\n"));

        assertTrue(err.toString(StandardCharsets.UTF_8).contains("Usage"));
    }

    @Test
    void printsUsageWithoutACommand() throws IOException {
        assertEquals(2, PiiCli.run(new String[0], ENV, null, reader(""), new PrintStream(out), new PrintStream(err)));

        assertTrue(err.toString(StandardCharsets.UTF_8).contains("Usage"));
    }

    private int run(Map<String, String> env, String command, String input) throws IOException {
        return PiiCli.run(new String[] {command}, env, null, reader(input), new PrintStream(out, true, StandardCharsets.UTF_8), new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    private List<String> output() {
        String text = out.toString(StandardCharsets.UTF_8);

        return text.isEmpty() ? List.of() : Arrays.asList(text.split("\\R"));
    }

    private static BufferedReader reader(String input) {
        return new BufferedReader(new StringReader(input));
    }

    private static String key(int fill) {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) fill);

        return Base64.getEncoder().encodeToString(key);
    }

}
