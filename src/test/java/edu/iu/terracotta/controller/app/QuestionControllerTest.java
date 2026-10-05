package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

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
import edu.iu.terracotta.dao.entity.Question;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.model.dto.QuestionDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.utils.TextConstants;

public class QuestionControllerTest extends BaseTest {

    // the uuid path variable for the one experiment under test; experiment.getExperimentId()
    // (the mock's globally-stubbed return value, see BaseModelTest) is what it resolves to
    private static final UUID EXPERIMENT_UUID = UUID.randomUUID();

    // the uuid path variable for the one condition under test; condition.getConditionId()
    // (the mock's globally-stubbed return value, see BaseModelTest) is what it resolves to
    private static final UUID CONDITION_UUID = UUID.randomUUID();

    // the uuid path variable for the one treatment under test; treatment.getTreatmentId()
    // (the mock's globally-stubbed return value, see BaseModelTest) is what it resolves to
    private static final UUID TREATMENT_UUID = UUID.randomUUID();

    // the uuid path variable for the one assessment under test; assessment.getAssessmentId()
    // (the mock's globally-stubbed return value, see BaseModelTest) is what it resolves to
    private static final UUID ASSESSMENT_UUID = UUID.randomUUID();

    // the uuid path variable for the one question under test; question.getQuestionId() (the
    // mock's globally-stubbed return value, see BaseModelTest) is what it resolves to
    private static final UUID QUESTION_UUID = UUID.randomUUID();

    // ConditionService has no mock in the BaseTest hierarchy, so it must be declared locally.
    @Mock private ConditionService conditionService;

