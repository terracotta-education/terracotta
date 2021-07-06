/**
 * Copyright 2021 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.*;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.enumerator.ExposureTypes;
import edu.iu.terracotta.model.app.enumerator.ParticipationTypes;
import edu.iu.terracotta.model.oauth2.Roles;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.*;
import edu.iu.terracotta.service.lti.LTIDataService;
import edu.iu.terracotta.utils.lti.LTI3Request;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import edu.iu.terracotta.utils.oauth.OAuthUtils;
import edu.iu.terracotta.utils.TextConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PublicKey;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * This manages all the data processing for the LTIRequest (and for LTI in general)
 * Necessary to get appropriate TX handling and service management
 */
@Service
public class APIJWTServiceImpl implements APIJWTService {

    static final Logger log = LoggerFactory.getLogger(APIJWTServiceImpl.class);

    @Autowired
    LTIDataService ltiDataService;

    @Autowired
    APIDataService apiDataService;

    @Autowired
    ExperimentService experimentService;

    @Autowired
    ConditionService conditionService;

    @Autowired
    ParticipantService participantService;

    @Autowired
    ExposureService exposureService;

    @Autowired
    AssignmentService assignmentService;

    @Autowired
    GroupService groupService;

    @Autowired
    TreatmentService treatmentService;

    @Autowired
    AssessmentService assessmentService;

    @Autowired
    QuestionService questionService;

    @Autowired
    AnswerService answerService;

    @Autowired
    SubmissionService submissionService;

    @Autowired
    QuestionSubmissionService questionSubmissionService;

    @Autowired
    SubmissionCommentService submissionCommentService;

    @Autowired
    QuestionSubmissionCommentService questionSubmissionCommentService;

    @Autowired
    OutcomeService outcomeService;

    @Autowired
    OutcomeScoreService outcomeScoreService;

    @Autowired
    AnswerSubmissionService answerSubmissionService;

    private static final String JWT_REQUEST_HEADER_NAME = "Authorization";
    private static final String JWT_BEARER_TYPE = "Bearer";
    private static final String QUERY_PARAM_NAME = "token";

    String error;

    /**
     * This will check that the state has been signed by us and retrieve the issuer private key.
     * We could add here other checks if we want (like the expiration of the state, nonce used only once, etc...)
     */
    //Here we could add other checks like expiration of the state (not implemented)
    @Override
    public Jws<Claims> validateToken(String token) {
        return Jwts.parser().setSigningKeyResolver(new SigningKeyResolverAdapter() {
            // This is done because each state is signed with a different key based on the issuer... so
            // we don't know the key and we need to check it pre-extracting the claims and finding the kid
            @Override
            public Key resolveSigningKey(JwsHeader header, Claims claims) {
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
        }).parseClaimsJws(token);
        // If we are on this point, then the state signature has been validated. We can start other tasks now.
    }

    @Override
    public Jwt<Header, Claims> unsecureToken(String token){
        int i = token.lastIndexOf('.');
        String withoutSignature = token.substring(0, i+1);
        return Jwts.parser().parseClaimsJwt(withoutSignature);
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
                           String canvasLoginId,
                           String canvasUserName,
                           String canvasCourseId,
                           String canvasAssignmentId,
                           String dueAt,
                           String lockAt,
                           String unlockAt
    ) throws GeneralSecurityException, IOException {

        int length = 3600;
        //We only allow 30 seconds (surely we can low that) for the one time token, because that one must be traded
        // immediately
        if (oneUse){
            length = 300; //TODO, change this test value to 30
        }
        Date date = new Date();
        Key toolPrivateKey = OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey());
        JwtBuilder builder = Jwts.builder()
                .setHeaderParam("kid", TextConstants.DEFAULT_KID)
                .setHeaderParam("typ", "JWT")
                .setIssuer("TERRACOTTA")
                .setSubject(userId) // The clientId
                .setAudience(ltiDataService.getLocalUrl())  //We send here the authToken url.
                .setExpiration(DateUtils.addSeconds(date, length)) //a java.util.Date
                .setNotBefore(date) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .claim("contextId", contextId)  //This is an specific claim to ask for tokens.
                .claim("platformDeploymentId", platformDeploymentId)  //This is an specific claim to ask for tokens.
                .claim("userId", userId)  //This is an specific claim to ask for tokens.
                .claim("roles", roles)
                .claim("assignmentId", assignmentId)
                .claim("consent", consent.toString())
                .claim("experimentId", experimentId)
                .claim("oneUse", oneUse)  //This is an specific claim to ask for tokens.
                .claim("canvasUserId", canvasUserId)
                .claim("canvasLoginId", canvasLoginId)
                .claim("canvasUserName", canvasUserName)
                .claim("canvasCourseId", canvasCourseId)
                .claim("canvasAssignmentId", canvasAssignmentId)
                .claim("dueAt", dueAt)
                .claim("lockAt", lockAt)
                .claim("unlockAt", unlockAt)
                .signWith(SignatureAlgorithm.RS256, toolPrivateKey);  //We sign it with our own private key. The platform has the public one.
        String token = builder.compact();
        if (oneUse){
            apiDataService.addOneUseToken(token);
        }
        log.debug("Token Request: \n {} \n", token);
        return token;
    }


