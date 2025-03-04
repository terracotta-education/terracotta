package edu.iu.terracotta.service.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.AssignmentFileArchive;
import edu.iu.terracotta.dao.entity.FileSubmissionLocal;
import edu.iu.terracotta.dao.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.FileInfoDto;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;

public interface FileStorageService {

    void deleteConsentFile(long experimentId);
    Resource getConsentFile(long experimentId);
    FileSubmissionLocal saveConsentFile(MultipartFile file, long experimentId);
    FileInfoDto uploadConsentFile(long experimentId, String title, MultipartFile multipartFile, SecuredInfo securedInfo) throws AssignmentNotCreatedException, AssignmentNotEditedException, AssignmentNotMatchingException, ApiException, IOException, TerracottaConnectorException;
    void deleteConsentAssignment(long experimentId, SecuredInfo securedInfo) throws AssignmentNotEditedException, ApiException, IOException, NumberFormatException, TerracottaConnectorException;
    String parseHTMLFiles (String html, String localUrl);
    FileSubmissionLocal saveFileSubmissionLocal(MultipartFile file);
    File getFileSubmissionLocal(long id);
    boolean compressFile(String filePathToCompress, String encryptionPhrase, String compressedFileExtension);
    boolean compressFile(String filePathToCompress, String encryptionPhrase, String compressedFileExtension, boolean encrypt);
    boolean compressDirectory(String filePathToCompress, String encryptionPhrase, String compressedFileExtension);
    boolean compressDirectory(String filePathToCompress, String encryptionPhrase, String compressedFileExtension, boolean encrypt);
    void saveAssignmentFileArchive(AssignmentFileArchive assignmentFileArchive, File file);
    File getAssignmentFileArchive(long id);

}
