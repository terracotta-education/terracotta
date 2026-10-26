package edu.iu.terracotta.controller.app;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AssignmentFileArchiveDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.service.app.AssignmentFileArchiveService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.security.app.roles.InstructorOrHigher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * File archive status across an experiment's assignments, for pages that cover more than one
 * assignment (the experiment's components page).
 */
@Controller
@RequiredArgsConstructor
@RequestMapping(value = ExperimentFileArchiveController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ExperimentFileArchiveController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/files";

    private final ApiJwtService apijwtService;
    private final ExperimentService experimentService;
    private final AssignmentFileArchiveService assignmentFileArchiveService;

    // the assignments with new uploads since their last file archive was made
    @GetMapping("/outdated")
    @InstructorOrHigher
    public ResponseEntity<List<AssignmentFileArchiveDto>> outdated(@PathVariable("experimentId") UUID experimentUuid, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, NumberFormatException, TerracottaConnectorException, IOException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        return new ResponseEntity<>(assignmentFileArchiveService.findOutdated(experimentId), HttpStatus.OK);
    }

}
