package edu.iu.terracotta.dao.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<AnswerDto> answerDtoList;

}
