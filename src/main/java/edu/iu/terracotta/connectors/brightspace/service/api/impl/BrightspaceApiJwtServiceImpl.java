package edu.iu.terracotta.connectors.brightspace.service.api.impl;

import edu.iu.terracotta.connectors.brightspace.dao.model.enums.jwt.BrightspaceJwtClaim;
import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiOneUseToken;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.model.enums.jwt.JwtClaim;
import edu.iu.terracotta.connectors.generic.dao.repository.api.ApiOneUseTokenRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.connectors.generic.service.lti.LtiDataService;
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
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import io.jsonwebtoken.Locator;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import edu.iu.terracotta.utils.oauth.OAuthUtils;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
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
@Service
@TerracottaConnector(LmsConnector.BRIGHTSPACE)
@SuppressWarnings({"unchecked", "PMD.GuardLogStatement", "PMD.LooseCoupling"})
public class BrightspaceApiJwtServiceImpl implements ApiJwtService {

    @Autowired private ApiOneUseTokenRepository apiOneUseTokenRepository;
    @Autowired private PlatformDeploymentRepository platformDeploymentRepository;
    @Autowired private LtiDataService ltiDataService;

    @Value("${app.token.logging.enabled:true}")
    private boolean tokenLoggingEnabled;

    /**
     * This will check that the state has been signed by us and retrieve the issuer private key.
     */
    @Override
    public Jws<Claims> validateToken(String token) {
        // This is done because each state is signed with a different key based on the issuer... so
        // we don't know the key and we need to check it pre-extracting the claims and finding the kid
        try {
            return Jwts.parser()
                .keyLocator(
                    new Locator<Key>() {
                        @Override
                        public Key locate(Header header) {
                            if (header instanceof JwsHeader) {
                                try {
                                    // We are dealing with RS256 encryption, so we have some Oauth utils to manage the keys and
                                    // convert them to keys from the string stored in DB. There are for sure other ways to manage this.
                                    return OAuthUtils.loadPublicKey(ltiDataService.getOwnPublicKey());
                                } catch (GeneralSecurityException ex) {
                                    log.error("Error validating the state. Error generating the tool public key", ex);
                                    return null;
                                }
                            }

                                return null;
                            }
                    }
                )
                .build()
                .parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired.", e);
            return null;
        }
    }

    @Override
    public boolean validateFileToken(String token, String fileId) {
        Jws<Claims> claims = validateToken(token);

        return claims != null && claims.getPayload().get(JwtClaim.FILE_ID.key()) != null && Strings.CS.equals(claims.getPayload().get(JwtClaim.FILE_ID.key()).toString(), fileId);
    }

    @Override
    public Map<String, Object> unsecureToken(String token, PlatformDeployment platformDeployment) {
        try {
            return JsonMapper.builder()
                .build()
                .readValue(
                    token,
                    new TypeReference<Map<String,Object>>() {}
                );
        } catch (JacksonException e) {
            throw new IllegalStateException("Request is not a valid LTI3 request.", e);
        }
    }

