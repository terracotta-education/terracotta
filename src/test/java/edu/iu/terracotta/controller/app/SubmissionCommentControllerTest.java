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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.SubmissionComment;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.SubmissionCommentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.SubmissionCommentDto;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.service.app.SubmissionCommentService;
import edu.iu.terracotta.utils.TextConstants;
import jakarta.servlet.http.HttpServletRequest;

public class SubmissionCommentControllerTest extends BaseTest {

    // SubmissionCommentService and SubmissionCommentDto have no mock declared anywhere in the BaseTest hierarchy, so they are declared here.
    @Mock private SubmissionCommentService submissionCommentService;
    @Mock private SubmissionCommentDto submissionCommentDto;

    private SubmissionCommentController submissionCommentController;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        submissionCommentController = new SubmissionCommentController(ltiUserRepository, apiJwtService, submissionService, submissionCommentService);

        when(apiJwtService.extractValues(any(HttpServletRequest.class), anyBoolean())).thenReturn(securedInfo);
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(submissionCommentDto.getSubmissionCommentId()).thenReturn(1L);
    }

    @Test
    void getSubmissionCommentsBySubmissionTest() throws Exception {
        when(submissionCommentService.getSubmissionComments(anyLong())).thenReturn(List.of(submissionCommentDto));

        ResponseEntity<List<SubmissionCommentDto>> ret = submissionCommentController.getSubmissionCommentsBySubmission(1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(1, ret.getBody().size());
    }

    @Test
    void getSubmissionCommentsBySubmissionNoContentTest() throws Exception {
        when(submissionCommentService.getSubmissionComments(anyLong())).thenReturn(Collections.emptyList());

        ResponseEntity<List<SubmissionCommentDto>> ret = submissionCommentController.getSubmissionCommentsBySubmission(1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, ret.getStatusCode());
    }

    @Test
    void getSubmissionCommentsBySubmissionUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<List<SubmissionCommentDto>> ret = submissionCommentController.getSubmissionCommentsBySubmission(1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void getSubmissionCommentsBySubmissionStudentValidatesUserTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);
        when(submissionCommentService.getSubmissionComments(anyLong())).thenReturn(List.of(submissionCommentDto));

        ResponseEntity<List<SubmissionCommentDto>> ret = submissionCommentController.getSubmissionCommentsBySubmission(1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        verify(submissionService, times(1)).validateUser(anyLong(), anyString(), anyLong());
    }

    @Test
    void getSubmissionCommentsBySubmissionInvalidUserTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);
        doThrow(new InvalidUserException("invalid user")).when(submissionService).validateUser(anyLong(), anyString(), anyLong());

        assertThrows(InvalidUserException.class, () -> submissionCommentController.getSubmissionCommentsBySubmission(1L, 1L, 1L, 1L, 1L, httpServletRequest));
    }

    @Test
    void getSubmissionCommentsBySubmissionExperimentNotMatchingTest() throws Exception {
        doThrow(new ExperimentNotMatchingException("experiment not matching")).when(apiJwtService).experimentAllowed(any(SecuredInfo.class), anyLong());

        assertThrows(ExperimentNotMatchingException.class, () -> submissionCommentController.getSubmissionCommentsBySubmission(1L, 1L, 1L, 1L, 1L, httpServletRequest));
    }

    @Test
    void getSubmissionCommentTest() throws Exception {
        SubmissionComment entity = SubmissionComment.builder().submissionCommentId(1L).creator(DISPLAY_NAME).build();
        when(submissionCommentService.getSubmissionComment(anyLong())).thenReturn(entity);
        when(submissionCommentService.toDto(entity)).thenReturn(submissionCommentDto);

        ResponseEntity<SubmissionCommentDto> ret = submissionCommentController.getSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(submissionCommentDto, ret.getBody());
    }

    @Test
    void getSubmissionCommentUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<SubmissionCommentDto> ret = submissionCommentController.getSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void getSubmissionCommentNotMatchingTest() throws Exception {
        doThrow(new SubmissionCommentNotMatchingException("comment not matching")).when(apiJwtService).submissionCommentAllowed(any(SecuredInfo.class), anyLong(), anyLong(), anyLong());

        assertThrows(SubmissionCommentNotMatchingException.class, () -> submissionCommentController.getSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest));
    }

    @Test
    void postSubmissionCommentTest() throws Exception {
        when(submissionCommentService.postSubmissionComment(any(SubmissionCommentDto.class), anyLong(), any(SecuredInfo.class))).thenReturn(submissionCommentDto);
        when(submissionCommentService.buildHeaders(any(UriComponentsBuilder.class), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong())).thenReturn(new HttpHeaders());

        ResponseEntity<SubmissionCommentDto> ret = submissionCommentController.postSubmissionComment(1L, 1L, 1L, 1L, 1L, submissionCommentDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.CREATED, ret.getStatusCode());
        assertEquals(submissionCommentDto, ret.getBody());
    }

    @Test
    void postSubmissionCommentUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<SubmissionCommentDto> ret = submissionCommentController.postSubmissionComment(1L, 1L, 1L, 1L, 1L, submissionCommentDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void postSubmissionCommentStudentValidatesUserTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);
        when(submissionCommentService.postSubmissionComment(any(SubmissionCommentDto.class), anyLong(), any(SecuredInfo.class))).thenReturn(submissionCommentDto);
        when(submissionCommentService.buildHeaders(any(UriComponentsBuilder.class), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong())).thenReturn(new HttpHeaders());

        submissionCommentController.postSubmissionComment(1L, 1L, 1L, 1L, 1L, submissionCommentDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        verify(submissionService, times(1)).validateUser(anyLong(), anyString(), anyLong());
    }

    @Test
    void postSubmissionCommentIdInPostTest() throws Exception {
        doThrow(new IdInPostException("id in post")).when(submissionCommentService).postSubmissionComment(any(SubmissionCommentDto.class), anyLong(), any(SecuredInfo.class));

        assertThrows(IdInPostException.class, () -> submissionCommentController.postSubmissionComment(1L, 1L, 1L, 1L, 1L, submissionCommentDto, UriComponentsBuilder.newInstance(), httpServletRequest));
    }

    @Test
    void postSubmissionCommentAssessmentNotMatchingTest() throws Exception {
        doThrow(new AssessmentNotMatchingException("assessment not matching")).when(apiJwtService).assessmentAllowed(any(SecuredInfo.class), anyLong(), anyLong(), anyLong(), anyLong());

        assertThrows(AssessmentNotMatchingException.class, () -> submissionCommentController.postSubmissionComment(1L, 1L, 1L, 1L, 1L, submissionCommentDto, UriComponentsBuilder.newInstance(), httpServletRequest));
    }

    @Test
    void updateSubmissionCommentTest() throws Exception {
        SubmissionComment entity = SubmissionComment.builder().submissionCommentId(1L).creator(DISPLAY_NAME).build();
        when(submissionCommentService.getSubmissionComment(anyLong())).thenReturn(entity);
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong())).thenReturn(ltiUserEntity);

        ResponseEntity<Void> ret = submissionCommentController.updateSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, submissionCommentDto, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        verify(submissionCommentService, times(1)).updateSubmissionComment(entity, submissionCommentDto);
    }

    @Test
    void updateSubmissionCommentNotCreatorTest() throws Exception {
        SubmissionComment entity = SubmissionComment.builder().submissionCommentId(1L).creator("someone else").build();
        when(submissionCommentService.getSubmissionComment(anyLong())).thenReturn(entity);
        when(ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(anyString(), anyLong())).thenReturn(ltiUserEntity);

        ResponseEntity<Void> ret = submissionCommentController.updateSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, submissionCommentDto, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        verify(submissionCommentService, never()).updateSubmissionComment(any(), any());
    }

    @Test
    void updateSubmissionCommentUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<Void> ret = submissionCommentController.updateSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, submissionCommentDto, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        verify(submissionCommentService, never()).updateSubmissionComment(any(), any());
    }

    @Test
    void updateSubmissionCommentNotMatchingTest() throws Exception {
        doThrow(new SubmissionCommentNotMatchingException("comment not matching")).when(apiJwtService).submissionCommentAllowed(any(SecuredInfo.class), anyLong(), anyLong(), anyLong());

        assertThrows(SubmissionCommentNotMatchingException.class, () -> submissionCommentController.updateSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, submissionCommentDto, httpServletRequest));
    }

    @Test
    void deleteSubmissionCommentTest() throws Exception {
        ResponseEntity<Void> ret = submissionCommentController.deleteSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
    }

    @Test
    void deleteSubmissionCommentUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<Void> ret = submissionCommentController.deleteSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void deleteSubmissionCommentNotFoundTest() throws Exception {
        doThrow(new EmptyResultDataAccessException(1)).when(submissionCommentService).deleteById(anyLong());

        ResponseEntity<Void> ret = submissionCommentController.deleteSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, ret.getStatusCode());
    }

    @Test
    void deleteSubmissionCommentAssessmentNotMatchingTest() throws Exception {
        doThrow(new AssessmentNotMatchingException("assessment not matching")).when(apiJwtService).assessmentAllowed(any(SecuredInfo.class), anyLong(), anyLong(), anyLong(), anyLong());

        assertThrows(AssessmentNotMatchingException.class, () -> submissionCommentController.deleteSubmissionComment(1L, 1L, 1L, 1L, 1L, 1L, httpServletRequest));
    }

}
