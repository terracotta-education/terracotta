package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.QuestionSubmissionCommentNotMatchingException;
import edu.iu.terracotta.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.QuestionSubmissionComment;
import edu.iu.terracotta.model.app.Submission;
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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                Participant participant = submissionService.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, securedInfo.getUserId());
                Optional<Submission> submission = submissionService.findByParticipantIdAndSubmissionId(participant.getParticipantId(), submissionId);
                if(!submission.isPresent()){
                    return new ResponseEntity("Students can only access question submission comments from their own submissions. Submission with id "
                            + submissionId + " does not belong to participant with id " + participant.getParticipantId(), HttpStatus.UNAUTHORIZED);
                }
            }
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

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        apijwtService.questionSubmissionCommentAllowed(securedInfo, questionSubmissionId, questionSubmissionCommentId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {

            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                Participant participant = submissionService.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, securedInfo.getUserId());
                Optional<Submission> submission = submissionService.findByParticipantIdAndSubmissionId(participant.getParticipantId(), submissionId);
                if(!submission.isPresent()){
                    return new ResponseEntity("Students can only access question submission comments from their own submissions. Submission with id "
                            + submissionId + " does not belong to participant with id " + participant.getParticipantId(), HttpStatus.UNAUTHORIZED);
                }
            }

            Optional<QuestionSubmissionComment> questionSubmissionCommentSearchResult = questionSubmissionCommentService.findById(questionSubmissionCommentId);

            if(!questionSubmissionCommentSearchResult.isPresent()) {
                log.error("Question submission comment in platform {} and context {} and experiment {} and condition {} and treatment {}  and assessment {} and submission {} and question submission {} with id {} not found",
                        securedInfo.getPlatformDeploymentId(), securedInfo.getContextId(), experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId, questionSubmissionCommentId);
                return new ResponseEntity("Question submission comment in platform " + securedInfo.getPlatformDeploymentId() + " and context " + securedInfo.getContextId() +
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

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            if(questionSubmissionCommentDto.getQuestionSubmissionCommentId() != null) {
                log.error(TextConstants.ID_IN_POST_ERROR);
                return new ResponseEntity(TextConstants.ID_IN_POST_ERROR, HttpStatus.CONFLICT);
            }

            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                Participant participant = submissionService.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, securedInfo.getUserId());
                Optional<Submission> submission = submissionService.findByParticipantIdAndSubmissionId(participant.getParticipantId(), submissionId);
                if(!submission.isPresent()){
                    return new ResponseEntity("Students can only post question submission comments to their own submissions. Submission with id "
                            + submissionId + " does not belong to participant with id " + participant.getParticipantId(), HttpStatus.UNAUTHORIZED);
                }
            }

            questionSubmissionCommentDto.setQuestionSubmissionId(questionSubmissionId);
            LtiUserEntity user = questionSubmissionCommentService.findByUserKey(securedInfo.getUserId());
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
                    .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId, questionSubmissionComment.getQuestionSubmissionCommentId()).toUri());
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
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        apijwtService.questionSubmissionCommentAllowed(securedInfo, questionSubmissionId, questionSubmissionCommentId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {

            if(!apijwtService.isInstructorOrHigher(securedInfo)){
                Participant participant = submissionService.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, securedInfo.getUserId());
                Optional<Submission> submission = submissionService.findByParticipantIdAndSubmissionId(participant.getParticipantId(), submissionId);
                if(!submission.isPresent()){
                    return new ResponseEntity("Students can only edit question submission comments from their own submissions. Submission with id "
                            + submissionId + " does not belong to participant with id " + participant.getParticipantId(), HttpStatus.UNAUTHORIZED);
                }
            }

            Optional<QuestionSubmissionComment> questionSubmissionCommentSearchResult = questionSubmissionCommentService.findById(questionSubmissionCommentId);

            if(!questionSubmissionCommentSearchResult.isPresent()) {
                log.error("Unable to update. Question submission comment with id {} not found.", questionSubmissionCommentId);
                return new ResponseEntity("Unable to update. Question submission comment with id " + questionSubmissionCommentId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            }

            LtiUserEntity user = questionSubmissionCommentService.findByUserKey(securedInfo.getUserId());
            if(!user.getDisplayName().equals(questionSubmissionCommentSearchResult.get().getCreator())){
                return new ResponseEntity("Only the creator of a comment can edit their own comment.", HttpStatus.UNAUTHORIZED);
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
                log.error(ex.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }
}
