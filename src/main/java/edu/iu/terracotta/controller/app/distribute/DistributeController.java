package edu.iu.terracotta.controller.app.distribute;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.Strings;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
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

import edu.iu.terracotta.security.app.roles.InstructorOrHigher;
import edu.iu.terracotta.security.app.roles.LearnerOrHigher;
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
import edu.iu.terracotta.dao.model.dto.distribute.CopyStatusDto;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentCopyStatus;
import edu.iu.terracotta.dao.model.dto.distribute.ExportDto;
import edu.iu.terracotta.dao.model.dto.distribute.ImportDto;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.ExperimentExportException;
import edu.iu.terracotta.exceptions.ExperimentImportException;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.async.ExperimentCopyRecreationAsyncService;
import edu.iu.terracotta.service.app.distribute.ExperimentCopyCandidateService;
import edu.iu.terracotta.service.app.distribute.ExperimentExportService;
import edu.iu.terracotta.service.app.distribute.ExperimentImportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
    @RequiredArgsConstructor
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = ExperimentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class DistributeController {

    public static final String REQUEST_ROOT = "api/experiments";

    private final ApiJwtService apijwtService;
    private final ExperimentExportService exportService;
    private final ExperimentImportService importService;
    private final ExperimentService experimentService;
    private final ExperimentCopyCandidateService experimentCopyCandidateService;
    private final ExperimentCopyRecreationAsyncService experimentCopyRecreationAsyncService;

    @GetMapping("/{id}/export")
    @LearnerOrHigher
    public ResponseEntity<Resource> export(@PathVariable("id") UUID uuid, HttpServletRequest req) throws ExperimentNotMatchingException, BadTokenException, NumberFormatException, TerracottaConnectorException {
        long id = experimentService.getExperimentIdByUuid(uuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        Experiment experiment = apijwtService.experimentAllowed(securedInfo, id);

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
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(transferExportDto.getFilename(), StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(new FileInputStream(transferExportDto.getFile())));
        } catch (ExperimentExportException | FileNotFoundException e) {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/import")
    @InstructorOrHigher
    public ResponseEntity<ImportDto> importExperiment(@RequestParam("file") MultipartFile file, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ApiException, IOException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);

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
    @InstructorOrHigher
    public ResponseEntity<ImportDto> poll(@PathVariable UUID id, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException, AssessmentNotMatchingException, NumberFormatException,
                TerracottaConnectorException, IOException, ExposureNotMatchingException, ExperimentImportNotFoundException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        ExperimentImport experimentImport = apijwtService.experimentImportAllowed(securedInfo, id);

        try {
            return new ResponseEntity<>(importService.toDto(experimentImport), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/import/poll")
    @InstructorOrHigher
    public ResponseEntity<List<ImportDto>> pollAll(HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException, AssessmentNotMatchingException, NumberFormatException,
                TerracottaConnectorException, IOException, ExposureNotMatchingException, ExperimentImportNotFoundException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);

        try {
            return new ResponseEntity<>(importService.getAll(securedInfo), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/import/{id}/acknowledge")
    @InstructorOrHigher
    public ResponseEntity<ImportDto> acknowledgeError(@PathVariable UUID id, @RequestParam ExperimentImportStatus status, HttpServletRequest req)
        throws ExperimentNotMatchingException, BadTokenException, IOException, ExposureNotMatchingException, NumberFormatException, TerracottaConnectorException, ExperimentImportNotFoundException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        ExperimentImport experimentImport = apijwtService.experimentImportAllowed(securedInfo, id);

        try {
            return new ResponseEntity<>(importService.acknowledge(experimentImport, status), HttpStatus.OK);
        } catch (Exception e) {
            log.warn("Error acknowledging status: [{}] of experiment import with ID: [{}]", status, experimentImport.getId());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/copy-status")
    @InstructorOrHigher
    public ResponseEntity<CopyStatusDto> copyStatus(HttpServletRequest req) throws BadTokenException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);

        return new ResponseEntity<>(experimentCopyCandidateService.getCopyStatus(securedInfo), HttpStatus.OK);
    }

    @PostMapping("/copy-status/acknowledge")
    @InstructorOrHigher
    public ResponseEntity<Void> acknowledgeCopyStatus(HttpServletRequest req) throws BadTokenException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);

        experimentCopyCandidateService.acknowledgeCopyStatus(securedInfo);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    // an instructor launching into a course whose copied experiments failed to recreate (e.g. after
    // following the failure email's instructions to re-approve LMS access) tries again as themselves
    @PostMapping("/copy-status/retry")
    @InstructorOrHigher
    public ResponseEntity<CopyStatusDto> retryCopy(HttpServletRequest req) throws BadTokenException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);

        if (experimentCopyCandidateService.hasFailedForContext(securedInfo.getContextId()) && !experimentCopyCandidateService.hasLmsAuthorization(securedInfo)) {
            // a retry now could only fail again on the missing token, and the failure alert would
            // show before the instructor ever got to re-authorize. Wait: the next launch sends them
            // through authorization, and the retry runs once they're back.
            CopyStatusDto copyStatus = experimentCopyCandidateService.getCopyStatus(securedInfo);
            copyStatus.setStatus(ExperimentCopyStatus.AUTHORIZATION_REQUIRED);

            return new ResponseEntity<>(copyStatus, HttpStatus.OK);
        }

        if (experimentCopyCandidateService.resetFailedForRetry(securedInfo.getContextId())) {
            experimentCopyRecreationAsyncService.recreate(securedInfo.getContextId(), securedInfo.getUserId());
        }

        return new ResponseEntity<>(experimentCopyCandidateService.getCopyStatus(securedInfo), HttpStatus.OK);
    }

}
