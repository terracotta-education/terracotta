package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AnswerSubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.model.app.AnswerEssaySubmission;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.AnswerMcSubmission;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.dto.AnswerSubmissionDto;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import edu.iu.terracotta.repository.AllRepositories;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
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
    APIJWTService apijwtService;

    @Autowired
    AllRepositories allRepositories;

    @Autowired
    SubmissionService submissionService;


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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException{

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securityInfo, assessmentId, submissionId, questionSubmissionId);

        if(apijwtService.isLearnerOrHigher(securityInfo)){
            if(!apijwtService.isInstructorOrHigher(securityInfo)){
                Participant participant = submissionService.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, securityInfo.getUserId());
                Optional<Submission> submission = submissionService.findByParticipantIdAndSubmissionId(participant.getParticipantId(), submissionId);
                if(!submission.isPresent()){
                    return new ResponseEntity("Students can only access answer submissions from their own submissions. Submission with id "
                            + submissionId + " does not belong to participant with id " + participant.getParticipantId(), HttpStatus.UNAUTHORIZED);
                }
            }
            Optional<QuestionSubmission> questionSubmission = allRepositories.questionSubmissionRepository.findById(questionSubmissionId);
            if(questionSubmission.isPresent()){
                Optional<Question> question = allRepositories.questionRepository.findById(questionSubmission.get().getQuestion().getQuestionId());
                if(question.isPresent()){
                    String answerType = question.get().getQuestionType().toString();
                    switch (answerType) {
                        case "MC":
                            List<AnswerMcSubmission> answerMcSubmissionList = answerSubmissionService.findByQuestionSubmissionIdMC(questionSubmissionId);
                            if(answerMcSubmissionList.isEmpty()){
                                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                            }
                            List<AnswerSubmissionDto> mcAnswerSubmissionDtoList = new ArrayList<>();
                            for(AnswerMcSubmission answerMcSubmission : answerMcSubmissionList){
                                mcAnswerSubmissionDtoList.add(answerSubmissionService.toDtoMC(answerMcSubmission));
                            }
                            return new ResponseEntity<>(mcAnswerSubmissionDtoList, HttpStatus.OK);
                        case "ESSAY":
                            List<AnswerEssaySubmission> answerEssaySubmissionList = answerSubmissionService.findAllByQuestionSubmissionIdEssay(questionSubmissionId);
                            if(answerEssaySubmissionList.isEmpty()){
                                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                            }
                            List<AnswerSubmissionDto> essayAnswerSubmissionDtoList = new ArrayList<>();
                            for(AnswerEssaySubmission answerEssaySubmission : answerEssaySubmissionList){
                                essayAnswerSubmissionDtoList.add(answerSubmissionService.toDtoEssay(answerEssaySubmission));
                            }
                            return new ResponseEntity<>(essayAnswerSubmissionDtoList, HttpStatus.OK);

                        default: return new ResponseEntity("Answer type not supported.", HttpStatus.BAD_REQUEST);
                    }
                } else {
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, AnswerSubmissionNotMatchingException, BadTokenException{

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securityInfo, assessmentId, submissionId, questionSubmissionId);
        QuestionSubmission questionSubmission = allRepositories.questionSubmissionRepository.findByQuestionSubmissionId(questionSubmissionId);
        Question question = allRepositories.questionRepository.findByQuestionId(questionSubmission.getQuestion().getQuestionId());
        String answerType = question.getQuestionType().toString();
        apijwtService.answerSubmissionAllowed(securityInfo, questionSubmissionId, answerType, answerSubmissionId);

        if(apijwtService.isLearnerOrHigher(securityInfo)){

            if(!apijwtService.isInstructorOrHigher(securityInfo)){
                Participant participant = submissionService.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, securityInfo.getUserId());
                Optional<Submission> submission = submissionService.findByParticipantIdAndSubmissionId(participant.getParticipantId(), submissionId);
                if(!submission.isPresent()){
                    return new ResponseEntity("Students can only access answer submissions from their own submissions. Submission with id "
                            + submissionId + " does not belong to participant with id " + participant.getParticipantId(), HttpStatus.UNAUTHORIZED);
                }
            }
            if(answerType.equals("MC")){
                Optional<AnswerMcSubmission> mcAnswerSearchResult = answerSubmissionService.findByIdMC(answerSubmissionId);
                if(!mcAnswerSearchResult.isPresent()){
                    log.error(answerSubmissionService.answerSubmissionNotFound(securityInfo, experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId, answerSubmissionId));
                    return new ResponseEntity(answerSubmissionService.answerSubmissionNotFound(securityInfo, experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId, answerSubmissionId),
                            HttpStatus.NOT_FOUND);
                }
                AnswerSubmissionDto mcAnswerSubmissionDto = answerSubmissionService.toDtoMC(mcAnswerSearchResult.get());
                return new ResponseEntity<>(mcAnswerSubmissionDto, HttpStatus.OK);
            } else if (answerType.equals("ESSAY")){
                Optional<AnswerEssaySubmission> essayAnswerSearchResult = answerSubmissionService.findByIdEssay(answerSubmissionId);
                if(!essayAnswerSearchResult.isPresent()){
                    log.error(answerSubmissionService.answerSubmissionNotFound(securityInfo, experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId, answerSubmissionId));
                    return new ResponseEntity(answerSubmissionService.answerSubmissionNotFound(securityInfo, experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId, answerSubmissionId),
                            HttpStatus.NOT_FOUND);
                }
                AnswerSubmissionDto essayAnswerSubmissionDto = answerSubmissionService.toDtoEssay(essayAnswerSearchResult.get());
                return new ResponseEntity<>(essayAnswerSubmissionDto, HttpStatus.OK);
            } else {
                return new ResponseEntity("Answer type not supported.", HttpStatus.BAD_REQUEST);
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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException {

        log.info("Creating answer submission: {}", answerSubmissionDto);
        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securityInfo, assessmentId, submissionId, questionSubmissionId);

        if(apijwtService.isLearnerOrHigher(securityInfo)){
            if(answerSubmissionDto.getAnswerSubmissionId() != null){
                log.error(TextConstants.ID_IN_POST_ERROR);
                return new ResponseEntity(TextConstants.ID_IN_POST_ERROR, HttpStatus.CONFLICT);
            }

            if(!apijwtService.isInstructorOrHigher(securityInfo)){
                Participant participant = submissionService.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, securityInfo.getUserId());
                Optional<Submission> submission = submissionService.findByParticipantIdAndSubmissionId(participant.getParticipantId(), submissionId);
                if(!submission.isPresent()){
                    return new ResponseEntity("Students can only access answer submissions from their own submissions. Submission with id "
                            + submissionId + " does not belong to participant with id " + participant.getParticipantId(), HttpStatus.UNAUTHORIZED);
                }
            }
            answerSubmissionDto.setQuestionSubmissionId(questionSubmissionId);

            QuestionSubmission questionSubmission = allRepositories.questionSubmissionRepository.findByQuestionSubmissionId(questionSubmissionId);
            String answerType = questionSubmission.getQuestion().getQuestionType().toString();
            HttpHeaders headers = new HttpHeaders();
            switch(answerType){
                case "MC":
                    AnswerMcSubmission answerMcSubmission;
                    try {
                        answerMcSubmission = answerSubmissionService.fromDtoMC(answerSubmissionDto);
                    } catch (DataServiceException ex) {
                        return new ResponseEntity("Unable to create answer submission: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
                    }
                    AnswerMcSubmission answerMcSubmissionSaved = answerSubmissionService.saveMC(answerMcSubmission);
                    AnswerSubmissionDto returnedMcDto = answerSubmissionService.toDtoMC(answerMcSubmissionSaved);
                    headers.setLocation(ucBuilder.path(
                            "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/answer_submissions/{type}/{answer_submission_id}")
                            .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId,
                                    answerMcSubmission.getQuestionSubmission().getQuestion().getQuestionType(), answerMcSubmission.getAnswerMcSubId()).toUri());
                    return new ResponseEntity<>(returnedMcDto, headers, HttpStatus.CREATED);
                case "ESSAY":
                    AnswerEssaySubmission answerEssaySubmission;
                    try{
                        answerEssaySubmission = answerSubmissionService.fromDtoEssay(answerSubmissionDto);
                    } catch (DataServiceException ex) {
                        return new ResponseEntity("Unable to create answer submission: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
                    }
                    AnswerEssaySubmission answerEssaySubmissionSaved = answerSubmissionService.saveEssay(answerEssaySubmission);
                    AnswerSubmissionDto returnedEssayDto = answerSubmissionService.toDtoEssay(answerEssaySubmissionSaved);
                    headers.setLocation(ucBuilder.path(
                            "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/answer_submissions/{type}/{answer_submission_id}")
                            .buildAndExpand(experimentId, conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId,
                                    answerEssaySubmission.getQuestionSubmission().getQuestion().getQuestionType(), answerEssaySubmission.getAnswerEssaySubmissionId()).toUri());
                    return new ResponseEntity<>(returnedEssayDto, headers, HttpStatus.CREATED);
                default: return new ResponseEntity("Answer type not supported.", HttpStatus.BAD_REQUEST);
            }
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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, AnswerSubmissionNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securityInfo, assessmentId, submissionId, questionSubmissionId);
        QuestionSubmission questionSubmission = allRepositories.questionSubmissionRepository.findByQuestionSubmissionId(questionSubmissionId);
        String answerType = questionSubmission.getQuestion().getQuestionType().toString();
        apijwtService.answerSubmissionAllowed(securityInfo, questionSubmissionId, answerType, answerSubmissionId);

        if (apijwtService.isLearnerOrHigher(securityInfo)) {
            if(!apijwtService.isInstructorOrHigher(securityInfo)){
                Participant participant = submissionService.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(experimentId, securityInfo.getUserId());
                Optional<Submission> submission = submissionService.findByParticipantIdAndSubmissionId(participant.getParticipantId(), submissionId);
                if(!submission.isPresent()){
                    return new ResponseEntity("Students can only access question submissions from their own submissions. Submission with id "
                            + submissionId + " does not belong to participant with id " + participant.getParticipantId(), HttpStatus.UNAUTHORIZED);
                }
            }
            switch (answerType) {
                case "MC":
                    Optional<AnswerMcSubmission> answerMcSubmissionSearchResult = answerSubmissionService.findByIdMC(answerSubmissionId);
                    if (!answerMcSubmissionSearchResult.isPresent()) {
                        log.error("Unable to update. MC answer submission with id {} not found.", answerSubmissionId);
                        return new ResponseEntity("Unable to update. MC answer submission with id " + answerSubmissionId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
                    }
                    AnswerMcSubmission mcAnswerSubmissionToChange = answerMcSubmissionSearchResult.get();
                    Optional<AnswerMc> answerMc = allRepositories.answerMcRepository.findById(answerSubmissionDto.getAnswerId());
                    if (answerMc.isPresent()) {
                        mcAnswerSubmissionToChange.setAnswerMc(answerMc.get());
                    } else {
                        return new ResponseEntity("the mc answer for the answer submission does not exist.", HttpStatus.BAD_REQUEST);
                    }

                    answerSubmissionService.saveAndFlushMC(mcAnswerSubmissionToChange);
                    return new ResponseEntity<>(HttpStatus.OK);
                case "ESSAY":
                    Optional<AnswerEssaySubmission> answerEssaySubmissionSearchResult = answerSubmissionService.findByIdEssay(answerSubmissionId);
                    if(!answerEssaySubmissionSearchResult.isPresent()){
                        log.error("Unable to update. Essay answer submission with id {} not found.", answerSubmissionId);
                        return new ResponseEntity("Unable to update. Essay answer submission with id " + answerSubmissionId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
                    }
                    AnswerEssaySubmission essayAnswerSubmissionToChange = answerEssaySubmissionSearchResult.get();
                    essayAnswerSubmissionToChange.setResponse(answerSubmissionDto.getResponse());
                    answerSubmissionService.saveAndFlushEssay(essayAnswerSubmissionToChange);
                    return new ResponseEntity<>(HttpStatus.OK);
                default: return new ResponseEntity("Answer type not supported.", HttpStatus.BAD_REQUEST);
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

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.assessmentAllowed(securityInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securityInfo, assessmentId, submissionId, questionSubmissionId);
        QuestionSubmission questionSubmission = allRepositories.questionSubmissionRepository.findByQuestionSubmissionId(questionSubmissionId);
        String answerType = questionSubmission.getQuestion().getQuestionType().toString();
        apijwtService.answerSubmissionAllowed(securityInfo, questionSubmissionId, answerType, answerSubmissionId);

        if(apijwtService.isInstructorOrHigher(securityInfo)){
            switch(answerType){
                case "MC":
                    try{
                        answerSubmissionService.deleteByIdMC(answerSubmissionId);
                        return new ResponseEntity<>(HttpStatus.OK);
                    } catch (EmptyResultDataAccessException ex) {
                        log.error(ex.getMessage());
                        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                    }
                case "ESSAY":
                    try{
                        answerSubmissionService.deleteByIdEssay(answerSubmissionId);
                        return new ResponseEntity<>(HttpStatus.OK);
                    } catch (EmptyResultDataAccessException ex) {
                        log.error(ex.getMessage());
                        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                    }
                default: return new ResponseEntity("Answer type not supported.", HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }
}
