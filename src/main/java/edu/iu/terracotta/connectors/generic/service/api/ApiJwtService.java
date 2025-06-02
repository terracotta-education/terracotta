package edu.iu.terracotta.connectors.generic.service.api;

import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImport;
import edu.iu.terracotta.dao.entity.messaging.conditional.MessageConditionalText;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainerConfiguration;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.entity.messaging.message.MessageConfiguration;
import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRule;
import edu.iu.terracotta.dao.entity.messaging.recipient.MessageRecipientRuleSet;
import edu.iu.terracotta.dao.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AnswerSubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentImportNotFoundException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.dao.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.dao.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.dao.exceptions.OutcomeScoreNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionSubmissionCommentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.SubmissionCommentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationOwnerNotMatchingException;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.ConditionsLockedException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.messaging.MessageConditionalTextNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageConfigurationNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerConfigurationNotFoundException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerNotFoundException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerOwnerNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageContentNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageNotFoundException;
import edu.iu.terracotta.exceptions.messaging.MessageNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageOwnerNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageRuleNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageRuleSetNotMatchingException;
import edu.iu.terracotta.utils.lti.Lti3Request;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;

@TerracottaConnector(LmsConnector.GENERIC)
public interface ApiJwtService {

    String ISSUER_TERRACOTTA_API = "TERRACOTTA";

    //Here we could add other checks like expiration of the state (not implemented)
    Jws<Claims> validateToken(String token);

    String buildJwt(long platformDeploymentId, String userKey, Claims claims) throws GeneralSecurityException, IOException, TerracottaConnectorException;

    String buildJwt(boolean oneUse,
            List<String> roles,
            Long contextId,
            Long platformDeploymentId,
            String userId,
            Long assignmentId,
            Long experimentId,
            Boolean consent,
            String lmsUserId,
            String lmsUserGlobalId,
            String lmsLoginId,
            String lmsUserName,
            String lmsCourseId,
            String lmsAssignmentId,
            String dueAt,
            String lockAt,
            String unlockAt,
            String nonce,
            Integer allowedAttempts,
            Integer studentAttempts) throws GeneralSecurityException, IOException, TerracottaConnectorException;

    String buildJwt(boolean oneUse, Lti3Request lti3Request) throws GeneralSecurityException, IOException, TerracottaConnectorException;

    /**
     * Create a JWT that can be used as the state for a request for an API token
     * to access the API of an LMS (for example, the LMS API). After the API
     * token is successfully obtained, the values in this JWT will be used to
     * construct the one time token that is handed off to the Terracotta
     * frontend app.
     *
     * @param lti3Request
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws TerracottaConnectorException
     */
    String generateStateForAPITokenRequest(Lti3Request lti3Request) throws GeneralSecurityException, IOException, TerracottaConnectorException;

    ResponseEntity<String> getTimedToken(HttpServletRequest httpServletRequest) throws NumberFormatException, TerracottaConnectorException;
    ResponseEntity<String> getTimedToken(String token) throws NumberFormatException, TerracottaConnectorException;
    Map<String, Object> unsecureToken(String token, PlatformDeployment platformDeployment) throws TerracottaConnectorException;
    Optional<Jws<Claims>> validateStateForAPITokenRequest(String state);
    String refreshToken(String token) throws GeneralSecurityException, IOException, BadTokenException, NumberFormatException, TerracottaConnectorException;
    String extractJwtStringValue(HttpServletRequest request, boolean allowQueryParam);
    SecuredInfo extractValues(HttpServletRequest request, boolean allowQueryParam) throws NumberFormatException, TerracottaConnectorException;
    SecuredInfo extractValues(String token) throws NumberFormatException, TerracottaConnectorException;
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
    Exposure exposureAllowed(SecuredInfo securedInfo, Long experimentId, Long exposureId) throws ExposureNotMatchingException;
    void groupAllowed(SecuredInfo securedInfo, Long experimentId, Long groupId) throws GroupNotMatchingException;
    Assignment assignmentAllowed(SecuredInfo securedInfo, long experimentId, long assignmentId) throws AssignmentNotMatchingException;
    Assignment assignmentAllowed(SecuredInfo securedInfo, long experimentId, long exposureId, long assignmentId) throws AssignmentNotMatchingException;
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
    ExperimentImport experimentImportAllowed(SecuredInfo securedInfo, UUID uuid) throws ExperimentImportNotFoundException;
    MessageContainer messagingContainerAllowed(SecuredInfo securedInfo, long exposureId, UUID containerUuid) throws MessageContainerOwnerNotMatchingException, MessageContainerNotMatchingException, MessageContainerNotFoundException;
    MessageContainerConfiguration messagingContainerConfigurationAllowed(SecuredInfo securedInfo, UUID containerUuid, UUID configurationId) throws MessageContainerOwnerNotMatchingException, MessageContainerNotMatchingException, MessageContainerNotFoundException, MessageContainerConfigurationNotFoundException;
    Message messagingAllowed(SecuredInfo securedInfo, UUID containerUuid, UUID messageUuid) throws MessageOwnerNotMatchingException, MessageNotMatchingException, MessageNotFoundException;
    MessageContent messagingContentAllowed(SecuredInfo securedInfo, UUID messageUuid, UUID contentUuid) throws MessageContentNotMatchingException;
    MessageConfiguration messagingConfigurationAllowed(SecuredInfo securedInfo, UUID messageUuid, UUID configurationUuid) throws MessageConfigurationNotMatchingException;
    MessageRecipientRuleSet messagingRuleSetAllowed(SecuredInfo securedInfo, UUID messageUuid, UUID messageRuleSetUuid) throws MessageRuleSetNotMatchingException;
    MessageRecipientRule messagingRuleAllowed(SecuredInfo securedInfo, UUID messageUuid, UUID messageRuleSetUuid) throws MessageRuleNotMatchingException;
    MessageConditionalText messagingConditionalTextAllowed(SecuredInfo securedInfo, UUID contentUuid, UUID conditionalTextUuid) throws MessageContentNotMatchingException, MessageNotMatchingException, MessageConditionalTextNotMatchingException;

}
