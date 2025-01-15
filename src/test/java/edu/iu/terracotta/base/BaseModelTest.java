package edu.iu.terracotta.base;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.mockito.Mock;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.LtiMembershipEntity;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.ToolDeployment;
import edu.iu.terracotta.model.ags.LineItem;
import edu.iu.terracotta.model.ags.LineItems;
import edu.iu.terracotta.model.app.AnswerEssaySubmission;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.AnswerMcSubmission;
import edu.iu.terracotta.model.app.AnswerMcSubmissionOption;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.ConsentDocument;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import edu.iu.terracotta.model.app.Group;
import edu.iu.terracotta.model.app.Outcome;
import edu.iu.terracotta.model.app.OutcomeScore;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.QuestionMc;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.QuestionSubmissionComment;
import edu.iu.terracotta.model.app.RegradeDetails;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.AnswerDto;
import edu.iu.terracotta.model.app.dto.AnswerSubmissionDto;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import edu.iu.terracotta.model.app.dto.AssignmentDto;
import edu.iu.terracotta.model.app.dto.OutcomeDto;
import edu.iu.terracotta.model.app.dto.ParticipantDto;
import edu.iu.terracotta.model.app.dto.QuestionDto;
import edu.iu.terracotta.model.app.dto.QuestionSubmissionCommentDto;
import edu.iu.terracotta.model.app.dto.SubmissionDto;
import edu.iu.terracotta.model.app.dto.TreatmentDto;
import edu.iu.terracotta.model.app.dto.UserDto;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.ResultsOutcomesDto;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.condition.OutcomesCondition;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.condition.OutcomesConditions;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.enums.AlternateIdType;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.exposure.OutcomesExposure;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.exposure.OutcomesExposureOverall;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.exposure.OutcomesExposures;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.request.AlternateIdDto;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.request.ResultsOutcomesRequestDto;
import edu.iu.terracotta.model.app.dto.dashboard.results.overview.ResultsOverviewDto;
import edu.iu.terracotta.model.app.enumerator.DistributionTypes;
import edu.iu.terracotta.model.app.enumerator.ExposureTypes;
import edu.iu.terracotta.model.app.enumerator.MultipleSubmissionScoringScheme;
import edu.iu.terracotta.model.app.enumerator.ParticipationTypes;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import edu.iu.terracotta.model.app.integrations.AnswerIntegrationSubmission;
import edu.iu.terracotta.model.app.integrations.Integration;
import edu.iu.terracotta.model.app.integrations.IntegrationClient;
import edu.iu.terracotta.model.app.integrations.IntegrationConfiguration;
import edu.iu.terracotta.model.app.integrations.IntegrationToken;
import edu.iu.terracotta.model.app.integrations.IntegrationTokenLog;
import edu.iu.terracotta.model.app.integrations.dto.IntegrationClientDto;
import edu.iu.terracotta.model.app.integrations.dto.IntegrationConfigurationDto;
import edu.iu.terracotta.model.app.integrations.dto.IntegrationDto;
import edu.iu.terracotta.model.app.integrations.dto.IntegrationLaunchParameterDto;
import edu.iu.terracotta.model.app.integrations.enums.IntegrationTokenType;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.model.canvas.CanvasAPIScope;
import edu.iu.terracotta.model.canvas.CanvasAPITokenEntity;
import edu.iu.terracotta.model.events.Event;
import edu.iu.terracotta.model.membership.CourseUser;
import edu.iu.terracotta.model.membership.CourseUsers;
import edu.iu.terracotta.model.oauth2.LTIToken;
import edu.iu.terracotta.model.oauth2.Roles;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.canvas.AssignmentWriterExtended;
import edu.iu.terracotta.utils.lti.LTI3Request;
import edu.ksu.canvas.model.User;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseModelTest {

    public static final String ASSIGNMENT_TITLE = "test assignment title";
    public static final String CONDITION_TITLE = "test condition title";
    public static final String DISPLAY_NAME = "Terracotta User";
    public static final String EMAIL = "abc@terracotta.edu";
    public static final String EXPOSURE_TITLE = "test exposure title";
    public static final String OUTCOME_TITLE = "test outcome title";
    public static final String RESOURCE_LINK_ID = "resource_link_id";
    public static final String CANVAS_API_SCOPE = "url:POST|/api/v1/courses/:course_id/assignments";
    public static final String USER_ID = "user_id";
    public static final String INTEGRATION_CLIENT_NAME = "integration client name";
    public static final String INTEGRATION_SCORE_VARIABLE = "integration score variable";
    public static final String INTEGRATION_TOKEN_VARIABLE = "integration token variable";
    public static final String INTEGRATION_LAUNCH_URL = "http://launch.url";
    public static final String INTEGRATION_RETURN_URL = "http://return.url";
    public static final String INTEGRATION_TOKEN = "token";
    public static final String INTEGRATION_TOKEN_LOG_CODE = "token_error_code";

    @Mock protected EntityManager entityManager;

    @Mock protected AlternateIdDto alternateIdDto;
    @Mock protected AnswerDto answerDto;
    @Mock protected AnswerEssaySubmission answerEssaySubmission;
    @Mock protected AnswerIntegrationSubmission answerIntegrationSubmission;
    @Mock protected AnswerMc answerMc;
    @Mock protected AnswerMcSubmission answerMcSubmission;
    @Mock protected AnswerMcSubmissionOption answerMcSubmissionOption;
    @Mock protected AnswerSubmissionDto answerSubmissionDto;
    @Mock protected Assessment assessment;
    @Mock protected AssessmentDto assessmentDto;
    @Mock protected Assignment assignment;
    @Mock protected AssignmentDto assignmentDto;
    @Mock protected AssignmentExtended assignmentExtended;
    @Mock protected AssignmentWriterExtended assignmentWriterExtended;
    @Mock protected CanvasAPIScope canvasAPIScope;
    @Mock protected CanvasAPITokenEntity canvasAPITokenEntity;
    @Mock protected Condition condition;
    @Mock protected ConsentDocument consentDocument;
    @Mock protected CourseUser courseUser;
    @Mock protected CourseUsers courseUsers;
    @Mock protected Environment environment;
    @Mock protected Event event;
    @Mock protected Experiment experiment;
    @Mock protected Exposure exposure;
    @Mock protected ExposureGroupCondition exposureGroupCondition;
    @Mock protected Group group;
    @Mock protected HttpServletRequest httpServletRequest;
    @Mock protected InputStream inputStream;
    @Mock protected Integration integration;
    @Mock protected IntegrationClient integrationClient;
    @Mock protected IntegrationClientDto integrationClientDto;
    @Mock protected IntegrationConfiguration integrationConfiguration;
    @Mock protected IntegrationConfigurationDto integrationConfigurationDto;
    @Mock protected IntegrationDto integrationDto;
    @Mock protected IntegrationLaunchParameterDto integrationLaunchParameterDto;
    @Mock protected IntegrationToken integrationToken;
    @Mock protected IntegrationTokenLog integrationTokenLog;
    @Mock protected Map<String, Object> jwt;
    @Mock protected LineItem lineItem;
    @Mock protected LineItems lineItems;
    @Mock protected LtiContextEntity ltiContextEntity;
    @Mock protected LtiMembershipEntity ltiMembershipEntity;
    @Mock protected LTIToken ltiToken;
    @Mock protected LtiUserEntity ltiUserEntity;
    @Mock protected LTI3Request lti3Request;
    @Mock protected Outcome outcome;
    @Mock protected OutcomeDto outcomeDto;
    @Mock protected OutcomesCondition outcomesCondition;
    @Mock protected OutcomesConditions outcomesConditions;
    @Mock protected OutcomeScore outcomeScore;
    @Mock protected OutcomesExposure outcomesExposure;
    @Mock protected OutcomesExposures outcomesExposures;
    @Mock protected Participant participant;
    @Mock protected ParticipantDto participantDto;
    @Mock protected PlatformDeployment platformDeployment;
    @Mock protected Question question;
    @Mock protected QuestionDto questionDto;
    @Mock protected QuestionMc questionMc;
    @Mock protected QuestionSubmission questionSubmission;
    @Mock protected QuestionSubmissionComment questionSubmissionComment;
    @Mock protected QuestionSubmissionCommentDto questionSubmissionCommentDto;
    @Mock protected RegradeDetails regradeDetails;
    @Mock protected RestTemplate restTemplate;
    @Mock protected ResultsOutcomesDto resultsOutcomesDto;
    @Mock protected ResultsOutcomesRequestDto resultsOutcomesRequestDto;
    @Mock protected ResultsOverviewDto resultsOverviewDto;
    @Mock protected SecuredInfo securedInfo;
    @Mock protected Submission submission;
    @Mock protected SubmissionDto submissionDto;
    @Mock protected edu.ksu.canvas.model.assignment.Submission submissionCanvas;
    @Mock protected ToolDeployment toolDeployment;
    @Mock protected Treatment treatment;
    @Mock protected TreatmentDto treatmentDto;
    @Mock protected User user;
    @Mock protected UserDto userDto;

    public void setup() {
        try {
            when(alternateIdDto.getId()).thenReturn(AlternateIdType.AVERAGE_ASSIGNMENT_SCORE.name());
            when(alternateIdDto.getExposures()).thenReturn(Collections.singletonList(1L));
            when(answerEssaySubmission.getQuestionSubmission()).thenReturn(questionSubmission);
            when(answerMc.getAnswerMcId()).thenReturn(1L);
            when(answerMc.getCorrect()).thenReturn(true);
            when(answerMc.getQuestion()).thenReturn(questionMc);
            when(answerMcSubmission.getAnswerMc()).thenReturn(answerMc);
            when(answerMcSubmissionOption.getAnswerMc()).thenReturn(answerMc);
            when(answerMcSubmissionOption.getAnswerOrder()).thenReturn(0);
            when(assessment.canViewCorrectAnswers()).thenReturn(true);
            when(assessment.canViewResponses()).thenReturn(true);
            when(assessment.getAssessmentId()).thenReturn(1L);
            when(assessment.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.MOST_RECENT);
            when(assessment.getQuestions()).thenReturn(Collections.singletonList(question));
            when(assessment.getTreatment()).thenReturn(treatment);
            when(assessmentDto.getAssessmentId()).thenReturn(1L);
            when(assessmentDto.getTreatmentId()).thenReturn(1L);
            when(assignment.getAssignmentId()).thenReturn(1L);
            when(assignment.getExposure()).thenReturn(exposure);
            when(assignment.getLmsAssignmentId()).thenReturn("1");
            when(assignment.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.MOST_RECENT);
            when(assignment.getSoftDeleted()).thenReturn(false);
            when(assignment.getTitle()).thenReturn(ASSIGNMENT_TITLE);
            when(assignmentDto.getAssignmentId()).thenReturn(1L);
            when(assignmentDto.getTreatments()).thenReturn(Collections.singletonList(treatmentDto));
            when(assignmentExtended.getId()).thenReturn(1L);
            when(assignmentExtended.getSecureParams()).thenReturn(RESOURCE_LINK_ID);
            when(assignmentExtended.isPublished()).thenReturn(true);
            when(canvasAPIScope.getScope()).thenReturn(CANVAS_API_SCOPE);
            when(canvasAPITokenEntity.getTokenId()).thenReturn(1L);
            when(canvasAPITokenEntity.getExpiresAt()).thenReturn(Timestamp.from(Instant.now()));
            when(condition.getConditionId()).thenReturn(1L);
            when(condition.getExperiment()).thenReturn(experiment);
            when(condition.getName()).thenReturn(CONDITION_TITLE);
            when(consentDocument.getLmsAssignmentId()).thenReturn("1");
            when(consentDocument.getResourceLinkId()).thenReturn(RESOURCE_LINK_ID);
            when(courseUser.getRoles()).thenReturn(Collections.singletonList(Roles.LEARNER));
            when(courseUser.getUserId()).thenReturn(USER_ID);
            when(courseUsers.getCourseUserList()).thenReturn(Collections.singletonList(courseUser));
            when(event.getJson()).thenReturn(eventJson());
            when(event.getParticipant()).thenReturn(participant);
            when(experiment.getConditions()).thenReturn(Collections.singletonList(condition));
            when(experiment.getConsentDocument()).thenReturn(consentDocument);
            when(experiment.getCreatedAt()).thenReturn(new Timestamp(System.currentTimeMillis()));
            when(experiment.getCreatedBy()).thenReturn(ltiUserEntity);
            when(experiment.getDistributionType()).thenReturn(DistributionTypes.EVEN);
            when(experiment.getExperimentId()).thenReturn(1L);
            when(experiment.getExposures()).thenReturn(Collections.singletonList(exposure));
            when(experiment.getExposureType()).thenReturn(ExposureTypes.BETWEEN);
            when(experiment.getLtiContextEntity()).thenReturn(ltiContextEntity);
            when(experiment.getParticipants()).thenReturn(Collections.singletonList(participant));
            when(experiment.getParticipationType()).thenReturn(ParticipationTypes.AUTO);
            when(experiment.getPlatformDeployment()).thenReturn(platformDeployment);
            when(exposure.getExperiment()).thenReturn(experiment);
            when(exposure.getExposureId()).thenReturn(1L);
            when(exposure.getTitle()).thenReturn(EXPOSURE_TITLE);
            when(exposureGroupCondition.getCondition()).thenReturn(condition);
            when(exposureGroupCondition.getExposure()).thenReturn(exposure);
            when(exposureGroupCondition.getGroup()).thenReturn(group);
            when(group.getGroupId()).thenReturn(1L);
            when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer());
            when(httpServletRequest.getQueryString()).thenReturn("a=b");
            when(integration.getConfiguration()).thenReturn(integrationConfiguration);
            when(integration.getQuestion()).thenReturn(question);
            when(integrationClient.getConfiguration()).thenReturn(Collections.singletonList(integrationConfiguration));
            when(integrationClient.getId()).thenReturn(1l);
            when(integrationClient.getName()).thenReturn(INTEGRATION_CLIENT_NAME);
            when(integrationClient.getPreviewToken()).thenReturn(INTEGRATION_TOKEN);
            when(integrationClient.getTokenVariable()).thenReturn(INTEGRATION_TOKEN_VARIABLE);
            when(integrationClient.getScoreVariable()).thenReturn(INTEGRATION_SCORE_VARIABLE);
            when(integrationClient.getUuid()).thenReturn(UUID.randomUUID());
            when(integrationClientDto.getId()).thenReturn(UUID.randomUUID());
            when(integrationClientDto.getName()).thenReturn(INTEGRATION_CLIENT_NAME);
            when(integrationClientDto.getReturnUrl()).thenReturn(INTEGRATION_RETURN_URL);
            when(integrationConfiguration.getClient()).thenReturn(integrationClient);
            when(integrationConfiguration.getId()).thenReturn(1l);
            when(integrationConfiguration.getIntegration()).thenReturn(integration);
            when(integrationConfiguration.getLaunchUrl()).thenReturn(INTEGRATION_LAUNCH_URL);
            when(integrationConfiguration.getUuid()).thenReturn(UUID.randomUUID());
            when(integrationConfigurationDto.getClient()).thenReturn(integrationClientDto);
            when(integrationConfigurationDto.getId()).thenReturn(UUID.randomUUID());
            when(integrationConfigurationDto.getIntegrationId()).thenReturn(UUID.randomUUID());
            when(integrationConfigurationDto.getLaunchUrl()).thenReturn(INTEGRATION_LAUNCH_URL);
            when(integrationDto.getConfiguration()).thenReturn(integrationConfigurationDto);
            when(integrationToken.getId()).thenReturn(1l);
            when(integrationToken.getIntegration()).thenReturn(integration);
            when(integrationToken.getLogs()).thenReturn(Collections.singletonList(integrationTokenLog));
            when(integrationToken.getSecuredInfo()).thenReturn(Optional.of(securedInfo));
            when(integrationToken.getSubmission()).thenReturn(submission);
            when(integrationToken.getType()).thenReturn(IntegrationTokenType.STANDARD);
            when(integrationToken.getUser()).thenReturn(ltiUserEntity);
            when(integrationTokenLog.getCode()).thenReturn(INTEGRATION_TOKEN_LOG_CODE);
            when(jwt.get(anyString())).thenReturn(RESOURCE_LINK_ID);
            when(lineItem.getId()).thenReturn("1");
            when(lineItem.getResourceLinkId()).thenReturn(RESOURCE_LINK_ID);
            when(lineItems.getLineItemList()).thenReturn(Collections.singletonList(lineItem));
            when(ltiContextEntity.getContextId()).thenReturn(1l);
            when(ltiContextEntity.getContext_memberships_url()).thenReturn("courses/1/names");
            when(ltiMembershipEntity.getUser()).thenReturn(ltiUserEntity);
            when(ltiUserEntity.getDisplayName()).thenReturn(DISPLAY_NAME);
            when(ltiUserEntity.getEmail()).thenReturn(EMAIL);
            when(ltiUserEntity.getPlatformDeployment()).thenReturn(platformDeployment);
            when(ltiContextEntity.getToolDeployment()).thenReturn(toolDeployment);
            when(ltiUserEntity.getUserKey()).thenReturn(USER_ID);
            when(outcome.getExposure()).thenReturn(exposure);
            when(outcome.getLmsOutcomeId()).thenReturn("1");
            when(outcome.getMaxPoints()).thenReturn(1F);
            when(outcome.getOutcomeId()).thenReturn(1L);
            when(outcome.getOutcomeScores()).thenReturn(Collections.singletonList(outcomeScore));
            when(outcomeDto.getExternal()).thenReturn(false);
            when(outcomeDto.getTitle()).thenReturn(OUTCOME_TITLE);
            when(outcomesCondition.getTitle()).thenReturn(OUTCOME_TITLE);
            when(outcomesConditions.getRows()).thenReturn(Collections.singletonList(outcomesCondition));
            when(outcomeScore.getOutcome()).thenReturn(outcome);
            when(outcomeScore.getParticipant()).thenReturn(participant);
            when(outcomesExposures.getRows()).thenReturn(Collections.singletonList(outcomesExposure));
            when(outcomesExposure.getTitle()).thenReturn(EXPOSURE_TITLE, OutcomesExposureOverall.EXPOSURE_OVERALL_TITLE);
            when(participant.getConsent()).thenReturn(true);
            when(participant.getDateGiven()).thenReturn(Timestamp.from(Instant.now()));
            when(participant.getExperiment()).thenReturn(experiment);
            when(participant.getGroup()).thenReturn(group);
            when(participant.getLtiMembershipEntity()).thenReturn(ltiMembershipEntity);
            when(participant.getLtiUserEntity()).thenReturn(ltiUserEntity);
            when(participant.getParticipantId()).thenReturn(1L);
            when(participant.getSource()).thenReturn(ParticipationTypes.AUTO);
            when(participant.isTestStudent()).thenReturn(false);
            when(participantDto.getGroupId()).thenReturn(1L);
            when(participantDto.getParticipantId()).thenReturn(1L);
            when(participantDto.getUser()).thenReturn(userDto);
            when(platformDeployment.getLocalUrl()).thenReturn("https://localhost");
            when(question.getAssessment()).thenReturn(assessment);
            when(question.getIntegration()).thenReturn(integration);
            when(question.getPoints()).thenReturn(1F);
            when(question.getQuestionId()).thenReturn(1L);
            when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);
            when(question.isIntegration()).thenReturn(false);
            when(questionDto.getAnswers()).thenReturn(Collections.singletonList(answerDto));
            when(questionDto.getQuestionId()).thenReturn(null);
            when(questionMc.getAssessment()).thenReturn(assessment);
            when(questionMc.getPoints()).thenReturn(1F);
            when(questionMc.getQuestionId()).thenReturn(1L);
            when(questionMc.getQuestionType()).thenReturn(QuestionTypes.MC);
            when(questionMc.isRandomizeAnswers()).thenReturn(true);
            when(questionSubmission.getAlteredGrade()).thenReturn(1F);
            when(questionSubmission.getCalculatedPoints()).thenReturn(1F);
            when(questionSubmission.getQuestion()).thenReturn(question);
            when(questionSubmission.getQuestionSubmissionId()).thenReturn(1L);
            when(questionSubmission.getSubmission()).thenReturn(submission);
            when(resultsOutcomesRequestDto.getAlternateId()).thenReturn(alternateIdDto);
            when(resultsOutcomesRequestDto.getOutcomeIds()).thenReturn(Collections.singletonList(1L));
            when(securedInfo.getCanvasAssignmentId()).thenReturn("1");
            when(securedInfo.getCanvasCourseId()).thenReturn("1");
            when(securedInfo.getCanvasUserId()).thenReturn("1");
            when(securedInfo.getPlatformDeploymentId()).thenReturn(1L);
            when(securedInfo.getUserId()).thenReturn(USER_ID);
            when(submission.getAlteredCalculatedGrade()).thenReturn(1F);
            when(submission.getAssessment()).thenReturn(assessment);
            when(submission.getCalculatedGrade()).thenReturn(1F);
            when(submission.getDateSubmitted()).thenReturn(Timestamp.from(Instant.now()));
            when(submission.getIntegration()).thenReturn(integration);
            when(submission.getIntegrationLaunchUrl()).thenReturn(INTEGRATION_LAUNCH_URL);
            when(submission.getLatestIntegrationToken()).thenReturn(Optional.of(integrationToken));
            when(submission.getParticipant()).thenReturn(participant);
            when(submission.getQuestionSubmissions()).thenReturn(Collections.singletonList(questionSubmission));
            when(submission.getSubmissionId()).thenReturn(1L);
            when(submission.getTotalAlteredGrade()).thenReturn(1F);
            when(submission.isIntegration()).thenReturn(false);
            when(submissionCanvas.getScore()).thenReturn(1.0D);
            when(submissionCanvas.getUser()).thenReturn(user);
            when(submissionDto.getDateCreated()).thenReturn(Timestamp.from(Instant.now()));
            when(submissionDto.getDateSubmitted()).thenReturn(Timestamp.from(Instant.now()));
            when(submissionDto.getParticipantId()).thenReturn(1L);
            when(toolDeployment.getPlatformDeployment()).thenReturn(platformDeployment);
            when(treatment.getAssessment()).thenReturn(assessment);
            when(treatment.getAssignment()).thenReturn(assignment);
            when(treatment.getCondition()).thenReturn(condition);
            when(treatment.getTreatmentId()).thenReturn(1L);
            when(user.getId()).thenReturn(1L);
            when(user.getLoginId()).thenReturn(EMAIL);
            when(user.getName()).thenReturn(DISPLAY_NAME);
            when(userDto.getUserId()).thenReturn(1L);
        } catch (Exception e) {
            log.error("Exception occurred in BaseModelTest setup()", e);
        }
    }

    private String eventJson() {
        return """
            {
            \"sendTime\": \"2023-09-27T12:51:26.874Z\",
            \"dataVersion\": \"http://purl.imsglobal.org/ctx/caliper/v1p2\",
            \"data\":
            [
                {
                \"@context\": \"http://purl.imsglobal.org/ctx/caliper/v1p2\",
                \"type\": \"ToolUseEvent\",
                \"id\": \"urn:uuid:08164d74-1c50-4318-b1e2-4cec525fdbf2\",
                \"actor\":
                {
                    \"id\": \"https://bob.terracotta.education/users/2\",
                    \"type\": \"Person\",
                    \"extensions\":
                    {
                        \"lti_id\": \"d8ad9069-11fe-4123-bb0d-78b370db3758\",
                        \"lti_tenant\": \"https://terracotta.instructure.com\",
                        \"canvas_global_id\": \"202570000000000196\"
                    }
                },
                \"action\": \"Used\",
                \"object\":
                {
                    \"id\": \"https://bob.terracotta.education\",
                    \"type\": \"SoftwareApplication\",
                    \"name\": \"Terracotta Bob\"
                },
                \"referrer\":
                {
                    \"id\": \"https://terracotta.instructure.com\",
                    \"type\": \"SoftwareApplication\"
                },
                \"eventTime\": \"2023-09-27T12:51:26.810Z\",
                \"edApp\":
                {
                    \"id\": \"https://bob.terracotta.education\",
                    \"type\": \"SoftwareApplication\",
                    \"name\": \"Terracotta Bob\"
                },
                \"group\":
                {
                    \"id\": \"https://terracotta.instructure.com/courses/106\",
                    \"type\": \"CourseOffering\",
                    \"name\": \"Terracotta QA Course\"
                },
                \"federatedSession\":
                {
                    \"id\": \"urn:session_id_localized:https://bob.terracotta.education/lti/oauth_nonce/null\",
                    \"type\": \"LtiSession\",
                    \"messageParameters\":
                    {
                    \"canvas_course_id\": \"106\",
                    \"lti_context_id\": \"93e5df5143152919f977e68e399f71a62eceb6d8\",
                    \"canvas_login_id\": \"rlong@unicon.net\",
                    \"canvas_roles\":
                    [
                        \"http://purl.imsglobal.org/vocab/lis/v2/institution/person#Administrator\",
                        \"http://purl.imsglobal.org/vocab/lis/v2/institution/person#Instructor\",
                        \"http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor\",
                        \"http://purl.imsglobal.org/vocab/lis/v2/system/person#User\"
                    ],
                    \"canvas_user_name\": \"rlong@unicon.net\",
                    \"canvas_user_id\": \"196\",
                    \"canvas_user_global_id\": \"202570000000000196\"
                    }
                }
                }
            ]
            }
        """;
    }

}
