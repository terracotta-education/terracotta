package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.SubmissionCommentNotMatchingException;
import edu.iu.terracotta.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import edu.iu.terracotta.utils.lti.LTI3Request;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public interface APIJWTService {
    //Here we could add other checks like expiration of the state (not implemented)
    Jws<Claims> validateToken(String token);

    String buildJwt(boolean oneUse, List<String> roles, Long contextId, Long platformDeploymentId, String userId, Long assignmentId, Long experimentId, Boolean consent) throws GeneralSecurityException, IOException;

    String buildJwt(boolean oneUse, LTI3Request lti3Request) throws GeneralSecurityException, IOException;

    String refreshToken(String token) throws GeneralSecurityException, IOException, BadTokenException;

    String extractJwtStringValue(HttpServletRequest request, boolean allowQueryParam);

    SecurityInfo extractValues(HttpServletRequest request, boolean allowQueryParam);

    boolean isAdmin(SecurityInfo securityInfo);

    boolean isInstructor(SecurityInfo securityInfo);

    boolean isInstructorOrHigher(SecurityInfo securityInfo);

    boolean isLearner(SecurityInfo securityInfo);

    boolean isLearnerOrHigher(SecurityInfo securityInfo);

    boolean isGeneral(SecurityInfo securityInfo);

    void experimentAllowed(SecurityInfo securityInfo, Long experimentId) throws BadTokenException, ExperimentNotMatchingException;

    void conditionAllowed(SecurityInfo securityInfo, Long experimentId, Long conditionId) throws ConditionNotMatchingException;

    void participantAllowed(SecurityInfo securityInfo, Long experimentId, Long participantId) throws ParticipantNotMatchingException;

    void exposureAllowed(SecurityInfo securityInfo, Long experimentId, Long exposureId) throws ExposureNotMatchingException;

    void groupAllowed(SecurityInfo securityInfo, Long experimentId, Long groupId) throws GroupNotMatchingException;

    void assignmentAllowed(SecurityInfo securityInfo, Long experimentId, Long assignmentId) throws AssignmentNotMatchingException;

    void assignmentAllowed(SecurityInfo securityInfo, Long experimentId, Long exposureId, Long assignmentId) throws AssignmentNotMatchingException;

    void treatmentAllowed(SecurityInfo securityInfo, Long experimentId, Long conditionId, Long treatmentId) throws TreatmentNotMatchingException;

    void assessmentAllowed(SecurityInfo securityInfo, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId) throws AssessmentNotMatchingException;

    void questionAllowed(SecurityInfo securityInfo, Long assessmentId, Long questionId) throws QuestionNotMatchingException;

    void answerAllowed(SecurityInfo securityInfo, Long assessmentId, Long questionId, Long answerId) throws AnswerNotMatchingException;

    void submissionAllowed(SecurityInfo securityInfo, Long assessmendId, Long submissionId) throws SubmissionNotMatchingException;

    void questionSubmissionAllowed(SecurityInfo securityInfo, Long assessmentId, Long submissionId, Long questionSubmissionId) throws QuestionSubmissionNotMatchingException;

    void submissionCommentAllowed(SecurityInfo securityInfo, Long assessmentId, Long submissionId, Long submissionCommentId) throws SubmissionCommentNotMatchingException;
}
