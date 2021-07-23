package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.app.FileStorageException;
import edu.iu.terracotta.exceptions.app.MyFileNotFoundException;
import edu.iu.terracotta.model.app.ConsentDocument;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.FileInfo;
import edu.iu.terracotta.model.app.dto.FileInfoDto;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;
import edu.ksu.canvas.model.assignment.Assignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${upload.path}")
    private String uploadDir;

    final static Logger log = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    @Autowired
    ExperimentService experimentService;

    @Autowired
    CanvasAPIClient canvasAPIClient;

    @Autowired
    AllRepositories allRepositories;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
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
                return filePath.toFile().delete();
            }else {
                throw new MyFileNotFoundException("Error 126: File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
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
                    return filePath.toFile().delete();
                } else {
                    throw new MyFileNotFoundException("Error 126: File not found.");
                }
            } else {
                throw new MyFileNotFoundException("Error 126: File not found.");
            }
        } catch (MalformedURLException ex){
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
    public List<FileInfoDto> getFiles(long experimentId){
        List<FileInfo> fileInfoList = findByExperimentId(experimentId);
        List<FileInfoDto> fileInfoDtoList = new ArrayList<>();
        for(FileInfo fileInfo : fileInfoList){
            fileInfoDtoList.add(toDto(fileInfo));
        }
        return fileInfoDtoList;
    }

    @Override
    public void uploadConsent(long experimentId, String title, FileInfoDto consentUploaded) throws AssignmentNotCreatedException {
        Experiment experiment = experimentService.getExperiment(experimentId);
        ConsentDocument consentDocument = experiment.getConsentDocument();
        if (consentDocument == null){
            consentDocument = new ConsentDocument();
            consentDocument.setFilePointer(consentUploaded.getUrl());
            consentDocument.setExperiment(experiment);
            consentDocument.setTitle(title);
        } else {
            consentDocument.setFilePointer(consentUploaded.getUrl());
        }
        //Let's see if we have the assignment generated in Canvas
        if (consentDocument.getLmsAssignmentId()==null){
            AssignmentExtended canvasAssignment = new AssignmentExtended();
            Assignment.ExternalToolTagAttribute canvasExternalToolTagAttributes = canvasAssignment.new ExternalToolTagAttribute();
            canvasExternalToolTagAttributes.setUrl(ServletUriComponentsBuilder.fromCurrentContextPath().path("/lti3?consent=true&experiment=" + experimentId).build().toUriString());
            canvasAssignment.setExternalToolTagAttributes(canvasExternalToolTagAttributes);
            canvasAssignment.setName(title);
            //TODO: Think about the description of the assignment.
            canvasAssignment.setDescription("You are being asked to participate in a research study.  " +
                    "Please read the statement below, and then select your response.  " +
                    "Your teacher will be able to see whether you submitted a response, but will not be able to see your selection.");
            canvasAssignment.setPublished(false);
            canvasAssignment.setPointsPossible(0.0);
            canvasAssignment.setSubmissionTypes(Collections.singletonList("external_tool"));
            try {
                String canvasCourseId = org.apache.commons.lang3.StringUtils.substringBetween(experiment.getLtiContextEntity().getContext_memberships_url(), "courses/", "/names");
                Optional<AssignmentExtended> assignment = canvasAPIClient.createCanvasAssignment(canvasAssignment,canvasCourseId, experiment.getPlatformDeployment());
                consentDocument.setLmsAssignmentId(Integer.toString(assignment.get().getId()));
                consentDocument.setResourceLinkId(assignment.get().getExternalToolTagAttributes().getResourceLinkId());
            } catch (CanvasApiException e) {
                log.info("Create the assignment failed");
                e.printStackTrace();
                throw new AssignmentNotCreatedException("Error 137: The assignment was not created.");
            }
            log.info("Here we will create the assignment");
        }
        consentDocument = experimentService.saveConsentDocument(consentDocument);
        experiment.setConsentDocument(consentDocument);
        experimentService.saveAndFlush(experiment);
    }
}
