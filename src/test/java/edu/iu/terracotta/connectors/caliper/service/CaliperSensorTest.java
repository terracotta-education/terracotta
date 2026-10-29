package edu.iu.terracotta.connectors.caliper.service;

import com.sun.net.httpserver.HttpServer;

import edu.iu.terracotta.connectors.caliper.dao.model.Envelope;
import edu.iu.terracotta.connectors.caliper.dao.model.enums.Action;
import edu.iu.terracotta.connectors.caliper.dao.model.events.ToolUseEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaliperSensorTest {

    private final AtomicInteger responseStatus = new AtomicInteger(200);
    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> contentType = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();

    private HttpServer server;
    private String host;

    @BeforeEach
    void beforeEach() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/caliper", exchange -> {
            method.set(exchange.getRequestMethod());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(responseStatus.get(), -1);
            exchange.close();
        });
        server.start();
        host = "http://127.0.0.1:" + server.getAddress().getPort() + "/caliper";
    }

    @AfterEach
    void afterEach() {
        server.stop(0);
    }

    @Test
    void testSendPostsTheEnvelopeWithTheApiKey() throws Exception {
        Envelope envelope = envelope();

        sensor("api-key", "application/json").send(envelope);

        assertEquals("POST", method.get());
        assertEquals("api-key", authorization.get());
        assertEquals("application/json", contentType.get());
        assertEquals(CaliperJsonSerializer.serialize(envelope), body.get());
    }

    @Test
    void testSendDefaultsTheContentTypeAndLeavesOutAMissingApiKey() throws Exception {
        sensor(null, " ").send(envelope());

        assertEquals("application/json", contentType.get());
        assertNull(authorization.get());
    }

    @Test
    void testSendAcceptsAnyStatusFrom200To202() throws Exception {
        for (int status : List.of(200, 201, 202)) {
            responseStatus.set(status);

            sensor("api-key", "application/json").send(envelope());
        }
    }

    @Test
    void testSendFailsOnAnyOtherStatus() {
        for (int status : List.of(204, 400, 401, 500)) {
            responseStatus.set(status);

            IOException exception = assertThrows(IOException.class, () -> sensor("api-key", "application/json").send(envelope()));

            assertTrue(exception.getMessage().contains(String.valueOf(status)));
        }
    }

    private CaliperSensor sensor(String apiKey, String contentType) {
        return CaliperSensor.builder()
            .id("sensor-1")
            .host(host)
            .apiKey(apiKey)
            .contentType(contentType)
            .connectionTimeout(5000)
            .socketTimeout(5000)
            .build();
    }

    private Envelope envelope() {
        return new Envelope("sensor-1", Instant.now(), "http://purl.imsglobal.org/ctx/caliper/v1p2", List.of(ToolUseEvent.builder().id("urn:uuid:1").action(Action.USED).build()));
    }

}
