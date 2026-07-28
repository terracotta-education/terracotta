package edu.iu.terracotta.connectors.generic.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class LmsOAuthExceptionTest {

    @Test
    void testConstructorWithMessage() {
        LmsOAuthException exception = new LmsOAuthException("oauth failed");

        assertEquals("oauth failed", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithCauseOnly() {
        Throwable cause = new RuntimeException("root cause");
        LmsOAuthException exception = new LmsOAuthException(cause);

        assertSame(cause, exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        LmsOAuthException exception = new LmsOAuthException("oauth failed", cause);

        assertEquals("oauth failed", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

}
