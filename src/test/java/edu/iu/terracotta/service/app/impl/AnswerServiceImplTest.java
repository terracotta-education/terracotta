package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.dto.AnswerDto;
import edu.iu.terracotta.service.app.FileStorageService;

public class AnswerServiceImplTest {

    @InjectMocks
    private AnswerServiceImpl answerService;

    @Mock private FileStorageService fileStorageService;

    @Mock private AnswerMc answerMc;
    @Mock private Question question;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        when(fileStorageService.parseHTMLFiles(anyString())).thenReturn("html");

        when(answerMc.getQuestion()).thenReturn(question);
        when(question.getQuestionId()).thenReturn(1L);
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

}
