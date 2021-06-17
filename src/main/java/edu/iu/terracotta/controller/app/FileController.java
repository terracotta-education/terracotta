package edu.iu.terracotta.controller.app;

import com.google.common.net.HttpHeaders;
import edu.iu.terracotta.exceptions.BadConsentFileTypeException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.app.MyFileNotFoundException;
import edu.iu.terracotta.model.app.ConsentDocument;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.FileInfo;
import edu.iu.terracotta.model.app.dto.FileInfoDto;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;
import edu.iu.terracotta.utils.TextConstants;
import edu.ksu.canvas.model.assignment.Assignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = FileController.REQUEST_ROOT)
public class FileController {

    static final String REQUEST_ROOT = "api/experiments";
    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    @Autowired
    FileStorageService fileStorageService;

    @Autowired
    APIJWTService apijwtService;

    @Autowired
    ExperimentService experimentService;

    @Autowired
    CanvasAPIClient canvasAPIClient;

    private FileInfoDto uploadFile(MultipartFile file, String prefix, String extraPath, Long experimentId, boolean consent) {
        String path = prefix + extraPath;
        String fileName = fileStorageService.storeFile(file, path, experimentId, consent);
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
            FileInfo fileInfo = fileStorageService.findByExperimentIdAndFilename(experimentId, extraPath + "/" + fileName);
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

    @RequestMapping(value = "/{experiment_id}/consent", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<FileInfoDto> uploadConsentFiles(@RequestParam("consent") MultipartFile file,
                                                        @PathVariable("experiment_id") long experimentId,
                                                         @RequestParam(name = "title", defaultValue = "Invitation to Participate in a Research Study") String title,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, BadConsentFileTypeException {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);

        if (apijwtService.isInstructorOrHigher(securityInfo)) {
            if (!file.getContentType().equals(MediaType.APPLICATION_PDF_VALUE)) {
                throw new BadConsentFileTypeException(TextConstants.BAD_CONSENT_FILETYPE);
            }
            FileInfoDto consentUploaded = uploadFile(file, "/" + experimentId + "/consent", "", experimentId,true);
            //TODO, if we upload a consent we need:
            Optional<Experiment> experimentOptional = experimentService.findById(experimentId);
            if (experimentOptional.isPresent()) {
                Experiment experiment = experimentOptional.get();
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
                    Assignment canvasAssignment = new Assignment();
                    Assignment.ExternalToolTagAttribute canvasExternalToolTagAttributes = canvasAssignment.new ExternalToolTagAttribute();
                    canvasExternalToolTagAttributes.setUrl(ServletUriComponentsBuilder.fromCurrentContextPath().path("/lti3?consent=true&experiment=" + experimentId).build().toUriString());
                    canvasAssignment.setExternalToolTagAttributes(canvasExternalToolTagAttributes);
                    canvasAssignment.setName(title);
                    //TODO: Think about the description of the assignment.
                    canvasAssignment.setDescription(experiment.getDescription());
                    canvasAssignment.setPublished(false);
                    canvasAssignment.setPointsPossible(0.0);
                    canvasAssignment.setSubmissionTypes(Collections.singletonList("external_tool"));
                    try {
                        Optional<Assignment> assignment = canvasAPIClient.createCanvasAssignment(canvasAssignment,experiment.getLtiContextEntity().getContext_memberships_url(), experiment.getPlatformDeployment());
                        consentDocument.setLmsAssignmentId(Integer.toString(assignment.get().getId()));
                        consentDocument.setResourceLinkId(assignment.get().getExternalToolTagAttributes().getResourceLinkId());
                    } catch (CanvasApiException e) {
                        log.info("Create the assignment failed");
                        e.printStackTrace();
                        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                    }

                    log.info("Here we will create the assignment");
                }
                consentDocument = experimentService.saveConsentDocument(consentDocument);
                experiment.setConsentDocument(consentDocument);
                experimentService.saveAndFlush(experiment);
            } else {
                //this will never happen... but... is a good practice to check it.
                throw new ExperimentNotMatchingException("The experiment does not exist in the database");
            }
            return new ResponseEntity<>(consentUploaded, HttpStatus.OK);
        }  else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/files", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<List<FileInfoDto>> uploadFiles(@RequestParam("files") MultipartFile[] files,
                                                        @PathVariable("experiment_id") long experimentId,
                                                        @RequestParam(name = "extra_path", defaultValue = "") String extraPath,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);

        if (apijwtService.isLearnerOrHigher(securityInfo)) {
            for(MultipartFile file : files){
                fileStorageService.saveFile(file, extraPath, experimentId);
            }
            return new ResponseEntity<>(Arrays.stream(files).map(file -> uploadFile(file, "/" + experimentId + "/files/",  extraPath, experimentId,false)).collect(Collectors.toList()), HttpStatus.OK);
        }  else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/files", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<List<FileInfoDto>> getFilesByExperiment(@PathVariable("experiment_id") long experimentId,
                                                                  HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);

