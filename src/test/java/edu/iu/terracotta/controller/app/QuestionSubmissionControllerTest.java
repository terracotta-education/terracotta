package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.QuestionSubmission;
import edu.iu.terracotta.dao.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.dao.model.dto.QuestionSubmissionDto;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentLockedException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.utils.TextConstants;

import jakarta.servlet.http.HttpServletRequest;

public class QuestionSubmissionControllerTest extends BaseTest {

    private QuestionSubmissionController questionSubmissionController;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        // ApiJwtService has two matching mocks in BaseServiceTest (apiJwtService and canvasApiJwtService),
        // so the controller is constructed manually rather than relying on @InjectMocks to avoid ambiguous wiring.
        questionSubmissionController = new QuestionSubmissionController(apiJwtService, questionSubmissionService, submissionService);

        when(apiJwtService.extractValues(any(HttpServletRequest.class), anyBoolean())).thenReturn(securedInfo);
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(questionSubmissionService.buildHeaders(any(UriComponentsBuilder.class), anyLong(), anyLong(), anyLong(), anyLong(), anyLong())).thenReturn(new HttpHeaders());
    }

    // ---- getQuestionSubmissionsBySubmission ----

    @Test
    void getQuestionSubmissionsBySubmissionSuccessTest() throws Exception {
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).build();
        when(questionSubmissionService.getQuestionSubmissions(1L, false, false, 1L, false)).thenReturn(List.of(dto));

        ResponseEntity<List<QuestionSubmissionDto>> ret = questionSubmissionController.getQuestionSubmissionsBySubmission(1L, 1L, 1L, 1L, 1L, false, false, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(List.of(dto), ret.getBody());
    }

    @Test
    void getQuestionSubmissionsBySubmissionNoContentTest() throws Exception {
        when(questionSubmissionService.getQuestionSubmissions(1L, false, false, 1L, false)).thenReturn(Collections.emptyList());

        ResponseEntity<List<QuestionSubmissionDto>> ret = questionSubmissionController.getQuestionSubmissionsBySubmission(1L, 1L, 1L, 1L, 1L, false, false, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, ret.getStatusCode());
    }

    @Test
    void getQuestionSubmissionsBySubmissionUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<List<QuestionSubmissionDto>> ret = questionSubmissionController.getQuestionSubmissionsBySubmission(1L, 1L, 1L, 1L, 1L, false, false, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void getQuestionSubmissionsBySubmissionStudentValidatesUserTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);
        when(questionSubmissionService.getQuestionSubmissions(1L, false, false, 1L, false)).thenReturn(Collections.emptyList());

        ResponseEntity<List<QuestionSubmissionDto>> ret = questionSubmissionController.getQuestionSubmissionsBySubmission(1L, 1L, 1L, 1L, 1L, false, false, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, ret.getStatusCode());
        verify(submissionService, times(1)).validateUser(anyLong(), anyString(), anyLong());
    }

    @Test
    void getQuestionSubmissionsBySubmissionInvalidUserTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);
        doThrow(new InvalidUserException("invalid user")).when(submissionService).validateUser(anyLong(), anyString(), anyLong());

        assertThrows(InvalidUserException.class, () -> questionSubmissionController.getQuestionSubmissionsBySubmission(1L, 1L, 1L, 1L, 1L, false, false, httpServletRequest));
    }

    @Test
    void getQuestionSubmissionsBySubmissionExperimentNotMatchingTest() throws Exception {
        doThrow(new ExperimentNotMatchingException("not matching")).when(apiJwtService).experimentAllowed(any(SecuredInfo.class), anyLong());

        assertThrows(ExperimentNotMatchingException.class, () -> questionSubmissionController.getQuestionSubmissionsBySubmission(1L, 1L, 1L, 1L, 1L, false, false, httpServletRequest));
    }

    @Test
    void getQuestionSubmissionsBySubmissionSubmissionNotMatchingTest() throws Exception {
        doThrow(new SubmissionNotMatchingException("not matching")).when(apiJwtService).submissionAllowed(any(SecuredInfo.class), anyLong(), anyLong());

        assertThrows(SubmissionNotMatchingException.class, () -> questionSubmissionController.getQuestionSubmissionsBySubmission(1L, 1L, 1L, 1L, 1L, false, false, httpServletRequest));
    }

    // ---- getQuestionSubmission ----

    @Test
    void getQuestionSubmissionSuccessTest() throws Exception {
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).build();
        when(questionSubmissionService.getQuestionSubmission(1L)).thenReturn(questionSubmission);
        when(questionSubmissionService.toDto(questionSubmission, false, false)).thenReturn(dto);

        ResponseEntity<QuestionSubmissionDto> ret = questionSubmissionController.getQuestionSubmission(1L, 1L, 1L, 1L, 1L, 1L, false, false, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(dto, ret.getBody());
    }

    @Test
    void getQuestionSubmissionUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<QuestionSubmissionDto> ret = questionSubmissionController.getQuestionSubmission(1L, 1L, 1L, 1L, 1L, 1L, false, false, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void getQuestionSubmissionStudentValidatesUserTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);
        when(questionSubmissionService.getQuestionSubmission(1L)).thenReturn(questionSubmission);

        questionSubmissionController.getQuestionSubmission(1L, 1L, 1L, 1L, 1L, 1L, false, false, httpServletRequest);

        verify(submissionService, times(1)).validateUser(anyLong(), anyString(), anyLong());
    }

    @Test
    void getQuestionSubmissionNotMatchingTest() throws Exception {
        doThrow(new QuestionSubmissionNotMatchingException("not matching")).when(apiJwtService).questionSubmissionAllowed(any(SecuredInfo.class), anyLong(), anyLong(), anyLong());

        assertThrows(QuestionSubmissionNotMatchingException.class, () -> questionSubmissionController.getQuestionSubmission(1L, 1L, 1L, 1L, 1L, 1L, false, false, httpServletRequest));
    }

    // ---- postQuestionSubmission ----

    @Test
    void postQuestionSubmissionSuccessTest() throws Exception {
        QuestionSubmissionDto requestDto = QuestionSubmissionDto.builder().questionId(1L).build();
        QuestionSubmissionDto returnedDto = QuestionSubmissionDto.builder().questionSubmissionId(1L).questionId(1L).build();
        when(questionSubmissionService.postQuestionSubmissions(anyList(), eq(1L), eq(1L), eq(false))).thenReturn(List.of(returnedDto));

        ResponseEntity<List<QuestionSubmissionDto>> ret = questionSubmissionController.postQuestionSubmission(1L, 1L, 1L, 1L, 1L, List.of(requestDto), UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.CREATED, ret.getStatusCode());
        assertEquals(List.of(returnedDto), ret.getBody());
    }

    @Test
    void postQuestionSubmissionUnauthorizedNotFoundTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);
        QuestionSubmissionDto requestDto = QuestionSubmissionDto.builder().questionId(1L).build();

        ResponseEntity<List<QuestionSubmissionDto>> ret = questionSubmissionController.postQuestionSubmission(1L, 1L, 1L, 1L, 1L, List.of(requestDto), UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, ret.getStatusCode());
    }

    @Test
    void postQuestionSubmissionStudentPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);
        QuestionSubmissionDto requestDto = QuestionSubmissionDto.builder().questionId(1L).build();
        when(questionSubmissionService.postQuestionSubmissions(anyList(), eq(1L), eq(1L), eq(true))).thenReturn(List.of(requestDto));

        ResponseEntity<List<QuestionSubmissionDto>> ret = questionSubmissionController.postQuestionSubmission(1L, 1L, 1L, 1L, 1L, List.of(requestDto), UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.CREATED, ret.getStatusCode());
        verify(submissionService, times(1)).validateUser(anyLong(), anyString(), anyLong());
        verify(questionSubmissionService, times(1)).validateAndPrepareQuestionSubmissionList(anyList(), eq(1L), eq(1L), eq(true));
    }

    @Test
    void postQuestionSubmissionAssignmentAttemptCaughtTest() throws Exception {
        QuestionSubmissionDto requestDto = QuestionSubmissionDto.builder().questionId(1L).build();
        doThrow(new AssignmentAttemptException("no more attempts")).when(questionSubmissionService).canSubmit(any(SecuredInfo.class), anyLong());

        ResponseEntity<List<QuestionSubmissionDto>> ret = questionSubmissionController.postQuestionSubmission(1L, 1L, 1L, 1L, 1L, List.of(requestDto), UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals("no more attempts", ret.getBody());
    }

    @Test
    void postQuestionSubmissionAssignmentLockedCaughtTest() throws Exception {
        QuestionSubmissionDto requestDto = QuestionSubmissionDto.builder().questionId(1L).build();
        doThrow(new AssignmentLockedException("locked")).when(questionSubmissionService).canSubmit(any(SecuredInfo.class), anyLong());

        ResponseEntity<List<QuestionSubmissionDto>> ret = questionSubmissionController.postQuestionSubmission(1L, 1L, 1L, 1L, 1L, List.of(requestDto), UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals("locked", ret.getBody());
    }

    @Test
    void postQuestionSubmissionDataServiceExceptionPropagatesTest() throws Exception {
        QuestionSubmissionDto requestDto = QuestionSubmissionDto.builder().questionId(1L).build();
        doThrow(new DataServiceException("failed")).when(questionSubmissionService).validateAndPrepareQuestionSubmissionList(anyList(), anyLong(), anyLong(), anyBoolean());

        assertThrows(
            DataServiceException.class,
            () -> questionSubmissionController.postQuestionSubmission(1L, 1L, 1L, 1L, 1L, List.of(requestDto), UriComponentsBuilder.newInstance(), httpServletRequest)
        );
    }

    @Test
    void postQuestionSubmissionExperimentNotMatchingTest() throws Exception {
        QuestionSubmissionDto requestDto = QuestionSubmissionDto.builder().questionId(1L).build();
        doThrow(new ExperimentNotMatchingException("not matching")).when(apiJwtService).experimentAllowed(any(SecuredInfo.class), anyLong());

        assertThrows(
            ExperimentNotMatchingException.class,
            () -> questionSubmissionController.postQuestionSubmission(1L, 1L, 1L, 1L, 1L, List.of(requestDto), UriComponentsBuilder.newInstance(), httpServletRequest)
        );
    }

    // ---- updateQuestionSubmission (single) ----

    @Test
    void updateQuestionSubmissionSuccessTest() throws Exception {
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).build();
        when(questionSubmissionService.getQuestionSubmission(1L)).thenReturn(questionSubmission);

        ResponseEntity<Void> ret = questionSubmissionController.updateQuestionSubmission(1L, 1L, 1L, 1L, 1L, 1L, dto, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        verify(questionSubmissionService, times(1)).updateQuestionSubmissions(anyMapWithSize(1), eq(false));
    }

    @Test
    void updateQuestionSubmissionUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).build();

        ResponseEntity<Void> ret = questionSubmissionController.updateQuestionSubmission(1L, 1L, 1L, 1L, 1L, 1L, dto, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void updateQuestionSubmissionStudentPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).build();
        when(questionSubmissionService.getQuestionSubmission(1L)).thenReturn(questionSubmission);

        ResponseEntity<Void> ret = questionSubmissionController.updateQuestionSubmission(1L, 1L, 1L, 1L, 1L, 1L, dto, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        verify(submissionService, times(1)).validateUser(anyLong(), anyString(), anyLong());
        verify(questionSubmissionService, times(1)).updateQuestionSubmissions(anyMapWithSize(1), eq(true));
    }

    @Test
    void updateQuestionSubmissionNotMatchingTest() throws Exception {
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).build();
        doThrow(new QuestionSubmissionNotMatchingException("not matching")).when(apiJwtService).questionSubmissionAllowed(any(SecuredInfo.class), anyLong(), anyLong(), anyLong());

        assertThrows(QuestionSubmissionNotMatchingException.class, () -> questionSubmissionController.updateQuestionSubmission(1L, 1L, 1L, 1L, 1L, 1L, dto, httpServletRequest));
    }

    @Test
    void updateQuestionSubmissionDataServiceExceptionTest() throws Exception {
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).build();
        doThrow(new DataServiceException("failed")).when(questionSubmissionService).validateQuestionSubmission(any(QuestionSubmissionDto.class));

        assertThrows(DataServiceException.class, () -> questionSubmissionController.updateQuestionSubmission(1L, 1L, 1L, 1L, 1L, 1L, dto, httpServletRequest));
    }

    @Test
    void updateQuestionSubmissionAnswerNotMatchingTest() throws Exception {
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).build();
        when(questionSubmissionService.getQuestionSubmission(1L)).thenReturn(questionSubmission);
        doThrow(new AnswerNotMatchingException("not matching")).when(questionSubmissionService).updateQuestionSubmissions(any(), anyBoolean());

        assertThrows(AnswerNotMatchingException.class, () -> questionSubmissionController.updateQuestionSubmission(1L, 1L, 1L, 1L, 1L, 1L, dto, httpServletRequest));
    }

    // ---- updateQuestionSubmissions (bulk) ----

    @Test
    void updateQuestionSubmissionsSuccessTest() throws Exception {
        when(questionSubmission.getQuestionSubmissionId()).thenReturn(1L);
        when(questionSubmissionService.getQuestionSubmission(1L)).thenReturn(questionSubmission);
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).build();

        ResponseEntity<Void> ret = questionSubmissionController.updateQuestionSubmissions(1L, 1L, 1L, 1L, 1L, List.of(dto), httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        verify(questionSubmissionService, times(1)).updateQuestionSubmissions(anyMapWithSize(1), eq(false));
    }

    @Test
    void updateQuestionSubmissionsUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).build();

        ResponseEntity<Void> ret = questionSubmissionController.updateQuestionSubmissions(1L, 1L, 1L, 1L, 1L, List.of(dto), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void updateQuestionSubmissionsStudentPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);
        when(questionSubmission.getQuestionSubmissionId()).thenReturn(1L);
        when(questionSubmissionService.getQuestionSubmission(1L)).thenReturn(questionSubmission);
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).build();

        ResponseEntity<Void> ret = questionSubmissionController.updateQuestionSubmissions(1L, 1L, 1L, 1L, 1L, List.of(dto), httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        verify(submissionService, times(1)).validateUser(anyLong(), anyString(), anyLong());
        verify(questionSubmissionService, times(1)).updateQuestionSubmissions(anyMapWithSize(1), eq(true));
    }

    @Test
    void updateQuestionSubmissionsNotMatchingInLoopTest() throws Exception {
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).build();
        doThrow(new QuestionSubmissionNotMatchingException("not matching")).when(apiJwtService).questionSubmissionAllowed(any(SecuredInfo.class), anyLong(), anyLong(), eq(1L));

        assertThrows(QuestionSubmissionNotMatchingException.class, () -> questionSubmissionController.updateQuestionSubmissions(1L, 1L, 1L, 1L, 1L, List.of(dto), httpServletRequest));
    }

    @Test
    void updateQuestionSubmissionsWrapsUnexpectedExceptionAsDataServiceExceptionTest() throws Exception {
        when(questionSubmission.getQuestionSubmissionId()).thenReturn(1L);
        when(questionSubmissionService.getQuestionSubmission(1L)).thenReturn(questionSubmission);
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).build();
        doThrow(new RuntimeException("boom")).when(questionSubmissionService).updateQuestionSubmissions(any(), anyBoolean());

        DataServiceException ex = assertThrows(DataServiceException.class, () -> questionSubmissionController.updateQuestionSubmissions(1L, 1L, 1L, 1L, 1L, List.of(dto), httpServletRequest));
        assertTrue(ex.getMessage().contains("boom"));
    }

    @Test
    void updateQuestionSubmissionsExperimentNotMatchingTest() throws Exception {
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).build();
        doThrow(new ExperimentNotMatchingException("not matching")).when(apiJwtService).experimentAllowed(any(SecuredInfo.class), anyLong());

        assertThrows(ExperimentNotMatchingException.class, () -> questionSubmissionController.updateQuestionSubmissions(1L, 1L, 1L, 1L, 1L, List.of(dto), httpServletRequest));
    }

    // ---- deleteQuestionSubmission ----

    @Test
    void deleteQuestionSubmissionSuccessTest() throws Exception {
        ResponseEntity<Void> ret = questionSubmissionController.deleteQuestionSubmission(1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
    }

    @Test
    void deleteQuestionSubmissionUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<Void> ret = questionSubmissionController.deleteQuestionSubmission(1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void deleteQuestionSubmissionNotFoundTest() throws Exception {
        doThrow(new EmptyResultDataAccessException(1)).when(questionSubmissionService).deleteById(1L);

        ResponseEntity<Void> ret = questionSubmissionController.deleteQuestionSubmission(1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, ret.getStatusCode());
    }

    @Test
    void deleteQuestionSubmissionNotMatchingTest() throws Exception {
        doThrow(new QuestionSubmissionNotMatchingException("not matching")).when(apiJwtService).questionSubmissionAllowed(any(SecuredInfo.class), anyLong(), anyLong(), anyLong());

        assertThrows(QuestionSubmissionNotMatchingException.class, () -> questionSubmissionController.deleteQuestionSubmission(1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest));
    }

    // ---- postFileQuestionSubmission ----

    @Test
    void postFileQuestionSubmissionFileEmptyTest() throws Exception {
        when(multipartFile.isEmpty()).thenReturn(true);

        ResponseEntity<List<QuestionSubmissionDto>> ret = questionSubmissionController.postFileQuestionSubmission(1L, 1L, 1L, 1L, 1L, "{}", UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, ret.getStatusCode());
        assertEquals(TextConstants.FILE_MISSING, ret.getBody());
    }

    @Test
    void postFileQuestionSubmissionUnauthorizedNotFoundTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<List<QuestionSubmissionDto>> ret = questionSubmissionController.postFileQuestionSubmission(1L, 1L, 1L, 1L, 1L, "{}", UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, ret.getStatusCode());
    }

    @Test
    void postFileQuestionSubmissionSuccessTest() throws Exception {
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).build();
        when(questionSubmissionService.handleFileQuestionSubmission(eq(multipartFile), anyString(), anyLong(), anyLong(), anyLong(), eq(false), any(SecuredInfo.class))).thenReturn(List.of(dto));

        ResponseEntity<List<QuestionSubmissionDto>> ret = questionSubmissionController.postFileQuestionSubmission(1L, 1L, 1L, 1L, 1L, "{}", UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest);

        assertEquals(HttpStatus.CREATED, ret.getStatusCode());
        assertEquals(List.of(dto), ret.getBody());
    }

    @Test
    void postFileQuestionSubmissionStudentPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);
        when(questionSubmissionService.handleFileQuestionSubmission(eq(multipartFile), anyString(), anyLong(), anyLong(), anyLong(), eq(true), any(SecuredInfo.class))).thenReturn(List.of());

        questionSubmissionController.postFileQuestionSubmission(1L, 1L, 1L, 1L, 1L, "{}", UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest);

        verify(submissionService, times(1)).validateUser(anyLong(), anyString(), anyLong());
        verify(questionSubmissionService, times(1)).handleFileQuestionSubmission(eq(multipartFile), anyString(), anyLong(), anyLong(), anyLong(), eq(true), any(SecuredInfo.class));
    }

    @Test
    void postFileQuestionSubmissionAssignmentAttemptNotCaughtTest() throws Exception {
        // unlike postQuestionSubmission(), this endpoint does not catch AssignmentAttemptException/AssignmentLockedException
        // internally even though it declares them, so it propagates instead of returning a 401.
        doThrow(new AssignmentAttemptException("no more attempts")).when(questionSubmissionService)
            .handleFileQuestionSubmission(eq(multipartFile), anyString(), anyLong(), anyLong(), anyLong(), anyBoolean(), any(SecuredInfo.class));

        assertThrows(
            AssignmentAttemptException.class,
            () -> questionSubmissionController.postFileQuestionSubmission(1L, 1L, 1L, 1L, 1L, "{}", UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest)
        );
    }

    @Test
    void postFileQuestionSubmissionExperimentNotMatchingTest() throws Exception {
        doThrow(new ExperimentNotMatchingException("not matching")).when(apiJwtService).experimentAllowed(any(SecuredInfo.class), anyLong());

        assertThrows(
            ExperimentNotMatchingException.class,
            () -> questionSubmissionController.postFileQuestionSubmission(1L, 1L, 1L, 1L, 1L, "{}", UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest)
        );
    }

    // ---- putFileQuestionSubmission ----

    @Test
    void putFileQuestionSubmissionFileEmptyTest() throws Exception {
        when(multipartFile.isEmpty()).thenReturn(true);

        ResponseEntity<List<QuestionSubmissionDto>> ret = questionSubmissionController.putFileQuestionSubmission(1L, 1L, 1L, 1L, 1L, 1L, "{}", UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, ret.getStatusCode());
        assertEquals(TextConstants.FILE_MISSING, ret.getBody());
    }

    @Test
    void putFileQuestionSubmissionUnauthorizedNotFoundTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<List<QuestionSubmissionDto>> ret = questionSubmissionController.putFileQuestionSubmission(1L, 1L, 1L, 1L, 1L, 1L, "{}", UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, ret.getStatusCode());
    }

    @Test
    void putFileQuestionSubmissionSuccessTest() throws Exception {
        QuestionSubmissionDto dto = QuestionSubmissionDto.builder().questionSubmissionId(1L).build();
        when(questionSubmissionService.handleFileQuestionSubmissionUpdate(eq(multipartFile), anyString(), anyLong(), anyLong(), anyLong(), anyLong(), eq(false), any(SecuredInfo.class))).thenReturn(List.of(dto));

        ResponseEntity<List<QuestionSubmissionDto>> ret = questionSubmissionController.putFileQuestionSubmission(1L, 1L, 1L, 1L, 1L, 1L, "{}", UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest);

        assertEquals(HttpStatus.CREATED, ret.getStatusCode());
        assertEquals(List.of(dto), ret.getBody());
    }

    @Test
    void putFileQuestionSubmissionStudentPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);
        when(questionSubmissionService.handleFileQuestionSubmissionUpdate(eq(multipartFile), anyString(), anyLong(), anyLong(), anyLong(), anyLong(), eq(true), any(SecuredInfo.class))).thenReturn(List.of());

        questionSubmissionController.putFileQuestionSubmission(1L, 1L, 1L, 1L, 1L, 1L, "{}", UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest);

        verify(submissionService, times(1)).validateUser(anyLong(), anyString(), anyLong());
    }

    @Test
    void putFileQuestionSubmissionNotMatchingPropagatesTest() throws Exception {
        // this endpoint has no submissionAllowed()/questionSubmissionAllowed() check of its own before calling the
        // service, but the service is still declared to throw QuestionSubmissionNotMatchingException, so verify it propagates.
        doThrow(new QuestionSubmissionNotMatchingException("not matching")).when(questionSubmissionService)
            .handleFileQuestionSubmissionUpdate(eq(multipartFile), anyString(), anyLong(), anyLong(), anyLong(), anyLong(), anyBoolean(), any(SecuredInfo.class));

        assertThrows(
            QuestionSubmissionNotMatchingException.class,
            () -> questionSubmissionController.putFileQuestionSubmission(1L, 1L, 1L, 1L, 1L, 1L, "{}", UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest)
        );
    }

    @Test
    void putFileQuestionSubmissionExperimentNotMatchingTest() throws Exception {
        doThrow(new ExperimentNotMatchingException("not matching")).when(apiJwtService).experimentAllowed(any(SecuredInfo.class), anyLong());

        assertThrows(
            ExperimentNotMatchingException.class,
            () -> questionSubmissionController.putFileQuestionSubmission(1L, 1L, 1L, 1L, 1L, 1L, "{}", UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest)
        );
    }

    private static Map<QuestionSubmission, QuestionSubmissionDto> anyMapWithSize(int size) {
        return ArgumentMatchers.argThat(map -> map != null && map.size() == size);
    }

}
