package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionSubmissionDto {

    private Long questionSubmissionId;
    private Long questionId;
    private Float calculatedPoints;
    private Float alteredGrade;
    private Long submissionId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<QuestionSubmissionCommentDto> questionSubmissionCommentDtoList;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<AnswerSubmissionDto> answerSubmissionDtoList;

    public Long getQuestionSubmissionId() { return questionSubmissionId; }

    public void setQuestionSubmissionId(Long questionSubmissionId) { this.questionSubmissionId = questionSubmissionId; }

    public Long getQuestionId() { return questionId; }

    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public Float getCalculatedPoints() { return calculatedPoints; }

    public void setCalculatedPoints(Float calculatedPoints) { this.calculatedPoints = calculatedPoints; }

    public Float getAlteredGrade() { return alteredGrade; }

    public void setAlteredGrade(Float alteredGrade) { this.alteredGrade = alteredGrade; }

    public Long getSubmissionId() { return submissionId; }

    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }

    public List<QuestionSubmissionCommentDto> getQuestionSubmissionCommentDtoList() { return questionSubmissionCommentDtoList; }

    public void setQuestionSubmissionCommentDtoList(List<QuestionSubmissionCommentDto> questionSubmissionCommentDtoList) { this.questionSubmissionCommentDtoList = questionSubmissionCommentDtoList; }

    public List<AnswerSubmissionDto> getAnswerSubmissionDtoList() { return answerSubmissionDtoList; }

    public void setAnswerSubmissionDtoList(List<AnswerSubmissionDto> answerSubmissionDtoList) { this.answerSubmissionDtoList = answerSubmissionDtoList; }

}