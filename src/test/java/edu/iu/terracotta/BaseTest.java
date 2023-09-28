package edu.iu.terracotta;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.mockito.Mock;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.RestTemplate;

import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.LtiMembershipEntity;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
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
import edu.iu.terracotta.model.app.dto.TreatmentDto;
import edu.iu.terracotta.model.app.dto.UserDto;
import edu.iu.terracotta.model.app.enumerator.DistributionTypes;
import edu.iu.terracotta.model.app.enumerator.ExposureTypes;
import edu.iu.terracotta.model.app.enumerator.MultipleSubmissionScoringScheme;
import edu.iu.terracotta.model.app.enumerator.ParticipationTypes;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.model.events.Event;
import edu.iu.terracotta.model.membership.CourseUser;
import edu.iu.terracotta.model.membership.CourseUsers;
import edu.iu.terracotta.model.oauth2.LTIToken;
import edu.iu.terracotta.model.oauth2.Roles;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.repository.AnswerEssaySubmissionRepository;
import edu.iu.terracotta.repository.AnswerFileSubmissionRepository;
import edu.iu.terracotta.repository.AnswerMcRepository;
import edu.iu.terracotta.repository.AnswerMcSubmissionOptionRepository;
import edu.iu.terracotta.repository.AnswerMcSubmissionRepository;
import edu.iu.terracotta.repository.AssessmentRepository;
import edu.iu.terracotta.repository.AssignmentRepository;
import edu.iu.terracotta.repository.CanvasAPIOAuthSettingsRepository;
import edu.iu.terracotta.repository.CanvasAPITokenRepository;
import edu.iu.terracotta.repository.ConditionRepository;
import edu.iu.terracotta.repository.EventRepository;
import edu.iu.terracotta.repository.ExperimentRepository;
import edu.iu.terracotta.repository.ExposureGroupConditionRepository;
import edu.iu.terracotta.repository.ExposureRepository;
import edu.iu.terracotta.repository.GroupRepository;
import edu.iu.terracotta.repository.LtiUserRepository;
import edu.iu.terracotta.repository.OutcomeRepository;
import edu.iu.terracotta.repository.OutcomeScoreRepository;
import edu.iu.terracotta.repository.ParticipantRepository;
import edu.iu.terracotta.repository.PlatformDeploymentRepository;
import edu.iu.terracotta.repository.QuestionMcRepository;
import edu.iu.terracotta.repository.QuestionRepository;
import edu.iu.terracotta.repository.QuestionSubmissionCommentRepository;
import edu.iu.terracotta.repository.QuestionSubmissionRepository;
import edu.iu.terracotta.repository.SubmissionRepository;
import edu.iu.terracotta.repository.TreatmentRepository;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AnswerService;
import edu.iu.terracotta.service.app.AnswerSubmissionService;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.ExposureService;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.app.GroupService;
import edu.iu.terracotta.service.app.OutcomeScoreService;
import edu.iu.terracotta.service.app.OutcomeService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.service.app.QuestionService;
import edu.iu.terracotta.service.app.QuestionSubmissionCommentService;
import edu.iu.terracotta.service.app.QuestionSubmissionService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.app.TreatmentService;
import edu.iu.terracotta.service.aws.AWSService;
import edu.iu.terracotta.service.canvas.AssignmentWriterExtended;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;
import edu.iu.terracotta.service.lti.AdvantageAGSService;
import edu.iu.terracotta.service.lti.AdvantageMembershipService;
import edu.iu.terracotta.service.lti.LTIDataService;
import edu.iu.terracotta.utils.lti.LTI3Request;
import edu.ksu.canvas.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings({"rawtypes"})
public class BaseTest {

    public static final String ASSIGNMENT_TITLE = "test assignment title";
    public static final String CONDITION_TITLE = "test condition title";
    public static final String DISPLAY_NAME = "Terracotta User";
    public static final String EMAIL = "abc@terracotta.edu";
    public static final String EXPOSURE_TITLE = "test exposure title";
    public static final String OUTCOME_TITLE = "test outcome title";
    public static final String RESOURCE_LINK_ID = "resource_link_id";
    public static final String USER_ID = "user_id";

