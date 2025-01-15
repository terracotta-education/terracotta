package edu.iu.terracotta.dao.entity;

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
import java.util.List;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;

@Entity
@Getter
@Setter
@Table(name = "terr_question_submission")
public class QuestionSubmission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "question_submission_id",
        nullable = false
    )
    private Long questionSubmissionId;

    @Column private Float calculatedPoints;
    @Column private Float alteredGrade;

    @OneToMany(
        mappedBy = "questionSubmission",
        orphanRemoval = true
    )
    private List<QuestionSubmissionComment> questionSubmissionComments;

    @OneToMany(mappedBy = "questionSubmission")
    private List<AnswerMcSubmissionOption> answerMcSubmissionOptions;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "submission_submission_id",
        nullable = false
    )
    private Submission submission;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "question_question_id",
        nullable = false
    )
    private Question question;

}
