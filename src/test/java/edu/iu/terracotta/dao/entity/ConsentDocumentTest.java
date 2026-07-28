package edu.iu.terracotta.dao.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link ConsentDocument} is a Lombok {@code @Builder} JPA entity. These tests exercise the
 * hand-written {@code @Transient} helper methods: {@code isCompressed()}, {@code getEncodedFileName()},
 * and {@code getEncryptedFileUri()}.
 */
public class ConsentDocumentTest {

    @Test
    public void testIsCompressedTrueWhenAllSet() {
        ConsentDocument consentDocument = ConsentDocument.builder()
            .encryptionMethod("AES")
            .encryptionPhrase("phrase")
            .fileUri("folder/file.txt")
            .build();

        assertTrue(consentDocument.isCompressed());
    }

    @Test
    public void testIsCompressedFalseWhenEncryptionMethodNull() {
        ConsentDocument consentDocument = ConsentDocument.builder()
            .encryptionMethod(null)
            .encryptionPhrase("phrase")
            .fileUri("folder/file.txt")
            .build();

        assertFalse(consentDocument.isCompressed());
    }

    @Test
    public void testIsCompressedFalseWhenEncryptionPhraseNull() {
        ConsentDocument consentDocument = ConsentDocument.builder()
            .encryptionMethod("AES")
            .encryptionPhrase(null)
            .fileUri("folder/file.txt")
            .build();

        assertFalse(consentDocument.isCompressed());
    }

    @Test
    public void testIsCompressedFalseWhenFileUriNull() {
        ConsentDocument consentDocument = ConsentDocument.builder()
            .encryptionMethod("AES")
            .encryptionPhrase("phrase")
            .fileUri(null)
            .build();

        assertFalse(consentDocument.isCompressed());
    }

    @Test
    public void testIsCompressedFalseWhenEncryptionMethodEmpty() {
        ConsentDocument consentDocument = ConsentDocument.builder()
            .encryptionMethod("")
            .encryptionPhrase("phrase")
            .fileUri("folder/file.txt")
            .build();

        assertFalse(consentDocument.isCompressed());
    }

    @Test
    public void testIsCompressedFalseWhenEncryptionPhraseEmpty() {
        ConsentDocument consentDocument = ConsentDocument.builder()
            .encryptionMethod("AES")
            .encryptionPhrase("")
            .fileUri("folder/file.txt")
            .build();

        assertFalse(consentDocument.isCompressed());
    }

    @Test
    public void testIsCompressedFalseWhenFileUriEmpty() {
        ConsentDocument consentDocument = ConsentDocument.builder()
            .encryptionMethod("AES")
            .encryptionPhrase("phrase")
            .fileUri("")
            .build();

        assertFalse(consentDocument.isCompressed());
    }

    @Test
    public void testGetEncodedFileNameWithPathSeparators() {
        ConsentDocument consentDocument = ConsentDocument.builder()
            .fileUri("folder/subfolder/file.txt")
            .build();

        assertEquals("file.txt", consentDocument.getEncodedFileName());
    }

    @Test
    public void testGetEncodedFileNameWithNoPathSeparator() {
        ConsentDocument consentDocument = ConsentDocument.builder()
            .fileUri("file.txt")
            .build();

        // StringUtils.substringAfterLast returns "" when the separator is not found.
        assertEquals("", consentDocument.getEncodedFileName());
    }

    @Test
    public void testGetEncryptedFileUri() {
        ConsentDocument consentDocument = ConsentDocument.builder()
            .fileUri("export-1")
            .build();

        assertEquals("export-1" + ConsentDocument.COMPRESSED_FILE_EXTENSION, consentDocument.getEncryptedFileUri());
    }

}
