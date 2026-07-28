package edu.iu.terracotta.connectors.brightspace.io.model.enums.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class TopicTypeTest {

    @Test
    public void testFromKeyReturnsMatchingConstant() {
        for (TopicType type : TopicType.values()) {
            assertEquals(type, TopicType.fromKey(type.getKey()));
        }
    }

    @Test
    public void testFromKeyThrowsForUnknownKey() {
        int unknownKey = -999;

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> TopicType.fromKey(unknownKey)
        );

        assertEquals(String.format("Unknown TopicType key: [%s]", unknownKey), exception.getMessage());
    }

}
