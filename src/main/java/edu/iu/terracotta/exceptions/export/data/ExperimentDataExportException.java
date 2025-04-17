package edu.iu.terracotta.exceptions.export.data;

public class ExperimentDataExportException extends Exception {

    public ExperimentDataExportException(String message) {
        super(message);
    }

    public ExperimentDataExportException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExperimentDataExportException(Throwable cause) {
        super(cause);
    }

}
