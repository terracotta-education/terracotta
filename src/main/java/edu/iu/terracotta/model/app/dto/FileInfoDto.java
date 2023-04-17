package edu.iu.terracotta.model.app.dto;

import java.sql.Timestamp;

import edu.iu.terracotta.model.app.FileSubmissionLocal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileInfoDto {

    private String fileId;
    private Long experimentId;
    private String path;
    private String url;
    private String fileType;
    private Long size;
    private Timestamp dateUpdated;
    private Timestamp dateCreated;
    private String tempToken;
    private FileSubmissionLocal fileSubmissionLocal;

}
