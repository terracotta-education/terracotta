package edu.iu.terracotta.security.pii;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import edu.iu.Terracotta;
import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiOAuthSettings;
import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiTokenEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatch;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchEmailProjection;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.repository.api.ApiOAuthSettingsRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.api.ApiTokenRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.runner.PiiEncryptionBackfillRunner;

/**
 * Runs the real Hibernate mapping against H2 to check what lands in the tables: personal values
 * are stored encrypted, read back decrypted, and email hashes follow their emails. Also checks the
 * startup backfill converts rows saved before encryption.
 */
@SpringBootTest(
    classes = Terracotta.class,
    properties = {
        "aws.enabled=false",
        // isolated in-memory H2 instance, overriding any ambient/profile-based datasource
        "spring.datasource.url=jdbc:h2:mem:pii-encryption-it;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=sa",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // the tests run the backfill themselves; at startup it would race them
        "app.pii.encryption.backfill.enabled=false"
    }
)
@ActiveProfiles("test")
class PiiEncryptionRealHibernateTest {

    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private LmsUserBatchRepository lmsUserBatchRepository;
    @Autowired private ApiTokenRepository apiTokenRepository;
    @Autowired private ApiOAuthSettingsRepository apiOAuthSettingsRepository;
    @Autowired private PlatformDeploymentRepository platformDeploymentRepository;
    @Autowired private PiiCipher piiCipher;
    @Autowired private PiiEncryptionBackfillRunner backfillRunner;
    @Autowired private JdbcTemplate jdbcTemplate;

    private PlatformDeployment platformDeployment;

    @BeforeEach
    void seed() {
        jdbcTemplate.update("DELETE FROM api_token");
        jdbcTemplate.update("DELETE FROM api_oauth_settings");
        jdbcTemplate.update("DELETE FROM lti_user");
        jdbcTemplate.update("DELETE FROM lms_user_batch");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.update("DELETE FROM terr_submission_comment");
        jdbcTemplate.update("DELETE FROM terr_messaging_message_log");
        // platforms earlier tests created (and encrypted secrets on) would otherwise count here too
        jdbcTemplate.update("DELETE FROM iss_configuration");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        platformDeployment = platformDeploymentRepository.saveAndFlush(
            PlatformDeployment.builder()
                .iss("https://issuer.example.org")
                .clientId(UUID.randomUUID().toString())
                .oidcEndpoint("https://issuer.example.org/oidc")
                .enableAutomaticDeployments(false)
                .lmsConnector(LmsConnector.CANVAS)
                .build()
        );
    }

    @Test
    void storesTheEmailAndNameEncrypted() {
        LtiUserEntity saved = save("ada@example.com", "Ada Lovelace");

        Map<String, Object> row = rawRow(saved.getUserId());

        assertTrue(PiiCipher.isEncrypted((String) row.get("email")));
        assertFalse(((String) row.get("email")).contains("ada"));
        assertTrue(PiiCipher.isEncrypted((String) row.get("displayname")));
        assertFalse(((String) row.get("displayname")).contains("Ada"));
        assertEquals(piiCipher.hashEmail("ada@example.com"), row.get("email_hash"));
    }

    @Test
    void readsTheEmailAndNameBackDecrypted() {
        long userId = save("ada@example.com", "Ada Lovelace").getUserId();

        LtiUserEntity loaded = ltiUserRepository.findById(userId).orElseThrow();

        assertEquals("ada@example.com", loaded.getEmail());
        assertEquals("Ada Lovelace", loaded.getDisplayName());
    }

    @Test
    void findsUsersByEmailHashIgnoringCase() {
        long userId = save("ada@example.com", "Ada Lovelace").getUserId();
        save("grace@example.com", "Grace Hopper");

        List<LtiUserEntity> found = ltiUserRepository.findAllByEmailHashInAndPlatformDeployment_KeyId(
            List.of(piiCipher.hashEmail("ADA@example.com")),
            platformDeployment.getKeyId()
        );

        assertEquals(List.of(userId), found.stream().map(LtiUserEntity::getUserId).toList());
    }

    @Test
    void updatesTheHashWhenTheEmailChanges() {
        long userId = save("ada@example.com", "Ada Lovelace").getUserId();

        LtiUserEntity loaded = ltiUserRepository.findById(userId).orElseThrow();
        loaded.setEmail("countess@example.com");
        ltiUserRepository.saveAndFlush(loaded);

        assertEquals(piiCipher.hashEmail("countess@example.com"), rawRow(userId).get("email_hash"));
    }

