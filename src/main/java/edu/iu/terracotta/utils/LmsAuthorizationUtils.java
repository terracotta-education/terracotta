package edu.iu.terracotta.utils;

import org.apache.commons.lang3.exception.ExceptionUtils;

import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;

public final class LmsAuthorizationUtils {

    private LmsAuthorizationUtils() { }

    /**
     * Whether a failure was caused by the LMS refusing (or Terracotta not holding) a user's API
     * token, which only that user re-authorizing on a launch can fix. Searches the whole cause
     * chain: LmsOAuthException wraps the underlying HTTP failure as its own cause (see
     * CanvasLmsOAuthServiceImpl#postToTokenURL), so it is rarely the deepest cause.
     */
    public static boolean isAuthorizationFailure(Throwable throwable) {
        return ExceptionUtils.throwableOfType(throwable, LmsOAuthException.class) != null;
    }

}
