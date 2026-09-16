package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.entity.RegradeDetails;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotFoundException;
import edu.iu.terracotta.dao.model.dto.AssessmentDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.app.TreatmentService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
import java.util.UUID;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = AssessmentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class AssessmentController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments";

    private final ApiJwtService apijwtService;
    private final ExperimentService experimentService;
    private final ConditionService conditionService;
    private final AssessmentService assessmentService;
    private final SubmissionService submissionService;
    private final TreatmentService treatmentService;

    @GetMapping
    public ResponseEntity<List<AssessmentDto>> getAssessmentByTreatment(@PathVariable("experimentId") UUID experimentUuid,
                                                                        @PathVariable("conditionId") UUID conditionUuid,
                                                                        @PathVariable("treatmentId") UUID treatmentUuid,
                                                                        @RequestParam(name = "submissions", defaultValue = "false") boolean submissions,
                                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ConditionNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.treatmentAllowed(securedInfo, experimentId, conditionId, treatmentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        List<AssessmentDto> assessmentDtoList = assessmentService.getAllAssessmentsByTreatment(treatmentId, submissions, securedInfo);

        if (assessmentDtoList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(assessmentDtoList, HttpStatus.OK);
    }

    @GetMapping(value = "/{assessmentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssessmentDto> getAssessment(@PathVariable("experimentId") UUID experimentUuid,
                                                       @PathVariable("conditionId") UUID conditionUuid,
                                                       @PathVariable("treatmentId") UUID treatmentUuid,
                                                       @PathVariable("assessmentId") UUID assessmentUuid,
                                                       @RequestParam(name = "questions", defaultValue = "false") boolean questions,
                                                       @RequestParam(name = "answers", defaultValue = "false") boolean answers,
                                                       @RequestParam(name = "submissions", defaultValue = "false") boolean submissions,
                                                       @RequestParam(name = "submission_id", required = false) UUID submissionUuid,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ConditionNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, SubmissionNotMatchingException, NoSubmissionsException, NumberFormatException, TerracottaConnectorException {
        Long submissionId = submissionUuid != null ? submissionService.getSubmissionByUuid(submissionUuid).getSubmissionId() : null;
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();

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

        AssessmentDto assessmentDto = assessmentService.toDto(assessmentService.getAssessment(assessmentId), submissionId, questions, answers, submissions, isStudent, securedInfo);

        return new ResponseEntity<>(assessmentDto, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<AssessmentDto> postAssessment(@PathVariable("experimentId") UUID experimentUuid,
                                                        @PathVariable("conditionId") UUID conditionUuid,
                                                        @PathVariable("treatmentId") UUID treatmentUuid,
                                                        @RequestBody AssessmentDto assessmentDto,
                                                        UriComponentsBuilder ucBuilder,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, BadTokenException, ConditionNotMatchingException,
            TitleValidationException, AssessmentNotMatchingException, IdInPostException, DataServiceException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        log.debug("Creating Assessment for experiment ID: {}", experimentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.treatmentAllowed(securedInfo, experimentId, conditionId, treatmentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        AssessmentDto returnedDto = assessmentService.postAssessment(assessmentDto, treatmentId, securedInfo);
        HttpHeaders headers = assessmentService.buildHeaders(ucBuilder, experimentUuid, conditionUuid, treatmentUuid, returnedDto.getAssessmentId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

    @PutMapping("/{assessmentId}")
    public ResponseEntity<AssessmentDto> putAssessment(@PathVariable("experimentId") UUID experimentUuid,
                                                 @PathVariable("conditionId") UUID conditionUuid,
                                                 @PathVariable("treatmentId") UUID treatmentUuid,
                                                 @PathVariable("assessmentId") UUID assessmentUuid,
                                                 @RequestBody AssessmentDto assessmentDto,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, BadTokenException, ConditionNotMatchingException, TitleValidationException, RevealResponsesSettingValidationException,
            MultipleAttemptsSettingsValidationException, IdInPostException, DataServiceException, NegativePointsException, QuestionNotMatchingException,
            MultipleChoiceLimitReachedException, IntegrationClientNotFoundException, IntegrationNotFoundException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();
        log.debug("Updating assessment with id: {}", assessmentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        AssessmentDto updatedAssessmentDto = assessmentService.putAssessment(assessmentId, assessmentDto, true, securedInfo);

        return new ResponseEntity<>(updatedAssessmentDto, HttpStatus.OK);
    }

    @Transactional
    @DeleteMapping("/{assessmentId}")
    public ResponseEntity<Void> deleteAssessment(@PathVariable("experimentId") UUID experimentUuid,
                                                 @PathVariable("conditionId") UUID conditionUuid,
                                                 @PathVariable("treatmentId") UUID treatmentUuid,
                                                 @PathVariable("assessmentId") UUID assessmentUuid,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, BadTokenException, ConditionNotMatchingException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();
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
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{assessmentId}/regrade")
    public ResponseEntity<Void> regrade(@PathVariable("experimentId") UUID experimentUuid,
                                                @PathVariable("conditionId") UUID conditionUuid,
                                                @PathVariable("treatmentId") UUID treatmentUuid,
                                                @PathVariable("assessmentId") UUID assessmentUuid,
                                                @RequestBody RegradeDetails regradeDetails,
                                                HttpServletRequest req)
        throws ExperimentNotMatchingException, TreatmentNotMatchingException, BadTokenException, ConditionNotMatchingException,
            TitleValidationException, AssessmentNotMatchingException, IdInPostException, DataServiceException, ConnectionException, ApiException, IOException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();
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
