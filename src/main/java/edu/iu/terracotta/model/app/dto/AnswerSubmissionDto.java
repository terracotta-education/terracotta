package edu.iu.terracotta.model.app.dto;

import java.io.File;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnswerSubmissionDto {

    private Long answerSubmissionId;
    private Long answerId;
    private Long questionSubmissionId;
    private String response;
    private String fileContent;
    private String fileName;
    private String mimeType;
    private String fileUri;
    private File file;
    private String encryptionPhrase;
    private String encryptionMethod;

}
