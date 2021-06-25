package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.sql.Timestamp;
import java.util.List;

public class SubmissionDto {

    private Long submissionId;
    private Long participantId;
    private Long assessmentId;
    private Float calculatedGrade;
    private Float alteredCalculatedGrade;
    private Float totalAlteredGrade;
    private Timestamp dateSubmitted;
    private boolean lateSubmission;

    public String getAssessmentLink() {
        return assessmentLink;
    }

    public void setAssessmentLink(String assessmentLink) {
        this.assessmentLink = assessmentLink;
    }

    private String assessmentLink;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<QuestionSubmissionDto> questionSubmissionDtoList;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<SubmissionCommentDto> submissionCommentDtoList;

    public Long getSubmissionId() { return submissionId; }

    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }

    public Long getParticipantId() { return participantId; }

    public void setParticipantId(Long participantId) { this.participantId = participantId; }

    public Long getAssessmentId() { return assessmentId; }

    public void setAssessmentId(Long assessmentId) { this.assessmentId = assessmentId; }

    public Float getCalculatedGrade() { return calculatedGrade; }

    public void setCalculatedGrade(Float calculatedGrade) { this.calculatedGrade = calculatedGrade; }

    public Float getAlteredCalculatedGrade() { return alteredCalculatedGrade; }

    public void setAlteredCalculatedGrade(Float alteredCalculatedGrade) { this.alteredCalculatedGrade = alteredCalculatedGrade; }

    public Float getTotalAlteredGrade() { return totalAlteredGrade; }

    public void setTotalAlteredGrade(Float totalAlteredGrade) { this.totalAlteredGrade = totalAlteredGrade; }

    public Timestamp getDateSubmitted() { return dateSubmitted; }

    public void setDateSubmitted(Timestamp dateSubmitted) { this.dateSubmitted = dateSubmitted; }

    public boolean getLateSubmission() { return lateSubmission; }

    public void setLateSubmission(boolean lateSubmission) { this.lateSubmission = lateSubmission; }

    public List<QuestionSubmissionDto> getQuestionSubmissionDtoList() { return questionSubmissionDtoList; }

    public void setQuestionSubmissionDtoList(List<QuestionSubmissionDto> questionSubmissionDtoList) { this.questionSubmissionDtoList = questionSubmissionDtoList; }

    public List<SubmissionCommentDto> getSubmissionCommentDtoList() { return submissionCommentDtoList; }

    public void setSubmissionCommentDtoList(List<SubmissionCommentDto> submissionCommentDtoList) { this.submissionCommentDtoList = submissionCommentDtoList; }
}