    @Override
    public String buildJwt(long platformDeploymentId, String userKey, Claims claims) throws GeneralSecurityException, IOException {
        return buildJwt(
            true,
            claims.get(JwtClaim.ROLES.key(), List.class),
            claims.get(JwtClaim.CONTEXT_ID.key(), Long.class),
            platformDeploymentId,
            userKey,
            claims.get(JwtClaim.ASSIGNMENT_ID.key(), Long.class),
            claims.get(JwtClaim.EXPERIMENT_ID.key(), Long.class),
            claims.get(JwtClaim.CONSENT.key(), Boolean.class),
            claims.get(BrightspaceJwtClaim.BRIGHTSPACE_USER_ID.key(), String.class),
            claims.get(BrightspaceJwtClaim.BRIGHTSPACE_USER_GLOBAL_ID.key(), String.class),
            claims.get(BrightspaceJwtClaim.BRIGHTSPACE_LOGIN_ID.key(), String.class),
            claims.get(BrightspaceJwtClaim.BRIGHTSPACE_USER_NAME.key(), String.class),
            claims.get(BrightspaceJwtClaim.BRIGHTSPACE_COURSE_ID.key(), String.class),
            claims.get(JwtClaim.LMS_ASSIGNMENT_ID.key(), String.class),
            claims.get(JwtClaim.DUE_AT.key(), String.class),
            claims.get(JwtClaim.LOCK_AT.key(), String.class),
            claims.get(JwtClaim.UNLOCK_AT.key(), String.class),
            claims.get(JwtClaim.NONCE.key(), String.class),
            claims.get(JwtClaim.ALLOWED_ATTEMPTS.key(), Integer.class),
            claims.get(JwtClaim.STUDENT_ATTEMPTS.key(), Integer.class)
        );
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
        String brightspaceUserId,
        String brightspaceUserGlobalId,
        String brightspaceLoginId,
        String brightspaceUserName,
        String brightspaceCourseId,
        String lmsAssignmentId,
        String dueAt,
        String lockAt,
        String unlockAt,
        String nonce,
        Integer allowedAttempts,
        Integer studentAttempts
    ) throws GeneralSecurityException, IOException {
        return buildJwt(
            oneUse,
            ISSUER_TERRACOTTA_API,
            roles,
            contextId,
            platformDeploymentId,
            userId,
            assignmentId,
            experimentId,
            consent,
            brightspaceUserId,
            brightspaceUserGlobalId,
            brightspaceLoginId,
            brightspaceUserName,
            brightspaceCourseId,
            lmsAssignmentId,
            dueAt,
            lockAt,
            unlockAt,
            nonce,
            allowedAttempts,
            studentAttempts
        );
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
        String brightspaceUserId,
        String brightspaceUserGlobalId,
        String brightspaceLoginId,
        String brightspaceUserName,
        String brightspaceCourseId,
        String lmsAssignmentId,
        String dueAt,
        String lockAt,
        String unlockAt,
        String nonce,
        Integer allowedAttempts,
        Integer studentAttempts
    ) throws GeneralSecurityException, IOException {

        int length = 3600;

        // 30 seconds TTL for the one time token, because that one must be traded immediately
        if (oneUse) {
            length = 300;
        }

        Date date = new Date();
        Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(platformDeploymentId);

        String token = Jwts.builder()
            .header()
            .add(LtiStrings.KID, TextConstants.DEFAULT_KID)
            .add(LtiStrings.TYP, LtiStrings.JWT)
            .and()
            .issuer(issuer)
            .subject(userId)
            .audience()
            .add(platformDeployment.isPresent() ? platformDeployment.get().getLocalUrl() : PlatformDeployment.LOCAL_URL)
            .and()
            .expiration(DateUtils.addSeconds(date, length))
            .notBefore(date)
            .issuedAt(date)
            .claim(JwtClaim.CONTEXT_ID.key(), contextId)
            .claim(JwtClaim.PLATFORM_DEPLOYMENT_ID.key(), platformDeploymentId)
            .claim(JwtClaim.USER_ID.key(), userId)
            .claim(JwtClaim.ROLES.key(), roles)
            .claim(JwtClaim.ASSIGNMENT_ID.key(), assignmentId)
            .claim(JwtClaim.CONSENT.key(), consent)
            .claim(JwtClaim.EXPERIMENT_ID.key(), experimentId)
            .claim(JwtClaim.ONE_USE.key(), oneUse)
            .claim(BrightspaceJwtClaim.BRIGHTSPACE_USER_ID.key(), brightspaceUserId)
            .claim(BrightspaceJwtClaim.BRIGHTSPACE_USER_GLOBAL_ID.key(), brightspaceUserGlobalId)
            .claim(BrightspaceJwtClaim.BRIGHTSPACE_LOGIN_ID.key(), brightspaceLoginId)
            .claim(BrightspaceJwtClaim.BRIGHTSPACE_USER_NAME.key(), brightspaceUserName)
            .claim(BrightspaceJwtClaim.BRIGHTSPACE_COURSE_ID.key(), brightspaceCourseId)
            .claim(JwtClaim.LMS_ASSIGNMENT_ID.key(), lmsAssignmentId)
            .claim(JwtClaim.DUE_AT.key(), dueAt)
            .claim(JwtClaim.LOCK_AT.key(), lockAt)
            .claim(JwtClaim.UNLOCK_AT.key(), unlockAt)
            .claim(JwtClaim.NONCE.key(), nonce)
            .claim(JwtClaim.ALLOWED_ATTEMPTS.key(), allowedAttempts)
            .claim(JwtClaim.STUDENT_ATTEMPTS.key(), studentAttempts)
            .claim(JwtClaim.LMS_NAME.key(), BrightspaceJwtClaim.BRIGHTSPACE.key())
            .signWith(OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey()), SIG.RS256)
            .compact();

