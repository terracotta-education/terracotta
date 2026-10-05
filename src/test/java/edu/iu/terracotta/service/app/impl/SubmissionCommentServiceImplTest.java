package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.SubmissionComment;
import edu.iu.terracotta.dao.exceptions.SubmissionCommentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.SubmissionCommentDto;
import edu.iu.terracotta.dao.repository.SubmissionCommentRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.utils.TextConstants;

public class SubmissionCommentServiceImplTest extends BaseTest {

    @Mock private SubmissionCommentRepository submissionCommentRepository;

    @InjectMocks private SubmissionCommentServiceImpl submissionCommentService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    public void testGetSubmissionCommentsSuccess() {
        SubmissionComment comment = SubmissionComment.builder().submissionCommentId(1L).comment("comment").creator("creator").submission(submission).build();
        comment.setUuid(UUID.randomUUID());
        when(submissionCommentRepository.findBySubmission_SubmissionId(anyLong())).thenReturn(List.of(comment));

        List<SubmissionCommentDto> retVal = submissionCommentService.getSubmissionComments(1L);

        assertEquals(1, retVal.size());
        assertEquals("comment", retVal.get(0).getComment());
        assertEquals(submission.getUuid(), retVal.get(0).getSubmissionId());
    }

    @Test
    public void testGetSubmissionCommentsEmpty() {
        when(submissionCommentRepository.findBySubmission_SubmissionId(anyLong())).thenReturn(null);

        List<SubmissionCommentDto> retVal = submissionCommentService.getSubmissionComments(1L);

        assertTrue(retVal.isEmpty());
    }

    @Test
    public void testGetSubmissionCommentsEmptyList() {
        when(submissionCommentRepository.findBySubmission_SubmissionId(anyLong())).thenReturn(Collections.emptyList());

        List<SubmissionCommentDto> retVal = submissionCommentService.getSubmissionComments(1L);

        assertTrue(retVal.isEmpty());
    }

    @Test
    public void testGetSubmissionComment() {
        SubmissionComment comment = mock(SubmissionComment.class);
        when(submissionCommentRepository.findBySubmissionCommentId(anyLong())).thenReturn(comment);

        SubmissionComment retVal = submissionCommentService.getSubmissionComment(1L);

        assertEquals(comment, retVal);
    }

    @Test
    public void testPostSubmissionCommentSuccess() throws IdInPostException, DataServiceException {
        SubmissionCommentDto dto = SubmissionCommentDto.builder().comment("new comment").build();
        SubmissionComment saved = SubmissionComment.builder().submissionCommentId(1L).comment("new comment").creator("Terracotta User").submission(submission).build();
        saved.setUuid(UUID.randomUUID());
        when(submissionCommentRepository.save(any(SubmissionComment.class))).thenReturn(saved);

        SubmissionCommentDto retVal = submissionCommentService.postSubmissionComment(dto, 1L, securedInfo);

        assertNotNull(retVal);
        assertEquals(saved.getUuid(), retVal.getSubmissionCommentId());
        assertEquals("new comment", retVal.getComment());
        assertEquals("Terracotta User", retVal.getCreator());
    }

    @Test
    public void testPostSubmissionCommentIdInPostExceptionThrows() {
        SubmissionCommentDto dto = SubmissionCommentDto.builder().submissionCommentId(UUID.randomUUID()).build();

        Exception exception = assertThrows(IdInPostException.class, () -> submissionCommentService.postSubmissionComment(dto, 1L, securedInfo));

        assertEquals(TextConstants.ID_IN_POST_ERROR, exception.getMessage());
    }

