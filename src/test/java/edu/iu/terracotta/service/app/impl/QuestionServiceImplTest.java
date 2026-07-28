package edu.iu.terracotta.service.app.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.AnswerFileSubmission;
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
import edu.iu.terracotta.exceptions.InvalidQuestionTypeException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.NegativePointsException;

import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.StringUtils;

public class QuestionServiceImplTest extends BaseTest {

    @InjectMocks private QuestionServiceImpl questionService;

    @BeforeEach
    public void beforeEach() throws QuestionNotMatchingException {
        MockitoAnnotations.openMocks(this);

        setup();
        // @InjectMocks only does constructor injection here (all constructor params already match), so
        // the separate @PersistenceContext EntityManager field is never populated unless set explicitly
        org.springframework.test.util.ReflectionTestUtils.setField(questionService, "entityManager", entityManager);

        when(questionRepository.findByAssessment_AssessmentIdOrderByQuestionOrder(anyLong())).thenReturn(Collections.singletonList(question));
        when(questionRepository.save(any(Question.class))).thenReturn(questionMc);
        when(questionRepository.findByQuestionId(anyLong())).thenReturn(question);

        when(answerService.duplicateAnswersForQuestion(anyLong(), any(Question.class))).thenReturn(Collections.emptyList());
        when(fileStorageService.parseHTMLFiles(anyString(), anyString())).thenReturn(StringUtils.EMPTY);

        when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);
        when(questionDto.getQuestionType()).thenReturn(QuestionTypes.MC.toString());

