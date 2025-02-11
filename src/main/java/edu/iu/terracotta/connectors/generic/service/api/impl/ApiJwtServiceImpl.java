package edu.iu.terracotta.connectors.generic.service.api.impl;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lti.Roles;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.connectors.generic.service.connector.ConnectorService;
import edu.iu.terracotta.connectors.generic.service.lti.LtiDataService;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AnswerSubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ConditionNotMatchingException;
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
import edu.iu.terracotta.dao.model.enums.ExposureTypes;
import edu.iu.terracotta.dao.repository.AnswerEssaySubmissionRepository;
import edu.iu.terracotta.dao.repository.AnswerMcRepository;
import edu.iu.terracotta.dao.repository.AnswerMcSubmissionRepository;
import edu.iu.terracotta.dao.repository.AssessmentRepository;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ConditionRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.ExposureRepository;
import edu.iu.terracotta.dao.repository.GroupRepository;
import edu.iu.terracotta.dao.repository.OutcomeRepository;
import edu.iu.terracotta.dao.repository.OutcomeScoreRepository;
import edu.iu.terracotta.dao.repository.ParticipantRepository;
import edu.iu.terracotta.dao.repository.QuestionRepository;
import edu.iu.terracotta.dao.repository.QuestionSubmissionCommentRepository;
import edu.iu.terracotta.dao.repository.QuestionSubmissionRepository;
import edu.iu.terracotta.dao.repository.SubmissionCommentRepository;
import edu.iu.terracotta.dao.repository.SubmissionRepository;
import edu.iu.terracotta.dao.repository.TreatmentRepository;
import edu.iu.terracotta.dao.repository.integrations.IntegrationRepository;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.ConditionsLockedException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.service.app.AdminService;
import edu.iu.terracotta.utils.lti.Lti3Request;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import io.jsonwebtoken.Locator;
import lombok.extern.slf4j.Slf4j;
import edu.iu.terracotta.utils.oauth.OAuthUtils;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * This manages all the data processing for the LTIRequest (and for LTI in general)
 * Necessary to get appropriate TX handling and service management
 */
