package edu.iu.terracotta.connectors.brightspace.dao.model.extended;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsUser;

public class UserExtendedTest {

    @Test
    public void testOfWithNullLmsUserReturnsEmptyUserExtended() {
        UserExtended userExtended = UserExtended.of(null);

        assertNotNull(userExtended);
        assertEquals("null", userExtended.getId());
        assertNull(userExtended.getEmail());
    }

    @Test
    public void testOfWithLmsUserPropagatesIdAndEmailToClasslistUser() {
        LmsUser lmsUser = LmsUser.builder()
            .id("456")
            .email("user@example.com")
            .build();

        UserExtended userExtended = UserExtended.of(lmsUser);

        assertNotNull(userExtended);
        assertEquals("456", userExtended.getId());
        assertEquals("user@example.com", userExtended.getEmail());
    }

    @Test
    public void testGetIdDelegatesToClasslistUserIdentifier() {
        UserExtended userExtended = UserExtended.builder().build();
        userExtended.getClasslistUser().setIdentifier("789");

        assertEquals("789", userExtended.getId());
    }

    @Test
    public void testGetEmailDelegatesToClasslistUserEmail() {
        UserExtended userExtended = UserExtended.builder().build();
        userExtended.getClasslistUser().setEmail("nested@example.com");

        assertEquals("nested@example.com", userExtended.getEmail());
    }

    @Test
    public void testFromMapsIdAndEmailFromClasslistUser() {
        UserExtended userExtended = UserExtended.builder().build();
        userExtended.getClasslistUser().setIdentifier("321");
        userExtended.getClasslistUser().setEmail("from@example.com");

        LmsUser mapped = userExtended.from();

        assertNotNull(mapped);
        assertEquals("321", mapped.getId());
        assertEquals("from@example.com", mapped.getEmail());
    }

    @Test
    public void testFromWithDefaultClasslistUserReturnsNullStringIdAndNullEmail() {
        UserExtended userExtended = UserExtended.builder().build();

        LmsUser mapped = userExtended.from();

        assertNotNull(mapped);
        assertEquals("null", mapped.getId());
        assertNull(mapped.getEmail());
    }

}
