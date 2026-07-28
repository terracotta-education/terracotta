package edu.iu.terracotta.connectors.brightspace.io.model.enums.ltiadvantage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class LinkTypeTest {

    @Test
    public void testFromKeyReturnsMatchingConstant() {
        for (LinkType type : LinkType.values()) {
            assertEquals(type, LinkType.fromKey(type.getKey()));
        }
    }

    @Test
    public void testFromKeyThrowsForUnknownKey() {
        int unknownKey = -999;

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> LinkType.fromKey(unknownKey)
        );

        assertEquals(String.format("Unknown LinkType key: [%s]", unknownKey), exception.getMessage());
    }

}
