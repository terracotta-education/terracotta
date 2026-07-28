package edu.iu.terracotta.connectors.brightspace.io.model.enums.dropbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class DropboxTypeTest {

    @Test
    public void testFromKeyReturnsMatchingConstant() {
        for (DropboxType type : DropboxType.values()) {
            assertEquals(type, DropboxType.fromKey(type.getKey()));
        }
    }

    @Test
    public void testFromKeyThrowsForUnknownKey() {
        int unknownKey = -999;

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> DropboxType.fromKey(unknownKey)
        );

        assertEquals(String.format("Unknown DropboxType key: [%s]", unknownKey), exception.getMessage());
    }

}
