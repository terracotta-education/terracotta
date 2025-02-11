package edu.iu.terracotta.connectors.generic.exceptions;

public class LmsOAuthException extends Exception {

    public LmsOAuthException(String message, Throwable cause) {
        super(message, cause);
    }

    public LmsOAuthException(String message) {
        super(message);
    }

    public LmsOAuthException(Throwable cause) {
        super(cause);
    }

}
