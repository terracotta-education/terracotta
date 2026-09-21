package edu.iu.terracotta.controller.app.export.data;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.dao.model.dto.export.data.ExperimentDataExportDto;
import edu.iu.terracotta.dao.model.enums.export.data.ExperimentDataExportStatus;
import edu.iu.terracotta.exceptions.AssignmentFileArchiveNotFoundException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.service.app.export.data.ExperimentDataExportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@SuppressWarnings({"PMD.GuardLogStatement"})
@RequestMapping(value = ExperimentDataExportController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ExperimentDataExportController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/export/data";

    private final ApiJwtService apijwtService;
    private final ExperimentService experimentService;
    private final ExperimentDataExportService experimentDataExportService;

    @GetMapping
    public ResponseEntity<ExperimentDataExportDto> process(@PathVariable("experimentId") UUID experimentUuid, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, NumberFormatException, IOException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
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
    public ResponseEntity<ExperimentDataExportDto> poll(@PathVariable("experimentId") UUID experimentUuid, @RequestParam(defaultValue = "false") boolean createNewOnOutdated, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, NumberFormatException, IOException, TerracottaConnectorException, AssignmentFileArchiveNotFoundException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            Experiment experiment = apijwtService.experimentAllowed(securedInfo, experimentId);
            return new ResponseEntity<>(experimentDataExportService.poll(experiment, securedInfo, createNewOnOutdated), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    // this list endpoint hangs off the per-experiment route purely for URL shape - the frontend
    // sends a placeholder "0" for that segment, so it's taken as an opaque String and never
    // resolved (binding it as a UUID would reject the placeholder before this method even ran)
    @PostMapping("/poll/list")
    public ResponseEntity<List<ExperimentDataExportDto>> pollList(@PathVariable("experimentId") String ignoredExperimentId, @RequestParam(defaultValue = "false") boolean createNewOnOutdated, @RequestBody List<UUID> experimentUuids, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, NumberFormatException, IOException, TerracottaConnectorException, AssignmentFileArchiveNotFoundException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            List<Experiment> experiments = new ArrayList<>();

            for (UUID experimentUuid : experimentUuids) {
                long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
                experiments.add(apijwtService.experimentAllowed(securedInfo, experimentId));
            }

            return new ResponseEntity<>(experimentDataExportService.poll(experiments, securedInfo, createNewOnOutdated), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{fileId}/retrieve")
    public ResponseEntity<Resource> retrieve(@PathVariable("experimentId") UUID experimentUuid, @PathVariable UUID fileId, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, NumberFormatException, IOException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
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
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(experimentDataExportDto.getFileName(), StandardCharsets.UTF_8).build().toString())
            .body(new InputStreamResource(new FileInputStream(experimentDataExportDto.getFile())));
    }

    @PutMapping("/{fileId}/acknowledge")
    public ResponseEntity<ExperimentDataExportDto> errorAcknowledge(@PathVariable("experimentId") UUID experimentUuid, @PathVariable UUID fileId, @RequestParam ExperimentDataExportStatus status, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, NumberFormatException, IOException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
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
