package edu.iu.terracotta.service.caliper.impl;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.imsglobal.caliper.Envelope;
import org.imsglobal.caliper.clients.AbstractClient;

/**
 * Utility class that serializes Caliper envelope.
 */
public class JSONSerializerClient extends AbstractClient {

    protected JSONSerializerClient() {
        super("JSONSerializerClient", null);
    }

    @Override
    public void send(Envelope envelope) {

    }

    public static String serialize(Envelope envelope) {
        try {
            return new JSONSerializerClient().serializeEnvelope(envelope);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize envelope", e);
        }
    }
}
