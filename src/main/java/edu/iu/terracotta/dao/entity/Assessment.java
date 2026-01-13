package edu.iu.terracotta.dao.entity;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.apache.commons.lang3.Strings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
import jakarta.persistence.Transient;
import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;
import edu.iu.terracotta.dao.entity.integrations.Integration;
import edu.iu.terracotta.dao.model.enums.MultipleSubmissionScoringScheme;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "terr_assessment")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Assessment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "assessment_id",
        nullable = false
    )
    private Long assessmentId;

    @Column private String title;
    @Column private Integer numOfSubmissions; // if null, no multiple attempts allowed; if zero, then the number of submissions is unlimited
    @Column private Float hoursBetweenSubmissions; // if null then no minimum time between submissions
    @Column private Float cumulativeScoringInitialPercentage;
    @Column private Timestamp studentViewResponsesAfter;
    @Column private Timestamp studentViewResponsesBefore;
    @Column private Timestamp studentViewCorrectAnswersAfter;
    @Column private Timestamp studentViewCorrectAnswersBefore;

    @Lob
    @Column
    private String html;

    @Column(nullable = false)
    private boolean autoSubmit;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
        name = "multiple_submission_scoring_scheme",
        nullable = false
    )
    private MultipleSubmissionScoringScheme multipleSubmissionScoringScheme = MultipleSubmissionScoringScheme.MOST_RECENT;

    @Builder.Default
    @Column(
        name = "allow_student_view_responses",
        nullable = false
    )
    private boolean allowStudentViewResponses = false;

    @Builder.Default
    @Column(
        name = "allow_student_view_correct_answers",
        nullable = false
    )
    private boolean allowStudentViewCorrectAnswers = false;

    @OneToOne(optional = false)
    @JoinColumn(
        name = "treatment_treatment_id",
        nullable = false
    )
    private Treatment treatment;

    @OneToMany(
        mappedBy = "assessment",
        orphanRemoval = true
    )
    private List<Question> questions;

    @OneToMany(
        mappedBy = "assessment",
        orphanRemoval = true
    )
    private List<Submission> submissions;

    @Transient
    public Integration getIntegration() {
        return questions.get(0).getIntegration();
    }

    @Transient
    public boolean isIntegration() {
        return questions.stream()
            .anyMatch(question -> question.getQuestionType() == QuestionTypes.INTEGRATION);
    }

    public boolean canViewResponses() {
        if (!isAllowStudentViewResponses()) {
            return false;
        }

        if (isIntegration()) {
            if (Strings.CS.equals(getIntegration().getConfiguration().getClient().getName(), "Qualtrics")) {
                // Qualtrics integrations never see feedback
                return false;
            }
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
