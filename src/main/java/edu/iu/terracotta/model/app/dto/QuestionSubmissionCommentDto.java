package edu.iu.terracotta.model.app.dto;

public class QuestionSubmissionCommentDto {

    private Long questionSubmissionCommentId;
    private Long questionSubmissionId;
    private String comment;
    private String creator;

    public Long getQuestionSubmissionCommentId() { return questionSubmissionCommentId; }

    public void setQuestionSubmissionCommentId(Long questionSubmissionCommentId) { this.questionSubmissionCommentId = questionSubmissionCommentId; }

    public Long getQuestionSubmissionId() { return questionSubmissionId; }

    public void setQuestionSubmissionId(Long questionSubmissionId) { this.questionSubmissionId = questionSubmissionId; }

    public String getComment() { return comment; }

    public void setComment(String comment) { this.comment = comment; }

    public String getCreator() { return creator; }

    public void setCreator(String creator) { this.creator = creator; }
}
