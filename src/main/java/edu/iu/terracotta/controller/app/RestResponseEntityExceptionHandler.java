package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.exceptions.AnswerSubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadConsentFileTypeException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.exceptions.ConditionsLockedException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.DuplicateQuestionException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.ExperimentConditionLimitReachedException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.InvalidParticipantException;
import edu.iu.terracotta.exceptions.InvalidQuestionTypeException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.exceptions.OutcomeScoreNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.QuestionSubmissionCommentNotMatchingException;
import edu.iu.terracotta.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.SubmissionCommentNotMatchingException;
import edu.iu.terracotta.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.exceptions.TypeNotSupportedException;
import edu.iu.terracotta.exceptions.WrongValueException;
import edu.iu.terracotta.utils.TextConstants;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
@SuppressWarnings({"PMD.GuardLogStatement"})
public class RestResponseEntityExceptionHandler
        extends ResponseEntityExceptionHandler {

    @ExceptionHandler({ BadTokenException.class})
    protected ResponseEntity<Object> handleBadTokenException(BadTokenException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.BAD_TOKEN;
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({ ExperimentNotMatchingException.class})
    protected ResponseEntity<Object> handleExperimentNotMatchingException(ExperimentNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.EXPERIMENT_NOT_MATCHING;
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({ ConditionNotMatchingException.class})
    protected ResponseEntity<Object> handleConditionNotMatchingException(ConditionNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.CONDITION_NOT_MATCHING;
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({ParticipantNotMatchingException.class})
    protected ResponseEntity<Object> handleParticipantNotMatchingException(ParticipantNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({ExposureNotMatchingException.class})
    protected ResponseEntity<Object> handleExposureNotMatchingException(ExposureNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.EXPOSURE_NOT_MATCHING;
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({AssignmentNotMatchingException.class})
    protected ResponseEntity<Object> handleAssignmentNotMatchingException(AssignmentNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.ASSIGNMENT_NOT_MATCHING;
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({TreatmentNotMatchingException.class})
    protected ResponseEntity<Object> handleTreatmentNotMatchingException(TreatmentNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.TREATMENT_NOT_MATCHING;
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({AssessmentNotMatchingException.class})
    protected ResponseEntity<Object> handleAssessmentNotMatchingException(
        AssessmentNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({QuestionNotMatchingException.class})
    protected ResponseEntity<Object> handleQuestionNotMatchingException(
        QuestionNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.QUESTION_NOT_MATCHING;
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({AnswerNotMatchingException.class})
    protected ResponseEntity<Object> handleAnswerNotMatchingException(
        AnswerNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.ANSWER_NOT_MATCHING;
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({AnswerSubmissionNotMatchingException.class})
    protected ResponseEntity<Object> handleAnswerSubmissionNotMatchingException(
        AnswerSubmissionNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.ANSWER_SUBMISSION_NOT_MATCHING;
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }
    @ExceptionHandler({GroupNotMatchingException.class})
    protected ResponseEntity<Object> handleGroupNotMatchingException(
        GroupNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.GROUP_NOT_MATCHING;
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({SubmissionNotMatchingException.class})
    protected ResponseEntity<Object> handleSubmissionNotMatchingException(
        SubmissionNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({QuestionSubmissionNotMatchingException.class})
    protected ResponseEntity<Object> handleQuestionSubmissionNotMatchingException(
        QuestionSubmissionNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.QUESTION_SUBMISSION_NOT_MATCHING;
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({SubmissionCommentNotMatchingException.class})
    protected ResponseEntity<Object> handleSubmissionCommentNotMatchingException(
        SubmissionCommentNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.SUBMISSION_COMMENT_NOT_MATCHING;
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({QuestionSubmissionCommentNotMatchingException.class})
    protected ResponseEntity<Object> handleQuestionSubmissionCommentNotMatchingException(
        QuestionSubmissionCommentNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.QUESTION_SUBMISSION_COMMENT_NOT_MATCHING;
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({OutcomeNotMatchingException.class})
    protected ResponseEntity<Object> handleOutcomeNotMatchingException(
        OutcomeNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.OUTCOME_NOT_MATCHING;
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({OutcomeScoreNotMatchingException.class})
    protected ResponseEntity<Object> handleOutcomeScoreNotMatchingException(
        OutcomeScoreNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.OUTCOME_SCORE_NOT_MATCHING;
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({IdMissingException.class})
    protected ResponseEntity<Object> handleIdMissingException(
        IdMissingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.ID_MISSING;
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({BadConsentFileTypeException.class})
    protected ResponseEntity<Object> handleBadConsentFileTypeException(
        BadConsentFileTypeException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({ ExpiredJwtException.class})
    protected ResponseEntity<Object> handleExpiredJwtException(
        ExpiredJwtException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({ ParticipantNotUpdatedException.class})
    protected ResponseEntity<Object> handleParticipantNotUpdatedException(
        ParticipantNotUpdatedException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler({DataServiceException.class})
    protected ResponseEntity<Object> handleDataServiceException(
        DataServiceException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler({WrongValueException.class})
    protected ResponseEntity<Object> handleWrongValueException(
        WrongValueException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler({AssignmentDatesException.class})
    protected ResponseEntity<Object> handleAssignmentDatesException(
        AssignmentDatesException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({ExperimentLockedException.class})
    protected ResponseEntity<Object> handleExperimentLockedException(
        ExperimentLockedException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({TitleValidationException.class})
    protected ResponseEntity<Object> handleTitleValidationException(TitleValidationException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        if (bodyOfResponse.startsWith("Error 100") || bodyOfResponse.startsWith("Error 102")) {
            return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.CONFLICT, request);
        }

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({ConditionsLockedException.class})
    protected ResponseEntity<Object> handleConditionsLockedException(
        ConditionsLockedException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler({MultipleChoiceLimitReachedException.class})
    protected ResponseEntity<Object> handleMultipleChoiceLimitReachedException(
        MultipleChoiceLimitReachedException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler({InvalidUserException.class})
    protected ResponseEntity<Object> handleInvalidUserException(
        InvalidUserException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({InvalidParticipantException.class})
    protected ResponseEntity<Object> handleInvalidParticipantException(InvalidParticipantException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        if (bodyOfResponse.startsWith("Error 105")) {
            return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
        }

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler({InvalidQuestionTypeException.class})
    protected ResponseEntity<Object> handleInvalidQuestionTypeException(InvalidQuestionTypeException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({DuplicateQuestionException.class})
    protected ResponseEntity<Object> handleDuplicateQuestionException(DuplicateQuestionException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse,new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler({NoSubmissionsException.class})
    protected ResponseEntity<Object> handleNoSubmissionsException(NoSubmissionsException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        if (bodyOfResponse.startsWith("A submission")) {
            return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
        }

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.NO_CONTENT, request);
    }

    @ExceptionHandler({AssignmentNotCreatedException.class})
    protected ResponseEntity<Object> handleAssignmentNotCreatedException(AssignmentNotCreatedException ex, WebRequest request) {
        log.warn(ex.getMessage());

        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler({AssignmentNotEditedException.class})
    protected ResponseEntity<Object> handleAssignmentNotEditedException(AssignmentNotEditedException ex, WebRequest request) {
        log.warn(ex.getMessage());

        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler({IdInPostException.class})
    protected ResponseEntity<Object> handleIdInPostException(IdInPostException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler({ExceedingLimitException.class})
    protected ResponseEntity<Object> handleExceedingLimitException(ExceedingLimitException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.ALREADY_REPORTED, request);
    }

    @ExceptionHandler({NegativePointsException.class})
    protected ResponseEntity<Object> handleNegativePointsException(NegativePointsException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler({TypeNotSupportedException.class})
    protected ResponseEntity<Object> handleTypeNotSupportedException(TypeNotSupportedException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({ExperimentConditionLimitReachedException.class})
    protected ResponseEntity<Object> handleExperimentConditionReachedException(ExperimentConditionLimitReachedException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({ RevealResponsesSettingValidationException.class })
    protected ResponseEntity<Object> handleRevealResponsesSettingValidationException(    RevealResponsesSettingValidationException ex,    WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({ MultipleAttemptsSettingsValidationException.class })
    protected ResponseEntity<Object> handleMultipleAttemptsSettingsValidationException(    MultipleAttemptsSettingsValidationException ex,    WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({ AssignmentAttemptException.class })
    protected ResponseEntity<Object> handleAssignmentAttemptException(    AssignmentAttemptException ex,    WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

}
