package edu.iu.terracotta.service.caliper.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.imsglobal.caliper.CaliperSendable;
import org.imsglobal.caliper.Envelope;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

/**
 * Plain unit test for {@link JsonSerializerClient}. This is a tiny utility class with no
 * Spring/Mockito dependencies, so it is exercised directly against a real
 * {@link org.imsglobal.caliper.Envelope}.
 */
class JsonSerializerClientTest {

    private static final String DATA_VERSION = "http://purl.imsglobal.org/ctx/caliper/v1p2";

    @Test
    void testSerialize() {
        Envelope envelope = new Envelope("https://terracotta.instructure.com/sensors/1", DateTime.now(), DATA_VERSION, Collections.<CaliperSendable>emptyList());

        String json = JsonSerializerClient.serialize(envelope);

        assertTrue(json.contains("\"sensor\""));
        assertTrue(json.contains("https://terracotta.instructure.com/sensors/1"));
        assertTrue(json.contains("\"dataVersion\""));
        assertTrue(json.contains(DATA_VERSION));
    }

    @Test
    void testSerializeWrapsJsonProcessingExceptionAsRuntimeException() {
        // a CaliperSendable whose getter throws forces Jackson to fail mid-serialization,
        // exercising serialize()'s catch (JsonProcessingException) -> RuntimeException branch
        CaliperSendable poison = new CaliperSendable() {
            @SuppressWarnings("unused")
            public String getFoo() {
                throw new IllegalStateException("boom");
            }
        };
        Envelope envelope = new Envelope("https://terracotta.instructure.com/sensors/1", DateTime.now(), DATA_VERSION, Collections.singletonList(poison));

        RuntimeException exception = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> JsonSerializerClient.serialize(envelope));

        assertTrue(exception.getMessage().contains("Failed to serialize envelope"));
    }

    @Test
    void testSend() {
        Envelope envelope = new Envelope("https://terracotta.instructure.com/sensors/1", DateTime.now(), DATA_VERSION, Collections.<CaliperSendable>emptyList());

        // send() is a no-op override; just verify it does not throw.
        assertDoesNotThrow(() -> new JsonSerializerClient().send(envelope));
    }

}
