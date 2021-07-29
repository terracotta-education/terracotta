package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.exceptions.AnswerSubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.TypeNotSupportedException;
import edu.iu.terracotta.model.app.AnswerEssaySubmission;
import edu.iu.terracotta.model.app.AnswerMcSubmission;
import edu.iu.terracotta.model.app.dto.AnswerSubmissionDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AnswerSubmissionService;
import edu.iu.terracotta.service.app.SubmissionService;
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
import java.util.List;

@Controller
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = AnswerSubmissionController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class AnswerSubmissionController {

    /**
     This controller was built to support the addition of answer submission types. The current state of the controller supports MC (multiple choice) and ESSAY types.
     All of the request methods can be updated to support additional types.
     */

    static final String REQUEST_ROOT = "api/experiments";
    static final Logger log = LoggerFactory.getLogger(AnswerSubmissionController.class);

    @Autowired
    AnswerSubmissionService answerSubmissionService;

    @Autowired
    SubmissionService submissionService;

    @Autowired
    APIJWTService apijwtService;


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/answer_submissions",
                    method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<AnswerSubmissionDto>> getAnswerSubmissionsByQuestionId(@PathVariable("experiment_id") Long experimentId,
                                                                                      @PathVariable("condition_id") Long conditionId,
                                                                                      @PathVariable("treatment_id") Long treatmentId,
                                                                                      @PathVariable("assessment_id") Long assessmentId,
                                                                                      @PathVariable("submission_id") Long submissionId,
                                                                                      @PathVariable("question_submission_id") Long questionSubmissionId,
                                                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, InvalidUserException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if(apijwtService.isLearnerOrHigher(securedInfo)){
            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }

            String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
            switch (answerType) {
                case "MC":
                    List<AnswerSubmissionDto> answerSubmissionDtoListMC = answerSubmissionService.getAnswerMcSubmissions(questionSubmissionId);
                    if(answerSubmissionDtoListMC.isEmpty()){
                        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                    }
                    return new ResponseEntity<>(answerSubmissionDtoListMC, HttpStatus.OK);
                case "ESSAY":
                    List<AnswerSubmissionDto> answerSubmissionDtoListEssay = answerSubmissionService.getAnswerEssaySubmissions(questionSubmissionId);
                    if(answerSubmissionDtoListEssay.isEmpty()){
                        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                    }
                    return new ResponseEntity<>(answerSubmissionDtoListEssay, HttpStatus.OK);
                default: return new ResponseEntity("Error 103: Answer type not supported.", HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/answer_submissions/{answer_submission_id}",
                    method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<AnswerSubmissionDto> getAnswerSubmission(@PathVariable("experiment_id") Long experimentId,
                                                                   @PathVariable("condition_id") Long conditionId,
                                                                   @PathVariable("treatment_id") Long treatmentId,
                                                                   @PathVariable("assessment_id") Long assessmentId,
                                                                   @PathVariable("submission_id") Long submissionId,
                                                                   @PathVariable("question_submission_id") Long questionSubmissionId,
                                                                   @PathVariable("answer_submission_id") Long answerSubmissionId,
                                                                   HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, AnswerSubmissionNotMatchingException, BadTokenException, InvalidUserException{

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
        apijwtService.answerSubmissionAllowed(securedInfo, questionSubmissionId, answerType, answerSubmissionId);

        if(apijwtService.isLearnerOrHigher(securedInfo)){

            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            if(answerType.equals("MC")){
                AnswerSubmissionDto mcAnswerSubmissionDto = answerSubmissionService.toDtoMC(answerSubmissionService.getAnswerMcSubmission(answerSubmissionId));
                return new ResponseEntity<>(mcAnswerSubmissionDto, HttpStatus.OK);
            } else if (answerType.equals("ESSAY")){
                AnswerSubmissionDto essayAnswerSubmissionDto = answerSubmissionService.toDtoEssay(answerSubmissionService.getAnswerEssaySubmission(answerSubmissionId));
                return new ResponseEntity<>(essayAnswerSubmissionDto, HttpStatus.OK);
            } else {
                return new ResponseEntity("Error 103: Answer type not supported.", HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/answer_submissions",
                    method = RequestMethod.POST)
    public ResponseEntity<AnswerSubmissionDto> postAnswerSubmission(@PathVariable("experiment_id") Long experimentId,
                                                                    @PathVariable("condition_id") Long conditionId,
                                                                    @PathVariable("treatment_id") Long treatmentId,
                                                                    @PathVariable("assessment_id") Long assessmentId,
                                                                    @PathVariable("submission_id") Long submissionId,
                                                                    @PathVariable("question_submission_id") Long questionSubmissionId,
                                                                    @RequestBody AnswerSubmissionDto answerSubmissionDto,
                                                                    UriComponentsBuilder ucBuilder,
                                                                    HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, InvalidUserException, IdInPostException, TypeNotSupportedException, DataServiceException {

        log.info("Creating answer submission: {}", answerSubmissionDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if(apijwtService.isLearnerOrHigher(securedInfo)){
            if(answerSubmissionDto.getAnswerSubmissionId() != null){
                throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
            }

            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }

            answerSubmissionDto.setQuestionSubmissionId(questionSubmissionId);
            String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
            AnswerSubmissionDto returnedDto = answerSubmissionService.postAnswerSubmission(answerType, answerSubmissionDto);
            HttpHeaders headers = answerSubmissionService.buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId, returnedDto.getAnswerSubmissionId());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    /*
    As other question types are added, it may be useful to add another request allowing for the PUT of a list of answer submissions.
    For example, a fill-in-the-blank question with multiple blanks to fill in.
    */
    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/answer_submissions/{answer_submission_id}",
                    method = RequestMethod.PUT)
    public ResponseEntity<Void> updateAnswerSubmission(@PathVariable("experiment_id") Long experimentId,
                                                       @PathVariable("condition_id") Long conditionId,
                                                       @PathVariable("treatment_id") Long treatmentId,
                                                       @PathVariable("assessment_id") Long assessmentId,
                                                       @PathVariable("submission_id") Long submissionId,
                                                       @PathVariable("question_submission_id") Long questionSubmissionId,
                                                       @PathVariable("answer_submission_id") Long answerSubmissionId,
                                                       @RequestBody AnswerSubmissionDto answerSubmissionDto,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, AnswerSubmissionNotMatchingException, BadTokenException, InvalidUserException, AnswerNotMatchingException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
        apijwtService.answerSubmissionAllowed(securedInfo, questionSubmissionId, answerType, answerSubmissionId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            switch (answerType) {
                case "MC":
                    answerSubmissionService.updateAnswerMcSubmission(answerSubmissionId, answerSubmissionDto);
                    return new ResponseEntity<>(HttpStatus.OK);
                case "ESSAY":
                    answerSubmissionService.updateAnswerEssaySubmission(answerSubmissionId, answerSubmissionDto);
                    return new ResponseEntity<>(HttpStatus.OK);
                default: return new ResponseEntity("Error 103: Answer type not supported.", HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }



    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/answer_submissions/{answer_submission_id}",
                    method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteAnswerSubmission(@PathVariable("experiment_id") Long experimentId,
                                                       @PathVariable("condition_id") Long conditionId,
                                                       @PathVariable("treatment_id") Long treatmentId,
                                                       @PathVariable("assessment_id") Long assessmentId,
                                                       @PathVariable("submission_id") Long submissionId,
                                                       @PathVariable("question_submission_id") Long questionSubmissionId,
                                                       @PathVariable("answer_submission_id") Long answerSubmissionId,
                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, AnswerSubmissionNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
        apijwtService.answerSubmissionAllowed(securedInfo, questionSubmissionId, answerType, answerSubmissionId);

        if(apijwtService.isInstructorOrHigher(securedInfo)){
            switch(answerType){
                case "MC":
                    try{
                        answerSubmissionService.deleteByIdMC(answerSubmissionId);
                        return new ResponseEntity<>(HttpStatus.OK);
                    } catch (EmptyResultDataAccessException ex) {
                        log.warn(ex.getMessage());
                        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                    }
                case "ESSAY":
                    try{
                        answerSubmissionService.deleteByIdEssay(answerSubmissionId);
                        return new ResponseEntity<>(HttpStatus.OK);
                    } catch (EmptyResultDataAccessException ex) {
                        log.warn(ex.getMessage());
                        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                    }
                default: return new ResponseEntity("Error 103: Answer type not supported.", HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }
}
