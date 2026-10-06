package edu.iu.terracotta.security.app.roles;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * A learner, instructor or admin may call this endpoint - the same as
 * ApiJwtService.isLearnerOrHigher. Anyone else gets a 401 (see
 * RestResponseEntityExceptionHandler).
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAnyRole('LEARNER', 'INSTRUCTOR', 'ADMIN')")
public @interface LearnerOrHigher {
}
