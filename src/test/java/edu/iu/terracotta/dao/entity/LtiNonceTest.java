package edu.iu.terracotta.dao.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class LtiNonceTest {

    @Test
    public void testBuilderAndGettersSetters() {
        LtiNonce ltiNonce = LtiNonce.builder()
            .id(1L)
            .nonce("nonce-value")
            .build();

        assertEquals(1L, ltiNonce.getId());
        assertEquals("nonce-value", ltiNonce.getNonce());

        ltiNonce.setId(2L);
        ltiNonce.setNonce("nonce-value-2");

        assertEquals(2L, ltiNonce.getId());
        assertEquals("nonce-value-2", ltiNonce.getNonce());
    }

    @Test
    public void testConvenienceConstructorSetsNonce() {
        LtiNonce ltiNonce = new LtiNonce("nonce-abc");

        assertEquals("nonce-abc", ltiNonce.getNonce());
        assertNull(ltiNonce.getId());
    }

    @Test
    public void testConvenienceConstructorThrowsForBlankNonce() {
        assertThrows(AssertionError.class, () -> new LtiNonce(""));
        assertThrows(AssertionError.class, () -> new LtiNonce("   "));
        assertThrows(AssertionError.class, () -> new LtiNonce(null));
    }

    @Test
    public void testNoArgsConstructor() {
        LtiNonce ltiNonce = new LtiNonce();

        assertNull(ltiNonce.getNonce());
    }

}
