package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.QuestionSubmissionCommentNotMatchingException;
import edu.iu.terracotta.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.QuestionSubmissionComment;
import edu.iu.terracotta.model.app.dto.QuestionSubmissionCommentDto;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.QuestionSubmissionCommentService;
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
@RequestMapping(value = QuestionSubmissionCommentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class QuestionSubmissionCommentController {

    static final String REQUEST_ROOT = "api/experiments";
    static final Logger log = LoggerFactory.getLogger(QuestionSubmissionCommentController.class);

    @Autowired
    APIJWTService apijwtService;

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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securityInfo, assessmentId, submissionId, questionSubmissionId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            List<QuestionSubmissionComment> questionSubmissionCommentList = questionSubmissionCommentService.findAllByQuestionSubmissionId(questionSubmissionId);

            if(questionSubmissionCommentList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            List<QuestionSubmissionCommentDto> questionSubmissionCommentDtoList = new ArrayList<>();
            for(QuestionSubmissionComment questionSubmissionComment : questionSubmissionCommentList) {
                questionSubmissionCommentDtoList.add(questionSubmissionCommentService.toDto(questionSubmissionComment));
            }
            return new ResponseEntity<>(questionSubmissionCommentDtoList, HttpStatus.OK);
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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, QuestionSubmissionCommentNotMatchingException, BadTokenException{

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securityInfo, assessmentId, submissionId, questionSubmissionId);
        apijwtService.questionSubmissionCommentAllowed(securityInfo, questionSubmissionId, questionSubmissionCommentId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            Optional<QuestionSubmissionComment> questionSubmissionCommentSearchResult = questionSubmissionCommentService.findById(questionSubmissionCommentId);

            if(!questionSubmissionCommentSearchResult.isPresent()) {
                log.error("Question submission comment in platform {} and context {} and experiment {} and condition {} and treatment {}  and assessment {} and submission {} and question submission {} with id {} not found",
                        securityInfo.getPlatformDeploymentId(), securityInfo.getContextId(), experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId, questionSubmissionCommentId);
                return new ResponseEntity("Question submission comment in platform " + securityInfo.getPlatformDeploymentId() + " and context " + securityInfo.getContextId() +
                        " and experiment with id " + experimentId + " and condition id " + conditionId + " and treatment id " + treatmentId + " and assessment id " + assessmentId +
                        " and submission id " + submissionId + " and question submission id " + questionSubmissionId + " with id " + questionSubmissionCommentId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            } else {
                QuestionSubmissionCommentDto questionSubmissionCommentDto = questionSubmissionCommentService.toDto(questionSubmissionCommentSearchResult.get());
                return new ResponseEntity<>(questionSubmissionCommentDto, HttpStatus.OK);
            }
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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securityInfo, assessmentId, submissionId, questionSubmissionId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            if(questionSubmissionCommentDto.getQuestionSubmissionCommentId() != null) {
                log.error(TextConstants.ID_IN_POST_ERROR);
                return new ResponseEntity(TextConstants.ID_IN_POST_ERROR, HttpStatus.CONFLICT);
            }

            questionSubmissionCommentDto.setQuestionSubmissionId(questionSubmissionId);
            LtiUserEntity user = questionSubmissionCommentService.findByUserKey(securityInfo.getUserId());
            questionSubmissionCommentDto.setCreator(user.getDisplayName());
            QuestionSubmissionComment questionSubmissionComment;
            try {
                questionSubmissionComment = questionSubmissionCommentService.fromDto(questionSubmissionCommentDto);
            } catch (DataServiceException ex) {
                return new ResponseEntity("Unable to create question submission comment: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
            }

            QuestionSubmissionComment questionSubmissionCommentSaved = questionSubmissionCommentService.save(questionSubmissionComment);
            QuestionSubmissionCommentDto returnedDto = questionSubmissionCommentService.toDto(questionSubmissionCommentSaved);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/question_submission_comments/{question_submission_comment_id}")
                    .buildAndExpand(questionSubmissionComment.getQuestionSubmission().getSubmission().getAssessment().getTreatment().getCondition().getExperiment().getExperimentId(),
                            questionSubmissionComment.getQuestionSubmission().getSubmission().getAssessment().getTreatment().getCondition().getConditionId(),
                            questionSubmissionComment.getQuestionSubmission().getSubmission().getAssessment().getTreatment().getTreatmentId(),
                            questionSubmissionComment.getQuestionSubmission().getSubmission().getAssessment().getAssessmentId(), questionSubmissionComment.getQuestionSubmission().getSubmission().getSubmissionId(),
                            questionSubmissionComment.getQuestionSubmission().getQuestionSubmissionId(), questionSubmissionComment.getQuestionSubmissionCommentId()).toUri());
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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, QuestionSubmissionCommentNotMatchingException, BadTokenException {

        log.info("Updating question submission comment with id {}", questionSubmissionCommentId);
        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securityInfo, assessmentId, submissionId, questionSubmissionId);
        apijwtService.questionSubmissionCommentAllowed(securityInfo, questionSubmissionId, questionSubmissionCommentId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            Optional<QuestionSubmissionComment> questionSubmissionCommentSearchResult = questionSubmissionCommentService.findById(questionSubmissionCommentId);

            if(!questionSubmissionCommentSearchResult.isPresent()) {
                log.error("Unable to update. Question submission comment with id {} not found.", questionSubmissionCommentId);
                return new ResponseEntity("Unable to update. Question submission comment with id " + questionSubmissionCommentId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            }

            QuestionSubmissionComment questionSubmissionCommentToChange = questionSubmissionCommentSearchResult.get();
            questionSubmissionCommentToChange.setComment(questionSubmissionCommentDto.getComment());

            questionSubmissionCommentService.saveAndFlush(questionSubmissionCommentToChange);
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

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securityInfo, assessmentId, submissionId, questionSubmissionId);
        apijwtService.questionSubmissionCommentAllowed(securityInfo, questionSubmissionId, questionSubmissionCommentId);

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
            try{
                questionSubmissionCommentService.deleteById(questionSubmissionCommentId);
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
