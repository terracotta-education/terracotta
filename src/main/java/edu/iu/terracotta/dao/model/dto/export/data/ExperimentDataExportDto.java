package edu.iu.terracotta.dao.model.dto.export.data;

import java.io.File;
import java.util.UUID;

import edu.iu.terracotta.dao.model.enums.export.data.ExperimentDataExportStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ExperimentDataExportDto {

    private UUID id;
    private long experimentId;
    private String experimentTitle;
    private ExperimentDataExportStatus status;
    private String fileContent;
    private String fileName;
    private String mimeType;
    private File file;

}