        if (oneUse) {
            apiOneUseTokenRepository.save(new ApiOneUseToken(token));
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
    public String buildJwt(boolean oneUse, Lti3Request lti3Request) throws GeneralSecurityException, IOException {
        return buildJwt(oneUse, ISSUER_TERRACOTTA_API, lti3Request);
    }

    private String buildJwt(boolean oneUse, String issuer, Lti3Request lti3Request) throws GeneralSecurityException, IOException {
        String targetLinkUrl = lti3Request.getLtiTargetLinkUrl();
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(targetLinkUrl).build().getQueryParams();
        String assignmentIdText = queryParams.getFirst(JwtClaim.ASSIGNMENT.key());
        Long assignmentId = null;

        if (StringUtils.isNotBlank(assignmentIdText)) {
            assignmentId = Long.parseLong(assignmentIdText);
        }

        String experimentIdText = queryParams.getFirst(JwtClaim.EXPERIMENT.key());
        Long experimentId = null;

        if (StringUtils.isNotBlank(experimentIdText)) {
            experimentId = Long.parseLong(experimentIdText);
        }

        return buildJwt(
            oneUse,
            issuer,
            lti3Request.getLtiRoles(),
            lti3Request.getContext().getContextId(),
            lti3Request.getKey().getKeyId(),
            lti3Request.getUser().getUserKey(),
            assignmentId,
            experimentId,
            BooleanUtils.toBoolean(queryParams.getFirst(JwtClaim.CONSENT.key())),
            lti3Request.getUser().getLmsUserId(),
            lti3Request.getUser().getUserKey(),
            MapUtils.getString(lti3Request.getLtiCustom(), BrightspaceJwtClaim.BRIGHTSPACE_LOGIN_ID.key(), ""),
            MapUtils.getString(lti3Request.getLtiCustom(), BrightspaceJwtClaim.BRIGHTSPACE_USER_NAME.key(), ""),
            lti3Request.getContext().getContextKey(),
            MapUtils.getString(lti3Request.getLtiCustom(), BrightspaceJwtClaim.BRIGHTSPACE_ASSIGNMENT_ID.key(), ""),
            MapUtils.getString(lti3Request.getLtiCustom(), JwtClaim.DUE_AT.key(), ""),
            MapUtils.getString(lti3Request.getLtiCustom(), JwtClaim.LOCK_AT.key(), ""),
            MapUtils.getString(lti3Request.getLtiCustom(), JwtClaim.UNLOCK_AT.key(), ""),
            lti3Request.getNonce(),
            extractAllowedAttempts(lti3Request.getLtiCustom()),
            extractStudentAttempts(lti3Request.getLtiCustom()));
    }

    @Override
    public String generateStateForAPITokenRequest(Lti3Request lti3Request) throws GeneralSecurityException, IOException {
        return buildJwt(false, JwtClaim.ISSUER_LMS_OAUTH_API_TOKEN_REQUEST.key(), lti3Request);
    }

    @Override
    public Optional<Jws<Claims>> validateStateForAPITokenRequest(String state) {
        Jws<Claims> claims = validateToken(state);

        if (claims != null) {
            String issuer = claims.getPayload().getIssuer();

            if (!JwtClaim.ISSUER_LMS_OAUTH_API_TOKEN_REQUEST.key().equals(issuer)) {
                return Optional.empty();
            }

            return Optional.of(claims);
        }

        return Optional.empty();
    }

    @Override
    public String buildFileToken(String fileId, String localUrl) throws GeneralSecurityException {
        Date date = new Date();
        String token = Jwts.builder()
            .header()
            .add(LtiStrings.KID, TextConstants.DEFAULT_KID)
            .add(LtiStrings.TYP, LtiStrings.JWT)
            .and()
            .issuer(JwtClaim.TERRACOTTA.key())
            .subject(JwtClaim.NO_USER.key())
            .audience()
            .add(localUrl)
            .and()
            .expiration(DateUtils.addSeconds(date, 3600))
            .notBefore(date)
            .issuedAt(date)
            .claim(JwtClaim.FILE_ID.key(), fileId)
            .signWith(OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey()), SIG.RS256)
            .compact();

        if (tokenLoggingEnabled) {
            log.debug("Token Request: \n {} \n", token);
        }

        return token;
    }

