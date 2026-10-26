package edu.iu.terracotta.security.app.roles;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Only an instructor or an admin may call this endpoint - the same as
 * ApiJwtService.isInstructorOrHigher. Anyone else gets a 401 (see
 * RestResponseEntityExceptionHandler).
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
public @interface InstructorOrHigher {
}
