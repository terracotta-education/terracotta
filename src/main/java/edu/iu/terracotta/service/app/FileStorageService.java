package edu.iu.terracotta.service.app;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

public interface FileStorageService {

    String storeFile(MultipartFile file, String extraPath);

    Resource loadFileAsResource(String fileName , String extraPath);

    boolean deleteFile(String fileName , String extraPath);

    /*
    String getFileStorageLocation();

    void setFileStorageLocation(String fileStorageLocation);
     */
}
