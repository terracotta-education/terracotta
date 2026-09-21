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
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.ExposureService;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.AssignmentTreatmentService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

import java.util.UUID;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = AssignmentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class AssignmentController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/exposures/{exposureId}/assignments";

    private final AssignmentService assignmentService;
    private final AssignmentTreatmentService assignmentTreatmentService;
    private final ApiJwtService apijwtService;
    private final ExperimentService experimentService;
    private final ExposureService exposureService;

    @GetMapping
    public ResponseEntity<List<AssignmentDto>> allAssignmentsByExposure(@PathVariable("experimentId") UUID experimentUuid,
                                                                        @PathVariable("exposureId") UUID exposureUuid,
                                                                        @RequestParam(name = "submissions", defaultValue = "false") boolean submissions,
                                                                        @RequestParam(name = "includeDeleted", defaultValue = "false") boolean includeDeleted,
                                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExposureNotMatchingException, AssessmentNotMatchingException, ApiException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        long exposureId = exposureService.getExposureIdByUuid(exposureUuid);
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
    public ResponseEntity<AssignmentDto> getAssignment(@PathVariable("experimentId") UUID experimentUuid,
                                                       @PathVariable("exposureId") UUID exposureUuid,
                                                       @PathVariable("assignmentId") UUID assignmentUuid,
                                                       @RequestParam(name = "submissions", defaultValue = "false") boolean submissions,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExposureNotMatchingException, AssignmentNotMatchingException, AssessmentNotMatchingException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        long exposureId = exposureService.getExposureIdByUuid(exposureUuid);
        long assignmentId = assignmentService.getAssignmentIdByUuid(assignmentUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assignmentAllowed(securedInfo, experimentId, exposureId, assignmentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        AssignmentDto assignmentDto = assignmentTreatmentService.toAssignmentDto(assignmentService.getAssignment(assignmentId), submissions, true, securedInfo);

        return new ResponseEntity<>(assignmentDto, HttpStatus.OK);
    }

    @PostMapping
    @Transactional(rollbackFor = { AssignmentNotCreatedException.class })
    public ResponseEntity<AssignmentDto> postAssignment(@PathVariable("experimentId") UUID experimentUuid,
                                                        @PathVariable("exposureId") UUID exposureUuid,
                                                        @RequestBody AssignmentDto assignmentDto,
                                                        UriComponentsBuilder ucBuilder,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, ExposureNotMatchingException, BadTokenException,
            AssessmentNotMatchingException, TitleValidationException, AssignmentNotCreatedException, IdInPostException,
            DataServiceException, RevealResponsesSettingValidationException,
            MultipleAttemptsSettingsValidationException, NumberFormatException, ApiException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        long exposureId = exposureService.getExposureIdByUuid(exposureUuid);
        log.debug("Creating Assignment for experiment ID: {}", experimentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        AssignmentDto returnedDto = assignmentService.postAssignment(assignmentDto, experimentId, exposureId, securedInfo);
        HttpHeaders headers = assignmentService.buildHeaders(ucBuilder, experimentUuid, exposureUuid, returnedDto.getAssignmentId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

    @PutMapping("/{assignmentId}")
    @Transactional(rollbackFor = { AssignmentNotEditedException.class, ApiException.class })
    public ResponseEntity<AssignmentDto> updateAssignment(@PathVariable("experimentId") UUID experimentUuid,
                                                 @PathVariable("exposureId") UUID exposureUuid,
                                                 @PathVariable("assignmentId") UUID assignmentUuid,
                                                 @RequestBody AssignmentDto assignmentDto,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException,
                    TitleValidationException, ApiException, AssignmentNotEditedException,
                    RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, ExposureNotMatchingException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        long exposureId = exposureService.getExposureIdByUuid(exposureUuid);
        long assignmentId = assignmentService.getAssignmentIdByUuid(assignmentUuid);
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
    public ResponseEntity<List<AssignmentDto>> updateAssignments(@PathVariable("experimentId") UUID experimentUuid,
                                                                 @PathVariable("exposureId") UUID exposureUuid,
                                                                 @RequestBody List<AssignmentDto> assignmentDtos,
                                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssignmentNotMatchingException,
                    TitleValidationException, ApiException, AssignmentNotEditedException,
                    RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, ExposureNotMatchingException, AssessmentNotMatchingException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        long exposureId = exposureService.getExposureIdByUuid(exposureUuid);
        log.debug("Updating assignments for exposure with id: {}", exposureId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        // bulk endpoint: each item's numeric id is resolved individually rather than once up
        // front, since each AssignmentDto in the list carries its own uuid
        for (AssignmentDto assignmentDto : assignmentDtos) {
            long assignmentId = assignmentService.getAssignmentIdByUuid(assignmentDto.getAssignmentId());
            apijwtService.assignmentAllowed(securedInfo, experimentId, exposureId, assignmentId);
        }

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        List<AssignmentDto> updatedAssignmentDtos = assignmentService.updateAssignments(assignmentDtos, securedInfo);

        return new ResponseEntity<>(updatedAssignmentDtos, HttpStatus.OK);
    }

    @DeleteMapping("/{assignmentId}")
    @Transactional(rollbackFor = { AssignmentNotEditedException.class, ApiException.class })
    public ResponseEntity<Void> deleteAssignment(@PathVariable("experimentId") UUID experimentUuid,
                                                 @PathVariable("exposureId") UUID exposureUuid,
                                                 @PathVariable("assignmentId") UUID assignmentUuid,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, ExposureNotMatchingException, AssignmentNotMatchingException, BadTokenException, ApiException, AssignmentNotEditedException, ExperimentLockedException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        long exposureId = exposureService.getExposureIdByUuid(exposureUuid);
        long assignmentId = assignmentService.getAssignmentIdByUuid(assignmentUuid);
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
    public ResponseEntity<AssignmentDto> duplicateAssignment(@PathVariable("experimentId") UUID experimentUuid,
                                                        @PathVariable("exposureId") UUID exposureUuid,
                                                        @PathVariable("assignmentId") UUID assignmentUuid,
                                                        UriComponentsBuilder ucBuilder,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, ExposureNotMatchingException, BadTokenException,
                    AssessmentNotMatchingException, TitleValidationException, AssignmentNotCreatedException, IdInPostException,
                    DataServiceException, RevealResponsesSettingValidationException, AssignmentNotMatchingException,
                    MultipleAttemptsSettingsValidationException, NumberFormatException, ApiException, ExceedingLimitException, TreatmentNotMatchingException, QuestionNotMatchingException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        long exposureId = exposureService.getExposureIdByUuid(exposureUuid);
        long assignmentId = assignmentService.getAssignmentIdByUuid(assignmentUuid);

        log.debug("Duplicating Assignment: {}", assignmentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        AssignmentDto returnedDto = assignmentService.duplicateAssignment(assignmentId, securedInfo);
        HttpHeaders headers = assignmentService.buildHeaders(ucBuilder, experimentUuid, exposureUuid, returnedDto.getAssignmentId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

    @PostMapping("/{assignmentId}/move")
    @Transactional(rollbackFor = { AssignmentNotCreatedException.class, ApiException.class, AssignmentNotEditedException.class })
    public ResponseEntity<AssignmentDto> moveAssignment(@PathVariable("experimentId") UUID experimentUuid,
                                                        @PathVariable("exposureId") UUID exposureUuid,
                                                        @PathVariable("assignmentId") UUID assignmentUuid,
                                                        @RequestBody AssignmentDto assignmentDto,
                                                        UriComponentsBuilder ucBuilder,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, ExposureNotMatchingException, BadTokenException,
                    AssessmentNotMatchingException, TitleValidationException, AssignmentNotCreatedException, IdInPostException,
                    DataServiceException, RevealResponsesSettingValidationException, AssignmentNotMatchingException,
                    MultipleAttemptsSettingsValidationException, NumberFormatException, ApiException, ExceedingLimitException, TreatmentNotMatchingException, AssignmentMoveException, AssignmentNotEditedException, QuestionNotMatchingException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        long exposureId = exposureService.getExposureIdByUuid(exposureUuid);
        long assignmentId = assignmentService.getAssignmentIdByUuid(assignmentUuid);
        log.debug("Duplicating Assignment: {}", assignmentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        AssignmentDto returnedDto = assignmentService.moveAssignment(assignmentId, assignmentDto, experimentId, exposureId, securedInfo);
        HttpHeaders headers = assignmentService.buildHeaders(ucBuilder, experimentUuid, exposureUuid, returnedDto.getAssignmentId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

}