    /**
     * This JWT will contain the token request
     */
    @Override
    public String buildJwt(boolean oneUse, LTI3Request lti3Request) throws GeneralSecurityException, IOException {

        String targetLinkUrl = lti3Request.getLtiTargetLinkUrl();
        MultiValueMap<String, String> queryParams =
                UriComponentsBuilder.fromUriString(targetLinkUrl).build().getQueryParams();
        String assignmentIdText = queryParams.getFirst("assignment");
        Long assignmentId = null;
        if (StringUtils.isNotBlank(assignmentIdText)){
            assignmentId = Long.parseLong(assignmentIdText);
        }
        String consentText = queryParams.getFirst("consent");
        boolean consent = false;
        if (StringUtils.isNotBlank(consentText)){
            if (consentText.equals("true")){
                consent = true;
            }
        }
        String experimentIdText = queryParams.getFirst("experiment");
        Long experimentId = null;
        if (StringUtils.isNotBlank(experimentIdText)){
            experimentId = Long.parseLong(experimentIdText);
        }



        return buildJwt(oneUse, lti3Request.getLtiRoles(),
                lti3Request.getContext().getContextId(),
                lti3Request.getKey().getKeyId(),
                lti3Request.getUser().getUserKey(),
                assignmentId,
                experimentId,
                consent,
                lti3Request.getLtiCustom().get("canvas_user_id").toString(),
                lti3Request.getLtiCustom().get("canvas_login_id").toString(),
                lti3Request.getLtiCustom().get("canvas_user_name").toString(),
                lti3Request.getLtiCustom().get("canvas_course_id").toString(),
                lti3Request.getLtiCustom().get("canvas_assignment_id").toString(),
                lti3Request.getLtiCustom().get("due_at").toString(),
                lti3Request.getLtiCustom().get("lock_at").toString(),
                lti3Request.getLtiCustom().get("unlock_at").toString());
    }

