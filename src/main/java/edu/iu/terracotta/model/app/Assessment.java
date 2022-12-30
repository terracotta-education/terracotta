package edu.iu.terracotta.model.app;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import edu.iu.terracotta.model.BaseEntity;
import edu.iu.terracotta.model.app.enumerator.MultipleSubmissionScoringScheme;

@Table(name = "terr_assessment")
@Entity
public class Assessment extends BaseEntity {
    @Column(name = "assessment_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assessmentId;

    @Column(name = "html")
    @Lob
    private String html;

    @OneToMany(mappedBy = "assessment", orphanRemoval = true)
    private List<Question> questions;

    @JoinColumn(name = "treatment_treatment_id", nullable = false)
    @OneToOne(optional = false)
    private Treatment treatment;

    @Column(name = "title")
    private String title;

    @Column(name = "auto_submit", nullable = false)
    private boolean autoSubmit;

    // if null, no multiple attempts allowed; if zero, then the number of submissions is unlimited
    @Column
    private Integer numOfSubmissions;

    // if null then no minimum time between submissions
    @Column
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

    @OneToMany(mappedBy = "assessment", orphanRemoval = true)
    private List<Submission> submissions;

    public Long getAssessmentId() { return assessmentId; }

    public void setAssessmentId(Long assessmentId) { this.assessmentId = assessmentId; }

    public String getHtml() { return html; }

    public void setHtml(String html) { this.html = html; }

    public List<Question> getQuestions() { return questions; }

    public void setQuestions(List<Question> questions) { this.questions = questions; }

    public Treatment getTreatment() { return treatment; }

    public void setTreatment(Treatment treatment) { this.treatment = treatment; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public boolean getAutoSubmit() { return autoSubmit; }

    public void setAutoSubmit(boolean autoSubmit) { this.autoSubmit = autoSubmit; }

    public Integer getNumOfSubmissions() { return numOfSubmissions; }

    public void setNumOfSubmissions(Integer numOfSubmissions) { this.numOfSubmissions = numOfSubmissions; }

    public List<Submission> getSubmissions() { return submissions; }

    public void setSubmissions(List<Submission> submissions) { this.submissions = submissions; }

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

    public boolean canViewResponses() {
        if (!isAllowStudentViewResponses()) {
            return false;
        }

        Timestamp now = Timestamp.valueOf(ZonedDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneOffset.UTC).toLocalDateTime());

        if (getStudentViewResponsesAfter() != null && now.before(getStudentViewResponsesAfter())) {
            // current time is before the allowed time
            return false;
        }

        // return false if current time is after the allowed time; true otherwise
        return !(getStudentViewResponsesBefore() != null && now.after(getStudentViewResponsesBefore()));
    }

    public boolean canViewCorrectAnswers() {
        if (!isAllowStudentViewCorrectAnswers()) {
            return false;
        }

        Timestamp now = Timestamp.valueOf(ZonedDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneOffset.UTC).toLocalDateTime());

        if (getStudentViewCorrectAnswersAfter() != null && now.before(getStudentViewCorrectAnswersAfter())) {
            // current time is before the allowed time
            return false;
        }

        // return false if current time is after the allowed time; true otherwise
        return !(getStudentViewCorrectAnswersBefore() != null && now.after(getStudentViewCorrectAnswersBefore()));
    }

}
