package edu.iu.terracotta.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class ParticipantAlreadyStartedExceptionTest {

    @Test
    void testConstructorWithMessage() {
        ParticipantAlreadyStartedException exception = new ParticipantAlreadyStartedException("already started");

        assertEquals("already started", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        ParticipantAlreadyStartedException exception = new ParticipantAlreadyStartedException("already started", cause);

        assertEquals("already started", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

}