    @Override
    public String refreshToken(String token) throws GeneralSecurityException, IOException, BadTokenException {
        int length = 3600;
        Jws<Claims> tokenClaims = validateToken(token);
        if (tokenClaims.getBody().get("oneUse").equals("true")){
            throw new BadTokenException("Trying to refresh an one use token");
        }
        Date date = new Date();
        Key toolPrivateKey = OAuthUtils.loadPrivateKey(ltiDataService.getOwnPrivateKey());
        JwtBuilder builder = Jwts.builder()
                .setHeaderParam("kid", tokenClaims.getHeader().getKeyId())
                .setHeaderParam("typ", "JWT")
                .setIssuer(tokenClaims.getBody().getIssuer())
                .setSubject(tokenClaims.getBody().getSubject()) // The clientId
                .setAudience(tokenClaims.getBody().getAudience())  //We send here the authToken url.
                .setExpiration(DateUtils.addDays(date, length)) //a java.util.Date
                .setNotBefore(date) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .claim("contextId", tokenClaims.getBody().get("contextId"))
                .claim("platformDeploymentId", tokenClaims.getBody().get("platformDeploymentId"))
                .claim("userId", tokenClaims.getBody().get("userId"))
                .claim("roles", tokenClaims.getBody().get("roles"))
                .claim("assignmentId", tokenClaims.getBody().get("assignmentId"))
                .claim("consent", tokenClaims.getBody().get("consent"))
                .claim("experimentId", tokenClaims.getBody().get("experimentId"))
                .claim("oneUse", false)
                .claim("canvasUserId", tokenClaims.getBody().get("canvasUserId"))
                .claim("canvasLoginId", tokenClaims.getBody().get("canvasLoginId"))
                .claim("canvasUserName", tokenClaims.getBody().get("canvasUserName"))
                .claim("canvasCourseId", tokenClaims.getBody().get("canvasCourseId"))
                .claim("canvasAssignmentId", tokenClaims.getBody().get("canvasAssignmentId"))
                .claim("dueAt", tokenClaims.getBody().get("dueAt"))
                .claim("lockAt", tokenClaims.getBody().get("lockAt"))
                .claim("unlockAt", tokenClaims.getBody().get("unlockAt"))
                .signWith(SignatureAlgorithm.RS256, toolPrivateKey);  //We sign it with our own private key. The platform has the public one.
        String newToken = builder.compact();
        log.debug("Token Request: \n {} \n", newToken);
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
        if (claims != null) {
          SecuredInfo securedInfo = new SecuredInfo();
          securedInfo.setUserId(claims.getBody().get("userId").toString());
          securedInfo.setPlatformDeploymentId(Long.valueOf((Integer) claims.getBody().get("platformDeploymentId")));
          securedInfo.setContextId(Long.valueOf((Integer) claims.getBody().get("contextId")));
          securedInfo.setRoles((List<String>) claims.getBody().get("roles"));
          securedInfo.setCanvasUserId(claims.getBody().get("canvasUserId").toString());
          securedInfo.setCanvasLoginId(claims.getBody().get("canvasLoginId").toString());
          securedInfo.setCanvasUserName(claims.getBody().get("canvasUserName").toString());
          securedInfo.setCanvasCourseId(claims.getBody().get("canvasCourseId").toString());
          securedInfo.setCanvasAssignmentId(claims.getBody().get("canvasAssignmentId").toString());
          securedInfo.setDueAt(extractTimestamp(claims,"dueAt"));
          securedInfo.setLockAt(extractTimestamp(claims,"lockAt"));
          securedInfo.setUnlockAt(extractTimestamp(claims,"unlockAt"));
          return securedInfo;
        } else {
          return null;
        }
    }

    private Timestamp extractTimestamp(Jws<Claims> claims, String id){
        Timestamp extracted;
        try {
            extracted = Timestamp.valueOf(LocalDateTime.parse(claims.getBody().get(id).toString()));
        } catch (Exception ex){
            return null;
        }
        return extracted;
    }

    @Override
    public boolean isAdmin(SecuredInfo securedInfo){
        return securedInfo.getRoles().contains(Roles.ADMIN);
    }

    @Override
    public boolean isInstructor(SecuredInfo securedInfo){
        return (securedInfo.getRoles().contains(Roles.INSTRUCTOR) || securedInfo.getRoles().contains(Roles.MEMBERSHIP_INSTRUCTOR));
    }

    @Override
    public boolean isLearner(SecuredInfo securedInfo){
        return (securedInfo.getRoles().contains(Roles.LEARNER) || securedInfo.getRoles().contains(Roles.MEMBERSHIP_LEARNER));
    }

    @Override
    public boolean isGeneral(SecuredInfo securedInfo){
        return securedInfo.getRoles().contains(Roles.GENERAL);
    }

    @Override
    public boolean isInstructorOrHigher(SecuredInfo securedInfo){
        return (isInstructor(securedInfo) || isAdmin(securedInfo));
    }

    @Override
    public boolean isLearnerOrHigher(SecuredInfo securedInfo){
        return (isLearner(securedInfo) || isInstructorOrHigher(securedInfo));
    }

    private boolean isBearerToken(String rawHeaderValue) {
        return rawHeaderValue.toLowerCase().startsWith(JWT_BEARER_TYPE.toLowerCase());
    }

    @Override
    public void experimentAllowed(SecuredInfo securedInfo, Long experimentId) throws BadTokenException, ExperimentNotMatchingException {
        if (securedInfo ==null){
            log.error(TextConstants.BAD_TOKEN);
            throw new BadTokenException(TextConstants.BAD_TOKEN);
        }
        if (!experimentService.experimentBelongsToDeploymentAndCourse(experimentId, securedInfo.getPlatformDeploymentId(), securedInfo.getContextId())){
            throw new ExperimentNotMatchingException(TextConstants.EXPERIMENT_NOT_MATCHING);
        }
    }