        when(assessmentRepository.findById(anyLong())).thenReturn(Optional.of(assessment));
        when(submissionRepository.findByAssessment_AssessmentId(anyLong())).thenReturn(Collections.singletonList(submission));
    }

    @Test
    public void testDuplicateQuestionNewAssessmentNotFound() throws IdInPostException, ExceedingLimitException, AssessmentNotMatchingException {
        Exception exception = assertThrows(DataServiceException.class, () -> { questionService.duplicateQuestionsForAssessment(1L, null); });

        assertEquals("The new assessment with the given ID does not exist", exception.getMessage());
    }

    @Test
    public void testDuplicateQuestionsForAssessmentEmpty() throws DataServiceException, QuestionNotMatchingException {
        when(questionRepository.findByAssessment_AssessmentIdOrderByQuestionOrder(anyLong())).thenReturn(Collections.emptyList());

        List<Question> result = questionService.duplicateQuestionsForAssessment(1L, assessment);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDuplicateQuestionsForAssessmentNotIntegration() throws DataServiceException, QuestionNotMatchingException {
        when(questionMc.isIntegration()).thenReturn(false);
        when(questionRepository.save(any(Question.class))).thenReturn(questionMc);

        List<Question> result = questionService.duplicateQuestionsForAssessment(1L, assessment);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(entityManager).detach(any(Question.class));
        verify(answerService).duplicateAnswersForQuestion(anyLong(), any(Question.class));
        verify(integrationService, never()).duplicate(any(), any());
    }

    @Test
    public void testDuplicateQuestionsForAssessmentIsIntegration() throws DataServiceException, QuestionNotMatchingException {
        when(questionMc.isIntegration()).thenReturn(true);
        when(questionRepository.save(any(Question.class))).thenReturn(questionMc);

        List<Question> result = questionService.duplicateQuestionsForAssessment(1L, assessment);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(integrationService).duplicate(any(), any(Question.class));
        verify(questionRepository, times(2)).save(any(Question.class));
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

    @Test
    public void testPostQuestionIntegrationNew() throws IdInPostException, DataServiceException, MultipleChoiceLimitReachedException, IntegrationNotFoundException, IntegrationClientNotFoundException {
        when(questionDto.getQuestionType()).thenReturn(QuestionTypes.INTEGRATION.toString());
        when(questionRepository.save(any(Question.class))).thenReturn(question);
        when(question.getQuestionType()).thenReturn(QuestionTypes.INTEGRATION);
        when(integrationService.create(any(Question.class), any())).thenReturn(integration);

        QuestionDto retDto = questionService.postQuestion(questionDto, 1l, false, true);

        assertNotNull(retDto);
        verify(integrationService).create(any(Question.class), any());
        verify(integrationService, never()).duplicate(any(), any());
    }

    @Test
    public void testPostQuestionIntegrationExisting() throws IdInPostException, DataServiceException, MultipleChoiceLimitReachedException, IntegrationNotFoundException, IntegrationClientNotFoundException {
        when(questionDto.getQuestionType()).thenReturn(QuestionTypes.INTEGRATION.toString());
        when(questionRepository.save(any(Question.class))).thenReturn(question);
        when(question.getQuestionType()).thenReturn(QuestionTypes.INTEGRATION);
        when(questionDto.getIntegration()).thenReturn(integrationDto);
        when(integrationDto.getId()).thenReturn(UUID.randomUUID());
        when(integrationService.findByUuid(any(UUID.class))).thenReturn(integration);

        QuestionDto retDto = questionService.postQuestion(questionDto, 1l, false, false);

        assertNotNull(retDto);
        verify(integrationService, never()).create(any(), any());
        verify(integrationService).duplicate(any(), any(Question.class));
    }

    @Test
    public void testPostQuestionIdInPost() {
        when(questionDto.getQuestionId()).thenReturn(1L);

        assertThrows(IdInPostException.class, () -> questionService.postQuestion(questionDto, 1l, false, true));
    }

    @Test
    public void testPostQuestionInvalidType() {
        when(questionDto.getQuestionType()).thenReturn("BOGUS_TYPE");

        Exception exception = assertThrows(DataServiceException.class, () -> questionService.postQuestion(questionDto, 1l, false, true));

        assertTrue(exception.getMessage().startsWith("Error 105"));
    }

    @Test
    public void testPostQuestionNegativePoints() {
        when(questionDto.getQuestionType()).thenReturn(QuestionTypes.ESSAY.toString());
        when(questionDto.getPoints()).thenReturn(-1F);

        Exception exception = assertThrows(DataServiceException.class, () -> questionService.postQuestion(questionDto, 1l, false, true));

        assertTrue(exception.getMessage().startsWith("Error 105"));
    }

    @Test
    public void testPostQuestionAssessmentNotFound() {
        when(questionDto.getQuestionType()).thenReturn(QuestionTypes.ESSAY.toString());
        when(assessmentRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(DataServiceException.class, () -> questionService.postQuestion(questionDto, 1l, false, true));

        assertTrue(exception.getMessage().startsWith("Error 105"));
    }

    @Test
    public void testGetQuestions() {
        List<QuestionDto> result = questionService.getQuestions(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testGetQuestionsNoQuestions() {
        when(questionRepository.findByAssessment_AssessmentIdOrderByQuestionOrder(anyLong())).thenReturn(null);

        List<QuestionDto> result = questionService.getQuestions(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetQuestion() {
        Question result = questionService.getQuestion(1L);

        assertNotNull(result);
        verify(questionRepository).findByQuestionId(1L);
    }

    @Test
    public void testFindByQuestionId() {
        Question result = questionService.findByQuestionId(1L);

        assertNotNull(result);
        verify(questionRepository).findByQuestionId(1L);
    }

    @Test
    public void testSave() {
        Question result = questionService.save(question);

        assertNotNull(result);
        verify(questionRepository).save(question);
    }

    @Test
    public void testToDtoListOverload() {
        List<QuestionDto> result = questionService.toDto(Collections.singletonList(question), false, false);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testToDtoTwoArgNonMc() {
        QuestionDto result = questionService.toDto(question, false, false);

        assertNotNull(result);
        assertEquals(QuestionTypes.ESSAY.name(), result.getQuestionType());
    }

    @Test
    public void testToDtoFourArgMcNoAnswers() {
        QuestionDto result = questionService.toDto(questionMc, null, false, false);

        assertNotNull(result);
        assertTrue(result.isRandomizeAnswers());
        assertNull(result.getAnswers());
    }

    @Test
    public void testToDtoFourArgMcAnswersNoSubmissionId() {
        when(answerService.findAllByQuestionIdMC(anyLong(), Mockito.eq(true))).thenReturn(Collections.singletonList(answerDto));

        QuestionDto result = questionService.toDto(questionMc, null, true, true);

        assertNotNull(result);
        assertEquals(1, result.getAnswers().size());
        verify(answerService).findAllByQuestionIdMC(anyLong(), Mockito.eq(true));
        verify(answerService, never()).findAllByQuestionIdMC(any(edu.iu.terracotta.dao.entity.QuestionSubmission.class), Mockito.anyBoolean());
    }

    @Test
    public void testToDtoFourArgMcAnswersSubmissionPresent() {
        when(questionSubmissionRepository.findByQuestion_QuestionIdAndSubmission_SubmissionId(anyLong(), anyLong())).thenReturn(Optional.of(questionSubmission));
        when(answerService.findAllByQuestionIdMC(any(edu.iu.terracotta.dao.entity.QuestionSubmission.class), Mockito.eq(true))).thenReturn(Collections.singletonList(answerDto));

        QuestionDto result = questionService.toDto(questionMc, 5L, true, true);

        assertNotNull(result);
        assertEquals(1, result.getAnswers().size());
        verify(answerService).findAllByQuestionIdMC(any(edu.iu.terracotta.dao.entity.QuestionSubmission.class), Mockito.eq(true));
    }

    @Test
    public void testToDtoFourArgMcAnswersSubmissionAbsent() {
        when(questionSubmissionRepository.findByQuestion_QuestionIdAndSubmission_SubmissionId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(answerService.findAllByQuestionIdMC(anyLong(), Mockito.eq(true))).thenReturn(Collections.singletonList(answerDto));

        QuestionDto result = questionService.toDto(questionMc, 5L, true, true);

        assertNotNull(result);
        assertEquals(1, result.getAnswers().size());
        verify(answerService).findAllByQuestionIdMC(anyLong(), Mockito.eq(true));
    }

    @Test
    public void testFromDtoMc() throws DataServiceException, NegativePointsException {
        when(questionDto.getQuestionType()).thenReturn(QuestionTypes.MC.toString());

        Question result = questionService.fromDto(questionDto);

        assertNotNull(result);
        assertTrue(result instanceof edu.iu.terracotta.dao.entity.QuestionMc);
    }

    @Test
    public void testFromDtoNonMc() throws DataServiceException, NegativePointsException {
        when(questionDto.getQuestionType()).thenReturn(QuestionTypes.ESSAY.toString());

        Question result = questionService.fromDto(questionDto);

        assertNotNull(result);
        assertFalse(result instanceof edu.iu.terracotta.dao.entity.QuestionMc);
    }

    @Test
    public void testFromDtoNegativePoints() {
        when(questionDto.getQuestionType()).thenReturn(QuestionTypes.ESSAY.toString());
        when(questionDto.getPoints()).thenReturn(-5F);

        assertThrows(NegativePointsException.class, () -> questionService.fromDto(questionDto));
    }

    @Test
    public void testFromDtoAssessmentNotFound() {
        when(questionDto.getQuestionType()).thenReturn(QuestionTypes.ESSAY.toString());
        when(assessmentRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(DataServiceException.class, () -> questionService.fromDto(questionDto));

        assertEquals("The assessment for the question does not exist", exception.getMessage());
    }

    @Test
    public void testUpdateQuestionMc() throws NegativePointsException, IntegrationNotFoundException, edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotMatchingException,
            edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotFoundException, edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotMatchingException,
            IntegrationClientNotFoundException {
        when(questionMc.getQuestionType()).thenReturn(QuestionTypes.MC);
        Map<Question, QuestionDto> map = new HashMap<>();
        map.put(questionMc, questionDto);

        questionService.updateQuestion(map);

        verify(questionMc).setRandomizeAnswers(Mockito.anyBoolean());
        verify(questionRepository).save(questionMc);
    }

    @Test
    public void testUpdateQuestionIntegration() throws NegativePointsException, IntegrationNotFoundException, edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotMatchingException,
            edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotFoundException, edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotMatchingException,
            IntegrationClientNotFoundException {
        when(question.getQuestionType()).thenReturn(QuestionTypes.INTEGRATION);
        when(integrationService.update(any(), any(Question.class))).thenReturn(integration);
        Map<Question, QuestionDto> map = new HashMap<>();
        map.put(question, questionDto);

        questionService.updateQuestion(map);

        verify(integrationService).update(any(), any(Question.class));
        verify(question).setIntegration(integration);
        verify(questionRepository).save(question);
    }

    @Test
    public void testUpdateQuestionDefault() throws NegativePointsException, IntegrationNotFoundException, edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotMatchingException,
            edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotFoundException, edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotMatchingException,
            IntegrationClientNotFoundException {
        when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);
        Map<Question, QuestionDto> map = new HashMap<>();
        map.put(question, questionDto);

        questionService.updateQuestion(map);

        verify(questionRepository).save(question);
        verify(integrationService, never()).update(any(), any());
    }

    @Test
    public void testUpdateQuestionNegativePoints() {
        when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);
        when(questionDto.getPoints()).thenReturn(-2F);
        Map<Question, QuestionDto> map = new HashMap<>();
        map.put(question, questionDto);

        assertThrows(NegativePointsException.class, () -> questionService.updateQuestion(map));
    }

    @Test
    public void testDeleteByIdWithIntegrationAndFileSubmission() {
        AnswerFileSubmission answerFileSubmission = Mockito.mock(AnswerFileSubmission.class);
        when(question.getIntegration()).thenReturn(integration);
        when(questionSubmission.getQuestion()).thenReturn(question);
        when(question.getQuestionType()).thenReturn(QuestionTypes.FILE);
        when(answerFileSubmissionRepository.findByQuestionSubmission_QuestionSubmissionId(anyLong())).thenReturn(Collections.singletonList(answerFileSubmission));

        questionService.deleteById(1L);

        verify(integrationService).delete(integration);
        verify(fileStorageService).deleteFileSubmission(answerFileSubmission);
        verify(answerFileSubmissionRepository).delete(answerFileSubmission);
        verify(answerFileSubmissionRepository).flush();
        verify(submissionRepository).delete(submission);
        verify(submissionRepository).flush();
        verify(questionRepository).deleteByQuestionId(1L);
    }

    @Test
    public void testDeleteByIdNoIntegrationNoFileSubmission() {
        when(question.getIntegration()).thenReturn(null);
        when(questionSubmission.getQuestion()).thenReturn(question);
        when(question.getQuestionType()).thenReturn(QuestionTypes.ESSAY);

        questionService.deleteById(1L);

        verify(integrationService, never()).delete(any());
        verify(answerFileSubmissionRepository, never()).delete(any());
        verify(submissionRepository).delete(submission);
        verify(questionRepository).deleteByQuestionId(1L);
    }

    @Test
    public void testBuildHeaders() {
        HttpHeaders headers = questionService.buildHeaders(UriComponentsBuilder.newInstance(), 1L, 2L, 3L, 4L, 5L);

        assertNotNull(headers);
        assertNotNull(headers.getLocation());
        assertTrue(headers.getLocation().toString().contains("/api/experiments/1/conditions/2/treatments/3/assessments/4/questions/5"));
    }

    @Test
    public void testValidateQuestionTypeNull() {
        when(questionDto.getQuestionType()).thenReturn(null);

        Exception exception = assertThrows(InvalidQuestionTypeException.class, () -> questionService.validateQuestionType(questionDto));

        assertEquals("Error 119: Must include a question type in the post.", exception.getMessage());
    }

    @Test
    public void testValidateQuestionTypeInvalid() {
        when(questionDto.getQuestionType()).thenReturn("NOT_A_TYPE");

        Exception exception = assertThrows(InvalidQuestionTypeException.class, () -> questionService.validateQuestionType(questionDto));

        assertEquals("Error 103: Please use a supported question type.", exception.getMessage());
    }

    @Test
    public void testValidateQuestionTypeValid() {
        when(questionDto.getQuestionType()).thenReturn(QuestionTypes.MC.toString());

        assertDoesNotThrow(() -> questionService.validateQuestionType(questionDto));
    }

}
