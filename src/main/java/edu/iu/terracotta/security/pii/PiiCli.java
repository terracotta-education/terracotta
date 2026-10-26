package edu.iu.terracotta.security.pii;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Encrypts a value the way Terracotta stores it, so a secret (or other sensitive value) can be
 * written straight to the database already encrypted, never sitting there in plain text. Uses the
 * same keys as the app, from the same environment variables.
 *
 * Run it from the Terracotta jar:
 *
 * <pre>
 * PII_ENCRYPTION_KEY=... PII_HASH_KEY=... \
 *     java -cp terracotta.jar -Dloader.main=edu.iu.terracotta.security.pii.PiiCli \
 *     org.springframework.boot.loader.launch.PropertiesLauncher encrypt
 * </pre>
 *
 * The value is read from standard input, not the command line, so it stays out of shell history
 * and the process list. At a terminal it's prompted for without echoing; piped in, each line is
 * one value and each output line is its result.
 */
public final class PiiCli {

    static final String USAGE = """
        Usage: PiiCli <encrypt | decrypt | hash-email>

          encrypt     print the value encrypted, ready to store in an encrypted column
          decrypt     print a stored (pii1:...) value decrypted
          hash-email  print an email's hash, for an email_hash column

        The value is read from standard input. Keys are read from the same environment
        variables as the app: PII_ENCRYPTION_KEY and PII_HASH_KEY (required), and
        PII_ENCRYPTION_KEY_ID and PII_ENCRYPTION_RETIRED_KEYS (optional).
        """;

    private PiiCli() {
    }

    // a command-line tool: printing to the terminal is its output, not logging. run() takes the
    // streams so tests can supply their own
    @SuppressWarnings("java:S106")
    public static void main(String[] args) throws IOException {
        System.exit(run(
            args,
            System.getenv(),
            System.console(),
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)),
            System.out,
            System.err
        ));
    }

    /**
     * @return the process exit code: 0 on success, 1 if a value couldn't be processed, 2 for bad usage or configuration
     */
    static int run(String[] args, Map<String, String> env, Console console, BufferedReader in, PrintStream out, PrintStream err) throws IOException {
        if (args.length != 1) {
            err.print(USAGE);
            return 2;
        }

        PiiCipher piiCipher;

        try {
            piiCipher = new PiiCipher(
                env.getOrDefault("PII_ENCRYPTION_KEY_ID", "1"),
                env.get("PII_ENCRYPTION_KEY"),
                env.get("PII_ENCRYPTION_RETIRED_KEYS"),
                env.get("PII_HASH_KEY")
            );
        } catch (IllegalStateException e) {
            // the message names the setting that's wrong, never a key's value
            err.println(e.getMessage().replace("pii.encryption.key-id", "PII_ENCRYPTION_KEY_ID")
                .replace("pii.encryption.retired-keys", "PII_ENCRYPTION_RETIRED_KEYS")
                .replace("pii.encryption.key", "PII_ENCRYPTION_KEY")
                .replace("pii.hash.key", "PII_HASH_KEY"));
            return 2;
        }

        UnaryOperator<String> operation = switch (args[0]) {
            case "encrypt" -> piiCipher::encrypt;
            case "decrypt" -> piiCipher::decrypt;
            case "hash-email" -> piiCipher::hashEmail;
            default -> null;
        };

        if (operation == null) {
            err.print(USAGE);
            return 2;
        }

        if (console != null && console.isTerminal()) {
            char[] value = console.readPassword("Value: ");

            if (value == null) {
                return 0;
            }

            return apply(operation, new String(value), out, err) ? 0 : 1;
        }

        int exitCode = 0;
        String line;

        while ((line = in.readLine()) != null) {
            if (!apply(operation, line, out, err)) {
                exitCode = 1;
            }
        }

        return exitCode;
    }

    private static boolean apply(UnaryOperator<String> operation, String value, PrintStream out, PrintStream err) {
        try {
            String result = operation.apply(value);
            // a blank email has no hash; print an empty line rather than "null"
            out.println(result != null ? result : "");
            return true;
        } catch (IllegalStateException e) {
            // e.g. a value that won't decrypt with these keys; keep the value itself out of the message
            err.println(e.getMessage());
            return false;
        }
    }

}
