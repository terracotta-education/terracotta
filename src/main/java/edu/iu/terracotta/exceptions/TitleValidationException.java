package edu.iu.terracotta.exceptions;

public class TitleValidationException extends Exception {

    // the message is shown to the user; the log message, when given, names what failed for the logs
    private final String logMessage;

    public TitleValidationException(String message) {
        this(message, null);
    }

    public TitleValidationException(String message, String logMessage) {
        super(message);
        this.logMessage = logMessage;
    }

    public String getLogMessage() {
        return logMessage != null ? logMessage : getMessage();
    }

}
