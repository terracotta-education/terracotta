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
package edu.iu.terracotta.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Special service to give access to all the repositories in one place
 * <p/>
 * This is just here to make it a little easier to get access to the full set of repositories instead of always injecting
 * the lot of them (reduces code duplication)
 */
@Component
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("SpringJavaAutowiringInspection")
public class AllRepositories {

    @Autowired public AdminUserRepository adminUserRepository;
    @Autowired public AnswerMcRepository answerMcRepository;
    @Autowired public AnswerEssaySubmissionRepository answerEssaySubmissionRepository;
    @Autowired public AnswerFileSubmissionRepository answerFileSubmissionRepository;
    @Autowired public AnswerMcSubmissionOptionRepository answerMcSubmissionOptionRepository;
    @Autowired public AnswerMcSubmissionRepository answerMcSubmissionRepository;
    @Autowired public AssessmentRepository assessmentRepository;
    @Autowired public ApiOneUseTokenRepository apiOneUseTokenRepository;
    @Autowired public AssignmentRepository assignmentRepository;
    @Autowired public CanvasAPIOAuthSettingsRepository canvasAPIOAuthSettingsRepository;
    @Autowired public CanvasAPITokenRepository canvasAPITokenRepository;
    @Autowired public ConditionRepository conditionRepository;
    @Autowired public ConfigRepository configs;
    @Autowired public ConsentDocumentRepository consentDocumentRepository;
    @Autowired public EventRepository eventRepository;
    @Autowired public ExperimentRepository experimentRepository;
    @Autowired public ExposureGroupConditionRepository exposureGroupConditionRepository;
    @Autowired public ExposureRepository exposureRepository;
    @Autowired public FileInfoRepository fileInfoRepository;
    @Autowired public GroupRepository groupRepository;
    @Autowired public LtiContextRepository contexts;
    @Autowired public LtiLinkRepository links;
    @Autowired public LtiMembershipRepository members;
    @Autowired public LtiResultRepository results;
    @Autowired public LtiMembershipRepository ltiMembershipRepository;
    @Autowired public LtiUserRepository ltiUserRepository;
    @Autowired public OutcomeRepository outcomeRepository;
    @Autowired public OutcomeScoreRepository outcomeScoreRepository;
    @Autowired public ParticipantRepository participantRepository;
    @Autowired public PlatformDeploymentRepository platformDeploymentRepository;
    @Autowired public QuestionMcRepository questionMcRepository;
    @Autowired public QuestionRepository questionRepository;
    @Autowired public QuestionSubmissionCommentRepository questionSubmissionCommentRepository;
    @Autowired public QuestionSubmissionRepository questionSubmissionRepository;
    @Autowired public SubmissionCommentRepository submissionCommentRepository;
    @Autowired public SubmissionRepository submissionRepository;
    @Autowired public ToolDeploymentRepository toolDeploymentRepository;
    @Autowired public TreatmentRepository treatmentRepository;

    @PersistenceContext public EntityManager entityManager;

}
