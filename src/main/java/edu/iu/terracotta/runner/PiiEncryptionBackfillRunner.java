package edu.iu.terracotta.runner;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import edu.iu.terracotta.security.pii.PiiCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Encrypts personal values (and LMS OAuth tokens) stored before their columns were encrypted, and fills in missing
 * email hashes. It runs in the background after startup, a batch at a time, and only touches rows
 * that still need it, so it's a no-op once everything is converted.
 *
 * Until a row is converted it still works: the converter reads plain text as-is. A user whose
 * email isn't hashed yet can't be matched by email, though, so messaging may miss them until the
 * backfill reaches their row.
 *
 * With pii.encryption.decrypt-all on, it does the reverse instead: every encrypted value is
 * decrypted back to plain text, so a build without encryption can read the database again. That
 * is only for rolling back. The email hashes are left in place: the older build ignores them, and
 * they're recomputed if encryption is turned back on.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings({"PMD.GuardLogStatement"})
public class PiiEncryptionBackfillRunner implements ApplicationListener<ApplicationReadyEvent> {

    private static final String EMAIL = "email";
    private static final String WHERE = " WHERE ";

    /**
     * The encrypted columns, by table. A secret an admin later adds straight to the database (e.g.
     * a new iss_configuration row) is read as plain text until the next startup encrypts it. lms_user_batch isn't here: it only holds a roster while a
     * sync runs, and the migration dropped what was left of it.
     */
    static final List<Target> TARGETS = List.of(
        new Target("lti_user", "user_id", List.of(EMAIL, "displayname"), EMAIL, "email_hash"),
        new Target("api_token", "token_id", List.of("lms_user_name", "access_token", "refresh_token"), null, null),
        new Target("terr_messaging_email_reply_to", "id", List.of(EMAIL), null, null),
        new Target("terr_submission_comment", "submission_comment_id", List.of("creator"), null, null),
        new Target("terr_question_submission_comment", "question_submission_comment_id", List.of("creator"), null, null),
        new Target("terr_messaging_piped_text_item_value", "id", List.of("value"), null, null),
        new Target("terr_messaging_message_log", "id", List.of("body"), null, null),
        new Target("api_oauth_settings", "settings_id", List.of("client_secret"), null, null),
        new Target("iss_configuration", "key_id", List.of("api_token", "caliper_api_key"), null, null)
    );

    private final JdbcTemplate jdbcTemplate;
    private final PiiCipher piiCipher;

    @Value("${app.pii.encryption.backfill.enabled:true}")
    private boolean enabled;

    @Value("${app.pii.encryption.backfill.batch-size:500}")
    private int batchSize;

    @Value("${pii.encryption.decrypt-all:false}")
    private boolean decryptAll;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (decryptAll) {
            log.warn("pii.encryption.decrypt-all is on: decrypting all stored personal data and secrets back to plain text, and saving new values unencrypted. Only use this to roll back to a build without encryption.");
            start(this::decryptAll, "pii-decrypt-all");
            return;
        }

