package edu.iu.terracotta.service.app.impl;

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
import edu.iu.terracotta.model.ApiOneUseToken;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.enumerator.ExposureTypes;
import edu.iu.terracotta.model.oauth2.Roles;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AdminService;
import edu.iu.terracotta.service.lti.LTIDataService;
import edu.iu.terracotta.utils.lti.LTI3Request;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import io.jsonwebtoken.Locator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import edu.iu.terracotta.utils.oauth.OAuthUtils;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * This manages all the data processing for the LTIRequest (and for LTI in general)
 * Necessary to get appropriate TX handling and service management
 */
@Slf4j
@Service
@SuppressWarnings({"unchecked", "PMD.GuardLogStatement"})
public class APIJWTServiceImpl implements APIJWTService {

    private static final String ISSUER_LMS_OAUTH_API_TOKEN_REQUEST = "lmsOAuthAPITokenRequest";
    private static final String JWT_REQUEST_HEADER_NAME = "Authorization";
    private static final String JWT_BEARER_TYPE = "Bearer";
    private static final String QUERY_PARAM_NAME = "token";

    @Autowired private AllRepositories allRepositories;
    @Autowired private AdminService adminService;
    @Autowired private LTIDataService ltiDataService;

    @Value("${app.token.logging.enabled:true}")
    private boolean tokenLoggingEnabled;

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
    public Map<String, Object> unsecureToken(String token) {
        String[] jwtSections = token.split("\\.");
        String jwtPayload = new String(Base64.getUrlDecoder().decode(jwtSections[1]));

        try {
            return new ObjectMapper().readValue(jwtPayload, new TypeReference<Map<String,Object>>(){});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Request is not a valid LTI3 request.", e);
        }
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
        Integer studentAttempts
    ) throws GeneralSecurityException, IOException {
        return buildJwt(oneUse, ISSUER_TERRACOTTA_API, roles, contextId, platformDeploymentId, userId, assignmentId,
                experimentId, consent, canvasUserId, canvasUserGlobalId, canvasLoginId, canvasUserName, canvasCourseId,
                canvasAssignmentId, dueAt, lockAt, unlockAt, nonce, allowedAttempts, studentAttempts);
    }

    private String buildJwt(boolean oneUse,
        String issuer,
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
        Integer studentAttempts
    ) throws GeneralSecurityException, IOException {

        int length = 3600;
        // We only allow 30 seconds (surely we can low that) for the one time token, because that one must be traded immediately
        if (oneUse) {
            length = 300;
        }

        Date date = new Date();
        PrivateKey toolPrivateKey = OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey());
        Optional<PlatformDeployment> platformDeployment = allRepositories.platformDeploymentRepository.findById(platformDeploymentId);

        JwtBuilder builder = Jwts.builder()
            .header()
            .add(LtiStrings.KID, TextConstants.DEFAULT_KID)
            .add(LtiStrings.TYP, LtiStrings.JWT)
            .and()
            .issuer(issuer)
            .subject(userId) // The clientId
            .audience()
            .add(platformDeployment.isPresent() ? platformDeployment.get().getLocalUrl() : PlatformDeployment.LOCAL_URL) // We send here the authToken url.
            .and()
            .expiration(DateUtils.addSeconds(date, length)) // a java.util.Date
            .notBefore(date) // a java.util.Date
            .issuedAt(date) // for example, now
            .claim("contextId", contextId) // This is an specific claim to ask for tokens.
            .claim("platformDeploymentId", platformDeploymentId) // This is an specific claim to ask for tokens.
            .claim("userId", userId) // This is an specific claim to ask for tokens.
            .claim("roles", roles)
            .claim("assignmentId", assignmentId)
            .claim("consent", consent)
            .claim("experimentId", experimentId)
            .claim("oneUse", oneUse) // This is an specific claim to ask for tokens.
            .claim("canvasUserId", canvasUserId)
            .claim("canvasUserGlobalId", canvasUserGlobalId)
            .claim("canvasLoginId", canvasLoginId)
            .claim("canvasUserName", canvasUserName)
            .claim("canvasCourseId", canvasCourseId)
            .claim("canvasAssignmentId", canvasAssignmentId)
            .claim("dueAt", dueAt)
            .claim("lockAt", lockAt)
            .claim("unlockAt", unlockAt)
            .claim("nonce", nonce)
            .claim("allowedAttempts", allowedAttempts)
            .claim("studentAttempts", studentAttempts)
            .signWith(toolPrivateKey, SIG.RS256);  //We sign it with our own private key. The platform has the public one.

