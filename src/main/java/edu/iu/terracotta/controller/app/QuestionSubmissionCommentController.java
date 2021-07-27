package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.QuestionSubmissionCommentNotMatchingException;
import edu.iu.terracotta.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.QuestionSubmissionComment;
import edu.iu.terracotta.model.app.dto.QuestionSubmissionCommentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.QuestionSubmissionCommentService;
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
@RequestMapping(value = QuestionSubmissionCommentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class QuestionSubmissionCommentController {

    static final String REQUEST_ROOT = "api/experiments";
    static final Logger log = LoggerFactory.getLogger(QuestionSubmissionCommentController.class);

    @Autowired
    APIJWTService apijwtService;

    @Autowired
    SubmissionService submissionService;

    @Autowired
    QuestionSubmissionCommentService questionSubmissionCommentService;

    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/question_submission_comments",
            method = RequestMethod.GET,
            produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<QuestionSubmissionCommentDto>> getQuestionSubmissionComments(@PathVariable("experiment_id") Long experimentId,
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

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            List<QuestionSubmissionCommentDto> questionSubmissionCommentList = questionSubmissionCommentService.getQuestionSubmissionComments(questionSubmissionId);

            if(questionSubmissionCommentList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(questionSubmissionCommentList, HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/question_submission_comments/{question_submission_comment_id}",
            method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<QuestionSubmissionCommentDto> getQuestionSubmissionComment(@PathVariable("experiment_id") Long experimentId,
                                                                                     @PathVariable("condition_id") Long conditionId,
                                                                                     @PathVariable("treatment_id") Long treatmentId,
                                                                                     @PathVariable("assessment_id") Long assessmentId,
                                                                                     @PathVariable("submission_id") Long submissionId,
                                                                                     @PathVariable("question_submission_id") Long questionSubmissionId,
                                                                                     @PathVariable("question_submission_comment_id") Long questionSubmissionCommentId,
                                                                                     HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, QuestionSubmissionCommentNotMatchingException, BadTokenException, InvalidUserException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        apijwtService.questionSubmissionCommentAllowed(securedInfo, questionSubmissionId, questionSubmissionCommentId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {

            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            QuestionSubmissionCommentDto questionSubmissionCommentDto = questionSubmissionCommentService.toDto(questionSubmissionCommentService.getQuestionSubmissionComment(questionSubmissionCommentId));
            return new ResponseEntity<>(questionSubmissionCommentDto, HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/question_submission_comments",
            method = RequestMethod.POST)
    public ResponseEntity<QuestionSubmissionCommentDto> postQuestionSubmissionComment(@PathVariable("experiment_id") Long experimentId,
                                                                                      @PathVariable("condition_id") Long conditionId,
                                                                                      @PathVariable("treatment_id") Long treatmentId,
                                                                                      @PathVariable("assessment_id") Long assessmentId,
                                                                                      @PathVariable("submission_id") Long submissionId,
                                                                                      @PathVariable("question_submission_id") Long questionSubmissionId,
                                                                                      @RequestBody QuestionSubmissionCommentDto questionSubmissionCommentDto,
                                                                                      UriComponentsBuilder ucBuilder,
                                                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, InvalidUserException, IdInPostException {

        log.debug("Creating question submission comment: {}", questionSubmissionCommentDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            if(questionSubmissionCommentDto.getQuestionSubmissionCommentId() != null) {
                throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
            }

            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }

            questionSubmissionCommentDto.setQuestionSubmissionId(questionSubmissionId);
            LtiUserEntity user = questionSubmissionCommentService.findByUserKey(securedInfo.getUserId());
            questionSubmissionCommentDto.setCreator(user.getDisplayName());
            QuestionSubmissionComment questionSubmissionComment;
            try {
                questionSubmissionComment = questionSubmissionCommentService.fromDto(questionSubmissionCommentDto);
            } catch (DataServiceException ex) {
                return new ResponseEntity("Error 105: Unable to create question submission comment: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
            }
            QuestionSubmissionCommentDto returnedDto = questionSubmissionCommentService.toDto(questionSubmissionCommentService.save(questionSubmissionComment));
            HttpHeaders headers = questionSubmissionCommentService.buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId, returnedDto.getQuestionSubmissionCommentId());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/question_submission_comments/{question_submission_comment_id}",
            method = RequestMethod.PUT)
    public ResponseEntity<Void> updateQuestionSubmissionComment(@PathVariable("experiment_id") Long experimentId,
                                                                @PathVariable("condition_id") Long conditionId,
                                                                @PathVariable("treatment_id") Long treatmentId,
                                                                @PathVariable("assessment_id") Long assessmentId,
                                                                @PathVariable("submission_id") Long submissionId,
                                                                @PathVariable("question_submission_id") Long questionSubmissionId,
                                                                @PathVariable("question_submission_comment_id") Long questionSubmissionCommentId,
                                                                @RequestBody QuestionSubmissionCommentDto questionSubmissionCommentDto,
                                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, QuestionSubmissionCommentNotMatchingException, BadTokenException, InvalidUserException {

        log.debug("Updating question submission comment with id {}", questionSubmissionCommentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        apijwtService.questionSubmissionCommentAllowed(securedInfo, questionSubmissionId, questionSubmissionCommentId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {

            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            QuestionSubmissionComment questionSubmissionComment = questionSubmissionCommentService.getQuestionSubmissionComment(questionSubmissionCommentId);
            LtiUserEntity user = questionSubmissionCommentService.findByUserKey(securedInfo.getUserId());
            if(!user.getDisplayName().equals(questionSubmissionComment.getCreator())){
                return new ResponseEntity("Error 122: Only the creator of a comment can edit their own comment.", HttpStatus.UNAUTHORIZED);
            }
            questionSubmissionComment.setComment(questionSubmissionCommentDto.getComment());
            questionSubmissionCommentService.saveAndFlush(questionSubmissionComment);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/question_submission_comments/{question_submission_comment_id}",
            method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteQuestionSubmissionComment(@PathVariable("experiment_id") Long experimentId,
                                                                @PathVariable("condition_id") Long conditionId,
                                                                @PathVariable("treatment_id") Long treatmentId,
                                                                @PathVariable("assessment_id") Long assessmentId,
                                                                @PathVariable("submission_id") Long submissionId,
                                                                @PathVariable("question_submission_id") Long questionSubmissionId,
                                                                @PathVariable("question_submission_comment_id") Long questionSubmissionCommentId,
                                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, QuestionSubmissionCommentNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        apijwtService.questionSubmissionCommentAllowed(securedInfo, questionSubmissionId, questionSubmissionCommentId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            try{
                questionSubmissionCommentService.deleteById(questionSubmissionCommentId);
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
