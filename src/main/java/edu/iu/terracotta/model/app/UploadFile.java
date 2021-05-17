package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
/*
@Table(name = "terr_upload_file")
@Entity*/
public class UploadFile {

    //@Column(name = "file_name")
    private String fileName;

    //@Column(name = "file_download_uri")
    private String fileDownloadUri;

    //@Column(name = "file_type")
    private String fileType;

    //@Column(name = "size")
    private Long size;

    public UploadFile(String fileName, String fileDownloadUri, String fileType, long size) {
        this.fileName = fileName;
        this.fileDownloadUri = fileDownloadUri;
        this.fileType = fileType;
        this.size = size;
    }
    public String getFileName() { return fileName; }

    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileDownloadUri() { return fileDownloadUri; }

    public void setFileDownloadUri(String fileDownloadUri) { this.fileDownloadUri = fileDownloadUri; }

    public String getFileType() { return fileType; }

    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getSize() { return size; }

    public void setSize(Long size) { this.size = size; }
}