package edu.iu.terracotta.connectors.brightspace.io.model.enums.dropbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class CompletionTypeTest {

    @Test
    public void testFromKeyReturnsMatchingConstant() {
        for (CompletionType type : CompletionType.values()) {
            assertEquals(type, CompletionType.fromKey(type.getKey()));
        }
    }

    @Test
    public void testFromKeyThrowsForUnknownKey() {
        int unknownKey = -999;

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> CompletionType.fromKey(unknownKey)
        );

        assertEquals(String.format("Unknown CompletionType key: [%s]", unknownKey), exception.getMessage());
    }

}
