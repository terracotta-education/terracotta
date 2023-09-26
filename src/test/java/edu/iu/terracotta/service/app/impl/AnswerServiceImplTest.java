package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.BaseTest;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.dto.AnswerDto;
import edu.iu.terracotta.utils.TextConstants;

public class AnswerServiceImplTest extends BaseTest {

    @InjectMocks private AnswerServiceImpl answerService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
        clearInvocations(answerMcRepository);

        when(answerMcRepository.findByQuestion_QuestionId(anyLong())).thenReturn(Collections.singletonList(answerMc));

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
    public void testDuplicateAnswersForQuestion() throws QuestionNotMatchingException {
        List<AnswerMc> retList = answerService.duplicateAnswersForQuestion(1L, questionMc);

        assertEquals(1, retList.size());
        verify(answerMcRepository).save(any(AnswerMc.class));
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

}
