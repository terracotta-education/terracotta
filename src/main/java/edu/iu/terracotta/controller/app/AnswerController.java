package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.dto.AnswerDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AnswerService;
import edu.iu.terracotta.service.app.QuestionService;
import edu.iu.terracotta.utils.TextConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = AnswerController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class AnswerController {
    /**
     This controller was built to support the addition of question/answer types. The current state of the controller only supports the MC (multiple choice) type,
     but all of the requests can be updated with switch statements to support additional types.
     */

    static final String REQUEST_ROOT = "api/experiments";
    static final Logger log = LoggerFactory.getLogger(AnswerController.class);

    @Autowired
    APIJWTService apijwtService;

    @Autowired
    AnswerService answerService;

    @Autowired
    QuestionService questionService;



    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}/answers",
            method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<AnswerDto>> getAnswersByQuestion(@PathVariable("experiment_id") Long experimentId,
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

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            boolean student = !apijwtService.isInstructorOrHigher(securedInfo);
            if (answerService.getQuestionType(questionId).equals("MC")) {
                List<AnswerDto> answerDtoList = answerService.findAllByQuestionIdMC(questionId, student);
                if(answerDtoList.isEmpty()){
                    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                }
                return new ResponseEntity<>(answerDtoList, HttpStatus.OK);
            }
            return new ResponseEntity("Error 120: Answer type is not supported.", HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}/answers/{answer_id}",
            method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<AnswerDto> getAnswer(@PathVariable("experiment_id") Long experimentId,
                                               @PathVariable("condition_id") Long conditionId,
                                               @PathVariable("treatment_id") Long treatmentId,
                                               @PathVariable("assessment_id") Long assessmentId,
                                               @PathVariable("question_id") Long questionId,
                                               @PathVariable("answer_id") Long answerId,
                                               HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, AnswerNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securedInfo, assessmentId, questionId);
        apijwtService.answerAllowed(securedInfo, assessmentId, questionId, answerService.getQuestionType(questionId), answerId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            String answerType = answerService.getQuestionType(questionId);
            boolean student = !apijwtService.isInstructorOrHigher(securedInfo);
            if(answerType.equals("MC")){
                return new ResponseEntity<>(answerService.getAnswerMC(answerId, student), HttpStatus.OK);
            } else {
                return new ResponseEntity("Error 120: Answer type not supported.", HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}/answers",
            method = RequestMethod.POST)
    public ResponseEntity<AnswerDto> postAnswer(@PathVariable("experiment_id") Long experimentId,
                                                @PathVariable("condition_id") Long conditionId,
                                                @PathVariable("treatment_id") Long treatmentId,
                                                @PathVariable("assessment_id") Long assessmentId,
                                                @PathVariable("question_id") Long questionId,
                                                @RequestBody AnswerDto answerDto,
                                                UriComponentsBuilder ucBuilder,
                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, BadTokenException {

        log.info("Creating Answer: {}", answerDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securedInfo, assessmentId, questionId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {

            if (answerDto.getAnswerId() != null) {
                log.error(TextConstants.ID_IN_POST_ERROR);
                return new ResponseEntity(TextConstants.ID_IN_POST_ERROR, HttpStatus.CONFLICT);
            }

            answerDto.setQuestionId(questionId);
            answerDto.setAnswerType(answerService.getQuestionType(questionId));
            if ("MC".equals(answerDto.getAnswerType())) {
                AnswerMc answerMc;
                try {
                    answerMc = answerService.fromDtoMC(answerDto);
                } catch (DataServiceException ex) {
                    return new ResponseEntity("Unable to create Answer: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
                }
                AnswerDto returnedMcdDto = answerService.toDtoMC(answerService.saveMC(answerMc), false);
                HttpHeaders mcHeaders = answerService.buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, assessmentId, questionId, answerMc.getAnswerMcId());
                return new ResponseEntity<>(returnedMcdDto, mcHeaders, HttpStatus.CREATED);
            }
            return new ResponseEntity("Error 120: Answer type not supported.", HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}/answers",
            method = RequestMethod.PUT)
    public ResponseEntity<Void> updateAnswers(@PathVariable("experiment_id") Long experimentId,
                                              @PathVariable("condition_id") Long conditionId,
                                              @PathVariable("treatment_id") Long treatmentId,
                                              @PathVariable("assessment_id") Long assessmentId,
                                              @PathVariable("question_id") Long questionId,
                                              @RequestBody List<AnswerDto> answerDtoList,
                                              HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, AnswerNotMatchingException, BadTokenException, DataServiceException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securedInfo, assessmentId, questionId);

        if(apijwtService.isInstructorOrHigher(securedInfo)){
            String answerType = answerService.getQuestionType(questionId);
            if(answerType.equals("MC")){
                List<AnswerMc> answerList = new ArrayList<>();

                for(AnswerDto answerDto : answerDtoList) {
                    apijwtService.answerAllowed(securedInfo, assessmentId, questionId, answerType, answerDto.getAnswerId());
                    AnswerMc mcAnswer = answerService.findByAnswerId(answerDto.getAnswerId());
                    answerList.add(answerService.updateAnswerMC(mcAnswer, answerDto));
                }
                try{
                    answerService.saveAllAnswersMC(answerList);
                    return new ResponseEntity<>(HttpStatus.OK);
                } catch (Exception ex) {
                    throw new DataServiceException("An error occurred trying to update the answer list. No answers were updated. " + ex.getMessage());
                }
            } else {
                return new ResponseEntity("Answer type not supported.", HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}/answers/{answer_id}",
            method = RequestMethod.PUT)
    public ResponseEntity<Void> updateAnswer(@PathVariable("experiment_id") Long experimentId,
                                             @PathVariable("condition_id") Long conditionId,
                                             @PathVariable("treatment_id") Long treatmentId,
                                             @PathVariable("assessment_id") Long assessmentId,
                                             @PathVariable("question_id") Long questionId,
                                             @PathVariable("answer_id") Long answerId,
                                             @RequestBody AnswerDto answerDto,
                                             HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, AnswerNotMatchingException, BadTokenException {

        log.info("Updating answer with id: {}", answerId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securedInfo, assessmentId, questionId);
        String answerType = questionService.findByQuestionId(questionId).getQuestionType().toString();
        apijwtService.answerAllowed(securedInfo, assessmentId, questionId, answerType, answerId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            if(answerType.equals("MC")){
                AnswerMc answerMc = answerService.findByAnswerId(answerId);
                answerService.saveAndFlushMC(answerService.updateAnswerMC(answerMc, answerDto));
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return new ResponseEntity("Answer type not supported.", HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}/answers/{answer_id}",
            method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteAnswer(@PathVariable("experiment_id") Long experimentId,
                                             @PathVariable("condition_id") Long conditionId,
                                             @PathVariable("treatment_id") Long treatmentId,
                                             @PathVariable("assessment_id") Long assessmentId,
                                             @PathVariable("question_id") Long questionId,
                                             @PathVariable("answer_id") Long answerId,
                                             HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionNotMatchingException, AnswerNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securedInfo, assessmentId, questionId);
        String answerType = questionService.findByQuestionId(questionId).getQuestionType().toString();
        apijwtService.answerAllowed(securedInfo, assessmentId, questionId, answerType, answerId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            if(answerType.equals("MC")){
                try{
                    answerService.deleteByIdMC(answerId);
                    return new ResponseEntity<>(HttpStatus.OK);
                } catch (EmptyResultDataAccessException ex) {
                    log.error(ex.getMessage());
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
            } else {
                return new ResponseEntity("Answer type not supported.", HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }
}