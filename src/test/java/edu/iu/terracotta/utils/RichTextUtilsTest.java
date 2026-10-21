package edu.iu.terracotta.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class RichTextUtilsTest {

    @Test
    public void testRecognizesWhatTheEditorSaves() {
        assertTrue(RichTextUtils.isRichText("<p>text</p>"));
        assertTrue(RichTextUtils.isRichText(" <ul><li><p>item</p></li></ul>"));
        assertFalse(RichTextUtils.isRichText("plain text"));
        assertFalse(RichTextUtils.isRichText("<3 this"));
        assertFalse(RichTextUtils.isRichText(null));
    }

    // responses typed before essays took rich text
    @Test
    public void testLeavesPlainTextAsItIs() {
        assertEquals("a < b\n\nsecond paragraph", RichTextUtils.toPlainText("a < b\n\nsecond paragraph"));
        assertNull(RichTextUtils.toPlainText(null));
    }

    @Test
    public void testSeparatesParagraphsWithABlankLineAndDropsFormatting() {
        assertEquals(
            "First <strong> & bold\nwith a break\n\nSecond",
            RichTextUtils.toPlainText("<p>First &lt;strong&gt; &amp; <strong>bold</strong><br>with a break</p><p><em>Second</em></p>")
        );
    }

    @Test
    public void testPutsEachListItemOnItsOwnLine() {
        assertEquals(
            "Intro\n\n- one\n- two\n  1. nested\n  2. again\n\n1. first\n2. second\n\nEnd",
            RichTextUtils.toPlainText(
                "<p>Intro</p>"
                + "<ul><li><p>one</p></li><li><p>two</p><ol><li><p>nested</p></li><li><p>again</p></li></ol></li></ul>"
                + "<ol><li><p>first</p></li><li><p>second</p></li></ol>"
                + "<p>End</p>"
            )
        );
    }

}
