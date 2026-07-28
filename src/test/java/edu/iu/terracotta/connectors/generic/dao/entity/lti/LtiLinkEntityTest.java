package edu.iu.terracotta.connectors.generic.dao.entity.lti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests the hand-written, validating constructor of {@link LtiLinkEntity}:
 * {@code LtiLinkEntity(String linkKey, LtiContextEntity context, String title)}, plus
 * {@link LtiLinkEntity#createHtmlFromLink()}.
 */
public class LtiLinkEntityTest {

    private LtiContextEntity buildContext() {
        return LtiContextEntity.builder()
            .contextKey("context-key")
            .build();
    }

    @Test
    public void testConstructorSetsAllFieldsCorrectly() {
        LtiContextEntity context = buildContext();

        LtiLinkEntity entity = new LtiLinkEntity("link-key", context, "Link Title");

        assertEquals("link-key", entity.getLinkKey());
        assertEquals(context, entity.getContext());
        assertEquals("Link Title", entity.getTitle());
    }

    @Test
    public void testConstructorBlankLinkKeyThrowsAssertionError() {
        LtiContextEntity context = buildContext();

        assertThrows(AssertionError.class, () -> new LtiLinkEntity("", context, "title"));
        assertThrows(AssertionError.class, () -> new LtiLinkEntity("   ", context, "title"));
        assertThrows(AssertionError.class, () -> new LtiLinkEntity(null, context, "title"));
    }

    @Test
    public void testConstructorNullContextThrowsAssertionError() {
        assertThrows(AssertionError.class, () -> new LtiLinkEntity("link-key", null, "title"));
    }

    @Test
    public void testCreateHtmlFromLinkWithTitle() {
        LtiContextEntity context = buildContext();
        LtiLinkEntity entity = new LtiLinkEntity("abc123", context, "My Title");

        String expected = "Link Requested:\n" +
            "Link Key:" +
            "abc123" +
            "\nLink Title:" +
            "My Title" +
            "\n";

        assertEquals(expected, entity.createHtmlFromLink());
    }

    @Test
    public void testCreateHtmlFromLinkWithNullTitle() {
        LtiContextEntity context = buildContext();
        LtiLinkEntity entity = new LtiLinkEntity("link-2", context, null);

        String expected = "Link Requested:\n" +
            "Link Key:" +
            "link-2" +
            "\nLink Title:" +
            "null" +
            "\n";

        assertEquals(expected, entity.createHtmlFromLink());
    }

}
