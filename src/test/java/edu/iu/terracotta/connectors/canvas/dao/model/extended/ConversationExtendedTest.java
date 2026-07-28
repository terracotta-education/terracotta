package edu.iu.terracotta.connectors.canvas.dao.model.extended;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsConversation;

public class ConversationExtendedTest {

    @Test
    public void testGetIdReturnsNullWhenConversationIsNull() {
        ConversationExtended conversationExtended = ConversationExtended.builder().conversation(null).build();

        assertNull(conversationExtended.getId());
    }

    @Test
    public void testGetIdReturnsNullWhenConversationIdIsNull() {
        ConversationExtended conversationExtended = ConversationExtended.builder().build();

        assertNull(conversationExtended.getConversation().getId());
        assertNull(conversationExtended.getId());
    }

    @Test
    public void testGetIdReturnsStringWhenConversationIdIsSet() {
        ConversationExtended conversationExtended = ConversationExtended.builder().build();
        conversationExtended.getConversation().setId(99L);

        assertEquals("99", conversationExtended.getId());
    }

    @Test
    public void testSetIdDoesNothingWhenConversationIsNull() {
        ConversationExtended conversationExtended = ConversationExtended.builder().conversation(null).build();

        // Should not throw despite the conversation object being null.
        conversationExtended.setId("123");

        assertNull(conversationExtended.getId());
    }

    @Test
    public void testSetIdParsesAndSetsWhenConversationIsPresent() {
        ConversationExtended conversationExtended = ConversationExtended.builder().build();

        conversationExtended.setId("456");

        assertEquals(Long.valueOf(456L), conversationExtended.getConversation().getId());
        assertEquals("456", conversationExtended.getId());
    }

    @Test
    public void testSetIdDoesNothingWhenIdIsNull() {
        ConversationExtended conversationExtended = ConversationExtended.builder().build();
        conversationExtended.getConversation().setId(5L);

        conversationExtended.setId(null);

        assertEquals("5", conversationExtended.getId());
    }

    @Test
    public void testSetIdThrowsNumberFormatExceptionForNonNumericId() {
        ConversationExtended conversationExtended = ConversationExtended.builder().build();

        assertThrows(NumberFormatException.class, () -> conversationExtended.setId("not-a-number"));
    }

    @Test
    public void testFromCopiesIdAndType() {
        ConversationExtended conversationExtended = ConversationExtended.builder().build();
        conversationExtended.getConversation().setId(11L);

        LmsConversation lmsConversation = conversationExtended.from();

        assertNotNull(lmsConversation);
        assertEquals("11", lmsConversation.getId());
    }

    @Test
    public void testOfReturnsDefaultInstanceWhenLmsConversationIsNull() {
        ConversationExtended conversationExtended = ConversationExtended.of(null);

        assertNotNull(conversationExtended);
        assertNull(conversationExtended.getId());
    }

    @Test
    public void testOfCopiesIdWhenLmsConversationIsNonNull() {
        LmsConversation lmsConversation = LmsConversation.builder().id("321").build();

        ConversationExtended conversationExtended = ConversationExtended.of(lmsConversation);

        assertEquals("321", conversationExtended.getId());
    }

    @Test
    public void testOfReturnsNullIdWhenLmsConversationIdIsNull() {
        LmsConversation lmsConversation = LmsConversation.builder().build();

        ConversationExtended conversationExtended = ConversationExtended.of(lmsConversation);

        assertNotNull(conversationExtended);
        assertNull(conversationExtended.getId());
    }

}