    @Override
    public boolean experimentLocked(Long experimentId, boolean throwException) throws ExperimentLockedException, ExperimentNotMatchingException {

        Optional<Experiment> experiment = experimentService.findById(experimentId);
        if (experiment.isPresent()){
            if (experiment.get().getStarted()!=null){
                if (throwException) {
                    throw new ExperimentLockedException("Error 110: The experiment has started and can't be modified");
                } else {
                    return true;
                }
            } else {
                return false;
            }
        } else {
            throw new ExperimentNotMatchingException("The experiment with id " + experimentId + " does not exist");
        }
    }

    @Override
    public boolean conditionsLocked(Long experimentId, boolean throwException) throws ConditionsLockedException, ExperimentNotMatchingException {

        Optional<Experiment> experiment = experimentService.findById(experimentId);
        if (experiment.isPresent()){
            if (experiment.get().getExposureType().equals(ExposureTypes.NOSET)) {
                return false;
            } else {
                if (throwException) {
                    throw new ConditionsLockedException("Error 111: The conditions can't be modified at this point");
                } else {
                    return true;
                }
            }
        } else {
            throw new ExperimentNotMatchingException("The experiment with id " + experimentId + " does not exist");
        }
    }


    @Override
    public void conditionAllowed(SecuredInfo securedInfo, Long experimentId, Long conditionId) throws ConditionNotMatchingException {
        if(!conditionService.conditionBelongsToExperiment(experimentId, conditionId)) {
            throw new ConditionNotMatchingException(TextConstants.CONDITION_NOT_MATCHING);
        }
    }

    @Override
    public void participantAllowed(SecuredInfo securedInfo, Long experimentId, Long participantId) throws ParticipantNotMatchingException {
        if(!participantService.participantBelongsToExperiment(experimentId, participantId)) {
            throw new ParticipantNotMatchingException(TextConstants.PARTICIPANT_NOT_MATCHING);
        }
    }

    @Override
    public void exposureAllowed(SecuredInfo securedInfo, Long experimentId, Long exposureId) throws ExposureNotMatchingException {
        if(!exposureService.exposureBelongsToExperiment(experimentId, exposureId)) {
            throw new ExposureNotMatchingException(TextConstants.EXPOSURE_NOT_MATCHING);
        }
    }

    @Override
    public void groupAllowed(SecuredInfo securedInfo, Long experimentId, Long groupId) throws GroupNotMatchingException {
        if(!groupService.groupBelongsToExperiment(experimentId, groupId)) {
            throw new GroupNotMatchingException(TextConstants.GROUP_NOT_MATCHING);
        }
    }

    @Override
    public void assignmentAllowed(SecuredInfo securedInfo, Long experimentId, Long assignmentId) throws AssignmentNotMatchingException {
        if(!assignmentService.assignmentBelongsToExperiment(experimentId, assignmentId)) {
            throw new AssignmentNotMatchingException(TextConstants.ASSIGNMENT_NOT_MATCHING);
        }
    }

    @Override
    public void assignmentAllowed(SecuredInfo securedInfo, Long experimentId, Long exposureId, Long assignmentId) throws AssignmentNotMatchingException {
        if(!assignmentService.assignmentBelongsToExperimentAndExposure(experimentId, exposureId, assignmentId)) {
            throw new AssignmentNotMatchingException(TextConstants.ASSIGNMENT_NOT_MATCHING);
        }
    }

    @Override
    public void treatmentAllowed(SecuredInfo securedInfo, Long experimentId, Long conditionId, Long treatmentId) throws TreatmentNotMatchingException {
        if(!treatmentService.treatmentBelongsToExperimentAndCondition(experimentId, conditionId, treatmentId)) {
            throw new TreatmentNotMatchingException(TextConstants.TREATMENT_NOT_MATCHING);
        }
    }

    @Override
    public void assessmentAllowed(SecuredInfo securedInfo, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId) throws AssessmentNotMatchingException {
        if(!assessmentService.assessmentBelongsToExperimentAndConditionAndTreatment(experimentId, conditionId, treatmentId, assessmentId)) {
            throw new AssessmentNotMatchingException(TextConstants.ASSESSMENT_NOT_MATCHING);
        }
    }