    @Mock protected AllRepositories allRepositories;
    @Mock protected AnswerEssaySubmissionRepository answerEssaySubmissionRepository;
    @Mock protected AnswerFileSubmissionRepository answerFileSubmissionRepository;
    @Mock protected AnswerMcRepository answerMcRepository;
    @Mock protected AnswerMcSubmissionOptionRepository answerMcSubmissionOptionRepository;
    @Mock protected AnswerMcSubmissionRepository answerMcSubmissionRepository;
    @Mock protected AssessmentRepository assessmentRepository;
    @Mock protected AssignmentRepository assignmentRepository;
    @Mock protected CanvasAPITokenRepository canvasAPITokenRepository;
    @Mock protected CanvasAPIOAuthSettingsRepository canvasAPIOAuthSettingsRepository;
    @Mock protected ConditionRepository conditionRepository;
    @Mock protected EventRepository eventRepository;
    @Mock protected ExperimentRepository experimentRepository;
    @Mock protected ExposureRepository exposureRepository;
    @Mock protected ExposureGroupConditionRepository exposureGroupConditionRepository;
    @Mock protected GroupRepository groupRepository;
    @Mock protected LtiUserRepository ltiUserRepository;
    @Mock protected OutcomeRepository outcomeRepository;
    @Mock protected OutcomeScoreRepository outcomeScoreRepository;
    @Mock protected ParticipantRepository participantRepository;
    @Mock protected PlatformDeploymentRepository platformDeploymentRepository;
    @Mock protected QuestionMcRepository questionMcRepository;
    @Mock protected QuestionRepository questionRepository;
    @Mock protected QuestionSubmissionCommentRepository questionSubmissionCommentRepository;
    @Mock protected QuestionSubmissionRepository questionSubmissionRepository;
    @Mock protected SubmissionRepository submissionRepository;
    @Mock protected TreatmentRepository treatmentRepository;

    @Mock protected AdvantageAGSService advantageAGSService;
    @Mock protected AdvantageMembershipService advantageMembershipService;
    @Mock protected AnswerService answerService;
    @Mock protected AnswerSubmissionService answerSubmissionService;
    @Mock protected APIJWTService apijwtService;
    @Mock protected AssessmentService assessmentService;
    @Mock protected AssignmentService assignmentService;
    @Mock protected AWSService awsService;
    @Mock protected CanvasAPIClient canvasAPIClient;
    @Mock protected EntityManager entityManager;
    @Mock protected ExperimentService experimentService;
    @Mock protected ExposureService exposureService;
    @Mock protected FileStorageService fileStorageService;
    @Mock protected GroupService groupService;
    @Mock protected LTIDataService ltiDataService;
    @Mock protected OutcomeScoreService outcomeScoreService;
    @Mock protected OutcomeService outcomeService;
    @Mock protected ParticipantService participantService;
    @Mock protected QuestionService questionService;
    @Mock protected QuestionSubmissionCommentService questionSubmissionCommentService;
    @Mock protected QuestionSubmissionService questionSubmissionService;
    @Mock protected SubmissionService submissionService;
    @Mock protected TreatmentService treatmentService;

    @Mock protected AnswerDto answerDto;
    @Mock protected AnswerEssaySubmission answerEssaySubmission;
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
    @Mock protected Claims claims;
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
    @Mock protected InputStream inputStream;
    @Mock protected Jwt<Header, Claims> jwt;
    @Mock protected LineItem lineItem;
    @Mock protected LineItems lineItems;
    @Mock protected LtiContextEntity ltiContextEntity;
    @Mock protected LtiMembershipEntity ltiMembershipEntity;
    @Mock protected LTIToken ltiToken;
    @Mock protected LtiUserEntity ltiUserEntity;
    @Mock protected LTI3Request lti3Request;
    @Mock protected Outcome outcome;
    @Mock protected OutcomeDto outcomeDto;
    @Mock protected OutcomeScore outcomeScore;
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
    @Mock protected SecuredInfo securedInfo;
    @Mock protected Submission submission;
    @Mock protected edu.ksu.canvas.model.assignment.Submission submissionCanvas;
    @Mock protected Treatment treatment;
    @Mock protected TreatmentDto treatmentDto;
    @Mock protected User user;
    @Mock protected UserDto userDto;

