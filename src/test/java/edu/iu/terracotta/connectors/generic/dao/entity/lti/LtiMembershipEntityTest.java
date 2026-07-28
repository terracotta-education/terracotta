package edu.iu.terracotta.connectors.generic.dao.entity.lti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests the hand-written, validating constructor of {@link LtiMembershipEntity}:
 * {@code LtiMembershipEntity(LtiContextEntity context, LtiUserEntity user, Integer role)}, which
 * requires non-null {@code context} and {@code user} (throwing {@link AssertionError} otherwise)
 * but does not validate {@code role}, which may be {@code null}.
 */
public class LtiMembershipEntityTest {

    private LtiContextEntity buildContext() {
        return LtiContextEntity.builder()
            .contextKey("context-key")
            .build();
    }

    private LtiUserEntity buildUser() {
        return LtiUserEntity.builder()
            .userKey("user-key")
            .build();
    }

    @Test
    public void testConstructorSetsAllFieldsCorrectly() {
        LtiContextEntity context = buildContext();
        LtiUserEntity user = buildUser();

        LtiMembershipEntity entity = new LtiMembershipEntity(context, user, 1);

        assertEquals(context, entity.getContext());
        assertEquals(user, entity.getUser());
        assertEquals(1, entity.getRole());
    }

    @Test
    public void testConstructorNullContextThrowsAssertionError() {
        LtiUserEntity user = buildUser();

        assertThrows(AssertionError.class, () -> new LtiMembershipEntity(null, user, 1));
    }

    @Test
    public void testConstructorNullUserThrowsAssertionError() {
        LtiContextEntity context = buildContext();

        assertThrows(AssertionError.class, () -> new LtiMembershipEntity(context, null, 1));
    }

    @Test
    public void testConstructorNullRoleIsAllowed() {
        LtiContextEntity context = buildContext();
        LtiUserEntity user = buildUser();

        LtiMembershipEntity entity = new LtiMembershipEntity(context, user, null);

        assertEquals(context, entity.getContext());
        assertEquals(user, entity.getUser());
        assertNull(entity.getRole());
    }

}
