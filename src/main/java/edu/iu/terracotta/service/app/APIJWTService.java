package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.*;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.utils.lti.LTI3Request;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwt;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public interface APIJWTService {
    //Here we could add other checks like expiration of the state (not implemented)
    Jws<Claims> validateToken(String token);

    Jwt<Header, Claims> unsecureToken(String token);

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
                    String nonce) throws GeneralSecurityException, IOException;

    String buildJwt(boolean oneUse, LTI3Request lti3Request) throws GeneralSecurityException, IOException;

    String refreshToken(String token) throws GeneralSecurityException, IOException, BadTokenException;

    String extractJwtStringValue(HttpServletRequest request, boolean allowQueryParam);

    SecuredInfo extractValues(HttpServletRequest request, boolean allowQueryParam);

    boolean isAdmin(SecuredInfo securedInfo);

    boolean isInstructor(SecuredInfo securedInfo);

    boolean isInstructorOrHigher(SecuredInfo securedInfo);

    boolean isLearner(SecuredInfo securedInfo);

    boolean isLearnerOrHigher(SecuredInfo securedInfo);

    boolean isGeneral(SecuredInfo securedInfo);

    void experimentAllowed(SecuredInfo securedInfo, Long experimentId) throws BadTokenException, ExperimentNotMatchingException;

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

    void answerSubmissionAllowed(SecuredInfo securedInfo, Long QuestionSubmissionId, String answerType, Long answerSubmissionId) throws AnswerSubmissionNotMatchingException;

    boolean validateFileToken(String token, String fileId);

    String buildFileToken(String fileId) throws GeneralSecurityException;
}
