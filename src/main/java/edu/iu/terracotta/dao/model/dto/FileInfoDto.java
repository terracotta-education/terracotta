package edu.iu.terracotta.dao.model.dto;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.entity.FileSubmissionLocal;
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
