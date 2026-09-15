package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionSubmissionCommentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.QuestionSubmissionCommentDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.QuestionSubmissionCommentService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = QuestionSubmissionCommentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class QuestionSubmissionCommentController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions/{submissionId}/question_submissions/{questionSubmissionId}/question_submission_comments";

    private final ApiJwtService apijwtService;
    private final ExperimentService experimentService;
    private final ConditionService conditionService;
    private final SubmissionService submissionService;
    private final QuestionSubmissionCommentService questionSubmissionCommentService;
    private final TreatmentService treatmentService;
    private final AssessmentService assessmentService;

    @GetMapping
    public ResponseEntity<List<QuestionSubmissionCommentDto>> getQuestionSubmissionComments(@PathVariable("experimentId") UUID experimentUuid,
                                                                                            @PathVariable("conditionId") UUID conditionUuid,
                                                                                            @PathVariable("treatmentId") UUID treatmentUuid,
                                                                                            @PathVariable("assessmentId") UUID assessmentUuid,
                                                                                            @PathVariable long submissionId,
                                                                                            @PathVariable long questionSubmissionId,
                                                                                            HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, ConditionNotMatchingException, InvalidUserException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        List<QuestionSubmissionCommentDto> questionSubmissionCommentList = questionSubmissionCommentService.getQuestionSubmissionComments(questionSubmissionId);

        if (questionSubmissionCommentList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(questionSubmissionCommentList, HttpStatus.OK);
    }

    @GetMapping("/{questionSubmissionCommentId}")
    public ResponseEntity<QuestionSubmissionCommentDto> getQuestionSubmissionComment(@PathVariable("experimentId") UUID experimentUuid,
                                                                                     @PathVariable("conditionId") UUID conditionUuid,
                                                                                     @PathVariable("treatmentId") UUID treatmentUuid,
                                                                                     @PathVariable("assessmentId") UUID assessmentUuid,
                                                                                     @PathVariable long submissionId,
                                                                                     @PathVariable long questionSubmissionId,
                                                                                     @PathVariable long questionSubmissionCommentId,
                                                                                     HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, QuestionSubmissionCommentNotMatchingException, BadTokenException, ConditionNotMatchingException, InvalidUserException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        apijwtService.questionSubmissionCommentAllowed(securedInfo, questionSubmissionId, questionSubmissionCommentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        QuestionSubmissionCommentDto questionSubmissionCommentDto = questionSubmissionCommentService.toDto(questionSubmissionCommentService.getQuestionSubmissionComment(questionSubmissionCommentId));

        return new ResponseEntity<>(questionSubmissionCommentDto, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<QuestionSubmissionCommentDto> postQuestionSubmissionComment(@PathVariable("experimentId") UUID experimentUuid,
                                                                                      @PathVariable("conditionId") UUID conditionUuid,
                                                                                      @PathVariable("treatmentId") UUID treatmentUuid,
                                                                                      @PathVariable("assessmentId") UUID assessmentUuid,
                                                                                      @PathVariable long submissionId,
                                                                                      @PathVariable long questionSubmissionId,
                                                                                      @RequestBody QuestionSubmissionCommentDto questionSubmissionCommentDto,
                                                                                      UriComponentsBuilder ucBuilder,
                                                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, ConditionNotMatchingException, InvalidUserException,
                    IdInPostException, DataServiceException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();
        log.debug("Creating question submission comment for question submission ID: {}", questionSubmissionId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        QuestionSubmissionCommentDto returnedDto = questionSubmissionCommentService.postQuestionSubmissionComment(questionSubmissionCommentDto, questionSubmissionId, securedInfo);
        HttpHeaders headers = questionSubmissionCommentService.buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId, returnedDto.getQuestionSubmissionCommentId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

    @PutMapping("/{questionSubmissionCommentId}")
    public ResponseEntity<Void> updateQuestionSubmissionComment(@PathVariable("experimentId") UUID experimentUuid,
                                                                @PathVariable("conditionId") UUID conditionUuid,
                                                                @PathVariable("treatmentId") UUID treatmentUuid,
                                                                @PathVariable("assessmentId") UUID assessmentUuid,
                                                                @PathVariable long submissionId,
                                                                @PathVariable long questionSubmissionId,
                                                                @PathVariable long questionSubmissionCommentId,
                                                                @RequestBody QuestionSubmissionCommentDto questionSubmissionCommentDto,
                                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, QuestionSubmissionCommentNotMatchingException, BadTokenException, ConditionNotMatchingException, InvalidUserException, DataServiceException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();
        log.debug("Updating question submission comment with id {}", questionSubmissionCommentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        apijwtService.questionSubmissionCommentAllowed(securedInfo, questionSubmissionId, questionSubmissionCommentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        questionSubmissionCommentService.updateQuestionSubmissionComment(questionSubmissionCommentDto, questionSubmissionCommentId, experimentId, submissionId, securedInfo);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{questionSubmissionCommentId}")
    public ResponseEntity<Void> deleteQuestionSubmissionComment(@PathVariable("experimentId") UUID experimentUuid,
                                                                @PathVariable("conditionId") UUID conditionUuid,
                                                                @PathVariable("treatmentId") UUID treatmentUuid,
                                                                @PathVariable("assessmentId") UUID assessmentUuid,
                                                                @PathVariable long submissionId,
                                                                @PathVariable long questionSubmissionId,
                                                                @PathVariable long questionSubmissionCommentId,
                                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, QuestionSubmissionCommentNotMatchingException, BadTokenException, ConditionNotMatchingException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        apijwtService.questionSubmissionCommentAllowed(securedInfo, questionSubmissionId, questionSubmissionCommentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        try {
            questionSubmissionCommentService.deleteById(questionSubmissionCommentId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EmptyResultDataAccessException ex) {
            log.warn(ex.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}
