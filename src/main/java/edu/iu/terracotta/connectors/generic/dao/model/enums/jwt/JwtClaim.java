package edu.iu.terracotta.connectors.generic.dao.model.enums.jwt;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum JwtClaim {

    ALLOWED_ATTEMPTS("allowedAttempts"),
    ASSIGNMENT("assignment"),
    ASSIGNMENT_ID("assignmentId"),
    CONSENT("consent"),
    CONTEXT_ID("contextId"),
    DUE_AT("due_at"),
    EXPERIMENT("experiment"),
    EXPERIMENT_ID("experimentId"),
    FILE_ID("fileId"),
    ISSUER_LMS_OAUTH_API_TOKEN_REQUEST("lmsOAuthAPITokenRequest"),
    JWT_BEARER_TYPE("Bearer"),
    JWT_REQUEST_HEADER_NAME("Authorization"),
    LMS_ASSIGNMENT_ID("lmsAssignmentId"),
    LMS_NAME("lms_name"),
    LOCK_AT("lock_at"),
    NONCE("nonce"),
    NO_USER("no_user"),
    ONE_USE("oneUse"),
    PLATFORM_DEPLOYMENT_ID("platformDeploymentId"),
    QUERY_PARAM_NAME("token"),
    ROLES("roles"),
    STUDENT_ATTEMPTS("studentAttempts"),
    TERRACOTTA("TERRACOTTA"),
    UNLOCK_AT("unlock_at"),
    USER_ID("userId");

    private final String key;

    public String key() {
        return key;
    }

}
