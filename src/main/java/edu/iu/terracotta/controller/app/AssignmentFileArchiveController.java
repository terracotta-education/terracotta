package edu.iu.terracotta.controller.app;

import java.io.FileInputStream;
import java.io.IOException;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AssignmentFileArchiveDto;
import edu.iu.terracotta.exceptions.AssignmentFileArchiveNotFoundException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.service.app.AssignmentFileArchiveService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@SuppressWarnings({"PMD.GuardLogStatement"})
@RequestMapping(value = AssignmentFileArchiveController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class AssignmentFileArchiveController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/exposures/{exposureId}/assignments/{assignmentId}/files";

    @Autowired private ApiJwtService apijwtService;
    @Autowired private AssignmentFileArchiveService assignmentFileArchiveService;

    @GetMapping
    public ResponseEntity<AssignmentFileArchiveDto> files(@PathVariable long experimentId,
                                                       @PathVariable long exposureId,
                                                       @PathVariable long assignmentId,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException, AssessmentNotMatchingException, NumberFormatException,
                TerracottaConnectorException, IOException, ExposureNotMatchingException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);
        Assignment assignment = apijwtService.assignmentAllowed(securedInfo, experimentId, exposureId, assignmentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(assignmentFileArchiveService.process(assignment, securedInfo), HttpStatus.ACCEPTED);
    }

    @GetMapping("/poll")
    public ResponseEntity<AssignmentFileArchiveDto> poll(@PathVariable long experimentId,
                                                       @PathVariable long exposureId,
                                                       @PathVariable long assignmentId,
                                                       @RequestParam(defaultValue = "false") boolean createNewOnOutdated,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException, AssessmentNotMatchingException, NumberFormatException,
                TerracottaConnectorException, IOException, ExposureNotMatchingException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);
        Assignment assignment = apijwtService.assignmentAllowed(securedInfo, experimentId, exposureId, assignmentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            return new ResponseEntity<>(assignmentFileArchiveService.poll(assignment, securedInfo, createNewOnOutdated), HttpStatus.OK);
        } catch (AssignmentFileArchiveNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{fileId}/retrieve")
    public ResponseEntity<Resource> retrieve(@PathVariable long experimentId,
                                                       @PathVariable long exposureId,
                                                       @PathVariable long assignmentId,
                                                       @PathVariable UUID fileId,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException, AssessmentNotMatchingException, NumberFormatException,
                TerracottaConnectorException, IOException, ExposureNotMatchingException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);
        Assignment assignment = apijwtService.assignmentAllowed(securedInfo, experimentId, exposureId, assignmentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        AssignmentFileArchiveDto assignmentFileArchiveDto = assignmentFileArchiveService.retrieve(fileId, assignment, securedInfo);

        if (assignmentFileArchiveDto.getFile() == null) {
            // file archive is outdated; send processing response
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(null);
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(assignmentFileArchiveDto.getMimeType()))
            .contentLength(assignmentFileArchiveDto.getFile().length())
            .header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=%s", assignmentFileArchiveDto.getFileName()))
            .body(new InputStreamResource(new FileInputStream(assignmentFileArchiveDto.getFile())));
    }

    @PutMapping("/{fileId}/error/acknowledge")
    public ResponseEntity<Void> errorAcknowledge(@PathVariable long experimentId,
                                                       @PathVariable long exposureId,
                                                       @PathVariable long assignmentId,
                                                       @PathVariable UUID fileId,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException, AssessmentNotMatchingException, NumberFormatException,
                TerracottaConnectorException, IOException, ExposureNotMatchingException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);
        Assignment assignment = apijwtService.assignmentAllowed(securedInfo, experimentId, exposureId, assignmentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            assignmentFileArchiveService.errorAcknowledge(fileId, assignment);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (AssignmentFileArchiveNotFoundException e) {
            log.warn("Assignment file archive with ID: [{}] and assignment ID: [{}] not found.", fileId, assignment.getAssignmentId());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

}
