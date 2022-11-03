package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionDto {

    private Long questionId;
    private String html;
    private Float points;
    private Long assessmentId;
    private Integer questionOrder;
    private String questionType;
    private List<AnswerDto> answers;
    private boolean randomizeAnswers; // only applies to 'MC' questions

}
