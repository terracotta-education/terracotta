package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.app.FileStorageException;
import edu.iu.terracotta.exceptions.app.MyFileNotFoundException;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.FileInfo;
import edu.iu.terracotta.model.app.dto.FileInfoDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.FileStorageService;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${upload.path}")
    private String uploadDir;

    @Autowired
    ExperimentService experimentService;

    @Autowired
    AllRepositories allRepositories;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        }catch (IOException e) {
            throw new RuntimeException("Could not create upload folder!");
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
                throw new FileStorageException("Sorry, Filename contains invalid path sequence " + fileName);
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
            throw new FileStorageException("Could not store file " + fileName + ". Please try again.", ex);
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
                throw new MyFileNotFoundException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new MyFileNotFoundException("File not found " + fileName, ex);
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
                    throw new MyFileNotFoundException("File not found.");
                }
            } catch (MalformedURLException ex) {
                throw new MyFileNotFoundException("File not found.", ex);
            }
        } else {
            throw new MyFileNotFoundException("File not found in repository.");
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
                throw new MyFileNotFoundException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new MyFileNotFoundException("File not found.", ex);
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
                    throw new MyFileNotFoundException("File not found.");
                }
            } else {
                throw new MyFileNotFoundException("File not found.");
            }
        } catch (MalformedURLException ex){
            throw new MyFileNotFoundException("File Not found.", ex);
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
}