    @Test
    void clearsTheHashWhenTheEmailIsRemoved() {
        long userId = save("ada@example.com", "Ada Lovelace").getUserId();

        LtiUserEntity loaded = ltiUserRepository.findById(userId).orElseThrow();
        loaded.setEmail(null);
        ltiUserRepository.saveAndFlush(loaded);

        assertNull(rawRow(userId).get("email_hash"));
    }

    @Test
    void backfillEncryptsRowsSavedBeforeEncryption() {
        long legacy = insertRaw("ada@example.com", "Ada Lovelace", null);
        // encrypted already, but saved before its hash was filled in
        long missingHash = insertRaw(piiCipher.encrypt("grace@example.com"), piiCipher.encrypt("Grace Hopper"), null);
        long noPii = insertRaw(null, null, null);

        assertEquals(2, backfillRunner.backfill());

        Map<String, Object> legacyRow = rawRow(legacy);
        assertTrue(PiiCipher.isEncrypted((String) legacyRow.get("email")));
        assertTrue(PiiCipher.isEncrypted((String) legacyRow.get("displayname")));
        assertEquals(piiCipher.hashEmail("ada@example.com"), legacyRow.get("email_hash"));
        assertEquals("Ada Lovelace", ltiUserRepository.findById(legacy).orElseThrow().getDisplayName());

        assertEquals(piiCipher.hashEmail("grace@example.com"), rawRow(missingHash).get("email_hash"));
        assertEquals("Grace Hopper", ltiUserRepository.findById(missingHash).orElseThrow().getDisplayName());

        assertNull(rawRow(noPii).get("email"));
    }

    @Test
    void backfillIsANoOpOnceEverythingIsConverted() {
        insertRaw("ada@example.com", "Ada Lovelace", null);
        backfillRunner.backfill();

        assertEquals(0, backfillRunner.backfill());
    }

    @Test
    void readsALegacyRowBeforeTheBackfillReachesIt() {
        long legacy = insertRaw("ada@example.com", "Ada Lovelace", null);

        LtiUserEntity loaded = ltiUserRepository.findById(legacy).orElseThrow();

        assertEquals("ada@example.com", loaded.getEmail());
        assertEquals("Ada Lovelace", loaded.getDisplayName());
    }

    @Test
    void storesAStagedRosterUserEncryptedAndFindsThemByEmailHash() {
        UUID batchId = UUID.randomUUID();
        LmsUserBatch staged = lmsUserBatchRepository.saveAndFlush(
            LmsUserBatch.builder().batchId(batchId).userKey("key-1").lmsUserId("lms-1").email("Ada@Example.com").name("Ada Lovelace").build()
        );

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT email, email_hash, name FROM lms_user_batch WHERE id = ?", staged.getId());
        assertTrue(PiiCipher.isEncrypted((String) row.get("email")));
        assertTrue(PiiCipher.isEncrypted((String) row.get("name")));
        assertEquals(piiCipher.hashEmail("ada@example.com"), row.get("email_hash"));

        List<LmsUserBatchEmailProjection> found = lmsUserBatchRepository.findBatchProjectionsByBatchIdAndEmailHashIn(
            batchId,
            List.of(piiCipher.hashEmail("ada@example.com")),
            PageRequest.of(0, 10)
        );

        // the query's projection is decrypted too, not just whole entities
        assertEquals(1, found.size());
        assertEquals("Ada@Example.com", found.get(0).getEmail());
        assertEquals("lms-1", found.get(0).getLmsUserId());
    }

    @Test
    void backfillEncryptsTheOtherTablesToo() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.update(
            "INSERT INTO terr_submission_comment (submission_comment_id, submission_submission_id, creator, comment, uuid, entity_version) VALUES (1, 1, ?, 'Nice work', ?, 0)",
            "Ada Lovelace", UUID.randomUUID()
        );
        jdbcTemplate.update(
            "INSERT INTO terr_messaging_message_log (id, lti_user_user_id, message_id, body, uuid, entity_version) VALUES (1, 1, 1, ?, ?, 0)",
            "<p>Hi Ada, here are your results.</p>", UUID.randomUUID()
        );
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        assertEquals(2, backfillRunner.backfill());

        String creator = jdbcTemplate.queryForObject("SELECT creator FROM terr_submission_comment WHERE submission_comment_id = 1", String.class);
        String body = jdbcTemplate.queryForObject("SELECT body FROM terr_messaging_message_log WHERE id = 1", String.class);

