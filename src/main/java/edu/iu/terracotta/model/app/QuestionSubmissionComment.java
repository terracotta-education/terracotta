package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "terr_question_submission_comment")
public class QuestionSubmissionComment extends BaseEntity {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
