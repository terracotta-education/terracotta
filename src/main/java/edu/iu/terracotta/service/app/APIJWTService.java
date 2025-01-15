package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.exceptions.AnswerSubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.exceptions.ConditionsLockedException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.exceptions.OutcomeScoreNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.QuestionSubmissionCommentNotMatchingException;
import edu.iu.terracotta.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.SubmissionCommentNotMatchingException;
import edu.iu.terracotta.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.exceptions.integrations.IntegrationOwnerNotMatchingException;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.utils.lti.LTI3Request;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface APIJWTService {

    String ISSUER_TERRACOTTA_API = "TERRACOTTA";

    //Here we could add other checks like expiration of the state (not implemented)
    Jws<Claims> validateToken(String token);

    String buildJwt(boolean oneUse,
            List<String> roles,
            Long contextId,
            Long platformDeploymentId,
            String userId,
            Long assignmentId,
            Long experimentId,
            Boolean consent,
            String canvasUserId,
            String canvasUserGlobalId,
            String canvasLoginId,
            String canvasUserName,
            String canvasCourseId,
            String canvasAssignmentId,
            String dueAt,
            String lockAt,
            String unlockAt,
            String nonce,
            Integer allowedAttempts,
            Integer studentAttempts) throws GeneralSecurityException, IOException;

    String buildJwt(boolean oneUse, LTI3Request lti3Request) throws GeneralSecurityException, IOException;

    /**
     * Create a JWT that can be used as the state for a request for an API token
     * to access the API of an LMS (for example, the Canvas API). After the API
     * token is successfully obtained, the values in this JWT will be used to
     * construct the one time token that is handed off to the Terracotta
     * frontend app.
     *
     * @param lti3Request
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     */
    String generateStateForAPITokenRequest(LTI3Request lti3Request) throws GeneralSecurityException, IOException;

    Map<String, Object> unsecureToken(String token);
    Optional<Jws<Claims>> validateStateForAPITokenRequest(String state);
    String refreshToken(String token) throws GeneralSecurityException, IOException, BadTokenException;
    String extractJwtStringValue(HttpServletRequest request, boolean allowQueryParam);
    SecuredInfo extractValues(HttpServletRequest request, boolean allowQueryParam);
    boolean isAdmin(SecuredInfo securedInfo);
    boolean isTerracottaAdmin(SecuredInfo securedInfo);
    boolean isInstructor(SecuredInfo securedInfo);
    boolean isInstructorOrHigher(SecuredInfo securedInfo);
    boolean isLearner(SecuredInfo securedInfo);
    boolean isLearnerOrHigher(SecuredInfo securedInfo);
    boolean isGeneral(SecuredInfo securedInfo);
    boolean isTestStudent(SecuredInfo securedInfo);
    Experiment experimentAllowed(SecuredInfo securedInfo, Long experimentId) throws BadTokenException, ExperimentNotMatchingException;
    boolean experimentLocked(Long experimentId, boolean throwException) throws ExperimentLockedException, ExperimentNotMatchingException;
    boolean conditionsLocked(Long experimentId, boolean throwException) throws ConditionsLockedException, ExperimentNotMatchingException;
    void conditionAllowed(SecuredInfo securedInfo, Long experimentId, Long conditionId) throws ConditionNotMatchingException;
    void participantAllowed(SecuredInfo securedInfo, Long experimentId, Long participantId) throws ParticipantNotMatchingException;
    void exposureAllowed(SecuredInfo securedInfo, Long experimentId, Long exposureId) throws ExposureNotMatchingException;
    void groupAllowed(SecuredInfo securedInfo, Long experimentId, Long groupId) throws GroupNotMatchingException;
    void assignmentAllowed(SecuredInfo securedInfo, Long experimentId, Long assignmentId) throws AssignmentNotMatchingException;
    void assignmentAllowed(SecuredInfo securedInfo, Long experimentId, Long exposureId, Long assignmentId) throws AssignmentNotMatchingException;
    void treatmentAllowed(SecuredInfo securedInfo, Long experimentId, Long conditionId, Long treatmentId) throws TreatmentNotMatchingException;
    void assessmentAllowed(SecuredInfo securedInfo, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId) throws AssessmentNotMatchingException;
    void questionAllowed(SecuredInfo securedInfo, Long assessmentId, Long questionId) throws QuestionNotMatchingException;
    void answerAllowed(SecuredInfo securedInfo, Long assessmentId, Long questionId, String answerType, Long answerId) throws AnswerNotMatchingException;
    void submissionAllowed(SecuredInfo securedInfo, Long assessmentId, Long submissionId) throws SubmissionNotMatchingException;
    void questionSubmissionAllowed(SecuredInfo securedInfo, Long assessmentId, Long submissionId, Long questionSubmissionId) throws QuestionSubmissionNotMatchingException;
    void submissionCommentAllowed(SecuredInfo securedInfo, Long assessmentId, Long submissionId, Long submissionCommentId) throws SubmissionCommentNotMatchingException;
    void questionSubmissionCommentAllowed(SecuredInfo securedInfo, Long questionSubmissionId, Long questionSubmissionCommentId) throws QuestionSubmissionCommentNotMatchingException;
    void outcomeAllowed(SecuredInfo securedInfo, Long experimentId, Long exposureId, Long outcomeId) throws OutcomeNotMatchingException;
    void outcomeScoreAllowed(SecuredInfo securedInfo, Long outcomeId, Long outcomeScoreId) throws OutcomeScoreNotMatchingException;
    void answerSubmissionAllowed(SecuredInfo securedInfo, Long questionSubmissionId, String answerType, Long answerSubmissionId) throws AnswerSubmissionNotMatchingException;
    boolean validateFileToken(String token, String fileId);
    String buildFileToken(String fileId, String localUrl) throws GeneralSecurityException;
    void integrationAllowed(long questionId, UUID integrationUuid) throws IntegrationOwnerNotMatchingException;

}
