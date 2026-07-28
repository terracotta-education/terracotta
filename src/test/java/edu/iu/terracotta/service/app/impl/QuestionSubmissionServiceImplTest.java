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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.dao.entity.AnswerEssaySubmission;
import edu.iu.terracotta.dao.entity.AnswerFileSubmission;
import edu.iu.terracotta.dao.entity.AnswerMcSubmission;
import edu.iu.terracotta.dao.entity.FileSubmissionLocal;
import edu.iu.terracotta.dao.entity.QuestionSubmission;
import edu.iu.terracotta.dao.entity.QuestionSubmissionComment;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AnswerSubmissionDto;
import edu.iu.terracotta.dao.model.dto.QuestionSubmissionDto;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentLockedException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.DuplicateQuestionException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.exceptions.InvalidUserException;

public class QuestionSubmissionServiceImplTest extends BaseTest {

    private QuestionSubmissionServiceImpl questionSubmissionService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        // apiClient below also collides with CanvasApiClientImpl in BaseServiceTest (see the @InjectMocks
        // pitfall note there), so this class is constructed manually instead of relying on @InjectMocks,
        // which non-deterministically wired the wrong mock and left apiClient calls silently unstubbed.
        questionSubmissionService = new QuestionSubmissionServiceImpl(
            answerEssaySubmissionRepository,
            answerFileSubmissionRepository,
            answerMcRepository,
            answerMcSubmissionRepository,
            assessmentRepository,
            assignmentRepository,
            questionRepository,
            questionSubmissionCommentRepository,
            questionSubmissionRepository,
            submissionRepository,
            answerService,
            answerSubmissionService,
            fileStorageService,
            questionSubmissionCommentService,
            apiClient
        );

        when(answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.singletonList(answerEssaySubmission));
        when(answerEssaySubmissionRepository.findByQuestionSubmission_QuestionSubmissionIdIn(any())).thenReturn(Collections.singletonList(answerEssaySubmission));
        when(answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.singletonList(answerMcSubmission));
        when(answerMcSubmissionRepository.findByQuestionSubmission_QuestionSubmissionIdIn(any())).thenReturn(Collections.singletonList(answerMcSubmission));
        when(questionSubmissionCommentRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.singletonList(questionSubmissionComment));
        when(questionSubmissionCommentRepository.findByQuestionSubmission_QuestionSubmissionIdIn(any())).thenReturn(Collections.singletonList(questionSubmissionComment));
        when(questionSubmissionRepository.findBySubmission_SubmissionId(anyLong())).thenReturn(Collections.singletonList(questionSubmission));
        when(questionSubmissionRepository.findByQuestionSubmissionId(anyLong())).thenReturn(questionSubmission);
        when(questionRepository.findByAssessment_AssessmentIdAndQuestionId(anyLong(), anyLong())).thenReturn(Optional.of(question));

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
        when(assignmentRepository.findByExposure_Experiment_ExperimentIdAndLmsAssignmentId(anyLong(), anyString())).thenReturn(Optional.of(assignment));
        when(securedInfo.getAllowedAttempts()).thenReturn(2);
        when(securedInfo.getStudentAttempts()).thenReturn(1);

