package edu.iu.terracotta.controller.app.export.data;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.export.data.ExperimentDataExportDto;
import edu.iu.terracotta.dao.model.enums.export.data.ExperimentDataExportStatus;
import edu.iu.terracotta.exceptions.AssignmentFileArchiveNotFoundException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.service.app.export.data.ExperimentDataExportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@SuppressWarnings({"PMD.GuardLogStatement"})
@RequestMapping(value = ExperimentDataExportController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ExperimentDataExportController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/export/data";

    @Autowired private ApiJwtService apijwtService;
    @Autowired private ExperimentDataExportService experimentDataExportService;

    @GetMapping
    public ResponseEntity<ExperimentDataExportDto> process(@PathVariable long experimentId, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, NumberFormatException, IOException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            return new ResponseEntity<>(experimentDataExportService.process(apijwtService.experimentAllowed(securedInfo, experimentId), securedInfo), HttpStatus.ACCEPTED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/poll")
    public ResponseEntity<ExperimentDataExportDto> poll(@PathVariable long experimentId, @RequestParam(defaultValue = "false") boolean createNewOnOutdated, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, NumberFormatException, IOException, TerracottaConnectorException, AssignmentFileArchiveNotFoundException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            return new ResponseEntity<>(experimentDataExportService.poll(apijwtService.experimentAllowed(securedInfo, experimentId), securedInfo, createNewOnOutdated), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/poll/list")
    public ResponseEntity<List<ExperimentDataExportDto>> pollList(@PathVariable long experimentId, @RequestParam(defaultValue = "false") boolean createNewOnOutdated, @RequestBody List<Long> experimentIds, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, NumberFormatException, IOException, TerracottaConnectorException, AssignmentFileArchiveNotFoundException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            List<Experiment> experiments = new ArrayList<>();

            for (Long id : experimentIds) {
                experiments.add(apijwtService.experimentAllowed(securedInfo, id));
            }

            return new ResponseEntity<>(experimentDataExportService.poll(experiments, securedInfo, createNewOnOutdated), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{fileId}/retrieve")
    public ResponseEntity<Resource> retrieve(@PathVariable long experimentId, @PathVariable UUID fileId, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, NumberFormatException, IOException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        ExperimentDataExportDto experimentDataExportDto;

        try {
            experimentDataExportDto = experimentDataExportService.retrieve(fileId,  apijwtService.experimentAllowed(securedInfo, experimentId), securedInfo);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if (experimentDataExportDto.getFile() == null) {
            // file archive is outdated; send processing response
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(null);
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(experimentDataExportDto.getMimeType()))
            .contentLength(experimentDataExportDto.getFile().length())
            .header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=%s", experimentDataExportDto.getFileName()))
            .body(new InputStreamResource(new FileInputStream(experimentDataExportDto.getFile())));
    }

    @PutMapping("/{fileId}/acknowledge")
    public ResponseEntity<ExperimentDataExportDto> errorAcknowledge(@PathVariable long experimentId, @PathVariable UUID fileId, @RequestParam ExperimentDataExportStatus status, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, NumberFormatException, IOException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            experimentDataExportService.acknowledge(fileId, apijwtService.experimentAllowed(securedInfo, experimentId), status);

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.warn("Experiment data export with ID: [{}] and experiment ID: [{}] not found.", fileId, experimentId);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

}
