package edu.iu.terracotta.connectors.generic.dao.model.lms.options;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LmsFileUploadOptions {

    private String filename;
    private String parentFolderPath;
    private String contentType;
    private long size;

}