    @Override
    public String refreshToken(String token) throws GeneralSecurityException, IOException, BadTokenException {
        Jws<Claims> tokenClaims = validateToken(token);

        if (tokenClaims == null) {
            throw new BadTokenException("Token is invalid.");
        }

        if (BooleanUtils.isTrue((Boolean) tokenClaims.getPayload().get("oneUse"))) {
            throw new BadTokenException("Trying to refresh an one use token");
        }

        Date date = new Date();
        String newToken = Jwts.builder()
            .header()
            .add(LtiStrings.KID, tokenClaims.getHeader().getKeyId())
            .add(LtiStrings.TYP, LtiStrings.JWT)
            .and()
            .issuer(tokenClaims.getPayload().getIssuer())
            .subject(tokenClaims.getPayload().getSubject())
            .audience()
            .add(tokenClaims.getPayload().getAudience())
            .and()
            .expiration(DateUtils.addSeconds(date, 3600))
            .notBefore(date)
            .issuedAt(date)
            .claim(JwtClaim.CONTEXT_ID.key(), tokenClaims.getPayload().get(JwtClaim.CONTEXT_ID.key()))
            .claim(JwtClaim.PLATFORM_DEPLOYMENT_ID.key(), tokenClaims.getPayload().get(JwtClaim.PLATFORM_DEPLOYMENT_ID.key()))
            .claim(JwtClaim.USER_ID.key(), tokenClaims.getPayload().get(JwtClaim.USER_ID.key()))
            .claim(JwtClaim.ROLES.key(), tokenClaims.getPayload().get(JwtClaim.ROLES.key()))
            .claim(JwtClaim.ASSIGNMENT_ID.key(), tokenClaims.getPayload().get(JwtClaim.ASSIGNMENT_ID.key()))
            .claim(JwtClaim.CONSENT.key(), tokenClaims.getPayload().get(JwtClaim.CONSENT.key()))
            .claim(JwtClaim.EXPERIMENT_ID.key(), tokenClaims.getPayload().get(JwtClaim.EXPERIMENT_ID.key()))
            .claim(JwtClaim.ONE_USE.key(), false)
            .claim(BrightspaceJwtClaim.BRIGHTSPACE_USER_ID.key(), tokenClaims.getPayload().getOrDefault(BrightspaceJwtClaim.BRIGHTSPACE_USER_ID.key(), tokenClaims.getPayload().get(JwtClaim.USER_ID.key())).toString())
            .claim(BrightspaceJwtClaim.BRIGHTSPACE_USER_GLOBAL_ID.key(), tokenClaims.getPayload().get(BrightspaceJwtClaim.BRIGHTSPACE_USER_GLOBAL_ID.key()))
            .claim(BrightspaceJwtClaim.BRIGHTSPACE_LOGIN_ID.key(), tokenClaims.getPayload().get(BrightspaceJwtClaim.BRIGHTSPACE_LOGIN_ID.key()))
            .claim(BrightspaceJwtClaim.BRIGHTSPACE_USER_NAME.key(), tokenClaims.getPayload().get(BrightspaceJwtClaim.BRIGHTSPACE_USER_NAME.key()))
            .claim(BrightspaceJwtClaim.BRIGHTSPACE_COURSE_ID.key(), tokenClaims.getPayload().get(BrightspaceJwtClaim.BRIGHTSPACE_COURSE_ID.key()))
            .claim(JwtClaim.LMS_ASSIGNMENT_ID.key(), tokenClaims.getPayload().get(JwtClaim.LMS_ASSIGNMENT_ID.key()))
            .claim(JwtClaim.DUE_AT.key(), null)
            .claim(JwtClaim.LOCK_AT.key(), null)
            .claim(JwtClaim.UNLOCK_AT.key(), null)
            .claim(JwtClaim.NONCE.key(), tokenClaims.getPayload().get(JwtClaim.NONCE.key()))
            .claim(JwtClaim.ALLOWED_ATTEMPTS.key(), tokenClaims.getPayload().get(JwtClaim.ALLOWED_ATTEMPTS.key()))
            .claim(JwtClaim.STUDENT_ATTEMPTS.key(), tokenClaims.getPayload().get(JwtClaim.STUDENT_ATTEMPTS.key()))
            .claim(JwtClaim.LMS_NAME.key(), BrightspaceJwtClaim.BRIGHTSPACE.key())
            .signWith(OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey()), SIG.RS256)
            .compact();

