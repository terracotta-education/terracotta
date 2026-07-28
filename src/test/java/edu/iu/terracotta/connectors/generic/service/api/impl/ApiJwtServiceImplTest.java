package edu.iu.terracotta.connectors.generic.service.api.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lti.Roles;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
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
import edu.iu.terracotta.dao.model.enums.ExposureTypes;
import edu.iu.terracotta.dao.repository.SubmissionCommentRepository;
import edu.iu.terracotta.dao.repository.messaging.conditional.MessageConditionalTextRepository;
import edu.iu.terracotta.dao.repository.messaging.container.MessageContainerConfigurationRepository;
import edu.iu.terracotta.dao.repository.messaging.container.MessageContainerRepository;
import edu.iu.terracotta.dao.repository.messaging.content.MessageContentRepository;
import edu.iu.terracotta.dao.repository.messaging.message.MessageConfigurationRepository;
import edu.iu.terracotta.dao.repository.messaging.message.MessageRepository;
import edu.iu.terracotta.dao.repository.messaging.recipient.MessageRecipientRuleRepository;
import edu.iu.terracotta.dao.repository.messaging.recipient.MessageRecipientRuleSetRepository;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.ConditionsLockedException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.messaging.MessageConditionalTextNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageConfigurationNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerConfigurationNotFoundException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerOwnerNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageContentNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageRuleNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageRuleSetNotMatchingException;
import edu.iu.terracotta.service.app.AdminService;
import edu.iu.terracotta.utils.TextConstants;
import edu.iu.terracotta.utils.oauth.OAuthUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;

/**
 * NOTE on local @Mock fields below: MessageConditionalTextRepository, MessageConfigurationRepository,
 * MessageContainerRepository, MessageContainerConfigurationRepository, MessageContentRepository,
 * MessageRepository, MessageRecipientRuleRepository, MessageRecipientRuleSetRepository,
 * SubmissionCommentRepository and AdminService (plus the entities they return) are NOT declared
 * anywhere in the BaseModelTest/BaseRepositoryTest/BaseServiceTest hierarchy, so they must be
 * declared here and injected manually via the constructor.
 *
 * ApiJwtServiceImpl is constructed manually (not via @InjectMocks) because BaseServiceTest already
 * declares an "apiJwtService" mock of the *interface* type (used here as the delegate returned by
 * the mocked ConnectorService for all of the instance(...)-based pass-through methods), and also a
 * "canvasApiJwtService" mock of the concrete CanvasApiJwtServiceImpl type - both are documented
 * ambiguous-mock hazards for @InjectMocks. Naming the field under test "apiJwtServiceImpl" (instead
 * of "apiJwtService") avoids shadowing the inherited interface mock.
 */
@SuppressWarnings({"unchecked", "PMD.LooseCoupling"})
public class ApiJwtServiceImplTest extends BaseTest {

    private static final String ISSUER_LMS_OAUTH_API_TOKEN_REQUEST = "lmsOAuthAPITokenRequest";

    private static String testPrivateKeyPem;
    private static String testPublicKeyPem;

    @Mock private MessageConditionalTextRepository messageConditionalTextRepository;
    @Mock private MessageConfigurationRepository messageConfigurationRepository;
    @Mock private MessageContainerRepository messageContainerRepository;
    @Mock private MessageContainerConfigurationRepository messageContainerConfigurationRepository;
    @Mock private MessageContentRepository messageContentRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private MessageRecipientRuleRepository messageRuleRepository;
    @Mock private MessageRecipientRuleSetRepository messageRuleSetRepository;
    @Mock private SubmissionCommentRepository submissionCommentRepository;
    @Mock private AdminService adminService;

    @Mock private MessageContainer messageContainer;
    @Mock private MessageContainerConfiguration messageContainerConfiguration;
    @Mock private Message message;
    @Mock private MessageContent messageContent;
    @Mock private MessageConfiguration messageConfiguration;
    @Mock private MessageRecipientRuleSet messageRecipientRuleSet;
    @Mock private MessageRecipientRule messageRecipientRule;
    @Mock private MessageConditionalText messageConditionalText;

    private ApiJwtServiceImpl apiJwtServiceImpl;

