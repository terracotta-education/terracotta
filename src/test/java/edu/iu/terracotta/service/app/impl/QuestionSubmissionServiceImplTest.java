package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.dao.entity.AnswerEssaySubmission;
import edu.iu.terracotta.dao.entity.AnswerMcSubmission;
import edu.iu.terracotta.dao.entity.QuestionSubmissionComment;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.QuestionSubmissionDto;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;

public class QuestionSubmissionServiceImplTest extends BaseTest {

    @InjectMocks private QuestionSubmissionServiceImpl questionSubmissionService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        when(answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.singletonList(answerEssaySubmission));
        when(answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.singletonList(answerMcSubmission));
        when(questionSubmissionCommentRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.singletonList(questionSubmissionComment));
        when(questionSubmissionRepository.findBySubmission_SubmissionId(anyLong())).thenReturn(Collections.singletonList(questionSubmission));

        when(answerService.findAllByQuestionIdMC(anyLong(), anyBoolean())).thenReturn(Collections.singletonList(answerDto));
        when(answerSubmissionService.toDtoEssay(any(AnswerEssaySubmission.class))).thenReturn(answerSubmissionDto);
        when(answerSubmissionService.toDtoMC(any(AnswerMcSubmission.class))).thenReturn(answerSubmissionDto);
        when(questionSubmissionCommentService.toDto(any(QuestionSubmissionComment.class))).thenReturn(questionSubmissionCommentDto);
    }

    @Test
    public void testGetQuestionSubmissionsIsStudent() throws AssessmentNotMatchingException, IOException {
        List<QuestionSubmissionDto> questionSubmissions = questionSubmissionService.getQuestionSubmissions(1l, true, true, 1l, true);

        assertNotNull(questionSubmissions);
        assertEquals(1, questionSubmissions.size());
        assertEquals(1, questionSubmissions.get(0).getAnswerDtoList().size());
        assertEquals(2, questionSubmissions.get(0).getAnswerSubmissionDtoList().size());
        assertEquals(1, questionSubmissions.get(0).getQuestionSubmissionCommentDtoList().size());
    }

    @Test
    public void testGetQuestionSubmissionsIsNotStudent() throws AssessmentNotMatchingException, IOException {
        List<QuestionSubmissionDto> questionSubmissions = questionSubmissionService.getQuestionSubmissions(1l, true, true, 1l, false);

        assertNotNull(questionSubmissions);
        assertEquals(1, questionSubmissions.size());
        assertEquals(1, questionSubmissions.get(0).getAnswerDtoList().size());
        assertEquals(2, questionSubmissions.get(0).getAnswerSubmissionDtoList().size());
        assertEquals(1, questionSubmissions.get(0).getQuestionSubmissionCommentDtoList().size());
    }

    @Test
    public void testGetQuestionSubmissionsIsStudentCannotViewResponses() throws AssessmentNotMatchingException, IOException {
        when(assessment.canViewResponses()).thenReturn(false);
        // The submission has been submitted (dateSubmitted != null)
        when(submission.getDateSubmitted()).thenReturn(new Timestamp(System.currentTimeMillis()));
        List<QuestionSubmissionDto> questionSubmissions = questionSubmissionService.getQuestionSubmissions(1l, true, true, 1l, true);

        assertNotNull(questionSubmissions);
        assertEquals(1, questionSubmissions.size());
        assertEquals(1, questionSubmissions.get(0).getAnswerDtoList().size());
        assertTrue(CollectionUtils.isEmpty(questionSubmissions.get(0).getAnswerSubmissionDtoList()));
        assertTrue(CollectionUtils.isEmpty(questionSubmissions.get(0).getQuestionSubmissionCommentDtoList()));
    }

    @Test
    public void testGetQuestionSubmissionsIsStudentCannotViewResponsesButSubmissionIsNotSubmitted() throws AssessmentNotMatchingException, IOException {
        when(assessment.canViewResponses()).thenReturn(false);
        // The submission has NOT been submitted (dateSubmitted == null)
        when(submission.getDateSubmitted()).thenReturn(null);

        List<QuestionSubmissionDto> questionSubmissions = questionSubmissionService.getQuestionSubmissions(1l, true, true, 1l, true);

        assertNotNull(questionSubmissions);
        assertEquals(1, questionSubmissions.size());
        assertEquals(1, questionSubmissions.get(0).getAnswerDtoList().size());
        assertEquals(2, questionSubmissions.get(0).getAnswerSubmissionDtoList().size());
        assertEquals(1, questionSubmissions.get(0).getQuestionSubmissionCommentDtoList().size());

        verify(answerService).findAllByQuestionIdMC(anyLong(), eq(false));
    }

    @Test
    public void testGetQuestionSubmissionsIsStudentCannotViewCorrectAnswers() throws AssessmentNotMatchingException, IOException {
        when(assessment.canViewCorrectAnswers()).thenReturn(false);
        List<QuestionSubmissionDto> questionSubmissions = questionSubmissionService.getQuestionSubmissions(1l, true, true, 1l, true);

        assertNotNull(questionSubmissions);
        assertEquals(1, questionSubmissions.size());
        assertEquals(1, questionSubmissions.get(0).getAnswerDtoList().size());
        assertEquals(2, questionSubmissions.get(0).getAnswerSubmissionDtoList().size());
        assertEquals(1, questionSubmissions.get(0).getQuestionSubmissionCommentDtoList().size());
    }

    // test that when securedInfo has allowedAttempts = -1 and studentAttempts = 3
    // that it doesn't throw exception
    @Test
    public void testCanSubmitWithUnlimitedAllowedAttempts() throws ApiException, IOException {
        SecuredInfo securedInfo = new SecuredInfo();
        securedInfo.setAllowedAttempts(-1);
        securedInfo.setStudentAttempts(3);
        assertDoesNotThrow(() -> {
            questionSubmissionService.canSubmit(securedInfo, 0);
        });
    }

    // test that when securedInfo has allowedAttempts = 2 and studentAttempts = 3
    // that it does throw exception
    @Test
    public void testCanSubmitWithLimitedAllowedAttemptsLessThanStudentAttempts()
            throws ApiException, IOException {
        SecuredInfo securedInfo = new SecuredInfo();
        securedInfo.setAllowedAttempts(2);
        securedInfo.setStudentAttempts(3);
        assertThrows(AssignmentAttemptException.class, () -> {
            questionSubmissionService.canSubmit(securedInfo, 0);
        });
    }

    // test that when securedInfo has allowedAttempts = 3 and studentAttempts = 3
    // that it does throw exception
    @Test
    public void testCanSubmitWithLimitedAllowedAttemptsEqualToStudentAttempts()
            throws ApiException, IOException {
        SecuredInfo securedInfo = new SecuredInfo();
        securedInfo.setAllowedAttempts(3);
        securedInfo.setStudentAttempts(3);
        assertThrows(AssignmentAttemptException.class, () -> {
            questionSubmissionService.canSubmit(securedInfo, 0);
        });
    }

    // test that when securedInfo has allowedAttempts = 4 and studentAttempts = 3
    // that it doesn't throw exception
    @Test
    public void testCanSubmitWithLimitedAllowedAttemptsMoreThanStudentAttempts()
            throws ApiException, IOException {
        SecuredInfo securedInfo = new SecuredInfo();
        securedInfo.setAllowedAttempts(4);
        securedInfo.setStudentAttempts(3);
        assertDoesNotThrow(() -> {
            questionSubmissionService.canSubmit(securedInfo, 0);
        });
    }

    // test that when securedInfo has studentAttempts = null that it makes canvas
    // api calls
    @Test
    public void testCanSubmitWithNullStudentAttempts()
            throws ApiException, IOException, AssignmentAttemptException {
        when(assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(anyLong(), anyString())).thenReturn(assignment);
        when(securedInfo.getAllowedAttempts()).thenReturn(2);
        when(securedInfo.getStudentAttempts()).thenReturn(1);

        assertDoesNotThrow(() -> {
            questionSubmissionService.canSubmit(securedInfo, 0);
        });
    }

}
