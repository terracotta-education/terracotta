package edu.iu.terracotta.connectors.brightspace.io.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrightspaceAssignmentMetadata {

    public static final String KEY = "brightspace";
    public static final String LTI_ASSIGNMENT_ID = "lti_assignment_id";

    private Long contentModuleId;
    private Long contentTopicId;
    private Long dropboxFolderId;
    private String dueAt;
    private String lockAt;
    private Long gradeObjectId;
    private Long ltiAdvantageLinkId;
    private Long ltiAdvantageQuickLinkId;
    private String unlockAt;

}
