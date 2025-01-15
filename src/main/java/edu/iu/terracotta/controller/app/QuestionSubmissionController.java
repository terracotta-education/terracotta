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
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = QuestionSubmissionController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class QuestionSubmissionController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/conditions/{conditionId}/treatments/{treatmentId}/assessments/{assessmentId}/submissions/{submissionId}/question_submissions";

    @Autowired private APIJWTService apijwtService;
    @Autowired private QuestionSubmissionService questionSubmissionService;
    @Autowired private SubmissionService submissionService;

    @GetMapping({"", "/"})
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
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, QuestionSubmissionNotMatchingException, BadTokenException, InvalidUserException, IOException {

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

        try {
            questionSubmissionService.canSubmit(securedInfo, experimentId);
            questionSubmissionService.validateAndPrepareQuestionSubmissionList(questionSubmissionDtoList, assessmentId, submissionId, student);
            List<QuestionSubmissionDto> returnedDtoList = questionSubmissionService.postQuestionSubmissions(questionSubmissionDtoList, assessmentId, submissionId, student);
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
        log.debug("Updating question submission with id: [{}]", questionSubmissionId);
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
            log.debug("Updating question submission with id: [{}]", questionSubmission.getQuestionSubmissionId());
            questionSubmissionService.validateQuestionSubmission(questionSubmissionDto);
            map.put(questionSubmission, questionSubmissionDto);
        }

        try {
            questionSubmissionService.updateQuestionSubmissions(map, student);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception ex) {
            throw new DataServiceException("Error 105: There was an error updating the question submission list. No question submissions were updated. " + ex.getMessage(), ex);
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

    @PostMapping(value = "/file", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
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

    @PutMapping(value = "/{questionSubmissionId}/file", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<List<QuestionSubmissionDto>> putFileQuestionSubmission(@PathVariable long experimentId,
                                                                                  @PathVariable long conditionId,
                                                                                  @PathVariable long treatmentId,
                                                                                  @PathVariable long assessmentId,
                                                                                  @PathVariable long submissionId,
                                                                                  @PathVariable long questionSubmissionId,
                                                                                  @RequestParam("question_dto") String questionSubmissionDtoStr,
                                                                                  UriComponentsBuilder ucBuilder,
                                                                                  @RequestPart("file") MultipartFile file,
                                                                                  HttpServletRequest req)
            throws ExperimentNotMatchingException, AssessmentNotMatchingException, BadTokenException, InvalidUserException, TypeNotSupportedException, DataServiceException, IdInPostException, IOException, SubmissionNotMatchingException, NoSubmissionsException, CanvasApiException, IdMissingException, ExceedingLimitException, AnswerNotMatchingException, DuplicateQuestionException, AnswerSubmissionNotMatchingException, AssignmentAttemptException, QuestionSubmissionNotMatchingException {
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

        List<QuestionSubmissionDto> returnedDtoList = questionSubmissionService.handleFileQuestionSubmissionUpdate(file, questionSubmissionDtoStr, experimentId, assessmentId, submissionId, questionSubmissionId, student, securedInfo);
        HttpHeaders headers = questionSubmissionService.buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, assessmentId, submissionId);

        return new ResponseEntity<>(returnedDtoList, headers, HttpStatus.CREATED);
    }

}
