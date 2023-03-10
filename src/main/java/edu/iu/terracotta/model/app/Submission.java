package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "terr_submission")
public class Submission extends BaseEntity {

    @Id
    @Column(name = "submission_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long submissionId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "participant_participant_id", nullable = false)
    private Participant participant;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assessment_assessment_id", nullable = false)
    private Assessment assessment;

    //grade calculated by points
    @Column
    private Float calculatedGrade;

    //calculated grade altered by instructor (i.e. for partial credit)
    @Column
    private Float alteredCalculatedGrade;

    //manual total altered grade (i.e. 0 for cheating)
    @Column
    private Float totalAlteredGrade;

    @Column
    private Timestamp dateSubmitted;

    @Column
    private boolean lateSubmission;

    @OneToMany(mappedBy = "submission", orphanRemoval = true)
    private List<QuestionSubmission> questionSubmissions;

    @OneToMany(mappedBy = "submission", orphanRemoval = true)
    private List<SubmissionComment> submissionComments;

}
