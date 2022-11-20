package edu.iu.terracotta.exceptions;

public class LMSOAuthException extends Exception {

    public LMSOAuthException(String message, Throwable cause) {
        super(message, cause);
    }

    public LMSOAuthException(String message) {
        super(message);
    }

    public LMSOAuthException(Throwable cause) {
        super(cause);
    }
}
