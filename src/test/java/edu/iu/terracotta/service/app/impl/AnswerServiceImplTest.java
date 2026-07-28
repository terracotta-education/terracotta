package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.AnswerMc;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AnswerDto;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.utils.TextConstants;

public class AnswerServiceImplTest extends BaseTest {

    @InjectMocks private AnswerServiceImpl answerService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
        clearInvocations(answerMcRepository);
        // @InjectMocks only does constructor injection here (all constructor params already match), so
        // the separate @PersistenceContext EntityManager field is never populated unless set explicitly
        org.springframework.test.util.ReflectionTestUtils.setField(answerService, "entityManager", entityManager);

        when(answerMcRepository.findByQuestion_QuestionId(anyLong())).thenReturn(Collections.singletonList(answerMc));
        when(answerMc.getAnswerOrder()).thenReturn(0);

        when(fileStorageService.parseHTMLFiles(anyString(), anyString())).thenReturn("html");
    }

    @Test
    public void testToDtoMCShowCorrectAnswers() {
        AnswerDto answerDto = answerService.toDtoMC(answerMc, 0, true);

        assertNotNull(answerDto.getCorrect());
    }

    @Test
    public void testToDtoMCDoNotShowCorrectAnswers() {
        AnswerDto answerDto = answerService.toDtoMC(answerMc, 0, false);

        assertNull(answerDto.getCorrect());
    }

    @Test
    public void testDuplicateAnswersForQuestionNotMC() throws QuestionNotMatchingException {
        List<AnswerMc> retList = answerService.duplicateAnswersForQuestion(1L, question);

        assertEquals(0, retList.size());
        verify(answerMcRepository, never()).findByQuestion_QuestionId(anyLong());
        verify(answerMcRepository, never()).save(any(AnswerMc.class));
    }

    @Test
    public void testDuplicateAnswersForQuestionNoOriginalId() {
        Exception exception = assertThrows(QuestionNotMatchingException.class, () -> { answerService.duplicateAnswersForQuestion(null, question); });

        assertEquals(TextConstants.QUESTION_NOT_MATCHING, exception.getMessage());
    }

    @Test
    public void testDuplicateAnswersForQuestionNoNewQuestion() {
        Exception exception = assertThrows(QuestionNotMatchingException.class, () -> { answerService.duplicateAnswersForQuestion(1l, null); });

        assertEquals(TextConstants.QUESTION_NOT_MATCHING, exception.getMessage());
    }

    @Test
    public void testDuplicateAnswersForQuestionSuccess() throws QuestionNotMatchingException {
        when(answerMcRepository.save(any(AnswerMc.class))).thenReturn(answerMc);

        List<AnswerMc> retList = answerService.duplicateAnswersForQuestion(1L, questionMc);

        assertEquals(1, retList.size());
        verify(answerMcRepository).findByQuestion_QuestionId(1L);
        verify(entityManager).detach(answerMc);
        verify(answerMcRepository).save(answerMc);
    }

    @Test
    public void testFindAllByQuestionIdMCLong() {
        List<AnswerDto> answerDtoList = answerService.findAllByQuestionIdMC(1L, true);

        assertEquals(1, answerDtoList.size());
        assertNotNull(answerDtoList.get(0).getCorrect());
    }

    @Test
    public void testFindAllByQuestionIdMCLongEmpty() {
        when(answerMcRepository.findByQuestion_QuestionId(anyLong())).thenReturn(null);

        List<AnswerDto> answerDtoList = answerService.findAllByQuestionIdMC(1L, true);

        assertTrue(answerDtoList.isEmpty());
    }

    @Test
    public void testFindAllByQuestionIdMCSubmissionMatchedAndMissing() {
        AnswerMc answerMc2 = mock(AnswerMc.class);
        when(answerMc2.getAnswerMcId()).thenReturn(2L);
        when(answerMc2.getQuestion()).thenReturn(questionMc);

        when(answerMcRepository.findByQuestion_QuestionId(anyLong())).thenReturn(List.of(answerMc, answerMc2));
        when(questionSubmission.getAnswerMcSubmissionOptions()).thenReturn(new ArrayList<>(List.of(answerMcSubmissionOption)));
        when(answerMcSubmissionOption.getAnswerMc()).thenReturn(answerMc);

        List<AnswerDto> answerDtoList = answerService.findAllByQuestionIdMC(questionSubmission, true);

        // one from the submission options (answerMc, matched/skipped in the missing-answer pass)
        // and one appended because it was missing from the submission options (answerMc2)
        assertEquals(2, answerDtoList.size());
        assertEquals(1L, answerDtoList.get(0).getAnswerId());
        assertEquals(2L, answerDtoList.get(1).getAnswerId());
    }

    @Test
    public void testFindAllByQuestionIdMCSubmissionNoOptions() {
        when(questionSubmission.getAnswerMcSubmissionOptions()).thenReturn(new ArrayList<>());

        List<AnswerDto> answerDtoList = answerService.findAllByQuestionIdMC(questionSubmission, false);

        assertEquals(1, answerDtoList.size());
        assertNull(answerDtoList.get(0).getCorrect());
    }

    @Test
    public void testGetAnswerMC() {
        when(answerMcRepository.findByAnswerMcId(anyLong())).thenReturn(answerMc);

        AnswerDto answerDto = answerService.getAnswerMC(1L);

        assertNotNull(answerDto);
        assertEquals(1L, answerDto.getAnswerId());
    }

    @Test
    public void testPostAnswerMCIdInPost() {
        AnswerDto answerDto = AnswerDto.builder().answerId(1L).build();

        Exception exception = assertThrows(IdInPostException.class, () -> { answerService.postAnswerMC(answerDto, 1L); });

        assertEquals(TextConstants.ID_IN_POST_ERROR, exception.getMessage());
    }

    @Test
    public void testPostAnswerMCNotMCType() {
        when(questionRepository.findByQuestionId(anyLong())).thenReturn(question);
        AnswerDto answerDto = AnswerDto.builder().answerOrder(0).build();

        Exception exception = assertThrows(DataServiceException.class, () -> { answerService.postAnswerMC(answerDto, 1L); });

        assertEquals("Error 103: Answer type not supported.", exception.getMessage());
    }

    @Test
    public void testPostAnswerMCLimitReached() {
        when(questionRepository.findByQuestionId(anyLong())).thenReturn(questionMc);
        when(answerMcRepository.findByQuestion_QuestionId(anyLong())).thenReturn(Collections.nCopies(20, answerMc));
        AnswerDto answerDto = AnswerDto.builder().answerOrder(0).build();

        assertThrows(MultipleChoiceLimitReachedException.class, () -> { answerService.postAnswerMC(answerDto, 1L); });
    }

    @Test
    public void testPostAnswerMCFromDtoFailure() {
        when(questionRepository.findByQuestionId(anyLong())).thenReturn(questionMc);
        when(questionRepository.findById(anyLong())).thenReturn(Optional.empty());
        AnswerDto answerDto = AnswerDto.builder().answerOrder(0).build();

        Exception exception = assertThrows(DataServiceException.class, () -> { answerService.postAnswerMC(answerDto, 1L); });

        assertTrue(exception.getMessage().startsWith("Error 105: Unable to create Answer:"));
    }

    @Test
    public void testPostAnswerMCSuccess() throws IdInPostException, DataServiceException, MultipleChoiceLimitReachedException {
        when(questionRepository.findByQuestionId(anyLong())).thenReturn(questionMc);
        when(questionRepository.findById(anyLong())).thenReturn(Optional.of(question));
        when(answerMcRepository.save(any(AnswerMc.class))).thenReturn(answerMc);
        AnswerDto answerDto = AnswerDto.builder().html("html").correct(true).answerOrder(0).build();

        AnswerDto result = answerService.postAnswerMC(answerDto, 1L);

        assertNotNull(result);
        assertEquals(QuestionTypes.MC.toString(), result.getAnswerType());
        verify(answerMcRepository).save(any(AnswerMc.class));
    }

    @Test
    public void testFromDtoMCSuccess() throws DataServiceException {
        when(questionRepository.findById(anyLong())).thenReturn(Optional.of(question));
        AnswerDto answerDto = AnswerDto.builder().answerId(1L).html("html").correct(true).answerOrder(2).questionId(1L).build();

        AnswerMc answerMcResult = answerService.fromDtoMC(answerDto);

        assertNotNull(answerMcResult);
        assertEquals(1L, answerMcResult.getAnswerMcId());
        assertEquals("html", answerMcResult.getHtml());
        assertEquals(question, answerMcResult.getQuestion());
    }

    @Test
    public void testFromDtoMCQuestionNotFound() {
        when(questionRepository.findById(anyLong())).thenReturn(Optional.empty());
        AnswerDto answerDto = AnswerDto.builder().questionId(1L).build();

        Exception exception = assertThrows(DataServiceException.class, () -> { answerService.fromDtoMC(answerDto); });

        assertEquals("The question for the answer does not exist", exception.getMessage());
    }

    @Test
    public void testFindByAnswerId() {
        when(answerMcRepository.findByAnswerMcId(anyLong())).thenReturn(answerMc);

        assertEquals(answerMc, answerService.findByAnswerId(1L));
    }

    @Test
    public void testUpdateAnswerMCAllFieldsSet() {
        when(answerMcRepository.save(any(AnswerMc.class))).thenReturn(answerMc);
        AnswerMc key = AnswerMc.builder().answerMcId(5L).answerOrder(1).correct(false).html("old").build();
        AnswerDto value = AnswerDto.builder().html("new html").answerOrder(9).correct(true).build();
        Map<AnswerMc, AnswerDto> map = new LinkedHashMap<>();
        map.put(key, value);

        List<AnswerDto> result = answerService.updateAnswerMC(map);

        assertEquals(1, result.size());
        assertEquals("new html", key.getHtml());
        assertEquals(9, key.getAnswerOrder());
        assertTrue(key.getCorrect());
        verify(answerMcRepository).save(key);
    }

    @Test
    public void testUpdateAnswerMCNoFieldsSet() {
        when(answerMcRepository.save(any(AnswerMc.class))).thenReturn(answerMc);
        AnswerMc key = AnswerMc.builder().answerMcId(5L).answerOrder(1).correct(false).html("old").build();
        AnswerDto value = AnswerDto.builder().build();
        Map<AnswerMc, AnswerDto> map = new LinkedHashMap<>();
        map.put(key, value);

        List<AnswerDto> result = answerService.updateAnswerMC(map);

        assertEquals(1, result.size());
        assertEquals("old", key.getHtml());
        assertEquals(1, key.getAnswerOrder());
        assertFalse(key.getCorrect());
    }

    @Test
    public void testDeleteByIdMC() {
        answerService.deleteByIdMC(1L);

        verify(answerMcRepository).deleteByAnswerMcId(1L);
    }

    @Test
    public void testLimitReachedUnderLimit() throws MultipleChoiceLimitReachedException {
        answerService.limitReached(1L);

        verify(answerMcRepository).findByQuestion_QuestionId(1L);
    }

    @Test
    public void testLimitReachedAtLimit() {
        when(answerMcRepository.findByQuestion_QuestionId(anyLong())).thenReturn(Collections.nCopies(20, answerMc));

        Exception exception = assertThrows(MultipleChoiceLimitReachedException.class, () -> { answerService.limitReached(1L); });

        assertEquals("Error 120: The multiple choice option limit of 20 options has been reached.", exception.getMessage());
    }

    @Test
    public void testGetQuestionType() {
        when(questionRepository.findByQuestionId(anyLong())).thenReturn(questionMc);

        assertEquals(QuestionTypes.MC.toString(), answerService.getQuestionType(1L));
    }

    @Test
    public void testBuildHeaders() {
        UriComponentsBuilder ucBuilder = UriComponentsBuilder.newInstance().scheme("https").host("localhost");

        HttpHeaders headers = answerService.buildHeaders(ucBuilder, 1L, 2L, 3L, 4L, 5L, 6L);

        assertNotNull(headers.getLocation());
        assertTrue(headers.getLocation().toString().contains("/api/experiments/1/conditions/2/treatments/3/assessments/4/questions/5/answers/6"));
    }

}
