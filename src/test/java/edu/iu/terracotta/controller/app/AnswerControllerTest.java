package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AnswerDto;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.utils.TextConstants;
import jakarta.servlet.http.HttpServletRequest;

public class AnswerControllerTest extends BaseTest {

    private AnswerController answerController;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        // manual construction: ApiJwtService is also implemented by canvasApiJwtService in
        // BaseServiceTest, so @InjectMocks constructor-injection (type-only matching) could wire
        // the wrong candidate.
        answerController = new AnswerController(apiJwtService, answerService, questionService);

        when(apiJwtService.extractValues(any(HttpServletRequest.class), eq(false))).thenReturn(securedInfo);
    }

    @Test
    void getAnswersByQuestionHappyPathTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(answerService.getQuestionType(1L)).thenReturn(QuestionTypes.MC.toString());
        when(answerService.findAllByQuestionIdMC(1L, false)).thenReturn(List.of(answerDto));

        ResponseEntity<List<AnswerDto>> ret = answerController.getAnswersByQuestion(1, 1, 1, 1, 1, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(1, ret.getBody().size());
    }

    @Test
    void getAnswersByQuestionEmptyTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(answerService.getQuestionType(1L)).thenReturn(QuestionTypes.MC.toString());
        when(answerService.findAllByQuestionIdMC(1L, false)).thenReturn(List.of());

        ResponseEntity<List<AnswerDto>> ret = answerController.getAnswersByQuestion(1, 1, 1, 1, 1, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, ret.getStatusCode());
    }

    @Test
    void getAnswersByQuestionUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<List<AnswerDto>> ret = answerController.getAnswersByQuestion(1, 1, 1, 1, 1, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void getAnswersByQuestionUnsupportedTypeTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(answerService.getQuestionType(1L)).thenReturn(QuestionTypes.ESSAY.toString());

        ResponseEntity<List<AnswerDto>> ret = answerController.getAnswersByQuestion(1, 1, 1, 1, 1, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, ret.getStatusCode());
    }

    @Test
    void getAnswersByQuestionPropagatesExperimentNotMatchingTest() throws Exception {
        doThrow(new ExperimentNotMatchingException("no match")).when(apiJwtService).experimentAllowed(any(SecuredInfo.class), anyLong());

        assertThrows(ExperimentNotMatchingException.class, () -> answerController.getAnswersByQuestion(1, 1, 1, 1, 1, httpServletRequest));
    }

    @Test
    void getAnswerHappyPathTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(answerService.getQuestionType(1L)).thenReturn(QuestionTypes.MC.toString());
        when(answerService.getAnswerMC(1L)).thenReturn(answerDto);

        ResponseEntity<AnswerDto> ret = answerController.getAnswer(1, 1, 1, 1, 1, 1, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(answerDto, ret.getBody());
    }

    @Test
    void getAnswerUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<AnswerDto> ret = answerController.getAnswer(1, 1, 1, 1, 1, 1, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void getAnswerUnsupportedTypeTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(answerService.getQuestionType(1L)).thenReturn(QuestionTypes.ESSAY.toString());

        ResponseEntity<AnswerDto> ret = answerController.getAnswer(1, 1, 1, 1, 1, 1, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, ret.getStatusCode());
    }

    @Test
    void postAnswerHappyPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        AnswerDto returnedDto = AnswerDto.builder().answerId(10L).build();
        when(answerService.postAnswerMC(any(AnswerDto.class), anyLong())).thenReturn(returnedDto);
        when(answerService.buildHeaders(any(UriComponentsBuilder.class), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), eq(10L))).thenReturn(new HttpHeaders());

        AnswerDto inputDto = AnswerDto.builder().answerType("MC").questionId(1L).build();
        ResponseEntity<AnswerDto> ret = answerController.postAnswer(1, 1, 1, 1, 1, inputDto, mock(UriComponentsBuilder.class), httpServletRequest);

        assertEquals(HttpStatus.CREATED, ret.getStatusCode());
        assertEquals(returnedDto, ret.getBody());
    }

    @Test
    void postAnswerUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<AnswerDto> ret = answerController.postAnswer(1, 1, 1, 1, 1, AnswerDto.builder().build(), mock(UriComponentsBuilder.class), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void postAnswerPropagatesMultipleChoiceLimitReachedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        doThrow(new MultipleChoiceLimitReachedException("limit reached")).when(answerService).postAnswerMC(any(AnswerDto.class), anyLong());

        AnswerDto inputDto = AnswerDto.builder().answerType("MC").questionId(1L).build();
        UriComponentsBuilder ucBuilder = mock(UriComponentsBuilder.class);

        assertThrows(MultipleChoiceLimitReachedException.class, () -> answerController.postAnswer(1, 1, 1, 1, 1, inputDto, ucBuilder, httpServletRequest));
    }

    @Test
    void updateAnswersHappyPathTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(answerService.getQuestionType(1L)).thenReturn(QuestionTypes.MC.toString());
        when(answerService.findByAnswerId(anyLong())).thenReturn(answerMc);
        List<AnswerDto> updated = List.of(AnswerDto.builder().answerId(1L).build());
        when(answerService.updateAnswerMC(anyMap())).thenReturn(updated);

        ResponseEntity<List<AnswerDto>> ret = answerController.updateAnswers(1, 1, 1, 1, 1, List.of(AnswerDto.builder().answerId(1L).build()), httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(updated, ret.getBody());
    }

    @Test
    void updateAnswersUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<List<AnswerDto>> ret = answerController.updateAnswers(1, 1, 1, 1, 1, List.of(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void updateAnswersUnsupportedTypeTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(answerService.getQuestionType(1L)).thenReturn(QuestionTypes.ESSAY.toString());

        ResponseEntity<List<AnswerDto>> ret = answerController.updateAnswers(1, 1, 1, 1, 1, List.of(), httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, ret.getStatusCode());
    }

    @Test
    void updateAnswersServiceExceptionWrappedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(answerService.getQuestionType(1L)).thenReturn(QuestionTypes.MC.toString());
        when(answerService.findByAnswerId(anyLong())).thenReturn(answerMc);
        when(answerService.updateAnswerMC(anyMap())).thenThrow(new RuntimeException("db failure"));

        List<AnswerDto> answerDtoList = List.of(AnswerDto.builder().answerId(1L).build());

        assertThrows(DataServiceException.class, () -> answerController.updateAnswers(1, 1, 1, 1, 1, answerDtoList, httpServletRequest));
    }

    @Test
    void updateAnswerHappyPathTest() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);
        when(questionService.findByQuestionId(anyLong())).thenReturn(question);
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(answerService.findByAnswerId(anyLong())).thenReturn(answerMc);
        AnswerDto updatedDto = AnswerDto.builder().answerId(5L).build();
        when(answerService.updateAnswerMC(anyMap())).thenReturn(List.of(updatedDto));

        ResponseEntity<AnswerDto> ret = answerController.updateAnswer(1, 1, 1, 1, 1, 5, AnswerDto.builder().answerId(5L).build(), httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(updatedDto, ret.getBody());
    }

    @Test
    void updateAnswerUnauthorizedTest() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);
        when(questionService.findByQuestionId(anyLong())).thenReturn(question);
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<AnswerDto> ret = answerController.updateAnswer(1, 1, 1, 1, 1, 5, AnswerDto.builder().build(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void updateAnswerUnsupportedTypeTest() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);
        when(questionService.findByQuestionId(anyLong())).thenReturn(question);
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);

        ResponseEntity<AnswerDto> ret = answerController.updateAnswer(1, 1, 1, 1, 1, 5, AnswerDto.builder().build(), httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, ret.getStatusCode());
    }

    @Test
    void updateAnswerNoAnswersUpdatedThrowsTest() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);
        when(questionService.findByQuestionId(anyLong())).thenReturn(question);
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        when(answerService.findByAnswerId(anyLong())).thenReturn(answerMc);
        when(answerService.updateAnswerMC(anyMap())).thenReturn(List.of());

        AnswerDto dto = AnswerDto.builder().answerId(5L).build();

        assertThrows(DataServiceException.class, () -> answerController.updateAnswer(1, 1, 1, 1, 1, 5, dto, httpServletRequest));
    }

    @Test
    void deleteAnswerHappyPathTest() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);
        when(questionService.findByQuestionId(anyLong())).thenReturn(question);
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);

        ResponseEntity<Void> ret = answerController.deleteAnswer(1, 1, 1, 1, 1, 1, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
    }

    @Test
    void deleteAnswerUnauthorizedTest() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);
        when(questionService.findByQuestionId(anyLong())).thenReturn(question);
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<Void> ret = answerController.deleteAnswer(1, 1, 1, 1, 1, 1, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, ret.getBody());
    }

    @Test
    void deleteAnswerUnsupportedTypeTest() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);
        when(questionService.findByQuestionId(anyLong())).thenReturn(question);
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);

        ResponseEntity<Void> ret = answerController.deleteAnswer(1, 1, 1, 1, 1, 1, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, ret.getStatusCode());
    }

    @Test
    void deleteAnswerNotFoundTest() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);
        when(questionService.findByQuestionId(anyLong())).thenReturn(question);
        when(apiJwtService.isInstructorOrHigher(any(SecuredInfo.class))).thenReturn(true);
        doThrow(new EmptyResultDataAccessException(1)).when(answerService).deleteByIdMC(anyLong());

        ResponseEntity<Void> ret = answerController.deleteAnswer(1, 1, 1, 1, 1, 1, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, ret.getStatusCode());
    }

    @Test
    void deleteAnswerPropagatesAnswerNotMatchingTest() throws Exception {
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);
        when(questionService.findByQuestionId(anyLong())).thenReturn(question);
        doThrow(new AnswerNotMatchingException("no match")).when(apiJwtService).answerAllowed(any(SecuredInfo.class), anyLong(), anyLong(), any(), anyLong());

        assertThrows(AnswerNotMatchingException.class, () -> answerController.deleteAnswer(1, 1, 1, 1, 1, 1, httpServletRequest));
    }

}
