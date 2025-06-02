package edu.iu.terracotta.exceptions.messaging;

public class MessageSendEmailException extends Exception {

    public MessageSendEmailException(String message, Throwable e) {
        super(message, e);
    }

}
