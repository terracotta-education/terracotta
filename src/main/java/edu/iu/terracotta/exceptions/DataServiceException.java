package edu.iu.terracotta.exceptions;

public class DataServiceException extends Exception {

    public DataServiceException(String message) {
        super(message);
    }

    public DataServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
