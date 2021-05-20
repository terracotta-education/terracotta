package edu.iu.terracotta.exceptions;

public class CanvasApiException extends Exception {

    public static final String ERROR_CODE_PREFIX = "CA";

    public CanvasApiException(String message) {
        super(message);
    }

    public CanvasApiException(String message, Exception e) {
        super(message,e);
    }

}