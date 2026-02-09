package edu.iu.terracotta.connectors.brightspace.io.model;

import java.io.Serializable;

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
public class ContentObjectTopicUpdate extends BaseBrightspaceModel implements Serializable {

    /*
     {
        "Title": <string>,
        "ShortTitle": <string>,
        "Type": 1,
        "TopicType": <number:TOPIC_T>,
        "Url": <string>,
        "StartDate": <string:UTCDateTime>|null,
        "EndDate": <string:UTCDateTime>|null,
        "DueDate": <string:UTCDateTime>|null,
        "IsHidden": <boolean>,
        "IsLocked": <boolean>,
        "OpenAsExternalResource": <boolean>|null,
        "Description": { <composite:RichTextInput> }|null,
        "MajorUpdate": <boolean>|null,
        "MajorUpdateText": <string>,
        "ResetCompletionTracking": <boolean>|null,
        "Duration": <number>|null
     }
     */

    @JsonProperty("Title") private String title;
    @JsonProperty("ShortTitle") private String shortTitle;
    @JsonProperty("Url") private String url;
    @JsonProperty("StartDate") private String startDate;
    @JsonProperty("EndDate") private String endDate;
    @JsonProperty("DueDate") private String dueDate;
    @JsonProperty("IsHidden") private Boolean isHidden;
    @JsonProperty("IsLocked") private Boolean isLocked;
    @JsonProperty("OpenAsExternalResource") private Boolean openAsExternalResource;
    @JsonProperty("Description") private RichTextInput description;
    @JsonProperty("MajorUpdate") private Boolean majorUpdate;
    @JsonProperty("MajorUpdateText") private String majorUpdateText;
    @JsonProperty("ResetCompletionTracking") private Boolean resetCompletionTracking;
    @JsonProperty("Duration") private Integer duration;
    @JsonProperty("IsBroken") private Boolean isBroken;
    //@JsonProperty("GradeItemId") private Long gradeItemId;

    @Builder.Default
    @JsonProperty("Type")
    private int type = 1;

    @Builder.Default
    @JsonProperty("TopicType")
    private int topicType = 1;

    public static ContentObjectTopicUpdate from(ContentObjectTopic contentObjectTopic) {
        if (contentObjectTopic == null) {
            return ContentObjectTopicUpdate.builder().build();
        }

        return ContentObjectTopicUpdate.builder()
            .description(RichTextInput.from(contentObjectTopic.getDescription()))
            .duration(contentObjectTopic.getDuration())
            .dueDate(contentObjectTopic.getDueDate())
            .endDate(contentObjectTopic.getEndDate())
            .isHidden(contentObjectTopic.getIsHidden())
            .isLocked(contentObjectTopic.getIsLocked())
            .majorUpdate(contentObjectTopic.getMajorUpdate())
            .majorUpdateText(contentObjectTopic.getMajorUpdateText())
            .openAsExternalResource(contentObjectTopic.getOpenAsExternalResource())
            .resetCompletionTracking(contentObjectTopic.getResetCompletionTracking())
            .shortTitle(contentObjectTopic.getShortTitle())
            .startDate(contentObjectTopic.getStartDate())
            .title(contentObjectTopic.getTitle())
            .type(contentObjectTopic.getType())
            .topicType(contentObjectTopic.getTopicType())
            .url(contentObjectTopic.getUrl())
            .build();
    }

}
