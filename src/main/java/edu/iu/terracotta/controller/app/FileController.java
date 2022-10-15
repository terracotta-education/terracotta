package edu.iu.terracotta.controller.app;

import com.google.common.net.HttpHeaders;
import edu.iu.terracotta.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.exceptions.AssignmentNotEditedException;
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

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
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



    @RequestMapping(value = "/{experiment_id}/consent", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<FileInfoDto> uploadConsentFiles(@RequestParam("consent") MultipartFile file,
                                                          @PathVariable("experiment_id") long experimentId,
                                                          @RequestParam(name = "title", defaultValue = "Invitation to Participate in a Research Study") String title,
                                                          HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, BadConsentFileTypeException, AssignmentNotCreatedException, CanvasApiException, AssignmentNotEditedException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (apijwtService.isInstructorOrHigher(securedInfo)) {
            if (!file.getContentType().equals(MediaType.APPLICATION_PDF_VALUE)) {
                throw new BadConsentFileTypeException(TextConstants.BAD_CONSENT_FILETYPE);
            }
            FileInfoDto consentUploaded = fileStorageService.uploadFile(file, "/" + experimentId + "/consent", "", experimentId,true);
            fileStorageService.uploadConsent(experimentId, title, consentUploaded, securedInfo.getUserId());
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

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            for(MultipartFile file : files){
                fileStorageService.saveFile(file, extraPath, experimentId);
            }
            return new ResponseEntity<>(Arrays.stream(files).map(file -> fileStorageService.uploadFile(file, "/" + experimentId + "/files/",  extraPath, experimentId,false)).collect(Collectors.toList()), HttpStatus.OK);
        }  else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/files", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<List<FileInfoDto>> getFilesByExperiment(@PathVariable("experiment_id") long experimentId,
                                                                  HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if(apijwtService.isInstructorOrHigher(securedInfo)){
            List<FileInfoDto> fileInfoList = fileStorageService.getFiles(experimentId);
            if(fileInfoList.isEmpty()){
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(fileInfoList, HttpStatus.OK);
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

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            Resource resource = fileStorageService.getFileAsResource(fileId);
            String contentType = null;
            try {
                contentType = req.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            }catch (IOException ex) {
                log.error("Could not determine file type.");
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

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            Resource resource = fileStorageService.loadFileAsResource("consent.pdf", "/" + experimentId + "/consent");
            String contentType = null;
            try {
                contentType = req.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            }catch (IOException ex) {
                log.error("Could not determine file type.");
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
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (apijwtService.isInstructorOrHigher(securedInfo)) {
            try {
                Optional<FileInfo> fileInfo = fileStorageService.findByFileId(fileId);
                if(fileInfo.isPresent()){
                    if(fileStorageService.deleteByFileId(fileId)) {
                        return new ResponseEntity<>(HttpStatus.OK);
                    } else {
                        return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
                    }
                } else {
                    log.error("Error 126: File not found.");
                    throw new MyFileNotFoundException("Error 126: File not found.");
                }
            } catch (MyFileNotFoundException ex){
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
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
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
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
