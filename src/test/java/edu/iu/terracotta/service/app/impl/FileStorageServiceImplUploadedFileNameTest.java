package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceImplUploadedFileNameTest {

    @Test
    void keepsTheUploadedName() {
        assertEquals("xyz.pdf", FileStorageServiceImpl.uploadedFileName(upload("xyz.pdf")));
    }

    @Test
    void dropsAPathSomeBrowsersSend() {
        assertEquals("xyz.pdf", FileStorageServiceImpl.uploadedFileName(upload("C:\\Users\\ada\\Documents\\xyz.pdf")));
        assertEquals("xyz.pdf", FileStorageServiceImpl.uploadedFileName(upload("/home/ada/xyz.pdf")));
    }

    @Test
    void removesControlCharactersAndSurroundingSpace() {
        assertEquals("xyz.pdf", FileStorageServiceImpl.uploadedFileName(upload("  xyz\u0007.pdf\n ")));
    }

    @Test
    void cutsALongNameToTheColumnLength() {
        assertEquals(255, FileStorageServiceImpl.uploadedFileName(upload("a".repeat(300) + ".pdf")).length());
    }

    @Test
    void hasNoNameWithoutOne() {
        assertNull(FileStorageServiceImpl.uploadedFileName(upload(null)));
        assertNull(FileStorageServiceImpl.uploadedFileName(upload("   ")));
        assertNull(FileStorageServiceImpl.uploadedFileName(upload("folder/")));
    }

    private static MockMultipartFile upload(String originalFilename) {
        return new MockMultipartFile("consent", originalFilename, "application/pdf", new byte[] {1});
    }

}
