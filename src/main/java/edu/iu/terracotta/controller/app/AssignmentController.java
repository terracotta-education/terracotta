package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AssignmentDto;
import edu.iu.terracotta.exceptions.AssignmentMoveException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.AssignmentTreatmentService;
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

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@Controller
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = AssignmentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class AssignmentController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/exposures/{exposureId}/assignments";

    @Autowired private AssignmentService assignmentService;
    @Autowired private AssignmentTreatmentService assignmentTreatmentService;
    @Autowired private ApiJwtService apijwtService;

    @GetMapping
    public ResponseEntity<List<AssignmentDto>> allAssignmentsByExposure(@PathVariable long experimentId,
                                                                        @PathVariable long exposureId,
                                                                        @RequestParam(name = "submissions", defaultValue = "false") boolean submissions,
                                                                        @RequestParam(name = "includeDeleted", defaultValue = "false") boolean includeDeleted,
                                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExposureNotMatchingException, AssessmentNotMatchingException, ApiException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        List<AssignmentDto> assignments = assignmentService.getAssignments(exposureId, submissions, includeDeleted, securedInfo);

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
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException, AssessmentNotMatchingException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assignmentAllowed(securedInfo, experimentId, exposureId, assignmentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        AssignmentDto assignmentDto = assignmentTreatmentService.toAssignmentDto(assignmentService.getAssignment(assignmentId), submissions, true);

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
            MultipleAttemptsSettingsValidationException, NumberFormatException, ApiException, TerracottaConnectorException {
        log.debug("Creating Assignment for experiment ID: {}", experimentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        AssignmentDto returnedDto = assignmentService.postAssignment(assignmentDto, experimentId, exposureId, securedInfo);
        HttpHeaders headers = assignmentService.buildHeaders(ucBuilder, experimentId, exposureId, returnedDto.getAssignmentId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

    @PutMapping("/{assignmentId}")
    @Transactional(rollbackFor = { AssignmentNotEditedException.class, ApiException.class })
    public ResponseEntity<AssignmentDto> updateAssignment(@PathVariable long experimentId,
                                                 @PathVariable long exposureId,
                                                 @PathVariable long assignmentId,
                                                 @RequestBody AssignmentDto assignmentDto,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException,
                    TitleValidationException, ApiException, AssignmentNotEditedException,
                    RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, ExposureNotMatchingException, TerracottaConnectorException {
        log.debug("Updating assignment with id: {}", assignmentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assignmentAllowed(securedInfo, experimentId, exposureId, assignmentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        AssignmentDto updatedAssignmentDto = assignmentService.putAssignment(assignmentId, assignmentDto, securedInfo);

        return new ResponseEntity<>(updatedAssignmentDto, HttpStatus.OK);
    }

    @PutMapping
    @Transactional(rollbackFor = { AssignmentNotEditedException.class, ApiException.class })
    public ResponseEntity<List<AssignmentDto>> updateAssignments(@PathVariable long experimentId,
                                                                 @PathVariable long exposureId,
                                                                 @RequestBody List<AssignmentDto> assignmentDtos,
                                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException,
                    TitleValidationException, ApiException, AssignmentNotEditedException,
                    RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, ExposureNotMatchingException, AssessmentNotMatchingException, NumberFormatException, TerracottaConnectorException {
        log.debug("Updating assignments for exposure with id: {}", exposureId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        for (AssignmentDto assignmentDto : assignmentDtos) {
            apijwtService.assignmentAllowed(securedInfo, experimentId, exposureId, assignmentDto.getAssignmentId());
        }

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        List<AssignmentDto> updatedAssignmentDtos = assignmentService.updateAssignments(assignmentDtos, securedInfo);

        return new ResponseEntity<>(updatedAssignmentDtos, HttpStatus.OK);
    }

    @DeleteMapping("/{assignmentId}")
    @Transactional(rollbackFor = { AssignmentNotEditedException.class, ApiException.class })
    public ResponseEntity<Void> deleteAssignment(@PathVariable long experimentId,
                                                 @PathVariable long exposureId,
                                                 @PathVariable long assignmentId,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, AssignmentNotMatchingException, BadTokenException, ApiException, AssignmentNotEditedException, ExperimentLockedException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentLocked(experimentId, true);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assignmentAllowed(securedInfo, experimentId, exposureId, assignmentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        try {
            assignmentService.deleteById(assignmentId, securedInfo);
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
                    MultipleAttemptsSettingsValidationException, NumberFormatException, ApiException, ExceedingLimitException, TreatmentNotMatchingException, QuestionNotMatchingException, TerracottaConnectorException {

        log.debug("Duplicating Assignment: {}", assignmentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        AssignmentDto returnedDto = assignmentService.duplicateAssignment(assignmentId, securedInfo);
        HttpHeaders headers = assignmentService.buildHeaders(ucBuilder, experimentId, exposureId, returnedDto.getAssignmentId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

    @PostMapping("/{assignmentId}/move")
    @Transactional(rollbackFor = { AssignmentNotCreatedException.class, ApiException.class, AssignmentNotEditedException.class })
    public ResponseEntity<AssignmentDto> moveAssignment(@PathVariable long experimentId,
                                                        @PathVariable long exposureId,
                                                        @PathVariable long assignmentId,
                                                        @RequestBody AssignmentDto assignmentDto,
                                                        UriComponentsBuilder ucBuilder,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, ExposureNotMatchingException, BadTokenException,
                    AssessmentNotMatchingException, TitleValidationException, AssignmentNotCreatedException, IdInPostException,
                    DataServiceException, RevealResponsesSettingValidationException, AssignmentNotMatchingException,
                    MultipleAttemptsSettingsValidationException, NumberFormatException, ApiException, ExceedingLimitException, TreatmentNotMatchingException, AssignmentMoveException, AssignmentNotEditedException, QuestionNotMatchingException, TerracottaConnectorException {
        log.debug("Duplicating Assignment: {}", assignmentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        AssignmentDto returnedDto = assignmentService.moveAssignment(assignmentId, assignmentDto, experimentId, exposureId, securedInfo);
        HttpHeaders headers = assignmentService.buildHeaders(ucBuilder, experimentId, exposureId, returnedDto.getAssignmentId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

}