        String token = builder.compact();

        if (oneUse) {
            allRepositories.apiOneUseTokenRepository.save(new ApiOneUseToken(token));
        }

        if (tokenLoggingEnabled) {
            log.debug("Token Request: \n {} \n", token);
        }

        return token;
    }

    /**
     * This JWT will contain the token request
     */
    @Override
    public String buildJwt(boolean oneUse, LTI3Request lti3Request) throws GeneralSecurityException, IOException {
        return buildJwt(oneUse, ISSUER_TERRACOTTA_API, lti3Request);
    }

    private String buildJwt(boolean oneUse, String issuer, LTI3Request lti3Request) throws GeneralSecurityException, IOException {
        String targetLinkUrl = lti3Request.getLtiTargetLinkUrl();
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(targetLinkUrl).build().getQueryParams();
        String assignmentIdText = queryParams.getFirst("assignment");
        Long assignmentId = null;

        if (StringUtils.isNotBlank(assignmentIdText)) {
            assignmentId = Long.parseLong(assignmentIdText);
        }

        String consentText = queryParams.getFirst("consent");
        boolean consent = BooleanUtils.toBoolean(consentText);

        String experimentIdText = queryParams.getFirst("experiment");
        Long experimentId = null;

        if (StringUtils.isNotBlank(experimentIdText)) {
            experimentId = Long.parseLong(experimentIdText);
        }

        return buildJwt(oneUse, issuer, lti3Request.getLtiRoles(),
            lti3Request.getContext().getContextId(),
            lti3Request.getKey().getKeyId(),
            lti3Request.getUser().getUserKey(),
            assignmentId,
            experimentId,
            consent,
            lti3Request.getLtiCustom().get("canvas_user_id").toString(),
            lti3Request.getLtiCustom().get("canvas_user_global_id").toString(),
            lti3Request.getLtiCustom().get("canvas_login_id").toString(),
            lti3Request.getLtiCustom().get("canvas_user_name").toString(),
            lti3Request.getLtiCustom().get("canvas_course_id").toString(),
            lti3Request.getLtiCustom().get("canvas_assignment_id").toString(),
            lti3Request.getLtiCustom().get("due_at").toString(),
            lti3Request.getLtiCustom().get("lock_at").toString(),
            lti3Request.getLtiCustom().get("unlock_at").toString(),
            lti3Request.getNonce(),
            extractAllowedAttempts(lti3Request.getLtiCustom()),
            extractStudentAttempts(lti3Request.getLtiCustom()));
    }

    @Override
    public String generateStateForAPITokenRequest(LTI3Request lti3Request) throws GeneralSecurityException, IOException {
        return buildJwt(false, ISSUER_LMS_OAUTH_API_TOKEN_REQUEST, lti3Request);
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
            .issuer("TERRACOTTA")
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
    public String refreshToken(String token) throws GeneralSecurityException, IOException, BadTokenException {
        int length = 3600;
        Jws<Claims> tokenClaims = validateToken(token);

        if (tokenClaims == null) {
            throw new BadTokenException("Token is invalid.");
        }

        if (BooleanUtils.isTrue((Boolean) tokenClaims.getPayload().get("oneUse"))) {
            throw new BadTokenException("Trying to refresh an one use token");
        }

        Date date = new Date();
        PrivateKey toolPrivateKey = OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey());
        JwtBuilder builder = Jwts.builder()
            .header()
            .add(LtiStrings.KID, tokenClaims.getHeader().getKeyId())
            .add(LtiStrings.TYP, LtiStrings.JWT)
            .and()
            .issuer(tokenClaims.getPayload().getIssuer())
            .subject(tokenClaims.getPayload().getSubject()) // The clientId
            .audience()
            .add(tokenClaims.getPayload().getAudience())  //We send here the authToken url.
            .and()
            .expiration(DateUtils.addDays(date, length)) //a java.util.Date
            .notBefore(date) //a java.util.Date
            .issuedAt(date) // for example, now
            .claim("contextId", tokenClaims.getPayload().get("contextId"))
            .claim("platformDeploymentId", tokenClaims.getPayload().get("platformDeploymentId"))
            .claim("userId", tokenClaims.getPayload().get("userId"))
            .claim("roles", tokenClaims.getPayload().get("roles"))
            .claim("assignmentId", tokenClaims.getPayload().get("assignmentId"))
            .claim("consent", tokenClaims.getPayload().get("consent"))
            .claim("experimentId", tokenClaims.getPayload().get("experimentId"))
            .claim("oneUse", false)
            .claim("canvasUserId", tokenClaims.getPayload().get("canvasUserId"))
            .claim("canvasUserGlobalId", tokenClaims.getPayload().get("canvasUserGlobalId"))
            .claim("canvasLoginId", tokenClaims.getPayload().get("canvasLoginId"))
            .claim("canvasUserName", tokenClaims.getPayload().get("canvasUserName"))
            .claim("canvasCourseId", tokenClaims.getPayload().get("canvasCourseId"))
            .claim("canvasAssignmentId", tokenClaims.getPayload().get("canvasAssignmentId"))
            .claim("dueAt", tokenClaims.getPayload().get("dueAt"))
            .claim("lockAt", tokenClaims.getPayload().get("lockAt"))
            .claim("unlockAt", tokenClaims.getPayload().get("unlockAt"))
            .claim("nonce", tokenClaims.getPayload().get("nonce"))
            .claim("allowedAttempts", tokenClaims.getPayload().get("allowedAttempts"))
            .claim("studentAttempts", tokenClaims.getPayload().get("studentAttempts"))
            .signWith(toolPrivateKey, SIG.RS256);  //We sign it with our own private key. The platform has the public one.

        String newToken = builder.compact();

        if (tokenLoggingEnabled) {
            log.debug("Token Request: \n {} \n", newToken);
        }

        return newToken;
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
    public SecuredInfo extractValues(HttpServletRequest request, boolean allowQueryParam) {
        String token = extractJwtStringValue(request,allowQueryParam);
        Jws<Claims> claims = validateToken(token);

        if (claims == null) {
            return null;
        }

        SecuredInfo securedInfo = new SecuredInfo();
        securedInfo.setUserId(claims.getPayload().get("userId").toString());
        securedInfo.setPlatformDeploymentId(Long.valueOf((Integer) claims.getPayload().get("platformDeploymentId")));
        securedInfo.setContextId(Long.valueOf((Integer) claims.getPayload().get("contextId")));
        securedInfo.setRoles((List<String>) claims.getPayload().get("roles"));
        securedInfo.setCanvasUserId(claims.getPayload().get("canvasUserId").toString());
        securedInfo.setCanvasUserGlobalId(claims.getPayload().get("canvasUserGlobalId").toString());
        securedInfo.setCanvasLoginId(claims.getPayload().get("canvasLoginId").toString());
        securedInfo.setCanvasUserName(claims.getPayload().get("canvasUserName").toString());
        securedInfo.setCanvasCourseId(claims.getPayload().get("canvasCourseId").toString());
        securedInfo.setCanvasAssignmentId(claims.getPayload().get("canvasAssignmentId").toString());
        securedInfo.setDueAt(extractTimestamp(claims,"dueAt"));
        securedInfo.setLockAt(extractTimestamp(claims,"lockAt"));
        securedInfo.setUnlockAt(extractTimestamp(claims,"unlockAt"));
        securedInfo.setNonce(claims.getPayload().get("nonce").toString());
        securedInfo.setConsent((Boolean)claims.getPayload().get("consent"));
        securedInfo.setAllowedAttempts(claims.getPayload().get("allowedAttempts", Integer.class));
        securedInfo.setStudentAttempts(claims.getPayload().get("studentAttempts", Integer.class));

        return securedInfo;
    }

    private Timestamp extractTimestamp(Jws<Claims> claims, String id) {
        Timestamp extracted;

        try {
            extracted = Timestamp.valueOf(LocalDateTime.parse(claims.getPayload().get(id).toString()));
        } catch (Exception ex) {
            return null;
        }

        return extracted;
    }

    private Integer parseInt(Object value) {
        try {
            return value != null ? Integer.valueOf(value.toString()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get allowed_attempts from LTI custom claims (variable substitution). If
     * allowed_attempts claim exists, but the value is null, return -1 instead. The
     * special value -1 stands in place of null to indicate unlimited attempts since
     * the value of null is indistinguishable from there not being an
     * allowed_attempts claim.
     *
     * @param ltiCustomClaims
     * @return
     */
    private Integer extractAllowedAttempts(Map<String, Object> ltiCustomClaims) {
        if (ltiCustomClaims.containsKey("allowed_attempts")) {
            Integer allowedAttempts = parseInt(ltiCustomClaims.get("allowed_attempts"));

            if (allowedAttempts != null) {
                return allowedAttempts;
            } else if (ltiCustomClaims.get("allowed_attempts") == null) {
                return -1;
            }
        }

        return null;
    }

    /**
     * Get student_attempts from LTI custom claims (variable substitution). If
     * student_attempts claim exists, but the value is null, return 0 instead.
     *
     * @param ltiCustomClaims
     * @return
     */
    private Integer extractStudentAttempts(Map<String, Object> ltiCustomClaims) {
        if (ltiCustomClaims.containsKey("student_attempts")) {
            Integer allowedAttempts = parseInt(ltiCustomClaims.get("student_attempts"));

            if (allowedAttempts != null) {
                return allowedAttempts;
            } else if (ltiCustomClaims.get("student_attempts") == null) {
                return 0;
            }
        }

        return null;
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
    public void experimentAllowed(SecuredInfo securedInfo, Long experimentId) throws BadTokenException, ExperimentNotMatchingException {
        if (securedInfo ==null) {
            log.error(TextConstants.BAD_TOKEN);
            throw new BadTokenException(TextConstants.BAD_TOKEN);
        }

        if (!allRepositories.experimentRepository.existsByExperimentIdAndPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(experimentId, securedInfo.getPlatformDeploymentId(), securedInfo.getContextId())) {
            throw new ExperimentNotMatchingException(TextConstants.EXPERIMENT_NOT_MATCHING);
        }
    }

    @Override
    public boolean experimentLocked(Long experimentId, boolean throwException) throws ExperimentLockedException, ExperimentNotMatchingException {
        Optional<Experiment> experiment = allRepositories.experimentRepository.findById(experimentId);

        if (!experiment.isPresent()) {
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
        Optional<Experiment> experiment = allRepositories.experimentRepository.findById(experimentId);

        if (!experiment.isPresent()) {
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
        if (!allRepositories.conditionRepository.existsByExperiment_ExperimentIdAndConditionId(experimentId, conditionId)) {
            throw new ConditionNotMatchingException(TextConstants.CONDITION_NOT_MATCHING);
        }
    }

    @Override
    public void participantAllowed(SecuredInfo securedInfo, Long experimentId, Long participantId) throws ParticipantNotMatchingException {
        if (!allRepositories.participantRepository.existsByExperiment_ExperimentIdAndParticipantId(experimentId, participantId)) {
            throw new ParticipantNotMatchingException(TextConstants.PARTICIPANT_NOT_MATCHING);
        }
    }

    @Override
    public void exposureAllowed(SecuredInfo securedInfo, Long experimentId, Long exposureId) throws ExposureNotMatchingException {
        if (!allRepositories.exposureRepository.existsByExperiment_ExperimentIdAndExposureId(experimentId, exposureId)) {
            throw new ExposureNotMatchingException(TextConstants.EXPOSURE_NOT_MATCHING);
        }
    }

    @Override
    public void groupAllowed(SecuredInfo securedInfo, Long experimentId, Long groupId) throws GroupNotMatchingException {
        if (!allRepositories.groupRepository.existsByExperiment_ExperimentIdAndGroupId(experimentId, groupId)) {
            throw new GroupNotMatchingException(TextConstants.GROUP_NOT_MATCHING);
        }
    }

    @Override
    public void assignmentAllowed(SecuredInfo securedInfo, Long experimentId, Long assignmentId) throws AssignmentNotMatchingException {
        if (!allRepositories.assignmentRepository.existsByExposure_Experiment_ExperimentIdAndAssignmentId(experimentId, assignmentId)) {
            throw new AssignmentNotMatchingException(TextConstants.ASSIGNMENT_NOT_MATCHING);
        }
    }

    @Override
    public void assignmentAllowed(SecuredInfo securedInfo, Long experimentId, Long exposureId, Long assignmentId) throws AssignmentNotMatchingException {
        if (!allRepositories.assignmentRepository.existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndAssignmentId(experimentId, exposureId, assignmentId)) {
            throw new AssignmentNotMatchingException(TextConstants.ASSIGNMENT_NOT_MATCHING);
        }
    }

    @Override
    public void treatmentAllowed(SecuredInfo securedInfo, Long experimentId, Long conditionId, Long treatmentId) throws TreatmentNotMatchingException {
        if (!allRepositories.treatmentRepository.existsByCondition_Experiment_ExperimentIdAndCondition_ConditionIdAndTreatmentId(experimentId, conditionId, treatmentId)) {
            throw new TreatmentNotMatchingException(TextConstants.TREATMENT_NOT_MATCHING);
        }
    }

    @Override
    public void assessmentAllowed(SecuredInfo securedInfo, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId) throws AssessmentNotMatchingException {
        if (!allRepositories.assessmentRepository.existsByTreatment_Condition_Experiment_ExperimentIdAndTreatment_Condition_ConditionIdAndTreatment_TreatmentIdAndAssessmentId(
            experimentId, conditionId, treatmentId, assessmentId)
        ) {
            throw new AssessmentNotMatchingException(TextConstants.ASSESSMENT_NOT_MATCHING);
        }
    }

    @Override
    public void questionAllowed(SecuredInfo securedInfo, Long assessmentId, Long questionId) throws QuestionNotMatchingException {
        if (!allRepositories.questionRepository.existsByAssessment_AssessmentIdAndQuestionId(assessmentId, questionId)) {
            throw new QuestionNotMatchingException(TextConstants.QUESTION_NOT_MATCHING);
        }
    }

    @Override
    public void answerAllowed(SecuredInfo securedInfo, Long assessmentId, Long questionId, String answerType, Long answerId) throws AnswerNotMatchingException {
        if ("MC".equals(answerType)) {
            if (!allRepositories.answerMcRepository.existsByQuestion_Assessment_AssessmentIdAndQuestion_QuestionIdAndAnswerMcId(assessmentId, questionId, answerId)) {
                throw new AnswerNotMatchingException(TextConstants.ANSWER_NOT_MATCHING);
            }
        } //Note: as more answer types are added, continue checking. Same exception can be thrown.
    }

    @Override
    public void answerSubmissionAllowed(SecuredInfo securedInfo, Long questionSubmissionId, String answerType, Long answerSubmissionId) throws AnswerSubmissionNotMatchingException{
        if ("MC".equals(answerType)) {
            if (!allRepositories.answerMcSubmissionRepository.existsByQuestionSubmission_QuestionSubmissionIdAndAnswerMcSubId(questionSubmissionId, answerSubmissionId)) {
                throw new AnswerSubmissionNotMatchingException(TextConstants.ANSWER_SUBMISSION_NOT_MATCHING);
            }
        } else if ("ESSAY".equals(answerType)) {
            if (!allRepositories.answerEssaySubmissionRepository.existsByQuestionSubmission_QuestionSubmissionIdAndAnswerEssaySubmissionId(questionSubmissionId, answerSubmissionId)) {
                throw new AnswerSubmissionNotMatchingException(TextConstants.ANSWER_SUBMISSION_NOT_MATCHING);
            }
        }
        //Note: as more answer submission types are added, continue checking. Same exception can be thrown.
    }

    @Override
    public void submissionAllowed(SecuredInfo securedInfo, Long assessmentId, Long submissionId) throws SubmissionNotMatchingException {
        if (!allRepositories.submissionRepository.existsByAssessment_AssessmentIdAndSubmissionId(assessmentId, submissionId)) {
            throw new SubmissionNotMatchingException(TextConstants.SUBMISSION_NOT_MATCHING);
        }
    }

    @Override
    public void questionSubmissionAllowed(SecuredInfo securedInfo, Long assessmentId, Long submissionId, Long questionSubmissionId) throws QuestionSubmissionNotMatchingException {
        if (!allRepositories.questionSubmissionRepository.existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndQuestionSubmissionId(assessmentId, submissionId, questionSubmissionId)) {
            throw new QuestionSubmissionNotMatchingException(TextConstants.QUESTION_SUBMISSION_NOT_MATCHING);
        }
    }

    @Override
    public void submissionCommentAllowed(SecuredInfo securedInfo, Long assessmentId, Long submissionId, Long submissionCommentId) throws SubmissionCommentNotMatchingException {
        if (!allRepositories.submissionCommentRepository.existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndSubmissionCommentId(assessmentId, submissionId, submissionCommentId)) {
            throw new SubmissionCommentNotMatchingException(TextConstants.SUBMISSION_COMMENT_NOT_MATCHING);
        }
    }

    @Override
    public void questionSubmissionCommentAllowed(SecuredInfo securedInfo, Long questionSubmissionId, Long questionSubmissionCommentId) throws QuestionSubmissionCommentNotMatchingException {
        if (!allRepositories.questionSubmissionCommentRepository.existsByQuestionSubmission_QuestionSubmissionIdAndQuestionSubmissionCommentId(questionSubmissionId, questionSubmissionCommentId)) {
            throw new QuestionSubmissionCommentNotMatchingException(TextConstants.QUESTION_SUBMISSION_COMMENT_NOT_MATCHING);
        }
    }

    @Override
    public void outcomeAllowed(SecuredInfo securedInfo, Long experimentId, Long exposureId, Long outcomeId) throws OutcomeNotMatchingException {
        if (!allRepositories.outcomeRepository.existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndOutcomeId(experimentId, exposureId, outcomeId)) {
            throw new OutcomeNotMatchingException(TextConstants.OUTCOME_NOT_MATCHING);
        }
    }

    @Override
    public void outcomeScoreAllowed(SecuredInfo securedInfo, Long outcomeId, Long outcomeScoreId) throws OutcomeScoreNotMatchingException {
        if (!allRepositories.outcomeScoreRepository.existsByOutcome_OutcomeIdAndOutcomeScoreId(outcomeId, outcomeScoreId)) {
            throw new OutcomeScoreNotMatchingException(TextConstants.OUTCOME_SCORE_NOT_MATCHING);
        }
    }

}
