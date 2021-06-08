package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.SubmissionCommentNotMatchingException;
import edu.iu.terracotta.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.SubmissionComment;
import edu.iu.terracotta.model.app.dto.SubmissionCommentDto;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.SubmissionCommentService;
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
@RequestMapping(value = SubmissionCommentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class SubmissionCommentController {

    static final String REQUEST_ROOT = "api/experiments";
    static final Logger log = LoggerFactory.getLogger(SubmissionCommentController.class);

    @Autowired
    APIJWTService apijwtService;

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
                throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securityInfo, assessmentId, submissionId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            List<SubmissionComment> submissionCommentList = submissionCommentService.findAllBySubmissionId(submissionId);

            if(submissionCommentList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            List<SubmissionCommentDto> submissionCommentDtos = new ArrayList<>();
            for(SubmissionComment submissionComment : submissionCommentList) {
                submissionCommentDtos.add(submissionCommentService.toDto(submissionComment));
            }
            return new ResponseEntity<>(submissionCommentDtos, HttpStatus.OK);
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
                throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionCommentNotMatchingException, BadTokenException{

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionCommentAllowed(securityInfo, assessmentId, submissionId, submissionCommentId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            Optional<SubmissionComment> submissionCommentSearchResult = submissionCommentService.findById(submissionCommentId);

            if(!submissionCommentSearchResult.isPresent()) {
                log.error("Submission comment in platform {} and context {} and experiment {} and condition {} and treatment {}  and assessment {} and submission {} with id {} not found",
                        securityInfo.getPlatformDeploymentId(), securityInfo.getContextId(), experimentId, conditionId, treatmentId, assessmentId, submissionId, submissionCommentId);
                return new ResponseEntity("Submission comment in platform " + securityInfo.getPlatformDeploymentId() + " and context " + securityInfo.getContextId() +
                        " and experiment with id " + experimentId + " and condition id " + conditionId + " and treatment id " + treatmentId + " and assessment id " + assessmentId +
                        " and submission id " + submissionId + " with id " + submissionCommentId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            } else {
                SubmissionCommentDto submissionCommentDto = submissionCommentService.toDto(submissionCommentSearchResult.get());
                return new ResponseEntity<>(submissionCommentDto, HttpStatus.OK);
            }
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
                throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securityInfo, assessmentId, submissionId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            if(submissionCommentDto.getSubmissionCommentId() != null) {
                log.error(TextConstants.ID_IN_POST_ERROR);
                return new ResponseEntity(TextConstants.ID_IN_POST_ERROR, HttpStatus.CONFLICT);
            }

            submissionCommentDto.setSubmissionId(submissionId);
            LtiUserEntity user = submissionCommentService.findByUserKey(securityInfo.getUserId());
            submissionCommentDto.setCreator(user.getDisplayName());
            SubmissionComment submissionComment;
            try {
                submissionComment = submissionCommentService.fromDto(submissionCommentDto);
            } catch (DataServiceException ex) {
                return new ResponseEntity("Unable to create submission comment: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
            }

            SubmissionComment submissionCommentSaved = submissionCommentService.save(submissionComment);
            SubmissionCommentDto returnedDto = submissionCommentService.toDto(submissionCommentSaved);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/submission_comments/{submission_comment_id}")
                    .buildAndExpand(submissionComment.getSubmission().getAssessment().getTreatment().getCondition().getExperiment().getExperimentId(),
                            submissionComment.getSubmission().getAssessment().getTreatment().getCondition().getConditionId(),
                            submissionComment.getSubmission().getAssessment().getTreatment().getTreatmentId(),
                            submissionComment.getSubmission().getAssessment().getAssessmentId(), submissionComment.getSubmission().getSubmissionId(),
                            submissionComment.getSubmissionCommentId()).toUri());
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
                throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionCommentNotMatchingException, BadTokenException {

        log.info("Updating submission comment with id {}", submissionCommentId);
        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionCommentAllowed(securityInfo, assessmentId, submissionId, submissionCommentId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            Optional<SubmissionComment> submissionCommentSearchResult = submissionCommentService.findById(submissionCommentId);

            if(!submissionCommentSearchResult.isPresent()) {
                log.error("Unable to update. Submission comment with id {} not found.", submissionCommentId);
                return new ResponseEntity("Unable to update. Submission comment with id " + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            }

            SubmissionComment submissionCommentToChange = submissionCommentSearchResult.get();
            submissionCommentToChange.setComment(submissionCommentDto.getComment());

            submissionCommentService.saveAndFlush(submissionCommentToChange);
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

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionCommentAllowed(securityInfo, assessmentId, submissionId, submissionCommentId);

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
            try{
                submissionCommentService.deleteById(submissionCommentId);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (EmptyResultDataAccessException ex) {
                log.error(ex.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }
}