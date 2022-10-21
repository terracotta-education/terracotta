package edu.iu.terracotta.model.app.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionSubmissionCommentDto {

    private Long questionSubmissionCommentId;
    private Long questionSubmissionId;
    private String comment;
    private String creator;

}
