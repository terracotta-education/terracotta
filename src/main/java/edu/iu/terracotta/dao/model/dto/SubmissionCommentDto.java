package edu.iu.terracotta.dao.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmissionCommentDto {

    private Long submissionCommentId;
    private Long submissionId;
    private String comment;
    private String creator;

}
