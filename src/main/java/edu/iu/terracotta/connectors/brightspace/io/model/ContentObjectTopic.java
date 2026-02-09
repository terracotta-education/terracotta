package edu.iu.terracotta.connectors.brightspace.io.model;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
public class ContentObjectTopic extends BaseBrightspaceModel implements Serializable {

    /*
     {
        "TopicType": <number:TOPIC_T>,
        "Url": <string>|null,
        "StartDate": <string:UTCDateTime>|null,
        "EndDate": <string:UTCDateTime>|null,
        "DueDate": <string:UTCDateTime>|null,
        "IsHidden": <boolean>,
        "IsLocked": <boolean>,
        "IsBroken": <boolean>,  // Added with LE API 1.72
        "OpenAsExternalResource": <boolean>|null,
        "Id": <number:D2LID>,
        "Title": <string>,
        "ShortTitle": <string>,
        "Type": 1,
        "Description": { <composite:RichText> }|null,
        "ParentModuleId": <number:D2LID>,
        "ActivityId": <string>|null,
        "Duration": <number>|null,  // Available in LE's unstable contract
        "IsExempt": <boolean>,
        "ToolId": <number:D2LID>|null,
        "ToolItemId":  <number:D2LID>|null,
        "ActivityType": <number:ACTIVITYTYPE_T>,
        "GradeItemId": <number:D2LID>|null,
        "LastModifiedDate": <string:UTCDateTime>|null,
        "AssociatedGradeItemIds": [<number:D2LID>, ...]
     }
   */

    @JsonProperty("Title") private String title;
    @JsonProperty("ShortTitle") private String shortTitle;
    @JsonProperty("TopicType") private Integer topicType;
    @JsonProperty("Url") private String url;
    @JsonProperty("StartDate") private String startDate;
    @JsonProperty("EndDate") private String endDate;
    @JsonProperty("DueDate") private String dueDate;
    @JsonProperty("IsHidden") private Boolean isHidden;
    @JsonProperty("IsLocked") private Boolean isLocked;
    @JsonProperty("OpenAsExternalResource") private Boolean openAsExternalResource;
    @JsonProperty("Description") private RichText description;
    @JsonProperty("MajorUpdate") private Boolean majorUpdate;
    @JsonProperty("MajorUpdateText") private String majorUpdateText;
    @JsonProperty("ResetCompletionTracking") private Boolean resetCompletionTracking;
    @JsonProperty("Duration") private Integer duration;
    @JsonProperty("IsBroken") private Boolean isBroken;
    @JsonProperty("Id") private Long id;
    @JsonProperty("ParentModuleId") private Long parentModuleId;
    @JsonProperty("ActivityId") private String activityId;
    @JsonProperty("IsExempt") private Boolean isExempt;
    @JsonProperty("ToolId") private Long toolId;
    @JsonProperty("ToolItemId") private Long toolItemId;
    @JsonProperty("ActivityType") private Integer activityType;
    @JsonProperty("GradeItemId") private Long gradeItemId;
    @JsonProperty("LastModifiedDate") private String lastModifiedDate;
    @JsonProperty("AssociatedGradeItemIds") private List<Long> associatedGradeItemIds;

    @Builder.Default
    @JsonProperty("Type")
    private int type = 1;

}
