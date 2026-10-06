package edu.iu.terracotta.security.app.roles;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This endpoint's role check can't be a single annotation - e.g. which role is required depends
 * on the request - so the handler does it in code. Says so explicitly, so ControllerRoleCoverageTest
 * can tell it apart from an endpoint that was never given a role check.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RoleCheckedInHandler {

    /**
     * Why the role is checked in the handler rather than with an annotation.
     */
    String value();

}
