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
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "terr_question_submission")
public class QuestionSubmission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_submission_id", nullable = false)
    private Long questionSubmissionId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "submission_submission_id", nullable = false)
    private Submission submission;

    @Column
    private Float calculatedPoints;

    @Column
    private Float alteredGrade;

    @ManyToOne(optional = false)
    @JoinColumn(name = "question_question_id", nullable = false)
    private Question question;

    @OneToMany(mappedBy = "questionSubmission", orphanRemoval = true)
    private List<QuestionSubmissionComment> questionSubmissionComments;

    @OneToMany(mappedBy = "questionSubmission")
    private List<AnswerMcSubmissionOption> answerMcSubmissionOptions;

}
