package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnswerDto {

    private Long answerId;
    private String html;
    private Boolean correct;
    private Long questionId;
    private Integer answerOrder;


    public Long getAnswerId() { return answerId; }

    public void setAnswerId(Long answerId) { this.answerId = answerId; }

    public String getHtml() { return html; }

    public void setHtml(String html) { this.html = html; }

    public Boolean getCorrect() { return correct; }

    public void setCorrect(Boolean correct) { this.correct = correct; }

    public Long getQuestionId() { return questionId; }

    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public Integer getAnswerOrder() { return answerOrder; }

    public void setAnswerOrder(Integer answerOrder) { this.answerOrder = answerOrder; }
}
