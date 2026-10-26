package edu.iu.terracotta.dao.model.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnswerSubmissionDto {

    private UUID answerSubmissionId;
    private UUID answerId;
    private UUID questionSubmissionId;
    private String response;
    private String fileContent;
    private String fileName;
    private String mimeType;
    // set by the server when it stores an uploaded file, never taken from a request: a client could
    // otherwise point a file answer at any path, or at another student's stored file
    @JsonIgnore private String fileUri;
    @JsonIgnore private String encryptionPhrase;
    @JsonIgnore private String encryptionMethod;

}
