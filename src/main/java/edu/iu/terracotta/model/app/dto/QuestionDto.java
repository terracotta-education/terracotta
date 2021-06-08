package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java .util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionDto {

    private Long questionId;
    private String html;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<AnswerDto> answers;
    private Float points;
    private Long assessmentId;
    private Integer questionOrder;


    public Long getQuestionId() { return questionId; }

    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getHtml() { return html; }

    public void setHtml(String html) { this.html = html; }

    public List<AnswerDto> getAnswers() { return answers; }

    public void setAnswers(List<AnswerDto> answers) {
        this.answers = answers;
    }

    public Float getPoints() { return points; }

    public void setPoints(Float points) { this.points = points; }

    public Long getAssessmentId() { return assessmentId; }

    public void setAssessmentId(Long assessmentId) { this.assessmentId = assessmentId; }

    public Integer getQuestionOrder() { return questionOrder; }

    public void setQuestionOrder(Integer questionOrder) { this.questionOrder = questionOrder; }
}
