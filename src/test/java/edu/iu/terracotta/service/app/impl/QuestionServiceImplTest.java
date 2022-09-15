package edu.iu.terracotta.service.app.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;

import java.util.Collections;
import java.util.List;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Question;
import edu.iu.terracotta.model.app.dto.QuestionDto;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.repository.AssessmentRepository;
import edu.iu.terracotta.repository.QuestionRepository;
import edu.iu.terracotta.service.app.FileStorageService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.StringUtils;

public class QuestionServiceImplTest {

    @InjectMocks
    private QuestionServiceImpl questionService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AllRepositories allRepositories;

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private Assessment assessment;

    @Mock
    private EntityManager entityManager;

    private Question question;
    private Question newQuestion;
    

    @BeforeEach
    public void beforeEach() throws DataServiceException, AssessmentNotMatchingException {
        MockitoAnnotations.openMocks(this);

        allRepositories.assessmentRepository = assessmentRepository;
        allRepositories.questionRepository = questionRepository;

        question = new Question();
        question.setQuestionId(1L);
        question.setAssessment(assessment);
        question.setQuestionType(QuestionTypes.ESSAY);

        newQuestion = new Question();
        newQuestion.setQuestionId(2L);
        newQuestion.setAssessment(assessment);
        newQuestion.setQuestionType(QuestionTypes.ESSAY);

        when(allRepositories.assessmentRepository.findByAssessmentId(anyLong())).thenReturn(assessment);
        when(allRepositories.questionRepository.save(any(Question.class))).thenReturn(newQuestion);
        when(fileStorageService.parseHTMLFiles(anyString())).thenReturn(StringUtils.EMPTY);
        when(assessment.getAssessmentId()).thenReturn(1L);
        when(assessment.getQuestions()).thenReturn(Collections.singletonList(question));
    }

    @Test
    public void testDuplicateQuestion() throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException {
        List<QuestionDto> questionDto = questionService.duplicateQuestionsForAssessment(1L, 2L);

        assertNotNull(questionDto);
        assertEquals(2L, questionDto.get(0).getQuestionId());
        assertNull(question.getQuestionId());
    }

    @Test
    public void testDuplicateQuestionById() throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException {
        List<QuestionDto> questionDto = questionService.duplicateQuestionsForAssessment(1L, 2L);

        assertNotNull(questionDto);
        assertEquals(2L, questionDto.get(0).getQuestionId());
        assertNull(question.getQuestionId());
    }

    @Test
    public void testDuplicateQuestionOldAssessmentNotFound() throws IdInPostException, ExceedingLimitException, AssessmentNotMatchingException {
        when(allRepositories.assessmentRepository.findByAssessmentId(1L)).thenReturn(null);

        Exception exception = assertThrows(DataServiceException.class, () -> { questionService.duplicateQuestionsForAssessment(1L, 2L); });

        assertEquals("The old assessment with the given ID does not exist", exception.getMessage());
    }

    @Test
    public void testDuplicateQuestionNewAssessmentNotFound() throws IdInPostException, ExceedingLimitException, AssessmentNotMatchingException {
        when(allRepositories.assessmentRepository.findByAssessmentId(2L)).thenReturn(null);

        Exception exception = assertThrows(DataServiceException.class, () -> { questionService.duplicateQuestionsForAssessment(1L, 2L); });

        assertEquals("The new assessment with the given ID does not exist", exception.getMessage());
    }

}