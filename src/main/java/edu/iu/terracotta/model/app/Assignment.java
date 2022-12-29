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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "terr_assignment")
public class Assignment extends BaseEntity {

    @Id
    @Column(nullable = false)
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

    @Column(columnDefinition = "boolean default false")
    private Boolean softDeleted = false;

    // if null, no multiple attempts allowed; if zero, then the number of submissions is unlimited
    @Column
    private Integer numOfSubmissions;

    // if null then no minimum time between submissions
    @Column
    private Float hoursBetweenSubmissions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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

    @Transient
    public boolean isStarted() {
        return this.started != null;
    }

}
