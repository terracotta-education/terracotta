package edu.iu.terracotta.exceptions.export.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class ExperimentDataExportNotFoundExceptionTest {

    @Test
    void testConstructorWithMessage() {
        ExperimentDataExportNotFoundException exception = new ExperimentDataExportNotFoundException("not found");

        assertEquals("not found", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        ExperimentDataExportNotFoundException exception = new ExperimentDataExportNotFoundException("not found", cause);

        assertEquals("not found", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void testConstructorWithCauseOnly() {
        Throwable cause = new RuntimeException("root cause");
        ExperimentDataExportNotFoundException exception = new ExperimentDataExportNotFoundException(cause);

        assertSame(cause, exception.getCause());
    }

    @Test
    void testConstructorWithMessageCauseAndSuppressionFlags() {
        Throwable cause = new RuntimeException("root cause");
        ExperimentDataExportNotFoundException exception = new ExperimentDataExportNotFoundException("not found", cause, false, false);

        assertEquals("not found", exception.getMessage());
        assertSame(cause, exception.getCause());
        // writableStackTrace=false means fillInStackTrace() is a no-op, leaving an empty trace.
        assertEquals(0, exception.getStackTrace().length);
        assertEquals(0, exception.getSuppressed().length);
    }

}
