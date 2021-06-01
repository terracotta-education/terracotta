package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadConsentFileTypeException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.utils.TextConstants;
import io.jsonwebtoken.ExpiredJwtException;
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

    @ExceptionHandler(value
            = { BadTokenException.class})
    protected ResponseEntity<Object> handleBadTokenException(
            BadTokenException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.BAD_TOKEN;
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = { ExperimentNotMatchingException.class})
    protected ResponseEntity<Object> handleExperimentNotMatchingException(
            ExperimentNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.EXPERIMENT_NOT_MATCHING;
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = { ConditionNotMatchingException.class})
    protected ResponseEntity<Object> handleConditionNotMatchingException(
            ConditionNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.CONDITION_NOT_MATCHING;
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {ParticipantNotMatchingException.class})
    protected ResponseEntity<Object> handleParticipantNotMatchingException(
            ParticipantNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.PARTICIPANT_NOT_MATCHING;
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {ExposureNotMatchingException.class})
    protected ResponseEntity<Object> handleExposureNotMatchingException(
            ExposureNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.EXPOSURE_NOT_MATCHING;
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {AssignmentNotMatchingException.class})
    protected ResponseEntity<Object> handleAssignmentNotMatchingException(
            AssignmentNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.ASSIGNMENT_NOT_MATCHING;
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {TreatmentNotMatchingException.class})
    protected ResponseEntity<Object> handleTreatmentNotMatchingException(
            TreatmentNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.TREATMENT_NOT_MATCHING;
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {AssessmentNotMatchingException.class})
    protected ResponseEntity<Object> handleAssessmentNotMatchingException(
            AssessmentNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.ASSESSMENT_NOT_MATCHING;
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {QuestionNotMatchingException.class})
    protected ResponseEntity<Object> handleQuestionNotMatchingException(
            QuestionNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.QUESTION_NOT_MATCHING;
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {AnswerNotMatchingException.class})
    protected ResponseEntity<Object> handleAnswerNotMatchingException(
            AnswerNotMatchingException ex, WebRequest request) {
        String bodyOfResponse = TextConstants.ANSWER_NOT_MATCHING;
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {BadConsentFileTypeException.class})
    protected ResponseEntity<Object> handleBadConsentFileTypeException(
            BadConsentFileTypeException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = { ExpiredJwtException.class})
    protected ResponseEntity<Object> handleExpiredJwtException(
            ExpiredJwtException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value
            = {DataServiceException.class})
    protected ResponseEntity<Object> handleDataServiceException(
            DataServiceException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.CONFLICT, request);
    }
}
