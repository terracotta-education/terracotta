package edu.iu.terracotta.connectors.generic.dao.entity.lti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;

/**
 * Tests the hand-written, validating constructor of {@link LtiResultEntity}:
 * {@code LtiResultEntity(LtiUserEntity, LtiLinkEntity, Date, Float, Float, String, String, String)}.
 *
 * <p>NOTE: as written, the constructor contains a bug where {@code comment}, {@code activityProgress},
 * and {@code gradingProgress} constructor arguments are all assigned into the single {@code this.comment}
 * field (lines 93-95 of the production class):
 * <pre>
 *     this.comment = comment;
 *     this.comment = activityProgress;
 *     this.comment = gradingProgress;
 * </pre>
 * This means {@code this.activityProgress} and {@code this.gradingProgress} are never actually set by the
 * constructor. The tests below assert the CORRECT/INTENDED behavior (each field holding its own distinct
 * value) and will therefore FAIL against the current buggy implementation until it is fixed.</p>
 */
public class LtiResultEntityTest {

    private LtiUserEntity buildUser() {
        return LtiUserEntity.builder()
            .userKey("user-key")
            .build();
    }

    private LtiLinkEntity buildLink() {
        return LtiLinkEntity.builder()
            .linkKey("link-key")
            .build();
    }

    @Test
    public void testConstructorSetsAllFieldsCorrectly() {
        LtiUserEntity user = buildUser();
        LtiLinkEntity link = buildLink();
        Date retrievedAt = new Date();

        LtiResultEntity entity = new LtiResultEntity(user, link, retrievedAt, 1.5f, 2.5f, "my-comment", "my-activity-progress", "my-grading-progress");

        assertEquals(user, entity.getUser());
        assertEquals(link, entity.getLink());
        assertEquals(retrievedAt.getTime(), entity.getTimestamp().getTime());
        assertEquals(1.5f, entity.getScoreGiven());
        assertEquals(2.5f, entity.getScoreMaximum());
        assertEquals("my-comment", entity.getComment());
        assertEquals("my-activity-progress", entity.getActivityProgress());
        assertEquals("my-grading-progress", entity.getGradingProgress());
    }

    @Test
    public void testConstructorNullUserThrowsAssertionError() {
        LtiLinkEntity link = buildLink();

        assertThrows(AssertionError.class, () -> new LtiResultEntity(null, link, new Date(), 1.0f, 2.0f, "comment", "activity", "grading"));
    }

    @Test
    public void testConstructorNullLinkThrowsAssertionError() {
        LtiUserEntity user = buildUser();

        assertThrows(AssertionError.class, () -> new LtiResultEntity(user, null, new Date(), 1.0f, 2.0f, "comment", "activity", "grading"));
    }

    @Test
    public void testConstructorNullRetrievedAtDefaultsToNow() {
        LtiUserEntity user = buildUser();
        LtiLinkEntity link = buildLink();

        long before = System.currentTimeMillis();
        LtiResultEntity entity = new LtiResultEntity(user, link, null, 1.0f, 2.0f, "comment", "activity", "grading");
        long after = System.currentTimeMillis();

        assertNotNull(entity.getTimestamp());
        assertTrue(entity.getTimestamp().getTime() >= before - 1000, "timestamp should be at or after just-before construction time");
        assertTrue(entity.getTimestamp().getTime() <= after + 1000, "timestamp should be at or before just-after construction time");
    }

    @Test
    public void testConstructorExplicitRetrievedAtIsPreserved() {
        LtiUserEntity user = buildUser();
        LtiLinkEntity link = buildLink();
        Date retrievedAt = new Date(System.currentTimeMillis() - 100_000L);

        LtiResultEntity entity = new LtiResultEntity(user, link, retrievedAt, 1.0f, 2.0f, "comment", "activity", "grading");

        assertEquals(retrievedAt.getTime(), entity.getTimestamp().getTime());
    }

}
