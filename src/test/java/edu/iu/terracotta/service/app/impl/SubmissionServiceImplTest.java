package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.AnswerMcSubmission;
import edu.iu.terracotta.dao.entity.AnswerMcSubmissionOption;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.Group;
import edu.iu.terracotta.dao.entity.QuestionSubmission;
import edu.iu.terracotta.dao.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.dao.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.dao.model.dto.SubmissionDto;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.dao.model.enums.RegradeOption;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;

public class SubmissionServiceImplTest extends BaseTest {

    @InjectMocks private SubmissionServiceImpl submissionService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
        clearInvocations(assignmentRepository);

        when(answerMcRepository.findByQuestion_QuestionId(anyLong())).thenReturn(Collections.singletonList(answerMc));
        when(answerMcSubmissionOptionRepository.save(any(AnswerMcSubmissionOption.class))).thenReturn(null);
        when(participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(participant);

        when(answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.singletonList(answerMcSubmission));
        when(questionSubmissionService.automaticGradingMC(any(QuestionSubmission.class), any(AnswerMcSubmission.class))).thenReturn(questionSubmission);

        when(answerMc.getCorrect()).thenReturn(true);
        when(assessment.getQuestions()).thenReturn(Collections.singletonList(question));
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);
        when(questionMc.getPoints()).thenReturn(10F);
        when(regradeDetails.getEditedMCQuestionIds()).thenReturn(Collections.singletonList(1L));
        when(regradeDetails.getRegradeOption()).thenReturn(RegradeOption.FULL);
    }

    @Test
    public void testPostSubmissionNotStarted() throws IdInPostException, ParticipantNotMatchingException, InvalidUserException, DataServiceException, IntegrationTokenNotFoundException {
        submissionService.postSubmission(SubmissionDto.builder().build(), 0l, securedInfo, 0l, false);

        verify(assignmentRepository).save(assignment);
    }

    @Test
    public void testPostSubmissionAlreadyStarted() throws IdInPostException, ParticipantNotMatchingException, InvalidUserException, DataServiceException, IntegrationTokenNotFoundException {
        when(assignment.isStarted()).thenReturn(true);
        submissionService.postSubmission(SubmissionDto.builder().build(), 0l, securedInfo, 0l, false);

        verify(assignmentRepository, never()).save(assignment);
    }

    @Test
    public void testCreateNewSubmissionNotStarted() throws IdInPostException, ParticipantNotMatchingException, InvalidUserException, DataServiceException, IntegrationTokenNotFoundException {
        when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);

        submissionService.createNewSubmission(assessment, participant, securedInfo);

        verify(assignmentRepository).save(assignment);
    }

    @Test
    public void testCreateNewSubmissionAlreadyStarted() throws IdInPostException, ParticipantNotMatchingException, InvalidUserException, DataServiceException, IntegrationTokenNotFoundException {
        when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);
        when(assignment.isStarted()).thenReturn(true);
        submissionService.createNewSubmission(assessment, participant, securedInfo);

        verify(assignmentRepository, never()).save(assignment);
    }

    @Test
    public void testCreateNewSubmissionTestStudent() throws IdInPostException, ParticipantNotMatchingException, InvalidUserException, DataServiceException, IntegrationTokenNotFoundException {
        when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);
        when(apiJwtService.isTestStudent(any(SecuredInfo.class))).thenReturn(true);
        submissionService.createNewSubmission(assessment, participant, securedInfo);

        verify(assignmentRepository, never()).save(assignment);
    }

    // test toDto when questionSubmissions is true and not submitted, calls
    // QuestionSubmissionService with answerSubmissions=true
    @Test
    public void testToDtoWithQuestionSubmissionsWhenSubmissionNotSubmitted() throws IOException {

        when(submission.getDateSubmitted()).thenReturn(null);

        List<QuestionSubmission> questionSubmissions = new ArrayList<>();
        QuestionSubmission qs1 = new QuestionSubmission();
        QuestionSubmission qs2 = new QuestionSubmission();
        questionSubmissions.add(qs1);
        questionSubmissions.add(qs2);

        when(questionSubmissionRepository.findBySubmission_SubmissionId(anyLong())).thenReturn(questionSubmissions);

        submissionService.toDto(submission, true, false);

        verify(questionSubmissionService).toDto(qs1, true, false);
        verify(questionSubmissionService).toDto(qs2, true, false);
    }

    // test toDto when questionSubmissions is true and submitted, calls
    // QuestionSubmissionService with answerSubmissions=false
    @Test
    public void testToDtoWithQuestionSubmissionsWhenSubmissionIsSubmitted() throws IOException {

        when(submission.getDateSubmitted()).thenReturn(new Timestamp(System.currentTimeMillis()));

        List<QuestionSubmission> questionSubmissions = new ArrayList<>();
        QuestionSubmission qs1 = new QuestionSubmission();
        QuestionSubmission qs2 = new QuestionSubmission();
        questionSubmissions.add(qs1);
        questionSubmissions.add(qs2);

        when(questionSubmissionRepository.findBySubmission_SubmissionId(anyLong())).thenReturn(questionSubmissions);

        submissionService.toDto(submission, true, false);

        verify(questionSubmissionService).toDto(qs1, false, false);
        verify(questionSubmissionService).toDto(qs2, false, false);
    }

    // test allowedSubmission that when participant has revoked consent but is
    // assigned to a group and submission is for default treatment, should NOT
    // throw exception
    @Test
    public void testAllowedSubmissionWithConsentRevokedAndGroupAssignmentAndDefaultTreatment() {
        when(participant.getConsent()).thenReturn(false);
        Group group1 = new Group();
        group1.setGroupId(1L);
        when(participant.getGroup()).thenReturn(group1);
        when(condition.getDefaultCondition()).thenReturn(true);

        ExposureGroupCondition exposureGroupCondition = new ExposureGroupCondition();
        Group group2 = new Group();
        group2.setGroupId(2L);
        exposureGroupCondition.setGroup(group2);
        // Don't expect this to be called, but for completeness this is stubbed out
        when(exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.of(exposureGroupCondition));
        assertNotEquals(group1, group2);

        assertDoesNotThrow(() -> {
            submissionService.allowedSubmission(1l, securedInfo);
        });
    }

    // test allowedSubmission that when participant has revoked consent but is
    // assigned to a group and submission is NOT for default treatment, SHOULD
    // throw exception
    @Test
    public void testAllowedSubmissionWithConsentRevokedAndGroupAssignmentAndNotDefaultTreatment() {
        when(participant.getConsent()).thenReturn(false);
        Group group1 = new Group();
        group1.setGroupId(1L);
        when(participant.getGroup()).thenReturn(group1);
        when(condition.getDefaultCondition()).thenReturn(false);

        ExposureGroupCondition exposureGroupCondition = new ExposureGroupCondition();
        Group group2 = new Group();
        group2.setGroupId(2L);
        exposureGroupCondition.setGroup(group2);
        // Don't expect this to be called, but for completeness this is stubbed out
        when(exposureGroupConditionRepository.getByCondition_ConditionIdAndExposure_ExposureId(anyLong(), anyLong())).thenReturn(Optional.of(exposureGroupCondition));
        assertNotEquals(group1, group2);

        assertThrows(SubmissionNotMatchingException.class, () -> {
            submissionService.allowedSubmission(1l, securedInfo);
        });
    }

}