        assertEquals("Ada Lovelace", piiCipher.decrypt(creator));
        assertTrue(PiiCipher.isEncrypted(creator));
        assertEquals("<p>Hi Ada, here are your results.</p>", piiCipher.decrypt(body));
        assertTrue(PiiCipher.isEncrypted(body));
        // the comment itself isn't personal data and stays as written
        assertEquals("Nice work", jdbcTemplate.queryForObject("SELECT comment FROM terr_submission_comment WHERE submission_comment_id = 1", String.class));
    }

    @Test
    void storesTheLmsOAuthTokensEncrypted() {
        LtiUserEntity user = save("ada@example.com", "Ada Lovelace");
        ApiTokenEntity saved = apiTokenRepository.saveAndFlush(
            ApiTokenEntity.builder()
                .user(user)
                .lmsConnector(LmsConnector.CANVAS)
                .accessToken("canvas-access-token")
                .refreshToken("canvas-refresh-token")
                .expiresAt(new Timestamp(System.currentTimeMillis()))
                .lmsUserId("lms-1")
                .lmsUserName("Ada Lovelace")
                .build()
        );

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT access_token, refresh_token, lms_user_name FROM api_token WHERE token_id = ?", saved.getTokenId());
        assertTrue(PiiCipher.isEncrypted((String) row.get("access_token")));
        assertTrue(PiiCipher.isEncrypted((String) row.get("refresh_token")));
        assertTrue(PiiCipher.isEncrypted((String) row.get("lms_user_name")));

        ApiTokenEntity loaded = apiTokenRepository.findById(saved.getTokenId()).orElseThrow();
        assertEquals("canvas-access-token", loaded.getAccessToken());
        assertEquals("canvas-refresh-token", loaded.getRefreshToken());
        assertEquals("Ada Lovelace", loaded.getLmsUserName());
    }

    @Test
    void backfillEncryptsTheLmsOAuthTokens() {
        long userId = save("ada@example.com", "Ada Lovelace").getUserId();
        jdbcTemplate.update(
            "INSERT INTO api_token (user_id, lms_connector, access_token, refresh_token, expires_at, lms_user_id, lms_user_name) VALUES (?, 'CANVAS', 'legacy-access', 'legacy-refresh', CURRENT_TIMESTAMP, 'lms-1', 'Ada Lovelace')",
            userId
        );

        assertEquals(1, backfillRunner.backfill());

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT access_token, refresh_token FROM api_token");
        assertEquals("legacy-access", piiCipher.decrypt((String) row.get("access_token")));
        assertTrue(PiiCipher.isEncrypted((String) row.get("access_token")));
        assertEquals("legacy-refresh", piiCipher.decrypt((String) row.get("refresh_token")));
        assertTrue(PiiCipher.isEncrypted((String) row.get("refresh_token")));
    }

    @Test
    void storesThePlatformAndDeveloperKeySecretsEncrypted() {
        platformDeployment.setApiToken("platform-api-token");
        platformDeployment.setCaliperApiKey("caliper-api-key");
        platformDeploymentRepository.saveAndFlush(platformDeployment);
        ApiOAuthSettings settings = apiOAuthSettingsRepository.saveAndFlush(
            ApiOAuthSettings.builder()
                .clientId("developer-key-id")
                .clientSecret("developer-key-secret")
                .oauth2AuthUrl("https://lms.example.org/login/oauth2/auth")
                .oauth2TokenUrl("https://lms.example.org/login/oauth2/token")
                .platformDeployment(platformDeployment)
                .build()
        );

        Map<String, Object> platformRow = jdbcTemplate.queryForMap("SELECT api_token, caliper_api_key FROM iss_configuration WHERE key_id = ?", platformDeployment.getKeyId());
        assertTrue(PiiCipher.isEncrypted((String) platformRow.get("api_token")));
        assertTrue(PiiCipher.isEncrypted((String) platformRow.get("caliper_api_key")));
        assertTrue(PiiCipher.isEncrypted(jdbcTemplate.queryForObject("SELECT client_secret FROM api_oauth_settings WHERE settings_id = ?", String.class, settings.getSettingsId())));

        PlatformDeployment loaded = platformDeploymentRepository.findById(platformDeployment.getKeyId()).orElseThrow();
        assertEquals("platform-api-token", loaded.getApiToken());
        assertEquals("caliper-api-key", loaded.getCaliperApiKey());
        assertEquals("developer-key-secret", apiOAuthSettingsRepository.findById(settings.getSettingsId()).orElseThrow().getClientSecret());
    }

    @Test
    void backfillEncryptsSecretsAddedStraightToTheDatabase() {
        // how an admin typically sets up a platform: rows written with SQL, secrets in plain text
        jdbcTemplate.update("UPDATE iss_configuration SET api_token = 'legacy-api-token', caliper_api_key = 'legacy-caliper-key' WHERE key_id = ?", platformDeployment.getKeyId());
        jdbcTemplate.update(
            "INSERT INTO api_oauth_settings (client_id, client_secret, oauth2_auth_url, oauth2_token_url, key_id) VALUES ('id', 'legacy-secret', 'https://a', 'https://t', ?)",
            platformDeployment.getKeyId()
        );

        assertEquals(2, backfillRunner.backfill());

        Map<String, Object> platformRow = jdbcTemplate.queryForMap("SELECT api_token, caliper_api_key FROM iss_configuration WHERE key_id = ?", platformDeployment.getKeyId());
        assertEquals("legacy-api-token", piiCipher.decrypt((String) platformRow.get("api_token")));
        assertTrue(PiiCipher.isEncrypted((String) platformRow.get("api_token")));
        assertEquals("legacy-caliper-key", piiCipher.decrypt((String) platformRow.get("caliper_api_key")));
        String secret = jdbcTemplate.queryForObject("SELECT client_secret FROM api_oauth_settings", String.class);
        assertEquals("legacy-secret", piiCipher.decrypt(secret));
        assertTrue(PiiCipher.isEncrypted(secret));
    }

    @Test
    void decryptAllTurnsEverythingBackIntoPlainTextForARollback() {
        long userId = save("ada@example.com", "Ada Lovelace").getUserId();
        apiTokenRepository.saveAndFlush(
            ApiTokenEntity.builder()
                .user(ltiUserRepository.findById(userId).orElseThrow())
                .lmsConnector(LmsConnector.CANVAS)
                .accessToken("canvas-access-token")
                .refreshToken("canvas-refresh-token")
                .expiresAt(new Timestamp(System.currentTimeMillis()))
                .lmsUserId("lms-1")
                .lmsUserName("Ada Lovelace")
                .build()
        );
        lmsUserBatchRepository.saveAndFlush(LmsUserBatch.builder().batchId(UUID.randomUUID()).email("ada@example.com").name("Ada").build());

        assertEquals(2, backfillRunner.decryptAll());

        Map<String, Object> user = rawRow(userId);
        assertEquals("ada@example.com", user.get("email"));
        assertEquals("Ada Lovelace", user.get("displayname"));
        Map<String, Object> token = jdbcTemplate.queryForMap("SELECT access_token, refresh_token, lms_user_name FROM api_token");
        assertEquals("canvas-access-token", token.get("access_token"));
        assertEquals("canvas-refresh-token", token.get("refresh_token"));
        assertEquals("Ada Lovelace", token.get("lms_user_name"));
        // the staged roster is dropped rather than decrypted
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM lms_user_batch", Integer.class));
        // a second run finds nothing left to decrypt
        assertEquals(0, backfillRunner.decryptAll());
    }

    @Test
    void encryptionCanBeTurnedBackOnAfterADecryptAll() {
        long userId = save("ada@example.com", "Ada Lovelace").getUserId();
        backfillRunner.decryptAll();
        // the older build changes the email while it's in plain text, leaving a stale hash behind
        jdbcTemplate.update("UPDATE lti_user SET email = 'countess@example.com' WHERE user_id = ?", userId);

        assertEquals(1, backfillRunner.backfill());

        Map<String, Object> row = rawRow(userId);
        assertTrue(PiiCipher.isEncrypted((String) row.get("email")));
        assertTrue(PiiCipher.isEncrypted((String) row.get("displayname")));
        assertEquals(piiCipher.hashEmail("countess@example.com"), row.get("email_hash"));
    }

    private LtiUserEntity save(String email, String displayName) {
        LtiUserEntity ltiUserEntity = new LtiUserEntity(UUID.randomUUID().toString(), null, platformDeployment);
        ltiUserEntity.setEmail(email);
        ltiUserEntity.setDisplayName(displayName);

        return ltiUserRepository.saveAndFlush(ltiUserEntity);
    }

    // a row written straight to the table, as it was before these columns were encrypted
    private long insertRaw(String email, String displayName, String emailHash) {
        jdbcTemplate.update(
            "INSERT INTO lti_user (user_key, key_id, email, email_hash, displayname, uuid, entity_version) VALUES (?, ?, ?, ?, ?, ?, 0)",
            UUID.randomUUID().toString(),
            platformDeployment.getKeyId(),
            email,
            emailHash,
            displayName,
            UUID.randomUUID()
        );

        return jdbcTemplate.queryForObject("SELECT MAX(user_id) FROM lti_user", Long.class);
    }

    private Map<String, Object> rawRow(long userId) {
        return jdbcTemplate.queryForMap("SELECT email, email_hash, displayname FROM lti_user WHERE user_id = ?", userId);
    }

}
