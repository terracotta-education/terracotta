package edu.iu.terracotta.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;
import edu.iu.terracotta.dao.model.enums.MultipleSubmissionScoringScheme;

import java.sql.Timestamp;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "terr_assignment")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Assignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assignmentId;

    private Timestamp studentViewResponsesAfter;
    private Timestamp studentViewResponsesBefore;
    private Timestamp studentViewCorrectAnswersAfter;
    private Timestamp studentViewCorrectAnswersBefore;
    private Timestamp started;
    private Float cumulativeScoringInitialPercentage;
    private String lmsAssignmentId;
    private String resourceLinkId;
    private String title;
    private Integer assignmentOrder;
    private Integer numOfSubmissions; // if null, no multiple attempts allowed; if zero, then the number of submissions is unlimited
    private Float hoursBetweenSubmissions;
    private String metadata; // JSON metadata from the LMS
    @Transient private Date dueDate;

    @Builder.Default
    private Boolean softDeleted = false;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MultipleSubmissionScoringScheme multipleSubmissionScoringScheme = MultipleSubmissionScoringScheme.MOST_RECENT;

    @Builder.Default
    @Column(nullable = false)
    private boolean allowStudentViewResponses = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean allowStudentViewCorrectAnswers = false;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(
        name = "exposure_exposure_id",
        nullable = false
    )
    private Exposure exposure;

    public boolean isStarted() {
        return this.started != null;
    }

    @Transient
    @Builder.Default
    private boolean published = false;

}
