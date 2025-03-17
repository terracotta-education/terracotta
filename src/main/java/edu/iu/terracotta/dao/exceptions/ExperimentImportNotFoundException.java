package edu.iu.terracotta.dao.exceptions;

public class ExperimentImportNotFoundException extends Exception {

    public ExperimentImportNotFoundException(String message) {
        super(message);
    }

    public ExperimentImportNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