        if (enabled) {
            start(this::backfill, "pii-encryption-backfill");
        }
    }

    private static void start(Runnable work, String name) {
        Thread thread = new Thread(work, name);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Decrypts every encrypted value back to plain text, for rolling back to a build without
     * encryption.
     *
     * @return the number of rows decrypted, across all tables
     */
    public int decryptAll() {
        int decrypted = 0;
        boolean complete = true;

        // a roster staged mid-sync; the older build's next sync stages it again
        int staged = jdbcTemplate.update("DELETE FROM lms_user_batch");

        if (staged > 0) {
            log.info("Deleted [{}] staged roster rows from table [lms_user_batch]; the next roster sync stages them again.", staged);
        }

        for (Target target : TARGETS) {
            try {
                decrypted += run(target, true);
            } catch (RuntimeException e) {
                complete = false;
                log.error("Decrypting table [{}] stopped; restart with decrypt-all still on to finish it before rolling back.", target.table(), e);
            }
        }

        if (complete) {
            log.warn("Decryption finished: [{}] rows were decrypted and no encrypted values remain. Stop Terracotta and deploy the earlier build; to keep encryption instead, turn pii.encryption.decrypt-all off and restart.", decrypted);
        }

        return decrypted;
    }

    /**
     * @return the number of rows converted, across all tables
     */
    public int backfill() {
        int converted = 0;

        for (Target target : TARGETS) {
            try {
                converted += run(target, false);
            } catch (RuntimeException e) {
                // the rest of the tables are still worth converting; this one resumes on the next startup
                log.error("Encrypting the sensitive values in table [{}] stopped; it resumes on the next startup.", target.table(), e);
            }
        }

        return converted;
    }

    private int run(Target target, boolean decrypting) {
        int converted = 0;
        int skipped = 0;
        long lastId = 0;

        while (true) {
            List<Map<String, String>> rows = new ArrayList<>();
            List<Long> ids = new ArrayList<>();

            jdbcTemplate.query(
                decrypting ? target.encryptedSelectSql() : target.selectSql(),
                rs -> {
                    ids.add(rs.getLong(target.idColumn()));
                    rows.add(values(rs, target));
                },
                lastId,
                batchSize
            );

            if (rows.isEmpty()) {
                break;
            }

            for (int i = 0; i < rows.size(); i++) {
                lastId = ids.get(i);

                if (convert(target, lastId, rows.get(i), decrypting)) {
                    converted++;
                } else {
                    skipped++;
                }
            }
        }

        if (converted > 0 || skipped > 0) {
            // a skipped row changed while it was being converted, and the app saved it in the new form
            log.info("{} the sensitive values of [{}] rows in table [{}] ([{}] changed meanwhile and were left as saved).", decrypting ? "Decrypted" : "Encrypted", converted, target.table(), skipped);
        }

        return converted;
    }

    private boolean convert(Target target, long id, Map<String, String> read, boolean decrypting) {
        List<String> sets = new ArrayList<>();
        List<Object> setArgs = new ArrayList<>();

        for (String column : target.columns()) {
            sets.add(column + " = ?");

            if (decrypting) {
                setArgs.add(piiCipher.decrypt(read.get(column)));
            } else {
                setArgs.add(PiiCipher.isEncrypted(read.get(column)) ? read.get(column) : piiCipher.encrypt(read.get(column)));
            }
        }

        if (!decrypting && target.hashColumn() != null) {
            sets.add(target.hashColumn() + " = ?");
            setArgs.add(piiCipher.hashEmail(piiCipher.decrypt(read.get(target.hashSource()))));
        }

        // only applies if the row hasn't changed since it was read, so a concurrent edit isn't lost
        List<String> unchanged = new ArrayList<>();
        List<Object> whereArgs = new ArrayList<>(List.of(id));

        for (String column : target.columns()) {
            if (read.get(column) == null) {
                unchanged.add(column + " IS NULL");
            } else {
                unchanged.add(column + " = ?");
                whereArgs.add(read.get(column));
            }
        }

        String sql = "UPDATE " + target.table() + " SET " + String.join(", ", sets)
            + WHERE + target.idColumn() + " = ? AND " + String.join(" AND ", unchanged);
        setArgs.addAll(whereArgs);

        return jdbcTemplate.update(sql, setArgs.toArray()) == 1;
    }

    private static Map<String, String> values(ResultSet rs, Target target) throws SQLException {
        Map<String, String> values = new LinkedHashMap<>();

        for (String column : target.columns()) {
            values.put(column, rs.getString(column));
        }

        return values;
    }

    /**
     * A table's encrypted columns, and optionally the hash column kept for one of them.
     */
    record Target(String table, String idColumn, List<String> columns, String hashSource, String hashColumn) {

        // rows with any value still encrypted
        String encryptedSelectSql() {
            String encrypted = columns.stream()
                .map(column -> column + " LIKE '" + PiiCipher.PREFIX + "%'")
                .collect(Collectors.joining(" OR "));

            return "SELECT " + idColumn + ", " + String.join(", ", columns)
                + " FROM " + table
                + WHERE + idColumn + " > ? AND (" + encrypted + ")"
                + " ORDER BY " + idColumn
                + " LIMIT ?";
        }

        // rows with a value not yet encrypted, or an email not yet hashed
        String selectSql() {
            String needsEncrypting = columns.stream()
                .map(column -> "(" + column + " IS NOT NULL AND " + column + " NOT LIKE '" + PiiCipher.PREFIX + "%')")
                .collect(Collectors.joining(" OR "));

            if (hashColumn != null) {
                needsEncrypting += " OR (" + hashSource + " IS NOT NULL AND " + hashColumn + " IS NULL)";
            }

            return "SELECT " + idColumn + ", " + String.join(", ", columns)
                + " FROM " + table
                + WHERE + idColumn + " > ? AND (" + needsEncrypting + ")"
                + " ORDER BY " + idColumn
                + " LIMIT ?";
        }

    }

}
