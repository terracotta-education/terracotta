package edu.iu.terracotta.connectors.generic.exceptions;

public class TerracottaConnectorException extends Exception {

    public TerracottaConnectorException(String message) {
        super(message);
    }

    public TerracottaConnectorException(Exception e) {
        super(e);
    }

    public TerracottaConnectorException(String message, Exception e) {
        super(message, e);
    }

}
