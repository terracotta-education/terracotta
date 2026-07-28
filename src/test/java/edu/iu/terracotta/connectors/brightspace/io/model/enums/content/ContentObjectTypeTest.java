package edu.iu.terracotta.connectors.brightspace.io.model.enums.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ContentObjectTypeTest {

    @Test
    public void testFromKeyReturnsMatchingConstant() {
        for (ContentObjectType type : ContentObjectType.values()) {
            assertEquals(type, ContentObjectType.fromKey(type.getKey()));
        }
    }

    @Test
    public void testFromKeyThrowsForUnknownKey() {
        int unknownKey = -999;

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ContentObjectType.fromKey(unknownKey)
        );

        assertEquals(String.format("Unknown ContentObjectType key: [%s]", unknownKey), exception.getMessage());
    }

}
