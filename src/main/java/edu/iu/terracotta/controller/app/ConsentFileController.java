package edu.iu.terracotta.controller.app;

import com.google.common.net.HttpHeaders;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.entity.ConsentDocument;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.FileInfoDto;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.exceptions.BadConsentFileTypeException;
import edu.iu.terracotta.exceptions.BadTokenException;
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

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;

@Slf4j
@Controller
@SuppressWarnings({"squid:S1192"})
@RequestMapping(ConsentFileController.REQUEST_ROOT)
public class ConsentFileController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/consent";

    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private ApiJwtService apijwtService;
    @Autowired private ExperimentService experimentService;
    @Autowired private FileStorageService fileStorageService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = {AssignmentNotCreatedException.class, ApiException.class})
    public ResponseEntity<FileInfoDto> postConsent(@RequestParam("consent") MultipartFile file,
                                                          @PathVariable long experimentId,
                                                          @RequestParam(name = "title", defaultValue = "Invitation to Participate in a Research Study") String title,
                                                          HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, BadConsentFileTypeException, AssignmentNotCreatedException, ApiException, AssignmentNotEditedException, AssignmentNotMatchingException, IOException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        if (!MediaType.APPLICATION_PDF_VALUE.equals(file.getContentType())) {
            throw new BadConsentFileTypeException(TextConstants.BAD_CONSENT_FILETYPE);
        }

        return new ResponseEntity<>(fileStorageService.uploadConsentFile(experimentId, title, file, securedInfo), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<Resource> getConsent(@PathVariable long experimentId, HttpServletRequest req) throws ExperimentNotMatchingException, BadTokenException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Resource resource = fileStorageService.getConsentFile(experimentId);
        String contentType = null;

        try {
            contentType = req.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.error("Could not determine file type.");
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", resource.getFilename()))
            .body(resource);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteConsent(@PathVariable long experimentId, HttpServletRequest req) throws ExperimentNotMatchingException, BadTokenException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        fileStorageService.deleteConsentFile(experimentId);

        Optional<Experiment> experimentOptional = experimentRepository.findById(experimentId);

        if (experimentOptional.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.OK);
        }

        Experiment experiment = experimentOptional.get();
        ConsentDocument consentDocument = experiment.getConsentDocument();
        experiment.setConsentDocument(null);
        experimentRepository.saveAndFlush(experiment);
        experimentService.deleteConsentDocument(consentDocument);

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
