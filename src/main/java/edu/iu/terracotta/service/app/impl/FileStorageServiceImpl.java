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
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;
import edu.iu.terracotta.dao.repository.AnswerFileSubmissionRepository;
import edu.iu.terracotta.dao.repository.AssignmentFileArchiveRepository;
import edu.iu.terracotta.dao.repository.ConsentDocumentRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.export.data.ExperimentDataExportRepository;
import edu.iu.terracotta.dao.repository.distribute.ExperimentImportRepository;
import edu.iu.terracotta.exceptions.app.FileStorageException;
import edu.iu.terracotta.exceptions.app.MyFileNotFoundException;
import edu.iu.terracotta.service.app.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import tools.jackson.databind.json.JsonMapper;

import org.apache.commons.io.FileUtils;
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
import java.io.InputStream;
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
    @Autowired private AssignmentFileArchiveRepository assignmentFileArchiveRepository;
    @Autowired private ConsentDocumentRepository consentDocumentRepository;
    @Autowired private ExperimentImportRepository experimentImportRepository;
    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private ExperimentDataExportRepository exportDataRepository;
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

    @Value("${assignment.file.archive.local.path}")
    private String assignmentFileArchiveLocalPath;

    @Value("${assignment.file.archive.local.path.root}")
    private String assignmentFileArchiveLocalPathRoot;

    @Value("${experiment.data.export.local.path}")
    private String experimentDataExportLocalPath;

    @Value("${experiment.data.export.local.path.root}")
    private String experimentDataExportLocalPathRoot;

    @Value("${experiment.export.local.path}")
    private String experimentExportLocalPath;

    @Value("${experiment.export.local.path.root}")
    private String experimentExportLocalPathRoot;

    private Path decompressedSubmissionFileTempDirectory;
    private Path decompressedConsentFileTempDirectory;
    private Path decompressedAssignmentFileArchiveTempDirectory;
    private Path decompressedExperimentDataExportTempDirectory;
    private Path decompressedExperimentImportTempDirectory;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            Files.createDirectories(Paths.get(uploadSubmissionsLocalPathRoot));
            Files.createDirectories(Paths.get(consentFileLocalPathRoot));
            Files.createDirectories(Paths.get(assignmentFileArchiveLocalPathRoot));
            Files.createDirectories(Paths.get(experimentDataExportLocalPathRoot));
            Files.createDirectories(Paths.get(experimentExportLocalPathRoot));
            this.decompressedSubmissionFileTempDirectory = Files.createTempDirectory(Paths.get(uploadSubmissionsLocalPath).toString() + ".");
            this.decompressedConsentFileTempDirectory = Files.createTempDirectory(Paths.get(consentFileLocalPath).toString() + ".");
            this.decompressedAssignmentFileArchiveTempDirectory = Files.createTempDirectory(Paths.get(assignmentFileArchiveLocalPath).toString() + ".");
            this.decompressedExperimentImportTempDirectory = Files.createTempDirectory(Paths.get(experimentExportLocalPath).toString() + ".");
            this.decompressedExperimentDataExportTempDirectory = Files.createTempDirectory(Paths.get(experimentDataExportLocalPath).toString() + ".");
        } catch (IOException e) {
            throw new RuntimeException("Error 138: Could not create upload folders!");
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
        FileSubmissionLocal fileSubmissionLocal = saveConsentFile(file);

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
    public FileSubmissionLocal saveConsentFile(MultipartFile file) {
        String filename = file.getOriginalFilename() != null ? StringUtils.cleanPath(file.getOriginalFilename()) : "";

        try {
            return saveConsentFile(file.getInputStream(), filename);
        } catch (IOException e) {
            throw new FileStorageException(String.format("Error 140: Could not store file [%s]. Please try again.", filename), e);
        }
    }

    @Override
    public FileSubmissionLocal saveConsentFile(InputStream inputStream, String filename) {
        if (inputStream == null) {
            throw new FileStorageException("Error 140: File cannot be null.");
        }

        String path = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd/HH"));

        if (filename.contains("..")) {
            throw new FileStorageException(String.format("Error 139: filename contains invalid path sequence: [%s]", filename));
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
                inputStream,
                Paths.get(String.format("%s/%s", consentFileLocalPathRoot, filePath)),
                StandardCopyOption.REPLACE_EXISTING
            );

            String encryptionPhrase = UUID.randomUUID().toString();

            FileSubmissionLocal fileSubmissionLocal = FileSubmissionLocal.builder()
                .compressed(
                    compressFile(
                        Paths.get(String.format("%s/%s", consentFileLocalPathRoot, filePath)).toString(),
                        encryptionPhrase,
                        ConsentDocument.COMPRESSED_FILE_EXTENSION
                    )
                )
                .encryptionMethod(EncryptionMethod.AES.toString())
                .encryptionPhrase(encryptionPhrase)
                .filePath(filePath)
                .build();

            if (!fileSubmissionLocal.isCompressed()) {
                // file was not compressed; return
                return fileSubmissionLocal;
            }

            // delete the original file
            Files.deleteIfExists(Paths.get(String.format("%s/%s", consentFileLocalPathRoot, filePath)));

            return fileSubmissionLocal;
        } catch (IOException ex) {
            throw new FileStorageException(String.format("Error 140: Could not store file [%s]. Please try again.", filename), ex);
        }
    }

    @Override
    public FileInfoDto uploadConsentFile(long experimentId, String title, MultipartFile multipartFile, SecuredInfo securedInfo)
            throws AssignmentNotCreatedException, ApiException, AssignmentNotEditedException, AssignmentNotMatchingException, IOException, TerracottaConnectorException {
        FileInfoDto fileInfoDto = uploadFile(multipartFile, experimentId);
        Experiment experiment = experimentRepository.findByExperimentId(experimentId);
        ConsentDocument consentDocument = experiment.getConsentDocument();
        LtiUserEntity instructorUser = ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        String lmsCourseId = org.apache.commons.lang3.StringUtils.substringBetween(experiment.getLtiContextEntity().getContext_memberships_url(), "courses/", "/names");

        if (consentDocument == null) {
            consentDocument = new ConsentDocument();
            consentDocument.setExperiment(experiment);
            consentDocument.setTitle(title);
        }

        consentDocument.setEncryptionMethod(fileInfoDto.getFileSubmissionLocal().getEncryptionMethod());
        consentDocument.setEncryptionPhrase(fileInfoDto.getFileSubmissionLocal().getEncryptionPhrase());
        consentDocument.setFileUri(fileInfoDto.getFileSubmissionLocal().getFilePath());

        // reset to null, as not needed
        fileInfoDto.setFileSubmissionLocal(null);

        // check if the assignment is in LMS
        if (consentDocument.getLmsAssignmentId() == null) {
            sendConsentFileToLms(consentDocument, experiment, instructorUser);
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
    public void sendConsentFileToLms(ConsentDocument consentDocument, Experiment experiment, LtiUserEntity instructorUser) throws AssignmentNotCreatedException, IOException, TerracottaConnectorException {
        try {
            LmsAssignment lmsAssignment = apiClient.uploadConsentFile(experiment, consentDocument, instructorUser);

            consentDocument.setLmsAssignmentId(lmsAssignment.getId());
            // This seems to be a more accurate way to get the resourceLinkId
            String jwtTokenAssignment = lmsAssignment.getSecureParams();
            String resourceLinkId = apijwtService.unsecureToken(jwtTokenAssignment, experiment.getPlatformDeployment()).get("lti_assignment_id").toString();
            consentDocument.setResourceLinkId(resourceLinkId);
        } catch (ApiException e) {
            throw new AssignmentNotCreatedException("Error 137: The consent document assignment was not created.");
        }
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
        LtiUserEntity instructorUser = ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
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
    public void deleteFileSubmission(long fileSubmissionId) {
        Optional<AnswerFileSubmission> answerFileSubmission = answerFileSubmissionRepository.findById(fileSubmissionId);

        if (answerFileSubmission.isEmpty()) {
            return;
        }

        deleteFileSubmission(answerFileSubmission.get());
    }

    @Override
    public void deleteFileSubmission(AnswerFileSubmission answerFileSubmission) {
        try {
            Files.deleteIfExists(Paths.get(String.format("%s/%s", uploadSubmissionsLocalPathRoot, answerFileSubmission.getFileUri())));
        } catch (IOException e) {
            throw new FileStorageException(String.format("Error 140: Could not delete file for file submission ID: '%s'.", answerFileSubmission.getAnswerFileSubmissionId()), e);
        }
    }

    @Override
    public void saveAssignmentFileArchive(AssignmentFileArchive assignmentFileArchive, File file) {
        if (file == null) {
            throw new FileStorageException("Error 140: File cannot be null.");
        }

        String path = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd/HH"));

        try {
            // create the upload directory
            Files.createDirectories(Paths.get(String.format("%s/%s", assignmentFileArchiveLocalPathRoot, path)));
            // assign random UUID as file name
            String filePath = String.format("%s/%s", path, UUID.randomUUID().toString());

            while (Files.exists(Paths.get(filePath))) {
                // ensure no file name clashes
                filePath = String.format("%s/%s", path, UUID.randomUUID().toString());
            }

            // copy the file to the directory
            Files.copy(
                FileUtils.openInputStream(file),
                Paths.get(String.format("%s/%s", assignmentFileArchiveLocalPathRoot, filePath)),
                StandardCopyOption.REPLACE_EXISTING
            );

            String encryptionPhrase = UUID.randomUUID().toString();

            assignmentFileArchive.setEncryptionMethod(EncryptionMethod.AES.toString());
            assignmentFileArchive.setEncryptionPhrase(encryptionPhrase);
            assignmentFileArchive.setMimeType(AssignmentFileArchive.MIME_TYPE);
            assignmentFileArchive.setFileUri(filePath);

            compressFile(
                Paths.get(String.format("%s/%s", assignmentFileArchiveLocalPathRoot, filePath)).toString(),
                encryptionPhrase,
                AssignmentFileArchive.COMPRESSED_FILE_EXTENSION
            );
        } catch (IOException ex) {
            throw new FileStorageException(String.format("Error 140: Could not store file '%s'. Please try again.", assignmentFileArchive.getFileName()), ex);
        }
    }

    @Override
    public File getAssignmentFileArchive(long id) {
        Optional<AssignmentFileArchive> assignmentFileArchive = assignmentFileArchiveRepository.findById(id);

        if (assignmentFileArchive.isEmpty()) {
            return null;
        }

        // decompress file and then return it
        try {
            decompressFile(
                Paths.get(
                    String.format(
                        "%s/%s%s",
                        assignmentFileArchiveLocalPathRoot,
                        assignmentFileArchive.get().getFileUri(),
                        AssignmentFileArchive.COMPRESSED_FILE_EXTENSION
                    )
                )
                .toString(),
                assignmentFileArchive.get().getEncryptionPhrase(),
                Path.of(
                    String.format(
                        "%s/%s",
                        decompressedAssignmentFileArchiveTempDirectory,
                        org.apache.commons.lang3.StringUtils.substringBeforeLast(
                            assignmentFileArchive.get().getFileUri(),
                            "/"
                        )
                    )
                )
            );

            return new File(
                String.format(
                    "%s/%s",
                    decompressedAssignmentFileArchiveTempDirectory,
                    assignmentFileArchive.get().getFileUri()
                )
            );
        } catch (FileStorageException e) {
            throw e;
        }
    }

    @Override
    public void saveExperimentDataExport(ExperimentDataExport experimentDataExport, File file) {
        if (file == null) {
            throw new FileStorageException("Error 140: File cannot be null.");
        }

        String path = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd/HH"));

        try {
            // create the upload directory
            Files.createDirectories(Paths.get(String.format("%s/%s", experimentDataExportLocalPathRoot, path)));
            // assign random UUID as file name
            String filePath = String.format("%s/%s", path, UUID.randomUUID().toString());

            while (Files.exists(Paths.get(filePath))) {
                // ensure no file name clashes
                filePath = String.format("%s/%s", path, UUID.randomUUID().toString());
            }

            // copy the file to the directory
            Files.copy(
                FileUtils.openInputStream(file),
                Paths.get(String.format("%s/%s", experimentDataExportLocalPathRoot, filePath)),
                StandardCopyOption.REPLACE_EXISTING
            );

            String encryptionPhrase = UUID.randomUUID().toString();

            experimentDataExport.setEncryptionMethod(EncryptionMethod.AES.toString());
            experimentDataExport.setEncryptionPhrase(encryptionPhrase);
            experimentDataExport.setMimeType(AssignmentFileArchive.MIME_TYPE);
            experimentDataExport.setFileUri(filePath);

            compressFile(
                Paths.get(String.format("%s/%s", experimentDataExportLocalPathRoot, filePath)).toString(),
                encryptionPhrase,
                ExperimentDataExport.COMPRESSED_FILE_EXTENSION
            );
        } catch (IOException ex) {
            throw new FileStorageException(String.format("Error 140: Could not store file '%s'. Please try again.", experimentDataExport.getFileName()), ex);
        }
    }

    @Override
    public File getExperimentDataExport(long id) {
        Optional<ExperimentDataExport> experimentDataExport = exportDataRepository.findById(id);

        if (experimentDataExport.isEmpty()) {
            return null;
        }

        // decompress file and then return it
        try {
            decompressFile(
                Paths.get(
                    String.format(
                        "%s/%s%s",
                        experimentDataExportLocalPathRoot,
                        experimentDataExport.get().getFileUri(),
                        AssignmentFileArchive.COMPRESSED_FILE_EXTENSION
                    )
                )
                .toString(),
                experimentDataExport.get().getEncryptionPhrase(),
                Path.of(
                    String.format(
                        "%s/%s",
                        decompressedExperimentDataExportTempDirectory,
                        org.apache.commons.lang3.StringUtils.substringBeforeLast(
                            experimentDataExport.get().getFileUri(),
                            "/"
                        )
                    )
                )
            );

            return new File(
                String.format(
                    "%s/%s",
                    decompressedExperimentDataExportTempDirectory,
                    experimentDataExport.get().getFileUri()
                )
            );
        } catch (FileStorageException e) {
            throw e;
        }
    }

    @Override
    public void createExperimentExportFile(ExportDto transferExportDto, Export export, String filename) throws IOException {
        // create a directory for the export files
        String path = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd/HH"));
        Path parentPath = Files.createDirectories(Paths.get(String.format("%s/%s/%s", experimentExportLocalPathRoot, path, filename)));
        // create the json file
        File jsonFile = new File(parentPath.toString(), ExperimentImport.JSON_FILE_NAME);
        JsonMapper.builder()
            .build()
            .writeValue(jsonFile, export);

        if (export.getExperiment().getParticipationType() == ParticipationTypes.CONSENT) {
            // experiment is a consent type, include the consent document
            Resource consentResource = getConsentFile(export.getExperiment().getId());

            if (consentResource != null) {
                FileUtils.copyFile(consentResource.getFile(), new File(String.format("%s/consent/%s", parentPath.toString(), ExperimentImport.CONSENT_FILE_NAME)));
            }
        }

        // create the zip file
        compressDirectory(parentPath.toString(), "", ".zip", false);
        File exportFile = new File(parentPath.toString(), filename);
        FileUtils.moveFile(new File(String.format("%s.zip", parentPath.toString())), exportFile);

        transferExportDto.setFile(exportFile);
    }

    @Override
    public File getExperimentImportFile(long id) {
        Optional<ExperimentImport> experimentImport = experimentImportRepository.findById(id);

        if (experimentImport.isEmpty()) {
            return null;
        }

        // decompress file and then return it
        try {
            decompressFile(
                Paths.get(
                    String.format(
                        "%s/%s",
                        experimentExportLocalPathRoot,
                        experimentImport.get().getFileUri()
                    )
                )
                .toString(),
                "",
                Path.of(
                    String.format(
                        "%s/%s",
                        decompressedExperimentImportTempDirectory,
                        org.apache.commons.lang3.StringUtils.substringBeforeLast(
                            experimentImport.get().getFileUri(),
                            "/"
                        )
                    )
                )
            );

            return new File(
                String.format(
                    "%s/%s/%s",
                    decompressedExperimentImportTempDirectory,
                    org.apache.commons.lang3.StringUtils.substringBeforeLast(
                            experimentImport.get().getFileUri(),
                            "/"
                    ),
                    org.apache.commons.lang3.StringUtils.substringBeforeLast(
                        experimentImport.get().getFileName(),
                        ".zip"
                    )
                )
            );
        } catch (FileStorageException e) {
            throw e;
        }
    }

    @Override
    public void saveExperimentImportFile(MultipartFile file, ExperimentImport experimentImport) throws IOException {
        String path = String.format("%s/%s", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd/HH")), UUID.randomUUID().toString());
        Path parentPath = Files.createDirectories(Paths.get(String.format("%s/%s", experimentExportLocalPathRoot, path)));
        String filename = String.format("%s.zip", UUID.randomUUID().toString());
        File storedFile = FileUtils.getFile(parentPath.toFile(), filename);
        file.transferTo(storedFile.toPath());

        experimentImport.setFileUri(String.format("%s/%s", path, filename));
    }

    @Override
    public boolean compressFile(String filePathToCompress, String encryptionPhrase, String compressedFileExtension) {
        return compressFile(filePathToCompress, encryptionPhrase, compressedFileExtension, true);
    }

    @Override
    public boolean compressFile(String filePathToCompress, String encryptionPhrase, String compressedFileExtension, boolean encrypt) {
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(encrypt);
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

    @Override
    public boolean compressDirectory(String filePathToCompress, String encryptionPhrase, String compressedFileExtension) {
        return compressDirectory(filePathToCompress, encryptionPhrase, compressedFileExtension, true);
    }

    @Override
    public boolean compressDirectory(String filePathToCompress, String encryptionPhrase, String compressedFileExtension, boolean encrypt) {
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(encrypt);
        zipParameters.setCompressionLevel(CompressionLevel.ULTRA);
        zipParameters.setEncryptionMethod(EncryptionMethod.AES);

        try (ZipFile zipFile = new ZipFile(filePathToCompress + compressedFileExtension, encryptionPhrase.toCharArray())) {
            zipFile.addFolder(new File(filePathToCompress), zipParameters);
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
