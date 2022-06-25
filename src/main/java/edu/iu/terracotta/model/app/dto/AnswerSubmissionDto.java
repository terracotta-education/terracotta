package edu.iu.terracotta.model.app.dto;

public class AnswerSubmissionDto {

    private Long answerSubmissionId;
    private Long answerId;
    private Long questionSubmissionId;
    private String response;
    private  byte[] fileContent;

    public Long getAnswerSubmissionId() { return answerSubmissionId; }

    public void setAnswerSubmissionId(Long answerSubmissionId) { this.answerSubmissionId = answerSubmissionId; }

    public Long getAnswerId() { return answerId; }

    public void setAnswerId(Long answerId) { this.answerId = answerId; }

    public Long getQuestionSubmissionId() { return questionSubmissionId; }

    public void setQuestionSubmissionId(Long questionSubmissionId) { this.questionSubmissionId = questionSubmissionId; }

    public String getResponse() { return response; }

    public void setResponse(String response) { this.response = response; }

    public byte[] getFileContent() {
        return fileContent;
    }

    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
    }
}
