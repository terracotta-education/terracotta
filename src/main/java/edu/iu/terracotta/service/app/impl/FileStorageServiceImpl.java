package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.app.FileStorageException;
import edu.iu.terracotta.exceptions.app.MyFileNotFoundException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.AnswerFileSubmission;
import edu.iu.terracotta.model.app.ConsentDocument;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.FileInfo;
import edu.iu.terracotta.model.app.FileSubmissionLocal;
import edu.iu.terracotta.model.app.dto.FileInfoDto;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;
import edu.iu.terracotta.utils.TextConstants;
import edu.ksu.canvas.model.assignment.Assignment;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.annotation.PostConstruct;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@SuppressWarnings({
    "squid:S1192", "squid:S112", "squid:S125", "squid:S2737", "squid:S4449", "squid:S1075",
    "PMD.GuardLogStatement", "PMD.PreserveStackTrace"
})
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${upload.path}")
    private String uploadDir;

    @Value("${upload.submissions.local.path}")
    private String uploadSubmissionsLocalPath;

    @Value("${upload.submissions.local.path.root}")
    private String uploadSubmissionsLocalPathRoot;

    @Autowired
    private ExperimentService experimentService;

    @Autowired
    private CanvasAPIClient canvasAPIClient;

    @Autowired
    private APIJWTService apijwtService;

    @Autowired
    private AllRepositories allRepositories;

    private Path decompressedFileTempDirectory;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            Files.createDirectories(Paths.get(uploadSubmissionsLocalPathRoot));
            this.decompressedFileTempDirectory = Files.createTempDirectory(Paths.get(uploadSubmissionsLocalPath).toString() + ".");
        }catch (IOException e) {
            throw new RuntimeException("Error 138: Could not create upload folder!");
        }
    }

    @Override
    public String storeFile(MultipartFile file, String extraPath, Long experimentId, boolean consent) {
        String fileName = "consent.pdf";
        if (!consent) {
            fileName = StringUtils.cleanPath(file.getOriginalFilename());
        }

        try {
            if(fileName.contains("..")) {
                throw new FileStorageException("Error 139: Sorry, Filename contains invalid path sequence " + fileName);
            }
            String finalPath = uploadDir;
            if (StringUtils.hasText(extraPath)){
                finalPath = finalPath + extraPath + "/";
            }

            if (!Files.exists(Paths.get(finalPath))){
                Files.createDirectories(Paths.get(finalPath));
            }

            Path targetLocation = Paths.get(finalPath).resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException ex) {
            throw new FileStorageException("Error 140: Could not store file " + fileName + ". Please try again.", ex);
        }
    }
    @Override
    public Resource loadFileAsResource(String fileName, String extraPath) {
        try {
            String finalPath = uploadDir;
            if (StringUtils.hasText(extraPath)){
                finalPath = finalPath + extraPath;
            }
            Path filePath = Paths.get(finalPath).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if(resource.exists()) {
                return resource;
            }else {
                throw new MyFileNotFoundException("Error 126: File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new MyFileNotFoundException("Error 126: File not found " + fileName, ex);
        }
    }

    @Override
    public Resource getFileAsResource(String fileId){
        Optional<FileInfo> fileInfo = allRepositories.fileInfoRepository.findByFileId(fileId);
        if(fileInfo.isPresent()){
            try{
                String finalPath = uploadDir + "/" + fileInfo.get().getExperiment().getExperimentId() + "/files/" + fileInfo.get().getFilename();
                Path filePath = Paths.get(finalPath).normalize();
                Resource resource = new UrlResource(filePath.toUri());
                if(resource.exists()){
                    return resource;
                } else {
                    throw new MyFileNotFoundException("Error 126: File not found.");
                }
            } catch (MalformedURLException ex) {
                throw new MyFileNotFoundException("Error 126: File not found.", ex);
            }
        } else {
            throw new MyFileNotFoundException("Error 126: File not found in repository.");
        }
    }

    @Override
    public void saveFile(MultipartFile multipartFile, String extraPath, Long experimentId){
        FileInfo file = new FileInfo();
        file.setFilename(extraPath + "/" + multipartFile.getOriginalFilename());
        file.setFileId(UUID.randomUUID().toString());
        file.setFileType(multipartFile.getContentType());
        file.setSize(multipartFile.getSize());
        Optional<Experiment> experiment = experimentService.findById(experimentId);
        experiment.ifPresent(file::setExperiment);
        allRepositories.fileInfoRepository.save(file);
    }

    @Override
    public boolean deleteFile(String fileName, String extraPath) {
        try {
            String finalPath = uploadDir;

            if (StringUtils.hasText(extraPath)){
                finalPath = finalPath + extraPath;
            }

            Path filePath = Paths.get(finalPath).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if(resource.exists()) {
                return Files.deleteIfExists(filePath);
            }

            throw new MyFileNotFoundException("Error 126: File not found " + fileName);
        } catch (IOException ex) {
            throw new MyFileNotFoundException("Error 126: File not found.", ex);
        }
    }

    @Override
    public boolean deleteByFileId(String fileId) {
        try{
            Optional<FileInfo> fileInfo = allRepositories.fileInfoRepository.findById(fileId);
            if (fileInfo.isPresent()) {
                String finalPath = uploadDir + "/" + fileInfo.get().getExperiment().getExperimentId() + "/files/" + fileInfo.get().getFilename();
                Path filePath = Paths.get(finalPath).normalize();
                Resource resource = new UrlResource(filePath.toUri());
                if (resource.exists()) {
                    allRepositories.fileInfoRepository.deleteByFileId(fileId);
                    return Files.deleteIfExists(filePath);
                } else {
                    throw new MyFileNotFoundException("Error 126: File not found.");
                }
            } else {
                throw new MyFileNotFoundException("Error 126: File not found.");
            }
        } catch (IOException ex){
            throw new MyFileNotFoundException("Error 126: File Not found.", ex);
        }
    }

    @Override
    public Optional<FileInfo> findByFileId(String fileId) { return allRepositories.fileInfoRepository.findById(fileId); }

    @Override
    public List<FileInfo> findByExperimentId(Long experimentId){ return allRepositories.fileInfoRepository.findByExperiment_ExperimentId(experimentId); }

    @Override
    public FileInfo findByExperimentIdAndFilename(Long experimentId, String filename){
        return allRepositories.fileInfoRepository.findByExperiment_ExperimentIdAndFilename(experimentId, filename);
    }

    @Override
    public FileInfoDto toDto(FileInfo fileInfo) {
        FileInfoDto fileInfoDto = new FileInfoDto();
        fileInfoDto.setFileId(fileInfo.getFileId());
        fileInfoDto.setExperimentId(fileInfo.getExperiment().getExperimentId());
        fileInfoDto.setPath(fileInfo.getFilename());
        fileInfoDto.setDateCreated(fileInfo.getCreatedAt());
        fileInfoDto.setDateUpdated(fileInfo.getUpdatedAt());
        fileInfoDto.setFileType(fileInfo.getFileType());
        fileInfoDto.setSize(fileInfo.getSize());
        fileInfoDto.setUrl(ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/experiments/" + fileInfoDto.getExperimentId() + "/files/" + fileInfo.getFileId()).build().toUriString());
        try {
            fileInfoDto.setTempToken(apijwtService.buildFileToken(fileInfo.getFileId(), fileInfo.getExperiment().getPlatformDeployment().getLocalUrl()));
        } catch (GeneralSecurityException ex) {
            //This shouldn't happen, but if it happens we just want a  warning in the log
            log.warn("Error generating the file token: " + fileInfo.getFileId() + " : " + ex.getMessage());
        }
        return fileInfoDto;
    }

    @Override
    public FileInfoDto uploadFile(MultipartFile file, String prefix, String extraPath, long experimentId, boolean consent) {
        String path = prefix + extraPath;
        String fileName = storeFile(file, path, experimentId, consent);
        FileInfoDto fileInfoDto = new FileInfoDto();
        if (consent) {
            fileInfoDto.setFileId(null);
            fileInfoDto.setDateCreated(Timestamp.valueOf(LocalDateTime.now()));
            fileInfoDto.setExperimentId(experimentId);
            fileInfoDto.setUrl(ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/experiments" + prefix + extraPath).build().toUriString());
            fileInfoDto.setFileType(file.getContentType());
            fileInfoDto.setSize(file.getSize());
            fileInfoDto.setDateUpdated(fileInfoDto.getDateCreated());
        } else {
            FileInfo fileInfo = findByExperimentIdAndFilename(experimentId, extraPath + "/" + fileName);
            fileInfoDto.setFileId(fileInfo.getFileId());
            fileInfoDto.setExperimentId(experimentId);
            fileInfoDto.setPath(fileInfo.getFilename());
            fileInfoDto.setSize(fileInfo.getSize());
            fileInfoDto.setFileType(fileInfo.getFileType());
            fileInfoDto.setDateCreated(fileInfo.getCreatedAt());
            fileInfoDto.setDateUpdated(fileInfo.getUpdatedAt());
            fileInfoDto.setUrl(ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/experiments" + prefix + fileInfo.getFileId()).build().toUriString());
        }
        return fileInfoDto;
    }

    @Override
    public List<FileInfoDto> getFiles(long experimentId) {
        List<FileInfo> fileInfoList = findByExperimentId(experimentId);
        List<FileInfoDto> fileInfoDtoList = new ArrayList<>();
        for(FileInfo fileInfo : fileInfoList){
            fileInfoDtoList.add(toDto(fileInfo));
        }
        return fileInfoDtoList;
    }

    @Override
    public void uploadConsent(long experimentId, String title, FileInfoDto consentUploaded, SecuredInfo securedInfo)
            throws AssignmentNotCreatedException, CanvasApiException, AssignmentNotEditedException, AssignmentNotMatchingException {
        Experiment experiment = experimentService.getExperiment(experimentId);
        ConsentDocument consentDocument = experiment.getConsentDocument();
        LtiUserEntity instructorUser = allRepositories.ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        String canvasCourseId = org.apache.commons.lang3.StringUtils.substringBetween(experiment.getLtiContextEntity().getContext_memberships_url(), "courses/", "/names");

        if (consentDocument == null) {
            consentDocument = new ConsentDocument();
            consentDocument.setFilePointer(consentUploaded.getUrl());
            consentDocument.setExperiment(experiment);
            consentDocument.setTitle(title);
        } else {
            consentDocument.setFilePointer(consentUploaded.getUrl());
        }

        //Let's see if we have the assignment generated in Canvas
        if (consentDocument.getLmsAssignmentId() == null) {
            AssignmentExtended canvasAssignment = new AssignmentExtended();
            Assignment.ExternalToolTagAttribute canvasExternalToolTagAttributes = canvasAssignment.new ExternalToolTagAttribute();
            canvasExternalToolTagAttributes.setUrl(ServletUriComponentsBuilder.fromCurrentContextPath().path("/lti3?consent=true&experiment=" + experimentId).build().toUriString());
            canvasAssignment.setExternalToolTagAttributes(canvasExternalToolTagAttributes);
            canvasAssignment.setName(title);
            canvasAssignment.setDescription("You are being asked to participate in a research study.  " +
                    "Please read the statement below, and then select your response.  " +
                    "Your teacher will be able to see whether you submitted a response, but will not be able to see your selection.");
            canvasAssignment.setPublished(false);
            canvasAssignment.setGradingType("points");
            canvasAssignment.setPointsPossible(1.0);
            canvasAssignment.setSubmissionTypes(Collections.singletonList("external_tool"));

            try {
                Optional<AssignmentExtended> assignment = canvasAPIClient.createCanvasAssignment(instructorUser,
                        canvasAssignment, canvasCourseId);

                if (!assignment.isPresent()) {
                    throw new AssignmentNotMatchingException(TextConstants.ASSIGNMENT_NOT_MATCHING);
                }

                consentDocument.setLmsAssignmentId(Integer.toString(assignment.get().getId()));
                // consentDocument.setResourceLinkId(assignment.get().getExternalToolTagAttributes().getResourceLinkId());
                // log.debug("getExternalToolTagAttributes().getResourceLinkId()={}", assignment.get().getExternalToolTagAttributes().getResourceLinkId());
                // This seems to be a more accurate way to get the resourceLinkId
                String jwtTokenAssignment = assignment.get().getSecureParams();
                String resourceLinkId = apijwtService.unsecureToken(jwtTokenAssignment).getBody().get("lti_assignment_id").toString();
                log.debug("jwtTokenAssignment lti_assignment_id = {}", resourceLinkId);
                consentDocument.setResourceLinkId(resourceLinkId);
            } catch (CanvasApiException e) {
                log.error("Create the assignment failed " + e.getMessage());
                throw new AssignmentNotCreatedException("Error 137: The assignment was not created.");
            }
        } else {
            String lmsId = consentDocument.getLmsAssignmentId();
            Optional<AssignmentExtended> assignmentExtendedOptional = canvasAPIClient.listAssignment(instructorUser, canvasCourseId, Integer.parseInt(lmsId));

            if (!assignmentExtendedOptional.isPresent()) {
                throw new AssignmentNotEditedException("Error 136: The assignment is not linked to any Canvas assignment");
            }

            AssignmentExtended assignmentExtended = assignmentExtendedOptional.get();
            assignmentExtended.setName(title);
            canvasAPIClient.editAssignment(instructorUser, assignmentExtended, canvasCourseId);
            consentDocument.setTitle(title);
        }

        consentDocument = experimentService.saveConsentDocument(consentDocument);
        experiment.setConsentDocument(consentDocument);
        experimentService.saveAndFlush(experiment);
    }

    @Override
    public void deleteConsentAssignment(long experimentId, SecuredInfo securedInfo) throws AssignmentNotEditedException, CanvasApiException {
        Experiment experiment = experimentService.getExperiment(experimentId);
        LtiUserEntity instructorUser = allRepositories.ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        ConsentDocument consentDocument = experiment.getConsentDocument();

        if (consentDocument!=null) {
            String lmsId = consentDocument.getLmsAssignmentId();
            Optional<AssignmentExtended> assignmentExtendedOptional = canvasAPIClient.listAssignment(instructorUser,
                    securedInfo.getCanvasCourseId(), Integer.parseInt(lmsId));

            if (assignmentExtendedOptional.isPresent()) {
                AssignmentExtended assignmentExtended = assignmentExtendedOptional.get();
                canvasAPIClient.deleteAssignment(instructorUser, assignmentExtended, securedInfo.getCanvasCourseId());
            }
        }
    }

    @Override
    public String parseHTMLFiles (String html, String localUrl) {

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
        for (Element element:elements){
            String originalLink = element.attr(attribute);
            if (originalLink.contains(stringToSearch)){
                String fileId = org.apache.commons.lang3.StringUtils.substringAfterLast(originalLink,"/");
                if (alreadyToken){
                    fileId = org.apache.commons.lang3.StringUtils.substringBefore(fileId,"?token=");
                }
                try {
                    String token = apijwtService.buildFileToken(fileId, localUrl);
                    String fileDownloadUrl = localUrl + "/files/" + fileId + "?token=" + token;
                    element.attr(attribute, fileDownloadUrl);
                } catch (GeneralSecurityException gs){
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
                    encryptionPhrase
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
        Optional<AnswerFileSubmission> answerFileSubmission = allRepositories.answerFileSubmissionRepository.findById(id);

        if (!answerFileSubmission.isPresent()) {
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
                answerFileSubmission.get().getEncryptionPhrase()
            );

            return new File(String.format("%s/%s", decompressedFileTempDirectory, answerFileSubmission.get().getEncodedFileName()));
        } catch (FileStorageException e) {
            throw e;
        }
    }

    private boolean compressFile(String filePathToCompress, String encryptionPhrase) {
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(true);
        zipParameters.setCompressionLevel(CompressionLevel.ULTRA);
        zipParameters.setEncryptionMethod(EncryptionMethod.AES);

        try (ZipFile zipFile = new ZipFile(filePathToCompress + AnswerFileSubmission.COMPRESSED_FILE_EXTENSION, encryptionPhrase.toCharArray())) {
            zipFile.addFile(new File(filePathToCompress), zipParameters);
        } catch (IOException e) {
            log.error("Error zipping file: {}", filePathToCompress, e);
            return false;
        }

        return true;
    }

    private void decompressFile(String filePathToDecompress, String encryptionPhrase) throws FileStorageException {
        try (ZipFile zipFile = new ZipFile(filePathToDecompress, encryptionPhrase.toCharArray())) {
            zipFile.extractAll(decompressedFileTempDirectory.toString());
        } catch (IOException e) {
            throw new FileStorageException(String.format("Error: Could not decompress file '%s'. Please try again.", filePathToDecompress), e);
        }
    }

}
