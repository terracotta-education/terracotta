package edu.iu.terracotta.connectors.generic.dao.entity.lti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;

/**
 * Tests the hand-written, validating constructor of {@link LtiUserEntity}:
 * {@code LtiUserEntity(String userKey, Date loginAt, PlatformDeployment platformDeployment1)},
 * plus {@link LtiUserEntity#isTestStudent()}.
 */
public class LtiUserEntityTest {

    private PlatformDeployment buildPlatformDeployment() {
        return PlatformDeployment.builder()
            .iss("iss")
            .clientId("client-id")
            .oidcEndpoint("oidc-endpoint")
            .build();
    }

    @Test
    public void testConstructorSetsAllFieldsCorrectly() {
        PlatformDeployment platformDeployment = buildPlatformDeployment();
        Date loginAt = new Date();

        LtiUserEntity entity = new LtiUserEntity("user-key", loginAt, platformDeployment);

        assertEquals("user-key", entity.getUserKey());
        assertEquals(loginAt.getTime(), entity.getLoginAt().getTime());
        assertEquals(platformDeployment, entity.getPlatformDeployment());
    }

    @Test
    public void testConstructorBlankUserKeyThrowsAssertionError() {
        PlatformDeployment platformDeployment = buildPlatformDeployment();

        assertThrows(AssertionError.class, () -> new LtiUserEntity("", new Date(), platformDeployment));
        assertThrows(AssertionError.class, () -> new LtiUserEntity("   ", new Date(), platformDeployment));
        assertThrows(AssertionError.class, () -> new LtiUserEntity(null, new Date(), platformDeployment));
    }

    @Test
    public void testConstructorNullLoginAtDefaultsToNow() {
        PlatformDeployment platformDeployment = buildPlatformDeployment();

        long before = System.currentTimeMillis();
        LtiUserEntity entity = new LtiUserEntity("user-key", null, platformDeployment);
        long after = System.currentTimeMillis();

        assertNotNull(entity.getLoginAt());
        assertTrue(entity.getLoginAt().getTime() >= before - 1000, "loginAt should be at or after just-before construction time");
        assertTrue(entity.getLoginAt().getTime() <= after + 1000, "loginAt should be at or before just-after construction time");
    }

    @Test
    public void testConstructorNullPlatformDeploymentLeavesFieldNull() {
        LtiUserEntity entity = new LtiUserEntity("user-key", new Date(), null);

        assertNull(entity.getPlatformDeployment());
    }

    @Test
    public void testIsTestStudentExactMatch() {
        LtiUserEntity entity = new LtiUserEntity("user-key", new Date(), null);
        entity.setDisplayName(LtiUserEntity.TEST_STUDENT_DISPLAY_NAME);

        assertTrue(entity.isTestStudent());
    }

    @Test
    public void testIsTestStudentDifferentCaseMatchesCaseInsensitively() {
        LtiUserEntity entity = new LtiUserEntity("user-key", new Date(), null);
        entity.setDisplayName(LtiUserEntity.TEST_STUDENT_DISPLAY_NAME.toUpperCase());

        assertTrue(entity.isTestStudent());
    }

    @Test
    public void testIsTestStudentNonMatchingDisplayNameReturnsFalse() {
        LtiUserEntity entity = new LtiUserEntity("user-key", new Date(), null);
        entity.setDisplayName("Some Real Student");

        assertFalse(entity.isTestStudent());
    }

    @Test
    public void testIsTestStudentNullDisplayNameReturnsFalse() {
        LtiUserEntity entity = new LtiUserEntity("user-key", new Date(), null);
        entity.setDisplayName(null);

        assertFalse(entity.isTestStudent());
    }

}
