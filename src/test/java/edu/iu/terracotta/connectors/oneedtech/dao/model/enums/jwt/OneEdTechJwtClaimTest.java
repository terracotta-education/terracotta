package edu.iu.terracotta.connectors.oneedtech.dao.model.enums.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class OneEdTechJwtClaimTest {

    @Test
    public void testKeyIndex0ForAllowedAttemptsReturnsNull() {
        assertNull(OneEdTechJwtClaim.ALLOWED_ATTEMPTS.key(0));
    }

    @Test
    public void testKeyIndex1ForAllowedAttemptsReturnsUnderscoreKey() {
        assertEquals("allowed_attempts", OneEdTechJwtClaim.ALLOWED_ATTEMPTS.key(1));
    }

    @Test
    public void testKeyIndex0ForOneEdTechReturnsKey() {
        assertEquals("ONE_ED_TECH", OneEdTechJwtClaim.ONE_ED_TECH.key(0));
    }

    @Test
    public void testKeyIndex1ForOneEdTechReturnsUnderscoreKey() {
        assertEquals("one_ed_tech", OneEdTechJwtClaim.ONE_ED_TECH.key(1));
    }

    @Test
    public void testKeyIndex0ForUserIdReturnsKey() {
        assertEquals("userId", OneEdTechJwtClaim.USER_ID.key(0));
    }

    @Test
    public void testKeyIndex1ForUserIdReturnsUnderscoreKey() {
        assertEquals("user_id", OneEdTechJwtClaim.USER_ID.key(1));
    }

    @Test
    public void testKeyWithNegativeIndexThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> OneEdTechJwtClaim.USER_ID.key(-1)
        );

        assertEquals("Invalid index: [-1]", exception.getMessage());
    }

    @Test
    public void testKeyWithIndexTwoThrowsIllegalArgumentException() {
        // Only indices 0 and 1 are handled by the switch statement; any other value,
        // including 2, falls through to the default branch.
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> OneEdTechJwtClaim.USER_ID.key(2)
        );

        assertEquals("Invalid index: [2]", exception.getMessage());
    }

    @Test
    public void testNoArgKeyMethodReturnsKeyField() {
        assertEquals("userId", OneEdTechJwtClaim.USER_ID.key());
        assertNull(OneEdTechJwtClaim.ALLOWED_ATTEMPTS.key());
    }

}
