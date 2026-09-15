package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.entity.Question;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotMatchingException;
import edu.iu.terracotta.dao.model.dto.QuestionDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.QuestionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = QuestionController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class QuestionController {

    public final static String REQUEST_ROOT = "api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/questions";

    private final QuestionService questionService;
    private final ApiJwtService apijwtService;
    private final ExperimentService experimentService;
    private final ConditionService conditionService;
    private final TreatmentService treatmentService;
    private final AssessmentService assessmentService;

    @GetMapping
    public ResponseEntity<List<QuestionDto>> getQuestionsByAssessment(@PathVariable("experimentId") UUID experimentUuid,
                                                                      @PathVariable("conditionId") UUID conditionUuid,
                                                                      @PathVariable("treatmentId") UUID treatmentUuid,
                                                                      @PathVariable("assessmentId") UUID assessmentUuid,
                                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, BadTokenException, ConditionNotMatchingException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        List<QuestionDto> questionList = questionService.getQuestions(assessmentId);

        if (questionList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(questionList, HttpStatus.OK);
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<QuestionDto> getQuestion(@PathVariable("experimentId") UUID experimentUuid,
                                                   @PathVariable("conditionId") UUID conditionUuid,
                                                   @PathVariable("treatmentId") UUID treatmentUuid,
                                                   @PathVariable("assessmentId") UUID assessmentUuid,
                                                   @PathVariable("questionId") UUID questionUuid,
                                                   @RequestParam(name = "answers", defaultValue = "false") boolean answers,
                                                   HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, BadTokenException, ConditionNotMatchingException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();
        long questionId = questionService.getQuestionByUuid(questionUuid).getQuestionId();
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securedInfo, assessmentId, questionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        QuestionDto questionDto = questionService.toDto(questionService.getQuestion(questionId), answers, apijwtService.isInstructorOrHigher(securedInfo));

        return new ResponseEntity<>(questionDto, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<QuestionDto> postQuestion(@PathVariable("experimentId") UUID experimentUuid,
                                                    @PathVariable("conditionId") UUID conditionUuid,
                                                    @PathVariable("treatmentId") UUID treatmentUuid,
                                                    @PathVariable("assessmentId") UUID assessmentUuid,
                                                    @RequestParam(name = "answers", defaultValue = "false") boolean answers,
                                                    @RequestBody QuestionDto questionDto,
                                                    UriComponentsBuilder ucBuilder,
                                                    HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, BadTokenException, ConditionNotMatchingException, IdInPostException, DataServiceException, MultipleChoiceLimitReachedException,
            IntegrationNotFoundException, IntegrationClientNotFoundException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();
        log.debug("Creating Question for assessment ID: {}", assessmentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        QuestionDto returnedDto = questionService.postQuestion(questionDto, assessmentId, answers, true);
        HttpHeaders headers = questionService.buildHeaders(ucBuilder, experimentUuid, conditionUuid, treatmentUuid, assessmentUuid, returnedDto.getQuestionId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<Void> updateQuestions(@PathVariable("experimentId") UUID experimentUuid,
                                                @PathVariable("conditionId") UUID conditionUuid,
                                                @PathVariable("treatmentId") UUID treatmentUuid,
                                                @PathVariable("assessmentId") UUID assessmentUuid,
                                                @RequestBody List<QuestionDto> questionDtoList,
                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, BadTokenException, ConditionNotMatchingException, DataServiceException, NumberFormatException, TerracottaConnectorException  {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        Map<Question, QuestionDto> map = new HashMap<>();

        for (QuestionDto questionDto : questionDtoList) {
            long itemQuestionId = questionService.getQuestionByUuid(questionDto.getQuestionId()).getQuestionId();
            apijwtService.questionAllowed(securedInfo, assessmentId, itemQuestionId);
            Question question = questionService.getQuestion(itemQuestionId);
            log.debug("Updating question with id: {}", question.getQuestionId());
            map.put(question, questionDto);
        }

        try {
            questionService.updateQuestion(map);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception ex) {
            throw new DataServiceException("Error 105: An error occurred trying to update the question list. No questions were updated. " + ex.getMessage(), ex);
        }
    }

    @PutMapping("/{questionId}")
    public ResponseEntity<Void> updateQuestion(@PathVariable("experimentId") UUID experimentUuid,
                                               @PathVariable("conditionId") UUID conditionUuid,
                                               @PathVariable("treatmentId") UUID treatmentUuid,
                                               @PathVariable("assessmentId") UUID assessmentUuid,
                                               @PathVariable("questionId") UUID questionUuid,
                                               @RequestBody QuestionDto questionDto,
                                               HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, BadTokenException, ConditionNotMatchingException, NegativePointsException, IntegrationNotFoundException,
                IntegrationNotMatchingException, IntegrationConfigurationNotFoundException, IntegrationConfigurationNotMatchingException, IntegrationClientNotFoundException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();
        long questionId = questionService.getQuestionByUuid(questionUuid).getQuestionId();
        log.debug("Updating question with id: {}", questionId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securedInfo, assessmentId, questionId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        Map<Question, QuestionDto> map = new HashMap<>();
        Question question = questionService.getQuestion(questionId);
        map.put(question, questionDto);
        questionService.updateQuestion(map);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable("experimentId") UUID experimentUuid,
                                               @PathVariable("conditionId") UUID conditionUuid,
                                               @PathVariable("treatmentId") UUID treatmentUuid,
                                               @PathVariable("assessmentId") UUID assessmentUuid,
                                               @PathVariable("questionId") UUID questionUuid,
                                               HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, BadTokenException, ConditionNotMatchingException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();
        long questionId = questionService.getQuestionByUuid(questionUuid).getQuestionId();
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securedInfo, assessmentId, questionId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        try {
            questionService.deleteById(questionId);
        } catch (EmptyResultDataAccessException e) {
            log.warn(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteQuestions(@PathVariable("experimentId") UUID experimentUuid,
                                               @PathVariable("conditionId") UUID conditionUuid,
                                               @PathVariable("treatmentId") UUID treatmentUuid,
                                               @PathVariable("assessmentId") UUID assessmentUuid,
                                               @RequestBody List<QuestionDto> questionDtoList,
                                               HttpServletRequest req)
            throws ExperimentNotMatchingException, TreatmentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, BadTokenException, ConditionNotMatchingException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentByUuid(experimentUuid).getExperimentId();
        long conditionId = conditionService.getConditionByUuid(conditionUuid).getConditionId();
        long treatmentId = treatmentService.getTreatmentByUuid(treatmentUuid).getTreatmentId();
        long assessmentId = assessmentService.getAssessmentByUuid(assessmentUuid).getAssessmentId();
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        for (QuestionDto questionDto : questionDtoList) {
            long itemQuestionId = questionService.getQuestionByUuid(questionDto.getQuestionId()).getQuestionId();
            apijwtService.questionAllowed(securedInfo, assessmentId, itemQuestionId);
            questionService.deleteById(itemQuestionId);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
