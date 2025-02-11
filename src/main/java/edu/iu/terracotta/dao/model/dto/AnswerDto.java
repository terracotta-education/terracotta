package edu.iu.terracotta.dao.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnswerDto {

    private Long answerId;
    private String answerType;
    private Long questionId;
    private String html;
    private Boolean correct;
    private Integer answerOrder;

}
