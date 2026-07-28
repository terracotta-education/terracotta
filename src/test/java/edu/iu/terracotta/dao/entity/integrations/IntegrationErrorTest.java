package edu.iu.terracotta.dao.entity.integrations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

/**
 * {@link IntegrationError} is a Lombok {@code @Builder} POJO. These tests exercise the static
 * factory method {@code from(String)}, which deserializes via the {@code tools.jackson} 3.x
 * {@code JsonMapper} and returns {@code null} (rather than throwing) on malformed input.
 */
public class IntegrationErrorTest {

    @Test
    public void testFromValidJsonRoundTrips() {
        IntegrationError original = IntegrationError.builder()
            .code("ERR-001")
            .errorMessage("something went wrong")
            .moreAttemptsAvailable(true)
            .build();

        String json = JsonMapper.builder()
            .build()
            .writeValueAsString(original);

        IntegrationError result = IntegrationError.from(json);

        assertEquals("ERR-001", result.getCode());
        assertEquals("something went wrong", result.getErrorMessage());
        assertEquals(true, result.isMoreAttemptsAvailable());
    }

    @Test
    public void testFromMalformedJsonReturnsNull() {
        IntegrationError result = IntegrationError.from("not-json-{");

        assertNull(result);
    }

}
