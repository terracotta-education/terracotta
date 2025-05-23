package edu.iu.terracotta.base;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiScope;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.repository.api.ApiOAuthSettingsRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.api.ApiOneUseTokenRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.api.ApiScopeRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.api.ApiTokenRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiContextRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiLinkRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiMembershipRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.ToolDeploymentRepository;
import edu.iu.terracotta.dao.entity.AnswerMc;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Outcome;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.Question;
import edu.iu.terracotta.dao.entity.QuestionMc;
import edu.iu.terracotta.dao.entity.QuestionSubmission;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.entity.integrations.Integration;
import edu.iu.terracotta.dao.entity.integrations.IntegrationConfiguration;
import edu.iu.terracotta.dao.entity.integrations.IntegrationToken;
import edu.iu.terracotta.dao.repository.AnswerEssaySubmissionRepository;
import edu.iu.terracotta.dao.repository.AnswerFileSubmissionRepository;
import edu.iu.terracotta.dao.repository.AnswerMcRepository;
import edu.iu.terracotta.dao.repository.AnswerMcSubmissionOptionRepository;
import edu.iu.terracotta.dao.repository.AnswerMcSubmissionRepository;
import edu.iu.terracotta.dao.repository.AssessmentRepository;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ConditionRepository;
import edu.iu.terracotta.dao.repository.ConsentDocumentRepository;
import edu.iu.terracotta.dao.repository.EventRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.ExposureGroupConditionRepository;
import edu.iu.terracotta.dao.repository.ExposureRepository;
import edu.iu.terracotta.dao.repository.GroupRepository;
import edu.iu.terracotta.dao.repository.ObsoleteAssignmentRepository;
import edu.iu.terracotta.dao.repository.OutcomeRepository;
import edu.iu.terracotta.dao.repository.OutcomeScoreRepository;
import edu.iu.terracotta.dao.repository.ParticipantRepository;
import edu.iu.terracotta.dao.repository.QuestionMcRepository;
import edu.iu.terracotta.dao.repository.QuestionRepository;
import edu.iu.terracotta.dao.repository.QuestionSubmissionCommentRepository;
import edu.iu.terracotta.dao.repository.QuestionSubmissionRepository;
import edu.iu.terracotta.dao.repository.SubmissionRepository;
import edu.iu.terracotta.dao.repository.TreatmentRepository;
import edu.iu.terracotta.dao.repository.distribute.ExperimentImportRepository;
import edu.iu.terracotta.dao.repository.integrations.AnswerIntegrationSubmissionRepository;
import edu.iu.terracotta.dao.repository.integrations.IntegrationClientRepository;
import edu.iu.terracotta.dao.repository.integrations.IntegrationConfigurationRepository;
import edu.iu.terracotta.dao.repository.integrations.IntegrationRepository;
import edu.iu.terracotta.dao.repository.integrations.IntegrationTokenLogRepository;
import edu.iu.terracotta.dao.repository.integrations.IntegrationTokenRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseRepositoryTest extends BaseModelTest {

    @Mock protected AnswerEssaySubmissionRepository answerEssaySubmissionRepository;
    @Mock protected AnswerFileSubmissionRepository answerFileSubmissionRepository;
    @Mock protected AnswerIntegrationSubmissionRepository answerIntegrationSubmissionRepository;
    @Mock protected AnswerMcRepository answerMcRepository;
    @Mock protected AnswerMcSubmissionOptionRepository answerMcSubmissionOptionRepository;
    @Mock protected AnswerMcSubmissionRepository answerMcSubmissionRepository;
    @Mock protected ApiOneUseTokenRepository apiOneUseTokenRepository;
    @Mock protected ApiScopeRepository apiScopeRepository;
    @Mock protected ApiTokenRepository apiTokenRepository;
    @Mock protected ApiOAuthSettingsRepository apiOAuthSettingsRepository;
    @Mock protected AssessmentRepository assessmentRepository;
    @Mock protected AssignmentRepository assignmentRepository;
    @Mock protected ConditionRepository conditionRepository;
    @Mock protected ConsentDocumentRepository consentDocumentRepository;
    @Mock protected ExperimentImportRepository experimentImportRepository;
    @Mock protected EventRepository eventRepository;
    @Mock protected ExperimentRepository experimentRepository;
    @Mock protected ExposureRepository exposureRepository;
    @Mock protected ExposureGroupConditionRepository exposureGroupConditionRepository;
    @Mock protected GroupRepository groupRepository;
    @Mock protected IntegrationClientRepository integrationClientRepository;
    @Mock protected IntegrationConfigurationRepository integrationConfigurationRepository;
    @Mock protected IntegrationRepository integrationRepository;
    @Mock protected IntegrationTokenLogRepository integrationTokenLogRepository;
    @Mock protected IntegrationTokenRepository integrationTokenRepository;
    @Mock protected LtiContextRepository ltiContextRepository;
    @Mock protected LtiLinkRepository ltiLinkRepository;
    @Mock protected LtiMembershipRepository ltiMembershipRepository;
    @Mock protected LtiUserRepository ltiUserRepository;
    @Mock protected ObsoleteAssignmentRepository obsoleteAssignmentRepository;
    @Mock protected OutcomeRepository outcomeRepository;
    @Mock protected OutcomeScoreRepository outcomeScoreRepository;
    @Mock protected ParticipantRepository participantRepository;
    @Mock protected PlatformDeploymentRepository platformDeploymentRepository;
    @Mock protected QuestionMcRepository questionMcRepository;
    @Mock protected QuestionRepository questionRepository;
    @Mock protected QuestionSubmissionCommentRepository questionSubmissionCommentRepository;
    @Mock protected QuestionSubmissionRepository questionSubmissionRepository;
    @Mock protected SubmissionRepository submissionRepository;
    @Mock protected ToolDeploymentRepository toolDeploymentRepository;
    @Mock protected TreatmentRepository treatmentRepository;

    public void setup() {
        try {
            super.setup();

            when(answerIntegrationSubmissionRepository.existsByQuestionSubmission_QuestionSubmissionIdAndId(anyLong(), anyLong())).thenReturn(true);
            when(answerIntegrationSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.singletonList(answerIntegrationSubmission));
            when(answerMcRepository.save(any(AnswerMc.class))).thenReturn(answerMc);
            when(answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.singletonList(answerEssaySubmission));
            when(answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.singletonList(answerMcSubmission));
            when(apiOAuthSettingsRepository.findByPlatformDeployment(any(PlatformDeployment.class))).thenReturn(Optional.of(apiOAuthSettings));
            when(apiOneUseTokenRepository.findByToken(anyString())).thenReturn(apiOneUseToken);
            when(apiScopeRepository.findAll()).thenReturn(Collections.singletonList(apiScope));
            when(apiScopeRepository.findAllByFeatures_Id(anyLong())).thenReturn(Collections.singletonList(apiScope));
            when(apiScopeRepository.findById(anyLong())).thenReturn(Optional.of(apiScope));
            when(apiScopeRepository.findByUuid(any(UUID.class))).thenReturn(Optional.of(apiScope));
            when(apiScopeRepository.save(any(ApiScope.class))).thenReturn(apiScope);
            when(apiTokenRepository.findByUser(any(LtiUserEntity.class))).thenReturn(Optional.of(apiTokenEntity));
            when(assessmentRepository.findByAssessmentId(anyLong())).thenReturn(assessment);
            when(assessmentRepository.findById(anyLong())).thenReturn(Optional.of(assessment));
            when(assessmentRepository.save(any(Assessment.class))).thenReturn(assessment);
            when(assignmentRepository.findAssignmentsToCheckByContext(anyLong())).thenReturn(Collections.singletonList(assignment));
            when(assignmentRepository.findByAssignmentId(anyLong())).thenReturn(assignment);
            when(assignmentRepository.findByExposure_Experiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(assignment));
            when(assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(anyLong(), anyString())).thenReturn(assignment);
            when(assignmentRepository.findByExposure_ExposureIdAndSoftDeleted(anyLong(), anyBoolean())).thenReturn(Collections.singletonList(assignment));
            when(assignmentRepository.findById(anyLong())).thenReturn(Optional.of(assignment));
            when(assignmentRepository.getReferenceById(anyLong())).thenReturn(assignment);
            when(assignmentRepository.save(any(Assignment.class))).thenReturn(assignment);
            when(assignmentRepository.saveAndFlush(any(Assignment.class))).thenReturn(assignment);
            when(conditionRepository.findByExperiment_ExperimentIdOrderByConditionIdAsc(anyLong())).thenReturn(Collections.singletonList(condition));
            when(conditionRepository.findById(anyLong())).thenReturn(Optional.of(condition));
            when(entityManager.createQuery(anyString())).thenReturn(query);
            when(experimentRepository.findAllByLtiContextEntity_ContextId(anyLong())).thenReturn(Collections.singletonList(experiment));
            when(experimentRepository.findByExperimentId(anyLong())).thenReturn(experiment);
            when(experimentRepository.findById(anyLong())).thenReturn(Optional.of(experiment));
            when(exposureGroupConditionRepository.findByCondition_Experiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(exposureGroupCondition));
            when(exposureGroupConditionRepository.findByExposure_ExposureId(anyLong())).thenReturn(Collections.singletonList(exposureGroupCondition));
            when(exposureGroupConditionRepository.findByGroup_GroupId(anyLong())).thenReturn(Collections.singletonList(exposureGroupCondition));
            when(exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.of(exposureGroupCondition));
            when(exposureRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(exposure));
            when(exposureRepository.findByExposureId(anyLong())).thenReturn(exposure);
            when(exposureRepository.findById(anyLong())).thenReturn(Optional.of(exposure));
            when(groupRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(group));
            when(groupRepository.getReferenceById(anyLong())).thenReturn(group);
            when(integrationConfigurationRepository.findById(anyLong())).thenReturn(Optional.of(integrationConfiguration));
            when(integrationConfigurationRepository.save(any(IntegrationConfiguration.class))).thenReturn(integrationConfiguration);
            when(integrationConfigurationRepository.saveAndFlush(any(IntegrationConfiguration.class))).thenReturn(integrationConfiguration);
            when(integrationClientRepository.findByUuid(any(UUID.class))).thenReturn(Optional.of(integrationClient));
            when(integrationClientRepository.findByPreviewToken(anyString())).thenReturn(Optional.of(integrationClient));
            when(integrationClientRepository.getAllByEnabled(anyBoolean())).thenReturn(Collections.singletonList(integrationClient));
            when(integrationRepository.existsByUuidAndQuestion_QuestionId(any(UUID.class), anyLong())).thenReturn(true);
            when(integrationRepository.findById(anyLong())).thenReturn(Optional.of(integration));
            when(integrationRepository.findByUuid(any(UUID.class))).thenReturn(Optional.of(integration));
            when(integrationRepository.save(any(Integration.class))).thenReturn(integration);
            when(integrationRepository.saveAndFlush(any(Integration.class))).thenReturn(integration);
            when(integrationTokenLogRepository.findAllByIntegrationToken_Id(anyLong())).thenReturn(Collections.singletonList(integrationTokenLog));
            when(integrationTokenLogRepository.findByCode(anyString())).thenReturn(Optional.of(integrationTokenLog));
            when(integrationTokenRepository.findByToken(anyString())).thenReturn(Optional.of(integrationToken));
            when(integrationTokenRepository.save(any(IntegrationToken.class))).thenReturn(integrationToken);
            when(integrationTokenRepository.saveAndFlush(any(IntegrationToken.class))).thenReturn(integrationToken);
            when(ltiContextRepository.findById(anyLong())).thenReturn(Optional.of(ltiContextEntity));
            when(ltiUserRepository.findById(anyLong())).thenReturn(Optional.of(ltiUserEntity));
            when(ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong())).thenReturn(ltiUserEntity);
            when(obsoleteAssignmentRepository.findAllByContext_ContextId(anyLong())).thenReturn(Collections.singletonList(obsoleteAssignment));
            when(outcomeRepository.findByExposure_Experiment_ExperimentId(anyLong(), any(Pageable.class))).thenReturn(new PageImpl<>(Collections.singletonList(outcome)));
            when(outcomeRepository.findByExposure_Experiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(outcome));
            when(outcomeRepository.findByExposure_ExposureId(anyLong())).thenReturn(Collections.singletonList(outcome));
            when(outcomeRepository.findById(anyLong())).thenReturn(Optional.of(outcome));
            when(outcomeRepository.findByOutcomeId(anyLong())).thenReturn(outcome);
            when(outcomeRepository.save(any(Outcome.class))).thenReturn(outcome);
            when(outcomeScoreRepository.findByOutcome_OutcomeId(anyLong())).thenReturn(Collections.singletonList(outcomeScore));
            when(participantRepository.countByGroup_GroupId(anyLong())).thenReturn(1L);
            when(participantRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(participant));
            when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(participant);
            when(participantRepository.findById(anyLong())).thenReturn(Optional.of(participant));
            when(participantRepository.findByParticipantId(anyLong())).thenReturn(participant);
            when(participantRepository.save(any(Participant.class))).thenReturn(participant);
            when(platformDeploymentRepository.findById(anyLong())).thenReturn(Optional.of(platformDeployment));
            when(platformDeploymentRepository.findByIssAndClientId(anyString(), anyString())).thenReturn(Collections.singletonList(platformDeployment));
            when(platformDeploymentRepository.getReferenceById(anyLong())).thenReturn(platformDeployment);
            when(questionRepository.findByQuestionId(anyLong())).thenReturn(question);
            when(questionRepository.save(any(Question.class))).thenReturn(question);
            when(questionRepository.save(any(QuestionMc.class))).thenReturn(questionMc);
            when(questionSubmissionRepository.save(any(QuestionSubmission.class))).thenReturn(questionSubmission);
            when(submissionRepository.findById(anyLong())).thenReturn(Optional.of(submission));
            when(submissionRepository.findByParticipant_ParticipantId(anyLong())).thenReturn(Collections.singletonList(submission));
            when(submissionRepository.findByParticipant_ParticipantIdAndAssessment_AssessmentId(anyLong(), anyLong())).thenReturn(Collections.singletonList(submission));
            when(submissionRepository.findBySubmissionId(anyLong())).thenReturn(submission);
            when(submissionRepository.save(any(Submission.class))).thenReturn(submission);
            when(toolDeploymentRepository.findByPlatformDeployment_IssAndPlatformDeployment_ClientIdAndLtiDeploymentId(anyString(), anyString(), anyString())).thenReturn(Collections.singletonList(toolDeployment));
            when(treatmentRepository.findByAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(anyLong())).thenReturn(Collections.singletonList(treatment));
            when(treatmentRepository.findByCondition_ConditionIdAndAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(anyLong(), anyLong())).thenReturn(Collections.singletonList(treatment));
            when(treatmentRepository.findByCondition_Experiment_ExperimentIdOrderByCondition_ConditionIdAsc(anyLong())).thenReturn(Collections.singletonList(treatment));
            when(treatmentRepository.findById(anyLong())).thenReturn(Optional.of(treatment));
            when(treatmentRepository.findByTreatmentId(anyLong())).thenReturn(treatment);
            when(treatmentRepository.save(any(Treatment.class))).thenReturn(treatment);
            when(treatmentRepository.saveAndFlush(any(Treatment.class))).thenReturn(treatment);
        } catch (Exception e) {
            log.error("Exception occurred in BaseRepositoryTest setup()", e);
        }
    }

}