    @BeforeAll
    public static void generateTestKeyPair() throws GeneralSecurityException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        testPrivateKeyPem = "-----BEGIN PRIVATE KEY-----"
            + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
            + "-----END PRIVATE KEY-----";
        testPublicKeyPem = "-----BEGIN PUBLIC KEY-----"
            + Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())
            + "-----END PUBLIC KEY-----";
    }

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        apiJwtServiceImpl = new ApiJwtServiceImpl(
            answerEssaySubmissionRepository,
            answerMcRepository,
            answerMcSubmissionRepository,
            assessmentRepository,
            assignmentRepository,
            conditionRepository,
            experimentImportRepository,
            experimentRepository,
            exposureRepository,
            groupRepository,
            integrationRepository,
            messageConditionalTextRepository,
            messageConfigurationRepository,
            messageContainerRepository,
            messageContainerConfigurationRepository,
            messageContentRepository,
            messageRepository,
            messageRuleRepository,
            messageRuleSetRepository,
            outcomeRepository,
            outcomeScoreRepository,
            participantRepository,
            questionRepository,
            questionSubmissionCommentRepository,
            questionSubmissionRepository,
            submissionCommentRepository,
            submissionRepository,
            treatmentRepository,
            adminService,
            apiJwtConnectorService,
            ltiDataService
        );

        when(ltiDataService.getOwnPrivateKey()).thenReturn(testPrivateKeyPem);
        when(ltiDataService.getOwnPublicKey()).thenReturn(testPublicKeyPem);
    }

    // helper: builds a real RS256-signed JWT using the generated test key pair
    private String buildSignedToken(String issuer, Map<String, Object> claims, Date expiration) throws GeneralSecurityException {
        Date now = new Date();
        var builder = Jwts.builder()
            .issuer(issuer)
            .subject("test-subject")
            .issuedAt(now)
            .expiration(expiration);

        if (claims != null) {
            claims.forEach(builder::claim);
        }

        return builder
            .signWith(OAuthUtils.loadPrivateKey(testPrivateKeyPem), SIG.RS256)
            .compact();
    }

    private String buildValidToken(Map<String, Object> claims) throws GeneralSecurityException {
        return buildSignedToken("testIssuer", claims, new Date(System.currentTimeMillis() + 60_000L));
    }

    private String buildExpiredToken(Map<String, Object> claims) throws GeneralSecurityException {
        return buildSignedToken("testIssuer", claims, new Date(System.currentTimeMillis() - 60_000L));
    }

    /* ***************** validateToken ***************** */

    @Test
    public void testValidateTokenValid() throws GeneralSecurityException {
        String token = buildValidToken(Map.of("someClaim", "someValue"));

        Jws<Claims> result = apiJwtServiceImpl.validateToken(token);

        assertNotNull(result);
        assertEquals("someValue", result.getPayload().get("someClaim"));
    }

    @Test
    public void testValidateTokenExpired() throws GeneralSecurityException {
        String token = buildExpiredToken(Map.of("someClaim", "someValue"));

        assertNull(apiJwtServiceImpl.validateToken(token));
    }

    @Test
    public void testValidateTokenBadPublicKeyThrows() throws GeneralSecurityException {
        // valid base64 (no PEM markers needed - OAuthUtils.loadPublicKey strips them unconditionally),
        // but not a well-formed X.509 SubjectPublicKeyInfo, so key parsing fails with a
        // GeneralSecurityException that the Locator swallows (logs + returns a null Key), which in
        // turn makes the underlying JJWT signature verification fail with a JwtException.
        when(ltiDataService.getOwnPublicKey()).thenReturn("aGVsbG93b3JsZA==");
        String token = buildValidToken(Map.of("someClaim", "someValue"));

        assertThrows(JwtException.class, () -> apiJwtServiceImpl.validateToken(token));
    }

    /* ***************** validateFileToken ***************** */

    @Test
    public void testValidateFileTokenMatches() throws GeneralSecurityException {
        String token = apiJwtServiceImpl.buildFileToken("file123", "http://localhost");

        assertTrue(apiJwtServiceImpl.validateFileToken(token, "file123"));
    }

    @Test
    public void testValidateFileTokenDoesNotMatch() throws GeneralSecurityException {
        String token = apiJwtServiceImpl.buildFileToken("file123", "http://localhost");

        assertFalse(apiJwtServiceImpl.validateFileToken(token, "otherFile"));
    }

    @Test
    public void testValidateFileTokenNullClaims() throws GeneralSecurityException {
        String token = buildExpiredToken(Map.of("fileId", "file123"));

        assertFalse(apiJwtServiceImpl.validateFileToken(token, "file123"));
    }

    /* ***************** unsecureToken ***************** */

    @Test
    public void testUnsecureTokenDelegates() throws TerracottaConnectorException {
        when(apiJwtConnectorService.instance(eq(platformDeployment), eq(ApiJwtService.class))).thenReturn(apiJwtService);
        Map<String, Object> claims = new HashMap<>();
        claims.put("k", "v");
        when(apiJwtService.unsecureToken("tok", platformDeployment)).thenReturn(claims);

        Map<String, Object> result = apiJwtServiceImpl.unsecureToken("tok", platformDeployment);

        assertSame(claims, result);
    }

    @Test
    public void testUnsecureTokenPropagatesConnectorException() throws TerracottaConnectorException {
        when(apiJwtConnectorService.instance(eq(platformDeployment), eq(ApiJwtService.class))).thenThrow(new TerracottaConnectorException("boom"));

        assertThrows(TerracottaConnectorException.class, () -> apiJwtServiceImpl.unsecureToken("tok", platformDeployment));
    }

    /* ***************** buildJwt(long, String, Claims) ***************** */

    @Test
    public void testBuildJwtByPlatformDeploymentIdDelegates() throws Exception {
        when(apiJwtConnectorService.instance(eq(10L), eq(ApiJwtService.class))).thenReturn(apiJwtService);
        Claims claims = mock(Claims.class);
        when(apiJwtService.buildJwt(10L, "userKey", claims)).thenReturn("built-jwt");

        String result = apiJwtServiceImpl.buildJwt(10L, "userKey", claims);

        assertEquals("built-jwt", result);
    }

    /* ***************** buildJwt(20-arg overload) ***************** */

    @Test
    public void testBuildJwtLongFormDelegates() throws Exception {
        List<String> roles = List.of(Roles.MEMBERSHIP_LEARNER);
        when(apiJwtConnectorService.instance(eq(20L), eq(ApiJwtService.class))).thenReturn(apiJwtService);
        when(
            apiJwtService.buildJwt(
                true, roles, 1L, 20L, "user1", 2L, 3L, true, "lmsUser1", "lmsGlobal1",
                "login1", "lmsUserName1", "course1", "lmsAssign1", "due", "lock", "unlock",
                "nonce1", 5, 2
            )
        ).thenReturn("big-jwt");

        String result = apiJwtServiceImpl.buildJwt(
            true, roles, 1L, 20L, "user1", 2L, 3L, true, "lmsUser1", "lmsGlobal1",
            "login1", "lmsUserName1", "course1", "lmsAssign1", "due", "lock", "unlock",
            "nonce1", 5, 2
        );

        assertEquals("big-jwt", result);
    }

    /* ***************** buildJwt(oneUse, Lti3Request) ***************** */

    @Test
    public void testBuildJwtWithLti3RequestDelegates() throws Exception {
        when(lti3Request.getToolDeployment()).thenReturn(toolDeployment);
        when(toolDeployment.getPlatformDeployment()).thenReturn(platformDeployment);
        when(apiJwtConnectorService.instance(eq(platformDeployment), eq(ApiJwtService.class))).thenReturn(apiJwtService);
        when(apiJwtService.buildJwt(true, lti3Request)).thenReturn("jwt-from-lti3request");

        String result = apiJwtServiceImpl.buildJwt(true, lti3Request);

        assertEquals("jwt-from-lti3request", result);
    }

    /* ***************** generateStateForAPITokenRequest ***************** */

    @Test
    public void testGenerateStateForAPITokenRequestDelegates() throws Exception {
        when(lti3Request.getToolDeployment()).thenReturn(toolDeployment);
        when(toolDeployment.getPlatformDeployment()).thenReturn(platformDeployment);
        when(apiJwtConnectorService.instance(eq(platformDeployment), eq(ApiJwtService.class))).thenReturn(apiJwtService);
        when(apiJwtService.generateStateForAPITokenRequest(lti3Request)).thenReturn("state-jwt");

        String result = apiJwtServiceImpl.generateStateForAPITokenRequest(lti3Request);

        assertEquals("state-jwt", result);
    }

    /* ***************** validateStateForAPITokenRequest ***************** */

    @Test
    public void testValidateStateForAPITokenRequestExpiredReturnsEmpty() throws GeneralSecurityException {
        String state = buildExpiredToken(null);

        assertEquals(Optional.empty(), apiJwtServiceImpl.validateStateForAPITokenRequest(state));
    }

    @Test
    public void testValidateStateForAPITokenRequestWrongIssuerReturnsEmpty() throws GeneralSecurityException {
        String state = buildValidToken(null);

        assertEquals(Optional.empty(), apiJwtServiceImpl.validateStateForAPITokenRequest(state));
    }

    @Test
    public void testValidateStateForAPITokenRequestMatchingIssuerReturnsClaims() throws GeneralSecurityException {
        String state = buildSignedToken(ISSUER_LMS_OAUTH_API_TOKEN_REQUEST, null, new Date(System.currentTimeMillis() + 60_000L));

        Optional<Jws<Claims>> result = apiJwtServiceImpl.validateStateForAPITokenRequest(state);

        assertTrue(result.isPresent());
        assertEquals(ISSUER_LMS_OAUTH_API_TOKEN_REQUEST, result.get().getPayload().getIssuer());
    }

    /* ***************** buildFileToken ***************** */

    @Test
    public void testBuildFileTokenBuildsValidToken() throws GeneralSecurityException {
        String token = apiJwtServiceImpl.buildFileToken("file123", "http://localhost");

        assertNotNull(token);

        Jws<Claims> claims = apiJwtServiceImpl.validateToken(token);
        assertNotNull(claims);
        assertEquals("file123", claims.getPayload().get("fileId"));
        assertEquals(ApiJwtService.ISSUER_TERRACOTTA_API, claims.getPayload().getIssuer());
        assertEquals("no_user", claims.getPayload().getSubject());
    }

    @Test
    public void testBuildFileTokenBadPrivateKeyThrows() {
        when(ltiDataService.getOwnPrivateKey()).thenReturn("not-a-valid-key");

        assertThrows(GeneralSecurityException.class, () -> apiJwtServiceImpl.buildFileToken("file123", "http://localhost"));
    }

    /* ***************** refreshToken ***************** */

    @Test
    public void testRefreshTokenNullClaimsReturnsNull() throws Exception {
        String token = buildExpiredToken(Map.of("platformDeploymentId", 55L));

        assertNull(apiJwtServiceImpl.refreshToken(token));
    }

    @Test
    public void testRefreshTokenDelegates() throws Exception {
        String token = buildValidToken(Map.of("platformDeploymentId", 55L));
        when(apiJwtConnectorService.instance(eq(55L), eq(ApiJwtService.class))).thenReturn(apiJwtService);
        when(apiJwtService.refreshToken(token)).thenReturn("refreshed-jwt");

        String result = apiJwtServiceImpl.refreshToken(token);

        assertEquals("refreshed-jwt", result);
    }

    /* ***************** getTimedToken(HttpServletRequest) ***************** */

    @Test
    public void testGetTimedTokenFromRequestDelegates() throws Exception {
        String token = buildValidToken(Map.of("platformDeploymentId", 66L));
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(apiJwtConnectorService.instance(eq(66L), eq(ApiJwtService.class))).thenReturn(apiJwtService);
        ResponseEntity<String> response = ResponseEntity.ok("timed-token");
        when(apiJwtService.getTimedToken(token)).thenReturn(response);

        ResponseEntity<String> result = apiJwtServiceImpl.getTimedToken(httpServletRequest);

        assertSame(response, result);
    }

    /* ***************** getTimedToken(String) ***************** */

    @Test
    public void testGetTimedTokenStringNullClaimsReturnsNull() throws Exception {
        String token = buildExpiredToken(Map.of("platformDeploymentId", 66L));

        assertNull(apiJwtServiceImpl.getTimedToken(token));
    }

    @Test
    public void testGetTimedTokenStringDelegates() throws Exception {
        String token = buildValidToken(Map.of("platformDeploymentId", 66L));
        when(apiJwtConnectorService.instance(eq(66L), eq(ApiJwtService.class))).thenReturn(apiJwtService);
        ResponseEntity<String> response = ResponseEntity.ok("timed-token");
        when(apiJwtService.getTimedToken(token)).thenReturn(response);

        ResponseEntity<String> result = apiJwtServiceImpl.getTimedToken(token);

        assertSame(response, result);
    }

    /* ***************** extractJwtStringValue ***************** */

    @Test
    public void testExtractJwtStringValueFromBearerHeader() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer abc123");

        assertEquals("abc123", apiJwtServiceImpl.extractJwtStringValue(httpServletRequest, true));
    }

    @Test
    public void testExtractJwtStringValueFromLowercaseBearerHeader() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn("bearer abc123");

        assertEquals("abc123", apiJwtServiceImpl.extractJwtStringValue(httpServletRequest, true));
    }

    @Test
    public void testExtractJwtStringValueNonBearerHeaderReturnsNull() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Basic abc123");

        assertNull(apiJwtServiceImpl.extractJwtStringValue(httpServletRequest, true));
    }

    @Test
    public void testExtractJwtStringValueFallsBackToQueryParam() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
        when(httpServletRequest.getParameter("token")).thenReturn("query-token");

        assertEquals("query-token", apiJwtServiceImpl.extractJwtStringValue(httpServletRequest, true));
    }

    @Test
    public void testExtractJwtStringValueNoHeaderQueryParamDisallowedReturnsNull() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        assertNull(apiJwtServiceImpl.extractJwtStringValue(httpServletRequest, false));
        verify(httpServletRequest, never()).getParameter(any());
    }

    @Test
    public void testExtractJwtStringValueNoHeaderNoQueryParamReturnsNull() {
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);
        when(httpServletRequest.getParameter("token")).thenReturn(null);

        assertNull(apiJwtServiceImpl.extractJwtStringValue(httpServletRequest, true));
    }

    /* ***************** extractValues(HttpServletRequest, boolean) ***************** */

    @Test
    public void testExtractValuesFromRequestDelegates() throws Exception {
        String token = buildValidToken(Map.of("platformDeploymentId", 77L));
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(apiJwtConnectorService.instance(eq(77L), eq(ApiJwtService.class))).thenReturn(apiJwtService);
        when(apiJwtService.extractValues(any(Jws.class))).thenReturn(securedInfo);

        SecuredInfo result = apiJwtServiceImpl.extractValues(httpServletRequest, true);

        assertSame(securedInfo, result);
    }

    /* ***************** extractValues(String) ***************** */

    @Test
    public void testExtractValuesBlankTokenReturnsNull() throws Exception {
        assertNull(apiJwtServiceImpl.extractValues(""));
        assertNull(apiJwtServiceImpl.extractValues((String) null));
    }

    @Test
    public void testExtractValuesNullClaimsReturnsNull() throws Exception {
        String token = buildExpiredToken(Map.of("platformDeploymentId", 77L));

        assertNull(apiJwtServiceImpl.extractValues(token));
    }

    @Test
    public void testExtractValuesDelegates() throws Exception {
        String token = buildValidToken(Map.of("platformDeploymentId", 77L));
        when(apiJwtConnectorService.instance(eq(77L), eq(ApiJwtService.class))).thenReturn(apiJwtService);
        when(apiJwtService.extractValues(any(Jws.class))).thenReturn(securedInfo);

        SecuredInfo result = apiJwtServiceImpl.extractValues(token);

        assertSame(securedInfo, result);
    }

    /* ***************** role checks ***************** */

    @Test
    public void testIsAdmin() {
        when(securedInfo.getRoles()).thenReturn(List.of(Roles.ADMIN));
        assertTrue(apiJwtServiceImpl.isAdmin(securedInfo));

        when(securedInfo.getRoles()).thenReturn(List.of(Roles.GENERAL));
        assertFalse(apiJwtServiceImpl.isAdmin(securedInfo));

        assertFalse(apiJwtServiceImpl.isAdmin(null));
    }

    @Test
    public void testIsTerracottaAdmin() {
        when(adminService.isTerracottaAdmin(USER_ID)).thenReturn(true);
        assertTrue(apiJwtServiceImpl.isTerracottaAdmin(securedInfo));

        when(adminService.isTerracottaAdmin(USER_ID)).thenReturn(false);
        assertFalse(apiJwtServiceImpl.isTerracottaAdmin(securedInfo));

        assertFalse(apiJwtServiceImpl.isTerracottaAdmin(null));
        verify(adminService, never()).isTerracottaAdmin(null);
    }

    @Test
    public void testIsInstructor() {
        when(securedInfo.getRoles()).thenReturn(List.of(Roles.MEMBERSHIP_INSTRUCTOR));
        assertTrue(apiJwtServiceImpl.isInstructor(securedInfo));

        when(securedInfo.getRoles()).thenReturn(List.of(Roles.GENERAL));
        assertFalse(apiJwtServiceImpl.isInstructor(securedInfo));
    }

    @Test
    public void testIsLearner() {
        when(securedInfo.getRoles()).thenReturn(List.of(Roles.MEMBERSHIP_LEARNER));
        assertTrue(apiJwtServiceImpl.isLearner(securedInfo));

        when(securedInfo.getRoles()).thenReturn(List.of(Roles.GENERAL));
        assertFalse(apiJwtServiceImpl.isLearner(securedInfo));
    }

    @Test
    public void testIsGeneral() {
        when(securedInfo.getRoles()).thenReturn(List.of(Roles.GENERAL));
        assertTrue(apiJwtServiceImpl.isGeneral(securedInfo));

        when(securedInfo.getRoles()).thenReturn(List.of(Roles.MEMBERSHIP_LEARNER));
        assertFalse(apiJwtServiceImpl.isGeneral(securedInfo));
    }

    @Test
    public void testIsTestStudent() {
        when(securedInfo.getRoles()).thenReturn(List.of(Roles.TEST_STUDENT));
        assertTrue(apiJwtServiceImpl.isTestStudent(securedInfo));

        when(securedInfo.getRoles()).thenReturn(List.of(Roles.MEMBERSHIP_LEARNER));
        assertFalse(apiJwtServiceImpl.isTestStudent(securedInfo));
    }

    @Test
    public void testIsInstructorOrHigher() {
        when(securedInfo.getRoles()).thenReturn(List.of(Roles.ADMIN));
        assertTrue(apiJwtServiceImpl.isInstructorOrHigher(securedInfo));

        when(securedInfo.getRoles()).thenReturn(List.of(Roles.MEMBERSHIP_INSTRUCTOR));
        assertTrue(apiJwtServiceImpl.isInstructorOrHigher(securedInfo));

        when(securedInfo.getRoles()).thenReturn(List.of(Roles.MEMBERSHIP_LEARNER));
        assertFalse(apiJwtServiceImpl.isInstructorOrHigher(securedInfo));
    }

    @Test
    public void testIsLearnerOrHigher() {
        when(securedInfo.getRoles()).thenReturn(List.of(Roles.MEMBERSHIP_LEARNER));
        assertTrue(apiJwtServiceImpl.isLearnerOrHigher(securedInfo));

        when(securedInfo.getRoles()).thenReturn(List.of(Roles.ADMIN));
        assertTrue(apiJwtServiceImpl.isLearnerOrHigher(securedInfo));

        when(securedInfo.getRoles()).thenReturn(List.of(Roles.GENERAL));
        assertFalse(apiJwtServiceImpl.isLearnerOrHigher(securedInfo));
    }

    /* ***************** experimentAllowed ***************** */

    @Test
    public void testExperimentAllowedNullSecuredInfoThrows() {
        assertThrows(BadTokenException.class, () -> apiJwtServiceImpl.experimentAllowed(null, 5L));
        verify(experimentRepository, never()).findByExperimentIdAndPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(anyLong(), anyLong(), anyLong());
    }

    @Test
    public void testExperimentAllowedFound() throws Exception {
        when(experimentRepository.findByExperimentIdAndPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(5L, 1L, 1L)).thenReturn(Optional.of(experiment));

        Experiment result = apiJwtServiceImpl.experimentAllowed(securedInfo, 5L);

        assertSame(experiment, result);
    }

    @Test
    public void testExperimentAllowedNotFoundThrows() {
        when(experimentRepository.findByExperimentIdAndPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(5L, 1L, 1L)).thenReturn(Optional.empty());

        assertThrows(ExperimentNotMatchingException.class, () -> apiJwtServiceImpl.experimentAllowed(securedInfo, 5L));
    }

    /* ***************** experimentLocked ***************** */

    @Test
    public void testExperimentLockedNotFoundThrows() {
        when(experimentRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ExperimentNotMatchingException.class, () -> apiJwtServiceImpl.experimentLocked(5L, true));
    }

    @Test
    public void testExperimentLockedNotStartedReturnsFalse() throws Exception {
        when(experiment.isStarted()).thenReturn(false);

        assertFalse(apiJwtServiceImpl.experimentLocked(5L, true));
    }

    @Test
    public void testExperimentLockedStartedNoThrowReturnsTrue() throws Exception {
        when(experiment.isStarted()).thenReturn(true);

        assertTrue(apiJwtServiceImpl.experimentLocked(5L, false));
    }

    @Test
    public void testExperimentLockedStartedThrows() {
        when(experiment.isStarted()).thenReturn(true);

        assertThrows(ExperimentLockedException.class, () -> apiJwtServiceImpl.experimentLocked(5L, true));
    }

    /* ***************** conditionsLocked ***************** */

    @Test
    public void testConditionsLockedNotFoundThrows() {
        when(experimentRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ExperimentNotMatchingException.class, () -> apiJwtServiceImpl.conditionsLocked(5L, true));
    }

    @Test
    public void testConditionsLockedNoSetReturnsFalse() throws Exception {
        when(experiment.getExposureType()).thenReturn(ExposureTypes.NOSET);

        assertFalse(apiJwtServiceImpl.conditionsLocked(5L, true));
    }

    @Test
    public void testConditionsLockedSetNoThrowReturnsTrue() throws Exception {
        when(experiment.getExposureType()).thenReturn(ExposureTypes.BETWEEN);

        assertTrue(apiJwtServiceImpl.conditionsLocked(5L, false));
    }

    @Test
    public void testConditionsLockedSetThrows() {
        when(experiment.getExposureType()).thenReturn(ExposureTypes.BETWEEN);

        assertThrows(ConditionsLockedException.class, () -> apiJwtServiceImpl.conditionsLocked(5L, true));
    }

    /* ***************** conditionAllowed ***************** */

    @Test
    public void testConditionAllowedExists() {
        when(conditionRepository.existsByExperiment_ExperimentIdAndConditionId(5L, 6L)).thenReturn(true);

        assertDoesNotThrow(() -> apiJwtServiceImpl.conditionAllowed(null, 5L, 6L));
    }

    @Test
    public void testConditionAllowedNotExistsThrows() {
        when(conditionRepository.existsByExperiment_ExperimentIdAndConditionId(5L, 6L)).thenReturn(false);

        assertThrows(ConditionNotMatchingException.class, () -> apiJwtServiceImpl.conditionAllowed(null, 5L, 6L));
    }

    /* ***************** participantAllowed ***************** */

    @Test
    public void testParticipantAllowedExists() {
        when(participantRepository.existsByExperiment_ExperimentIdAndId(5L, 9L)).thenReturn(true);

        assertDoesNotThrow(() -> apiJwtServiceImpl.participantAllowed(null, 5L, 9L));
    }

    @Test
    public void testParticipantAllowedNotExistsThrows() {
        when(participantRepository.existsByExperiment_ExperimentIdAndId(5L, 9L)).thenReturn(false);

        assertThrows(ParticipantNotMatchingException.class, () -> apiJwtServiceImpl.participantAllowed(null, 5L, 9L));
    }

    /* ***************** exposureAllowed ***************** */

    @Test
    public void testExposureAllowedFound() throws Exception {
        when(exposureRepository.findByExperiment_ExperimentIdAndExposureId(5L, 8L)).thenReturn(Optional.of(exposure));

        Exposure result = apiJwtServiceImpl.exposureAllowed(null, 5L, 8L);

        assertSame(exposure, result);
    }

    @Test
    public void testExposureAllowedNotFoundThrows() {
        when(exposureRepository.findByExperiment_ExperimentIdAndExposureId(5L, 8L)).thenReturn(Optional.empty());

        assertThrows(ExposureNotMatchingException.class, () -> apiJwtServiceImpl.exposureAllowed(null, 5L, 8L));
    }

    /* ***************** groupAllowed ***************** */

    @Test
    public void testGroupAllowedExists() {
        when(groupRepository.existsByExperiment_ExperimentIdAndGroupId(5L, 4L)).thenReturn(true);

        assertDoesNotThrow(() -> apiJwtServiceImpl.groupAllowed(null, 5L, 4L));
    }

    @Test
    public void testGroupAllowedNotExistsThrows() {
        when(groupRepository.existsByExperiment_ExperimentIdAndGroupId(5L, 4L)).thenReturn(false);

        assertThrows(GroupNotMatchingException.class, () -> apiJwtServiceImpl.groupAllowed(null, 5L, 4L));
    }

    /* ***************** assignmentAllowed(3-arg) ***************** */

    @Test
    public void testAssignmentAllowedThreeArgFound() throws Exception {
        when(assignmentRepository.findByExposure_Experiment_ExperimentIdAndAssignmentId(5L, 3L)).thenReturn(Optional.of(assignment));

        Assignment result = apiJwtServiceImpl.assignmentAllowed(null, 5L, 3L);

        assertSame(assignment, result);
    }

    @Test
    public void testAssignmentAllowedThreeArgNotFoundThrows() {
        when(assignmentRepository.findByExposure_Experiment_ExperimentIdAndAssignmentId(5L, 3L)).thenReturn(Optional.empty());

        assertThrows(AssignmentNotMatchingException.class, () -> apiJwtServiceImpl.assignmentAllowed(null, 5L, 3L));
    }

    /* ***************** assignmentAllowed(4-arg) ***************** */

    @Test
    public void testAssignmentAllowedFourArgFound() throws Exception {
        when(assignmentRepository.findByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndAssignmentId(5L, 8L, 3L)).thenReturn(Optional.of(assignment));

        Assignment result = apiJwtServiceImpl.assignmentAllowed(null, 5L, 8L, 3L);

        assertSame(assignment, result);
    }

    @Test
    public void testAssignmentAllowedFourArgNotFoundThrows() {
        when(assignmentRepository.findByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndAssignmentId(5L, 8L, 3L)).thenReturn(Optional.empty());

        assertThrows(AssignmentNotMatchingException.class, () -> apiJwtServiceImpl.assignmentAllowed(null, 5L, 8L, 3L));
    }

    /* ***************** treatmentAllowed ***************** */

    @Test
    public void testTreatmentAllowedExists() {
        when(treatmentRepository.existsByCondition_Experiment_ExperimentIdAndCondition_ConditionIdAndTreatmentId(5L, 6L, 7L)).thenReturn(true);

        assertDoesNotThrow(() -> apiJwtServiceImpl.treatmentAllowed(null, 5L, 6L, 7L));
    }

    @Test
    public void testTreatmentAllowedNotExistsThrows() {
        when(treatmentRepository.existsByCondition_Experiment_ExperimentIdAndCondition_ConditionIdAndTreatmentId(5L, 6L, 7L)).thenReturn(false);

        assertThrows(TreatmentNotMatchingException.class, () -> apiJwtServiceImpl.treatmentAllowed(null, 5L, 6L, 7L));
    }

    /* ***************** assessmentAllowed ***************** */

    @Test
    public void testAssessmentAllowedExists() {
        when(assessmentRepository.existsByTreatment_Condition_Experiment_ExperimentIdAndTreatment_Condition_ConditionIdAndTreatment_TreatmentIdAndAssessmentId(5L, 6L, 7L, 9L)).thenReturn(true);

        assertDoesNotThrow(() -> apiJwtServiceImpl.assessmentAllowed(null, 5L, 6L, 7L, 9L));
    }

    @Test
    public void testAssessmentAllowedNotExistsThrows() {
        when(assessmentRepository.existsByTreatment_Condition_Experiment_ExperimentIdAndTreatment_Condition_ConditionIdAndTreatment_TreatmentIdAndAssessmentId(5L, 6L, 7L, 9L)).thenReturn(false);

        assertThrows(AssessmentNotMatchingException.class, () -> apiJwtServiceImpl.assessmentAllowed(null, 5L, 6L, 7L, 9L));
    }

    /* ***************** questionAllowed ***************** */

    @Test
    public void testQuestionAllowedExists() {
        when(questionRepository.existsByAssessment_AssessmentIdAndQuestionId(9L, 11L)).thenReturn(true);

        assertDoesNotThrow(() -> apiJwtServiceImpl.questionAllowed(null, 9L, 11L));
    }

    @Test
    public void testQuestionAllowedNotExistsThrows() {
        when(questionRepository.existsByAssessment_AssessmentIdAndQuestionId(9L, 11L)).thenReturn(false);

        assertThrows(QuestionNotMatchingException.class, () -> apiJwtServiceImpl.questionAllowed(null, 9L, 11L));
    }

    /* ***************** answerAllowed ***************** */

    @Test
    public void testAnswerAllowedMcExists() {
        when(answerMcRepository.existsByQuestion_Assessment_AssessmentIdAndQuestion_QuestionIdAndAnswerMcId(9L, 11L, 12L)).thenReturn(true);

        assertDoesNotThrow(() -> apiJwtServiceImpl.answerAllowed(null, 9L, 11L, "MC", 12L));
    }

    @Test
    public void testAnswerAllowedMcNotExistsThrows() {
        when(answerMcRepository.existsByQuestion_Assessment_AssessmentIdAndQuestion_QuestionIdAndAnswerMcId(9L, 11L, 12L)).thenReturn(false);

        assertThrows(AnswerNotMatchingException.class, () -> apiJwtServiceImpl.answerAllowed(null, 9L, 11L, "MC", 12L));
    }

    @Test
    public void testAnswerAllowedUnknownTypeNeverChecksAndNeverThrows() {
        // NOTE: only "MC" answer types are validated; any other answerType (including null) silently
        // passes without ever consulting a repository. Documented in the source as "as more answer
        // types are added, continue checking" but currently a gap for any non-MC type.
        assertDoesNotThrow(() -> apiJwtServiceImpl.answerAllowed(null, 9L, 11L, "ESSAY", 12L));

        verify(answerMcRepository, never()).existsByQuestion_Assessment_AssessmentIdAndQuestion_QuestionIdAndAnswerMcId(any(), any(), any());
    }

    /* ***************** answerSubmissionAllowed ***************** */

    @Test
    public void testAnswerSubmissionAllowedMcExists() {
        when(answerMcSubmissionRepository.existsByQuestionSubmission_QuestionSubmissionIdAndAnswerMcSubId(11L, 12L)).thenReturn(true);

        assertDoesNotThrow(() -> apiJwtServiceImpl.answerSubmissionAllowed(null, 11L, "MC", 12L));
    }

    @Test
    public void testAnswerSubmissionAllowedMcNotExistsThrows() {
        when(answerMcSubmissionRepository.existsByQuestionSubmission_QuestionSubmissionIdAndAnswerMcSubId(11L, 12L)).thenReturn(false);

        assertThrows(AnswerSubmissionNotMatchingException.class, () -> apiJwtServiceImpl.answerSubmissionAllowed(null, 11L, "MC", 12L));
    }

    @Test
    public void testAnswerSubmissionAllowedEssayExists() {
        when(answerEssaySubmissionRepository.existsByQuestionSubmission_QuestionSubmissionIdAndAnswerEssaySubmissionId(11L, 12L)).thenReturn(true);

        assertDoesNotThrow(() -> apiJwtServiceImpl.answerSubmissionAllowed(null, 11L, "ESSAY", 12L));
    }

    @Test
    public void testAnswerSubmissionAllowedEssayNotExistsThrows() {
        when(answerEssaySubmissionRepository.existsByQuestionSubmission_QuestionSubmissionIdAndAnswerEssaySubmissionId(11L, 12L)).thenReturn(false);

        assertThrows(AnswerSubmissionNotMatchingException.class, () -> apiJwtServiceImpl.answerSubmissionAllowed(null, 11L, "ESSAY", 12L));
    }

    @Test
    public void testAnswerSubmissionAllowedUnknownTypeNeverChecksAndNeverThrows() {
        assertDoesNotThrow(() -> apiJwtServiceImpl.answerSubmissionAllowed(null, 11L, "OTHER", 12L));

        verify(answerMcSubmissionRepository, never()).existsByQuestionSubmission_QuestionSubmissionIdAndAnswerMcSubId(any(), any());
        verify(answerEssaySubmissionRepository, never()).existsByQuestionSubmission_QuestionSubmissionIdAndAnswerEssaySubmissionId(any(), any());
    }

    /* ***************** submissionAllowed ***************** */

    @Test
    public void testSubmissionAllowedExists() {
        when(submissionRepository.existsByAssessment_AssessmentIdAndSubmissionId(9L, 20L)).thenReturn(true);

        assertDoesNotThrow(() -> apiJwtServiceImpl.submissionAllowed(null, 9L, 20L));
    }

    @Test
    public void testSubmissionAllowedNotExistsThrows() {
        when(submissionRepository.existsByAssessment_AssessmentIdAndSubmissionId(9L, 20L)).thenReturn(false);

        assertThrows(SubmissionNotMatchingException.class, () -> apiJwtServiceImpl.submissionAllowed(null, 9L, 20L));
    }

    /* ***************** questionSubmissionAllowed ***************** */

    @Test
    public void testQuestionSubmissionAllowedExists() {
        when(questionSubmissionRepository.existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndQuestionSubmissionId(9L, 20L, 21L)).thenReturn(true);

        assertDoesNotThrow(() -> apiJwtServiceImpl.questionSubmissionAllowed(null, 9L, 20L, 21L));
    }

    @Test
    public void testQuestionSubmissionAllowedNotExistsThrows() {
        when(questionSubmissionRepository.existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndQuestionSubmissionId(9L, 20L, 21L)).thenReturn(false);

        assertThrows(QuestionSubmissionNotMatchingException.class, () -> apiJwtServiceImpl.questionSubmissionAllowed(null, 9L, 20L, 21L));
    }

    /* ***************** submissionCommentAllowed ***************** */

    @Test
    public void testSubmissionCommentAllowedExists() {
        when(submissionCommentRepository.existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndSubmissionCommentId(9L, 20L, 30L)).thenReturn(true);

        assertDoesNotThrow(() -> apiJwtServiceImpl.submissionCommentAllowed(null, 9L, 20L, 30L));
    }

    @Test
    public void testSubmissionCommentAllowedNotExistsThrows() {
        when(submissionCommentRepository.existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndSubmissionCommentId(9L, 20L, 30L)).thenReturn(false);

        assertThrows(SubmissionCommentNotMatchingException.class, () -> apiJwtServiceImpl.submissionCommentAllowed(null, 9L, 20L, 30L));
    }

    /* ***************** questionSubmissionCommentAllowed ***************** */

    @Test
    public void testQuestionSubmissionCommentAllowedExists() {
        when(questionSubmissionCommentRepository.existsByQuestionSubmission_QuestionSubmissionIdAndQuestionSubmissionCommentId(21L, 31L)).thenReturn(true);

        assertDoesNotThrow(() -> apiJwtServiceImpl.questionSubmissionCommentAllowed(null, 21L, 31L));
    }

    @Test
    public void testQuestionSubmissionCommentAllowedNotExistsThrows() {
        when(questionSubmissionCommentRepository.existsByQuestionSubmission_QuestionSubmissionIdAndQuestionSubmissionCommentId(21L, 31L)).thenReturn(false);

        assertThrows(QuestionSubmissionCommentNotMatchingException.class, () -> apiJwtServiceImpl.questionSubmissionCommentAllowed(null, 21L, 31L));
    }

    /* ***************** outcomeAllowed ***************** */

    @Test
    public void testOutcomeAllowedExists() {
        when(outcomeRepository.existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndOutcomeId(5L, 8L, 40L)).thenReturn(true);

        assertDoesNotThrow(() -> apiJwtServiceImpl.outcomeAllowed(null, 5L, 8L, 40L));
    }

    @Test
    public void testOutcomeAllowedNotExistsThrows() {
        when(outcomeRepository.existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndOutcomeId(5L, 8L, 40L)).thenReturn(false);

        assertThrows(OutcomeNotMatchingException.class, () -> apiJwtServiceImpl.outcomeAllowed(null, 5L, 8L, 40L));
    }

    /* ***************** outcomeScoreAllowed ***************** */

    @Test
    public void testOutcomeScoreAllowedExists() {
        when(outcomeScoreRepository.existsByOutcome_OutcomeIdAndOutcomeScoreId(40L, 50L)).thenReturn(true);

        assertDoesNotThrow(() -> apiJwtServiceImpl.outcomeScoreAllowed(null, 40L, 50L));
    }

    @Test
    public void testOutcomeScoreAllowedNotExistsThrows() {
        when(outcomeScoreRepository.existsByOutcome_OutcomeIdAndOutcomeScoreId(40L, 50L)).thenReturn(false);

        assertThrows(OutcomeScoreNotMatchingException.class, () -> apiJwtServiceImpl.outcomeScoreAllowed(null, 40L, 50L));
    }

    /* ***************** integrationAllowed ***************** */

    @Test
    public void testIntegrationAllowedExists() {
        UUID uuid = UUID.randomUUID();
        when(integrationRepository.existsByUuidAndQuestion_QuestionId(uuid, 11L)).thenReturn(true);

        assertDoesNotThrow(() -> apiJwtServiceImpl.integrationAllowed(11L, uuid));
    }

    @Test
    public void testIntegrationAllowedNotExistsThrows() {
        UUID uuid = UUID.randomUUID();
        when(integrationRepository.existsByUuidAndQuestion_QuestionId(uuid, 11L)).thenReturn(false);

        assertThrows(IntegrationOwnerNotMatchingException.class, () -> apiJwtServiceImpl.integrationAllowed(11L, uuid));
    }

    /* ***************** experimentImportAllowed ***************** */

    @Test
    public void testExperimentImportAllowedFound() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(experimentImportRepository.findByUuidAndOwner_UserKeyAndContext_ContextId(uuid, USER_ID, 1L)).thenReturn(Optional.of(experimentImport));

        ExperimentImport result = apiJwtServiceImpl.experimentImportAllowed(securedInfo, uuid);

        assertSame(experimentImport, result);
    }

    @Test
    public void testExperimentImportAllowedNotFoundThrows() {
        UUID uuid = UUID.randomUUID();
        when(experimentImportRepository.findByUuidAndOwner_UserKeyAndContext_ContextId(uuid, USER_ID, 1L)).thenReturn(Optional.empty());

        assertThrows(ExperimentImportNotFoundException.class, () -> apiJwtServiceImpl.experimentImportAllowed(securedInfo, uuid));
    }

    /* ***************** messagingContainerAllowed ***************** */

    @Test
    public void testMessagingContainerAllowedFound() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(messageContainerRepository.findByUuidAndExposure_ExposureIdAndOwner_LmsUserId(uuid, 7L, "1")).thenReturn(Optional.of(messageContainer));

        MessageContainer result = apiJwtServiceImpl.messagingContainerAllowed(securedInfo, 7L, uuid);

        assertSame(messageContainer, result);
    }

    @Test
    public void testMessagingContainerAllowedNotFoundThrowsOwnerNotMatching() {
        // NOTE: the method declares 3 possible exceptions (Owner/NotMatching/NotFound) but this
        // implementation only ever throws MessageContainerOwnerNotMatchingException on a miss.
        UUID uuid = UUID.randomUUID();
        when(messageContainerRepository.findByUuidAndExposure_ExposureIdAndOwner_LmsUserId(uuid, 7L, "1")).thenReturn(Optional.empty());

        assertThrows(MessageContainerOwnerNotMatchingException.class, () -> apiJwtServiceImpl.messagingContainerAllowed(securedInfo, 7L, uuid));
    }

    /* ***************** messagingContainerConfigurationAllowed ***************** */

    @Test
    public void testMessagingContainerConfigurationAllowedFound() throws Exception {
        UUID containerUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        when(messageContainerConfigurationRepository.findByUuidAndContainer_UuidAndContainer_Owner_LmsUserId(uuid, containerUuid, "1")).thenReturn(Optional.of(messageContainerConfiguration));

        MessageContainerConfiguration result = apiJwtServiceImpl.messagingContainerConfigurationAllowed(securedInfo, containerUuid, uuid);

        assertSame(messageContainerConfiguration, result);
    }

    @Test
    public void testMessagingContainerConfigurationAllowedNotFoundThrows() {
        UUID containerUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        when(messageContainerConfigurationRepository.findByUuidAndContainer_UuidAndContainer_Owner_LmsUserId(uuid, containerUuid, "1")).thenReturn(Optional.empty());

        assertThrows(MessageContainerConfigurationNotFoundException.class, () -> apiJwtServiceImpl.messagingContainerConfigurationAllowed(securedInfo, containerUuid, uuid));
    }

    /* ***************** messagingAllowed ***************** */

    @Test
    public void testMessagingAllowedFound() throws Exception {
        UUID containerUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        when(messageRepository.findByUuidAndContainer_UuidAndContainer_Owner_LmsUserId(uuid, containerUuid, "1")).thenReturn(Optional.of(message));

        Message result = apiJwtServiceImpl.messagingAllowed(securedInfo, containerUuid, uuid);

        assertSame(message, result);
    }

    @Test
    public void testMessagingAllowedNotFoundThrows() {
        UUID containerUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        when(messageRepository.findByUuidAndContainer_UuidAndContainer_Owner_LmsUserId(uuid, containerUuid, "1")).thenReturn(Optional.empty());

        assertThrows(MessageNotMatchingException.class, () -> apiJwtServiceImpl.messagingAllowed(securedInfo, containerUuid, uuid));
    }

    /* ***************** messagingContentAllowed ***************** */

    @Test
    public void testMessagingContentAllowedFound() throws Exception {
        UUID messageUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        when(messageContentRepository.findByUuidAndMessage_UuidAndMessage_Container_Owner_LmsUserId(uuid, messageUuid, "1")).thenReturn(Optional.of(messageContent));

        MessageContent result = apiJwtServiceImpl.messagingContentAllowed(securedInfo, messageUuid, uuid);

        assertSame(messageContent, result);
    }

    @Test
    public void testMessagingContentAllowedNotFoundThrows() {
        UUID messageUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        when(messageContentRepository.findByUuidAndMessage_UuidAndMessage_Container_Owner_LmsUserId(uuid, messageUuid, "1")).thenReturn(Optional.empty());

        assertThrows(MessageContentNotMatchingException.class, () -> apiJwtServiceImpl.messagingContentAllowed(securedInfo, messageUuid, uuid));
    }

    /* ***************** messagingConfigurationAllowed ***************** */

    @Test
    public void testMessagingConfigurationAllowedFound() throws Exception {
        UUID messageUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        when(messageConfigurationRepository.findByUuidAndMessage_UuidAndMessage_Container_Owner_LmsUserId(uuid, messageUuid, "1")).thenReturn(Optional.of(messageConfiguration));

        MessageConfiguration result = apiJwtServiceImpl.messagingConfigurationAllowed(securedInfo, messageUuid, uuid);

        assertSame(messageConfiguration, result);
    }

    @Test
    public void testMessagingConfigurationAllowedNotFoundThrows() {
        UUID messageUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        when(messageConfigurationRepository.findByUuidAndMessage_UuidAndMessage_Container_Owner_LmsUserId(uuid, messageUuid, "1")).thenReturn(Optional.empty());

        assertThrows(MessageConfigurationNotMatchingException.class, () -> apiJwtServiceImpl.messagingConfigurationAllowed(securedInfo, messageUuid, uuid));
    }

    /* ***************** messagingRuleSetAllowed ***************** */

    @Test
    public void testMessagingRuleSetAllowedFound() throws Exception {
        UUID messageUuid = UUID.randomUUID();
        UUID ruleSetUuid = UUID.randomUUID();
        when(messageRuleSetRepository.findByUuidAndMessage_Uuid(ruleSetUuid, messageUuid)).thenReturn(Optional.of(messageRecipientRuleSet));

        MessageRecipientRuleSet result = apiJwtServiceImpl.messagingRuleSetAllowed(null, messageUuid, ruleSetUuid);

        assertSame(messageRecipientRuleSet, result);
    }

    @Test
    public void testMessagingRuleSetAllowedNotFoundThrows() {
        UUID messageUuid = UUID.randomUUID();
        UUID ruleSetUuid = UUID.randomUUID();
        when(messageRuleSetRepository.findByUuidAndMessage_Uuid(ruleSetUuid, messageUuid)).thenReturn(Optional.empty());

        assertThrows(MessageRuleSetNotMatchingException.class, () -> apiJwtServiceImpl.messagingRuleSetAllowed(null, messageUuid, ruleSetUuid));
    }

    /* ***************** messagingRuleAllowed ***************** */

    @Test
    public void testMessagingRuleAllowedFound() throws Exception {
        UUID ruleSetUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        when(messageRuleRepository.findByUuidAndRuleSet_Uuid(uuid, ruleSetUuid)).thenReturn(Optional.of(messageRecipientRule));

        MessageRecipientRule result = apiJwtServiceImpl.messagingRuleAllowed(null, ruleSetUuid, uuid);

        assertSame(messageRecipientRule, result);
    }

    @Test
    public void testMessagingRuleAllowedNotFoundThrows() {
        UUID ruleSetUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        when(messageRuleRepository.findByUuidAndRuleSet_Uuid(uuid, ruleSetUuid)).thenReturn(Optional.empty());

        assertThrows(MessageRuleNotMatchingException.class, () -> apiJwtServiceImpl.messagingRuleAllowed(null, ruleSetUuid, uuid));
    }

    /* ***************** messagingConditionalTextAllowed ***************** */

    @Test
    public void testMessagingConditionalTextAllowedFound() throws Exception {
        UUID contentUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        when(messageConditionalTextRepository.findByUuidAndContent_UuidAndContent_Message_Container_Owner_LmsUserId(uuid, contentUuid, "1")).thenReturn(Optional.of(messageConditionalText));

        MessageConditionalText result = apiJwtServiceImpl.messagingConditionalTextAllowed(securedInfo, contentUuid, uuid);

        assertSame(messageConditionalText, result);
    }

    @Test
    public void testMessagingConditionalTextAllowedNotFoundThrows() {
        UUID contentUuid = UUID.randomUUID();
        UUID uuid = UUID.randomUUID();
        when(messageConditionalTextRepository.findByUuidAndContent_UuidAndContent_Message_Container_Owner_LmsUserId(uuid, contentUuid, "1")).thenReturn(Optional.empty());

        assertThrows(MessageConditionalTextNotMatchingException.class, () -> apiJwtServiceImpl.messagingConditionalTextAllowed(securedInfo, contentUuid, uuid));
    }

    /* ***************** sanity: TextConstants message check (spot check on one exception) ***************** */

    @Test
    public void testExperimentNotMatchingExceptionMessage() {
        when(experimentRepository.findByExperimentIdAndPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(5L, 1L, 1L)).thenReturn(Optional.empty());

        ExperimentNotMatchingException ex = assertThrows(ExperimentNotMatchingException.class, () -> apiJwtServiceImpl.experimentAllowed(securedInfo, 5L));
        assertEquals(TextConstants.EXPERIMENT_NOT_MATCHING, ex.getMessage());
    }

}
