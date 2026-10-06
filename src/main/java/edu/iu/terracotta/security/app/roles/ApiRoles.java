package edu.iu.terracotta.security.app.roles;

import lombok.experimental.UtilityClass;

/**
 * The Spring Security roles ApiOAuthProviderProcessingFilter grants a request from its API token,
 * matching ApiJwtService's isAdmin / isInstructor / isLearner checks. The role annotations in this
 * package are written in terms of these.
 */
@UtilityClass
public class ApiRoles {

    public static final String ADMIN = "ADMIN";
    public static final String INSTRUCTOR = "INSTRUCTOR";
    public static final String LEARNER = "LEARNER";

}
