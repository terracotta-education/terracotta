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

@Table(name = "terr_question_submission_comment")
@Entity
public class QuestionSubmissionComment extends BaseEntity {
    @Column(name = "question_submission_comment_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionSubmissionCommentId;

    @JoinColumn(name = "question_submission_question_submission_id", nullable = false)
    @ManyToOne(optional = false)
    private QuestionSubmission questionSubmission;

    @Column(name = "comment")
    @Lob
    private String comment;

    @Column(name = "creator")
    private String creator;


    public Long getQuestionSubmissionCommentId() { return questionSubmissionCommentId; }

    public void setQuestionSubmissionCommentId(Long questionSubmissionCommentId) { this.questionSubmissionCommentId = questionSubmissionCommentId; }

    public QuestionSubmission getQuestionSubmission() { return questionSubmission; }

    public void setQuestionSubmission(QuestionSubmission questionSubmission) { this.questionSubmission = questionSubmission; }

    public String getComment() { return comment; }

    public void setComment(String comment) { this.comment = comment; }

    public String getCreator() { return creator; }

    public void setCreator(String creator) { this.creator = creator; }
}