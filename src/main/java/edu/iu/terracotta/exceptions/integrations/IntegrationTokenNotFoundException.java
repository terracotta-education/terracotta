package edu.iu.terracotta.exceptions.integrations;

public class IntegrationTokenNotFoundException extends Exception {

    public IntegrationTokenNotFoundException(String message, Throwable e) {
        super(message, e);
    }

    public IntegrationTokenNotFoundException(String message) {
        super(message);
    }

}
