package edu.iu.terracotta.model.app.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnswerSubmissionDto {

    private Long answerSubmissionId;
    private Long answerId;
    private Long questionSubmissionId;
    private String response;

}
