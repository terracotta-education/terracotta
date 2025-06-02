package edu.iu.terracotta.exceptions.messaging;

import java.util.List;
import java.util.stream.Collectors;

public class MessagePipedTextValidationException extends Exception {

    public MessagePipedTextValidationException(List<String> messages) {
        super(messages.stream().collect(Collectors.joining("::")));
    }

    public MessagePipedTextValidationException(List<String> messages, Throwable cause) {
        super(messages.stream().collect(Collectors.joining("::")), cause);
    }

    public MessagePipedTextValidationException(String message, Throwable cause) {
        super(message, cause);
    }

}
