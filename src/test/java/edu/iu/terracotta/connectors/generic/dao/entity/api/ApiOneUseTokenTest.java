package edu.iu.terracotta.connectors.generic.dao.entity.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests the hand-written, validating constructor of {@link ApiOneUseToken}:
 * {@code ApiOneUseToken(String token)}, which rejects blank tokens (per
 * {@link org.apache.commons.lang3.StringUtils#isBlank(CharSequence)}, which also treats
 * {@code null} as blank) with an {@link AssertionError}.
 */
public class ApiOneUseTokenTest {

    @Test
    public void testConstructorWithValidTokenSetsToken() {
        ApiOneUseToken apiOneUseToken = new ApiOneUseToken("my-token");

        assertEquals("my-token", apiOneUseToken.getToken());
    }

    @Test
    public void testConstructorBlankTokenThrowsAssertionError() {
        assertThrows(AssertionError.class, () -> new ApiOneUseToken(""));
        assertThrows(AssertionError.class, () -> new ApiOneUseToken("   "));
    }

    @Test
    public void testConstructorNullTokenThrowsAssertionError() {
        assertThrows(AssertionError.class, () -> new ApiOneUseToken(null));
    }

}
