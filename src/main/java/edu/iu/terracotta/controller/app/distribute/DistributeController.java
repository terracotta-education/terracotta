package edu.iu.terracotta.controller.app.distribute;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.controller.app.ExperimentController;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImport;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentImportNotFoundException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.dao.model.dto.distribute.ExportDto;
import edu.iu.terracotta.dao.model.dto.distribute.ImportDto;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.ExperimentExportException;
import edu.iu.terracotta.exceptions.ExperimentImportException;
import edu.iu.terracotta.service.app.distribute.ExperimentExportService;
import edu.iu.terracotta.service.app.distribute.ExperimentImportService;
import edu.iu.terracotta.utils.TextConstants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = ExperimentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class DistributeController {

    public static final String REQUEST_ROOT = "api/experiments";

    @Autowired private ApiJwtService apijwtService;
    @Autowired private ExperimentExportService exportService;
    @Autowired private ExperimentImportService importService;

    @GetMapping("/{id}/export")
    public ResponseEntity<Resource> export(@PathVariable long id, HttpServletRequest req) throws ExperimentNotMatchingException, BadTokenException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        Experiment experiment = apijwtService.experimentAllowed(securedInfo, id);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        try {
            ExportDto transferExportDto = exportService.export(experiment);

            if (transferExportDto.getFile() == null) {
                // error occurred
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
            }

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(transferExportDto.getMimeType()))
                .contentLength(transferExportDto.getFile().length())
                .header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=%s", transferExportDto.getFilename()))
                .body(new InputStreamResource(new FileInputStream(transferExportDto.getFile())));
        } catch (ExperimentExportException | FileNotFoundException e) {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/import")
    public ResponseEntity<ImportDto> importExperiment(@RequestParam("file") MultipartFile file, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ApiException, IOException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        if (!Strings.CI.containsAny(file.getContentType(),"application/zip", "application/x-zip-compressed")) {
            String error = String.format("Invalid MIME type: [%s] for file: [%s]", file.getContentType(), file.getOriginalFilename());
            log.error(error);

            return new ResponseEntity<>(importService.preprocessError(file, error, securedInfo), HttpStatus.ACCEPTED);
        }

        try {
            return new ResponseEntity<>(importService.preprocess(file, securedInfo), HttpStatus.ACCEPTED);
        } catch (ExperimentImportException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/import/{id}/poll")
    public ResponseEntity<ImportDto> poll(@PathVariable UUID id, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException, AssessmentNotMatchingException, NumberFormatException,
                TerracottaConnectorException, IOException, ExposureNotMatchingException, ExperimentImportNotFoundException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        ExperimentImport experimentImport = apijwtService.experimentImportAllowed(securedInfo, id);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            return new ResponseEntity<>(importService.toDto(experimentImport), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/import/poll")
    public ResponseEntity<List<ImportDto>> pollAll(HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException, AssessmentNotMatchingException, NumberFormatException,
                TerracottaConnectorException, IOException, ExposureNotMatchingException, ExperimentImportNotFoundException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            return new ResponseEntity<>(importService.getAll(securedInfo), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/import/{id}/acknowledge")
    public ResponseEntity<ImportDto> acknowledgeError(@PathVariable UUID id, @RequestParam ExperimentImportStatus status, HttpServletRequest req)
        throws ExperimentNotMatchingException, BadTokenException, IOException, ExposureNotMatchingException, NumberFormatException, TerracottaConnectorException, ExperimentImportNotFoundException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        ExperimentImport experimentImport = apijwtService.experimentImportAllowed(securedInfo, id);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            return new ResponseEntity<>(importService.acknowledge(experimentImport, status), HttpStatus.OK);
        } catch (Exception e) {
            log.warn("Error acknowledging status: [{}] of experiment import with ID: [{}]", status, experimentImport.getId());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

}
