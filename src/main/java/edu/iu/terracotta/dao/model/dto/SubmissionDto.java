package edu.iu.terracotta.dao.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmissionDto {

    private Long submissionId;
    private UUID participantId;
    private Long assessmentId;
    private UUID conditionId;
    private Long treatmentId;
    private UUID experimentId;
    private Float calculatedGrade;
    private Float alteredCalculatedGrade;
    private Float totalAlteredGrade;
    private Timestamp dateCreated;
    private Timestamp dateSubmitted;
    private boolean lateSubmission;
    private String assessmentLink;
    private boolean gradeOverridden;
    private String integrationLaunchUrl;
    private boolean integrationFeedbackEnabled;
    private Long integrationTokenExpirationDate;
    private Long integrationTokenWarningPeriod;
    private Long integrationTokenExpirationCheckInterval;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<QuestionSubmissionDto> questionSubmissionDtoList;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<SubmissionCommentDto> submissionCommentDtoList;

}
