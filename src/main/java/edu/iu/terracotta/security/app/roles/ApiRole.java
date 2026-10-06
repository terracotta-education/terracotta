package edu.iu.terracotta.security.app.roles;

import java.util.function.BiPredicate;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;

/**
 * The Spring Security roles ApiOAuthProviderProcessingFilter grants a request from its API token,
 * each decided by the ApiJwtService check the controllers used to call themselves. The role
 * annotations in this package check these by name ('ADMIN', 'INSTRUCTOR', 'LEARNER') - renaming
 * one here means updating those too.
 */
public enum ApiRole {

    ADMIN(ApiJwtService::isAdmin),
    INSTRUCTOR(ApiJwtService::isInstructor),
    LEARNER(ApiJwtService::isLearner);

    private static final String AUTHORITY_PREFIX = "ROLE_";

    private final BiPredicate<ApiJwtService, SecuredInfo> granted;

    ApiRole(BiPredicate<ApiJwtService, SecuredInfo> granted) {
        this.granted = granted;
    }

    public boolean isGrantedTo(SecuredInfo securedInfo, ApiJwtService apiJwtService) {
        return granted.test(apiJwtService, securedInfo);
    }

    public GrantedAuthority authority() {
        return new SimpleGrantedAuthority(AUTHORITY_PREFIX + name());
    }

}
