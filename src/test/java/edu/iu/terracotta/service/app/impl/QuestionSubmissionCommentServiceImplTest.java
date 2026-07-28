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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.QuestionSubmissionComment;
import edu.iu.terracotta.dao.model.dto.QuestionSubmissionCommentDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.utils.TextConstants;

public class QuestionSubmissionCommentServiceImplTest extends BaseTest {

    @InjectMocks private QuestionSubmissionCommentServiceImpl questionSubmissionCommentService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        when(questionSubmissionRepository.findById(anyLong())).thenReturn(Optional.of(questionSubmission));
    }

    @Test
    public void testGetQuestionSubmissionCommentsSuccess() {
        QuestionSubmissionComment comment = QuestionSubmissionComment.builder().questionSubmissionCommentId(1L).comment("comment").creator("creator").questionSubmission(questionSubmission).build();
        when(questionSubmissionCommentRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(List.of(comment));

        List<QuestionSubmissionCommentDto> retVal = questionSubmissionCommentService.getQuestionSubmissionComments(1L);

        assertEquals(1, retVal.size());
        assertEquals("comment", retVal.get(0).getComment());
        assertEquals(1L, retVal.get(0).getQuestionSubmissionId());
    }

    @Test
    public void testGetQuestionSubmissionCommentsEmpty() {
        when(questionSubmissionCommentRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(null);

        List<QuestionSubmissionCommentDto> retVal = questionSubmissionCommentService.getQuestionSubmissionComments(1L);

        assertTrue(retVal.isEmpty());
    }

    @Test
    public void testGetQuestionSubmissionCommentsEmptyList() {
        when(questionSubmissionCommentRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.emptyList());

        List<QuestionSubmissionCommentDto> retVal = questionSubmissionCommentService.getQuestionSubmissionComments(1L);

        assertTrue(retVal.isEmpty());
    }

    @Test
    public void testGetQuestionSubmissionComment() {
        QuestionSubmissionComment comment = mock(QuestionSubmissionComment.class);
        when(questionSubmissionCommentRepository.findByQuestionSubmissionCommentId(anyLong())).thenReturn(comment);

        QuestionSubmissionComment retVal = questionSubmissionCommentService.getQuestionSubmissionComment(1L);

        assertEquals(comment, retVal);
    }

    @Test
    public void testPostQuestionSubmissionCommentSuccess() throws IdInPostException, DataServiceException {
        QuestionSubmissionCommentDto dto = QuestionSubmissionCommentDto.builder().comment("new comment").build();
        QuestionSubmissionComment saved = QuestionSubmissionComment.builder().questionSubmissionCommentId(1L).comment("new comment").creator("Terracotta User").questionSubmission(questionSubmission).build();
        when(questionSubmissionCommentRepository.save(any(QuestionSubmissionComment.class))).thenReturn(saved);

        QuestionSubmissionCommentDto retVal = questionSubmissionCommentService.postQuestionSubmissionComment(dto, 1L, securedInfo);

        assertNotNull(retVal);
        assertEquals(1L, retVal.getQuestionSubmissionCommentId());
        assertEquals("new comment", retVal.getComment());
        assertEquals("Terracotta User", retVal.getCreator());
    }

    @Test
    public void testPostQuestionSubmissionCommentIdInPostExceptionThrows() {
        QuestionSubmissionCommentDto dto = QuestionSubmissionCommentDto.builder().questionSubmissionCommentId(5L).build();

        Exception exception = assertThrows(IdInPostException.class, () -> questionSubmissionCommentService.postQuestionSubmissionComment(dto, 1L, securedInfo));

        assertEquals(TextConstants.ID_IN_POST_ERROR, exception.getMessage());
    }

    @Test
    public void testPostQuestionSubmissionCommentQuestionSubmissionNotFoundThrows() {
        when(questionSubmissionRepository.findById(anyLong())).thenReturn(Optional.empty());
        QuestionSubmissionCommentDto dto = QuestionSubmissionCommentDto.builder().build();

        Exception exception = assertThrows(DataServiceException.class, () -> questionSubmissionCommentService.postQuestionSubmissionComment(dto, 1L, securedInfo));

        assertTrue(exception.getMessage().startsWith("Error 105: Unable to create question submission comment:"));
    }

    @Test
    public void testUpdateQuestionSubmissionCommentSuccess() throws DataServiceException {
        QuestionSubmissionComment comment = QuestionSubmissionComment.builder().questionSubmissionCommentId(1L).comment("old comment").creator("Terracotta User").questionSubmission(questionSubmission).build();
        when(questionSubmissionCommentRepository.findByQuestionSubmissionCommentId(anyLong())).thenReturn(comment);
        QuestionSubmissionCommentDto dto = QuestionSubmissionCommentDto.builder().comment("updated comment").build();

        questionSubmissionCommentService.updateQuestionSubmissionComment(dto, 1L, 1L, 1L, securedInfo);

        assertEquals("updated comment", comment.getComment());
        verify(questionSubmissionCommentRepository).saveAndFlush(comment);
    }

    @Test
    public void testUpdateQuestionSubmissionCommentNotCreatorThrows() {
        QuestionSubmissionComment comment = QuestionSubmissionComment.builder().questionSubmissionCommentId(1L).comment("old comment").creator("Someone Else").questionSubmission(questionSubmission).build();
        when(questionSubmissionCommentRepository.findByQuestionSubmissionCommentId(anyLong())).thenReturn(comment);
        QuestionSubmissionCommentDto dto = QuestionSubmissionCommentDto.builder().comment("updated comment").build();

        Exception exception = assertThrows(DataServiceException.class, () -> questionSubmissionCommentService.updateQuestionSubmissionComment(dto, 1L, 1L, 1L, securedInfo));

        assertEquals("Error 122: Only the creator of a comment can edit their own comment.", exception.getMessage());
        assertEquals("old comment", comment.getComment());
    }

    @Test
    public void testToDto() {
        QuestionSubmissionComment comment = QuestionSubmissionComment.builder().questionSubmissionCommentId(1L).comment("comment").creator("creator").questionSubmission(questionSubmission).build();

        QuestionSubmissionCommentDto retVal = questionSubmissionCommentService.toDto(comment);

        assertEquals(1L, retVal.getQuestionSubmissionCommentId());
        assertEquals(1L, retVal.getQuestionSubmissionId());
        assertEquals("comment", retVal.getComment());
        assertEquals("creator", retVal.getCreator());
    }

    @Test
    public void testFromDtoSuccess() throws DataServiceException {
        QuestionSubmissionCommentDto dto = QuestionSubmissionCommentDto.builder().questionSubmissionCommentId(1L).questionSubmissionId(1L).comment("comment").creator("creator").build();

        QuestionSubmissionComment retVal = questionSubmissionCommentService.fromDto(dto);

        assertEquals(1L, retVal.getQuestionSubmissionCommentId());
        assertEquals("comment", retVal.getComment());
        assertEquals("creator", retVal.getCreator());
        assertEquals(questionSubmission, retVal.getQuestionSubmission());
    }

    @Test
    public void testFromDtoQuestionSubmissionNotFoundThrows() {
        when(questionSubmissionRepository.findById(anyLong())).thenReturn(Optional.empty());
        QuestionSubmissionCommentDto dto = QuestionSubmissionCommentDto.builder().questionSubmissionId(1L).build();

        Exception exception = assertThrows(DataServiceException.class, () -> questionSubmissionCommentService.fromDto(dto));

        assertEquals("The question submission for the question submission comment doesn't exist.", exception.getMessage());
    }

    @Test
    public void testDeleteById() {
        questionSubmissionCommentService.deleteById(1L);

        verify(questionSubmissionCommentRepository).deleteByQuestionSubmissionCommentId(1L);
    }

    @Test
    public void testBuildHeaders() {
        HttpHeaders retVal = questionSubmissionCommentService.buildHeaders(UriComponentsBuilder.newInstance(), 1L, 2L, 3L, 4L, 5L, 6L, 7L);

        assertNotNull(retVal);
        assertNotNull(retVal.getLocation());
        assertTrue(retVal.getLocation().toString().endsWith("/api/experiments/1/conditions/2/treatments/3/assessments/4/submissions/5/question_submissions/6/question_submission_comments/7"));
    }

}
