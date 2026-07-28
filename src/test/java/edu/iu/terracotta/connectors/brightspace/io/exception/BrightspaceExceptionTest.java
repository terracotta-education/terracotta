package edu.iu.terracotta.connectors.brightspace.io.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class BrightspaceExceptionTest {

    @Test
    void testNoArgConstructorLeavesAllFieldsNull() {
        BrightspaceException exception = new BrightspaceException();

        assertNull(exception.getMessage());
        assertNull(exception.getBrightspaceErrorMessage());
        assertNull(exception.getRequestUrl());
        assertNull(exception.getError());
    }

    @Test
    void testMessageAndUrlConstructorFormatsMessage() {
        BrightspaceException exception = new BrightspaceException("bad request", "https://brightspace.example.com/api");

        assertEquals("Error from URL https://brightspace.example.com/api : bad request", exception.getMessage());
        assertEquals("bad request", exception.getBrightspaceErrorMessage());
        assertEquals("https://brightspace.example.com/api", exception.getRequestUrl());
        assertNull(exception.getError());
    }

    @Test
    void testMessageUrlAndErrorConstructorFormatsMessage() {
        Object errorPayload = new Object();
        BrightspaceException exception = new BrightspaceException("bad request", "https://brightspace.example.com/api", errorPayload);

        assertEquals("Error from URL https://brightspace.example.com/api : bad request", exception.getMessage());
        assertEquals("bad request", exception.getBrightspaceErrorMessage());
        assertEquals("https://brightspace.example.com/api", exception.getRequestUrl());
        assertSame(errorPayload, exception.getError());
    }

}
