package edu.iu.terracotta.dao.model.dto;

import java.io.File;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.model.enums.AssignmentFileArchiveStatus;
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
public class AssignmentFileArchiveDto {

    private UUID id;
    private long assignmentId;
    private String assignmentTitle;
    private String experimentTitle;
    private AssignmentFileArchiveStatus status;
    private String fileContent;
    private String fileName;
    private String mimeType;
    private File file;

}
