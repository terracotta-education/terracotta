package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.DuplicateQuestionException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.dto.QuestionSubmissionDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.QuestionSubmissionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = QuestionSubmissionController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class QuestionSubmissionController {

    static final String REQUEST_ROOT = "api/experiments";
    static final Logger log = LoggerFactory.getLogger(QuestionSubmissionController.class);

    @Autowired
    APIJWTService apijwtService;

    @Autowired
    QuestionSubmissionService questionSubmissionService;

    @Autowired
    SubmissionService submissionService;


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions",
            method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<List<QuestionSubmissionDto>> getQuestionSubmissionsBySubmission(@PathVariable("experiment_id") Long experimentId,
                                                                                          @PathVariable("condition_id") Long conditionId,
                                                                                          @PathVariable("treatment_id") Long treatmentId,
                                                                                          @PathVariable("assessment_id") Long assessmentId,
                                                                                          @PathVariable("submission_id") Long submissionId,
                                                                                          HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionNotMatchingException, BadTokenException, InvalidUserException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securedInfo, assessmentId, submissionId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {

            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            List<QuestionSubmissionDto> questionSubmissionList = questionSubmissionService.getQuestionSubmissions(submissionId);
            if (questionSubmissionList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(questionSubmissionList, HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}",
            method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<QuestionSubmissionDto> getQuestionSubmission(@PathVariable("experiment_id") Long experimentId,
                                                                       @PathVariable("condition_id") Long conditionId,
                                                                       @PathVariable("treatment_id") Long treatmentId,
                                                                       @PathVariable("assessment_id") Long assessmentId,
                                                                       @PathVariable("submission_id") Long submissionId,
                                                                       @PathVariable("question_submission_id") Long questionSubmissionId,
                                                                       @RequestParam(name = "question_submission_comments", defaultValue = "false") boolean questionSubmissionComments,
                                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, InvalidUserException{

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {

            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            QuestionSubmissionDto questionSubmissionDto = questionSubmissionService.toDto(questionSubmissionService.getQuestionSubmission(questionSubmissionId), questionSubmissionComments);
            return new ResponseEntity<>(questionSubmissionDto, HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions",
            method = RequestMethod.POST)
    public ResponseEntity<QuestionSubmissionDto> postQuestionSubmission(@PathVariable("experiment_id") Long experimentId,
                                                                        @PathVariable("condition_id") Long conditionId,
                                                                        @PathVariable("treatment_id") Long treatmentId,
                                                                        @PathVariable("assessment_id") Long assessmentId,
                                                                        @PathVariable("submission_id") Long submissionId,
                                                                        @RequestBody QuestionSubmissionDto questionSubmissionDto,
                                                                        UriComponentsBuilder ucBuilder,
                                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionNotMatchingException, BadTokenException, InvalidUserException, DuplicateQuestionException, IdMissingException, IdInPostException {

        log.debug("Creating question submission: {}", questionSubmissionDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securedInfo, assessmentId, submissionId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            if (questionSubmissionDto.getQuestionSubmissionId() != null) {
                throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
            }
            boolean student = false;
            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
                student = true;
            }
            questionSubmissionDto.setSubmissionId(submissionId);
            QuestionSubmission questionSubmission;
            try {
                questionSubmissionService.validateDto(questionSubmissionDto, assessmentId, student);
                questionSubmission = questionSubmissionService.fromDto(questionSubmissionDto);
            } catch (DataServiceException ex) {
                return new ResponseEntity("Error 105: Unable to create question submission: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
            }
            QuestionSubmissionDto returnedDto = questionSubmissionService.toDto(questionSubmissionService.save(questionSubmission), false);
            HttpHeaders headers = questionSubmissionService.buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmission.getQuestionSubmissionId());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}",
            method = RequestMethod.PUT)
    public ResponseEntity<Void> updateQuestionSubmission(@PathVariable("experiment_id") Long experimentId,
                                                         @PathVariable("condition_id") Long conditionId,
                                                         @PathVariable("treatment_id") Long treatmentId,
                                                         @PathVariable("assessment_id") Long assessmentId,
                                                         @PathVariable("submission_id") Long submissionId,
                                                         @PathVariable("question_submission_id") Long questionSubmissionId,
                                                         @RequestBody QuestionSubmissionDto questionSubmissionDto,
                                                         HttpServletRequest req)
                throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, InvalidUserException {

        log.debug("Updating question submission with id {}", questionSubmissionId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            boolean student = false;
            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
                student = true;
            }
            QuestionSubmission questionSubmission = questionSubmissionService.getQuestionSubmission(questionSubmissionId);
            Map<QuestionSubmission, QuestionSubmissionDto> map = new HashMap<>();
            map.put(questionSubmission, questionSubmissionDto);
            questionSubmissionService.updateQuestionSubmissions(map, student);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions",
                    method = RequestMethod.PUT)
    public ResponseEntity<Void> updateQuestionSubmissions(@PathVariable("experiment_id") Long experimentId,
                                                          @PathVariable("condition_id") Long conditionId,
                                                          @PathVariable("treatment_id") Long treatmentId,
                                                          @PathVariable("assessment_id") Long assessmentId,
                                                          @PathVariable("submission_id") Long submissionId,
                                                          @RequestBody List<QuestionSubmissionDto> questionSubmissionDtoList,
                                                          HttpServletRequest req)
                throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, DataServiceException, BadTokenException, InvalidUserException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        if(apijwtService.isLearnerOrHigher(securedInfo)){
            boolean student = false;
            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
                student = true;
            }
            Map<QuestionSubmission, QuestionSubmissionDto> map = new HashMap<>();
            for(QuestionSubmissionDto questionSubmissionDto : questionSubmissionDtoList) {
                apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionDto.getQuestionSubmissionId());
                QuestionSubmission questionSubmission = questionSubmissionService.getQuestionSubmission(questionSubmissionDto.getQuestionSubmissionId());
                log.debug("Updating question submission with id: {}", questionSubmission.getQuestionSubmissionId());
                map.put(questionSubmission, questionSubmissionDto);
            }
            try{
                questionSubmissionService.updateQuestionSubmissions(map, student);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch(Exception ex) {
                throw new DataServiceException("Error 105: There was an error updating the question submission list. No question submissions were updated.");
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}",
                    method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteQuestionSubmission(@PathVariable("experiment_id") Long experimentId,
                                                         @PathVariable("condition_id") Long conditionId,
                                                         @PathVariable("treatment_id") Long treatmentId,
                                                         @PathVariable("assessment_id") Long assessmentId,
                                                         @PathVariable("submission_id") Long submissionId,
                                                         @PathVariable("question_submission_id") Long questionSubmissionId,
                                                         HttpServletRequest req)
                throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if(apijwtService.isInstructorOrHigher(securedInfo)){
            try{
                questionSubmissionService.deleteById(questionSubmissionId);
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