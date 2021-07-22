package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentDto {

    private Long assessmentId;
    private String html;
    private Long treatmentId;
    private String title;
    private boolean autoSubmit;
    private Integer numOfSubmissions;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<SubmissionDto> submissions;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<QuestionDto> questions;
    private boolean started;
    private Long submissionsExpected;
    private Long submissionsCompletedCount;
    private Long submissionsInProgressCount;


    public Long getAssessmentId() { return assessmentId; }

    public void setAssessmentId(Long assessmentId) { this.assessmentId = assessmentId; }

    public String getHtml() { return html; }

    public void setHtml(String html) { this.html = html; }

    public List<QuestionDto> getQuestions() { return questions; }

    public void setQuestions(List<QuestionDto> questions) { this.questions = questions; }

    public Long getTreatmentId() { return treatmentId; }

    public void setTreatmentId(Long treatmentId) { this.treatmentId = treatmentId; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public boolean getAutoSubmit() { return autoSubmit; }

    public void setAutoSubmit(boolean autoSubmit) { this.autoSubmit = autoSubmit; }

    public Integer getNumOfSubmissions() { return numOfSubmissions; }

    public void setNumOfSubmissions(Integer numOfSubmissions) { this.numOfSubmissions = numOfSubmissions; }

    public List<SubmissionDto> getSubmissions() { return submissions; }

    public void setSubmissions(List<SubmissionDto> submissions) { this.submissions = submissions; }

    public boolean getStarted() { return started; }

    public void setStarted(boolean started) { this.started = started; }

    public Long getSubmissionsExpected() {
        return submissionsExpected;
    }

    public void setSubmissionsExpected(Long submissionsExpected) {
        this.submissionsExpected = submissionsExpected;
    }

    public Long getSubmissionsCompletedCount() {
        return submissionsCompletedCount;
    }

    public void setSubmissionsCompletedCount(Long submissionsCompletedCount) {
        this.submissionsCompletedCount = submissionsCompletedCount;
    }

    public Long getSubmissionsInProgressCount() {
        return submissionsInProgressCount;
    }

    public void setSubmissionsInProgressCount(Long submissionsInProgressCount) {
        this.submissionsInProgressCount = submissionsInProgressCount;
    }
}
