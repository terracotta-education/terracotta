package edu.iu.terracotta.service.caliper.impl;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.imsglobal.caliper.Envelope;
import org.imsglobal.caliper.clients.AbstractClient;

/**
 * Utility class that serializes Caliper envelope.
 */
@SuppressWarnings({"PMD.UncommentedEmptyMethodBody"})
public class JsonSerializerClient extends AbstractClient {

    protected JsonSerializerClient() {
        super("JsonSerializerClient", null);
    }

    @Override
    public void send(Envelope envelope) {
    }

    public static String serialize(Envelope envelope) {
        try {
            return new JsonSerializerClient().serializeEnvelope(envelope);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize envelope", e);
        }
    }

}
