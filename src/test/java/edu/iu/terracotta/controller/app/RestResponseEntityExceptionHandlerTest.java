package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
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
import edu.iu.terracotta.utils.TextConstants;
import io.jsonwebtoken.ExpiredJwtException;

public class RestResponseEntityExceptionHandlerTest {

    private RestResponseEntityExceptionHandler handler;
    private WebRequest webRequest;

    @BeforeEach
    void beforeEach() {
        handler = new RestResponseEntityExceptionHandler();
        webRequest = mock(WebRequest.class);
    }

    @Test
    void handleBadTokenExceptionTest() {
        ResponseEntity<Object> response = handler.handleBadTokenException(new BadTokenException("bad token"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.BAD_TOKEN, response.getBody());
    }

    @Test
    void handleExperimentNotMatchingExceptionTest() {
        ResponseEntity<Object> response = handler.handleExperimentNotMatchingException(new ExperimentNotMatchingException("msg"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.EXPERIMENT_NOT_MATCHING, response.getBody());
    }

    @Test
    void handleConditionNotMatchingExceptionTest() {
        ResponseEntity<Object> response = handler.handleConditionNotMatchingException(new ConditionNotMatchingException("msg"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.CONDITION_NOT_MATCHING, response.getBody());
    }

    @Test
    void handleParticipantNotMatchingExceptionTest() {
        ResponseEntity<Object> response = handler.handleParticipantNotMatchingException(new ParticipantNotMatchingException("participant msg"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("participant msg", response.getBody());
    }

    @Test
    void handleExposureNotMatchingExceptionTest() {
        ResponseEntity<Object> response = handler.handleExposureNotMatchingException(new ExposureNotMatchingException("msg"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.EXPOSURE_NOT_MATCHING, response.getBody());
    }

    @Test
    void handleAssignmentNotMatchingExceptionTest() {
        ResponseEntity<Object> response = handler.handleAssignmentNotMatchingException(new AssignmentNotMatchingException("msg"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.ASSIGNMENT_NOT_MATCHING, response.getBody());
    }

    @Test
    void handleTreatmentNotMatchingExceptionTest() {
        ResponseEntity<Object> response = handler.handleTreatmentNotMatchingException(new TreatmentNotMatchingException("msg"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.TREATMENT_NOT_MATCHING, response.getBody());
    }

    @Test
    void handleAssessmentNotMatchingExceptionTest() {
        ResponseEntity<Object> response = handler.handleAssessmentNotMatchingException(new AssessmentNotMatchingException("assessment msg"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("assessment msg", response.getBody());
    }

    @Test
    void handleQuestionNotMatchingExceptionTest() {
        ResponseEntity<Object> response = handler.handleQuestionNotMatchingException(new QuestionNotMatchingException("msg"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.QUESTION_NOT_MATCHING, response.getBody());
    }

    @Test
    void handleAnswerNotMatchingExceptionTest() {
        ResponseEntity<Object> response = handler.handleAnswerNotMatchingException(new AnswerNotMatchingException("msg"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.ANSWER_NOT_MATCHING, response.getBody());
    }

    @Test
    void handleAnswerSubmissionNotMatchingExceptionTest() {
        ResponseEntity<Object> response = handler.handleAnswerSubmissionNotMatchingException(new AnswerSubmissionNotMatchingException("msg"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.ANSWER_SUBMISSION_NOT_MATCHING, response.getBody());
    }

    @Test
    void handleGroupNotMatchingExceptionTest() {
        ResponseEntity<Object> response = handler.handleGroupNotMatchingException(new GroupNotMatchingException("msg"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.GROUP_NOT_MATCHING, response.getBody());
    }

    @Test
    void handleSubmissionNotMatchingExceptionTest() {
        ResponseEntity<Object> response = handler.handleSubmissionNotMatchingException(new SubmissionNotMatchingException("submission msg"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("submission msg", response.getBody());
    }

    @Test
    void handleQuestionSubmissionNotMatchingExceptionTest() {
        ResponseEntity<Object> response = handler.handleQuestionSubmissionNotMatchingException(new QuestionSubmissionNotMatchingException("msg"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.QUESTION_SUBMISSION_NOT_MATCHING, response.getBody());
    }

    @Test
    void handleSubmissionCommentNotMatchingExceptionTest() {
        ResponseEntity<Object> response = handler.handleSubmissionCommentNotMatchingException(new SubmissionCommentNotMatchingException("msg"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.SUBMISSION_COMMENT_NOT_MATCHING, response.getBody());
    }

    @Test
    void handleQuestionSubmissionCommentNotMatchingExceptionTest() {
        ResponseEntity<Object> response = handler.handleQuestionSubmissionCommentNotMatchingException(new QuestionSubmissionCommentNotMatchingException("msg"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.QUESTION_SUBMISSION_COMMENT_NOT_MATCHING, response.getBody());
    }

    @Test
    void handleOutcomeNotMatchingExceptionTest() {
        ResponseEntity<Object> response = handler.handleOutcomeNotMatchingException(new OutcomeNotMatchingException("msg"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.OUTCOME_NOT_MATCHING, response.getBody());
    }

    @Test
    void handleOutcomeScoreNotMatchingExceptionTest() {
        ResponseEntity<Object> response = handler.handleOutcomeScoreNotMatchingException(new OutcomeScoreNotMatchingException("msg"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.OUTCOME_SCORE_NOT_MATCHING, response.getBody());
    }

    @Test
    void handleIdMissingExceptionTest() {
        ResponseEntity<Object> response = handler.handleIdMissingException(new IdMissingException("msg"), webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(TextConstants.ID_MISSING, response.getBody());
    }

    @Test
    void handleBadConsentFileTypeExceptionTest() {
        ResponseEntity<Object> response = handler.handleBadConsentFileTypeException(new BadConsentFileTypeException("bad consent file"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("bad consent file", response.getBody());
    }

    @Test
    void handleExpiredJwtExceptionTest() {
        ResponseEntity<Object> response = handler.handleExpiredJwtException(new ExpiredJwtException(null, null, "expired jwt"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("expired jwt", response.getBody());
    }

    @Test
    void handleParticipantNotUpdatedExceptionTest() {
        ResponseEntity<Object> response = handler.handleParticipantNotUpdatedException(new ParticipantNotUpdatedException("participant not updated"), webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("participant not updated", response.getBody());
    }

    @Test
    void handleDataServiceExceptionTest() {
        ResponseEntity<Object> response = handler.handleDataServiceException(new DataServiceException("data service error"), webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("data service error", response.getBody());
    }

    @Test
    void handleWrongValueExceptionTest() {
        ResponseEntity<Object> response = handler.handleWrongValueException(new WrongValueException("wrong value"), webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("wrong value", response.getBody());
    }

    @Test
    void handleAssignmentDatesExceptionTest() {
        ResponseEntity<Object> response = handler.handleAssignmentDatesException(new AssignmentDatesException("bad dates"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("bad dates", response.getBody());
    }

    @Test
    void handleExperimentLockedExceptionTest() {
        ResponseEntity<Object> response = handler.handleExperimentLockedException(new ExperimentLockedException("locked"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("locked", response.getBody());
    }

    @Test
    void handleTitleValidationExceptionError100Test() {
        ResponseEntity<Object> response = handler.handleTitleValidationException(new TitleValidationException("Error 100: title issue"), webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Error 100: title issue", response.getBody());
    }

    @Test
    void handleTitleValidationExceptionError102Test() {
        ResponseEntity<Object> response = handler.handleTitleValidationException(new TitleValidationException("Error 102: title issue"), webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Error 102: title issue", response.getBody());
    }

    @Test
    void handleTitleValidationExceptionOtherTest() {
        ResponseEntity<Object> response = handler.handleTitleValidationException(new TitleValidationException("Error 199: title issue"), webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error 199: title issue", response.getBody());
    }

    @Test
    void handleConditionsLockedExceptionTest() {
        ResponseEntity<Object> response = handler.handleConditionsLockedException(new ConditionsLockedException("conditions locked"), webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("conditions locked", response.getBody());
    }

    @Test
    void handleMultipleChoiceLimitReachedExceptionTest() {
        ResponseEntity<Object> response = handler.handleMultipleChoiceLimitReachedException(new MultipleChoiceLimitReachedException("limit reached"), webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("limit reached", response.getBody());
    }

    @Test
    void handleInvalidUserExceptionTest() {
        ResponseEntity<Object> response = handler.handleInvalidUserException(new InvalidUserException("invalid user"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("invalid user", response.getBody());
    }

    @Test
    void handleInvalidParticipantExceptionError105Test() {
        ResponseEntity<Object> response = handler.handleInvalidParticipantException(new InvalidParticipantException("Error 105: invalid participant"), webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error 105: invalid participant", response.getBody());
    }

    @Test
    void handleInvalidParticipantExceptionOtherTest() {
        ResponseEntity<Object> response = handler.handleInvalidParticipantException(new InvalidParticipantException("Error 199: invalid participant"), webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Error 199: invalid participant", response.getBody());
    }

    @Test
    void handleInvalidQuestionTypeExceptionTest() {
        ResponseEntity<Object> response = handler.handleInvalidQuestionTypeException(new InvalidQuestionTypeException("invalid question type"), webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("invalid question type", response.getBody());
    }

    @Test
    void handleDuplicateQuestionExceptionTest() {
        ResponseEntity<Object> response = handler.handleDuplicateQuestionException(new DuplicateQuestionException("duplicate question"), webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("duplicate question", response.getBody());
    }

    @Test
    void handleNoSubmissionsExceptionSubmissionPrefixTest() {
        ResponseEntity<Object> response = handler.handleNoSubmissionsException(new NoSubmissionsException("A submission already exists"), webRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("A submission already exists", response.getBody());
    }

    @Test
    void handleNoSubmissionsExceptionOtherTest() {
        ResponseEntity<Object> response = handler.handleNoSubmissionsException(new NoSubmissionsException("No submissions found"), webRequest);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals("No submissions found", response.getBody());
    }

    @Test
    void handleAssignmentNotCreatedExceptionTest() {
        ResponseEntity<Object> response = handler.handleAssignmentNotCreatedException(new AssignmentNotCreatedException("not created"), webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("not created", response.getBody());
    }

    @Test
    void handleAssignmentNotEditedExceptionTest() {
        ResponseEntity<Object> response = handler.handleAssignmentNotEditedException(new AssignmentNotEditedException("not edited"), webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("not edited", response.getBody());
    }

    @Test
    void handleIdInPostExceptionTest() {
        ResponseEntity<Object> response = handler.handleIdInPostException(new IdInPostException("id in post"), webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("id in post", response.getBody());
    }

    @Test
    void handleExceedingLimitExceptionTest() {
        ResponseEntity<Object> response = handler.handleExceedingLimitException(new ExceedingLimitException("exceeding limit"), webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("exceeding limit", response.getBody());
    }

    @Test
    void handleNegativePointsExceptionTest() {
        ResponseEntity<Object> response = handler.handleNegativePointsException(new NegativePointsException("negative points"), webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("negative points", response.getBody());
    }

    @Test
    void handleTypeNotSupportedExceptionTest() {
        ResponseEntity<Object> response = handler.handleTypeNotSupportedException(new TypeNotSupportedException("type not supported"), webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("type not supported", response.getBody());
    }

    @Test
    void handleExperimentConditionReachedExceptionTest() {
        ResponseEntity<Object> response = handler.handleExperimentConditionReachedException(new ExperimentConditionLimitReachedException("condition limit reached"), webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("condition limit reached", response.getBody());
    }

    @Test
    void handleRevealResponsesSettingValidationExceptionTest() {
        ResponseEntity<Object> response = handler.handleRevealResponsesSettingValidationException(new RevealResponsesSettingValidationException("reveal responses setting invalid"), webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("reveal responses setting invalid", response.getBody());
    }

    @Test
    void handleMultipleAttemptsSettingsValidationExceptionTest() {
        ResponseEntity<Object> response = handler.handleMultipleAttemptsSettingsValidationException(new MultipleAttemptsSettingsValidationException("multiple attempts setting invalid"), webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("multiple attempts setting invalid", response.getBody());
    }

    @Test
    void handleAssignmentAttemptExceptionTest() {
        ResponseEntity<Object> response = handler.handleAssignmentAttemptException(new AssignmentAttemptException("assignment attempt error"), webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("assignment attempt error", response.getBody());
    }

    @Test
    void handleApiExceptionTest() {
        ResponseEntity<Object> response = handler.handleApiException(new ApiException("api error"), webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("api error", response.getBody());
    }

}
