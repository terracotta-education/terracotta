package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.dto.AssignmentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.utils.TextConstants;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


@Controller
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = AssignmentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class AssignmentController {

    static final Logger log = LoggerFactory.getLogger(AssignmentController.class);
    static final String REQUEST_ROOT = "api/experiments";

    @Autowired
    AssignmentService assignmentService;

    @Autowired
    APIJWTService apijwtService;


    @GetMapping(value = "/{experimentId}/exposures/{exposureId}/assignments", produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<AssignmentDto>> allAssignmentsByExposure(@PathVariable long experimentId,
                                                                        @PathVariable long exposureId,
                                                                        @RequestParam(name = "submissions", defaultValue = "false") boolean submissions,
                                                                        @RequestParam(name = "includeDeleted", defaultValue = "false") boolean includeDeleted,
                                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExposureNotMatchingException, AssessmentNotMatchingException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        List<AssignmentDto> assignments = assignmentService.getAssignments(exposureId, submissions, includeDeleted);

        if (assignments.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

            return new ResponseEntity<>(assignments, HttpStatus.OK);
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
        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            AssignmentDto assignmentDto = assignmentService.toDto(assignmentService.getAssignment(assignmentId), submissions);
            return new ResponseEntity<>(assignmentDto, HttpStatus.OK);
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
            throws ExperimentNotMatchingException, ExposureNotMatchingException, BadTokenException,
            AssessmentNotMatchingException, TitleValidationException, AssignmentNotCreatedException, IdInPostException,
            DataServiceException, RevealResponsesSettingValidationException,
            MultipleAttemptsSettingsValidationException {

        log.debug("Creating Assignment: {}", assignmentDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if (apijwtService.isInstructorOrHigher(securedInfo)) {
            AssignmentDto returnedDto = assignmentService.postAssignment(assignmentDto, experimentId, securedInfo.getCanvasCourseId(), exposureId);
            HttpHeaders headers = assignmentService.buildHeaders(ucBuilder, experimentId, exposureId, returnedDto.getAssignmentId());
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
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException,
            TitleValidationException, CanvasApiException, AssignmentNotEditedException,
            RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException {

        log.debug("Updating assignment with id: {}", assignmentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assignmentAllowed(securedInfo, experimentId, exposureId, assignmentId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            assignmentService.updateAssignment(assignmentId, assignmentDto, securedInfo.getCanvasCourseId());
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @DeleteMapping("/{experimentId}/exposures/{exposureId}/assignments/{assignmentId}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable long experimentId,
                                                 @PathVariable long exposureId,
                                                 @PathVariable long assignmentId,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, AssignmentNotMatchingException, BadTokenException, CanvasApiException, AssignmentNotEditedException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assignmentAllowed(securedInfo, experimentId, exposureId, assignmentId);

        if(!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        try{
            assignmentService.deleteById(assignmentId, securedInfo.getCanvasCourseId());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EmptyResultDataAccessException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
