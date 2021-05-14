package edu.iu.terracotta.service.app;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

public interface FileStorageService {

    String storeFile(MultipartFile file);

    Resource loadFileAsResource(String fileName);

    /*
    String getFileStorageLocation();

    void setFileStorageLocation(String fileStorageLocation);
     */
}
