package edu.iu.terracotta.connectors.generic.dao.model.lms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

public class LmsConversationTest {

    @Test
    public void testBuilderGettersAndSetters() {
        LmsConversation lmsConversation = LmsConversation.builder()
            .type(LmsConversation.class)
            .id("conversation-1")
            .build();

        assertEquals(LmsConversation.class, lmsConversation.getType());
        assertEquals("conversation-1", lmsConversation.getId());

        lmsConversation.setType(String.class);
        lmsConversation.setId("conversation-2");

        assertEquals(String.class, lmsConversation.getType());
        assertEquals("conversation-2", lmsConversation.getId());
    }

    @Test
    public void testFromReturnsSameInstance() {
        LmsConversation lmsConversation = LmsConversation.builder().id("conversation-3").build();

        assertSame(lmsConversation, lmsConversation.from());
    }

}
