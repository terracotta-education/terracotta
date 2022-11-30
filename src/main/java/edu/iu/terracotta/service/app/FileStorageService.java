package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.model.app.FileInfo;
import edu.iu.terracotta.model.app.dto.FileInfoDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;

public interface FileStorageService {

    public enum StorageType{
        AWS
    }

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

    FileInfoDto toDto(FileInfo fileInfo) throws GeneralSecurityException;

    FileInfoDto uploadFile(MultipartFile multipartFile, String prefix, String extraPath, long experimentId, boolean consent);

    void uploadConsent(long experimentId, String title, FileInfoDto fileInfoDto, String instructorUserId)
            throws AssignmentNotCreatedException, CanvasApiException, AssignmentNotEditedException;

    void deleteConsentAssignment(long experimentId, SecuredInfo securedInfo) throws AssignmentNotEditedException, CanvasApiException;

    String parseHTMLFiles (String html);


    String uploadFileToAWSAndGetURI(File file, String fileName, String extension);


    File  downloadFilesFromURI(String uri, StorageType storageType) throws FileNotFoundException;
}
