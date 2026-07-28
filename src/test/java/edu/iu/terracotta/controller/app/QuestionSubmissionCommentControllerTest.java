package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionSubmissionCommentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.dao.model.dto.QuestionSubmissionCommentDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.utils.TextConstants;
import jakarta.servlet.http.HttpServletRequest;

public class QuestionSubmissionCommentControllerTest extends BaseTest {

    private QuestionSubmissionCommentController questionSubmissionCommentController;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        questionSubmissionCommentController = new QuestionSubmissionCommentController(apiJwtService, submissionService, questionSubmissionCommentService);

        when(apiJwtService.extractValues(any(HttpServletRequest.class), anyBoolean())).thenReturn(securedInfo);
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(questionSubmissionCommentDto.getQuestionSubmissionCommentId()).thenReturn(1L);
    }

    @Test
    void getQuestionSubmissionCommentsTest() throws Exception {
        when(questionSubmissionCommentService.getQuestionSubmissionComments(anyLong())).thenReturn(List.of(questionSubmissionCommentDto));

        ResponseEntity<List<QuestionSubmissionCommentDto>> ret = questionSubmissionCommentController.getQuestionSubmissionComments(1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(1, ret.getBody().size());
    }

    @Test
    void getQuestionSubmissionCommentsNoContentTest() throws Exception {
        when(questionSubmissionCommentService.getQuestionSubmissionComments(anyLong())).thenReturn(Collections.emptyList());

        ResponseEntity<List<QuestionSubmissionCommentDto>> ret = questionSubmissionCommentController.getQuestionSubmissionComments(1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, ret.getStatusCode());
    }

    @Test
    void getQuestionSubmissionCommentsUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<List<QuestionSubmissionCommentDto>> ret = questionSubmissionCommentController.getQuestionSubmissionComments(1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void getQuestionSubmissionCommentsStudentValidatesUserTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);
        when(questionSubmissionCommentService.getQuestionSubmissionComments(anyLong())).thenReturn(List.of(questionSubmissionCommentDto));

        ResponseEntity<List<QuestionSubmissionCommentDto>> ret = questionSubmissionCommentController.getQuestionSubmissionComments(1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        verify(submissionService, times(1)).validateUser(anyLong(), anyString(), anyLong());
    }

    @Test
    void getQuestionSubmissionCommentsInvalidUserTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);
        doThrow(new InvalidUserException("invalid user")).when(submissionService).validateUser(anyLong(), anyString(), anyLong());

        assertThrows(InvalidUserException.class, () -> questionSubmissionCommentController.getQuestionSubmissionComments(1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest));
    }

    @Test
    void getQuestionSubmissionCommentsExperimentNotMatchingTest() throws Exception {
        doThrow(new ExperimentNotMatchingException("experiment not matching")).when(apiJwtService).experimentAllowed(any(SecuredInfo.class), anyLong());

        assertThrows(ExperimentNotMatchingException.class, () -> questionSubmissionCommentController.getQuestionSubmissionComments(1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest));
    }

    @Test
    void getQuestionSubmissionCommentTest() throws Exception {
        when(questionSubmissionCommentService.getQuestionSubmissionComment(anyLong())).thenReturn(questionSubmissionComment);
        when(questionSubmissionCommentService.toDto(questionSubmissionComment)).thenReturn(questionSubmissionCommentDto);

        ResponseEntity<QuestionSubmissionCommentDto> ret = questionSubmissionCommentController.getQuestionSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(questionSubmissionCommentDto, ret.getBody());
    }

    @Test
    void getQuestionSubmissionCommentUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<QuestionSubmissionCommentDto> ret = questionSubmissionCommentController.getQuestionSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void getQuestionSubmissionCommentNotMatchingTest() throws Exception {
        doThrow(new QuestionSubmissionCommentNotMatchingException("comment not matching")).when(apiJwtService).questionSubmissionCommentAllowed(any(SecuredInfo.class), anyLong(), anyLong());

        assertThrows(QuestionSubmissionCommentNotMatchingException.class, () -> questionSubmissionCommentController.getQuestionSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest));
    }

    @Test
    void postQuestionSubmissionCommentTest() throws Exception {
        when(questionSubmissionCommentService.postQuestionSubmissionComment(any(QuestionSubmissionCommentDto.class), anyLong(), any(SecuredInfo.class))).thenReturn(questionSubmissionCommentDto);
        when(questionSubmissionCommentService.buildHeaders(any(UriComponentsBuilder.class), any(), any(), any(), any(), any(), any(), any())).thenReturn(new HttpHeaders());

        ResponseEntity<QuestionSubmissionCommentDto> ret = questionSubmissionCommentController.postQuestionSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, questionSubmissionCommentDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.CREATED, ret.getStatusCode());
        assertEquals(questionSubmissionCommentDto, ret.getBody());
    }

    @Test
    void postQuestionSubmissionCommentUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<QuestionSubmissionCommentDto> ret = questionSubmissionCommentController.postQuestionSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, questionSubmissionCommentDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void postQuestionSubmissionCommentStudentValidatesUserTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);
        when(questionSubmissionCommentService.postQuestionSubmissionComment(any(QuestionSubmissionCommentDto.class), anyLong(), any(SecuredInfo.class))).thenReturn(questionSubmissionCommentDto);
        when(questionSubmissionCommentService.buildHeaders(any(UriComponentsBuilder.class), any(), any(), any(), any(), any(), any(), any())).thenReturn(new HttpHeaders());

        questionSubmissionCommentController.postQuestionSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, questionSubmissionCommentDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        verify(submissionService, times(1)).validateUser(anyLong(), anyString(), anyLong());
    }

    @Test
    void postQuestionSubmissionCommentIdInPostTest() throws Exception {
        doThrow(new IdInPostException("id in post")).when(questionSubmissionCommentService).postQuestionSubmissionComment(any(QuestionSubmissionCommentDto.class), anyLong(), any(SecuredInfo.class));

        assertThrows(IdInPostException.class, () -> questionSubmissionCommentController.postQuestionSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, questionSubmissionCommentDto, UriComponentsBuilder.newInstance(), httpServletRequest));
    }

    @Test
    void postQuestionSubmissionCommentAssessmentNotMatchingTest() throws Exception {
        doThrow(new AssessmentNotMatchingException("assessment not matching")).when(apiJwtService).assessmentAllowed(any(SecuredInfo.class), anyLong(), anyLong(), anyLong(), anyLong());

        assertThrows(AssessmentNotMatchingException.class, () -> questionSubmissionCommentController.postQuestionSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, questionSubmissionCommentDto, UriComponentsBuilder.newInstance(), httpServletRequest));
    }

    @Test
    void updateQuestionSubmissionCommentTest() throws Exception {
        ResponseEntity<Void> ret = questionSubmissionCommentController.updateQuestionSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, 1L, questionSubmissionCommentDto, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        verify(questionSubmissionCommentService, times(1)).updateQuestionSubmissionComment(questionSubmissionCommentDto, 1L, 1L, 1L, securedInfo);
    }

    @Test
    void updateQuestionSubmissionCommentUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<Void> ret = questionSubmissionCommentController.updateQuestionSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, 1L, questionSubmissionCommentDto, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        verify(questionSubmissionCommentService, never()).updateQuestionSubmissionComment(any(), anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void updateQuestionSubmissionCommentDataServiceExceptionTest() throws Exception {
        doThrow(new DataServiceException("data service exception")).when(questionSubmissionCommentService).updateQuestionSubmissionComment(any(QuestionSubmissionCommentDto.class), anyLong(), anyLong(), anyLong(), any(SecuredInfo.class));

        assertThrows(DataServiceException.class, () -> questionSubmissionCommentController.updateQuestionSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, 1L, questionSubmissionCommentDto, httpServletRequest));
    }

    @Test
    void updateQuestionSubmissionCommentNotMatchingTest() throws Exception {
        doThrow(new QuestionSubmissionNotMatchingException("question submission not matching")).when(apiJwtService).questionSubmissionAllowed(any(SecuredInfo.class), anyLong(), anyLong(), anyLong());

        assertThrows(QuestionSubmissionNotMatchingException.class, () -> questionSubmissionCommentController.updateQuestionSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, 1L, questionSubmissionCommentDto, httpServletRequest));
    }

    @Test
    void deleteQuestionSubmissionCommentTest() throws Exception {
        ResponseEntity<Void> ret = questionSubmissionCommentController.deleteQuestionSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
    }

    @Test
    void deleteQuestionSubmissionCommentUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<Void> ret = questionSubmissionCommentController.deleteQuestionSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void deleteQuestionSubmissionCommentNotFoundTest() throws Exception {
        doThrow(new EmptyResultDataAccessException(1)).when(questionSubmissionCommentService).deleteById(anyLong());

        ResponseEntity<Void> ret = questionSubmissionCommentController.deleteQuestionSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, ret.getStatusCode());
    }

    @Test
    void deleteQuestionSubmissionCommentNotMatchingTest() throws Exception {
        doThrow(new QuestionSubmissionCommentNotMatchingException("comment not matching")).when(apiJwtService).questionSubmissionCommentAllowed(any(SecuredInfo.class), anyLong(), anyLong());

        assertThrows(QuestionSubmissionCommentNotMatchingException.class, () -> questionSubmissionCommentController.deleteQuestionSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest));
    }

}
