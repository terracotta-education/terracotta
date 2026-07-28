package edu.iu.terracotta.connectors.generic.dao.model.lms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests {@link LmsAssignment#addMetadata(String, Map)}, which:
 * <ul>
 *   <li>short-circuits (returns the current {@code metadata} unchanged, doing nothing else) when
 *   {@code key} is blank or {@code values} is empty/null;</li>
 *   <li>parses the existing {@code metadata} JSON (or starts from an empty map if none exists);</li>
 *   <li>if the resulting map is empty, replaces {@code metadata} with a fresh JSON object containing
 *   just {@code key -> values};</li>
 *   <li>if {@code key} already exists in the parsed metadata, merges {@code values} into the
 *   existing nested map via {@code Map#putAll}, so entries in {@code values} overwrite same-named
 *   entries already present for that key, while unrelated existing entries are preserved;</li>
 *   <li>if {@code key} is absent (but other keys exist), simply adds {@code key -> values};</li>
 *   <li>wraps any {@link tools.jackson.core.JacksonException} raised while parsing malformed
 *   existing {@code metadata} JSON into a {@link TerracottaConnectorException}.</li>
 * </ul>
 */
public class LmsAssignmentTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private Map<String, Map<String, Object>> parse(String json) {
        return jsonMapper.readValue(json, new TypeReference<Map<String, Map<String, Object>>>() {});
    }

    @Test
    public void testAddMetadataWithBlankKeyReturnsMetadataUnchanged() throws TerracottaConnectorException {
        LmsAssignment lmsAssignment = LmsAssignment.builder().build();
        lmsAssignment.setMetadata("existing-value");

        String resultEmptyKey = lmsAssignment.addMetadata("", Map.of("a", 1));
        assertEquals("existing-value", resultEmptyKey);
        assertEquals("existing-value", lmsAssignment.getMetadata());

        String resultBlankKey = lmsAssignment.addMetadata("   ", Map.of("a", 1));
        assertEquals("existing-value", resultBlankKey);
        assertEquals("existing-value", lmsAssignment.getMetadata());

        String resultNullKey = lmsAssignment.addMetadata(null, Map.of("a", 1));
        assertEquals("existing-value", resultNullKey);
        assertEquals("existing-value", lmsAssignment.getMetadata());
    }

    @Test
    public void testAddMetadataWithEmptyOrNullValuesReturnsMetadataUnchanged() throws TerracottaConnectorException {
        LmsAssignment lmsAssignment = LmsAssignment.builder().build();
        lmsAssignment.setMetadata("existing-value");

        String resultEmptyMap = lmsAssignment.addMetadata("key1", Map.of());
        assertEquals("existing-value", resultEmptyMap);
        assertEquals("existing-value", lmsAssignment.getMetadata());

        String resultNullMap = lmsAssignment.addMetadata("key1", null);
        assertEquals("existing-value", resultNullMap);
        assertEquals("existing-value", lmsAssignment.getMetadata());
    }

    @Test
    public void testAddMetadataWithNoExistingMetadataCreatesFreshMap() throws TerracottaConnectorException {
        LmsAssignment lmsAssignment = LmsAssignment.builder().build();

        assertNull(lmsAssignment.getMetadata());

        String result = lmsAssignment.addMetadata("key1", Map.of("a", 1));

        assertNotNull(result);
        assertEquals(result, lmsAssignment.getMetadata());

        Map<String, Map<String, Object>> parsed = parse(result);
        assertEquals(1, parsed.size());
        assertEquals(1, ((Number) parsed.get("key1").get("a")).intValue());
    }

    @Test
    public void testAddMetadataWithExistingKeyMergesValuesViaPutAll() throws TerracottaConnectorException {
        LmsAssignment lmsAssignment = LmsAssignment.builder().build();
        lmsAssignment.setMetadata(jsonMapper.writeValueAsString(Map.of("key1", Map.of("a", 1, "b", 2))));

        String result = lmsAssignment.addMetadata("key1", Map.of("a", 99, "c", 3));

        Map<String, Map<String, Object>> parsed = parse(result);
        assertEquals(1, parsed.size());
        Map<String, Object> merged = parsed.get("key1");
        // "a" came from the new values map, so it overwrites the previously-existing entry
        assertEquals(99, ((Number) merged.get("a")).intValue());
        // "b" was not present in the new values map, so the pre-existing entry is preserved
        assertEquals(2, ((Number) merged.get("b")).intValue());
        // "c" is a brand-new entry contributed by the new values map
        assertEquals(3, ((Number) merged.get("c")).intValue());
    }

    @Test
    public void testAddMetadataWithAbsentKeyAddsNewKeyPreservingExistingKeys() throws TerracottaConnectorException {
        LmsAssignment lmsAssignment = LmsAssignment.builder().build();
        lmsAssignment.setMetadata(jsonMapper.writeValueAsString(Map.of("key1", Map.of("a", 1))));

        String result = lmsAssignment.addMetadata("key2", Map.of("x", 42));

        Map<String, Map<String, Object>> parsed = parse(result);
        assertEquals(2, parsed.size());
        assertEquals(1, ((Number) parsed.get("key1").get("a")).intValue());
        assertEquals(42, ((Number) parsed.get("key2").get("x")).intValue());
    }

    @Test
    public void testAddMetadataWithMalformedExistingMetadataThrowsTerracottaConnectorException() {
        LmsAssignment lmsAssignment = LmsAssignment.builder().build();
        lmsAssignment.setMetadata("{ this is not valid json ]");

        TerracottaConnectorException exception = assertThrows(
            TerracottaConnectorException.class,
            () -> lmsAssignment.addMetadata("key1", Map.of("a", 1))
        );

        assertNotNull(exception.getCause());
        assertNotNull(exception.getMessage());
    }

}
