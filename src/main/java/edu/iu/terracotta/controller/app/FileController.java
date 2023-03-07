package edu.iu.terracotta.controller.app;

import com.google.common.net.HttpHeaders;
import edu.iu.terracotta.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadConsentFileTypeException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.app.MyFileNotFoundException;
import edu.iu.terracotta.model.app.ConsentDocument;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.FileInfo;
import edu.iu.terracotta.model.app.dto.FileInfoDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping(FileController.REQUEST_ROOT)
@SuppressWarnings({"squid:S1192"})
public class FileController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}";

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private APIJWTService apijwtService;

    @Autowired
    private ExperimentService experimentService;

    @PostMapping(value = "/consent", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = {AssignmentNotCreatedException.class, CanvasApiException.class})
    public ResponseEntity<FileInfoDto> uploadConsentFiles(@RequestParam("consent") MultipartFile file,
                                                          @PathVariable long experimentId,
                                                          @RequestParam(name = "title", defaultValue = "Invitation to Participate in a Research Study") String title,
                                                          HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, BadConsentFileTypeException, AssignmentNotCreatedException, CanvasApiException, AssignmentNotEditedException, AssignmentNotMatchingException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        if (!MediaType.APPLICATION_PDF_VALUE.equals(file.getContentType())) {
            throw new BadConsentFileTypeException(TextConstants.BAD_CONSENT_FILETYPE);
        }

        FileInfoDto consentUploaded = fileStorageService.uploadFile(file, "/" + experimentId + "/consent", "", experimentId,true);
        fileStorageService.uploadConsent(experimentId, title, consentUploaded, securedInfo);

        return new ResponseEntity<>(consentUploaded, HttpStatus.OK);
    }

    @PostMapping(value = "/files", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<FileInfoDto>> uploadFiles(@RequestParam("files") MultipartFile[] files,
                                                        @PathVariable long experimentId,
                                                        @RequestParam(name = "extra_path", defaultValue = "") String extraPath,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        for(MultipartFile file : files){
            fileStorageService.saveFile(file, extraPath, experimentId);
        }

        return new ResponseEntity<>(
            Arrays.stream(files)
                .map(
                    file ->
                        fileStorageService.uploadFile(file, "/" + experimentId + "/files/",  extraPath, experimentId, false)
                )
                .toList(),
            HttpStatus.OK
        );
    }

    @GetMapping("/files")
    public ResponseEntity<List<FileInfoDto>> getFilesByExperiment(@PathVariable long experimentId, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        List<FileInfoDto> fileInfoList = fileStorageService.getFiles(experimentId);

        if(fileInfoList.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(fileInfoList, HttpStatus.OK);
    }

    @GetMapping("/files/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable long experimentId,
                                                 @PathVariable String fileId,
                                                  HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Resource resource = fileStorageService.getFileAsResource(fileId);
        String contentType = null;

        try {
            contentType = req.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.error("Could not determine file type.");
        }

        if(contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/consent")
    public ResponseEntity<Resource> getConsent(@PathVariable long experimentId, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Resource resource = fileStorageService.loadFileAsResource("consent.pdf", "/" + experimentId + "/consent");
        String contentType = null;

        try {
            contentType = req.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.error("Could not determine file type.");
        }

        if(contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
            .body(resource);
    }

    @Transactional
    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable long experimentId,
                                           @PathVariable String fileId,
                                           HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Optional<FileInfo> fileInfo = fileStorageService.findByFileId(fileId);

        if (!fileInfo.isPresent()) {
            return new ResponseEntity<>(HttpStatus.OK);
        }

        if(fileStorageService.deleteByFileId(fileId)) {
            return new ResponseEntity<>(HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
    }

    @DeleteMapping("/consent")
    public ResponseEntity<Void> deleteConsent(@PathVariable long experimentId, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            if (!fileStorageService.deleteFile("consent.pdf", "/" + experimentId + "/consent")) {
                return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
            }

            Optional<Experiment> experimentOptional = experimentService.findById(experimentId);

            if (experimentOptional.isPresent()) {
                Experiment experiment = experimentOptional.get();
                ConsentDocument consentDocument = experiment.getConsentDocument();
                experiment.setConsentDocument(null);
                experimentService.saveAndFlush(experiment);
                experimentService.deleteConsentDocument(consentDocument);
            }

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (MyFileNotFoundException ex){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}
