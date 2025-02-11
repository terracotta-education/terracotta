package edu.iu.terracotta.dao.exceptions.integrations;

public class IntegrationTokenAlreadyRedeemedException extends Exception {

    public IntegrationTokenAlreadyRedeemedException(String message, Throwable e) {
        super(message, e);
    }

    public IntegrationTokenAlreadyRedeemedException(String message) {
        super(message);
    }

}
