package edu.iu.terracotta.connectors.brightspace.io.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.brightspace.io.model.enums.richtext.RichTextType;

public class RichTextInputTest {

    @Test
    public void testFromNullReturnsEmptyResult() {
        RichTextInput richTextInput = RichTextInput.from(null);

        assertNull(richTextInput.getContent());
        assertNull(richTextInput.getType());
    }

    @Test
    public void testFromWithNonBlankHtmlUsesHtmlContentAndType() {
        RichText richText = RichText.builder()
            .html("<p>html content</p>")
            .text("plain text content")
            .build();

        RichTextInput richTextInput = RichTextInput.from(richText);

        assertEquals("<p>html content</p>", richTextInput.getContent());
        assertEquals(RichTextType.HTML.type(), richTextInput.getType());
    }

    @Test
    public void testFromWithNullHtmlAndNonBlankTextUsesTextContentAndType() {
        RichText richText = RichText.builder()
            .html(null)
            .text("plain text content")
            .build();

        RichTextInput richTextInput = RichTextInput.from(richText);

        assertEquals("plain text content", richTextInput.getContent());
        assertEquals(RichTextType.TEXT.type(), richTextInput.getType());
    }

    @Test
    public void testFromWithBlankHtmlAndNonBlankTextUsesTextContentAndType() {
        RichText richText = RichText.builder()
            .html("   ")
            .text("plain text content")
            .build();

        RichTextInput richTextInput = RichTextInput.from(richText);

        assertEquals("plain text content", richTextInput.getContent());
        assertEquals(RichTextType.TEXT.type(), richTextInput.getType());
    }

    @Test
    public void testFromWithBothHtmlAndTextNullResultsInNullContentAndTextType() {
        RichText richText = RichText.builder()
            .html(null)
            .text(null)
            .build();

        RichTextInput richTextInput = RichTextInput.from(richText);

        assertNull(richTextInput.getContent());
        assertEquals(RichTextType.TEXT.type(), richTextInput.getType());
    }

}
