package edu.iu.terracotta.exceptions;

public class ParticipantAlreadyStartedException extends Exception {

    public ParticipantAlreadyStartedException(String message) {
        super(message);
    }

    public ParticipantAlreadyStartedException(String message, Throwable cause) {
        super(message, cause);
    }
}
