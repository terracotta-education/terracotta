package edu.iu.terracotta.connectors.canvas.dao.model.extended;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsUser;

public class UserExtendedTest {

    @Test
    public void testOfReturnsDefaultInstanceWhenLmsUserIsNull() {
        UserExtended userExtended = UserExtended.of(null);

        assertNotNull(userExtended);
        assertNotNull(userExtended.getUser());
        // Canvas User#getId() is a primitive long; the default, freshly-built
        // Canvas User object reports 0 rather than null.
        assertEquals("0", userExtended.getId());
        assertNull(userExtended.getEmail());
    }

    @Test
    public void testOfWithNonNullLmsUser() {
        LmsUser lmsUser = LmsUser.builder()
            .id("7")
            .email("student@example.com")
            .build();

        UserExtended userExtended = UserExtended.of(lmsUser);

        assertNotNull(userExtended);
        assertEquals("7", userExtended.getId());
        assertEquals("student@example.com", userExtended.getEmail());
    }

    @Test
    public void testGetIdDelegatesToWrappedUser() {
        UserExtended userExtended = UserExtended.builder().build();
        userExtended.getUser().setId(42L);

        assertEquals("42", userExtended.getId());
    }

    @Test
    public void testGetEmailDelegatesToWrappedUser() {
        UserExtended userExtended = UserExtended.builder().build();
        userExtended.getUser().setEmail("teacher@example.com");

        assertEquals("teacher@example.com", userExtended.getEmail());
    }

    @Test
    public void testFromReturnsSameInstanceAsLmsUser() {
        UserExtended userExtended = UserExtended.builder().build();

        assertEquals(userExtended, userExtended.from());
    }

}
