package edu.iu.terracotta.connectors.caliper.service;

import edu.iu.terracotta.connectors.caliper.dao.model.Envelope;
import lombok.Builder;
import lombok.Getter;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Sends Caliper envelopes to one endpoint, the way caliper-java's HttpClient did: a POST of the envelope's JSON,
 * with the API key as the Authorization header. Any response other than 200 to 202 is a failure.
 */
@Getter
@Builder
public class CaliperSensor {

    private static final String DEFAULT_CONTENT_TYPE = "application/json";

    private final String id;
    private final String host;
    private final String apiKey;
    private final String contentType;
    // in milliseconds; unset or 0 means no limit
    private final Integer connectionTimeout;
    private final Integer socketTimeout;

    public void send(Envelope envelope) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(host))
            .header("Content-Type", StringUtils.defaultIfBlank(contentType, DEFAULT_CONTENT_TYPE))
            .POST(HttpRequest.BodyPublishers.ofString(CaliperJsonSerializer.serialize(envelope), StandardCharsets.UTF_8));

        if (apiKey != null) {
            request.header("Authorization", apiKey);
        }

        if (isSet(socketTimeout)) {
            request.timeout(Duration.ofMillis(socketTimeout));
        }

        HttpClient.Builder client = HttpClient.newBuilder();

        if (isSet(connectionTimeout)) {
            client.connectTimeout(Duration.ofMillis(connectionTimeout));
        }

        try (HttpClient httpClient = client.build()) {
            int statusCode = httpClient.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();

            if (statusCode < 200 || statusCode > 202) {
                throw new IOException(String.format("Caliper POST to '%s' failed with status code %s", host, statusCode));
            }
        }
    }

    private static boolean isSet(Integer timeout) {
        return timeout != null && timeout > 0;
    }

}
