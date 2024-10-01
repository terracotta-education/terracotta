package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.exceptions.integrations.IntegrationNotFoundException;
import edu.iu.terracotta.model.app.RegradeDetails;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AssessmentService;
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

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.List;

@Slf4j
@Controller
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = AssessmentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class AssessmentController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments";

    @Autowired private APIJWTService apijwtService;
    @Autowired private AssessmentService assessmentService;
    @Autowired private SubmissionService submissionService;

    @GetMapping
    public ResponseEntity<List<AssessmentDto>> getAssessmentByTreatment(@PathVariable long experimentId,
                                                                        @PathVariable long conditionId,
                                                                        @PathVariable long treatmentId,
                                                                        @RequestParam(name = "submissions", defaultValue = "false") boolean submissions,
                                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, TreatmentNotMatchingException, AssessmentNotMatchingException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.treatmentAllowed(securedInfo, experimentId, conditionId, treatmentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        List<AssessmentDto> assessmentDtoList = assessmentService.getAllAssessmentsByTreatment(treatmentId, submissions);

        if (assessmentDtoList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(assessmentDtoList, HttpStatus.OK);
    }

    @GetMapping(value = "/{assessmentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssessmentDto> getAssessment(@PathVariable long experimentId,
                                                       @PathVariable long conditionId,
                                                       @PathVariable long treatmentId,
                                                       @PathVariable long assessmentId,
                                                       @RequestParam(name = "questions", defaultValue = "false") boolean questions,
                                                       @RequestParam(name = "answers", defaultValue = "false") boolean answers,
                                                       @RequestParam(name = "submissions", defaultValue = "false") boolean submissions,
                                                       @RequestParam(name = "submission_id", required = false) Long submissionId,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, AssessmentNotMatchingException, SubmissionNotMatchingException, NoSubmissionsException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        if (submissionId != null) {
            apijwtService.submissionAllowed(securedInfo, assessmentId, submissionId);
        }

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        boolean isStudent = !apijwtService.isInstructorOrHigher(securedInfo);

        if (isStudent && submissionId != null) {
            // This will throw NoSubmissionsException if the submission doesn't belong to the student
            this.submissionService.getSubmission(experimentId, securedInfo.getUserId(), submissionId, isStudent);
        }

        AssessmentDto assessmentDto = assessmentService.toDto(assessmentService.getAssessment(assessmentId),
                submissionId, questions, answers, submissions, isStudent);

        return new ResponseEntity<>(assessmentDto, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<AssessmentDto> postAssessment(@PathVariable long experimentId,
                                                        @PathVariable long conditionId,
                                                        @PathVariable long treatmentId,
                                                        @RequestBody AssessmentDto assessmentDto,
                                                        UriComponentsBuilder ucBuilder,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, BadTokenException,
            TitleValidationException, AssessmentNotMatchingException, IdInPostException, DataServiceException {
        log.debug("Creating Assessment for experiment ID: {}", experimentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.treatmentAllowed(securedInfo, experimentId, conditionId, treatmentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        AssessmentDto returnedDto = assessmentService.postAssessment(assessmentDto, treatmentId);
        HttpHeaders headers = assessmentService.buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, returnedDto.getAssessmentId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

    @PutMapping("/{assessmentId}")
    public ResponseEntity<AssessmentDto> putAssessment(@PathVariable long experimentId,
                                                 @PathVariable long conditionId,
                                                 @PathVariable long treatmentId,
                                                 @PathVariable long assessmentId,
                                                 @RequestBody AssessmentDto assessmentDto,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, BadTokenException,
            TitleValidationException, RevealResponsesSettingValidationException,
            MultipleAttemptsSettingsValidationException, IdInPostException, DataServiceException, NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException, IntegrationClientNotFoundException, IntegrationNotFoundException {
        log.debug("Updating assessment with id: {}", assessmentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        AssessmentDto updatedAssessmentDto = assessmentService.putAssessment(assessmentId, assessmentDto, true);

        return new ResponseEntity<>(updatedAssessmentDto, HttpStatus.OK);
    }

    @Transactional
    @DeleteMapping("/{assessmentId}")
    public ResponseEntity<Void> deleteAssessment(@PathVariable long experimentId,
                                                 @PathVariable long conditionId,
                                                 @PathVariable long treatmentId,
                                                 @PathVariable long assessmentId,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, BadTokenException {
        log.debug("Deleting assessment with id: {}", assessmentId);

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        try {
            assessmentService.deleteById(assessmentId);
        } catch (EmptyResultDataAccessException ex) {
            log.warn(ex.getMessage());
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{assessmentId}/regrade")
    public ResponseEntity<Void> regrade(@PathVariable long experimentId,
                                                @PathVariable long conditionId,
                                                @PathVariable long treatmentId,
                                                @PathVariable long assessmentId,
                                                @RequestBody RegradeDetails regradeDetails,
                                                HttpServletRequest req)
        throws ExperimentNotMatchingException, TreatmentNotMatchingException, BadTokenException,
            TitleValidationException, AssessmentNotMatchingException, IdInPostException, DataServiceException, ConnectionException, CanvasApiException, IOException {
        log.debug("Regrading questions for assessment ID: {}", assessmentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.treatmentAllowed(securedInfo, experimentId, conditionId, treatmentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        assessmentService.regradeQuestions(regradeDetails, assessmentId);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

}
