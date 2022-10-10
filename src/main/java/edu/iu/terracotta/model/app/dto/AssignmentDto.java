package edu.iu.terracotta.model.app.dto;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.model.app.enumerator.MultipleSubmissionScoringScheme;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignmentDto {

    private Long assignmentId;
    private Long exposureId;
    private String title;
    private String lmsAssignmentId;
    private Integer assignmentOrder;
    private String resourceLinkId;
    private boolean started;
    private Boolean softDeleted;
    private Integer numOfSubmissions;
    private Float hoursBetweenSubmissions;
    private String multipleSubmissionScoringScheme = MultipleSubmissionScoringScheme.MOST_RECENT.name();
    private Float cumulativeScoringInitialPercentage;
    private List<TreatmentDto> treatments;
    private boolean allowStudentViewResponses = false;
    private Timestamp studentViewResponsesAfter;
    private Timestamp studentViewResponsesBefore;
    private boolean allowStudentViewCorrectAnswers = false;
    private Timestamp studentViewCorrectAnswersAfter;
    private Timestamp studentViewCorrectAnswersBefore;
    private boolean published;
    private Date dueDate;

    public Long getAssignmentId() { return assignmentId; }

    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }

    public Long getExposureId() { return exposureId; }

    public void setExposureId(Long exposureId) { this.exposureId = exposureId; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getLmsAssignmentId() { return lmsAssignmentId; }

    public void setLmsAssignmentId(String lmsAssignmentId) { this.lmsAssignmentId = lmsAssignmentId; }

    public Integer getAssignmentOrder() { return assignmentOrder; }

    public void setAssignmentOrder(Integer assignmentOrder) { this.assignmentOrder = assignmentOrder; }

    public String getResourceLinkId() {
        return resourceLinkId;
    }

    public void setResourceLinkId(String resourceLinkId) {
        this.resourceLinkId = resourceLinkId;
    }

    public boolean getStarted() { return started; }

    public void setStarted(boolean started) { this.started = started; }

    public Boolean getSoftDeleted() {
        return softDeleted;
    }

    public void setSoftDeleted(Boolean softDeleted) {
        this.softDeleted = softDeleted;
    }

    public Integer getNumOfSubmissions() {
        return numOfSubmissions;
    }

    public void setNumOfSubmissions(Integer numOfSubmissions) {
        this.numOfSubmissions = numOfSubmissions;
    }

    public Float getHoursBetweenSubmissions() {
        return hoursBetweenSubmissions;
    }

    public void setHoursBetweenSubmissions(Float hoursBetweenSubmissions) {
        this.hoursBetweenSubmissions = hoursBetweenSubmissions;
    }

    public String getMultipleSubmissionScoringScheme() {
        return multipleSubmissionScoringScheme;
    }

    public void setMultipleSubmissionScoringScheme(String multipleSubmissionScoringScheme) {
        this.multipleSubmissionScoringScheme = multipleSubmissionScoringScheme;
    }

    public Float getCumulativeScoringInitialPercentage() {
        return cumulativeScoringInitialPercentage;
    }

    public void setCumulativeScoringInitialPercentage(Float cumulativeScoringInitialPercentage) {
        this.cumulativeScoringInitialPercentage = cumulativeScoringInitialPercentage;
    }

    public List<TreatmentDto> getTreatments() {
        return treatments;
    }

    public void setTreatments(List<TreatmentDto> treatments) {
        this.treatments = treatments;
    }

    public boolean isAllowStudentViewResponses() {
        return allowStudentViewResponses;
    }

    public void setAllowStudentViewResponses(boolean allowStudentViewResponses) {
        this.allowStudentViewResponses = allowStudentViewResponses;
    }

    public Timestamp getStudentViewResponsesAfter() {
        return studentViewResponsesAfter;
    }

    public void setStudentViewResponsesAfter(Timestamp studentViewResponsesAfter) {
        this.studentViewResponsesAfter = studentViewResponsesAfter;
    }

    public Timestamp getStudentViewResponsesBefore() {
        return studentViewResponsesBefore;
    }

    public void setStudentViewResponsesBefore(Timestamp studentViewResponsesBefore) {
        this.studentViewResponsesBefore = studentViewResponsesBefore;
    }

    public boolean isAllowStudentViewCorrectAnswers() {
        return allowStudentViewCorrectAnswers;
    }

    public void setAllowStudentViewCorrectAnswers(boolean allowStudentViewCorrectAnswers) {
        this.allowStudentViewCorrectAnswers = allowStudentViewCorrectAnswers;
    }

    public Timestamp getStudentViewCorrectAnswersAfter() {
        return studentViewCorrectAnswersAfter;
    }

    public void setStudentViewCorrectAnswersAfter(Timestamp studentViewCorrectAnswersAfter) {
        this.studentViewCorrectAnswersAfter = studentViewCorrectAnswersAfter;
    }

    public Timestamp getStudentViewCorrectAnswersBefore() {
        return studentViewCorrectAnswersBefore;
    }

    public void setStudentViewCorrectAnswersBefore(Timestamp studentViewCorrectAnswersBefore) {
        this.studentViewCorrectAnswersBefore = studentViewCorrectAnswersBefore;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

}
