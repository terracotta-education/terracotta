package edu.iu.terracotta.exceptions.export.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class ExperimentDataExportExceptionTest {

    @Test
    void testConstructorWithMessage() {
        ExperimentDataExportException exception = new ExperimentDataExportException("export failed");

        assertEquals("export failed", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        ExperimentDataExportException exception = new ExperimentDataExportException("export failed", cause);

        assertEquals("export failed", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void testConstructorWithCauseOnly() {
        Throwable cause = new RuntimeException("root cause");
        ExperimentDataExportException exception = new ExperimentDataExportException(cause);

        assertSame(cause, exception.getCause());
    }

}
