package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.*;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.Submission;
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

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions/{submissionId}/question_submissions";
    private static final Logger log = LoggerFactory.getLogger(QuestionSubmissionController.class);

    @Autowired
    private APIJWTService apijwtService;

    @Autowired
    private QuestionSubmissionService questionSubmissionService;

    @Autowired
    private SubmissionService submissionService;

    @GetMapping
    public ResponseEntity<List<QuestionSubmissionDto>> getQuestionSubmissionsBySubmission(@PathVariable long experimentId,
                                                                                          @PathVariable long conditionId,
                                                                                          @PathVariable long treatmentId,
                                                                                          @PathVariable long assessmentId,
                                                                                          @PathVariable long submissionId,
                                                                                          @RequestParam(name = "answer_submissions", defaultValue = "false") boolean answerSubmissions,
                                                                                          @RequestParam(name = "question_submission_comments", defaultValue = "false") boolean questionSubmissionComments,
                                                                                          HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, SubmissionNotMatchingException, BadTokenException, InvalidUserException {
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

    @GetMapping("/{questionSubmissionId}")
    public ResponseEntity<QuestionSubmissionDto> getQuestionSubmission(@PathVariable long experimentId,
                                                                       @PathVariable long conditionId,
                                                                       @PathVariable long treatmentId,
                                                                       @PathVariable long assessmentId,
                                                                       @PathVariable long submissionId,
                                                                       @PathVariable long questionSubmissionId,
                                                                       @RequestParam(name = "answer_submissions", defaultValue = "false") boolean answerSubmissions,
                                                                       @RequestParam(name = "question_submission_comments", defaultValue = "false") boolean questionSubmissionComments,
                                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, InvalidUserException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            submissionService.validateUser(experimentId, securedInfo.getUserId(), submissionId);
        }

        QuestionSubmissionDto questionSubmissionDto = questionSubmissionService.toDto(questionSubmissionService.getQuestionSubmission(questionSubmissionId), answerSubmissions, questionSubmissionComments);

        return new ResponseEntity<>(questionSubmissionDto, HttpStatus.OK);
    }

    @PostMapping
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

        Submission submission = submissionService.getSubmission(experimentId, securedInfo.getUserId(), submissionId, student);
        String assignmentId = submission.getAssessment().getTreatment().getAssignment().getLmsAssignmentId();

        try {
            questionSubmissionService.canSubmit(securedInfo.getCanvasCourseId(), assignmentId,securedInfo.getCanvasUserId(), securedInfo.getPlatformDeploymentId());
            questionSubmissionService.validateAndPrepareQuestionSubmissionList(questionSubmissionDtoList, assessmentId, submissionId, student);
            List<QuestionSubmissionDto> returnedDtoList = questionSubmissionService. postQuestionSubmissions(questionSubmissionDtoList, assessmentId, submissionId, student);
            HttpHeaders headers = questionSubmissionService.buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, assessmentId, submissionId);

            return new ResponseEntity<>(returnedDtoList, headers, HttpStatus.CREATED);
        } catch (AssignmentAttemptException e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }


    @PutMapping("/{questionSubmissionId}")
    public ResponseEntity<Void> updateQuestionSubmission(@PathVariable long experimentId,
                                                         @PathVariable long conditionId,
                                                         @PathVariable long treatmentId,
                                                         @PathVariable long assessmentId,
                                                         @PathVariable long submissionId,
                                                         @PathVariable long questionSubmissionId,
                                                         @RequestBody QuestionSubmissionDto questionSubmissionDto,
                                                         HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, InvalidUserException, DataServiceException, AnswerNotMatchingException, AnswerSubmissionNotMatchingException, IdMissingException {

        log.debug("Updating question submission with id {}", questionSubmissionId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

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
    }

    @PutMapping
    public ResponseEntity<Void> updateQuestionSubmissions(@PathVariable long experimentId,
                                                          @PathVariable long conditionId,
                                                          @PathVariable long treatmentId,
                                                          @PathVariable long assessmentId,
                                                          @PathVariable long submissionId,
                                                          @RequestBody List<QuestionSubmissionDto> questionSubmissionDtoList,
                                                          HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException,
            DataServiceException, BadTokenException, InvalidUserException, AnswerNotMatchingException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

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
    }

    @DeleteMapping("/{questionSubmissionId}")
    public ResponseEntity<Void> deleteQuestionSubmission(@PathVariable long experimentId,
                                                         @PathVariable long conditionId,
                                                         @PathVariable long treatmentId,
                                                         @PathVariable long assessmentId,
                                                         @PathVariable long submissionId,
                                                         @PathVariable long questionSubmissionId,
                                                         HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);
        apijwtService.questionSubmissionAllowed(securedInfo, assessmentId, submissionId, questionSubmissionId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        try {
            questionSubmissionService.deleteById(questionSubmissionId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EmptyResultDataAccessException ex) {
            log.warn(ex.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}
