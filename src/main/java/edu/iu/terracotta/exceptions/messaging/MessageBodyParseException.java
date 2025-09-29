package edu.iu.terracotta.exceptions.messaging;

public class MessageBodyParseException extends Exception {
    private static final long serialVersionUID = 1L;

    public MessageBodyParseException(String message) {
        super(message);
    }

    public MessageBodyParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageBodyParseException(Throwable cause) {
        super(cause);
    }

}