        assertDoesNotThrow(() -> {
            questionSubmissionService.canSubmit(securedInfo, 0);
        });
    }

    @Test
    public void testCanSubmitUnlockedInFuture() {
        SecuredInfo info = new SecuredInfo();
        info.setUnlockAt(new Timestamp(System.currentTimeMillis() + 100_000));

        assertThrows(AssignmentLockedException.class, () -> questionSubmissionService.canSubmit(info, 0));
    }

    @Test
    public void testCanSubmitLockedInPast() {
        SecuredInfo info = new SecuredInfo();
        info.setLockAt(new Timestamp(System.currentTimeMillis() - 100_000));

        assertThrows(AssignmentLockedException.class, () -> questionSubmissionService.canSubmit(info, 0));
    }

    @Test
    public void testCanSubmitLmsCheckNoMatchingSubmission() {
        // default lmsSubmission mock has a null getUser(), so it is filtered out of the submissions list
        assertDoesNotThrow(() -> questionSubmissionService.canSubmit(securedInfo, 1L, true));
    }

    @Test
    public void testCanSubmitLmsCheckAttemptIsNull() {
        when(lmsSubmission.getUser()).thenReturn(new Object());
        String lmsUserId = securedInfo.getLmsUserId();
        when(lmsSubmission.getUserId()).thenReturn(lmsUserId);

        assertDoesNotThrow(() -> questionSubmissionService.canSubmit(securedInfo, 1L, true));
    }

    @Test
    public void testCanSubmitLmsCheckAllowedAttemptsZeroOrLess() {
        when(lmsSubmission.getUser()).thenReturn(new Object());
        String lmsUserId = securedInfo.getLmsUserId();
        when(lmsSubmission.getUserId()).thenReturn(lmsUserId);
        when(lmsSubmission.getAttempt()).thenReturn(5L);
        when(lmsAssignment.getAllowedAttempts()).thenReturn(0);

        assertDoesNotThrow(() -> questionSubmissionService.canSubmit(securedInfo, 1L, true));
    }

    @Test
    public void testCanSubmitLmsCheckMoreAttemptsAvailable() {
        when(lmsSubmission.getUser()).thenReturn(new Object());
        String lmsUserId = securedInfo.getLmsUserId();
        when(lmsSubmission.getUserId()).thenReturn(lmsUserId);
        when(lmsSubmission.getAttempt()).thenReturn(2L);
        when(lmsAssignment.getAllowedAttempts()).thenReturn(5);

        assertDoesNotThrow(() -> questionSubmissionService.canSubmit(securedInfo, 1L, true));
    }

    @Test
    public void testCanSubmitLmsCheckAttemptsExceeded() {
        when(lmsSubmission.getUser()).thenReturn(new Object());
        String lmsUserId = securedInfo.getLmsUserId();
        when(lmsSubmission.getUserId()).thenReturn(lmsUserId);
        when(lmsSubmission.getAttempt()).thenReturn(5L);
        when(lmsAssignment.getAllowedAttempts()).thenReturn(3);

        assertThrows(AssignmentAttemptException.class, () -> questionSubmissionService.canSubmit(securedInfo, 1L, true));
    }

    @Test
    public void testCanSubmitLmsCheckAssignmentNotYetUnlocked() {
        when(lmsSubmission.getUser()).thenReturn(new Object());
        String lmsUserId = securedInfo.getLmsUserId();
        when(lmsSubmission.getUserId()).thenReturn(lmsUserId);
        when(lmsAssignment.getUnlockAt()).thenReturn(new Date(System.currentTimeMillis() + 1_000_000));

        assertThrows(AssignmentLockedException.class, () -> questionSubmissionService.canSubmit(securedInfo, 1L, true));
    }

    @Test
    public void testCanSubmitLmsCheckAssignmentLocked() {
        when(lmsSubmission.getUser()).thenReturn(new Object());
        String lmsUserId = securedInfo.getLmsUserId();
        when(lmsSubmission.getUserId()).thenReturn(lmsUserId);
        when(lmsAssignment.getLockAt()).thenReturn(new Date(System.currentTimeMillis() - 1_000_000));

        assertThrows(AssignmentLockedException.class, () -> questionSubmissionService.canSubmit(securedInfo, 1L, true));
    }

    @Test
    public void testGetQuestionSubmission() {
        QuestionSubmission result = questionSubmissionService.getQuestionSubmission(1L);

        assertNotNull(result);
        assertEquals(questionSubmission, result);
        verify(questionSubmissionRepository).findByQuestionSubmissionId(1L);
    }

    @Test
    public void testDeleteById() {
        questionSubmissionService.deleteById(1L);

        verify(questionSubmissionRepository).deleteByQuestionSubmissionId(1L);
    }

    @Test
    public void testAutomaticGradingMcCorrect() {
        when(answerMc.getCorrect()).thenReturn(true);

        QuestionSubmission result = questionSubmissionService.automaticGradingMC(questionSubmission, answerMcSubmission);

        assertNotNull(result);
        verify(questionSubmission).setCalculatedPoints(question.getPoints());
        verify(questionSubmissionRepository).save(questionSubmission);
    }

    @Test
    public void testAutomaticGradingMcIncorrect() {
        when(answerMc.getCorrect()).thenReturn(false);

        QuestionSubmission result = questionSubmissionService.automaticGradingMC(questionSubmission, answerMcSubmission);

        assertNotNull(result);
        verify(questionSubmission).setCalculatedPoints(0f);
    }

    @Test
    public void testValidateDtoPostIdMissing() {
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().build();

        assertThrows(IdMissingException.class, () -> questionSubmissionService.validateDtoPost(dto, 1L, 1L, false));
    }

    @Test
    public void testValidateDtoPostDuplicateQuestion() {
        when(questionSubmissionRepository.existsBySubmission_Assessment_AssessmentIdAndSubmission_SubmissionIdAndQuestion_QuestionId(anyLong(), anyLong(), anyLong())).thenReturn(true);
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionId(1L).build();

        assertThrows(DuplicateQuestionException.class, () -> questionSubmissionService.validateDtoPost(dto, 1L, 1L, false));
    }

    @Test
    public void testValidateDtoPostStudentCannotAlterGrade() {
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionId(1L).alteredGrade(5F).build();

        assertThrows(InvalidUserException.class, () -> questionSubmissionService.validateDtoPost(dto, 1L, 1L, true));
    }

    @Test
    public void testValidateDtoPostSuccess() {
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionId(1L).build();

        assertDoesNotThrow(() -> questionSubmissionService.validateDtoPost(dto, 1L, 1L, false));
    }

    @Test
    public void testBuildHeaders() {
        HttpHeaders headers = questionSubmissionService.buildHeaders(UriComponentsBuilder.newInstance(), 1L, 2L, 3L, 4L, 5L);

        assertNotNull(headers.getLocation());
        assertTrue(headers.getLocation().toString().contains("/api/experiments/1/conditions/2/treatments/3/assessments/4/submissions/5/question_submissions"));
    }

    @Test
    public void testUpdateQuestionSubmissionsStudentCannotAlterGrade() {
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().alteredGrade(5F).answerSubmissionDtoList(new ArrayList<>()).build();
        Map<QuestionSubmission, QuestionSubmissionDto> map = new HashMap<>();
        map.put(questionSubmission, dto);

        assertThrows(InvalidUserException.class, () -> questionSubmissionService.updateQuestionSubmissions(map, true));
    }

    @Test
    public void testUpdateQuestionSubmissionsMc() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);
        AnswerSubmissionDto answerSubmissionDto = AnswerSubmissionDto.builder().answerSubmissionId(1L).build();
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().answerSubmissionDtoList(List.of(answerSubmissionDto)).build();
        Map<QuestionSubmission, QuestionSubmissionDto> map = new HashMap<>();
        map.put(questionSubmission, dto);

        questionSubmissionService.updateQuestionSubmissions(map, false);

        verify(questionSubmission).setAlteredGrade(null);
        verify(answerSubmissionService).updateAnswerMcSubmission(eq(1L), eq(answerSubmissionDto));
        verify(answerSubmissionService, never()).updateAnswerEssaySubmission(any(), any());
    }

    @Test
    public void testUpdateQuestionSubmissionsEssay() throws Exception {
        // default question mock type is ESSAY
        AnswerSubmissionDto answerSubmissionDto = AnswerSubmissionDto.builder().answerSubmissionId(2L).build();
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().answerSubmissionDtoList(List.of(answerSubmissionDto)).build();
        Map<QuestionSubmission, QuestionSubmissionDto> map = new HashMap<>();
        map.put(questionSubmission, dto);

        questionSubmissionService.updateQuestionSubmissions(map, false);

        verify(answerSubmissionService).updateAnswerEssaySubmission(eq(2L), eq(answerSubmissionDto));
        verify(answerSubmissionService, never()).updateAnswerMcSubmission(any(), any());
    }

    @Test
    public void testUpdateQuestionSubmissionsOtherType() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.PAGE_BREAK);
        AnswerSubmissionDto answerSubmissionDto = AnswerSubmissionDto.builder().answerSubmissionId(3L).build();
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().answerSubmissionDtoList(List.of(answerSubmissionDto)).build();
        Map<QuestionSubmission, QuestionSubmissionDto> map = new HashMap<>();
        map.put(questionSubmission, dto);

        questionSubmissionService.updateQuestionSubmissions(map, false);

        verify(answerSubmissionService, never()).updateAnswerMcSubmission(any(), any());
        verify(answerSubmissionService, never()).updateAnswerEssaySubmission(any(), any());
    }

    @Test
    public void testPostQuestionSubmissionsSuccess() throws Exception {
        // the real (non-mock) QuestionSubmission built by fromDto() keeps flowing through postQuestionSubmissions()
        // after save(); it needs its ID populated on that same instance, not just on the shared questionSubmission mock
        when(questionSubmissionRepository.save(any(QuestionSubmission.class))).thenAnswer(invocation -> {
            QuestionSubmission saved = invocation.getArgument(0);
            saved.setQuestionSubmissionId(1L);
            return saved;
        });
        AnswerSubmissionDto answerSubmissionDto = AnswerSubmissionDto.builder().build();
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionId(1L).answerSubmissionDtoList(new ArrayList<>(List.of(answerSubmissionDto))).build();

        List<QuestionSubmissionDto> result = questionSubmissionService.postQuestionSubmissions(List.of(dto), 1L, 1L, false);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(answerSubmissionService).postAnswerSubmission(eq(answerSubmissionDto), anyLong());
    }

    @Test
    public void testPostQuestionSubmissionsError() {
        when(submissionRepository.findById(anyLong())).thenReturn(Optional.empty());
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionId(1L).answerSubmissionDtoList(new ArrayList<>()).build();

        assertThrows(DataServiceException.class, () -> questionSubmissionService.postQuestionSubmissions(List.of(dto), 1L, 1L, false));
    }

    @Test
    public void testValidateAndPrepareQuestionSubmissionListIdInPost() {
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).build();

        // IdInPostException is thrown internally but gets wrapped by the method's own blanket catch, so DataServiceException is what actually surfaces
        assertThrows(DataServiceException.class, () -> questionSubmissionService.validateAndPrepareQuestionSubmissionList(List.of(dto), 1L, 1L, false));
    }

    @Test
    public void testValidateAndPrepareQuestionSubmissionListMcAddsDefaultAnswerWhenNull() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionId(1L).build();

        questionSubmissionService.validateAndPrepareQuestionSubmissionList(List.of(dto), 1L, 1L, false);

        assertEquals(1, dto.getAnswerSubmissionDtoList().size());
    }

    @Test
    public void testValidateAndPrepareQuestionSubmissionListEssayAddsDefaultAnswerWhenEmpty() throws Exception {
        // default question mock type is ESSAY
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionId(1L).answerSubmissionDtoList(new ArrayList<>()).build();

        questionSubmissionService.validateAndPrepareQuestionSubmissionList(List.of(dto), 1L, 1L, false);

        assertEquals(1, dto.getAnswerSubmissionDtoList().size());
    }

    @Test
    public void testValidateAndPrepareQuestionSubmissionListExceedingLimit() {
        when(question.getQuestionType()).thenReturn(QuestionTypes.FILE);
        AnswerSubmissionDto a1 = AnswerSubmissionDto.builder().build();
        AnswerSubmissionDto a2 = AnswerSubmissionDto.builder().build();
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionId(1L).answerSubmissionDtoList(new ArrayList<>(List.of(a1, a2))).build();

        assertThrows(DataServiceException.class, () -> questionSubmissionService.validateAndPrepareQuestionSubmissionList(List.of(dto), 1L, 1L, false));
    }

    @Test
    public void testValidateAndPrepareQuestionSubmissionListAnswerNotMatching() {
        when(question.getQuestionType()).thenReturn(QuestionTypes.PAGE_BREAK);
        AnswerSubmissionDto a1 = AnswerSubmissionDto.builder().answerId(99L).build();
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionId(1L).answerSubmissionDtoList(new ArrayList<>(List.of(a1))).build();

        assertThrows(DataServiceException.class, () -> questionSubmissionService.validateAndPrepareQuestionSubmissionList(List.of(dto), 1L, 1L, false));
    }

    @Test
    public void testValidateAndPrepareQuestionSubmissionListSuccessOtherType() {
        when(question.getQuestionType()).thenReturn(QuestionTypes.PAGE_BREAK);
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionId(1L).answerSubmissionDtoList(new ArrayList<>()).build();

        assertDoesNotThrow(() -> questionSubmissionService.validateAndPrepareQuestionSubmissionList(List.of(dto), 1L, 1L, false));
    }

    @Test
    public void testValidateQuestionSubmissionAnswerSubmissionIdMissing() {
        AnswerSubmissionDto answerSubmissionDto = AnswerSubmissionDto.builder().build();
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).answerSubmissionDtoList(List.of(answerSubmissionDto)).build();

        assertThrows(DataServiceException.class, () -> questionSubmissionService.validateQuestionSubmission(dto));
    }

    @Test
    public void testValidateQuestionSubmissionMcSubmissionNotMatching() {
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);
        AnswerSubmissionDto answerSubmissionDto = AnswerSubmissionDto.builder().answerSubmissionId(1L).build();
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).answerSubmissionDtoList(List.of(answerSubmissionDto)).build();

        assertThrows(DataServiceException.class, () -> questionSubmissionService.validateQuestionSubmission(dto));
    }

    @Test
    public void testValidateQuestionSubmissionMcAnswerNotMatching() {
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);
        when(answerMcSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(answerMcSubmission));
        AnswerSubmissionDto answerSubmissionDto = AnswerSubmissionDto.builder().answerSubmissionId(1L).answerId(99L).build();
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).answerSubmissionDtoList(List.of(answerSubmissionDto)).build();

        assertThrows(DataServiceException.class, () -> questionSubmissionService.validateQuestionSubmission(dto));
    }

    @Test
    public void testValidateQuestionSubmissionMcSuccess() {
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);
        when(answerMcSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(answerMcSubmission));
        AnswerSubmissionDto answerSubmissionDto = AnswerSubmissionDto.builder().answerSubmissionId(1L).build();
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).answerSubmissionDtoList(List.of(answerSubmissionDto)).build();

        assertDoesNotThrow(() -> questionSubmissionService.validateQuestionSubmission(dto));
    }

    @Test
    public void testValidateQuestionSubmissionEssaySubmissionNotMatching() {
        // default question mock type is ESSAY
        AnswerSubmissionDto answerSubmissionDto = AnswerSubmissionDto.builder().answerSubmissionId(1L).build();
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).answerSubmissionDtoList(List.of(answerSubmissionDto)).build();

        assertThrows(DataServiceException.class, () -> questionSubmissionService.validateQuestionSubmission(dto));
    }

    @Test
    public void testValidateQuestionSubmissionEssaySuccess() {
        when(answerEssaySubmissionRepository.findById(anyLong())).thenReturn(Optional.of(answerEssaySubmission));
        AnswerSubmissionDto answerSubmissionDto = AnswerSubmissionDto.builder().answerSubmissionId(1L).build();
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).answerSubmissionDtoList(List.of(answerSubmissionDto)).build();

        assertDoesNotThrow(() -> questionSubmissionService.validateQuestionSubmission(dto));
    }

    @Test
    public void testValidateQuestionSubmissionOtherTypeSuccess() {
        when(question.getQuestionType()).thenReturn(QuestionTypes.PAGE_BREAK);
        AnswerSubmissionDto answerSubmissionDto = AnswerSubmissionDto.builder().answerSubmissionId(1L).build();
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).answerSubmissionDtoList(List.of(answerSubmissionDto)).build();

        assertDoesNotThrow(() -> questionSubmissionService.validateQuestionSubmission(dto));
    }

    @Test
    public void testHandleFileQuestionSubmissionSuccess() throws Exception {
        Path tempPath = Files.createTempFile("qs-test-", ".tmp");

        try {
            when(fileStorageService.saveFileSubmissionLocal(any())).thenReturn(
                FileSubmissionLocal.builder()
                    .filePath("path/to/file")
                    .compressed(true)
                    .encryptionMethod("AES")
                    .encryptionPhrase("phrase")
                    .build()
            );
            // the real (non-mock) QuestionSubmission built by fromDto() keeps flowing through postQuestionSubmissions()
            // after save(); it needs its ID populated on that same instance, not just on the shared questionSubmission mock
            when(questionSubmissionRepository.save(any(QuestionSubmission.class))).thenAnswer(invocation -> {
                QuestionSubmission saved = invocation.getArgument(0);
                saved.setQuestionSubmissionId(1L);
                return saved;
            });
            MockMultipartFile file = new MockMultipartFile(tempPath.toString(), "upload.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
            SecuredInfo info = new SecuredInfo();
            info.setAllowedAttempts(-1);

            List<QuestionSubmissionDto> result = questionSubmissionService.handleFileQuestionSubmission(file, "{\"questionId\":1}", 1L, 1L, 1L, false, info);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(fileStorageService).saveFileSubmissionLocal(file);
        } finally {
            Files.deleteIfExists(tempPath);
        }
    }

    @Test
    public void testHandleFileQuestionSubmissionUpdateNotFound() {
        when(questionSubmissionRepository.findByQuestionSubmissionId(anyLong())).thenReturn(null);

        assertThrows(
            QuestionSubmissionNotMatchingException.class,
            () -> questionSubmissionService.handleFileQuestionSubmissionUpdate(multipartFile, "{\"questionId\":1}", 1L, 1L, 1L, 1L, false, securedInfo)
        );
    }

    @Test
    public void testHandleFileQuestionSubmissionUpdateSuccess() throws Exception {
        Path tempPath = Files.createTempFile("qs-test-update-", ".tmp");

        try {
            when(answerFileSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.emptyList());
            when(fileStorageService.saveFileSubmissionLocal(any())).thenReturn(
                FileSubmissionLocal.builder()
                    .filePath("path/to/file2")
                    .compressed(false)
                    .build()
            );
            MockMultipartFile file = new MockMultipartFile(tempPath.toString(), "update.txt", "text/plain", "world".getBytes(StandardCharsets.UTF_8));
            SecuredInfo info = new SecuredInfo();
            info.setAllowedAttempts(-1);

            List<QuestionSubmissionDto> result = questionSubmissionService.handleFileQuestionSubmissionUpdate(file, "{\"questionId\":1}", 1L, 1L, 1L, 1L, false, info);

            assertNotNull(result);
            assertEquals(1, result.size());
        } finally {
            Files.deleteIfExists(tempPath);
        }
    }

    @Test
    public void testHandleFileQuestionSubmissionUpdateDeletesOldFiles() throws Exception {
        Path oldFilePath = Files.createTempFile("qs-test-old-", ".tmp");
        Path newFilePath = Files.createTempFile("qs-test-newupdate-", ".tmp");

        try {
            AnswerFileSubmission oldAnswerFileSubmission = AnswerFileSubmission.builder().answerFileSubmissionId(1L).fileName("old.txt").build();
            when(answerFileSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(List.of(oldAnswerFileSubmission));
            when(fileStorageService.getFileSubmissionLocal(1L)).thenReturn(oldFilePath.toFile());
            when(fileStorageService.saveFileSubmissionLocal(any())).thenReturn(
                FileSubmissionLocal.builder()
                    .filePath("path/to/file3")
                    .compressed(false)
                    .build()
            );
            MockMultipartFile file = new MockMultipartFile(newFilePath.toString(), "update2.txt", "text/plain", "world2".getBytes(StandardCharsets.UTF_8));
            SecuredInfo info = new SecuredInfo();
            info.setAllowedAttempts(-1);

            List<QuestionSubmissionDto> result = questionSubmissionService.handleFileQuestionSubmissionUpdate(file, "{\"questionId\":1}", 1L, 1L, 1L, 1L, false, info);

            assertNotNull(result);
            verify(answerFileSubmissionRepository).delete(oldAnswerFileSubmission);
            assertTrue(Files.notExists(oldFilePath));
        } finally {
            Files.deleteIfExists(oldFilePath);
            Files.deleteIfExists(newFilePath);
        }
    }

}
