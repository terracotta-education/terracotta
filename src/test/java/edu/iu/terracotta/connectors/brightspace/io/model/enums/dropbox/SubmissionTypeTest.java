package edu.iu.terracotta.connectors.brightspace.io.model.enums.dropbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class SubmissionTypeTest {

    @Test
    public void testFromKeyReturnsMatchingConstant() {
        for (SubmissionType type : SubmissionType.values()) {
            assertEquals(type, SubmissionType.fromKey(type.getKey()));
        }
    }

    @Test
    public void testFromKeyThrowsForUnknownKey() {
        int unknownKey = -999;

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> SubmissionType.fromKey(unknownKey)
        );

        assertEquals(String.format("Unknown SubmissionType key: [%s]", unknownKey), exception.getMessage());
    }

}
