package edu.iu.terracotta.dao.exceptions.integrations;

public class IntegrationTokenExpiredException extends Exception {

    public IntegrationTokenExpiredException(String message, Throwable e) {
        super(message, e);
    }

    public IntegrationTokenExpiredException(String message) {
        super(message);
    }

}
