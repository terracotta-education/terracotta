package edu.iu.terracotta.model.app;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import edu.iu.terracotta.model.BaseEntity;
import edu.iu.terracotta.model.app.enumerator.MultipleSubmissionScoringScheme;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "terr_assessment")
public class Assessment extends BaseEntity {

    @Id
    @Column(name = "assessment_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assessmentId;

    @Lob
    @Column
    private String html;

    @OneToMany(mappedBy = "assessment", orphanRemoval = true)
    private List<Question> questions;

    @OneToOne(optional = false)
    @JoinColumn(name = "treatment_treatment_id", nullable = false)
    private Treatment treatment;

    @Column
    private String title;

    @Column(nullable = false)
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
