package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.AnswerMcSubmission;
import edu.iu.terracotta.model.app.AnswerMcSubmissionOption;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import edu.iu.terracotta.model.app.enumerator.RegradeOption;

public class AssessmentSubmissionServiceImplTest extends BaseTest {

    @InjectMocks private AssessmentSubmissionServiceImpl assessmentSubmissionService;

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
    public void testRegradeFull() throws DataServiceException {
        assessmentSubmissionService.gradeSubmission(submission, regradeDetails);

        verify(questionSubmissionRepository).save(any(QuestionSubmission.class));
        verify(submissionRepository).save(submission);
    }

    @Test
    public void testRegradeFullIdNotFound() throws DataServiceException {
        when(question.getQuestionId()).thenReturn(2L);
        assessmentSubmissionService.gradeSubmission(submission, regradeDetails);

        verify(questionSubmission, never()).setCalculatedPoints(anyFloat());
        verify(questionSubmission, never()).setAlteredGrade(null);
        verify(questionSubmissionRepository, never()).save(any(QuestionSubmission.class));
        verify(submissionRepository).save(submission);
    }

    @Test
    public void testRegradeBoth() throws DataServiceException {
        when(regradeDetails.getRegradeOption()).thenReturn(RegradeOption.BOTH);
        assessmentSubmissionService.gradeSubmission(submission, regradeDetails);

        verify(questionSubmissionRepository).save(any(QuestionSubmission.class));
        verify(submissionRepository).save(submission);
    }

    @Test
    public void testRegradeBothIdNotFound() throws DataServiceException {
        when(question.getQuestionId()).thenReturn(2L);
        assessmentSubmissionService.gradeSubmission(submission, regradeDetails);

        verify(questionSubmission, never()).setAlteredGrade(null);
        verify(questionSubmissionRepository, never()).save(any(QuestionSubmission.class));
        verify(submissionRepository).save(submission);
    }

    @Test
    public void testRegradeBothAnswerIncorrect() throws DataServiceException {
        when(regradeDetails.getRegradeOption()).thenReturn(RegradeOption.BOTH);
        when(answerMc.getCorrect()).thenReturn(false);
        when(questionSubmission.getCalculatedPoints()).thenReturn(0F);
        assessmentSubmissionService.gradeSubmission(submission, regradeDetails);

        verify(questionSubmission, never()).setCalculatedPoints(anyFloat());
        verify(questionSubmission).setAlteredGrade(null);
        verify(questionSubmissionRepository).save(any(QuestionSubmission.class));
        verify(submissionRepository).save(submission);
    }

    @Test
    public void testRegradeCurrent() throws DataServiceException {
        when(regradeDetails.getRegradeOption()).thenReturn(RegradeOption.CURRENT);
        assessmentSubmissionService.gradeSubmission(submission, regradeDetails);

        verify(questionSubmissionRepository, never()).save(any(QuestionSubmission.class));
        verify(questionSubmissionService).automaticGradingMC(any(QuestionSubmission.class), any(AnswerMcSubmission.class));
        verify(submissionRepository).save(submission);
    }

    @Test
    public void testRegradeCurrentIdNotFound() throws DataServiceException {
        when(question.getQuestionId()).thenReturn(2L);
        assessmentSubmissionService.gradeSubmission(submission, regradeDetails);

        verify(questionSubmissionRepository, never()).save(any(QuestionSubmission.class));
        verify(submissionRepository).save(submission);
    }

    @Test
    public void testRegradeNone() throws DataServiceException {
        when(regradeDetails.getRegradeOption()).thenReturn(RegradeOption.NONE);
        assessmentSubmissionService.gradeSubmission(submission, regradeDetails);

        verify(questionSubmissionRepository, never()).save(any(QuestionSubmission.class));
        verify(questionSubmissionService, never()).automaticGradingMC(any(QuestionSubmission.class), any(AnswerMcSubmission.class));
        verify(submissionRepository).save(submission);
    }

    @Test
    public void testRegradeNA() throws DataServiceException {
        when(regradeDetails.getRegradeOption()).thenReturn(RegradeOption.NA);

        assessmentSubmissionService.gradeSubmission(submission, regradeDetails);

        verify(questionSubmissionRepository, never()).save(any(QuestionSubmission.class));
        verify(questionSubmissionService).automaticGradingMC(any(QuestionSubmission.class), any(AnswerMcSubmission.class));
        verify(submissionRepository).save(submission);
    }

    @Test
    public void testRegradeNAFull() throws DataServiceException {
        when(regradeDetails.getRegradeOption()).thenReturn(RegradeOption.NA);
        when(question.getRegradeOption()).thenReturn(RegradeOption.FULL);

        assessmentSubmissionService.gradeSubmission(submission, regradeDetails);

        verify(questionSubmissionRepository).save(any(QuestionSubmission.class));
        verify(questionSubmissionService, never()).automaticGradingMC(any(QuestionSubmission.class), any(AnswerMcSubmission.class));
        verify(submissionRepository).save(submission);
    }

    @Test
    public void testGradeSubmissionEssay() throws DataServiceException {
        when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);
        when(regradeDetails.getRegradeOption()).thenReturn(RegradeOption.NA);

        Submission retVal = assessmentSubmissionService.gradeSubmission(submission, regradeDetails);

        assertNotNull(retVal);
    }

    @Test
    public void testGradeSubmissionFile() throws DataServiceException {
        when(question.getQuestionType()).thenReturn(QuestionTypes.FILE);
        when(regradeDetails.getRegradeOption()).thenReturn(RegradeOption.NA);

        Submission retVal = assessmentSubmissionService.gradeSubmission(submission, regradeDetails);

        assertNotNull(retVal);
    }

    @Test
    public void testCalculateMaxScore() {
        float retVal = assessmentSubmissionService.calculateMaxScore(assessment);

        assertEquals(1f, retVal);
    }
}
