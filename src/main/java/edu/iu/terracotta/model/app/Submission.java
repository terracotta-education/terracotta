package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "terr_submission")
public class Submission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "submission_id",
        nullable = false
    )
    private Long submissionId;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "participant_participant_id",
        nullable = false
    )
    private Participant participant;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "assessment_assessment_id",
        nullable = false
    )
    private Assessment assessment;

    /**
     * grade calculated by points
     */
    @Column
    private Float calculatedGrade;

    /**
     * calculated grade altered by instructor (i.e. for partial credit)
     */
    @Column
    private Float alteredCalculatedGrade;

    /**
     * manual total altered grade (i.e. 0 for cheating)
     */
    @Column
    private Float totalAlteredGrade;

    @Column
    private Timestamp dateSubmitted;

    @Column
    private boolean lateSubmission;

    @Column
    private boolean gradeOverridden;

    @OneToMany(
        mappedBy = "submission",
        orphanRemoval = true
    )
    private List<QuestionSubmission> questionSubmissions;

    @OneToMany(
        mappedBy = "submission",
        orphanRemoval = true
    )
    private List<SubmissionComment> submissionComments;

    @Transient
    public boolean isSubmitted() {
        return dateSubmitted != null;
    }

}
