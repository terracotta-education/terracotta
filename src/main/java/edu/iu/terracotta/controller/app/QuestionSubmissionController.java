package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.exceptions.AnswerSubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.DuplicateQuestionException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.TypeNotSupportedException;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
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
    private APIJWTService apijwtService;

    @Autowired
    private QuestionSubmissionService questionSubmissionService;

    @Autowired
    private SubmissionService submissionService;

    @GetMapping("/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions/{submissionId}/question_submissions")
    @ResponseBody
    public ResponseEntity<List<QuestionSubmissionDto>> getQuestionSubmissionsBySubmission(@PathVariable long experimentId,
                                                                                          @PathVariable long conditionId,
                                                                                          @PathVariable long treatmentId,
                                                                                          @PathVariable long assessmentId,
                                                                                          @PathVariable long submissionId,
                                                                                          @RequestParam(name = "answer_submissions", defaultValue = "false") boolean answerSubmissions,
                                                                                          @RequestParam(name = "question_submission_comments", defaultValue = "false") boolean questionSubmissionComments,
                                                                                          HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionNotMatchingException, BadTokenException, InvalidUserException, IOException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securedInfo, assessmentId, submissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        List<QuestionSubmissionDto> questionSubmissionList = questionSubmissionService.getQuestionSubmissions(
            submissionId, answerSubmissions, questionSubmissionComments, assessmentId, apijwtService.isLearner(securedInfo));

        if (questionSubmissionList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(questionSubmissionList, HttpStatus.OK);
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
                                                                       @RequestParam(name = "answer_submissions", defaultValue = "false") boolean answerSubmissions,
                                                                       @RequestParam(name = "question_submission_comments", defaultValue = "false") boolean questionSubmissionComments,
                                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, InvalidUserException, IOException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            if (!apijwtService.isInstructorOrHigher(securedInfo)) {
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            }
            QuestionSubmissionDto questionSubmissionDto = questionSubmissionService.toDto(questionSubmissionService.getQuestionSubmission(questionSubmissionId), answerSubmissions, questionSubmissionComments);
            return new ResponseEntity<>(questionSubmissionDto, HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @PostMapping("/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions/{submissionId}/question_submissions")
    public ResponseEntity<List<QuestionSubmissionDto>> postQuestionSubmission(@PathVariable long experimentId,
                                                                              @PathVariable long conditionId,
                                                                              @PathVariable long treatmentId,
                                                                              @PathVariable long assessmentId,
                                                                              @PathVariable long submissionId,
                                                                              @RequestBody List<QuestionSubmissionDto> questionSubmissionDtoList,
                                                                              UriComponentsBuilder ucBuilder,
                                                                              HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionNotMatchingException, BadTokenException,
                    InvalidUserException, DataServiceException, DuplicateQuestionException, IdMissingException, IdInPostException,
                    TypeNotSupportedException, ExceedingLimitException, AnswerSubmissionNotMatchingException, AnswerNotMatchingException, CanvasApiException,
                    IOException, NoSubmissionsException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securedInfo, assessmentId, submissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        boolean student = false;

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            student = true;
        }

        try {
            questionSubmissionService.canSubmit(securedInfo, experimentId);
            questionSubmissionService.validateAndPrepareQuestionSubmissionList(questionSubmissionDtoList, assessmentId, submissionId, student);
            List<QuestionSubmissionDto> returnedDtoList = questionSubmissionService. postQuestionSubmissions(questionSubmissionDtoList, assessmentId, submissionId, student);
            HttpHeaders headers = questionSubmissionService.buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, assessmentId, submissionId);

            return new ResponseEntity<>(returnedDtoList, headers, HttpStatus.CREATED);
        } catch (AssignmentAttemptException e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.UNAUTHORIZED);
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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, InvalidUserException, DataServiceException, AnswerNotMatchingException, AnswerSubmissionNotMatchingException, IdMissingException {

        log.debug("Updating question submission with id {}", questionSubmissionId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            boolean student = false;
            if (!apijwtService.isInstructorOrHigher(securedInfo)) {
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
                student = true;
            }
            questionSubmissionService.validateQuestionSubmission(questionSubmissionDto);
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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException,
            DataServiceException, BadTokenException, InvalidUserException, AnswerNotMatchingException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            boolean student = false;
            if (!apijwtService.isInstructorOrHigher(securedInfo)) {
                submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
                student = true;
            }
            Map<QuestionSubmission, QuestionSubmissionDto> map = new HashMap<>();
            for (QuestionSubmissionDto questionSubmissionDto : questionSubmissionDtoList) {
                apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionDto.getQuestionSubmissionId());
                QuestionSubmission questionSubmission = questionSubmissionService.getQuestionSubmission(questionSubmissionDto.getQuestionSubmissionId());
                log.debug("Updating question submission with id: {}", questionSubmission.getQuestionSubmissionId());
                questionSubmissionService.validateQuestionSubmission(questionSubmissionDto);
                map.put(questionSubmission, questionSubmissionDto);
            }
            try {
                questionSubmissionService.updateQuestionSubmissions(map, student);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (Exception ex) {
                throw new DataServiceException("Error 105: There was an error updating the question submission list. No question submissions were updated. " + ex.getMessage());
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

        if (apijwtService.isInstructorOrHigher(securedInfo)) {
            try {
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

    @PostMapping(value = "/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions/{submissionId}/question_submissions/file",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<List<QuestionSubmissionDto>> postFileQuestionSubmission(@PathVariable long experimentId,
                                                                                  @PathVariable long conditionId,
                                                                                  @PathVariable long treatmentId,
                                                                                  @PathVariable long assessmentId,
                                                                                  @PathVariable long submissionId,
                                                                                  @RequestParam("question_dto") String questionSubmissionDtoStr,
                                                                                  UriComponentsBuilder ucBuilder,
                                                                                  @RequestPart("file") MultipartFile file,
                                                                                  HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, BadTokenException, InvalidUserException, TypeNotSupportedException, DataServiceException, IdInPostException, IOException, SubmissionNotMatchingException, NoSubmissionsException, CanvasApiException, IdMissingException, ExceedingLimitException, AnswerNotMatchingException, DuplicateQuestionException, AnswerSubmissionNotMatchingException, AssignmentAttemptException {

        if (file.isEmpty()) {
            log.error("File cannot be empty.");
            return new ResponseEntity(TextConstants.FILE_MISSING, HttpStatus.BAD_REQUEST);
        }

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.submissionAllowed(securedInfo, assessmentId, submissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        boolean student = false;

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
            student = true;
        }

        List<QuestionSubmissionDto> returnedDtoList = questionSubmissionService.handleFileQuestionSubmission(file, questionSubmissionDtoStr, experimentId, assessmentId, submissionId, student, securedInfo);
        HttpHeaders headers = questionSubmissionService.buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, assessmentId, submissionId);

        return new ResponseEntity<>(returnedDtoList, headers, HttpStatus.CREATED);
    }

}
