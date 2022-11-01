package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.*;
import edu.iu.terracotta.utils.TextConstants;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestResponseEntityExceptionHandler
        extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestResponseEntityExceptionHandler.class);

    @ExceptionHandler(BadTokenException.class)
    protected ResponseEntity<Object> handleBadTokenException(
            BadTokenException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.BAD_TOKEN;
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = { ExperimentNotMatchingException.class})
    protected ResponseEntity<Object> handleExperimentNotMatchingException(
            ExperimentNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.EXPERIMENT_NOT_MATCHING;
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = { ConditionNotMatchingException.class})
    protected ResponseEntity<Object> handleConditionNotMatchingException(
            ConditionNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.CONDITION_NOT_MATCHING;
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {ParticipantNotMatchingException.class})
    protected ResponseEntity<Object> handleParticipantNotMatchingException(
            ParticipantNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {ExposureNotMatchingException.class})
    protected ResponseEntity<Object> handleExposureNotMatchingException(
            ExposureNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.EXPOSURE_NOT_MATCHING;
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {AssignmentNotMatchingException.class})
    protected ResponseEntity<Object> handleAssignmentNotMatchingException(
            AssignmentNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.ASSIGNMENT_NOT_MATCHING;
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {TreatmentNotMatchingException.class})
    protected ResponseEntity<Object> handleTreatmentNotMatchingException(
            TreatmentNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.TREATMENT_NOT_MATCHING;
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {AssessmentNotMatchingException.class})
    protected ResponseEntity<Object> handleAssessmentNotMatchingException(
            AssessmentNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {QuestionNotMatchingException.class})
    protected ResponseEntity<Object> handleQuestionNotMatchingException(
            QuestionNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.QUESTION_NOT_MATCHING;
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {AnswerNotMatchingException.class})
    protected ResponseEntity<Object> handleAnswerNotMatchingException(
            AnswerNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.ANSWER_NOT_MATCHING;
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {AnswerSubmissionNotMatchingException.class})
    protected ResponseEntity<Object> handleAnswerSubmissionNotMatchingException(
            AnswerSubmissionNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.ANSWER_SUBMISSION_NOT_MATCHING;
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }
    @ExceptionHandler(value
            = {GroupNotMatchingException.class})
    protected ResponseEntity<Object> handleGroupNotMatchingException(
            GroupNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.GROUP_NOT_MATCHING;
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {SubmissionNotMatchingException.class})
    protected ResponseEntity<Object> handleSubmissionNotMatchingException(
            SubmissionNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {QuestionSubmissionNotMatchingException.class})
    protected ResponseEntity<Object> handleQuestionSubmissionNotMatchingException(
            QuestionSubmissionNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.QUESTION_SUBMISSION_NOT_MATCHING;
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {SubmissionCommentNotMatchingException.class})
    protected ResponseEntity<Object> handleSubmissionCommentNotMatchingException(
            SubmissionCommentNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.SUBMISSION_COMMENT_NOT_MATCHING;
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {QuestionSubmissionCommentNotMatchingException.class})
    protected ResponseEntity<Object> handleQuestionSubmissionCommentNotMatchingException(
            QuestionSubmissionCommentNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.QUESTION_SUBMISSION_COMMENT_NOT_MATCHING;
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {OutcomeNotMatchingException.class})
    protected ResponseEntity<Object> handleOutcomeNotMatchingException(
            OutcomeNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.OUTCOME_NOT_MATCHING;
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {OutcomeScoreNotMatchingException.class})
    protected ResponseEntity<Object> handleOutcomeScoreNotMatchingException(
            OutcomeScoreNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.OUTCOME_SCORE_NOT_MATCHING;
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {IdMissingException.class})
    protected ResponseEntity<Object> handleIdMissingException(
            IdMissingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.ID_MISSING;
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }



    @ExceptionHandler(value
            = {BadConsentFileTypeException.class})
    protected ResponseEntity<Object> handleBadConsentFileTypeException(
            BadConsentFileTypeException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = { ExpiredJwtException.class})
    protected ResponseEntity<Object> handleExpiredJwtException(
            ExpiredJwtException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = { ParticipantNotUpdatedException.class})
    protected ResponseEntity<Object> handleParticipantNotUpdatedException(
            ParticipantNotUpdatedException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(value
            = {DataServiceException.class})
    protected ResponseEntity<Object> handleDataServiceException(
            DataServiceException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(value
            = {WrongValueException.class})
    protected ResponseEntity<Object> handleWrongValueException(
            WrongValueException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(value
            = {AssignmentDatesException.class})
    protected ResponseEntity<Object> handleAssignmentDatesException(
            AssignmentDatesException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {ExperimentLockedException.class})
    protected ResponseEntity<Object> handleExperimentLockedException(
            ExperimentLockedException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value = {TitleValidationException.class})
    protected ResponseEntity<Object> handleTitleValidationException(
            TitleValidationException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        if(bodyOfResponse.startsWith("Error 100") || bodyOfResponse.startsWith("Error 102")){
            return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.CONFLICT, request);
        } else {
            return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
        }
    }

    @ExceptionHandler(value
            = {ConditionsLockedException.class})
    protected ResponseEntity<Object> handleConditionsLockedException(
            ConditionsLockedException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(value
            = {MultipleChoiceLimitReachedException.class})
    protected ResponseEntity<Object> handleMultipleChoiceLimitReachedException(
            MultipleChoiceLimitReachedException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(value
            = {InvalidUserException.class})
    protected ResponseEntity<Object> handleInvalidUserException(
            InvalidUserException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value = {InvalidParticipantException.class})
    protected ResponseEntity<Object> handleInvalidParticipantException(InvalidParticipantException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        if(bodyOfResponse.startsWith("Error 105")) {
            return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
        }
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(value
            = {InvalidQuestionTypeException.class})
    protected ResponseEntity<Object> handleInvalidQuestionTypeException(
            InvalidQuestionTypeException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value
            = {DuplicateQuestionException.class})
    protected ResponseEntity<Object> handleDuplicateQuestionException(
            DuplicateQuestionException ex, WebRequest request){
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(value = {NoSubmissionsException.class})
    protected ResponseEntity<Object> handleNoSubmissionsException(NoSubmissionsException ex, WebRequest request){
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        if(bodyOfResponse.startsWith("A submission")){
            return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
        }
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.NO_CONTENT, request);
    }

    @ExceptionHandler(value = {AssignmentNotCreatedException.class})
    protected ResponseEntity<Object> handleAssignmentNotCreatedException(AssignmentNotCreatedException ex, WebRequest request){
        log.warn(ex.getMessage());
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(value = {AssignmentNotEditedException.class})
    protected ResponseEntity<Object> handleAssignmentNotEditedException(AssignmentNotEditedException ex, WebRequest request){
        log.warn(ex.getMessage());
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(value = {IdInPostException.class})
    protected ResponseEntity<Object> handleIdInPostException(IdInPostException ex, WebRequest request){
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(value = {ExceedingLimitException.class})
    protected ResponseEntity<Object> handleExceedingLimitException(ExceedingLimitException ex, WebRequest request){
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.ALREADY_REPORTED, request);
    }

    @ExceptionHandler(value = {NegativePointsException.class})
    protected ResponseEntity<Object> handleNegativePointsException(NegativePointsException ex, WebRequest request){
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(value = {TypeNotSupportedException.class})
    protected ResponseEntity<Object> handleTypeNotSupportedException(TypeNotSupportedException ex, WebRequest request){
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value = {ExperimentConditionLimitReachedException.class})
    protected ResponseEntity<Object> handleExperimentConditionReachedException(ExperimentConditionLimitReachedException ex, WebRequest request){
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value = { RevealResponsesSettingValidationException.class })
    protected ResponseEntity<Object> handleRevealResponsesSettingValidationException(
                    RevealResponsesSettingValidationException ex,
                    WebRequest request) {
            String bodyOfResponse = ex.getMessage();
            log.warn(bodyOfResponse);
            return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value = { MultipleAttemptsSettingsValidationException.class })
    protected ResponseEntity<Object> handleMultipleAttemptsSettingsValidationException(
                    MultipleAttemptsSettingsValidationException ex,
                    WebRequest request) {
            String bodyOfResponse = ex.getMessage();
            log.warn(bodyOfResponse);
            return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value = { AssignmentAttemptException.class })
    protected ResponseEntity<Object> handleAssignmentAttemptException(
                    AssignmentAttemptException ex,
                    WebRequest request) {
            String bodyOfResponse = ex.getMessage();
            log.warn(bodyOfResponse);
            return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

}
