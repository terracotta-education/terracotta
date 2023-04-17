package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.model.app.dto.FileInfoDto;
import edu.iu.terracotta.model.app.FileSubmissionLocal;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.File;

public interface FileStorageService {

    void deleteConsentFile(long experimentId);

    Resource getConsentFile(long experimentId);

    FileSubmissionLocal saveConsentFile(MultipartFile file, long experimentId);

    FileInfoDto uploadConsentFile(long experimentId, String title, MultipartFile multipartFile, SecuredInfo securedInfo) throws AssignmentNotCreatedException, CanvasApiException, AssignmentNotEditedException, AssignmentNotMatchingException;

    void deleteConsentAssignment(long experimentId, SecuredInfo securedInfo) throws AssignmentNotEditedException, CanvasApiException;

    String parseHTMLFiles (String html, String localUrl);

    FileSubmissionLocal saveFileSubmissionLocal(MultipartFile file);

    File getFileSubmissionLocal(long id);

    boolean compressFile(String filePathToCompress, String encryptionPhrase, String compressedFileExtension);

}
