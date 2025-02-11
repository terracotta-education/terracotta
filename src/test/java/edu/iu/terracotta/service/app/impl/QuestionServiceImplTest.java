package edu.iu.terracotta.service.app.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import java.util.Collections;
import java.util.List;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.Question;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotFoundException;
import edu.iu.terracotta.dao.model.dto.AnswerDto;
import edu.iu.terracotta.dao.model.dto.QuestionDto;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.StringUtils;

public class QuestionServiceImplTest extends BaseTest {

    @InjectMocks private QuestionServiceImpl questionService;

    @BeforeEach
    public void beforeEach() throws QuestionNotMatchingException {
        MockitoAnnotations.openMocks(this);

        setup();

        when(questionRepository.findByAssessment_AssessmentIdOrderByQuestionOrder(anyLong())).thenReturn(Collections.singletonList(question));
        when(questionRepository.save(any(Question.class))).thenReturn(questionMc);

        when(answerService.duplicateAnswersForQuestion(anyLong(), any(Question.class))).thenReturn(Collections.emptyList());
        when(fileStorageService.parseHTMLFiles(anyString(), anyString())).thenReturn(StringUtils.EMPTY);

        when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);
        when(questionDto.getQuestionType()).thenReturn(QuestionTypes.MC.toString());
    }

    @Test
    public void testDuplicateQuestion() throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, QuestionNotMatchingException {
        List<Question> question = questionService.duplicateQuestionsForAssessment(1L, assessment);

        assertNotNull(question);
        assertEquals(1L, question.get(0).getQuestionId());
    }

    @Test
    public void testDuplicateQuestionNewAssessmentNotFound() throws IdInPostException, ExceedingLimitException, AssessmentNotMatchingException {
        Exception exception = assertThrows(DataServiceException.class, () -> { questionService.duplicateQuestionsForAssessment(1L, null); });

        assertEquals("The new assessment with the given ID does not exist", exception.getMessage());
    }

    @Test
    public void testPostQuestionMC() throws IdInPostException, DataServiceException, MultipleChoiceLimitReachedException, IntegrationNotFoundException, IntegrationClientNotFoundException {
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);
        QuestionDto retDto = questionService.postQuestion(questionDto, 1l, false, true);

        assertNotNull(retDto);
        verify(questionRepository).save(any(Question.class));
        verify(answerService).postAnswerMC(any(AnswerDto.class), anyLong());
    }

    @Test
    public void testPostQuestionEssay() throws IdInPostException, DataServiceException, MultipleChoiceLimitReachedException, IntegrationNotFoundException, IntegrationClientNotFoundException {
        when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);
        when(questionDto.getQuestionType()).thenReturn(QuestionTypes.ESSAY.toString());
        when(questionRepository.save(any(Question.class))).thenReturn(question);
        QuestionDto retDto = questionService.postQuestion(questionDto, 1l, false, true);

        assertNotNull(retDto);
        verify(questionRepository).save(any(Question.class));
        verify(answerService, never()).postAnswerMC(any(AnswerDto.class), anyLong());
    }

    @Test
    public void testPostQuestionMCNoAnswers() throws IdInPostException, DataServiceException, MultipleChoiceLimitReachedException, IntegrationNotFoundException, IntegrationClientNotFoundException {
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);
        when(questionDto.getAnswers()).thenReturn(null);
        QuestionDto retDto = questionService.postQuestion(questionDto, 1l, false, true);

        assertNotNull(retDto);
        verify(questionRepository).save(any(Question.class));
        verify(answerService, never()).postAnswerMC(any(AnswerDto.class), anyLong());
    }

}
