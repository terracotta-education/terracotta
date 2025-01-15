package edu.iu.terracotta.dao.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class SubmissionDto {

    private Long submissionId;
    private Long participantId;
    private Long assessmentId;
    private Long conditionId;
    private Long treatmentId;
    private Long experimentId;
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<QuestionSubmissionDto> questionSubmissionDtoList;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<SubmissionCommentDto> submissionCommentDtoList;

}
