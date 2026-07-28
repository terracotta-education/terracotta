package edu.iu.terracotta.connectors.generic.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class ApiExceptionTest {

    @Test
    void testConstructorWithMessage() {
        ApiException exception = new ApiException("api failed");

        assertEquals("api failed", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithExceptionOnly() {
        Exception cause = new RuntimeException("root cause");
        ApiException exception = new ApiException(cause);

        assertSame(cause, exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndException() {
        Exception cause = new RuntimeException("root cause");
        ApiException exception = new ApiException("api failed", cause);

        assertEquals("api failed", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

}
