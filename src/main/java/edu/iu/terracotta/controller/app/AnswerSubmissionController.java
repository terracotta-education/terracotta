package edu.iu.terracotta.controller.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.iu.terracotta.exceptions.*;
import edu.iu.terracotta.model.app.dto.AnswerSubmissionDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AnswerSubmissionService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.utils.TextConstants;
import liquibase.pro.packaged.O;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@Controller
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = AnswerSubmissionController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class AnswerSubmissionController {

    /**
     * This controller was built to support the addition of answer submission types.
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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, InvalidUserException, DataServiceException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            if (!apijwtService.isInstructorOrHigher(securedInfo)) {
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
            List<AnswerSubmissionDto> answerSubmissionDtoList = answerSubmissionService.getAnswerSubmissions(questionSubmissionId, answerType);
            if (answerSubmissionDtoList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(answerSubmissionDtoList, HttpStatus.OK);
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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, AnswerSubmissionNotMatchingException, BadTokenException, InvalidUserException, DataServiceException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
        apijwtService.answerSubmissionAllowed(securedInfo, questionSubmissionId, answerType, answerSubmissionId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            if (!apijwtService.isInstructorOrHigher(securedInfo)) {
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            return new ResponseEntity<>(answerSubmissionService.getAnswerSubmission(answerSubmissionId, answerType), HttpStatus.OK);
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

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            if (!apijwtService.isInstructorOrHigher(securedInfo)) {
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            AnswerSubmissionDto returnedDto = answerSubmissionService.postAnswerSubmission(answerSubmissionDto, questionSubmissionId);
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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, AnswerSubmissionNotMatchingException, BadTokenException, InvalidUserException, AnswerNotMatchingException, DataServiceException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
        apijwtService.answerSubmissionAllowed(securedInfo, questionSubmissionId, answerType, answerSubmissionId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            if (!apijwtService.isInstructorOrHigher(securedInfo)) {
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            try {
                answerSubmissionService.updateAnswerSubmission(answerSubmissionDto, answerSubmissionId, answerType);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (Exception e) {
                throw new DataServiceException("Error 105: Unable to update answer submission: " + e.getMessage());
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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, AnswerSubmissionNotMatchingException, BadTokenException, DataServiceException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);
        String answerType = answerSubmissionService.getAnswerType(questionSubmissionId);
        apijwtService.answerSubmissionAllowed(securedInfo, questionSubmissionId, answerType, answerSubmissionId);

        if (apijwtService.isInstructorOrHigher(securedInfo)) {
            try {
                answerSubmissionService.deleteAnswerSubmission(answerSubmissionId, answerType);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (DataServiceException e) {
                throw new DataServiceException("Error 105: Could not delete answer submission. " + e.getMessage());
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}/treatments/{treatment_id}/assessments/{assessment_id}/submissions/{submission_id}/question_submissions/{question_submission_id}/answer_submissions/file",
            method = RequestMethod.POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<AnswerSubmissionDto> postFileAnswerSubmission(@PathVariable("experiment_id") Long experimentId,
                                                                        @PathVariable("condition_id") Long conditionId,
                                                                        @PathVariable("treatment_id") Long treatmentId,
                                                                        @PathVariable("assessment_id") Long assessmentId,
                                                                        @PathVariable("submission_id") Long submissionId,
                                                                        @PathVariable("question_submission_id") Long questionSubmissionId,
                                                                        @RequestParam("answer_dto") String answerSubmissionDtoStr,
                                                                        UriComponentsBuilder ucBuilder,
                                                                        @RequestPart("file") MultipartFile file,
                                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, InvalidUserException, TypeNotSupportedException, DataServiceException, IdInPostException, IOException {

        if (file.isEmpty()) {
            log.error("Invalid file ");
            return new ResponseEntity(TextConstants.FILE_MISSING, HttpStatus.BAD_REQUEST);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        AnswerSubmissionDto answerSubmissionDto = objectMapper.readValue(answerSubmissionDtoStr,AnswerSubmissionDto.class);
        answerSubmissionDto.setFileContent(file.getBytes());
        log.info("Creating answer submission: {}", answerSubmissionDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            if (!apijwtService.isInstructorOrHigher(securedInfo)) {
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            AnswerSubmissionDto returnedDto = answerSubmissionService.postAnswerSubmission(answerSubmissionDto,
                    questionSubmissionId);
            HttpHeaders headers = answerSubmissionService.
                    buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, assessmentId, submissionId,
                            questionSubmissionId, returnedDto.getAnswerSubmissionId());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }
}