    @Test
    public void testPostSubmissionCommentSubmissionNotFoundThrows() {
        // findByUuid must be overridden too (not just findById): the global BaseRepositoryTest
        // findByUuid(any(UUID.class)) stub also matches a null uuid, which is what a not-found
        // findById resolves to below - see SubmissionCommentServiceImpl.postSubmissionComment/fromDto.
        when(submissionRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(submissionRepository.findByUuid(any())).thenReturn(null);
        SubmissionCommentDto dto = SubmissionCommentDto.builder().build();

        Exception exception = assertThrows(DataServiceException.class, () -> submissionCommentService.postSubmissionComment(dto, 1L, securedInfo));

        assertTrue(exception.getMessage().startsWith("Error 105: Unable to create submission comment:"));
    }

    @Test
    public void testUpdateSubmissionComment() {
        SubmissionComment comment = SubmissionComment.builder().comment("old comment").build();
        SubmissionCommentDto dto = SubmissionCommentDto.builder().comment("updated comment").build();

        submissionCommentService.updateSubmissionComment(comment, dto);

        assertEquals("updated comment", comment.getComment());
        verify(submissionCommentRepository).saveAndFlush(comment);
    }

    @Test
    public void testToDto() {
        SubmissionComment comment = SubmissionComment.builder().submissionCommentId(1L).comment("comment").creator("creator").submission(submission).build();
        comment.setUuid(UUID.randomUUID());

        SubmissionCommentDto retVal = submissionCommentService.toDto(comment);

        assertEquals(comment.getUuid(), retVal.getSubmissionCommentId());
        assertEquals(submission.getUuid(), retVal.getSubmissionId());
        assertEquals("comment", retVal.getComment());
        assertEquals("creator", retVal.getCreator());
    }

    @Test
    public void testFromDtoSuccess() throws DataServiceException {
        SubmissionCommentDto dto = SubmissionCommentDto.builder().submissionCommentId(UUID.randomUUID()).submissionId(UUID.randomUUID()).comment("comment").creator("creator").build();

        SubmissionComment retVal = submissionCommentService.fromDto(dto);

        assertEquals("comment", retVal.getComment());
        assertEquals("creator", retVal.getCreator());
        assertEquals(submission, retVal.getSubmission());
    }

    @Test
    public void testFromDtoSubmissionNotFoundThrows() {
        when(submissionRepository.findByUuid(any(UUID.class))).thenReturn(null);
        SubmissionCommentDto dto = SubmissionCommentDto.builder().submissionId(UUID.randomUUID()).build();

        Exception exception = assertThrows(DataServiceException.class, () -> submissionCommentService.fromDto(dto));

        assertEquals("The submission for the submission comment doesn't exist.", exception.getMessage());
    }

    @Test
    public void testDeleteById() {
        submissionCommentService.deleteById(1L);

        verify(submissionCommentRepository).deleteBySubmissionCommentId(1L);
    }

    @Test
    public void testBuildHeaders() {
        UUID experimentUuid = UUID.randomUUID();
        UUID conditionUuid = UUID.randomUUID();
        UUID treatmentUuid = UUID.randomUUID();
        UUID assessmentUuid = UUID.randomUUID();
        UUID submissionUuid = UUID.randomUUID();
        UUID submissionCommentUuid = UUID.randomUUID();

        HttpHeaders retVal = submissionCommentService.buildHeaders(UriComponentsBuilder.newInstance(), experimentUuid, conditionUuid, treatmentUuid, assessmentUuid, submissionUuid, submissionCommentUuid);

        assertNotNull(retVal);
        assertNotNull(retVal.getLocation());
        assertTrue(retVal.getLocation().toString().endsWith(
            "/api/experiments/" + experimentUuid + "/conditions/" + conditionUuid + "/treatments/" + treatmentUuid + "/assessments/" + assessmentUuid + "/submissions/" + submissionUuid + "/submission_comments/" + submissionCommentUuid));
    }

    // getSubmissionCommentByUuid

    @Test
    public void testGetSubmissionCommentByUuidFound() throws Exception {
        SubmissionComment comment = mock(SubmissionComment.class);
        UUID uuid = UUID.randomUUID();
        when(submissionCommentRepository.findByUuid(uuid)).thenReturn(comment);

        SubmissionComment retVal = submissionCommentService.getSubmissionCommentByUuid(uuid);

        assertEquals(comment, retVal);
    }

    @Test
    public void testGetSubmissionCommentByUuidNotFoundThrows() {
        UUID uuid = UUID.randomUUID();
        when(submissionCommentRepository.findByUuid(uuid)).thenReturn(null);

        Exception exception = assertThrows(SubmissionCommentNotMatchingException.class, () -> submissionCommentService.getSubmissionCommentByUuid(uuid));

        assertEquals(TextConstants.SUBMISSION_COMMENT_NOT_MATCHING, exception.getMessage());
    }

    @Test
    public void testGetSubmissionCommentIdByUuidFound() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(submissionCommentRepository.findIdByUuid(uuid)).thenReturn(Optional.of(42L));

        assertEquals(42L, submissionCommentService.getSubmissionCommentIdByUuid(uuid));
    }

    @Test
    public void testGetSubmissionCommentIdByUuidNotFoundThrows() {
        UUID uuid = UUID.randomUUID();
        when(submissionCommentRepository.findIdByUuid(uuid)).thenReturn(Optional.empty());

        assertThrows(SubmissionCommentNotMatchingException.class, () -> submissionCommentService.getSubmissionCommentIdByUuid(uuid));
    }

}
