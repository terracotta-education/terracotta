package edu.iu.terracotta.service.app;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.AnswerFileSubmission;
import edu.iu.terracotta.dao.entity.AssignmentFileArchive;
import edu.iu.terracotta.dao.entity.ConsentDocument;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.FileSubmissionLocal;
import edu.iu.terracotta.dao.entity.export.data.ExperimentDataExport;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImport;
import edu.iu.terracotta.dao.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.model.distribute.export.Export;
import edu.iu.terracotta.dao.model.dto.FileInfoDto;
import edu.iu.terracotta.dao.model.dto.distribute.ExportDto;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface FileStorageService {

    void deleteConsentFile(long experimentId);
    Resource getConsentFile(long experimentId);
    FileSubmissionLocal saveConsentFile(MultipartFile file);
    FileSubmissionLocal saveConsentFile(InputStream inputStream, String filename);
    FileInfoDto uploadConsentFile(long experimentId, String title, MultipartFile multipartFile, SecuredInfo securedInfo) throws AssignmentNotCreatedException, AssignmentNotEditedException, AssignmentNotMatchingException, ApiException, IOException, TerracottaConnectorException;
    void sendConsentFileToLms(ConsentDocument consentDocument, Experiment experiment, LtiUserEntity instructorUser) throws AssignmentNotCreatedException, IOException, TerracottaConnectorException;
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
    void deleteFileSubmission(long fileSubmissionId);
    void deleteFileSubmission(AnswerFileSubmission answer);
    void saveExperimentDataExport(ExperimentDataExport exportData, File file);
    File getExperimentDataExport(long id);
    void createExperimentExportFile(ExportDto transferExportDto, Export export, String filename) throws IOException;
    void saveExperimentImportFile(MultipartFile file, ExperimentImport experimentImport) throws IOException;
    File getExperimentImportFile(long id);

}
