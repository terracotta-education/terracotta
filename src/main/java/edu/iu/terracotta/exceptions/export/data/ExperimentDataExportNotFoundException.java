package edu.iu.terracotta.exceptions.export.data;

public class ExperimentDataExportNotFoundException extends Exception {

    public ExperimentDataExportNotFoundException(String message) {
        super(message);
    }

    public ExperimentDataExportNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExperimentDataExportNotFoundException(Throwable cause) {
        super(cause);
    }

    public ExperimentDataExportNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
