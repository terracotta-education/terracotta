package edu.iu.terracotta.dao.model.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

public class AnswerSubmissionDtoTest {

    // a file answer's stored path and encryption settings are the server's to set: from a request
    // they let a student point the answer at any file the server can read
    @Test
    public void testIgnoresStoredFileFieldsFromRequests() {
        AnswerSubmissionDto answerSubmissionDto = JsonMapper.builder().build().readValue(
            "{\"fileName\":\"essay.pdf\",\"fileUri\":\"../../../etc/passwd\",\"encryptionPhrase\":\"x\",\"encryptionMethod\":\"AES\"}",
            AnswerSubmissionDto.class
        );

        assertEquals("essay.pdf", answerSubmissionDto.getFileName());
        assertNull(answerSubmissionDto.getFileUri());
        assertNull(answerSubmissionDto.getEncryptionPhrase());
        assertNull(answerSubmissionDto.getEncryptionMethod());
    }

}
