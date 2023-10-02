package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import edu.iu.terracotta.model.app.enumerator.MultipleSubmissionScoringScheme;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
    @Column(name = "assignment_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assignmentId;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "exposure_exposure_id", nullable = false)
    private Exposure exposure;

    @Column
    private String lmsAssignmentId;

    @Column
    private String resourceLinkId;

    @Column
    private String title;

    @Column
    private Integer assignmentOrder;

    @Column
    private Boolean softDeleted = false;

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

    @Column
    private Timestamp started;

    @Transient
    private boolean published = false;

    @Transient
    private Date dueDate;

    public boolean isStarted() {
        return this.started != null;
    }

}
