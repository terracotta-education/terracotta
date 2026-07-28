package edu.iu.terracotta.connectors.canvas.dao.model.extended;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsFile;

public class FileExtendedTest {

    @Test
    public void testGetDisplayNameDelegatesToWrappedFile() {
        FileExtended fileExtended = FileExtended.builder().build();
        fileExtended.getFile().setDisplayName("display.txt");

        assertEquals("display.txt", fileExtended.getDisplayName());
    }

    @Test
    public void testGetFilenameDelegatesToWrappedFile() {
        FileExtended fileExtended = FileExtended.builder().build();
        fileExtended.getFile().setFilename("file.txt");

        assertEquals("file.txt", fileExtended.getFilename());
    }

    @Test
    public void testGetUrlDelegatesToWrappedFile() {
        FileExtended fileExtended = FileExtended.builder().build();
        fileExtended.getFile().setUrl("https://example.com/file.txt");

        assertEquals("https://example.com/file.txt", fileExtended.getUrl());
    }

    @Test
    public void testGetSizeDelegatesToWrappedFile() {
        FileExtended fileExtended = FileExtended.builder().build();
        fileExtended.getFile().setSize(42L);

        assertEquals(42L, fileExtended.getSize());
    }

    @Test
    public void testGetIdDelegatesToWrappedFile() {
        FileExtended fileExtended = FileExtended.builder().build();
        fileExtended.getFile().setId(9L);

        assertEquals("9", fileExtended.getId());
    }

    @Test
    public void testGetIdThrowsNullPointerExceptionWhenWrappedFileIdIsNull() {
        // NOTE (current-behavior / potential bug): FileExtended#getId() unconditionally
        // calls file.getId().toString() with no null guard. The default, freshly-built
        // Canvas File object has a null id, so this throws NullPointerException instead
        // of returning null (compare with CourseExtended/ConversationExtended, which
        // both null-guard their getId()). See FileExtended.java lines 19-22.
        FileExtended fileExtended = FileExtended.builder().build();

        assertThrows(NullPointerException.class, fileExtended::getId);
    }

    @Test
    public void testGetSizeThrowsNullPointerExceptionWhenWrappedFileSizeIsNull() {
        // NOTE (current-behavior / potential bug): FileExtended#getSize() returns a
        // primitive `long`, auto-unboxing file.getSize() (a Long). The default Canvas
        // File object has a null size, so this throws NullPointerException. See
        // FileExtended.java lines 34-37.
        FileExtended fileExtended = FileExtended.builder().build();

        assertThrows(NullPointerException.class, fileExtended::getSize);
    }

    @Test
    public void testFromReturnsSameInstanceAsLmsFile() {
        FileExtended fileExtended = FileExtended.builder().build();

        assertEquals(fileExtended, fileExtended.from());
    }

    @Test
    public void testOfReturnsDefaultInstanceWhenLmsFileIsNull() {
        FileExtended fileExtended = FileExtended.of(null);

        assertNotNull(fileExtended);
        assertNotNull(fileExtended.getFile());
        assertNull(fileExtended.getDisplayName());
        assertNull(fileExtended.getFilename());
        assertNull(fileExtended.getUrl());
    }

    @Test
    public void testOfPropagatesFieldsIntoWrappedFile() {
        LmsFile lmsFile = LmsFile.builder()
            .id("123")
            .displayName("display.txt")
            .filename("file.txt")
            .size(10L)
            .url("https://example.com/file.txt")
            .build();

        FileExtended fileExtended = FileExtended.of(lmsFile);

        assertNotNull(fileExtended);
        assertEquals("123", fileExtended.getId());
        assertEquals("display.txt", fileExtended.getDisplayName());
        assertEquals("file.txt", fileExtended.getFilename());
        assertEquals(10L, fileExtended.getSize());
        assertEquals("https://example.com/file.txt", fileExtended.getUrl());
    }

}
