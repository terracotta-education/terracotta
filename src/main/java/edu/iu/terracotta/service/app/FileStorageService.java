package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.FileInfo;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.util.Optional;

public interface FileStorageService {

    String storeFile(MultipartFile file, String extraPath, Long experimentId, boolean consent);

    void saveFile(MultipartFile multipartFile, String extraPath, Long experimentId);

    Resource loadFileAsResource(String fileName, String extraPath);

    Resource getFileAsResource(String fileId);

    Optional<FileInfo> findByFileId(String fileId);

    boolean deleteByFileId(String fileId);

    boolean deleteFile(String fileName, String extraPath);

}
