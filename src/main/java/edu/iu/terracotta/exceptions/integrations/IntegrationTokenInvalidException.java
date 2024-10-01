package edu.iu.terracotta.exceptions.integrations;

public class IntegrationTokenInvalidException extends Exception {

    public IntegrationTokenInvalidException(String message, Throwable e) {
        super(message, e);
    }

    public IntegrationTokenInvalidException(String message) {
        super(message);
    }

}
