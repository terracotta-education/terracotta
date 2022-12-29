package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.sql.Timestamp;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<QuestionSubmissionDto> questionSubmissionDtoList;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<SubmissionCommentDto> submissionCommentDtoList;

}
