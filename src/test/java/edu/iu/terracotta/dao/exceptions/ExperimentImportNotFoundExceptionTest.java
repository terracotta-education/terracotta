package edu.iu.terracotta.dao.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class ExperimentImportNotFoundExceptionTest {

    @Test
    void testConstructorWithMessage() {
        ExperimentImportNotFoundException exception = new ExperimentImportNotFoundException("not found");

        assertEquals("not found", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        ExperimentImportNotFoundException exception = new ExperimentImportNotFoundException("not found", cause);

        assertEquals("not found", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

}
