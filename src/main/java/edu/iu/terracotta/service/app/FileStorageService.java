package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.model.app.FileInfo;
import edu.iu.terracotta.model.app.dto.FileInfoDto;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Optional;

public interface FileStorageService {

    String storeFile(MultipartFile file, String extraPath, Long experimentId, boolean consent);

    void saveFile(MultipartFile multipartFile, String extraPath, Long experimentId);

    Resource loadFileAsResource(String fileName, String extraPath);

    Resource getFileAsResource(String fileId);

    Optional<FileInfo> findByFileId(String fileId);

    FileInfo findByExperimentIdAndFilename(Long experimentId, String filename);

    boolean deleteByFileId(String fileId);

    boolean deleteFile(String fileName, String extraPath);

    List<FileInfo> findByExperimentId(Long experimentId);

    List<FileInfoDto> getFiles(long experimentId);

    FileInfoDto toDto(FileInfo fileInfo);

    FileInfoDto uploadFile(MultipartFile multipartFile, String prefix, String extraPath, long experimentId, boolean consent);

    void uploadConsent(long experimentId, String title, FileInfoDto fileInfoDto) throws AssignmentNotCreatedException;
}