    @Override
    public void questionAllowed(SecuredInfo securedInfo, Long assessmentId, Long questionId) throws QuestionNotMatchingException {
        if(!questionService.questionBelongsToAssessment(assessmentId, questionId)) {
            throw new QuestionNotMatchingException(TextConstants.QUESTION_NOT_MATCHING);
        }
    }

    @Override
    public void answerAllowed(SecuredInfo securedInfo, Long assessmentId, Long questionId, String answerType, Long answerId) throws AnswerNotMatchingException {
        if(answerType.equals("MC")){
            if(!answerService.mcAnswerBelongsToQuestionAndAssessment(assessmentId, questionId, answerId)) {
                throw new AnswerNotMatchingException(TextConstants.ANSWER_NOT_MATCHING);
            }
        } //Note: as more answer types are added, continue checking. Same exception can be thrown.
    }

    @Override
    public void answerSubmissionAllowed(SecuredInfo securedInfo, Long questionSubmissionId, String answerType, Long answerSubmissionId) throws AnswerSubmissionNotMatchingException{
        if(answerType.equals("MC")){
            if(!answerSubmissionService.mcAnswerSubmissionBelongsToQuestionSubmission(questionSubmissionId, answerSubmissionId)){
                throw new AnswerSubmissionNotMatchingException(TextConstants.ANSWER_SUBMISSION_NOT_MATCHING);
            }
        } else if(answerType.equals("ESSAY")){
            if(!answerSubmissionService.essayAnswerSubmissionBelongsToQuestionSubmission(questionSubmissionId, answerSubmissionId)){
                throw new AnswerSubmissionNotMatchingException(TextConstants.ANSWER_SUBMISSION_NOT_MATCHING);
            }
        }
        //Note: as more answer submission types are added, continue checking. Same exception can be thrown.
    }

    @Override
    public void submissionAllowed(SecuredInfo securedInfo, Long assessmentId, Long submissionId) throws SubmissionNotMatchingException {
        if(!submissionService.submissionBelongsToAssessment(assessmentId, submissionId)) {
            throw new SubmissionNotMatchingException(TextConstants.SUBMISSION_NOT_MATCHING);
        }
    }

    @Override
    public void questionSubmissionAllowed(SecuredInfo securedInfo, Long assessmentId, Long submissionId, Long questionSubmissionId) throws QuestionSubmissionNotMatchingException {
        if(!questionSubmissionService.questionSubmissionBelongsToAssessmentAndSubmission(assessmentId, submissionId, questionSubmissionId)) {
            throw new QuestionSubmissionNotMatchingException(TextConstants.QUESTION_SUBMISSION_NOT_MATCHING);
        }
    }

    @Override
    public void submissionCommentAllowed(SecuredInfo securedInfo, Long assessmentId, Long submissionId, Long submissionCommentId) throws SubmissionCommentNotMatchingException {
        if(!submissionCommentService.submissionCommentBelongsToAssessmentAndSubmission(assessmentId, submissionId, submissionCommentId)) {
            throw new SubmissionCommentNotMatchingException(TextConstants.SUBMISSION_COMMENT_NOT_MATCHING);
        }
    }

    @Override
    public void questionSubmissionCommentAllowed(SecuredInfo securedInfo, Long questionSubmissionId, Long questionSubmissionCommentId) throws QuestionSubmissionCommentNotMatchingException {
        if(!questionSubmissionCommentService.questionSubmissionCommentBelongsToQuestionSubmission(questionSubmissionId, questionSubmissionCommentId)){
            throw new QuestionSubmissionCommentNotMatchingException(TextConstants.QUESTION_SUBMISSION_COMMENT_NOT_MATCHING);
        }
    }

    @Override
    public void outcomeAllowed(SecuredInfo securedInfo, Long experimentId, Long exposureId, Long outcomeId) throws OutcomeNotMatchingException {
        if(!outcomeService.outcomeBelongsToExperimentAndExposure(experimentId, exposureId, outcomeId)){
            throw new OutcomeNotMatchingException(TextConstants.OUTCOME_NOT_MATCHING);
        }
    }

    @Override
    public void outcomeScoreAllowed(SecuredInfo securedInfo, Long outcomeId, Long outcomeScoreId) throws OutcomeScoreNotMatchingException {
        if(!outcomeScoreService.outcomeScoreBelongsToOutcome(outcomeId, outcomeScoreId)){
            throw new OutcomeScoreNotMatchingException(TextConstants.OUTCOME_SCORE_NOT_MATCHING);
        }
    }

}