    public void setup() {
        allRepositories.answerEssaySubmissionRepository = answerEssaySubmissionRepository;
        allRepositories.answerFileSubmissionRepository = answerFileSubmissionRepository;
        allRepositories.answerMcRepository = answerMcRepository;
        allRepositories.answerMcSubmissionOptionRepository = answerMcSubmissionOptionRepository;
        allRepositories.answerMcSubmissionRepository = answerMcSubmissionRepository;
        allRepositories.assessmentRepository = assessmentRepository;
        allRepositories.assignmentRepository = assignmentRepository;
        allRepositories.canvasAPIOAuthSettingsRepository = canvasAPIOAuthSettingsRepository;
        allRepositories.canvasAPITokenRepository = canvasAPITokenRepository;
        allRepositories.conditionRepository = conditionRepository;
        allRepositories.eventRepository = eventRepository;
        allRepositories.experimentRepository = experimentRepository;
        allRepositories.exposureGroupConditionRepository = exposureGroupConditionRepository;
        allRepositories.exposureRepository = exposureRepository;
        allRepositories.groupRepository = groupRepository;
        allRepositories.ltiUserRepository = ltiUserRepository;
        allRepositories.outcomeRepository = outcomeRepository;
        allRepositories.outcomeScoreRepository = outcomeScoreRepository;
        allRepositories.participantRepository = participantRepository;
        allRepositories.platformDeploymentRepository = platformDeploymentRepository;
        allRepositories.questionMcRepository = questionMcRepository;
        allRepositories.questionRepository = questionRepository;
        allRepositories.questionSubmissionCommentRepository = questionSubmissionCommentRepository;
        allRepositories.questionSubmissionRepository = questionSubmissionRepository;
        allRepositories.submissionRepository = submissionRepository;
        allRepositories.treatmentRepository = treatmentRepository;

        try {
            when(answerMcRepository.save(any(AnswerMc.class))).thenReturn(answerMc);
            when(answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.singletonList(answerEssaySubmission));
            when(answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.singletonList(answerMcSubmission));
            when(assessmentRepository.findByAssessmentId(anyLong())).thenReturn(assessment);
            when(assessmentRepository.findById(anyLong())).thenReturn(Optional.of(assessment));
            when(assessmentRepository.save(any(Assessment.class))).thenReturn(assessment);
            when(assignmentRepository.findByAssignmentId(anyLong())).thenReturn(assignment);
            when(assignmentRepository.findByExposure_Experiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(assignment));
            when(assignmentRepository.findByExposure_ExposureIdAndSoftDeleted(anyLong(), anyBoolean())).thenReturn(Collections.singletonList(assignment));
            when(assignmentRepository.findById(anyLong())).thenReturn(Optional.of(assignment));
            when(assignmentRepository.getReferenceById(anyLong())).thenReturn(assignment);
            when(assignmentRepository.save(any(Assignment.class))).thenReturn(assignment);
            when(assignmentRepository.saveAndFlush(any(Assignment.class))).thenReturn(assignment);
            when(conditionRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(condition));
            when(conditionRepository.findById(anyLong())).thenReturn(Optional.of(condition));
            when(experimentRepository.findByExperimentId(anyLong())).thenReturn(experiment);
            when(experimentRepository.findById(anyLong())).thenReturn(Optional.of(experiment));
            when(exposureGroupConditionRepository.findByCondition_Experiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(exposureGroupCondition));
            when(exposureGroupConditionRepository.findByGroup_GroupId(anyLong())).thenReturn(Collections.singletonList(exposureGroupCondition));
            when(exposureRepository.findById(anyLong())).thenReturn(Optional.of(exposure));
            when(groupRepository.getReferenceById(anyLong())).thenReturn(group);
            when(ltiUserRepository.findById(anyLong())).thenReturn(Optional.of(ltiUserEntity));
            when(ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong())).thenReturn(ltiUserEntity);
            when(outcomeRepository.findByExposure_Experiment_ExperimentId(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(Collections.singletonList(outcome)));
            when(outcomeRepository.findByExposure_ExposureId(anyLong())).thenReturn(Collections.singletonList(outcome));
            when(outcomeRepository.findById(anyLong())).thenReturn(Optional.of(outcome));
            when(outcomeRepository.findByOutcomeId(anyLong())).thenReturn(outcome);
            when(outcomeRepository.save(any(Outcome.class))).thenReturn(outcome);
            when(outcomeScoreRepository.findByOutcome_OutcomeId(anyLong())).thenReturn(Collections.singletonList(outcomeScore));
            when(participantRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(participant));
            when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(participant);
            when(participantRepository.findById(anyLong())).thenReturn(Optional.of(participant));
            when(participantRepository.findByParticipantId(anyLong())).thenReturn(participant);
            when(participantRepository.save(any(Participant.class))).thenReturn(participant);
            when(platformDeploymentRepository.getReferenceById(anyLong())).thenReturn(platformDeployment);
            when(questionRepository.save(any(Question.class))).thenReturn(question);
            when(questionRepository.save(any(QuestionMc.class))).thenReturn(questionMc);
            when(questionSubmissionRepository.save(any(QuestionSubmission.class))).thenReturn(questionSubmission);
            when(submissionRepository.findById(anyLong())).thenReturn(Optional.of(submission));
            when(submissionRepository.findByParticipant_ParticipantId(anyLong())).thenReturn(Collections.singletonList(submission));
            when(submissionRepository.findBySubmissionId(anyLong())).thenReturn(submission);
            when(submissionRepository.save(any(Submission.class))).thenReturn(submission);
            when(treatmentRepository.findByCondition_ConditionIdAndAssignment_AssignmentId(anyLong(), anyLong())).thenReturn(Collections.singletonList(treatment));
            when(treatmentRepository.findByCondition_Experiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(treatment));
            when(treatmentRepository.findById(anyLong())).thenReturn(Optional.of(treatment));
            when(treatmentRepository.findByTreatmentId(anyLong())).thenReturn(treatment);
            when(treatmentRepository.save(any(Treatment.class))).thenReturn(treatment);
            when(treatmentRepository.saveAndFlush(any(Treatment.class))).thenReturn(treatment);

            when(advantageAGSService.getLineItems(any(LTIToken.class), any(LtiContextEntity.class))).thenReturn(lineItems);
            when(advantageAGSService.getToken(anyString(), any(PlatformDeployment.class))).thenReturn(ltiToken);
            when(advantageMembershipService.callMembershipService(any(LTIToken.class), any(LtiContextEntity.class))).thenReturn(courseUsers);
            when(advantageMembershipService.getToken(any(PlatformDeployment.class))).thenReturn(ltiToken);
            when(answerService.postAnswerMC(any(AnswerDto.class), anyLong())).thenReturn(answerDto);
            when(apijwtService.isTestStudent(any(SecuredInfo.class))).thenReturn(false);
            when(apijwtService.unsecureToken(anyString())).thenReturn(jwt);
            when(assignmentService.save(any(Assignment.class))).thenReturn(assignment);
            when(canvasAPIClient.listAssignment(any(LtiUserEntity.class), anyString(), anyInt())).thenReturn(Optional.of(assignmentExtended));
            when(canvasAPIClient.listAssignments(any(LtiUserEntity.class), anyString())).thenReturn(Collections.singletonList(assignmentExtended));
            when(canvasAPIClient.listSubmissions(any(LtiUserEntity.class), anyInt(), anyString())).thenReturn(Collections.singletonList(submissionCanvas));
            when(experimentService.findById(anyLong())).thenReturn(Optional.of(experiment));
            when(experimentService.getExperiment(anyLong())).thenReturn(experiment);
            when(exposureService.findById(anyLong())).thenReturn(Optional.of(exposure));
            when(ltiDataService.getAllRepositories()).thenReturn(allRepositories);
            when(participantService.refreshParticipants(anyLong(), anyList())).thenReturn(Collections.singletonList(participant));
            when(questionService.getQuestion(anyLong())).thenReturn(question);
            when(questionService.postQuestion(any(QuestionDto.class), anyLong(), anyBoolean())).thenReturn(questionDto);
            when(questionService.save(any(Question.class))).thenReturn(question);
            when(submissionService.findByParticipantId(anyLong())).thenReturn(Collections.singletonList(submission));
            when(submissionService.findByParticipantIdAndAssessmentId(anyLong(), anyLong())).thenReturn(Collections.singletonList(submission));
            when(submissionService.getScoreFromMultipleSubmissions(any(Participant.class), any(Assessment.class))).thenReturn(1F);
            when(submissionService.getSubmissionScore(any(Submission.class))).thenReturn(1F);
            when(submissionService.gradeSubmission(any(Submission.class), any(RegradeDetails.class))).thenReturn(submission);

            when(answerEssaySubmission.getQuestionSubmission()).thenReturn(questionSubmission);
            when(answerMc.getAnswerMcId()).thenReturn(1L);
            when(answerMc.getCorrect()).thenReturn(true);
            when(answerMc.getQuestion()).thenReturn(questionMc);
            when(answerMcSubmission.getAnswerMc()).thenReturn(answerMc);
            when(answerMcSubmissionOption.getAnswerMc()).thenReturn(answerMc);
            when(answerMcSubmissionOption.getAnswerOrder()).thenReturn(0);
            when(assessment.getAssessmentId()).thenReturn(1L);
            when(assessment.getMultipleSubmissionScoringScheme()).thenReturn(MultipleSubmissionScoringScheme.MOST_RECENT);
            when(assessment.canViewCorrectAnswers()).thenReturn(true);
            when(assessment.canViewResponses()).thenReturn(true);
            when(assessment.getTreatment()).thenReturn(treatment);
            when(assessmentDto.getAssessmentId()).thenReturn(1L);
            when(assessmentDto.getTreatmentId()).thenReturn(1L);
            when(assignment.getAssignmentId()).thenReturn(1L);
            when(assignment.getExposure()).thenReturn(exposure);
            when(assignment.getLmsAssignmentId()).thenReturn("1");
            when(assignment.getSoftDeleted()).thenReturn(false);
            when(assignment.getTitle()).thenReturn(ASSIGNMENT_TITLE);
            when(assignmentDto.getAssignmentId()).thenReturn(1L);
            when(assignmentExtended.getId()).thenReturn(1);
            when(assignmentExtended.getSecureParams()).thenReturn(RESOURCE_LINK_ID);
            when(claims.get(anyString())).thenReturn(RESOURCE_LINK_ID);
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
            when(experiment.getDistributionType()).thenReturn(DistributionTypes.CUSTOM);
            when(experiment.getExperimentId()).thenReturn(1L);
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
            when(jwt.getBody()).thenReturn(claims);
            when(lineItem.getId()).thenReturn("1");
            when(lineItem.getResourceLinkId()).thenReturn(RESOURCE_LINK_ID);
            when(lineItems.getLineItemList()).thenReturn(Collections.singletonList(lineItem));
            when(ltiContextEntity.getContextId()).thenReturn(1l);
            when(ltiContextEntity.getContext_memberships_url()).thenReturn("courses/1/names");
            when(ltiMembershipEntity.getUser()).thenReturn(ltiUserEntity);
            when(ltiUserEntity.getDisplayName()).thenReturn(DISPLAY_NAME);
            when(ltiUserEntity.getEmail()).thenReturn(EMAIL);
            when(ltiUserEntity.getPlatformDeployment()).thenReturn(platformDeployment);
            when(ltiUserEntity.getUserKey()).thenReturn(USER_ID);
            when(outcome.getExposure()).thenReturn(exposure);
            when(outcome.getLmsOutcomeId()).thenReturn("1");
            when(outcome.getMaxPoints()).thenReturn(1F);
            when(outcome.getOutcomeId()).thenReturn(1L);
            when(outcome.getOutcomeScores()).thenReturn(Collections.singletonList(outcomeScore));
            when(outcomeDto.getExternal()).thenReturn(false);
            when(outcomeDto.getTitle()).thenReturn(OUTCOME_TITLE);
            when(outcomeScore.getOutcome()).thenReturn(outcome);
            when(outcomeScore.getParticipant()).thenReturn(participant);
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
            when(question.getPoints()).thenReturn(1F);
            when(question.getQuestionId()).thenReturn(1L);
            when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);
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
            when(securedInfo.getCanvasAssignmentId()).thenReturn("canvasAssignmentId");
            when(securedInfo.getCanvasCourseId()).thenReturn("1");
            when(securedInfo.getCanvasUserId()).thenReturn("1");
            when(securedInfo.getPlatformDeploymentId()).thenReturn(1L);
            when(securedInfo.getUserId()).thenReturn(USER_ID);
            when(submission.getAlteredCalculatedGrade()).thenReturn(1F);
            when(submission.getAssessment()).thenReturn(assessment);
            when(submission.getCalculatedGrade()).thenReturn(1F);
            when(submission.getDateSubmitted()).thenReturn(Timestamp.from(Instant.now()));
            when(submission.getParticipant()).thenReturn(participant);
            when(submission.getQuestionSubmissions()).thenReturn(Collections.singletonList(questionSubmission));
            when(submission.getSubmissionId()).thenReturn(1L);
            when(submission.getTotalAlteredGrade()).thenReturn(1F);
            when(submissionCanvas.getScore()).thenReturn(1.0D);
            when(submissionCanvas.getUser()).thenReturn(user);
            when(treatment.getAssessment()).thenReturn(assessment);
            when(treatment.getAssignment()).thenReturn(assignment);
            when(treatment.getCondition()).thenReturn(condition);
            when(treatment.getTreatmentId()).thenReturn(1L);
            when(user.getId()).thenReturn(1);
            when(user.getLoginId()).thenReturn(EMAIL);
            when(user.getName()).thenReturn(DISPLAY_NAME);
            when(userDto.getUserId()).thenReturn(1L);
        } catch (Exception e) {
            log.error("Exception occurred in test setup()", e);
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
