package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.app.FileStorageException;
import edu.iu.terracotta.exceptions.app.MyFileNotFoundException;
import edu.iu.terracotta.service.app.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${upload.path}")
    private String uploadDir;


    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        }catch (IOException e) {
            throw new RuntimeException("Could not create upload folder!");
        }
    }

    @Override
    public String storeFile(MultipartFile file, String extraPath, boolean consent) {
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
                finalPath = finalPath + extraPath;
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
            throw new MyFileNotFoundException("File not found " + fileName, ex);
        }
    }



    /*
    @Override
    public String getFileStorageLocation() {return fileStorageLocation; }

    @Override
    public void setFileStorageLocation(String fileStorageLocation) { this.fileStorageLocation = fileStorageLocation; }
     */
}