        if(apijwtService.isInstructorOrHigher(securityInfo)){
            List<FileInfo> fileInfoList = fileStorageService.findByExperimentId(experimentId);
            if(fileInfoList.isEmpty()){
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            List<FileInfoDto> fileInfoDtoList = new ArrayList<>();
            for(FileInfo fileInfo : fileInfoList){
                fileInfoDtoList.add(fileStorageService.toDto(fileInfo));
            }
            return new ResponseEntity<>(fileInfoDtoList, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/files/{file_id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(@PathVariable("experiment_id") long experimentId,
                                                 @PathVariable("file_id") String fileId,
                                                  HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);

        if (apijwtService.isLearnerOrHigher(securityInfo)) {
            Resource resource = fileStorageService.getFileAsResource(fileId);

            String contentType = null;
            try {
                contentType = req.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            }catch (IOException ex) {
                log.info("Could not determine file type.");
            }

            if(contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        }  else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/consent", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Resource> getConsent(@PathVariable("experiment_id") long experimentId,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);

        if (apijwtService.isLearnerOrHigher(securityInfo)) {
            Resource resource = fileStorageService.loadFileAsResource("consent.pdf", "/" + experimentId + "/consent");

            String contentType = null;
            try {
                contentType = req.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            }catch (IOException ex) {
                log.info("Could not determine file type.");
            }

            if(contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        }  else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/files/{file_id}", method = RequestMethod.DELETE)
    @ResponseBody
    @Transactional
    public ResponseEntity<Void> deleteFile(@PathVariable("experiment_id") long experimentId,
                                           @PathVariable("file_id") String fileId,
                                           HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {
        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);

        if (apijwtService.isInstructorOrHigher(securityInfo)) {
            try {
                Optional<FileInfo> fileInfo = fileStorageService.findByFileId(fileId);
                if(fileInfo.isPresent()){
                    if(fileStorageService.deleteByFileId(fileId)) {
                        return new ResponseEntity<>(HttpStatus.OK);
                    } else {
                        return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
                    }
                } else {
                    throw new MyFileNotFoundException("File not found.");
                }
            } catch (MyFileNotFoundException ex){
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }
        }  else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/consent", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<Void> deleteConsent(@PathVariable("experiment_id") long experimentId,
                                           HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {
        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);

        if (apijwtService.isLearnerOrHigher(securityInfo)) {
            try {
                if (fileStorageService.deleteFile("consent.pdf", "/" + experimentId + "/consent")) {
                    Optional<Experiment> experimentOptional = experimentService.findById(experimentId);
                    if (experimentOptional.isPresent()) {
                        Experiment experiment = experimentOptional.get();
                        ConsentDocument consentDocument = experiment.getConsentDocument();
                        experiment.setConsentDocument(null);
                        experimentService.saveAndFlush(experiment);
                        experimentService.deleteConsentDocument(consentDocument);
                    }
                    return new ResponseEntity<>(HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
                }
            } catch (MyFileNotFoundException ex){
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        }  else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

}
