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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "terr_assessment")
public class Assessment extends BaseEntity {

    @Id
    @Column(nullable = false)
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

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MultipleSubmissionScoringScheme multipleSubmissionScoringScheme = MultipleSubmissionScoringScheme.MOST_RECENT;

    @Column
    private Float cumulativeScoringInitialPercentage;

    @Column(nullable = false)
    private boolean allowStudentViewResponses = false;

    @Column
    private Timestamp studentViewResponsesAfter;

    @Column
    private Timestamp studentViewResponsesBefore;

    @Column(nullable = false)
    private boolean allowStudentViewCorrectAnswers = false;

    @Column
    private Timestamp studentViewCorrectAnswersAfter;

    @Column
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
