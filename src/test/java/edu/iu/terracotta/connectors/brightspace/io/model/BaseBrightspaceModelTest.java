package edu.iu.terracotta.connectors.brightspace.io.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@link BaseBrightspaceModel} is exercised through real subclasses ({@link RichText}) for the
 * success path, and through a small test-local subclass for the failure/fallback path, since
 * {@link BaseBrightspaceModel} itself has no serializable fields of its own.
 */
public class BaseBrightspaceModelTest {

    @Test
    public void testToJsonSerializesPopulatedFields() {
        RichText richText = RichText.builder()
            .text("plain text")
            .html("<p>html</p>")
            .build();

        String json = richText.toJson();

        assertTrue(json.contains("\"Text\":\"plain text\""), json);
        assertTrue(json.contains("\"Html\":\"<p>html</p>\""), json);
    }

    @Test
    public void testToJsonWithBooleanOverloadSerializesPopulatedFields() {
        RichText richText = RichText.builder()
            .text("plain text")
            .html("<p>html</p>")
            .build();

        String json = richText.toJson(true);

        assertTrue(json.contains("\"Text\":\"plain text\""), json);
        assertTrue(json.contains("\"Html\":\"<p>html</p>\""), json);
    }

    @Test
    public void testToJsonNoArgOverloadDelegatesToFalse() {
        RichText richText = RichText.builder()
            .text("plain text")
            .html(null)
            .build();

        // toJson() is documented as delegating to toJson(false).
        assertEquals(richText.toJson(false), richText.toJson());
    }

    @Test
    public void testToJsonBooleanFlagFalseOmitsNullFields() {
        RichText richText = RichText.builder()
            .text("plain text")
            .html(null)
            .build();

        String json = richText.toJson(false);

        assertTrue(json.contains("\"Text\":\"plain text\""), json);
        assertTrue(!json.contains("Html"), json);
    }

    @Test
    public void testToJsonBooleanFlagTrueIncludesNullFields() {
        RichText richText = RichText.builder()
            .text("plain text")
            .html(null)
            .build();

        String json = richText.toJson(true);

        assertTrue(json.contains("\"Text\":\"plain text\""), json);
        assertTrue(json.contains("\"Html\":null"), json);
    }

    @Test
    public void testToJsonReturnsEmptyObjectStringOnSerializationFailure() {
        FailingBrightspaceModel failingBrightspaceModel = new FailingBrightspaceModel();

        assertEquals("{}", failingBrightspaceModel.toJson());
    }

    /**
     * Minimal concrete subclass whose getter always throws, used to force the
     * {@code catch (JacksonException e)} fallback branch in {@link BaseBrightspaceModel#toJson(boolean)}.
     * Jackson wraps unchecked exceptions thrown from a bean getter in a {@code DatabindException}
     * (a {@code JacksonException}) by default (SerializationFeature.WRAP_EXCEPTIONS is enabled
     * by default), so this reliably exercises the swallowed-exception path.
     */
    private static class FailingBrightspaceModel extends BaseBrightspaceModel {

        @JsonProperty("Broken")
        public String getBroken() {
            throw new IllegalStateException("forced serialization failure");
        }

    }

}
