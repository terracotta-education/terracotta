package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.SubmissionCommentNotMatchingException;
import edu.iu.terracotta.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.SubmissionComment;
import edu.iu.terracotta.model.app.dto.SubmissionCommentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.SubmissionCommentService;
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
@SuppressWarnings({"rawtypes","unchecked"})
@RequestMapping(value = SubmissionCommentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class SubmissionCommentController {

    static final String REQUEST_ROOT = "api/experiments";
    static final Logger log = LoggerFactory.getLogger(SubmissionCommentController.class);

    @Autowired
    APIJWTService apijwtService;

    @Autowired
    SubmissionService submissionService;

    @Autowired
    SubmissionCommentService submissionCommentService;



    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/submission_comments", method = RequestMethod.GET,
                    produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<SubmissionCommentDto>> getSubmissionCommentsBySubmission(@PathVariable("experiment_id") Long experimentId,
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

        if(apijwtService.isLearnerOrHigher(securedInfo)) {

            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            List<SubmissionCommentDto> submissionCommentList = submissionCommentService.getSubmissionComments(submissionId);
            if(submissionCommentList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(submissionCommentList, HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/submission_comments/{submission_comment_id}",
                    method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<SubmissionCommentDto> getSubmissionComment(@PathVariable("experiment_id") Long experimentId,
                                                                     @PathVariable("condition_id") Long conditionId,
                                                                     @PathVariable("treatment_id") Long treatmentId,
                                                                     @PathVariable("assessment_id") Long assessmentId,
                                                                     @PathVariable("submission_id") Long submissionId,
                                                                     @PathVariable("submission_comment_id") Long submissionCommentId,
                                                                     HttpServletRequest req)
                throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionCommentNotMatchingException, BadTokenException, InvalidUserException{

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionCommentAllowed(securedInfo, assessmentId, submissionId, submissionCommentId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {

            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            SubmissionCommentDto submissionCommentDto = submissionCommentService.toDto(submissionCommentService.getSubmissionComment(submissionCommentId));
            return new ResponseEntity<>(submissionCommentDto, HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/submission_comments",
                    method = RequestMethod.POST)
    public ResponseEntity<SubmissionCommentDto> postSubmissionComment(@PathVariable("experiment_id") Long experimentId,
                                                                      @PathVariable("condition_id") Long conditionId,
                                                                      @PathVariable("treatment_id") Long treatmentId,
                                                                      @PathVariable("assessment_id") Long assessmentId,
                                                                      @PathVariable("submission_id") Long submissionId,
                                                                      @RequestBody SubmissionCommentDto submissionCommentDto,
                                                                      UriComponentsBuilder ucBuilder,
                                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionNotMatchingException, BadTokenException, InvalidUserException, IdInPostException, DataServiceException {

        log.debug("Creating submission comment: {}", submissionCommentDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securedInfo, assessmentId, submissionId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            SubmissionCommentDto returnedDto = submissionCommentService.postSubmissionComment(submissionCommentDto, submissionId, securedInfo.getUserId());
            HttpHeaders headers = submissionCommentService.buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, assessmentId, submissionId, returnedDto.getSubmissionCommentId());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/submission_comments/{submission_comment_id}",
                    method = RequestMethod.PUT)
    public ResponseEntity<Void> updateSubmissionComment(@PathVariable("experiment_id") Long experimentId,
                                                        @PathVariable("condition_id") Long conditionId,
                                                        @PathVariable("treatment_id") Long treatmentId,
                                                        @PathVariable("assessment_id") Long assessmentId,
                                                        @PathVariable("submission_id") Long submissionId,
                                                        @PathVariable("submission_comment_id") Long submissionCommentId,
                                                        @RequestBody SubmissionCommentDto submissionCommentDto,
                                                        HttpServletRequest req)
                throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionCommentNotMatchingException, BadTokenException, InvalidUserException {

        log.debug("Updating submission comment with id {}", submissionCommentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionCommentAllowed(securedInfo, assessmentId, submissionId, submissionCommentId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            SubmissionComment submissionComment = submissionCommentService.getSubmissionComment(submissionCommentId);
            LtiUserEntity user = submissionCommentService.findByUserKey(securedInfo.getUserId());
            if(!user.getDisplayName().equals(submissionComment.getCreator())){
                return new ResponseEntity("Error 122: Only the creator of a comment can edit their own comment.", HttpStatus.UNAUTHORIZED);
            }
            submissionCommentService.updateSubmissionComment(submissionComment, submissionCommentDto);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/submission_comments/{submission_comment_id}",
                    method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteSubmissionComment(@PathVariable("experiment_id") Long experimentId,
                                                        @PathVariable("condition_id") Long conditionId,
                                                        @PathVariable("treatment_id") Long treatmentId,
                                                        @PathVariable("assessment_id") Long assessmentId,
                                                        @PathVariable("submission_id") Long submissionId,
                                                        @PathVariable("submission_comment_id") Long submissionCommentId,
                                                        HttpServletRequest req)
                throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionCommentNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionCommentAllowed(securedInfo, assessmentId, submissionId, submissionCommentId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            try{
                submissionCommentService.deleteById(submissionCommentId);
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