package edu.iu.terracotta.controller.app;

import com.google.common.net.HttpHeaders;
import edu.iu.terracotta.exceptions.app.MyFileNotFoundException;
import edu.iu.terracotta.model.app.UploadFile;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
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
import java.util.List;
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

    private UploadFile uploadFile(MultipartFile file, String extraPath) {
        String fileName = fileStorageService.storeFile(file, extraPath);
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/downloadFile/").path(fileName).toUriString();
        return new UploadFile(fileName, fileDownloadUri, file.getContentType(), file.getSize());
    }

    @RequestMapping(value = "/{experiment_id}/files", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<List<UploadFile>> uploadFiles(@RequestParam("files") MultipartFile[] files, @PathVariable("experiment_id") long experimentId, HttpServletRequest req) {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        if (securityInfo==null){
            log.error(TextConstants.BAD_TOKEN);
            return new ResponseEntity(TextConstants.BAD_TOKEN, HttpStatus.UNAUTHORIZED);
        }
        if (!experimentService.experimentBelongsToDeploymentAndCourse(experimentId, securityInfo.getPlatformDeploymentId(), securityInfo.getContextId())){
            return new ResponseEntity(TextConstants.EXPERIMENT_NOT_MATCHING , HttpStatus.UNAUTHORIZED);
        }

        if (apijwtService.isLearnerOrHigher(securityInfo)) {
            return new ResponseEntity<>(Arrays.asList(files).stream().map(file -> uploadFile(file, "/" + experimentId)).collect(Collectors.toList()), HttpStatus.OK);
        }  else {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/files/{file:.+}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(@PathVariable("experiment_id") long experimentId, @PathVariable String file,
                                                  HttpServletRequest req) {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        if (securityInfo==null){
            log.error(TextConstants.BAD_TOKEN);
            return new ResponseEntity(TextConstants.BAD_TOKEN, HttpStatus.UNAUTHORIZED);
        }
        if (!experimentService.experimentBelongsToDeploymentAndCourse(experimentId, securityInfo.getPlatformDeploymentId(), securityInfo.getContextId())){
            return new ResponseEntity(TextConstants.EXPERIMENT_NOT_MATCHING , HttpStatus.UNAUTHORIZED);
        }

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
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/files/{file:.+}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<Void> deleteFile(@PathVariable("experiment_id") long experimentId, @PathVariable String file, HttpServletRequest req) {
        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        if (securityInfo==null){
            log.error(TextConstants.BAD_TOKEN);
            return new ResponseEntity(TextConstants.BAD_TOKEN, HttpStatus.UNAUTHORIZED);
        }
        if (!experimentService.experimentBelongsToDeploymentAndCourse(experimentId, securityInfo.getPlatformDeploymentId(), securityInfo.getContextId())){
            return new ResponseEntity(TextConstants.EXPERIMENT_NOT_MATCHING , HttpStatus.UNAUTHORIZED);
        }

        if (apijwtService.isLearnerOrHigher(securityInfo)) {
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
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
    }

}
