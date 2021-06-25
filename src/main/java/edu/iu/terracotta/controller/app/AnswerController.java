package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.dto.AnswerDto;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
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

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securityInfo, assessmentId, questionId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            boolean student = !apijwtService.isInstructorOrHigher(securityInfo);
            Optional<Question> question = questionService.findById(questionId);
            if(question.isPresent()){
                if (question.get().getQuestionType() == QuestionTypes.MC) {
                    List<AnswerMc> mcAnswerList = answerService.findAllByQuestionIdMC(questionId);
                    if (mcAnswerList.isEmpty()) {
                        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                    }

                    List<AnswerDto> answerDtoList = new ArrayList<>();
                    for (AnswerMc mcAnswer : mcAnswerList) {
                        answerDtoList.add(answerService.toDtoMC(mcAnswer, student));
                    }
                    return new ResponseEntity<>(answerDtoList, HttpStatus.OK);
                }
                return new ResponseEntity("Question type is not supported.", HttpStatus.BAD_REQUEST);
            } else {
                //this will never be reached because the question will already have been validated. Just here to please java.
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
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

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securityInfo, assessmentId, questionId);
        Question question = questionService.findByQuestionId(questionId);
        String answerType = question.getQuestionType().toString();
        apijwtService.answerAllowed(securityInfo, assessmentId, questionId, answerType, answerId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            boolean student = !apijwtService.isInstructorOrHigher(securityInfo);
            if(answerType.equals("MC")){
                Optional<AnswerMc> mcAnswerSearchResult = answerService.findByIdMC(answerId);

                if(!mcAnswerSearchResult.isPresent()) {
                    log.error(answerService.answerNotFound(securityInfo, experimentId, conditionId, treatmentId, assessmentId, questionId, answerId));
                    return new ResponseEntity(answerService.answerNotFound(securityInfo, experimentId, conditionId, treatmentId, assessmentId, questionId, answerId), HttpStatus.NOT_FOUND);
                } else {
                    AnswerDto answerDto = answerService.toDtoMC(mcAnswerSearchResult.get(), student);
                    return new ResponseEntity<>(answerDto, HttpStatus.OK);
                }
            } else {
                return new ResponseEntity("Answer type not supported.", HttpStatus.BAD_REQUEST);
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
        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securityInfo, assessmentId, questionId);

        if(apijwtService.isInstructorOrHigher(securityInfo)) {

            if (answerDto.getAnswerId() != null) {
                log.error(TextConstants.ID_IN_POST_ERROR);
                return new ResponseEntity(TextConstants.ID_IN_POST_ERROR, HttpStatus.CONFLICT);
            }

            answerDto.setQuestionId(questionId);
            Optional<Question> question = questionService.findById(questionId);
            if(question.isPresent()){
                answerDto.setAnswerType(question.get().getQuestionType().toString());
            } else{
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            if ("MC".equals(answerDto.getAnswerType())) {
                AnswerMc answerMc;
                try {
                    answerMc = answerService.fromDtoMC(answerDto);
                } catch (DataServiceException ex) {
                    return new ResponseEntity("Unable to create Answer: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
                }
                AnswerMc answerMcSaved = answerService.saveMC(answerMc);
                AnswerDto returnedMcdDto = answerService.toDtoMC(answerMcSaved, false);
                HttpHeaders McHeaders = new HttpHeaders();
                McHeaders.setLocation(ucBuilder.path(
                        "/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/questions/{question_id}/answers/{answer_id}")
                        .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId, questionId, answerMc.getAnswerMcId()).toUri());
                return new ResponseEntity<>(returnedMcdDto, McHeaders, HttpStatus.CREATED);
            }
            return new ResponseEntity("Answer type not supported.", HttpStatus.BAD_REQUEST);
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

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securityInfo, assessmentId, questionId);

        if(apijwtService.isInstructorOrHigher(securityInfo)){
            Optional<Question> question = questionService.findById(questionId);
            if(question.isPresent()){
                if(question.get().getQuestionType() == QuestionTypes.MC){
                    List<AnswerMc> answerList = new ArrayList<>();

                    for(AnswerDto answerDto : answerDtoList) {
                        apijwtService.answerAllowed(securityInfo, assessmentId, questionId, question.get().getQuestionType().toString(), answerDto.getAnswerId());
                        Optional<AnswerMc> mcAnswer = answerService.findByIdMC(answerDto.getAnswerId());
                        if(mcAnswer.isPresent()) {
                            AnswerMc mcAnswerToChange = mcAnswer.get();
                            mcAnswerToChange.setHtml(answerDto.getHtml());
                            mcAnswerToChange.setAnswerOrder(answerDto.getAnswerOrder());
                            mcAnswerToChange.setCorrect(answerDto.getCorrect());
                            answerList.add(mcAnswerToChange);
                        }
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
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
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
        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securityInfo, assessmentId, questionId);
        Question question = questionService.findByQuestionId(questionId);
        String answerType = question.getQuestionType().toString();
        apijwtService.answerAllowed(securityInfo, assessmentId, questionId, answerType, answerId);

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
            if(answerType.equals("MC")){
                Optional<AnswerMc> mcAnswerSearchResult = answerService.findByIdMC(answerId);

                if(!mcAnswerSearchResult.isPresent()) {
                    log.error("Unable to update. Answer with id {} not found.", answerId);
                    return new ResponseEntity("Unable to update. Answer with id " + answerId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
                }
                AnswerMc mcAnswerToChange = mcAnswerSearchResult.get();
                if(answerDto.getHtml() != null)
                    mcAnswerToChange.setHtml(answerDto.getHtml());
                if(answerDto.getAnswerOrder() != null)
                    mcAnswerToChange.setAnswerOrder(answerDto.getAnswerOrder());
                if(answerDto.getCorrect() != null)
                    mcAnswerToChange.setCorrect(answerDto.getCorrect());

                answerService.saveAndFlushMC(mcAnswerToChange);
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

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionAllowed(securityInfo, assessmentId, questionId);
        Question question = questionService.findByQuestionId(questionId);
        String answerType = question.getQuestionType().toString();
        apijwtService.answerAllowed(securityInfo, assessmentId, questionId, answerType, answerId);

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
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
