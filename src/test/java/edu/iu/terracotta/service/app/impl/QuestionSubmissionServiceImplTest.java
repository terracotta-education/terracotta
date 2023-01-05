package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.app.AnswerEssaySubmission;
import edu.iu.terracotta.model.app.AnswerMcSubmission;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.QuestionSubmissionComment;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.dto.AnswerDto;
import edu.iu.terracotta.model.app.dto.AnswerSubmissionDto;
import edu.iu.terracotta.model.app.dto.QuestionSubmissionCommentDto;
import edu.iu.terracotta.model.app.dto.QuestionSubmissionDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.repository.AnswerEssaySubmissionRepository;
import edu.iu.terracotta.repository.AnswerMcSubmissionRepository;
import edu.iu.terracotta.repository.AssessmentRepository;
import edu.iu.terracotta.repository.AssignmentRepository;
import edu.iu.terracotta.repository.QuestionSubmissionCommentRepository;
import edu.iu.terracotta.repository.QuestionSubmissionRepository;
import edu.iu.terracotta.repository.SubmissionRepository;
import edu.iu.terracotta.service.app.AnswerService;
import edu.iu.terracotta.service.app.AnswerSubmissionService;
import edu.iu.terracotta.service.app.QuestionSubmissionCommentService;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;

public class QuestionSubmissionServiceImplTest {

    @InjectMocks
    private QuestionSubmissionServiceImpl questionSubmissionService;

    @Mock private AllRepositories allRepositories;
    @Mock private AnswerEssaySubmissionRepository answerEssaySubmissionRepository;
    @Mock private AnswerMcSubmissionRepository answerMcSubmissionRepository;
    @Mock private AssessmentRepository assessmentRepository;
    @Mock private QuestionSubmissionCommentRepository questionSubmissionCommentRepository;
    @Mock private QuestionSubmissionRepository questionSubmissionRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock private AnswerService answerService;
    @Mock private AnswerSubmissionService answerSubmissionService;
    @Mock private QuestionSubmissionCommentService questionSubmissionCommentService;
    @Mock
    private CanvasAPIClient canvasAPIClient;

    @Mock private AnswerDto answerDto;
    @Mock private AnswerEssaySubmission answerEssaySubmission;
    @Mock private AnswerMcSubmission answerMcSubmission;
    @Mock private AnswerSubmissionDto answerSubmissionDto;
    @Mock private Assessment assessment;
    @Mock private Question question;
    @Mock private QuestionSubmission questionSubmission;
    @Mock private QuestionSubmissionComment questionSubmissionComment;
    @Mock private QuestionSubmissionCommentDto questionSubmissionCommentDto;
    @Mock private Submission submission;

    @BeforeEach
    public void beforeEach() throws DataServiceException, AssessmentNotMatchingException {
        MockitoAnnotations.openMocks(this);

        allRepositories.assessmentRepository = assessmentRepository;
        allRepositories.answerEssaySubmissionRepository = answerEssaySubmissionRepository;
        allRepositories.answerMcSubmissionRepository = answerMcSubmissionRepository;
        allRepositories.questionSubmissionCommentRepository = questionSubmissionCommentRepository;
        allRepositories.questionSubmissionRepository = questionSubmissionRepository;
        allRepositories.submissionRepository = submissionRepository;
        allRepositories.assignmentRepository = assignmentRepository;

        when(assessmentRepository.findById(anyLong())).thenReturn(Optional.of(assessment));
        when(answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.singletonList(answerEssaySubmission));
        when(answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.singletonList(answerMcSubmission));
        when(questionSubmissionCommentRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.singletonList(questionSubmissionComment));
        when(questionSubmissionRepository.findBySubmission_SubmissionId(anyLong())).thenReturn(Collections.singletonList(questionSubmission));
        when(submissionRepository.findBySubmissionId(anyLong())).thenReturn(submission);

        when(answerService.findAllByQuestionIdMC(anyLong(), anyBoolean())).thenReturn(Collections.singletonList(answerDto));
        when(answerSubmissionService.toDtoEssay(any(AnswerEssaySubmission.class))).thenReturn(answerSubmissionDto);
        when(answerSubmissionService.toDtoMC(any(AnswerMcSubmission.class))).thenReturn(answerSubmissionDto);
        when(questionSubmissionCommentService.toDto(any(QuestionSubmissionComment.class))).thenReturn(questionSubmissionCommentDto);

        when(assessment.canViewCorrectAnswers()).thenReturn(true);
        when(assessment.canViewResponses()).thenReturn(true);
        when(question.getQuestionId()).thenReturn(1l);
        when(questionSubmission.getQuestion()).thenReturn(question);
        when(questionSubmission.getQuestionSubmissionId()).thenReturn(1l);
        when(questionSubmission.getSubmission()).thenReturn(submission);
        when(submission.getSubmissionId()).thenReturn(1L);
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
    public void testCanSubmitWithUnlimitedAllowedAttempts() throws CanvasApiException, IOException {
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
            throws CanvasApiException, IOException {
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
            throws CanvasApiException, IOException {
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
            throws CanvasApiException, IOException {
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
            throws CanvasApiException, IOException, AssignmentAttemptException {
        SecuredInfo securedInfo = new SecuredInfo();
        securedInfo.setAllowedAttempts(null);
        securedInfo.setStudentAttempts(null);
        String canvasAssignmentId = "925";
        securedInfo.setCanvasAssignmentId(canvasAssignmentId);
        String canvasCourseId = "1193";
        securedInfo.setCanvasCourseId(canvasCourseId);

        long experimentId = 251;

        Assignment assignment = new Assignment();
        Exposure exposure = new Exposure();
        assignment.setExposure(exposure);
        Experiment experiment = new Experiment();
        experiment.setExperimentId(experimentId);
        exposure.setExperiment(experiment);
        PlatformDeployment platformDeployment = new PlatformDeployment();
        LtiUserEntity instructorUser = new LtiUserEntity("userKey", null, platformDeployment);
        experiment.setCreatedBy(instructorUser);

        when(assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(experimentId,
                canvasAssignmentId)).thenReturn(assignment);

        questionSubmissionService.canSubmit(securedInfo, experimentId);

        verify(this.canvasAPIClient).listAssignment(instructorUser, canvasCourseId,
                Integer.valueOf(canvasAssignmentId));
        verify(this.canvasAPIClient).listSubmissions(instructorUser, Integer.valueOf(canvasAssignmentId),
                canvasCourseId);
    }
}
