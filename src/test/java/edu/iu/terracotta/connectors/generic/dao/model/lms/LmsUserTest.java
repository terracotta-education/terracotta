package edu.iu.terracotta.connectors.generic.dao.model.lms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

public class LmsUserTest {

    @Test
    public void testBuilderGettersAndSetters() {
        LmsUser lmsUser = LmsUser.builder()
            .type(LmsUser.class)
            .email("user@example.com")
            .id("user-1")
            .build();

        assertEquals(LmsUser.class, lmsUser.getType());
        assertEquals("user@example.com", lmsUser.getEmail());
        assertEquals("user-1", lmsUser.getId());

        lmsUser.setType(String.class);
        lmsUser.setEmail("user2@example.com");
        lmsUser.setId("user-2");

        assertEquals(String.class, lmsUser.getType());
        assertEquals("user2@example.com", lmsUser.getEmail());
        assertEquals("user-2", lmsUser.getId());
    }

    @Test
    public void testFromReturnsSameInstance() {
        LmsUser lmsUser = LmsUser.builder().id("user-3").build();

        assertSame(lmsUser, lmsUser.from());
    }

}
