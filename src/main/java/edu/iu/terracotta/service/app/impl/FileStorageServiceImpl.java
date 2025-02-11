package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.dao.entity.AnswerFileSubmission;
import edu.iu.terracotta.dao.entity.ConsentDocument;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.FileSubmissionLocal;
import edu.iu.terracotta.dao.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.FileInfoDto;
import edu.iu.terracotta.dao.repository.AnswerFileSubmissionRepository;
import edu.iu.terracotta.dao.repository.ConsentDocumentRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.exceptions.app.FileStorageException;
import edu.iu.terracotta.exceptions.app.MyFileNotFoundException;
import edu.iu.terracotta.service.app.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@SuppressWarnings({
    "squid:S1192", "squid:S112", "squid:S125", "squid:S2737", "squid:S4449", "squid:S1075",
    "PMD.GuardLogStatement", "PMD.PreserveStackTrace"
})
public class FileStorageServiceImpl implements FileStorageService {

    @Autowired private AnswerFileSubmissionRepository answerFileSubmissionRepository;
    @Autowired private ConsentDocumentRepository consentDocumentRepository;
    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private ApiClient apiClient;
    @Autowired private ApiJwtService apijwtService;

    @Value("${upload.path}")
    private String uploadDir;

    @Value("${upload.submissions.local.path}")
    private String uploadSubmissionsLocalPath;

    @Value("${upload.submissions.local.path.root}")
    private String uploadSubmissionsLocalPathRoot;

    @Value("${consent.file.local.path}")
    private String consentFileLocalPath;

    @Value("${consent.file.local.path.root}")
    private String consentFileLocalPathRoot;

