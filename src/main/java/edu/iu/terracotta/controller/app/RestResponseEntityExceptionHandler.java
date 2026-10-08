package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;
import edu.iu.terracotta.dao.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AnswerSubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.dao.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.dao.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.dao.exceptions.OutcomeScoreNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionSubmissionCommentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.SubmissionCommentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.BadConsentFileTypeException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.ConditionsLockedException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.DuplicateQuestionException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.ExperimentConditionLimitReachedException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.InvalidParticipantException;
import edu.iu.terracotta.exceptions.InvalidQuestionTypeException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.TypeNotSupportedException;
import edu.iu.terracotta.exceptions.WrongValueException;
import edu.iu.terracotta.utils.LmsAuthorizationUtils;
import edu.iu.terracotta.utils.TextConstants;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
@SuppressWarnings({"PMD.GuardLogStatement"})
public class RestResponseEntityExceptionHandler
        extends ResponseEntityExceptionHandler {

    public static final String LMS_REAUTHORIZATION_HEADER = "X-Terracotta-Lms-Reauthorization";

    // another request changed the same row first (e.g. two overlapping saves of the same
    // assignments). The transaction has already rolled back, so nothing was half-written - this is
    // a conflict the user can resolve by reloading, not a server error.
    @ExceptionHandler({ OptimisticLockingFailureException.class })
    protected ResponseEntity<Object> handleOptimisticLockingFailureException(OptimisticLockingFailureException ex, WebRequest request) {
        String bodyOfResponse = "This was changed by another save while yours was in progress. Refresh the page and try again.";

        if (ex instanceof ObjectOptimisticLockingFailureException objectEx) {
            log.warn("Save conflict on [{}] with ID: [{}]; it was changed by another request", objectEx.getPersistentClassName(), objectEx.getIdentifier());
        } else {
            log.warn("Save conflict: {}", ex.getMessage());
        }

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler({ BadTokenException.class})
    protected ResponseEntity<Object> handleBadTokenException(BadTokenException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.BAD_TOKEN;
        log.warn(bodyOfResponse);

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({ ExperimentNotMatchingException.class})
    protected ResponseEntity<Object> handleExperimentNotMatchingException(ExperimentNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.EXPERIMENT_NOT_MATCHING;
        // the exception's own message can name the experiment and course (see
        // ApiJwtServiceImpl.experimentAllowed) - logged, but not sent back in the response
        log.warn(StringUtils.defaultIfBlank(ex.getMessage(), bodyOfResponse));

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

        return withLmsReauthorization(ex, bodyOfResponse, HttpStatus.CONFLICT, request);
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
        log.warn(ex.getLogMessage());

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

        return withLmsReauthorization(ex, ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler({AssignmentNotEditedException.class})
    protected ResponseEntity<Object> handleAssignmentNotEditedException(AssignmentNotEditedException ex, WebRequest request) {
        log.warn(ex.getMessage());

        return withLmsReauthorization(ex, ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, request);
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

        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.CONFLICT, request);
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

    // was previously unhandled, resulting in a raw uncaught-exception 500 (with the full
    // stack trace) for any LMS API failure - e.g. a Canvas throttling/permissions error hit
    // while checking submission attempts - instead of a clean mapped error response
    @ExceptionHandler({ ApiException.class })
    protected ResponseEntity<Object> handleApiException(ApiException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        log.warn(bodyOfResponse);

        return withLmsReauthorization(ex, bodyOfResponse, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler({ LmsOAuthException.class })
    protected ResponseEntity<Object> handleLmsOAuthException(LmsOAuthException ex, WebRequest request) {
        log.warn(ex.getMessage());

        return withLmsReauthorization(ex, ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    // an LMS call failed because the user's LMS API token no longer works. Only relaunching
    // Terracotta can fix that (the authorization link needs the launch), so replace the raw error
    // with a message saying so and flag the response for the frontend, which shows it once.
    // The status is left as it was so existing error handling on each screen is unchanged.
    private ResponseEntity<Object> withLmsReauthorization(Exception ex, String bodyOfResponse, HttpStatus status, WebRequest request) {
        if (!LmsAuthorizationUtils.isAuthorizationFailure(ex)) {
            return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), status, request);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(LMS_REAUTHORIZATION_HEADER, "true");

        return handleExceptionInternal(ex, TextConstants.LMS_REAUTHORIZATION_REQUIRED, headers, status, request);
    }

    /**
     * Spring's own handling of a response that has already started sending only logs the exception,
     * not which request it was, so these warnings couldn't be traced to an endpoint. This logs the
     * request too, and logs a client that disconnected mid-response (e.g. the user left the page) at
     * debug, since there's nothing to fix on the server side.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            HttpServletResponse response = servletWebRequest.getResponse();

            if (response != null && response.isCommitted()) {
                HttpServletRequest httpServletRequest = servletWebRequest.getRequest();

                if (isClientDisconnect(ex)) {
                    log.debug(
                        "Client disconnected before the response to [{} {}] was fully sent: {}",
                        httpServletRequest.getMethod(),
                        httpServletRequest.getRequestURI(),
                        ExceptionUtils.getRootCauseMessage(ex)
                    );
                } else {
                    log.warn(
                        "Response to [{} {}] was already sent; ignoring: {}",
                        httpServletRequest.getMethod(),
                        httpServletRequest.getRequestURI(),
                        ex.toString()
                    );
                }

                return null;
            }
        }

        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private static boolean isClientDisconnect(Throwable ex) {
        return ExceptionUtils.getThrowableList(ex).stream()
            .anyMatch(
                throwable -> "org.apache.catalina.connector.ClientAbortException".equals(throwable.getClass().getName())
                    || (throwable instanceof IOException
                        && Strings.CI.containsAny(throwable.getMessage(), "Broken pipe", "Connection reset"))
            );
    }

}
