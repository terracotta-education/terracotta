package edu.iu.terracotta.model.app;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileSubmissionLocal {

    private String filePath;
    private boolean compressed;
    private String encryptionMethod;
    private String encryptionPhrase;

}
