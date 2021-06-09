package edu.iu.terracotta.controller.app;

import com.google.common.net.HttpHeaders;
import edu.iu.terracotta.exceptions.BadConsentFileTypeException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.app.MyFileNotFoundException;
import edu.iu.terracotta.model.app.ConsentDocument;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.UploadFile;
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

    private UploadFile uploadFile(MultipartFile file, String extraPath, boolean consent) {
        String fileName = fileStorageService.storeFile(file, extraPath, consent);
        String fileDownloadUri;
        if (consent) {
            fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/experiment" + extraPath).build().toUriString();
        } else {
            fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/experiment" + extraPath).path(fileName).build().toUriString();
        }
        return new UploadFile(fileName, fileDownloadUri, file.getContentType(), file.getSize());
    }

    @RequestMapping(value = "/{experiment_id}/consent", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<UploadFile> uploadConsentFiles(@RequestParam("consent") MultipartFile file,
                                                        @PathVariable("experiment_id") long experimentId,
                                                         @RequestParam(name = "title", defaultValue = "Terracotta Consent") String title,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, BadConsentFileTypeException {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);

        if (apijwtService.isInstructorOrHigher(securityInfo)) {
            if (!file.getContentType().equals(MediaType.APPLICATION_PDF_VALUE)) {
                throw new BadConsentFileTypeException(TextConstants.BAD_CONSENT_FILETYPE);
            }
            UploadFile consentUploaded = uploadFile(file, "/" + experimentId + "/consent", true);
            //TODO, if we upload a consent we need:
            Optional<Experiment> experimentOptional = experimentService.findById(experimentId);
            if (experimentOptional.isPresent()) {
                Experiment experiment = experimentOptional.get();
                ConsentDocument consentDocument = experiment.getConsentDocument();
                if (consentDocument == null){
                    consentDocument = new ConsentDocument();
                    consentDocument.setFilePointer(consentUploaded.getFileDownloadUri());
                    consentDocument.setExperiment(experiment);
                    consentDocument.setTitle(title);
                } else {
                    consentDocument.setFilePointer(consentUploaded.getFileDownloadUri());
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
    public ResponseEntity<List<UploadFile>> uploadFiles(@RequestParam("files") MultipartFile[] files,
                                                        @PathVariable("experiment_id") long experimentId,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);

        if (apijwtService.isLearnerOrHigher(securityInfo)) {
            return new ResponseEntity<>(Arrays.stream(files).map(file -> uploadFile(file, "/" + experimentId + "/", false)).collect(Collectors.toList()), HttpStatus.OK);
        }  else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/files/{file:.+}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(@PathVariable("experiment_id") long experimentId,
                                                 @PathVariable String file,
                                                  HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);

        if (apijwtService.isLearnerOrHigher(securityInfo)) {
            Resource resource = fileStorageService.loadFileAsResource(file, "/" + experimentId);

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

    @RequestMapping(value = "/{experiment_id}/files/{file:.+}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<Void> deleteFile(@PathVariable("experiment_id") long experimentId,
                                           @PathVariable String file,
                                           HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {
        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);

        if (apijwtService.isInstructorOrHigher(securityInfo)) {
            try {
                if (fileStorageService.deleteFile(file, "/" + experimentId)) {
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
