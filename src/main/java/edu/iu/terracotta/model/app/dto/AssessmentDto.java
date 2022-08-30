package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import edu.iu.terracotta.model.app.enumerator.MultipleSubmissionScoringScheme;

import java.sql.Timestamp;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentDto {

    private Long assessmentId;
    private String html;
    private Long treatmentId;
    private String title;
    private boolean autoSubmit;
    private Integer numOfSubmissions;
    private Float hoursBetweenSubmissions;
    private String multipleSubmissionScoringScheme = MultipleSubmissionScoringScheme.MOST_RECENT.name();
    private Float cumulativeScoringInitialPercentage;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<SubmissionDto> submissions;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<QuestionDto> questions;
    private Float maxPoints;
    private boolean started;
    private Long submissionsExpected;
    private Long submissionsCompletedCount;
    private Long submissionsInProgressCount;
    private boolean allowStudentViewResponses = false;
    private Timestamp studentViewResponsesAfter;
    private Timestamp studentViewResponsesBefore;
    private boolean allowStudentViewCorrectAnswers = false;
    private Timestamp studentViewCorrectAnswersAfter;
    private Timestamp studentViewCorrectAnswersBefore;


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

    public Float getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(Float maxPoints) {
        this.maxPoints = maxPoints;
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
}
