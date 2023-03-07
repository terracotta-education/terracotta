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
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Getter
@Setter
@Table(name = "terr_question_submission_comment")
public class QuestionSubmissionComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_submission_comment_id", nullable = false)
    private Long questionSubmissionCommentId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "question_submission_question_submission_id", nullable = false)
    private QuestionSubmission questionSubmission;

    @Lob
    @Column
    private String comment;

    @Column
    private String creator;

}
