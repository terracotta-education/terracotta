package edu.iu.terracotta.connectors.brightspace.io.model.enums.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BrightspaceQueryParam {

    IS_ACTIVE("isActive"),
    IS_GRADED("isGraded"),
    ONLY_SHOW_SHOWN_IN_GRADES("onlyShowShownInGrades"),
    PAGE("page"),
    PAGE_SIZE("pageSize"),
    ROLE_ID("roleId"),
    SEARCH_TERM("searchTerm"),
    SEARCH_TEXT("searchText"),
    SORT("sort");

    private final String key;

    public String key() {
        return key;
    }

}
