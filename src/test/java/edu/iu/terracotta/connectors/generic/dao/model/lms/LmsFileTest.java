package edu.iu.terracotta.connectors.generic.dao.model.lms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

public class LmsFileTest {

    @Test
    public void testBuilderGettersAndSetters() {
        LmsFile lmsFile = LmsFile.builder()
            .type(LmsFile.class)
            .id("file-1")
            .displayName("display-name")
            .filename("file.txt")
            .size(1024L)
            .url("http://example.com/file.txt")
            .build();

        assertEquals(LmsFile.class, lmsFile.getType());
        assertEquals("file-1", lmsFile.getId());
        assertEquals("display-name", lmsFile.getDisplayName());
        assertEquals("file.txt", lmsFile.getFilename());
        assertEquals(1024L, lmsFile.getSize());
        assertEquals("http://example.com/file.txt", lmsFile.getUrl());

        lmsFile.setType(String.class);
        lmsFile.setId("file-2");
        lmsFile.setDisplayName("display-name-2");
        lmsFile.setFilename("file2.txt");
        lmsFile.setSize(2048L);
        lmsFile.setUrl("http://example.com/file2.txt");

        assertEquals(String.class, lmsFile.getType());
        assertEquals("file-2", lmsFile.getId());
        assertEquals("display-name-2", lmsFile.getDisplayName());
        assertEquals("file2.txt", lmsFile.getFilename());
        assertEquals(2048L, lmsFile.getSize());
        assertEquals("http://example.com/file2.txt", lmsFile.getUrl());
    }

    @Test
    public void testFromReturnsSameInstance() {
        LmsFile lmsFile = LmsFile.builder().id("file-3").build();

        assertSame(lmsFile, lmsFile.from());
    }

}
