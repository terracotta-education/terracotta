package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.model.app.dto.AssignmentDto;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;
import edu.iu.terracotta.utils.TextConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import edu.iu.terracotta.model.app.Assignment;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.List;


@Controller
@RequestMapping(value = AssignmentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class AssignmentController {

    static final Logger log = LoggerFactory.getLogger(AssignmentController.class);
    static final String REQUEST_ROOT = "api/experiments";

    @Autowired
    AssignmentService assignmentService;

    @Autowired
    APIJWTService apijwtService;

    @Autowired
    CanvasAPIClient canvasAPIClient;


    @RequestMapping(value = "/{experiment_id}/exposures/{exposure_id}/assignments", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<AssignmentDto>> allAssignmentsByExposure(@PathVariable("experiment_id") long experimentId,
                                                                        @PathVariable("exposure_id") Long exposureId,
                                                                        @RequestParam(name = "submissions", defaultValue = "false") boolean submissions,
                                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExposureNotMatchingException, AssessmentNotMatchingException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            List<Assignment> assignmentList = assignmentService.findAllByExposureId(exposureId);

            if (assignmentList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            List<AssignmentDto> assignmentDtoList = new ArrayList<>();
            for (Assignment assignment : assignmentList) {
                assignmentDtoList.add(assignmentService.toDto(assignment, submissions));
            }
            return new ResponseEntity<>(assignmentDtoList, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/exposures/{exposure_id}/assignments/{assignment_id}", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<AssignmentDto> getAssignment(@PathVariable("experiment_id") long experimentId,
                                                       @PathVariable("exposure_id") long exposureId,
                                                       @PathVariable("assignment_id") long assignmentId,
                                                       @RequestParam(name = "submissions", defaultValue = "false") boolean submissions,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException, AssessmentNotMatchingException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assignmentAllowed(securedInfo, experimentId, exposureId, assignmentId);
        //student access? Or should it be instructorOrHigher?
        if (apijwtService.isLearnerOrHigher(securedInfo)) {

            Optional<Assignment> assignmentSearchResult = assignmentService.findById(assignmentId);

            if (!assignmentSearchResult.isPresent()) {
                log.error("assignment in platform {} and context {} and experiment {} and exposure {} with id {} not found",
                        securedInfo.getPlatformDeploymentId(), securedInfo.getContextId(), experimentId, experimentId, assignmentId);

                return new ResponseEntity("assignment in platform " + securedInfo.getPlatformDeploymentId()
                        + " and context " + securedInfo.getContextId() + " and experiment with id " + experimentId + " and exposure id " + exposureId
                        + " with id " + assignmentId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            } else {
                AssignmentDto assignmentDto = assignmentService.toDto(assignmentSearchResult.get(), submissions);
                return new ResponseEntity<>(assignmentDto, HttpStatus.OK);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @Transactional
    @RequestMapping(value = "/{experiment_id}/exposures/{exposure_id}/assignments", method = RequestMethod.POST)
    public ResponseEntity<AssignmentDto> postAssignment(@PathVariable("experiment_id") long experimentId,
                                                        @PathVariable("exposure_id") long exposureId,
                                                        @RequestBody AssignmentDto assignmentDto,
                                                        UriComponentsBuilder ucBuilder,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, ExposureNotMatchingException, BadTokenException, AssessmentNotMatchingException {
        log.info("Creating Assignment: {}", assignmentDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if (apijwtService.isInstructorOrHigher(securedInfo)) {
            if (assignmentDto.getAssignmentId() != null) {
                log.error("Cannot include id in the POST endpoint. To modify existing exposures you must use PUT");
                return new ResponseEntity("Cannot include id in the POST endpoint. To modify existing exposures you must use PUT", HttpStatus.CONFLICT);
            }
            if(!StringUtils.isAllBlank(assignmentDto.getTitle()) && assignmentDto.getTitle().length() > 255){
                return new ResponseEntity("Assignment title must be 255 characters or less.", HttpStatus.BAD_REQUEST);
            }
            assignmentDto.setExposureId(exposureId);
            Assignment assignment;
            try {
                assignment = assignmentService.fromDto(assignmentDto);
            } catch (DataServiceException e) {
                return new ResponseEntity("Unable to create Assignment: " + e.getMessage(), HttpStatus.BAD_REQUEST);
            }
            Assignment assignmentSaved = assignmentService.save(assignment);

            AssignmentExtended canvasAssignment = new AssignmentExtended();
            edu.ksu.canvas.model.assignment.Assignment.ExternalToolTagAttribute canvasExternalToolTagAttributes = canvasAssignment.new ExternalToolTagAttribute();
            canvasExternalToolTagAttributes.setUrl(ServletUriComponentsBuilder.fromCurrentContextPath().path("/lti3?experiment=" + experimentId + "&assignment=" + assignmentSaved.getAssignmentId()).build().toUriString());
            canvasAssignment.setExternalToolTagAttributes(canvasExternalToolTagAttributes);
            canvasAssignment.setName(assignmentSaved.getTitle());
            canvasAssignment.setDescription(null);
            canvasAssignment.setPublished(false);
            //TODO: This is interesting...because each condition assessment maybe has different points... so... what should we send here? 0? 100 and send a percent always????
            canvasAssignment.setPointsPossible(100.0);
            canvasAssignment.setSubmissionTypes(Collections.singletonList("external_tool"));
            try {
                Optional<AssignmentExtended> canvasAssignmentReturned = canvasAPIClient.createCanvasAssignment(canvasAssignment,
                        securedInfo.getCanvasCourseId(),
                        assignmentSaved.getExposure().getExperiment().getPlatformDeployment());
                assignmentSaved.setLmsAssignmentId(Integer.toString(canvasAssignmentReturned.get().getId()));
                String jwtTokenAssignment = canvasAssignmentReturned.get().getSecureParams();
                String resourceLinkId = apijwtService.unsecureToken(jwtTokenAssignment).getBody().get("lti_assignment_id").toString();
                assignmentSaved.setResourceLinkId(resourceLinkId);
            } catch (CanvasApiException e) {
                log.info("Create the assignment failed");
                e.printStackTrace();
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            assignmentService.saveAndFlush(assignmentSaved);
            AssignmentDto returnedDto = assignmentService.toDto(assignmentSaved, false);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/exposures/{exposure_id}/assignments/{assignment_id}")
                    .buildAndExpand(experimentId, exposureId, assignment.getAssignmentId()).toUri());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/exposures/{exposure_id}/assignments/{assignment_id}", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateAssignment(@PathVariable("experiment_id") Long experimentId,
                                                 @PathVariable("exposure_id") Long exposureId,
                                                 @PathVariable("assignment_id") Long assignmentId,
                                                 @RequestBody AssignmentDto assignmentDto,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException {

        log.info("Updating assignment with id: {}", assignmentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assignmentAllowed(securedInfo, experimentId, exposureId, assignmentId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            Optional<Assignment> assignmentSearchResult = assignmentService.findById(assignmentId);

            if(!assignmentSearchResult.isPresent()) {
                log.error("Unable to update. Assignment with id {} not found.", assignmentId);
                return new ResponseEntity("Unable to update. Assignment with id " + assignmentId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            }
            if(StringUtils.isAllBlank(assignmentDto.getTitle()) && StringUtils.isAllBlank(assignmentSearchResult.get().getTitle())){
                return new ResponseEntity("Please give the assignment a name.", HttpStatus.BAD_REQUEST);
            }
            if(!StringUtils.isAllBlank(assignmentDto.getTitle()) && assignmentDto.getTitle().length() > 255) {
                return new ResponseEntity("The title must be 255 characters or less.", HttpStatus.BAD_REQUEST);
            }
            Assignment assignmentToChange = assignmentSearchResult.get();
            assignmentToChange.setTitle(assignmentDto.getTitle());
            assignmentToChange.setAssignmentOrder(assignmentDto.getAssignmentOrder());
            assignmentToChange.setSoftDeleted(assignmentDto.getSoftDeleted());

            assignmentService.saveAndFlush(assignmentToChange);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/exposures/{exposure_id}/assignments/{assignment_id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteAssignment(@PathVariable("experiment_id") Long experimentId,
                                                 @PathVariable("exposure_id") Long exposureId,
                                                 @PathVariable("assignment_id") Long assignmentId,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, AssignmentNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assignmentAllowed(securedInfo, experimentId, exposureId, assignmentId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            try{
                assignmentService.deleteById(assignmentId);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (EmptyResultDataAccessException e) {
                log.error(e.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }
}
