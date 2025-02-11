package edu.iu.terracotta.connectors.generic.exceptions;

public class ApiException extends Exception {

    public ApiException(String message) {
        super(message);
    }

    public ApiException(Exception e) {
        super(e);
    }

    public ApiException(String message, Exception e) {
        super(message, e);
    }

}
