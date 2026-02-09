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
public class DropboxFolder extends BaseBrightspaceModel implements Serializable {

    /*
     {
        "Id": <number:D2LID>,
        "CategoryId": <number:D2LID>|null,
        "Name": <string>,
        "CustomInstructions": "{composite:RichText}",
        "Attachments": [  // Array of File blocks
            {
                "FileId": <number:D2LID>,
                "FileName": <string>,
                "Size": <number:long>
            },
            { <composite:File> }, ...
        ],
        "TotalFiles": <number>,
        "UnreadFiles": <number>,
        "FlaggedFiles": <number>,
        "TotalUsers": <number>,
        "TotalUsersWithSubmissions": <number>,
        "TotalUsersWithFeedback": <number>,
        "Availability": null|{
            "StartDate": <string:UTCDateTime>|null,
            "EndDate": <string:UTCDateTime>|null,
            "StartDateAvailabilityType": <string:AVAILABILITY_T>|null,
            "EndDateAvailabilityType": <string:AVAILABILITY_T>|null
        },
        "GroupTypeId": <number:D2LID>|null,
        "DueDate": <string:UTCDateTime>|null,
        "DisplayInCalendar": <boolean>,
        "Assessment": {
            "ScoreDenominator": <number:decimal>|null,
            "Rubrics": [  // Array of Rubric blocks
                { <composite:Rubric.Rubric> },
                { <composite:Rubric.Rubric> }, ...
            ]
        },
        "NotificationEmail": <string>|null,
        "IsHidden": <boolean>,
        "LinkAttachments": [  // Array of Link blocks
            {
                "LinkId": <number:D2LID>,
                "LinkName": <string>,
                "Href": <string>
            },
            { <composite:Link> }, ...
        ],
        "ActivityId": <string>|null,
        "IsAnonymous": <boolean>,
        "DropboxType": <string:DROPBOXTYPE_T>,
        "SubmissionType": <string:SUBMISSIONTYPE_T>,
        "CompletionType": <string:DROPBOX_COMPLETIONTYPE_T>,
        "GradeItemId": <number:D2LID>|null,
        "AllowOnlyUsersWithSpecialAccess": <boolean>|null
     }
     */

    @JsonProperty("Id") private Long id;
    @JsonProperty("CategoryId") private Long categoryId;
    @JsonProperty("Name") private String name;
    @JsonProperty("CustomInstructions") private RichText customInstructions;
    @JsonProperty("Attachments") private List<File> attachments;
    @JsonProperty("TotalFiles") private Integer totalFiles;
    @JsonProperty("UnreadFiles") private Integer unreadFiles;
    @JsonProperty("FlaggedFiles") private Integer flaggedFiles;
    @JsonProperty("TotalUsers") private Integer totalUsers;
    @JsonProperty("TotalUsersWithSubmissions") private Integer totalUsersWithSubmissions;
    @JsonProperty("TotalUsersWithFeedback") private Integer totalUsersWithFeedback;
    @JsonProperty("Availability") private Availability availability;
    @JsonProperty("GroupTypeId") private Long groupTypeId;
    @JsonProperty("DueDate") private String dueDate;
    @JsonProperty("DisplayInCalendar") private Boolean displayInCalendar;
    @JsonProperty("Assessment") private Assessment assessment;
    @JsonProperty("NotificationEmail") private String notificationEmail;
    @JsonProperty("IsHidden") private Boolean isHidden;
    @JsonProperty("LinkAttachments") private List<Link> linkAttachments;
    @JsonProperty("ActivityId") private String activityId;
    @JsonProperty("IsAnonymous") private Boolean isAnonymous;
    @JsonProperty("DropboxType") private Integer dropboxType;
    @JsonProperty("SubmissionType") private Integer submissionType;
    @JsonProperty("CompletionType") private Integer completionType;
    @JsonProperty("GradeItemId") private Long gradeItemId;
    @JsonProperty("AllowOnlyUsersWithSpecialAccess") private Boolean allowOnlyUsersWithSpecialAccess;

}
