package edu.iu.terracotta.exceptions;

public class ExperimentImportException extends RuntimeException {

    public ExperimentImportException(String message) {
        super(message);
    }

    public ExperimentImportException(String message, Throwable cause) {
        super(message, cause);
    }

}
