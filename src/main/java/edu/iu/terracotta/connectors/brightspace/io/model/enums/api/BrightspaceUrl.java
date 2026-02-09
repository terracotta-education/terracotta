package edu.iu.terracotta.connectors.brightspace.io.model.enums.api;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BrightspaceUrl {

    API_ROOT(
        "%s/d2l/api/%s",
        List.of(),
        List.of()
    ),
    CONTENT_MODULE(
        "le/%s/%s/content/modules/%s",
        List.of(),
        List.of(
            BrightspaceScope.CONTENT_MODULES_MANAGE.scope()
        )
    ),
    CONTENT_MODULE_ROOT(
        "le/%s/%s/content/root/",
        List.of(),
        List.of(
            BrightspaceScope.CONTENT_MODULES_READONLY.scope()
        )
    ),
    CONTENT_MODULE_STRUCTURE(
        "le/%s/%s/content/modules/%s/structure/",
        List.of(),
        List.of(
            BrightspaceScope.CONTENT_MODULES_MANAGE.scope()
        )
    ),
    CONTENT_TOPIC(
        "le/%s/%s/content/topics/%s",
        List.of(),
        List.of(
            BrightspaceScope.CONTENT_TOPICS_MANAGE.scope(),
            BrightspaceScope.CONTENT_TOPICS_READONLY.scope()
        )
    ),
    DROPBOX_FOLDER(
        "le/%s/%s/dropbox/folders/%s",
        List.of(),
        List.of(
            BrightspaceScope.DROPBOX_FOLDERS_DELETE.scope(),
            BrightspaceScope.DROPBOX_FOLDERS_READ.scope(),
            BrightspaceScope.DROPBOX_FOLDERS_UPDATE.scope()
        )
    ),
    DROPBOX_FOLDERS_ROOT(
        "le/%s/%s/dropbox/folders/",
        List.of(),
        List.of(
            BrightspaceScope.DROPBOX_FOLDERS_READ.scope(),
            BrightspaceScope.DROPBOX_FOLDERS_WRITE.scope()
        )
    ),
    ENROLLMENTS(
        "le/%s/%s/classlist/paged/",
        List.of(
            BrightspaceQueryParam.ONLY_SHOW_SHOWN_IN_GRADES.key(),
            BrightspaceQueryParam.ROLE_ID.key(),
            BrightspaceQueryParam.SEARCH_TERM.key()
        ),
        List.of(
            BrightspaceScope.ENROLLMENT_READ.scope()
        )
    ),
    GRADE_OBJECT(
        "le/%s/%s/grades/%s",
        List.of(),
        List.of(
            BrightspaceScope.GRADES_GRADEOBJECTS_DELETE.scope(),
            BrightspaceScope.GRADES_GRADEOBJECTS_READ.scope(),
            BrightspaceScope.GRADES_GRADEOBJECTS_WRITE.scope()
        )
    ),
    GRADE_OBJECTS_ROOT(
        "le/%s/%s/grades/",
        List.of(),
        List.of(
            BrightspaceScope.GRADES_GRADEOBJECTS_READ.scope()
        )
    ),
    GRADE_VALUES(
        "le/%s/%s/grades/%s/values/",
        List.of(),
        List.of(
            BrightspaceScope.GRADES_GRADEVALUES_READ.scope()
        )
    ),
    LTI_ADVANTAGE_LINK(
        "le/%s/ltiadvantage/links/orgunit/%s/%s",
        List.of(),
        List.of(
            BrightspaceScope.LTI_ADVANTAGE_LINK_DELETE.scope(),
            BrightspaceScope.LTI_ADVANTAGE_LINK_READ.scope()
        )
    ),
    LTI_ADVANTAGE_LINK_ROOT(
        "le/%s/ltiadvantage/links/orgunit/%s/",
        List.of(),
        List.of(
            BrightspaceScope.LTI_ADVANTAGE_LINK_CREATE.scope(),
            BrightspaceScope.LTI_ADVANTAGE_LINK_READ.scope()
        )
    ),
    LTI_ADVANTAGE_QUICKLINK_ROOT(
        "le/%s/ltiadvantage/quicklinks/orgunit/%s/link/%s",
        List.of(),
        List.of(
            BrightspaceScope.LTI_ADVANTAGE_QUICKLINK_CREATE.scope()
        )
    ),
    LTI_ASSIGNMENT_LAUNCH(
        "%s/lti3?experiment=%s&assignment=%s",
        List.of(),
        List.of()
    ),
    LTI_CONSENT_ASSIGNMENT_LAUNCH(
        "%s/lti3?consent=true&experiment=%s",
        List.of(),
        List.of()
    ),
    WHOAMI(
        "%s/d2l/api/lp/%s/users/whoami",
        List.of(),
        List.of(
            BrightspaceScope.CORE.scope()
        )
    );

    private String url;
    private List<String> queryParams;
    private List<String> scopes;

    public String url() {
        return url;
    }

    public List<String> scopes() {
        return scopes;
    }

}
