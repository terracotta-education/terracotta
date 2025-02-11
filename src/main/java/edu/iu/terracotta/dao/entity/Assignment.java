package edu.iu.terracotta.dao.entity;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
@Table(name = "terr_assignment")
public class Assignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "assignment_id",
        nullable = false
    )
    private Long assignmentId;

    @Column private Timestamp studentViewResponsesAfter;
    @Column private Timestamp studentViewResponsesBefore;
    @Column private Timestamp studentViewCorrectAnswersAfter;
    @Column private Timestamp studentViewCorrectAnswersBefore;
    @Column private Timestamp started;
    @Transient private boolean published = false;
    @Transient private Date dueDate;
    @Column private Float cumulativeScoringInitialPercentage;
    @Column private String lmsAssignmentId;
    @Column private String resourceLinkId;
    @Column private String title;
    @Column  private Integer assignmentOrder;
    @Column private Boolean softDeleted = false;
    @Column private Integer numOfSubmissions; // if null, no multiple attempts allowed; if zero, then the number of submissions is unlimited
    @Column private Float hoursBetweenSubmissions;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MultipleSubmissionScoringScheme multipleSubmissionScoringScheme = MultipleSubmissionScoringScheme.MOST_RECENT;

    @Column(nullable = false)
    private boolean allowStudentViewResponses = false;

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

}
