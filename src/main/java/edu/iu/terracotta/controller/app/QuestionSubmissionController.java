package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.dto.QuestionSubmissionDto;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AnswerService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securityInfo, assessmentId, submissionId);

        if (apijwtService.isLearnerOrHigher(securityInfo)) {

            if(!apijwtService.isInstructorOrHigher(securityInfo)){
                Participant participant = submissionService.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, securityInfo.getUserId());
                Optional<Submission> submission = submissionService.findByParticipantIdAndSubmissionId(participant.getParticipantId(), submissionId);
                if(!submission.isPresent()){
                    return new ResponseEntity("Students can only access question submissions from their own submissions. Submission with id "
                            + submissionId + " does not belong to participant with id " + participant.getParticipantId(), HttpStatus.UNAUTHORIZED);
                }
            }
            List<QuestionSubmission> questionSubmissionList = questionSubmissionService.findAllBySubmissionId(submissionId);

            if (questionSubmissionList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            List<QuestionSubmissionDto> questionSubmissionDtoList = new ArrayList<>();
            for (QuestionSubmission questionSubmission : questionSubmissionList) {
                questionSubmissionDtoList.add(questionSubmissionService.toDto(questionSubmission, false));
            }
            return new ResponseEntity<>(questionSubmissionDtoList, HttpStatus.OK);
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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securityInfo, assessmentId, submissionId, questionSubmissionId);

        if (apijwtService.isLearnerOrHigher(securityInfo)) {

            if(!apijwtService.isInstructorOrHigher(securityInfo)){
                Participant participant = submissionService.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, securityInfo.getUserId());
                Optional<Submission> submission = submissionService.findByParticipantIdAndSubmissionId(participant.getParticipantId(), submissionId);
                if(!submission.isPresent()){
                    return new ResponseEntity("Students can only access question submissions from their own submissions. Submission with id "
                            + submissionId + " does not belong to participant with id " + participant.getParticipantId(), HttpStatus.UNAUTHORIZED);
                }
            }
            Optional<QuestionSubmission> questionSubmissionSearchResult = questionSubmissionService.findById(questionSubmissionId);

            if (!questionSubmissionSearchResult.isPresent()) {
                log.error("Question submission in platform {} and context {} and experiment {} and condition {} and treatment {}  and assessment {} and submission {} with id {} not found",
                        securityInfo.getPlatformDeploymentId(), securityInfo.getContextId(), experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId);
                return new ResponseEntity("Question submission in platform" + securityInfo.getPlatformDeploymentId() + " and context " + securityInfo.getContextId() +
                        " and experiment with id " + experimentId + " and condition id " + conditionId + " and treatment id " + treatmentId + " and assessment id " + assessmentId +
                        " and submission id " + submissionId + " with id " + questionSubmissionId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            } else {
                QuestionSubmissionDto questionSubmissionDto = questionSubmissionService.toDto(questionSubmissionSearchResult.get(), questionSubmissionComments);
                return new ResponseEntity<>(questionSubmissionDto, HttpStatus.OK);
            }
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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionNotMatchingException, BadTokenException {

        log.info("Creating question submission: {}", questionSubmissionDto);
        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securityInfo, assessmentId, submissionId);

        if (apijwtService.isLearnerOrHigher(securityInfo)) {
            if (questionSubmissionDto.getQuestionSubmissionId() != null) {
                log.error(TextConstants.ID_IN_POST_ERROR);
                return new ResponseEntity(TextConstants.ID_IN_POST_ERROR, HttpStatus.CONFLICT);
            }

            if(!apijwtService.isInstructorOrHigher(securityInfo)){
                Participant participant = submissionService.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, securityInfo.getUserId());
                Optional<Submission> submission = submissionService.findByParticipantIdAndSubmissionId(participant.getParticipantId(), submissionId);
                if(!submission.isPresent()){
                    return new ResponseEntity("Students can only post question submissions to their own submissions. Submission with id "
                            + submissionId + " does not belong to participant with id " + participant.getParticipantId(), HttpStatus.UNAUTHORIZED);
                }
            }

            questionSubmissionDto.setSubmissionId(submissionId);
            QuestionSubmission questionSubmission;
            try {
                if(questionSubmissionDto.getQuestionId() == null){
                    return new ResponseEntity(TextConstants.ID_MISSING, HttpStatus.BAD_REQUEST);
                }
                if(questionSubmissionService.existsByAssessmentIdAndQuestionId(assessmentId, questionSubmissionDto.getQuestionId())){
                    return new ResponseEntity("A question submission with question id " + questionSubmissionDto.getQuestionId() + " already exists in assessment with id " + assessmentId,
                            HttpStatus.CONFLICT);
                }
                if(questionSubmissionDto.getAlteredGrade() != null && !apijwtService.isInstructorOrHigher(securityInfo)){
                    return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS + " Students cannot alter the grades.", HttpStatus.UNAUTHORIZED);
                }
                questionSubmission = questionSubmissionService.fromDto(questionSubmissionDto);
            } catch (DataServiceException ex) {
                return new ResponseEntity("Unable to create question submission: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
            }

            QuestionSubmission questionSubmissionSaved = questionSubmissionService.save(questionSubmission);
            QuestionSubmissionDto returnedDto = questionSubmissionService.toDto(questionSubmissionSaved, false);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}")
                    .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmission.getQuestionSubmissionId()).toUri());
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
                throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException {

        log.info("Updating question submission with id {}", questionSubmissionId);
        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securityInfo, assessmentId, submissionId, questionSubmissionId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {

            if(!apijwtService.isInstructorOrHigher(securityInfo)){
                Participant participant = submissionService.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, securityInfo.getUserId());
                Optional<Submission> submission = submissionService.findByParticipantIdAndSubmissionId(participant.getParticipantId(), submissionId);
                if(!submission.isPresent()){
                    return new ResponseEntity("Students can only change a question submissions from their own submissions. Submission with id "
                            + submissionId + " does not belong to participant with id " + participant.getParticipantId(), HttpStatus.UNAUTHORIZED);
                }
            }

            Optional<QuestionSubmission> questionSubmissionSearchResult = questionSubmissionService.findById(questionSubmissionId);

            if(!questionSubmissionSearchResult.isPresent()) {
                log.error("Unable to update. Question submission with id {} not found", questionSubmissionId);
                return new ResponseEntity("Unable to update. Question submission with id " + questionSubmissionId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            }
            QuestionSubmission questionSubmissionToChange = questionSubmissionSearchResult.get();
            if(questionSubmissionDto.getAlteredGrade() != null && !apijwtService.isInstructorOrHigher(securityInfo)){
                return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS + " Students cannot alter the grades.", HttpStatus.UNAUTHORIZED);
            }
            questionSubmissionToChange.setAlteredGrade(questionSubmissionDto.getAlteredGrade());

            questionSubmissionService.saveAndFlush(questionSubmissionToChange);
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
                throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, DataServiceException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);

        if(!apijwtService.isInstructorOrHigher(securityInfo)){
            Participant participant = submissionService.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, securityInfo.getUserId());
            Optional<Submission> submission = submissionService.findByParticipantIdAndSubmissionId(participant.getParticipantId(), submissionId);
            if(!submission.isPresent()){
                return new ResponseEntity("Students can only access question submissions from their own submissions. Submission with id "
                        + submissionId + " does not belong to participant with id " + participant.getParticipantId(), HttpStatus.UNAUTHORIZED);
            }
        }

        if(apijwtService.isLearnerOrHigher(securityInfo)){
            List<QuestionSubmission> questionSubmissionList = new ArrayList<>();

            for(QuestionSubmissionDto questionSubmissionDto : questionSubmissionDtoList) {
                apijwtService.questionSubmissionAllowed(securityInfo, assessmentId, submissionId, questionSubmissionDto.getQuestionSubmissionId());
                Optional<QuestionSubmission> questionSubmission = questionSubmissionService.findById(questionSubmissionDto.getQuestionSubmissionId());
                if(questionSubmission.isPresent()){
                    QuestionSubmission questionSubmissionToChange = questionSubmission.get();
                    if(questionSubmissionDto.getAlteredGrade() != null && !apijwtService.isInstructorOrHigher(securityInfo)){
                        return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS + " Students cannot alter the grades.", HttpStatus.UNAUTHORIZED);
                    }
                    questionSubmissionToChange.setAlteredGrade(questionSubmissionDto.getAlteredGrade());
                    questionSubmissionList.add(questionSubmissionToChange);
                }
            }
            try{
                questionSubmissionService.saveAllQuestionSubmissions(questionSubmissionList);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (Exception ex) {
                throw new DataServiceException("An error occurred try to update the question submission list. No question submissions were updated. " + ex.getMessage());
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

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securityInfo, assessmentId, submissionId, questionSubmissionId);

        if(apijwtService.isInstructorOrHigher(securityInfo)){
            try{
                questionSubmissionService.deleteById(questionSubmissionId);
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