        if (tokenLoggingEnabled) {
            log.debug("Token Request: \n {} \n", newToken);
        }

        return newToken;
    }

    @Override
    public String extractJwtStringValue(HttpServletRequest request, boolean allowQueryParam) {
        String rawHeaderValue = StringUtils.trimToNull(request.getHeader(JwtClaim.JWT_REQUEST_HEADER_NAME.key()));

        if (rawHeaderValue == null) {
            if (allowQueryParam) {
                return StringUtils.trimToNull(request.getParameter(JwtClaim.QUERY_PARAM_NAME.key()));
            }

            return null;
        }

        if (isBearerToken(rawHeaderValue)) {
            return rawHeaderValue.substring(JwtClaim.JWT_BEARER_TYPE.key().length()).trim();
        }

        return null;
    }

    @Override
    public SecuredInfo extractValues(HttpServletRequest request, boolean allowQueryParam) {
        return extractValues(
            extractJwtStringValue(
                request,
                allowQueryParam
            )
        );
    }

    @Override
    public SecuredInfo extractValues(String token) {
        Jws<Claims> claims = validateToken(token);

        if (claims == null) {
            return null;
        }

        return SecuredInfo.builder()
            .allowedAttempts(claims.getPayload().get(JwtClaim.ALLOWED_ATTEMPTS.key(), Integer.class))
            .consent((Boolean) claims.getPayload().get(JwtClaim.CONSENT.key()))
            .contextId(Long.valueOf((Integer) claims.getPayload().get(JwtClaim.CONTEXT_ID.key())))
            .dueAt(null)
            .lmsAssignmentId(claims.getPayload().getOrDefault(JwtClaim.LMS_ASSIGNMENT_ID.key(), "").toString())
            .lmsCourseId(claims.getPayload().get(BrightspaceJwtClaim.BRIGHTSPACE_COURSE_ID.key()).toString())
            .lmsLoginId(claims.getPayload().get(BrightspaceJwtClaim.BRIGHTSPACE_LOGIN_ID.key()).toString())
            .lmsName(claims.getPayload().getOrDefault(JwtClaim.LMS_NAME.key(), BrightspaceJwtClaim.BRIGHTSPACE.key()).toString())
            .lmsUserGlobalId(claims.getPayload().get(BrightspaceJwtClaim.BRIGHTSPACE_USER_GLOBAL_ID.key()).toString())
            .lmsUserId(
                extractUserId(
                    claims.getPayload().getOrDefault(
                        BrightspaceJwtClaim.BRIGHTSPACE_USER_ID.key(),
                        claims.getPayload().get(JwtClaim.USER_ID.key())
                    )
                    .toString()
                )
            )
            .lmsUserName(claims.getPayload().get(BrightspaceJwtClaim.BRIGHTSPACE_USER_NAME.key()).toString())
            .lockAt(null)
            .nonce(claims.getPayload().get(JwtClaim.NONCE.key()).toString())
            .platformDeploymentId(Long.valueOf((Integer) claims.getPayload().get(JwtClaim.PLATFORM_DEPLOYMENT_ID.key())))
            .roles((List<String>) claims.getPayload().get(JwtClaim.ROLES.key()))
            .studentAttempts(claims.getPayload().get(JwtClaim.STUDENT_ATTEMPTS.key(), Integer.class))
            .unlockAt(null)
            .userId(claims.getPayload().get(JwtClaim.USER_ID.key()).toString())
            .build();
    }

    @Override
    public ResponseEntity<String> getTimedToken(HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        return getTimedToken(
            extractJwtStringValue(
                req,
                true
            )
        );
    }

    @Override
    public ResponseEntity<String> getTimedToken(String token) throws NumberFormatException, TerracottaConnectorException {
        Jws<Claims> claims = validateToken(token);

        if ((Boolean) claims.getPayload().get(JwtClaim.ONE_USE.key())) {
            try {
                // experimentId and assignmentId are optionals so check the null.
                Long assignmentId = null;

                if (claims.getPayload().get(JwtClaim.ASSIGNMENT_ID.key()) != null) {
                    assignmentId = Long.parseLong(claims.getPayload().get(JwtClaim.ASSIGNMENT_ID.key()).toString());
                }

                Long experimentId = null;

                if (claims.getPayload().get(JwtClaim.EXPERIMENT_ID.key()) != null) {
                    experimentId = Long.parseLong(claims.getPayload().get(JwtClaim.EXPERIMENT_ID.key()).toString());
                }

                return new ResponseEntity<>(
                    buildJwt(
                        false,
                        (List<String>) claims.getPayload().get(JwtClaim.ROLES.key()),
                        Long.parseLong(claims.getPayload().get(JwtClaim.CONTEXT_ID.key()).toString()),
                        Long.parseLong(claims.getPayload().get(JwtClaim.PLATFORM_DEPLOYMENT_ID.key()).toString()),
                        claims.getPayload().get(JwtClaim.USER_ID.key()).toString(),
                        assignmentId,
                        experimentId,
                        (Boolean) claims.getPayload().get(JwtClaim.CONSENT.key()),
                        claims.getPayload().getOrDefault(BrightspaceJwtClaim.BRIGHTSPACE_USER_ID.key(), claims.getPayload().get(JwtClaim.USER_ID.key()).toString()).toString(),
                        claims.getPayload().get(BrightspaceJwtClaim.BRIGHTSPACE_USER_GLOBAL_ID.key()).toString(),
                        claims.getPayload().get(BrightspaceJwtClaim.BRIGHTSPACE_LOGIN_ID.key()).toString(),
                        claims.getPayload().get(BrightspaceJwtClaim.BRIGHTSPACE_USER_NAME.key()).toString(),
                        claims.getPayload().get(BrightspaceJwtClaim.BRIGHTSPACE_COURSE_ID.key()).toString(),
                        claims.getPayload().getOrDefault(JwtClaim.LMS_ASSIGNMENT_ID.key(), "").toString(),
                        claims.getPayload().get(JwtClaim.DUE_AT.key()).toString(),
                        claims.getPayload().get(JwtClaim.LOCK_AT.key()).toString(),
                        claims.getPayload().get(JwtClaim.UNLOCK_AT.key()).toString(),
                        claims.getPayload().get(JwtClaim.NONCE.key()).toString(),
                        claims.getPayload().get(JwtClaim.ALLOWED_ATTEMPTS.key(), Integer.class),
                        claims.getPayload().get(JwtClaim.STUDENT_ATTEMPTS.key(), Integer.class)),
                        HttpStatus.OK);
            } catch (GeneralSecurityException | IOException e) {
                return new ResponseEntity<>(String.format("Error generating token: [%s]", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>("Token passed was not a one time valid token", HttpStatus.UNAUTHORIZED);
    }

    private String extractUserId(String globalId) {
        // Brightspace global IDs are in the format "<global uuid>_<internal userId>"
        String[] parts = StringUtils.split(globalId, '_');

        return parts.length > 1 ? parts[1] : null;
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
        if (ltiCustomClaims.containsKey(JwtClaim.ALLOWED_ATTEMPTS.key())) {
            Integer allowedAttempts = parseInt(ltiCustomClaims.get(JwtClaim.ALLOWED_ATTEMPTS.key()));

            if (allowedAttempts != null) {
                return allowedAttempts;
            } else if (ltiCustomClaims.get(JwtClaim.ALLOWED_ATTEMPTS.key()) == null) {
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
        if (ltiCustomClaims.containsKey(JwtClaim.STUDENT_ATTEMPTS.key())) {
            Integer allowedAttempts = parseInt(ltiCustomClaims.get(JwtClaim.STUDENT_ATTEMPTS.key()));

            if (allowedAttempts != null) {
                return allowedAttempts;
            } else if (ltiCustomClaims.get(JwtClaim.STUDENT_ATTEMPTS.key()) == null) {
                return 0;
            }
        }

        return null;
    }

    private boolean isBearerToken(String rawHeaderValue) {
        return rawHeaderValue.toLowerCase(Locale.US).startsWith(JwtClaim.JWT_BEARER_TYPE.key().toLowerCase(Locale.US));
    }

    @Override
    public boolean isAdmin(SecuredInfo securedInfo) {
        throw new UnsupportedOperationException("Unimplemented method 'isAdmin'");
    }

    @Override
    public boolean isTerracottaAdmin(SecuredInfo securedInfo) {
        throw new UnsupportedOperationException("Unimplemented method 'isTerracottaAdmin'");
    }

    @Override
    public boolean isInstructor(SecuredInfo securedInfo) {
        throw new UnsupportedOperationException("Unimplemented method 'isInstructor'");
    }

    @Override
    public boolean isInstructorOrHigher(SecuredInfo securedInfo) {
        throw new UnsupportedOperationException("Unimplemented method 'isInstructorOrHigher'");
    }

    @Override
    public boolean isLearner(SecuredInfo securedInfo) {
        throw new UnsupportedOperationException("Unimplemented method 'isLearner'");
    }

    @Override
    public boolean isLearnerOrHigher(SecuredInfo securedInfo) {
        throw new UnsupportedOperationException("Unimplemented method 'isLearnerOrHigher'");
    }

    @Override
    public boolean isGeneral(SecuredInfo securedInfo) {
        throw new UnsupportedOperationException("Unimplemented method 'isGeneral'");
    }

    @Override
    public boolean isTestStudent(SecuredInfo securedInfo) {
        throw new UnsupportedOperationException("Unimplemented method 'isTestStudent'");
    }

    @Override
    public Experiment experimentAllowed(SecuredInfo securedInfo, Long experimentId) throws BadTokenException, ExperimentNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'experimentAllowed'");
    }

    @Override
    public boolean experimentLocked(Long experimentId, boolean throwException) throws ExperimentLockedException, ExperimentNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'experimentLocked'");
    }

    @Override
    public boolean conditionsLocked(Long experimentId, boolean throwException) throws ConditionsLockedException, ExperimentNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'conditionsLocked'");
    }

    @Override
    public void conditionAllowed(SecuredInfo securedInfo, Long experimentId, Long conditionId) throws ConditionNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'conditionAllowed'");
    }

    @Override
    public void participantAllowed(SecuredInfo securedInfo, Long experimentId, Long participantId) throws ParticipantNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'participantAllowed'");
    }

    @Override
    public Exposure exposureAllowed(SecuredInfo securedInfo, Long experimentId, Long exposureId) throws ExposureNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'exposureAllowed'");
    }

    @Override
    public void groupAllowed(SecuredInfo securedInfo, Long experimentId, Long groupId) throws GroupNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'groupAllowed'");
    }

    @Override
    public Assignment assignmentAllowed(SecuredInfo securedInfo, long experimentId, long assignmentId) throws AssignmentNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'assignmentAllowed'");
    }

    @Override
    public Assignment assignmentAllowed(SecuredInfo securedInfo, long experimentId, long exposureId, long assignmentId) throws AssignmentNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'assignmentAllowed'");
    }

    @Override
    public void treatmentAllowed(SecuredInfo securedInfo, Long experimentId, Long conditionId, Long treatmentId) throws TreatmentNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'treatmentAllowed'");
    }

    @Override
    public void assessmentAllowed(SecuredInfo securedInfo, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId) throws AssessmentNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'assessmentAllowed'");
    }

    @Override
    public void questionAllowed(SecuredInfo securedInfo, Long assessmentId, Long questionId) throws QuestionNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'questionAllowed'");
    }

    @Override
    public void answerAllowed(SecuredInfo securedInfo, Long assessmentId, Long questionId, String answerType, Long answerId) throws AnswerNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'answerAllowed'");
    }

    @Override
    public void submissionAllowed(SecuredInfo securedInfo, Long assessmentId, Long submissionId) throws SubmissionNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'submissionAllowed'");
    }

    @Override
    public void questionSubmissionAllowed(SecuredInfo securedInfo, Long assessmentId, Long submissionId, Long questionSubmissionId) throws QuestionSubmissionNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'questionSubmissionAllowed'");
    }

    @Override
    public void submissionCommentAllowed(SecuredInfo securedInfo, Long assessmentId, Long submissionId, Long submissionCommentId) throws SubmissionCommentNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'submissionCommentAllowed'");
    }

    @Override
    public void questionSubmissionCommentAllowed(SecuredInfo securedInfo, Long questionSubmissionId, Long questionSubmissionCommentId) throws QuestionSubmissionCommentNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'questionSubmissionCommentAllowed'");
    }

    @Override
    public void outcomeAllowed(SecuredInfo securedInfo, Long experimentId, Long exposureId, Long outcomeId) throws OutcomeNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'outcomeAllowed'");
    }

    @Override
    public void outcomeScoreAllowed(SecuredInfo securedInfo, Long outcomeId, Long outcomeScoreId) throws OutcomeScoreNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'outcomeScoreAllowed'");
    }

    @Override
    public void answerSubmissionAllowed(SecuredInfo securedInfo, Long questionSubmissionId, String answerType, Long answerSubmissionId) throws AnswerSubmissionNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'answerSubmissionAllowed'");
    }

    @Override
    public void integrationAllowed(long questionId, UUID integrationUuid) throws IntegrationOwnerNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'integrationAllowed'");
    }

    @Override
    public ExperimentImport experimentImportAllowed(SecuredInfo securedInfo, UUID uuid) throws ExperimentImportNotFoundException {
        throw new UnsupportedOperationException("Unimplemented method 'experimentImportAllowed'");
    }

    @Override
    public MessageContainer messagingContainerAllowed(SecuredInfo securedInfo, long exposureId, UUID containerUuid)
            throws MessageContainerOwnerNotMatchingException, MessageContainerNotMatchingException, MessageContainerNotFoundException {
        throw new UnsupportedOperationException("Unimplemented method 'messagingContainerAllowed'");
    }

    @Override
    public MessageContainerConfiguration messagingContainerConfigurationAllowed(SecuredInfo securedInfo, UUID containerUuid, UUID configurationId)
            throws MessageContainerOwnerNotMatchingException, MessageContainerNotMatchingException, MessageContainerNotFoundException, MessageContainerConfigurationNotFoundException {
        throw new UnsupportedOperationException("Unimplemented method 'messagingContainerConfigurationAllowed'");
    }

    @Override
    public Message messagingAllowed(SecuredInfo securedInfo, UUID containerUuid, UUID messageUuid) throws MessageOwnerNotMatchingException, MessageNotMatchingException, MessageNotFoundException {
        throw new UnsupportedOperationException("Unimplemented method 'messagingAllowed'");
    }

    @Override
    public MessageContent messagingContentAllowed(SecuredInfo securedInfo, UUID messageUuid, UUID contentUuid) throws MessageContentNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'messagingContentAllowed'");
    }

    @Override
    public MessageConfiguration messagingConfigurationAllowed(SecuredInfo securedInfo, UUID messageUuid, UUID configurationUuid) throws MessageConfigurationNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'messagingConfigurationAllowed'");
    }

    @Override
    public MessageRecipientRuleSet messagingRuleSetAllowed(SecuredInfo securedInfo, UUID messageUuid, UUID messageRuleSetUuid) throws MessageRuleSetNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'messagingRuleSetAllowed'");
    }

    @Override
    public MessageRecipientRule messagingRuleAllowed(SecuredInfo securedInfo, UUID messageUuid, UUID messageRuleSetUuid) throws MessageRuleNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'messagingRuleAllowed'");
    }

    @Override
    public MessageConditionalText messagingConditionalTextAllowed(SecuredInfo securedInfo, UUID contentUuid, UUID conditionalTextUuid)
        throws MessageContentNotMatchingException, MessageNotMatchingException, MessageConditionalTextNotMatchingException {
        throw new UnsupportedOperationException("Unimplemented method 'messagingConditionalTextAllowed'");
    }

}
