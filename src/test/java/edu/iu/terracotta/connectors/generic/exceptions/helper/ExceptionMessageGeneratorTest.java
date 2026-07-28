package edu.iu.terracotta.connectors.generic.exceptions.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExceptionMessageGeneratorTest {

    private ExceptionMessageGenerator exceptionMessageGenerator;

    @BeforeEach
    public void beforeEach() {
        exceptionMessageGenerator = new ExceptionMessageGenerator();
    }

    @Test
    public void testExceptionMessageWithNullExceptionReturnsOnlyCustomMessage() {
        String result = exceptionMessageGenerator.exceptionMessage("custom message", null);

        assertEquals("custom message\n", result);
    }

    @Test
    public void testExceptionMessageWithExceptionAndNoCauseAppendsExceptionMessage() {
        Exception exception = new RuntimeException("inner failure");

        String result = exceptionMessageGenerator.exceptionMessage("custom message", exception);

        assertEquals("custom message\nException : inner failure", result);
    }

    @Test
    public void testExceptionMessageWithExceptionAndCauseAppendsCauseMessage() {
        Exception cause = new RuntimeException("root cause");
        Exception exception = new RuntimeException("outer", cause);

        String result = exceptionMessageGenerator.exceptionMessage("custom message", exception);

        assertEquals("custom message\nException : outer\nCause :root cause", result);
    }

}
