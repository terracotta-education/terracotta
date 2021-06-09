package edu.iu.terracotta.model.app;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Table(name = "terr_submission_comment")
@Entity
public class SubmissionComment {
    @Column(name = "submission_comment_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long submissionCommentId;

    @JoinColumn(name = "submission_submission_id", nullable = false)
    @ManyToOne(optional = false)
    private Submission submission;

    @Column(name = "comment")
    @Lob
    private String comment;

    @Column(name = "creator")
    private String creator;


    public Long getSubmissionCommentId() { return submissionCommentId; }

    public void setSubmissionCommentId(Long submissionCommentId) { this.submissionCommentId = submissionCommentId; }

    public Submission getSubmission() { return submission; }

    public void setSubmission(Submission submission) { this.submission = submission; }

    public String getComment() { return comment; }

    public void setComment(String comment) { this.comment = comment; }

    public String getCreator() { return creator; }

    public void setCreator(String creator) { this.creator = creator; }
}