package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.dto.QuestionDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.QuestionService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = QuestionController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class QuestionController {

    public final static String REQUEST_ROOT = "api/experiments";

    @Autowired
    private QuestionService questionService;

    @Autowired
    private APIJWTService apijwtService;

    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions",
            method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<QuestionDto>> getQuestionsByAssessment(@PathVariable("experiment_id") Long experimentId,
                                                                      @PathVariable("condition_id") Long conditionId,
                                                                      @PathVariable("treatment_id") Long treatmentId,
                                                                      @PathVariable("assessment_id") Long assessmentId,
                                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            List<QuestionDto> questionList = questionService.getQuestions(assessmentId);
            if(questionList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(questionList, HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping(value = "/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/questions/{questionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<QuestionDto> getQuestion(@PathVariable long experimentId,
                                                   @PathVariable long conditionId,
                                                   @PathVariable long treatmentId,
                                                   @PathVariable long assessmentId,
                                                   @PathVariable long questionId,
                                                   @RequestParam(name = "answers", defaultValue = "false") boolean answers,
                                                   HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securedInfo, assessmentId, questionId);

        if(!apijwtService.isLearnerOrHigher(securedInfo)){
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        QuestionDto questionDto = questionService.toDto(questionService.getQuestion(questionId), answers, apijwtService.isInstructorOrHigher(securedInfo));

        return new ResponseEntity<>(questionDto, HttpStatus.OK);
    }

    @PostMapping("/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/questions")
    public ResponseEntity<QuestionDto> postQuestion(@PathVariable long experimentId,
                                                    @PathVariable long conditionId,
                                                    @PathVariable long treatmentId,
                                                    @PathVariable long assessmentId,
                                                    @RequestParam(name = "answers", defaultValue = "false") boolean answers,
                                                    @RequestBody QuestionDto questionDto,
                                                    UriComponentsBuilder ucBuilder,
                                                    HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, BadTokenException, IdInPostException, DataServiceException, MultipleChoiceLimitReachedException {

        log.debug("Creating Question for assessment ID: {}", assessmentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        if(!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        QuestionDto returnedDto = questionService.postQuestion(questionDto, assessmentId, answers);
        HttpHeaders headers = questionService.buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, assessmentId, returnedDto.getQuestionId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateQuestions(@PathVariable("experiment_id") Long experimentId,
                                                @PathVariable("condition_id") Long conditionId,
                                                @PathVariable("treatment_id") Long treatmentId,
                                                @PathVariable("assessment_id") Long assessmentId,
                                                @RequestBody List<QuestionDto> questionDtoList,
                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, BadTokenException, DataServiceException  {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            Map<Question, QuestionDto> map = new HashMap<>();
            for(QuestionDto questionDto : questionDtoList){
                apijwtService.questionAllowed(securedInfo, assessmentId, questionDto.getQuestionId());
                Question question = questionService.getQuestion(questionDto.getQuestionId());
                log.debug("Updating question with id: {}", question.getQuestionId());
                map.put(question, questionDto);
            }
            try{
                questionService.updateQuestion(map);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (Exception ex) {
                throw new DataServiceException("Error 105: An error occurred trying to update the question list. No questions were updated. " + ex.getMessage(), ex);
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }



    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}",
            method = RequestMethod.PUT)
    public ResponseEntity<Void> updateQuestion(@PathVariable("experiment_id") Long experimentId,
                                               @PathVariable("condition_id") Long conditionId,
                                               @PathVariable("treatment_id") Long treatmentId,
                                               @PathVariable("assessment_id") Long assessmentId,
                                               @PathVariable("question_id") Long questionId,
                                               @RequestBody QuestionDto questionDto,
                                               HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, BadTokenException, NegativePointsException {

        log.debug("Updating question with id: {}", questionId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securedInfo, assessmentId, questionId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            Map<Question, QuestionDto> map = new HashMap<>();
            Question question = questionService.getQuestion(questionId);
            map.put(question, questionDto);
            questionService.updateQuestion(map);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}",
            method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteQuestion(@PathVariable("experiment_id") Long experimentId,
                                               @PathVariable("condition_id") Long conditionId,
                                               @PathVariable("treatment_id") Long treatmentId,
                                               @PathVariable("assessment_id") Long assessmentId,
                                               @PathVariable("question_id") Long questionId,
                                               HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securedInfo, assessmentId, questionId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            try{
                questionService.deleteById(questionId);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (EmptyResultDataAccessException ex) {
                log.warn(ex.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }
}