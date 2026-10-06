package edu.iu.terracotta.security.app.roles;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Any caller with a valid API token may call this endpoint, whatever their role. Says so
 * explicitly, so ControllerRoleCoverageTest can tell it apart from an endpoint that was never
 * given a role check.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoRoleRequired {

    /**
     * Why no role is required.
     */
    String value();

}
