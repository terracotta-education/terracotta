package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.dao.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.dao.model.dto.SubmissionDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = SubmissionController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class SubmissionController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions";

    @Autowired private ApiJwtService apijwtService;
    @Autowired private SubmissionService submissionService;

    @GetMapping
    public ResponseEntity<List<SubmissionDto>> getSubmissionsByAssessment(@PathVariable long experimentId,
                                                                          @PathVariable long conditionId,
                                                                          @PathVariable long treatmentId,
                                                                          @PathVariable long assessmentId,
                                                                          HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, BadTokenException, NoSubmissionsException, NumberFormatException, TerracottaConnectorException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        boolean student = !apijwtService.isInstructorOrHigher(securedInfo);
        List<SubmissionDto> submissionDtoList = submissionService.getSubmissions(experimentId, securedInfo.getUserId(), assessmentId, student);

        if (submissionDtoList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(submissionDtoList, HttpStatus.OK);
    }

    @GetMapping("/{submissionId}")
    public ResponseEntity<SubmissionDto> getSubmission(@PathVariable long experimentId,
                                                       @PathVariable long conditionId,
                                                       @PathVariable long treatmentId,
                                                       @PathVariable long assessmentId,
                                                       @PathVariable long submissionId,
                                                       @RequestParam(defaultValue = "false") boolean questionSubmissions,
                                                       @RequestParam(defaultValue = "false") boolean submissionComments,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionNotMatchingException, BadTokenException, NoSubmissionsException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securedInfo, assessmentId, submissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        boolean student = !apijwtService.isInstructorOrHigher(securedInfo);
        Submission submission = submissionService.getSubmission(experimentId, securedInfo.getUserId(), submissionId, student);

        return new ResponseEntity<>(submissionService.toDto(submission, questionSubmissions, submissionComments), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<SubmissionDto> postSubmission(@PathVariable long experimentId,
                                                        @PathVariable long conditionId,
                                                        @PathVariable long treatmentId,
                                                        @PathVariable long assessmentId,
                                                        @RequestBody SubmissionDto submissionDto,
                                                        UriComponentsBuilder ucBuilder,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, BadTokenException, InvalidUserException,
                    ParticipantNotMatchingException, IdInPostException, DataServiceException, NumberFormatException, TerracottaConnectorException, IntegrationTokenNotFoundException {
        log.debug("Creating Submission for assessment ID: '{}' and participant ID: '{}'", assessmentId, submissionDto.getParticipantId());
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        if (!submissionService.datesAllowed(experimentId, treatmentId, securedInfo)) {
            return new ResponseEntity("Error 128: Assignment locked", HttpStatus.UNAUTHORIZED);
        }

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        boolean student = !apijwtService.isInstructorOrHigher(securedInfo);
        SubmissionDto returnedDto = submissionService.postSubmission(submissionDto, experimentId, securedInfo, assessmentId, student);
        HttpHeaders headers = submissionService.buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, assessmentId, returnedDto.getSubmissionId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }


    @PutMapping("/{submissionId}")
    public ResponseEntity<Void> updateSubmission(@PathVariable long experimentId,
                                                 @PathVariable long conditionId,
                                                 @PathVariable long treatmentId,
                                                 @PathVariable long assessmentId,
                                                 @PathVariable long submissionId,
                                                 @RequestBody SubmissionDto submissionDto,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionNotMatchingException, BadTokenException, NoSubmissionsException,
            ConnectionException, DataServiceException, ApiException, IOException, TerracottaConnectorException {
        log.debug("Updating submission with id {}", submissionId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securedInfo, assessmentId, submissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        boolean student = !apijwtService.isInstructorOrHigher(securedInfo);
        Map<Submission, SubmissionDto> map = new HashMap<>();
        Submission submission = submissionService.getSubmission(experimentId, securedInfo.getUserId(), submissionId, student);
        map.put(submission, submissionDto);
        submissionService.updateSubmissions(map, student);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping
    public ResponseEntity<Void> updateSubmissions(@PathVariable long experimentId,
                                                  @PathVariable long conditionId,
                                                  @PathVariable long treatmentId,
                                                  @PathVariable long assessmentId,
                                                  @RequestBody List<SubmissionDto> submissionDtoList,
                                                  HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, BadTokenException, SubmissionNotMatchingException, NoSubmissionsException, DataServiceException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        Map<Submission, SubmissionDto> map = new HashMap<>();

        for (SubmissionDto submissionDto : submissionDtoList) {
            apijwtService.submissionAllowed(securedInfo, assessmentId, submissionDto.getSubmissionId());
            Submission submission = submissionService.getSubmission(experimentId, securedInfo.getUserId(), submissionDto.getSubmissionId(), false);
            log.debug("Updating submission ID: [{}]", submission.getSubmissionId());
            map.put(submission, submissionDto);
        }

        try {
            submissionService.updateSubmissions(map, false);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            throw new DataServiceException("Error 105: There was an error updating the submission list. No submissions were updated. " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/{submissionId}")
    public ResponseEntity<Void> deleteSubmission(@PathVariable long experimentId,
                                                 @PathVariable long conditionId,
                                                 @PathVariable long treatmentId,
                                                 @PathVariable long assessmentId,
                                                 @PathVariable long submissionId,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionNotMatchingException, BadTokenException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securedInfo, assessmentId, submissionId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        try {
            submissionService.deleteById(submissionId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EmptyResultDataAccessException ex) {
            log.warn(ex.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}