@Slf4j
@Primary
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ApiJwtServiceImpl implements ApiJwtService {

    private static final String ISSUER_LMS_OAUTH_API_TOKEN_REQUEST = "lmsOAuthAPITokenRequest";
    private static final String JWT_REQUEST_HEADER_NAME = "Authorization";
    private static final String JWT_BEARER_TYPE = "Bearer";
    private static final String QUERY_PARAM_NAME = "token";

    @Autowired private AnswerEssaySubmissionRepository answerEssaySubmissionRepository;
    @Autowired private AnswerMcRepository answerMcRepository;
    @Autowired private AnswerMcSubmissionRepository answerMcSubmissionRepository;
    @Autowired private AssessmentRepository assessmentRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private ConditionRepository conditionRepository;
    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private ExposureRepository exposureRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private IntegrationRepository integrationRepository;
    @Autowired private OutcomeRepository outcomeRepository;
    @Autowired private OutcomeScoreRepository outcomeScoreRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private QuestionSubmissionCommentRepository questionSubmissionCommentRepository;
    @Autowired private QuestionSubmissionRepository questionSubmissionRepository;
    @Autowired private SubmissionCommentRepository submissionCommentRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private TreatmentRepository treatmentRepository;
    @Autowired private AdminService adminService;
    @Autowired private ConnectorService<ApiJwtService> connectorService;
    @Autowired private LtiDataService ltiDataService;

    @Value("${app.token.logging.enabled:true}")
    private boolean tokenLoggingEnabled;

    private ApiJwtService instance(Lti3Request lti3Request) throws TerracottaConnectorException {
        return instance(lti3Request.getToolDeployment().getPlatformDeployment());
    }

    private ApiJwtService instance(long platformDeploymentId) throws TerracottaConnectorException {
        return connectorService.instance(platformDeploymentId, ApiJwtService.class);
    }

    private ApiJwtService instance(Jws<Claims> claims) throws TerracottaConnectorException {
        return instance(Long.parseLong(String.valueOf(claims.getPayload().get("platformDeploymentId"))));
    }

    private ApiJwtService instance(PlatformDeployment platformDeployment) throws TerracottaConnectorException {
        return connectorService.instance(platformDeployment, ApiJwtService.class);
    }

    /**
     * This will check that the state has been signed by us and retrieve the issuer private key.
     * We could add here other checks if we want (like the expiration of the state, nonce used only once, etc...)
     */
    //Here we could add other checks like expiration of the state (not implemented)
    @Override
    public Jws<Claims> validateToken(String token) {
        // This is done because each state is signed with a different key based on the issuer... so
        // we don't know the key and we need to check it pre-extracting the claims and finding the kid
        try {
            return Jwts.parser().keyLocator(
                new Locator<Key>() {
                    @Override
                    public Key locate(Header header) {
                        if (header instanceof JwsHeader) {
                            PublicKey toolPublicKey;

                            try {
                                // We are dealing with RS256 encryption, so we have some Oauth utils to manage the keys and
                                // convert them to keys from the string stored in DB. There are for sure other ways to manage this.
                                toolPublicKey = OAuthUtils.loadPublicKey(ltiDataService.getOwnPublicKey());
                            } catch (GeneralSecurityException ex) {
                                log.error("Error validating the state. Error generating the tool public key", ex);
                                return null;
                            }
                            return toolPublicKey;
                        }

                        return null;
                    }
                }
            )
            .build()
            .parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired. {}", e.getMessage());
            return null;
        }
        // If we are on this point, then the state signature has been validated. We can start other tasks now.
    }

    @Override
    public boolean validateFileToken(String token, String fileId) {
        Jws<Claims> claims = validateToken(token);

        if (claims != null) {
            if (claims.getPayload().get("fileId").toString().equals(fileId)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Map<String, Object> unsecureToken(String token, PlatformDeployment platformDeployment) throws TerracottaConnectorException {
        return instance(platformDeployment).unsecureToken(token, platformDeployment);
    }

    @Override
    public String buildJwt(long platformDeploymentId, String userKey, Claims claims) throws GeneralSecurityException, IOException, TerracottaConnectorException {
        return instance(platformDeploymentId).buildJwt(platformDeploymentId, userKey, claims);
    }

    @Override
    public String buildJwt(boolean oneUse,
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
        Integer studentAttempts
    ) throws GeneralSecurityException, IOException, TerracottaConnectorException {
        return instance(platformDeploymentId)
            .buildJwt(
                oneUse,
                roles,
                contextId,
                platformDeploymentId,
                userId,
                assignmentId,
                experimentId,
                consent,
                lmsUserId,
                lmsUserGlobalId,
                lmsLoginId,
                lmsUserName,
                lmsCourseId,
                lmsAssignmentId,
                dueAt,
                lockAt,
                unlockAt,
                nonce,
                allowedAttempts,
                studentAttempts
            );
    }

    /**
     * This JWT will contain the token request
          * @throws TerracottaConnectorException
          */
         @Override
         public String buildJwt(boolean oneUse, Lti3Request lti3Request) throws GeneralSecurityException, IOException, TerracottaConnectorException {
        return instance(lti3Request).buildJwt(oneUse, lti3Request);
    }

    @Override
    public String generateStateForAPITokenRequest(Lti3Request lti3Request) throws GeneralSecurityException, IOException, TerracottaConnectorException {
        return instance(lti3Request).generateStateForAPITokenRequest(lti3Request);
    }

    @Override
    public Optional<Jws<Claims>> validateStateForAPITokenRequest(String state) {
        Jws<Claims> claims = validateToken(state);

        if (claims != null) {
            String issuer = claims.getPayload().getIssuer();

            if (!ISSUER_LMS_OAUTH_API_TOKEN_REQUEST.equals(issuer)) {
                return Optional.empty();
            }

            return Optional.of(claims);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String buildFileToken(String fileId, String localUrl) throws GeneralSecurityException {
        Date date = new Date();
        PrivateKey toolPrivateKey = OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey());
        JwtBuilder builder = Jwts.builder()
            .header()
            .add(LtiStrings.KID, TextConstants.DEFAULT_KID)
            .add(LtiStrings.TYP, LtiStrings.JWT)
            .and()
            .issuer(ISSUER_TERRACOTTA_API)
            .subject("no_user") // The clientId
            .audience()
            .add(localUrl)  //We send here the authToken url.
            .and()
            .expiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
            .notBefore(date) //a java.util.Date
            .issuedAt(date) // for example, now
            .claim("fileId", fileId)  //This is an specific claim to ask for tokens.
            .signWith(toolPrivateKey, SIG.RS256);  //We sign it with our own private key. The platform has the public one.
        String token = builder.compact();

        if (tokenLoggingEnabled) {
            log.debug("Token Request: \n {} \n", token);
        }

        return token;
    }

    @Override
    public String refreshToken(String token) throws GeneralSecurityException, IOException, BadTokenException, NumberFormatException, TerracottaConnectorException {
        return instance(validateToken(token)).refreshToken(token);
    }

    @Override
    public ResponseEntity<String> getTimedToken(HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        return instance(
            validateToken(
                extractJwtStringValue(
                    req,
                    true
                )
            )
        )
        .getTimedToken(req);
    }

    @Override
    public String extractJwtStringValue(HttpServletRequest request, boolean allowQueryParam) {
        String rawHeaderValue = StringUtils.trimToNull(request.getHeader(JWT_REQUEST_HEADER_NAME));

        if (rawHeaderValue == null) {
            if (allowQueryParam) {
                return StringUtils.trimToNull(request.getParameter(QUERY_PARAM_NAME));
            }
        }

        if (rawHeaderValue == null) {
            return null;
        }

        // very similar to BearerTokenExtractor.java in Spring spring-security-oauth2
        if (isBearerToken(rawHeaderValue)) {
            return rawHeaderValue.substring(JWT_BEARER_TYPE.length()).trim();
        }

        return null;
    }

    @Override
    public SecuredInfo extractValues(HttpServletRequest request, boolean allowQueryParam) throws NumberFormatException, TerracottaConnectorException {
        String token = extractJwtStringValue(request, allowQueryParam);
        Jws<Claims> claims = validateToken(token);

        if (claims == null) {
            return null;
        }

        return instance(claims).extractValues(request, allowQueryParam);
    }

    @Override
    public boolean isAdmin(SecuredInfo securedInfo) {
        return securedInfo.getRoles().contains(Roles.ADMIN);
    }

    @Override
    public boolean isTerracottaAdmin(SecuredInfo securedInfo) {
        return adminService.isTerracottaAdmin(securedInfo.getUserId());
    }

    @Override
    public boolean isInstructor(SecuredInfo securedInfo) {
        return securedInfo.getRoles().contains(Roles.MEMBERSHIP_INSTRUCTOR);
    }

    @Override
    public boolean isLearner(SecuredInfo securedInfo) {
        return securedInfo.getRoles().contains(Roles.MEMBERSHIP_LEARNER);
    }

    @Override
    public boolean isGeneral(SecuredInfo securedInfo) {
        return securedInfo.getRoles().contains(Roles.GENERAL);
    }

    @Override
    public boolean isTestStudent(SecuredInfo securedInfo) {
        return securedInfo.getRoles().contains(Roles.TEST_STUDENT);
    }

    @Override
    public boolean isInstructorOrHigher(SecuredInfo securedInfo) {
        return isInstructor(securedInfo) || isAdmin(securedInfo);
    }

    @Override
    public boolean isLearnerOrHigher(SecuredInfo securedInfo) {
        return isLearner(securedInfo) || isInstructorOrHigher(securedInfo);
    }

    private boolean isBearerToken(String rawHeaderValue) {
        return rawHeaderValue.toLowerCase(Locale.US).startsWith(JWT_BEARER_TYPE.toLowerCase(Locale.US));
    }

    @Override
    public Experiment experimentAllowed(SecuredInfo securedInfo, Long experimentId) throws BadTokenException, ExperimentNotMatchingException {
        if (securedInfo == null) {
            log.error(TextConstants.BAD_TOKEN);
            throw new BadTokenException(TextConstants.BAD_TOKEN);
        }

        return experimentRepository.findByExperimentIdAndPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(experimentId, securedInfo.getPlatformDeploymentId(), securedInfo.getContextId())
            .orElseThrow(() -> new ExperimentNotMatchingException(TextConstants.EXPERIMENT_NOT_MATCHING));
    }

    @Override
    public boolean experimentLocked(Long experimentId, boolean throwException) throws ExperimentLockedException, ExperimentNotMatchingException {
        Optional<Experiment> experiment = experimentRepository.findById(experimentId);

        if (experiment.isEmpty()) {
            throw new ExperimentNotMatchingException("The experiment with id " + experimentId + " does not exist");
        }

        if (!experiment.get().isStarted()) {
            return false;
        }

        if (throwException) {
            throw new ExperimentLockedException("Error 110: The experiment has started and can't be modified");
        }

        return true;
    }

    @Override
    public boolean conditionsLocked(Long experimentId, boolean throwException) throws ConditionsLockedException, ExperimentNotMatchingException {
        Optional<Experiment> experiment = experimentRepository.findById(experimentId);

        if (experiment.isEmpty()) {
            throw new ExperimentNotMatchingException("The experiment with id " + experimentId + " does not exist");
        }

        if (experiment.get().getExposureType().equals(ExposureTypes.NOSET)) {
            return false;
        }

        if (throwException) {
            throw new ConditionsLockedException("Error 111: The conditions can't be modified at this point");
        }

        return true;
    }

    @Override
    public void conditionAllowed(SecuredInfo securedInfo, Long experimentId, Long conditionId) throws ConditionNotMatchingException {
        if (!conditionRepository.existsByExperiment_ExperimentIdAndConditionId(experimentId, conditionId)) {
            throw new ConditionNotMatchingException(TextConstants.CONDITION_NOT_MATCHING);
        }
    }

    @Override
    public void participantAllowed(SecuredInfo securedInfo, Long experimentId, Long participantId) throws ParticipantNotMatchingException {
        if (!participantRepository.existsByExperiment_ExperimentIdAndParticipantId(experimentId, participantId)) {
            throw new ParticipantNotMatchingException(TextConstants.PARTICIPANT_NOT_MATCHING);
        }
    }

    @Override
    public void exposureAllowed(SecuredInfo securedInfo, Long experimentId, Long exposureId) throws ExposureNotMatchingException {
        if (!exposureRepository.existsByExperiment_ExperimentIdAndExposureId(experimentId, exposureId)) {
            throw new ExposureNotMatchingException(TextConstants.EXPOSURE_NOT_MATCHING);
        }
    }

    @Override
    public void groupAllowed(SecuredInfo securedInfo, Long experimentId, Long groupId) throws GroupNotMatchingException {
        if (!groupRepository.existsByExperiment_ExperimentIdAndGroupId(experimentId, groupId)) {
            throw new GroupNotMatchingException(TextConstants.GROUP_NOT_MATCHING);
        }
    }

    @Override
    public void assignmentAllowed(SecuredInfo securedInfo, Long experimentId, Long assignmentId) throws AssignmentNotMatchingException {
        if (!assignmentRepository.existsByExposure_Experiment_ExperimentIdAndAssignmentId(experimentId, assignmentId)) {
            throw new AssignmentNotMatchingException(TextConstants.ASSIGNMENT_NOT_MATCHING);
        }
    }

    @Override
    public void assignmentAllowed(SecuredInfo securedInfo, Long experimentId, Long exposureId, Long assignmentId) throws AssignmentNotMatchingException {
        if (!assignmentRepository.existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndAssignmentId(experimentId, exposureId, assignmentId)) {
            throw new AssignmentNotMatchingException(TextConstants.ASSIGNMENT_NOT_MATCHING);
        }
    }

    @Override
    public void treatmentAllowed(SecuredInfo securedInfo, Long experimentId, Long conditionId, Long treatmentId) throws TreatmentNotMatchingException {
        if (!treatmentRepository.existsByCondition_Experiment_ExperimentIdAndCondition_ConditionIdAndTreatmentId(experimentId, conditionId, treatmentId)) {
            throw new TreatmentNotMatchingException(TextConstants.TREATMENT_NOT_MATCHING);
        }
    }

    @Override
    public void assessmentAllowed(SecuredInfo securedInfo, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId) throws AssessmentNotMatchingException {
        if (!assessmentRepository.existsByTreatment_Condition_Experiment_ExperimentIdAndTreatment_Condition_ConditionIdAndTreatment_TreatmentIdAndAssessmentId(
            experimentId, conditionId, treatmentId, assessmentId)
        ) {
            throw new AssessmentNotMatchingException(TextConstants.ASSESSMENT_NOT_MATCHING);
        }
    }

    @Override
    public void questionAllowed(SecuredInfo securedInfo, Long assessmentId, Long questionId) throws QuestionNotMatchingException {
        if (!questionRepository.existsByAssessment_AssessmentIdAndQuestionId(assessmentId, questionId)) {
            throw new QuestionNotMatchingException(TextConstants.QUESTION_NOT_MATCHING);
        }
    }

    @Override
    public void answerAllowed(SecuredInfo securedInfo, Long assessmentId, Long questionId, String answerType, Long answerId) throws AnswerNotMatchingException {
        if ("MC".equals(answerType)) {
            if (!answerMcRepository.existsByQuestion_Assessment_AssessmentIdAndQuestion_QuestionIdAndAnswerMcId(assessmentId, questionId, answerId)) {
                throw new AnswerNotMatchingException(TextConstants.ANSWER_NOT_MATCHING);
            }
        } //Note: as more answer types are added, continue checking. Same exception can be thrown.
    }

    @Override
    public void answerSubmissionAllowed(SecuredInfo securedInfo, Long questionSubmissionId, String answerType, Long answerSubmissionId) throws AnswerSubmissionNotMatchingException{
        if ("MC".equals(answerType)) {
            if (!answerMcSubmissionRepository.existsByQuestionSubmission_QuestionSubmissionIdAndAnswerMcSubId(questionSubmissionId, answerSubmissionId)) {
                throw new AnswerSubmissionNotMatchingException(TextConstants.ANSWER_SUBMISSION_NOT_MATCHING);
            }
        } else if ("ESSAY".equals(answerType)) {
            if (!answerEssaySubmissionRepository.existsByQuestionSubmission_QuestionSubmissionIdAndAnswerEssaySubmissionId(questionSubmissionId, answerSubmissionId)) {
                throw new AnswerSubmissionNotMatchingException(TextConstants.ANSWER_SUBMISSION_NOT_MATCHING);
            }
        }
        //Note: as more answer submission types are added, continue checking. Same exception can be thrown.
    }

    @Override
    public void submissionAllowed(SecuredInfo securedInfo, Long assessmentId, Long submissionId) throws SubmissionNotMatchingException {
        if (!submissionRepository.existsByAssessment_AssessmentIdAndSubmissionId(assessmentId, submissionId)) {
            throw new SubmissionNotMatchingException(TextConstants.SUBMISSION_NOT_MATCHING);
        }
    }

    @Override
    public void questionSubmissionAllowed(SecuredInfo securedInfo, Long assessmentId, Long submissionId, Long questionSubmissionId) throws QuestionSubmissionNotMatchingException {
        if (!questionSubmissionRepository.existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndQuestionSubmissionId(assessmentId, submissionId, questionSubmissionId)) {
            throw new QuestionSubmissionNotMatchingException(TextConstants.QUESTION_SUBMISSION_NOT_MATCHING);
        }
    }

    @Override
    public void submissionCommentAllowed(SecuredInfo securedInfo, Long assessmentId, Long submissionId, Long submissionCommentId) throws SubmissionCommentNotMatchingException {
        if (!submissionCommentRepository.existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndSubmissionCommentId(assessmentId, submissionId, submissionCommentId)) {
            throw new SubmissionCommentNotMatchingException(TextConstants.SUBMISSION_COMMENT_NOT_MATCHING);
        }
    }

    @Override
    public void questionSubmissionCommentAllowed(SecuredInfo securedInfo, Long questionSubmissionId, Long questionSubmissionCommentId) throws QuestionSubmissionCommentNotMatchingException {
        if (!questionSubmissionCommentRepository.existsByQuestionSubmission_QuestionSubmissionIdAndQuestionSubmissionCommentId(questionSubmissionId, questionSubmissionCommentId)) {
            throw new QuestionSubmissionCommentNotMatchingException(TextConstants.QUESTION_SUBMISSION_COMMENT_NOT_MATCHING);
        }
    }

    @Override
    public void outcomeAllowed(SecuredInfo securedInfo, Long experimentId, Long exposureId, Long outcomeId) throws OutcomeNotMatchingException {
        if (!outcomeRepository.existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndOutcomeId(experimentId, exposureId, outcomeId)) {
            throw new OutcomeNotMatchingException(TextConstants.OUTCOME_NOT_MATCHING);
        }
    }

    @Override
    public void outcomeScoreAllowed(SecuredInfo securedInfo, Long outcomeId, Long outcomeScoreId) throws OutcomeScoreNotMatchingException {
        if (!outcomeScoreRepository.existsByOutcome_OutcomeIdAndOutcomeScoreId(outcomeId, outcomeScoreId)) {
            throw new OutcomeScoreNotMatchingException(TextConstants.OUTCOME_SCORE_NOT_MATCHING);
        }
    }

    @Override
    public void integrationAllowed(long questionId, UUID integrationUuid) throws IntegrationOwnerNotMatchingException {
        if (!integrationRepository.existsByUuidAndQuestion_QuestionId(integrationUuid, questionId)) {
            throw new IntegrationOwnerNotMatchingException(String.format("Question with ID: [%s] does not own integration with UUID: [%s]", questionId, integrationUuid));
        }
    }

}
