package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import edu.iu.terracotta.model.app.enumerator.MultipleSubmissionScoringScheme;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

@Table(name = "terr_assignment")
@Entity
public class Assignment extends BaseEntity {
    @Column(name = "assignment_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assignmentId;

    @JoinColumn(name = "exposure_exposure_id", nullable = false)
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Exposure exposure;

    @Column(name = "lms_assignment_id")
    private String lmsAssignmentId;

    @Column(name = "resource_link_id")
    private String resourceLinkId;

    @Column(name = "title")
    private String title;

    @Column(name = "assignment_order")
    private Integer assignmentOrder;

    @Column(name = "soft_deleted")
    private Boolean softDeleted = false;

    // if null, no multiple attempts allowed; if zero, then the number of submissions is unlimited
    @Column(name = "num_of_submissions", nullable = true)
    private Integer numOfSubmissions;

    // if null then no minimum time between submissions
    @Column(name = "hours_between_submissions", nullable = true)
    private Float hoursBetweenSubmissions;

    @Enumerated(EnumType.STRING)
    @Column(name = "multiple_submission_scoring_scheme", nullable = false)
    private MultipleSubmissionScoringScheme multipleSubmissionScoringScheme = MultipleSubmissionScoringScheme.MOST_RECENT;

    @Column(name = "cumulative_scoring_initial_percentage", nullable = true)
    private Float cumulativeScoringInitialPercentage;

    @Column(name = "allow_student_view_responses", nullable = false)
    private boolean allowStudentViewResponses = false;

    @Column(name = "student_view_responses_after", nullable = true)
    private Timestamp studentViewResponsesAfter;

    @Column(name = "student_view_responses_before", nullable = true)
    private Timestamp studentViewResponsesBefore;

    @Column(name = "allow_student_view_correct_answers", nullable = false)
    private boolean allowStudentViewCorrectAnswers = false;

    @Column(name = "student_view_correct_answers_after", nullable = true)
    private Timestamp studentViewCorrectAnswersAfter;

    @Column(name = "student_view_correct_answers_before", nullable = true)
    private Timestamp studentViewCorrectAnswersBefore;

    @Column
    private Timestamp started;

    @Transient
    private boolean published = false;

    @Transient
    private Date dueDate;

    //methods
    public Long getAssignmentId() { return assignmentId; }

    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }

    public Exposure getExposure() { return exposure; }

    public void setExposure(Exposure exposure) { this.exposure = exposure; }

    public String getLmsAssignmentId() { return lmsAssignmentId; }

    public void setLmsAssignmentId(String lmsAssignmentId) { this.lmsAssignmentId = lmsAssignmentId; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public Integer getAssignmentOrder() { return assignmentOrder; }

    public void setAssignmentOrder(Integer assignmentOrder) { this.assignmentOrder = assignmentOrder; }

    public String getResourceLinkId() {
        return resourceLinkId;
    }

    public void setResourceLinkId(String resourceLinkId) {
        this.resourceLinkId = resourceLinkId;
    }

    public Boolean getSoftDeleted() {
        return softDeleted;
    }

    public void setSoftDeleted(Boolean softDeleted) {
        this.softDeleted = softDeleted;
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

    public MultipleSubmissionScoringScheme getMultipleSubmissionScoringScheme() {
        return multipleSubmissionScoringScheme;
    }

    public void setMultipleSubmissionScoringScheme(MultipleSubmissionScoringScheme multipleSubmissionScoringScheme) {
        this.multipleSubmissionScoringScheme = multipleSubmissionScoringScheme;
    }

    public Float getCumulativeScoringInitialPercentage() {
        return cumulativeScoringInitialPercentage;
    }

    public void setCumulativeScoringInitialPercentage(Float cumulativeScoringInitialPercentage) {
        this.cumulativeScoringInitialPercentage = cumulativeScoringInitialPercentage;
    }

    public Timestamp getStarted() {
        return started;
    }

    public void setStarted(Timestamp started) {
        this.started = started;
    }

    public boolean isStarted() {
        return this.started != null;
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