    private Path decompressedSubmissionFileTempDirectory;
    private Path decompressedConsentFileTempDirectory;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            Files.createDirectories(Paths.get(uploadSubmissionsLocalPathRoot));
            Files.createDirectories(Paths.get(consentFileLocalPathRoot));
            this.decompressedSubmissionFileTempDirectory = Files.createTempDirectory(Paths.get(uploadSubmissionsLocalPath).toString() + ".");
            this.decompressedConsentFileTempDirectory = Files.createTempDirectory(Paths.get(consentFileLocalPath).toString() + ".");
        } catch (IOException e) {
            throw new RuntimeException("Error 138: Could not create upload folder!");
        }
    }

    @Override
    public Resource getConsentFile(long experimentId) {
        try {
            Optional<ConsentDocument> consentDocument = consentDocumentRepository.findByExperiment_ExperimentId(experimentId);

            if (consentDocument.isEmpty()) {
                throw new MyFileNotFoundException("Error 126: Consent file not found for experiment ID: '{}'" + experimentId);
            }

            if (consentDocument.isEmpty()) {
                return null;
            }

            final File consentFile;

            if (!consentDocument.get().isCompressed()) {
                // file is not compressed; return it as-is
                consentFile = new File(String.format("%s/%s", consentFileLocalPathRoot, consentDocument.get().getFileUri()));
                return new UrlResource(consentFile.toPath().toUri());
            }

            // file is compressed; decompress and then return it
            try {
                decompressFile(
                    Paths.get(
                        String.format("%s/%s", consentFileLocalPathRoot, consentDocument.get().getEncryptedFileUri())
                    ).toString(),
                    consentDocument.get().getEncryptionPhrase(),
                    decompressedConsentFileTempDirectory
                );

                consentFile = new File(String.format("%s/%s", decompressedConsentFileTempDirectory, consentDocument.get().getEncodedFileName()));
            } catch (FileStorageException e) {
                throw e;
            }

            return new UrlResource(consentFile.toPath().toUri());
        } catch (MalformedURLException ex) {
            throw new MyFileNotFoundException("Error 126: Consent file not found for experiment ID: '{}'" + experimentId, ex);
        }
    }

    private FileInfoDto uploadFile(MultipartFile file, long experimentId) {
        FileSubmissionLocal fileSubmissionLocal = saveConsentFile(file, experimentId);

        FileInfoDto fileInfoDto = new FileInfoDto();
        fileInfoDto.setFileId(null);
        fileInfoDto.setDateCreated(Timestamp.valueOf(LocalDateTime.now()));
        fileInfoDto.setExperimentId(experimentId);
        fileInfoDto.setFileType(file.getContentType());
        fileInfoDto.setSize(file.getSize());
        fileInfoDto.setDateUpdated(fileInfoDto.getDateCreated());
        fileInfoDto.setFileSubmissionLocal(fileSubmissionLocal);

        return fileInfoDto;
    }

    @Override
    public FileSubmissionLocal saveConsentFile(MultipartFile file, long experimentId) {
        if (file == null) {
            throw new FileStorageException("Error 140: File cannot be null.");
        }

        String path = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd/HH"));
        String filename = file.getOriginalFilename() != null ? StringUtils.cleanPath(file.getOriginalFilename()) : "";

        if (filename.contains("..")) {
            throw new FileStorageException(String.format("Error 139: filename contains invalid path sequence: '%s'", filename));
        }

        try {
            // create the upload directory
            Files.createDirectories(Paths.get(String.format("%s/%s", consentFileLocalPathRoot, path)));
            // assign random UUID as file name
            String filePath = String.format("%s/%s", path, UUID.randomUUID().toString());

            while (Files.exists(Paths.get(filePath))) {
                // ensure no file name clashes
                filePath = String.format("%s/%s", path, UUID.randomUUID().toString());
            }

            // copy the file to the directory
            Files.copy(
                file.getInputStream(),
                Paths.get(String.format("%s/%s", consentFileLocalPathRoot, filePath)),
                StandardCopyOption.REPLACE_EXISTING
            );

            String encryptionPhrase = UUID.randomUUID().toString();

            FileSubmissionLocal fileSubmissionLocal =  new FileSubmissionLocal(
                filePath,
                compressFile(
                    Paths.get(String.format("%s/%s", consentFileLocalPathRoot, filePath)).toString(),
                    encryptionPhrase,
                    ConsentDocument.COMPRESSED_FILE_EXTENSION
                ),
                EncryptionMethod.AES.toString(),
                encryptionPhrase
            );

            if (!fileSubmissionLocal.isCompressed()) {
                // file was not compressed; return
                return fileSubmissionLocal;
            }

            // delete the original file
            Files.deleteIfExists(Paths.get(String.format("%s/%s", consentFileLocalPathRoot, filePath)));

            return fileSubmissionLocal;
        } catch (IOException ex) {
            throw new FileStorageException(String.format("Error 140: Could not store file '%s'. Please try again.", filename), ex);
        }
    }

    @Override
    public FileInfoDto uploadConsentFile(long experimentId, String title, MultipartFile multipartFile, SecuredInfo securedInfo)
            throws AssignmentNotCreatedException, ApiException, AssignmentNotEditedException, AssignmentNotMatchingException, IOException, TerracottaConnectorException {
        FileInfoDto fileInfoDto = uploadFile(multipartFile, experimentId);
        Experiment experiment = experimentRepository.findByExperimentId(experimentId);
        ConsentDocument consentDocument = experiment.getConsentDocument();
        LtiUserEntity instructorUser = ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        String lmsCourseId = org.apache.commons.lang3.StringUtils.substringBetween(experiment.getLtiContextEntity().getContext_memberships_url(), "courses/", "/names");

        if (consentDocument == null) {
            consentDocument = new ConsentDocument();
            //consentDocument.setFilePointer(fileInfoDto.getUrl());
            consentDocument.setExperiment(experiment);
            consentDocument.setTitle(title);
        }

        consentDocument.setEncryptionMethod(fileInfoDto.getFileSubmissionLocal().getEncryptionMethod());
        consentDocument.setEncryptionPhrase(fileInfoDto.getFileSubmissionLocal().getEncryptionPhrase());
        consentDocument.setFileUri(fileInfoDto.getFileSubmissionLocal().getFilePath());

        // reset to null, as not needed
        fileInfoDto.setFileSubmissionLocal(null);

        //Let's see if we have the assignment generated in LMS
        if (consentDocument.getLmsAssignmentId() == null) {
            try {
                LmsAssignment lmsAssignment = apiClient.uploadConsentFile(experiment, consentDocument, instructorUser);

                consentDocument.setLmsAssignmentId(lmsAssignment.getId());
                // consentDocument.setResourceLinkId(assignment.get().getExternalToolTagAttributes().getResourceLinkId());
                // log.debug("getExternalToolTagAttributes().getResourceLinkId()={}", assignment.get().getExternalToolTagAttributes().getResourceLinkId());
                // This seems to be a more accurate way to get the resourceLinkId
                String jwtTokenAssignment = lmsAssignment.getSecureParams();
                String resourceLinkId = apijwtService.unsecureToken(jwtTokenAssignment, experiment.getPlatformDeployment()).get("lti_assignment_id").toString();
                log.debug("jwtTokenAssignment lti_assignment_id = {}", resourceLinkId);
                consentDocument.setResourceLinkId(resourceLinkId);
            } catch (ApiException e) {
                throw new AssignmentNotCreatedException("Error 137: The consent document assignment was not created.");
            }
        } else {
            Optional<LmsAssignment> lmsAssignment = apiClient.listAssignment(instructorUser, lmsCourseId, consentDocument.getLmsAssignmentId());

            if (lmsAssignment.isEmpty()) {
                throw new AssignmentNotEditedException("Error 136: The assignment is not linked to any LMS assignment");
            }

            lmsAssignment.get().setName(title);
            apiClient.editAssignment(instructorUser, lmsAssignment.get(), lmsCourseId);
            consentDocument.setTitle(title);
        }

        consentDocument = consentDocumentRepository.save(consentDocument);
        experiment.setConsentDocument(consentDocument);
        experimentRepository.saveAndFlush(experiment);

        return fileInfoDto;
    }

    @Override
    public void deleteConsentFile(long experimentId) {
        Optional<ConsentDocument> consentDocument = consentDocumentRepository.findByExperiment_ExperimentId(experimentId);

        if (consentDocument.isEmpty()) {
            return;
        }

        try {
            Files.deleteIfExists(Paths.get(String.format("%s/%s", consentFileLocalPathRoot, consentDocument.get().getFileUri())));
        } catch (IOException ex) {
            throw new FileStorageException(String.format("Error 140: Could not delete file for experiment ID: '%s'.", experimentId), ex);
        }
    }

    @Override
    public void deleteConsentAssignment(long experimentId, SecuredInfo securedInfo) throws AssignmentNotEditedException, ApiException, IOException, NumberFormatException, TerracottaConnectorException {
        Experiment experiment = experimentRepository.findByExperimentId(experimentId);
        LtiUserEntity instructorUser = ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        ConsentDocument consentDocument = experiment.getConsentDocument();

        if (consentDocument == null) {
            return;
        }

        Optional<LmsAssignment> lmsAssignment = apiClient.listAssignment(instructorUser, securedInfo.getLmsCourseId(), consentDocument.getLmsAssignmentId());

        if (lmsAssignment.isPresent()) {
            apiClient.deleteAssignmentInLms(lmsAssignment.get(), securedInfo.getLmsCourseId(), instructorUser);
        }

        deleteConsentFile(experimentId);
        experiment.setConsentDocument(null);
        consentDocumentRepository.deleteById(consentDocument.getConsentDocumentId());
    }

    @Override
    public String parseHTMLFiles(String html, String localUrl) {
        if (org.apache.commons.lang3.StringUtils.isNotBlank(html)) {
            Document doc = Jsoup.parse(html);
            parseAndUpdateElements(doc, "src", "/files/", "?token=", true, localUrl);
            parseAndUpdateElements(doc, "href", "/files/", "?token=", true, localUrl);
            parseAndUpdateElements(doc, "src", "/api/experiments/", "/files/", false, localUrl);
            parseAndUpdateElements(doc, "href", "/api/experiments/", "/files/", false, localUrl);

            return doc.body().html();
        }

        return html;
    }

    private void parseAndUpdateElements(Document doc, String attribute, String prefixToSearch, String stringToSearch, boolean alreadyToken, String localUrl) {
        Elements elements = doc.getElementsByAttributeValueStarting(attribute, localUrl + prefixToSearch);

        for (Element element:elements) {
            String originalLink = element.attr(attribute);
            if (originalLink.contains(stringToSearch)) {
                String fileId = org.apache.commons.lang3.StringUtils.substringAfterLast(originalLink,"/");

                if (alreadyToken) {
                    fileId = org.apache.commons.lang3.StringUtils.substringBefore(fileId,"?token=");
                }

                try {
                    String token = apijwtService.buildFileToken(fileId, localUrl);
                    String fileDownloadUrl = localUrl + "/files/" + fileId + "?token=" + token;
                    element.attr(attribute, fileDownloadUrl);
                } catch (GeneralSecurityException gs) {
                    //In case of problem we don't modify anything but it won't fail
                    log.warn("Error when trying to build a file token " + fileId);
                }
            }
        }
    }

    @Override
    public FileSubmissionLocal saveFileSubmissionLocal(MultipartFile file) {
        if (file == null) {
            throw new FileStorageException("Error 140: File cannot be null.");
        }

        String path = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd/HH"));
        String filename = file.getOriginalFilename() != null ? StringUtils.cleanPath(file.getOriginalFilename()) : "";

        if (filename.contains("..")) {
            throw new FileStorageException(String.format("Error 139: filename contains invalid path sequence: '%s'", filename));
        }

        try {
            // create the upload directory
            Files.createDirectories(Paths.get(String.format("%s/%s", uploadSubmissionsLocalPathRoot, path)));
            // assign random UUID as file name
            String filePath = String.format("%s/%s", path, UUID.randomUUID().toString());

            while (Files.exists(Paths.get(filePath))) {
                // ensure no file name clashes
                filePath = String.format("%s/%s", path, UUID.randomUUID().toString());
            }

            // copy the file to the directory
            Files.copy(
                file.getInputStream(),
                Paths.get(String.format("%s/%s", uploadSubmissionsLocalPathRoot, filePath)),
                StandardCopyOption.REPLACE_EXISTING
            );

            String encryptionPhrase = UUID.randomUUID().toString();

            FileSubmissionLocal fileSubmissionLocal =  new FileSubmissionLocal(
                filePath,
                compressFile(
                    Paths.get(String.format("%s/%s", uploadSubmissionsLocalPathRoot, filePath)).toString(),
                    encryptionPhrase,
                    AnswerFileSubmission.COMPRESSED_FILE_EXTENSION
                ),
                EncryptionMethod.AES.toString(),
                encryptionPhrase
            );

            if (!fileSubmissionLocal.isCompressed()) {
                // file was not compressed; return
                return fileSubmissionLocal;
            }

            // delete the original file
            Files.deleteIfExists(Paths.get(String.format("%s/%s", uploadSubmissionsLocalPathRoot, filePath)));

            return fileSubmissionLocal;
        } catch (IOException ex) {
            throw new FileStorageException(String.format("Error 140: Could not store file '%s'. Please try again.", filename), ex);
        }
    }

    @Override
    public File getFileSubmissionLocal(long id) {
        Optional<AnswerFileSubmission> answerFileSubmission = answerFileSubmissionRepository.findById(id);

        if (answerFileSubmission.isEmpty()) {
            return null;
        }

        if (!answerFileSubmission.get().isCompressed()) {
            // file is not compressed; return it as-is
            return new File(String.format("%s/%s", uploadSubmissionsLocalPathRoot, answerFileSubmission.get().getFileUri()));
        }

        // file is compressed; decompress and then return it
        try {
            decompressFile(
                Paths.get(
                    String.format("%s/%s", uploadSubmissionsLocalPathRoot, answerFileSubmission.get().getEncryptedFileUri())
                ).toString(),
                answerFileSubmission.get().getEncryptionPhrase(),
                decompressedSubmissionFileTempDirectory
            );

            return new File(String.format("%s/%s", decompressedSubmissionFileTempDirectory, answerFileSubmission.get().getEncodedFileName()));
        } catch (FileStorageException e) {
            throw e;
        }
    }

    @Override
    public boolean compressFile(String filePathToCompress, String encryptionPhrase, String compressedFileExtension) {
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(true);
        zipParameters.setCompressionLevel(CompressionLevel.ULTRA);
        zipParameters.setEncryptionMethod(EncryptionMethod.AES);

        try (ZipFile zipFile = new ZipFile(filePathToCompress + compressedFileExtension, encryptionPhrase.toCharArray())) {
            zipFile.addFile(new File(filePathToCompress), zipParameters);
        } catch (IOException e) {
            log.error("Error zipping file: {}", filePathToCompress, e);
            return false;
        }

        return true;
    }

    private void decompressFile(String filePathToDecompress, String encryptionPhrase, Path tempDirectory) throws FileStorageException {
        try (ZipFile zipFile = new ZipFile(filePathToDecompress, encryptionPhrase.toCharArray())) {
            zipFile.extractAll(tempDirectory.toString());
        } catch (IOException e) {
            throw new FileStorageException(String.format("Error: Could not decompress file '%s'. Please try again.", filePathToDecompress), e);
        }
    }

}
