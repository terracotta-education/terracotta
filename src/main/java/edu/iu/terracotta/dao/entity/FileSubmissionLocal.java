package edu.iu.terracotta.dao.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record FileSubmissionLocal(

    String filePath,
    boolean compressed,
    String encryptionMethod,
    String encryptionPhrase

) {

}
