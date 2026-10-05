package edu.iu.terracotta.connectors.generic.dao.model.lms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

public class LmsFolderTest {

    @Test
    public void testBuilderGettersAndSetters() {
        LmsFolder lmsFolder = LmsFolder.builder()
            .type(LmsFolder.class)
            .id("folder-1")
            .name("folder-name")
            .fullName("full/folder-name")
            .filesUrl("http://example.com/files")
            .build();

        assertEquals(LmsFolder.class, lmsFolder.getType());
        assertEquals("folder-1", lmsFolder.getId());
        assertEquals("folder-name", lmsFolder.getName());
        assertEquals("full/folder-name", lmsFolder.getFullName());
        assertEquals("http://example.com/files", lmsFolder.getFilesUrl());

        lmsFolder.setType(String.class);
        lmsFolder.setId("folder-2");
        lmsFolder.setName("folder-name-2");
        lmsFolder.setFullName("full/folder-name-2");
        lmsFolder.setFilesUrl("http://example.com/files2");

        assertEquals(String.class, lmsFolder.getType());
        assertEquals("folder-2", lmsFolder.getId());
        assertEquals("folder-name-2", lmsFolder.getName());
        assertEquals("full/folder-name-2", lmsFolder.getFullName());
        assertEquals("http://example.com/files2", lmsFolder.getFilesUrl());
    }

    @Test
    public void testFromReturnsSameInstance() {
        LmsFolder lmsFolder = LmsFolder.builder().id("folder-3").build();

        assertSame(lmsFolder, lmsFolder.from());
    }

}
