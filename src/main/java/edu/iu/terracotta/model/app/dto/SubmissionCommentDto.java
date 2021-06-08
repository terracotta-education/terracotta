package edu.iu.terracotta.model.app.dto;

public class SubmissionCommentDto {

    private Long submissionCommentId;
    private Long submissionId;
    private String comment;
    private String creator;

    public Long getSubmissionCommentId() { return submissionCommentId; }

    public void setSubmissionCommentId(Long submissionCommentId) { this.submissionCommentId = submissionCommentId; }

    public Long getSubmissionId() { return submissionId; }

    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }

    public String getComment() { return comment; }

    public void setComment(String comment) { this.comment = comment; }

    public String getCreator() { return creator; }

    public void setCreator(String creator) { this.creator = creator; }
}
