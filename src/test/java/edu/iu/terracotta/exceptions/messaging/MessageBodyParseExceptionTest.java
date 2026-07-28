package edu.iu.terracotta.exceptions.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class MessageBodyParseExceptionTest {

    @Test
    void testConstructorWithMessage() {
        MessageBodyParseException exception = new MessageBodyParseException("parse failed");

        assertEquals("parse failed", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        MessageBodyParseException exception = new MessageBodyParseException("parse failed", cause);

        assertEquals("parse failed", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void testConstructorWithCauseOnly() {
        Throwable cause = new RuntimeException("root cause");
        MessageBodyParseException exception = new MessageBodyParseException(cause);

        assertSame(cause, exception.getCause());
    }

}
