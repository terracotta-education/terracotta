package edu.iu.terracotta.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;

public class LmsAuthorizationUtilsTest {

    // the real shape of a rejected refresh token: LmsOAuthException sits in the middle of the chain,
    // wrapping the HTTP failure, so it is not the deepest cause
    @Test
    public void testFindsAnLmsOAuthExceptionAnywhereInTheCauseChain() {
        Exception failure = new ApiException(
            "Failed to get the list of assignments",
            new IllegalStateException("Failed to refresh", new LmsOAuthException("invalid_grant", new RuntimeException("400 Bad Request")))
        );

        assertTrue(LmsAuthorizationUtils.isAuthorizationFailure(failure));
    }

    @Test
    public void testFindsAnLmsOAuthExceptionThrownDirectly() {
        assertTrue(LmsAuthorizationUtils.isAuthorizationFailure(new LmsOAuthException("no token")));
    }

    @Test
    public void testOtherFailuresAreNotAuthorizationFailures() {
        assertFalse(LmsAuthorizationUtils.isAuthorizationFailure(new ApiException("Canvas returned 503", new RuntimeException("503"))));
        assertFalse(LmsAuthorizationUtils.isAuthorizationFailure(null));
    }

}
