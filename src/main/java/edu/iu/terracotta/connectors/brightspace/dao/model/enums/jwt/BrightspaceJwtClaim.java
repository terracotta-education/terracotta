package edu.iu.terracotta.connectors.brightspace.dao.model.enums.jwt;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum BrightspaceJwtClaim {

    BRIGHTSPACE("BRIGHTSPACE"),
    BRIGHTSPACE_ASSIGNMENT_ID("brightspace_assignment_id"),
    BRIGHTSPACE_COURSE_ID("brightspaceCourseId"),
    BRIGHTSPACE_LOGIN_ID("brightspaceLoginId"),
    BRIGHTSPACE_USER_GLOBAL_ID("brightspaceUserGlobalId"),
    BRIGHTSPACE_USER_ID("brightspaceUserId"),
    BRIGHTSPACE_USER_NAME("brightspaceUserName");

    private final String key;

    public String key() {
        return key;
    }

}
