package edu.iu.terracotta.exceptions.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class MyFileNotFoundExceptionTest {

    @Test
    void testConstructorWithMessage() {
        MyFileNotFoundException exception = new MyFileNotFoundException("file not found");

        assertEquals("file not found", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        MyFileNotFoundException exception = new MyFileNotFoundException("file not found", cause);

        assertEquals("file not found", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

}
