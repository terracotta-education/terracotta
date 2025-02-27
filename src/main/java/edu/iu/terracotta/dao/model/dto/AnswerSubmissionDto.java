package edu.iu.terracotta.dao.model.dto;

import java.io.File;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
