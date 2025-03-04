package edu.iu.terracotta.dao.model.dto;

import java.io.File;
import java.util.UUID;

import edu.iu.terracotta.dao.model.enums.AssignmentFileArchiveStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
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
