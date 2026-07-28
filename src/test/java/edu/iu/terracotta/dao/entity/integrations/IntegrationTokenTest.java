package edu.iu.terracotta.dao.entity.integrations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;

/**
 * {@link IntegrationToken} is a Lombok {@code @Builder} JPA entity extending {@code BaseUuidEntity}.
 * These tests exercise the hand-written {@code @Transient} methods {@code isExpired(long)},
 * {@code isAlreadyRedeemed()}, {@code getSecuredInfo()}, and the overridden {@code setSecuredInfo(SecuredInfo)}
 * (which serializes via the {@code tools.jackson} 3.x {@code JsonMapper}).
 */
public class IntegrationTokenTest {

    /* ***************** isExpired ***************** */

    @Test
    public void testIsExpiredTtlZeroAlwaysFalse() {
        IntegrationToken token = IntegrationToken.builder()
            .lastLaunchedAt(Timestamp.from(Instant.now().minusSeconds(1000)))
            .build();

        assertFalse(token.isExpired(0));
    }

    @Test
    public void testIsExpiredTtlNegativeAlwaysFalse() {
        IntegrationToken token = IntegrationToken.builder()
            .lastLaunchedAt(Timestamp.from(Instant.now().minusSeconds(1000)))
            .build();

        assertFalse(token.isExpired(-1));
    }

    @Test
    public void testIsExpiredTtlElapsedReturnsTrue() {
        IntegrationToken token = IntegrationToken.builder()
            .lastLaunchedAt(Timestamp.from(Instant.now().minusSeconds(10)))
            .build();

        assertTrue(token.isExpired(5));
    }

    @Test
    public void testIsExpiredTtlNotElapsedReturnsFalse() {
        IntegrationToken token = IntegrationToken.builder()
            .lastLaunchedAt(Timestamp.from(Instant.now()))
            .build();

        assertFalse(token.isExpired(5));
    }

    /* ***************** isAlreadyRedeemed ***************** */

    @Test
    public void testIsAlreadyRedeemedNullReturnsFalse() {
        IntegrationToken token = IntegrationToken.builder()
            .redeemedAt(null)
            .build();

        assertFalse(token.isAlreadyRedeemed());
    }

    @Test
    public void testIsAlreadyRedeemedNonNullReturnsTrue() {
        IntegrationToken token = IntegrationToken.builder()
            .redeemedAt(Timestamp.from(Instant.now()))
            .build();

        assertTrue(token.isAlreadyRedeemed());
    }

    /* ***************** getSecuredInfo / setSecuredInfo ***************** */

    @Test
    public void testGetSecuredInfoNullFieldReturnsEmpty() {
        IntegrationToken token = IntegrationToken.builder()
            .securedInfo(null)
            .build();

        assertEquals(Optional.empty(), token.getSecuredInfo());
    }

    @Test
    public void testGetSecuredInfoBlankFieldReturnsEmpty() {
        IntegrationToken token = IntegrationToken.builder()
            .securedInfo("")
            .build();

        assertEquals(Optional.empty(), token.getSecuredInfo());
    }

    @Test
    public void testSetAndGetSecuredInfoRoundTrips() {
        SecuredInfo securedInfo = SecuredInfo.builder()
            .userId("user-123")
            .lmsUserId("lms-user-456")
            .nonce("nonce-value")
            .roles(List.of("Learner"))
            .build();

        IntegrationToken token = IntegrationToken.builder()
            .build();
        token.setSecuredInfo(securedInfo);

        Optional<SecuredInfo> result = token.getSecuredInfo();

        assertTrue(result.isPresent());
        assertEquals("user-123", result.get().getUserId());
        assertEquals("lms-user-456", result.get().getLmsUserId());
        assertEquals("nonce-value", result.get().getNonce());
        assertEquals(List.of("Learner"), result.get().getRoles());
    }

    @Test
    public void testGetSecuredInfoMalformedJsonReturnsEmpty() {
        IntegrationToken token = IntegrationToken.builder()
            .securedInfo("not-json-{")
            .build();

        assertEquals(Optional.empty(), token.getSecuredInfo());
    }

}
