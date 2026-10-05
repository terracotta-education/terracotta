package edu.iu.terracotta.dao.model.enums.integrations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class IntegrationTokenTypeTest {

    @Test
    public void testValuesContainsExpectedConstants() {
        IntegrationTokenType[] values = IntegrationTokenType.values();

        assertEquals(2, values.length);
        assertEquals(IntegrationTokenType.PREVIEW, values[0]);
        assertEquals(IntegrationTokenType.STANDARD, values[1]);
    }

    @Test
    public void testValueOfReturnsMatchingConstant() {
        assertEquals(IntegrationTokenType.PREVIEW, IntegrationTokenType.valueOf("PREVIEW"));
        assertEquals(IntegrationTokenType.STANDARD, IntegrationTokenType.valueOf("STANDARD"));
    }

    @Test
    public void testNameMatchesDeclaredConstant() {
        assertNotNull(IntegrationTokenType.PREVIEW.name());
        assertEquals("STANDARD", IntegrationTokenType.STANDARD.name());
    }

}