    private QuestionController questionController;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        // Constructed manually rather than via @InjectMocks: ApiJwtService is also implemented by the
        // inherited canvasApiJwtService mock (see the ambiguity warning in BaseServiceTest), so
        // constructor-injection-by-type could silently wire the wrong ApiJwtService mock.
        questionController = new QuestionController(questionService, apiJwtService, experimentService, conditionService, treatmentService, assessmentService);

        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(experimentService.getExperimentIdByUuid(EXPERIMENT_UUID)).thenAnswer(invocation -> experiment.getExperimentId());
        when(conditionService.getConditionIdByUuid(CONDITION_UUID)).thenAnswer(invocation -> condition.getConditionId());
        when(treatmentService.getTreatmentIdByUuid(TREATMENT_UUID)).thenAnswer(invocation -> treatment.getTreatmentId());
        when(assessmentService.getAssessmentIdByUuid(ASSESSMENT_UUID)).thenAnswer(invocation -> assessment.getAssessmentId());
        when(questionService.getQuestionIdByUuid(QUESTION_UUID)).thenAnswer(invocation -> question.getQuestionId());
    }

    @Test
    void getQuestionsByAssessmentTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(questionService.getQuestions(anyLong())).thenReturn(List.of(questionDto));

        ResponseEntity<List<QuestionDto>> response = questionController.getQuestionsByAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getQuestionsByAssessmentNoContentTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(questionService.getQuestions(anyLong())).thenReturn(List.of());

        ResponseEntity<List<QuestionDto>> response = questionController.getQuestionsByAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getQuestionsByAssessmentUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<QuestionDto>> response = questionController.getQuestionsByAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void getQuestionsByAssessmentThrowsTest() throws Exception {
        doThrow(new ExperimentNotMatchingException("error")).when(apiJwtService).experimentAllowed(any(SecuredInfo.class), anyLong());

        assertThrows(ExperimentNotMatchingException.class, () -> questionController.getQuestionsByAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, httpServletRequest));
    }

    @Test
    void getQuestionTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(questionService.toDto(any(Question.class), anyBoolean(), anyBoolean())).thenReturn(questionDto);

        ResponseEntity<QuestionDto> response = questionController.getQuestion(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, QUESTION_UUID, false, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(questionDto, response.getBody());
    }

    @Test
    void getQuestionUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<QuestionDto> response = questionController.getQuestion(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, QUESTION_UUID, false, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void getQuestionThrowsTest() throws Exception {
        doThrow(new QuestionNotMatchingException("error")).when(apiJwtService).questionAllowed(any(SecuredInfo.class), anyLong(), anyLong());

        assertThrows(QuestionNotMatchingException.class, () -> questionController.getQuestion(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, QUESTION_UUID, false, httpServletRequest));
    }

    @Test
    void postQuestionTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(questionDto.getQuestionId()).thenReturn(QUESTION_UUID);
        when(questionService.postQuestion(any(QuestionDto.class), anyLong(), anyBoolean(), anyBoolean())).thenReturn(questionDto);
        when(questionService.buildHeaders(any(UriComponentsBuilder.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class))).thenReturn(new HttpHeaders());

        ResponseEntity<QuestionDto> response = questionController.postQuestion(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, false, questionDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(questionDto, response.getBody());
    }

    @Test
    void postQuestionUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<QuestionDto> response = questionController.postQuestion(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, false, questionDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void postQuestionThrowsTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(questionService.postQuestion(any(QuestionDto.class), anyLong(), anyBoolean(), anyBoolean())).thenThrow(new MultipleChoiceLimitReachedException("error"));

        assertThrows(MultipleChoiceLimitReachedException.class, () -> questionController.postQuestion(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, false, questionDto, UriComponentsBuilder.newInstance(), httpServletRequest));
    }

    @Test
    void updateQuestionsTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(questionDto.getQuestionId()).thenReturn(QUESTION_UUID);

        ResponseEntity<Void> response = questionController.updateQuestions(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, List.of(questionDto), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateQuestionsUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = questionController.updateQuestions(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, List.of(questionDto), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void updateQuestionsThrowsTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(questionDto.getQuestionId()).thenReturn(QUESTION_UUID);
        doThrow(new RuntimeException("boom")).when(questionService).updateQuestion(anyMap());

        assertThrows(DataServiceException.class, () -> questionController.updateQuestions(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, List.of(questionDto), httpServletRequest));
    }

    @Test
    void updateQuestionTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);

        ResponseEntity<Void> response = questionController.updateQuestion(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, QUESTION_UUID, questionDto, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateQuestionUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = questionController.updateQuestion(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, QUESTION_UUID, questionDto, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void updateQuestionThrowsTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        doThrow(new NegativePointsException("error")).when(questionService).updateQuestion(anyMap());

        assertThrows(NegativePointsException.class, () -> questionController.updateQuestion(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, QUESTION_UUID, questionDto, httpServletRequest));
    }

    @Test
    void deleteQuestionTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);

        ResponseEntity<Void> response = questionController.deleteQuestion(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, QUESTION_UUID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteQuestionUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = questionController.deleteQuestion(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, QUESTION_UUID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void deleteQuestionNotFoundTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        doThrow(new EmptyResultDataAccessException(1)).when(questionService).deleteById(anyLong());

        ResponseEntity<Void> response = questionController.deleteQuestion(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, QUESTION_UUID, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteQuestionsTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(questionDto.getQuestionId()).thenReturn(QUESTION_UUID);

        ResponseEntity<Void> response = questionController.deleteQuestions(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, List.of(questionDto), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteQuestionsUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = questionController.deleteQuestions(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, List.of(questionDto), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void deleteQuestionsThrowsTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(questionDto.getQuestionId()).thenReturn(QUESTION_UUID);
        doThrow(new QuestionNotMatchingException("error")).when(apiJwtService).questionAllowed(any(SecuredInfo.class), anyLong(), anyLong());

        assertThrows(QuestionNotMatchingException.class, () -> questionController.deleteQuestions(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, List.of(questionDto), httpServletRequest));
    }

    // NOTE: unlike deleteQuestion() (singular) which catches EmptyResultDataAccessException per-item,
    // deleteQuestions() (plural) has no such guard - see final report.
    @Test
    void deleteQuestionsPropagatesEmptyResultTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(questionDto.getQuestionId()).thenReturn(QUESTION_UUID);
        doThrow(new EmptyResultDataAccessException(1)).when(questionService).deleteById(anyLong());

        assertThrows(EmptyResultDataAccessException.class, () -> questionController.deleteQuestions(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, List.of(questionDto), httpServletRequest));
    }

}
