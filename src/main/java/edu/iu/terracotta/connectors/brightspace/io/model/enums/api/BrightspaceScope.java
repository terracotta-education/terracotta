package edu.iu.terracotta.connectors.brightspace.io.model.enums.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BrightspaceScope {

    CONTENT_MODULES_MANAGE("content:modules:manage"),
    CONTENT_MODULES_READONLY("content:modules:readonly"),
    CONTENT_TOPICS_MANAGE("content:topics:manage"),
    CONTENT_TOPICS_READONLY("content:topics:readonly"),
    CORE("core:*:*"),
    DROPBOX_FOLDERS_DELETE("dropbox:folders:delete"),
    DROPBOX_FOLDERS_READ("dropbox:folders:read"),
    DROPBOX_FOLDERS_UPDATE("dropbox:folders:update"),
    DROPBOX_FOLDERS_WRITE("dropbox:folders:write"),
    ENROLLMENT_READ("enrollment:orgunit:read"),
    GRADES_GRADEOBJECTS_DELETE("grades:gradeobjects:delete"),
    GRADES_GRADEOBJECTS_READ("grades:gradeobjects:read"),
    GRADES_GRADEOBJECTS_WRITE("grades:gradeobjects:write"),
    GRADES_GRADEVALUES_READ("grades:gradevalues:read"),
    LTI_ADVANTAGE_LINK_CREATE("ltiadvantage:links:create"),
    LTI_ADVANTAGE_LINK_DELETE("ltiadvantage:links:delete"),
    LTI_ADVANTAGE_LINK_READ("ltiadvantage:links:read"),
    LTI_ADVANTAGE_QUICKLINK_CREATE("ltiadvantage:quicklinks:create");

    private final String scope;

    public String scope() {
        return scope;
    }

}
