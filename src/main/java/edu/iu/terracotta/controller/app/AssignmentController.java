package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentMoveException;
import edu.iu.terracotta.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.app.dto.AssignmentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@Controller
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = AssignmentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class AssignmentController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/exposures/{exposureId}/assignments";

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private APIJWTService apijwtService;

    @GetMapping
    public ResponseEntity<List<AssignmentDto>> allAssignmentsByExposure(@PathVariable long experimentId,
                                                                        @PathVariable long exposureId,
                                                                        @RequestParam(name = "submissions", defaultValue = "false") boolean submissions,
                                                                        @RequestParam(name = "includeDeleted", defaultValue = "false") boolean includeDeleted,
                                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExposureNotMatchingException, AssessmentNotMatchingException, CanvasApiException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String instructorUserId = apijwtService.isInstructorOrHigher(securedInfo) ? securedInfo.getUserId() : null;
        List<AssignmentDto> assignments = assignmentService.getAssignments(exposureId, securedInfo.getCanvasCourseId(),
                submissions, includeDeleted, instructorUserId);

        if (assignments.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(assignments, HttpStatus.OK);
    }

    @GetMapping("/{assignmentId}")
    public ResponseEntity<AssignmentDto> getAssignment(@PathVariable long experimentId,
                                                       @PathVariable long exposureId,
                                                       @PathVariable long assignmentId,
                                                       @RequestParam(name = "submissions", defaultValue = "false") boolean submissions,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException, AssessmentNotMatchingException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assignmentAllowed(securedInfo, experimentId, exposureId, assignmentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        AssignmentDto assignmentDto = assignmentService.toDto(assignmentService.getAssignment(assignmentId), submissions, true);

        return new ResponseEntity<>(assignmentDto, HttpStatus.OK);
    }

    @PostMapping
    @Transactional(rollbackFor = { AssignmentNotCreatedException.class })
    public ResponseEntity<AssignmentDto> postAssignment(@PathVariable long experimentId,
                                                        @PathVariable long exposureId,
                                                        @RequestBody AssignmentDto assignmentDto,
                                                        UriComponentsBuilder ucBuilder,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, ExposureNotMatchingException, BadTokenException,
            AssessmentNotMatchingException, TitleValidationException, AssignmentNotCreatedException, IdInPostException,
            DataServiceException, RevealResponsesSettingValidationException,
            MultipleAttemptsSettingsValidationException, NumberFormatException, CanvasApiException {
        log.debug("Creating Assignment: {}", assignmentDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        AssignmentDto returnedDto = assignmentService.postAssignment(assignmentDto, experimentId, securedInfo.getCanvasCourseId(), exposureId, Long.toString(securedInfo.getPlatformDeploymentId()));
        HttpHeaders headers = assignmentService.buildHeaders(ucBuilder, experimentId, exposureId, returnedDto.getAssignmentId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

    @Transactional(rollbackFor = { AssignmentNotEditedException.class, CanvasApiException.class })
    @PutMapping("/{assignmentId}")
    public ResponseEntity<AssignmentDto> updateAssignment(@PathVariable long experimentId,
                                                 @PathVariable long exposureId,
                                                 @PathVariable long assignmentId,
                                                 @RequestBody AssignmentDto assignmentDto,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException,
                    TitleValidationException, CanvasApiException, AssignmentNotEditedException,
                    RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, ExposureNotMatchingException {

        log.debug("Updating assignment with id: {}", assignmentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assignmentAllowed(securedInfo, experimentId, exposureId, assignmentId);

        if(!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        AssignmentDto updatedAssignmentDto = assignmentService.putAssignment(assignmentId, assignmentDto, securedInfo.getCanvasCourseId(), securedInfo.getUserId());

        return new ResponseEntity<>(updatedAssignmentDto, HttpStatus.OK);
    }

    @Transactional(rollbackFor = { AssignmentNotEditedException.class, CanvasApiException.class })
    @PutMapping
    public ResponseEntity<List<AssignmentDto>> updateAssignments(@PathVariable long experimentId,
                                                                 @PathVariable long exposureId,
                                                                 @RequestBody List<AssignmentDto> assignmentDtos,
                                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException,
                    TitleValidationException, CanvasApiException, AssignmentNotEditedException,
                    RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, ExposureNotMatchingException, AssessmentNotMatchingException {

        log.debug("Updating assignments for exposure with id: {}", exposureId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        for (AssignmentDto assignmentDto : assignmentDtos) {
            apijwtService.assignmentAllowed(securedInfo, experimentId, exposureId, assignmentDto.getAssignmentId());
        }

        if(!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        List<AssignmentDto> updatedAssignmentDtos = assignmentService.updateAssignments(assignmentDtos,
                securedInfo.getCanvasCourseId(), securedInfo.getUserId());

        return new ResponseEntity<>(updatedAssignmentDtos, HttpStatus.OK);
    }

    @Transactional(rollbackFor = { AssignmentNotEditedException.class, CanvasApiException.class })
    @DeleteMapping("/{assignmentId}")
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
            assignmentService.deleteById(assignmentId, securedInfo.getCanvasCourseId(), securedInfo.getUserId());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EmptyResultDataAccessException e) {
            log.warn(e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Transactional
    @PostMapping("/{assignmentId}/duplicate")
    public ResponseEntity<AssignmentDto> duplicateAssignment(@PathVariable long experimentId,
                                                        @PathVariable long exposureId,
                                                        @PathVariable long assignmentId,
                                                        UriComponentsBuilder ucBuilder,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, ExposureNotMatchingException, BadTokenException,
                    AssessmentNotMatchingException, TitleValidationException, AssignmentNotCreatedException, IdInPostException,
                    DataServiceException, RevealResponsesSettingValidationException,
                    MultipleAttemptsSettingsValidationException, NumberFormatException, CanvasApiException, ExceedingLimitException, TreatmentNotMatchingException, QuestionNotMatchingException {

        log.debug("Duplicating Assignment: {}", assignmentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        AssignmentDto returnedDto = assignmentService.duplicateAssignment(assignmentId, securedInfo.getCanvasCourseId(),
                securedInfo.getUserId());
        HttpHeaders headers = assignmentService.buildHeaders(ucBuilder, experimentId, exposureId, returnedDto.getAssignmentId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

    @Transactional(rollbackFor = { AssignmentNotCreatedException.class, CanvasApiException.class, AssignmentNotEditedException.class })
    @PostMapping("/{assignmentId}/move")
    public ResponseEntity<AssignmentDto> moveAssignment(@PathVariable long experimentId,
                                                        @PathVariable long exposureId,
                                                        @PathVariable long assignmentId,
                                                        @RequestBody AssignmentDto assignmentDto,
                                                        UriComponentsBuilder ucBuilder,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, ExposureNotMatchingException, BadTokenException,
                    AssessmentNotMatchingException, TitleValidationException, AssignmentNotCreatedException, IdInPostException,
                    DataServiceException, RevealResponsesSettingValidationException,
                    MultipleAttemptsSettingsValidationException, NumberFormatException, CanvasApiException, ExceedingLimitException, TreatmentNotMatchingException, AssignmentMoveException, AssignmentNotEditedException, QuestionNotMatchingException {

        log.debug("Duplicating Assignment: {}", assignmentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        AssignmentDto returnedDto = assignmentService.moveAssignment(assignmentId, assignmentDto, experimentId,
                exposureId, securedInfo.getCanvasCourseId(), securedInfo.getUserId());
        HttpHeaders headers = assignmentService.buildHeaders(ucBuilder, experimentId, exposureId, returnedDto.getAssignmentId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

}
