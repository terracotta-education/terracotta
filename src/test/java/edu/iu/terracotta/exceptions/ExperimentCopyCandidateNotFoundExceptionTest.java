package edu.iu.terracotta.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ExperimentCopyCandidateNotFoundExceptionTest {

    @Test
    void testConstructorWithMessage() {
        ExperimentCopyCandidateNotFoundException exception = new ExperimentCopyCandidateNotFoundException("not found");

        assertEquals("not found", exception.getMessage());
        assertNull(exception.getCause());
    }

}
