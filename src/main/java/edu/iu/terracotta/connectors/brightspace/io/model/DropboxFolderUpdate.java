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
public class DropboxFolderUpdate extends BaseBrightspaceModel implements Serializable {

    /*
     {
        "CategoryId": <number:D2LID>|null,
        "Name": <string>,
        "CustomInstructions": "{composite:RichTextInput}",
        "Availability": null|{
            "StartDate": <string:UTCDateTime>|null,
            "EndDate": <string:UTCDateTime>|null,
            "StartDateAvailabilityType": <string:AVAILABILITY_T>|null,
            "EndDateAvailabilityType": <string:AVAILABILITY_T>|null
        },
        "GroupTypeId": <number:D2LID>|null,
        "DueDate": <string:UTCDateTime>|null,
        "DisplayInCalendar": <boolean>,
        "NotificationEmail" : <string>|null,
        "IsHidden": <boolean>|null,
        "Assessment": null|{
            "ScoreDenominator": <number:decimal>|null
        },
        "IsAnonymous": <boolean>|null,
        "DropboxType": <string:DROPBOXTYPE_T>|null,
        "SubmissionType": <string:SUBMISSIONTYPE_T>|null,
        "CompletionType": <string:DROPBOX_COMPLETIONTYPE_T>|null,
        "GradeItemId": <number:D2LID>|null,
        "AllowOnlyUsersWithSpecialAccess": <boolean>|null
     }
     */

    @JsonProperty("CategoryId") private Long categoryId;
    @JsonProperty("Name") private String name;
    @JsonProperty("CustomInstructions") private RichTextInput customInstructions;
    @JsonProperty("Availability") private Availability availability;
    @JsonProperty("GroupTypeId") private Long groupTypeId;
    @JsonProperty("DueDate") private String dueDate;
    @JsonProperty("DisplayInCalendar") private Boolean displayInCalendar;
    @JsonProperty("NotificationEmail") private String notificationEmail;
    @JsonProperty("IsHidden") private Boolean isHidden;
    @JsonProperty("Assessment") private Assessment assessment;
    @JsonProperty("IsAnonymous") private Boolean isAnonymous;
    @JsonProperty("DropboxType") private Integer dropboxType;
    @JsonProperty("SubmissionType") private Integer submissionType;
    @JsonProperty("CompletionType") private Integer completionType;
    @JsonProperty("GradeItemId") private Long gradeItemId;
    @JsonProperty("AllowOnlyUsersWithSpecialAccess") private Boolean allowOnlyUsersWithSpecialAccess;

    public static DropboxFolderUpdate from(DropboxFolder dropboxFolder) {
        if (dropboxFolder == null) {
            return DropboxFolderUpdate.builder().build();
        }

        return DropboxFolderUpdate.builder()
            .allowOnlyUsersWithSpecialAccess(dropboxFolder.getAllowOnlyUsersWithSpecialAccess())
            .assessment(dropboxFolder.getAssessment())
            .availability(dropboxFolder.getAvailability())
            .categoryId(dropboxFolder.getCategoryId())
            .completionType(dropboxFolder.getCompletionType())
            .customInstructions(RichTextInput.from(dropboxFolder.getCustomInstructions()))
            .displayInCalendar(dropboxFolder.getDisplayInCalendar())
            .dropboxType(dropboxFolder.getDropboxType())
            .dueDate(dropboxFolder.getDueDate())
            .gradeItemId(dropboxFolder.getGradeItemId())
            .groupTypeId(dropboxFolder.getGroupTypeId())
            .isAnonymous(dropboxFolder.getIsAnonymous())
            .isHidden(dropboxFolder.getIsHidden())
            .name(dropboxFolder.getName())
            .notificationEmail(dropboxFolder.getNotificationEmail())
            .submissionType(dropboxFolder.getSubmissionType())
            .build();
    }

}
