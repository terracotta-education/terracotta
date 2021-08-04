package edu.iu.terracotta.model.app.dto;

import java.sql.Timestamp;

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

    public String getFileId() { return fileId; }

    public void setFileId(String fileId) { this.fileId = fileId; }

    public Long getExperimentId() { return experimentId; }

    public void setExperimentId(Long experimentId) { this.experimentId = experimentId; }

    public String getPath() { return path; }

    public void setPath(String path) { this.path = path; }

    public String getUrl() { return url; }

    public void setUrl(String url) { this.url = url; }

    public Timestamp getDateUpdated() { return dateUpdated; }

    public void setDateUpdated(Timestamp dateUpdated) { this.dateUpdated = dateUpdated; }

    public Timestamp getDateCreated() { return dateCreated; }

    public void setDateCreated(Timestamp dateCreated) { this.dateCreated = dateCreated; }

    public String getFileType() { return fileType; }

    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getSize() { return size; }

    public void setSize(Long size) { this.size = size; }

    public String getTempToken() {
        return tempToken;
    }

    public void setTempToken(String tempToken) {
        this.tempToken = tempToken;
    }
}
