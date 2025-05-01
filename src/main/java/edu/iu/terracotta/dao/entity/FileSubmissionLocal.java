package edu.iu.terracotta.dao.entity;

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
public class FileSubmissionLocal {

    private String filePath;
    private boolean compressed;
    private String encryptionMethod;
    private String encryptionPhrase;

}
