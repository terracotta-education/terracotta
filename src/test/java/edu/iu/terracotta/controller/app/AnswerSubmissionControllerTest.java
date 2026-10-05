package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.exceptions.AnswerNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AnswerSubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionSubmissionNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AnswerSubmissionDto;
import edu.iu.terracotta.dao.model.dto.FileResponseDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.TypeNotSupportedException;
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.utils.TextConstants;
import tools.jackson.core.JacksonException;

public class AnswerSubmissionControllerTest extends BaseTest {

    private static final UUID EXPERIMENT_UUID = UUID.randomUUID();
    private static final long EXPERIMENT_ID = 1L;
    // matches condition.getConditionId() (the mock's globally-stubbed return value, see BaseModelTest),
    // which is what conditionService.getConditionIdByUuid(CONDITION_UUID) below resolves to
    private static final long CONDITION_ID = 1L;
    private static final UUID CONDITION_UUID = UUID.randomUUID();
    private static final long TREATMENT_ID = 3L;

    // the uuid path variable for the one treatment under test; overrides the shared treatment
    // mock's getTreatmentId() (see BaseModelTest) to resolve to TREATMENT_ID above, matching what
    // this test's apijwtService/assessmentService stubs already expect
    private static final UUID TREATMENT_UUID = UUID.randomUUID();
    private static final long ASSESSMENT_ID = 4L;

    // the uuid path variable for the one assessment under test; overrides the shared assessment
    // mock's getAssessmentId() (see BaseModelTest) to resolve to ASSESSMENT_ID above, matching
    // what this test's apijwtService stubs already expect
    private static final UUID ASSESSMENT_UUID = UUID.randomUUID();
    private static final long SUBMISSION_ID = 5L;
    private static final long QUESTION_SUBMISSION_ID = 6L;
    private static final long QUESTION_SUBMISSION_ID_2 = 8L;
    private static final long ANSWER_SUBMISSION_ID = 7L;

    // the uuid path variable for the one answer submission under test; resolves (via
    // answerSubmissionService.resolveAnswerSubmissionId, stubbed below) to ANSWER_SUBMISSION_ID above
    private static final UUID ANSWER_SUBMISSION_UUID = UUID.randomUUID();
    private static final String ANSWER_TYPE = "MC";

    // the uuid path variable for the one submission under test; overrides the shared submission
    // mock's getSubmissionId() (see BaseModelTest) to resolve to SUBMISSION_ID above
    private static final UUID SUBMISSION_UUID = UUID.randomUUID();

    // the uuid FK/path variable for the one question submission under test; overrides the shared
    // questionSubmission mock's getQuestionSubmissionId() (see BaseModelTest) to resolve to
    // QUESTION_SUBMISSION_ID above
    private static final UUID QUESTION_SUBMISSION_UUID = UUID.randomUUID();

    // a second, distinct question submission uuid used only by the postAnswerSubmissions bulk
    // tests; resolves (via a locally-stubbed mock) to QUESTION_SUBMISSION_ID_2 above
    private static final UUID QUESTION_SUBMISSION_UUID_2 = UUID.randomUUID();

    // ConditionService has no mock in the BaseTest hierarchy, so it must be declared locally.
    @Mock private ConditionService conditionService;

    private AnswerSubmissionController answerSubmissionController;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        // apiJwtService below also collides with CanvasApiJwtServiceImpl in BaseServiceTest (see the
        // @InjectMocks pitfall note there), so this class is constructed manually instead of relying
        // on @InjectMocks, which non-deterministically wired the wrong mock and left apiJwtService
        // calls silently unstubbed.
        answerSubmissionController = new AnswerSubmissionController(answerSubmissionService, submissionService, questionSubmissionService, apiJwtService, experimentService, conditionService, treatmentService, assessmentService);

        when(apiJwtService.extractValues(any(), eq(false))).thenReturn(securedInfo);
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(answerSubmissionService.getAnswerType(anyLong())).thenReturn(ANSWER_TYPE);
        when(answerSubmissionService.resolveAnswerSubmissionId(ANSWER_SUBMISSION_UUID, ANSWER_TYPE)).thenReturn(ANSWER_SUBMISSION_ID);
        when(experimentService.getExperimentIdByUuid(EXPERIMENT_UUID)).thenAnswer(invocation -> experiment.getExperimentId());
        when(conditionService.getConditionIdByUuid(CONDITION_UUID)).thenAnswer(invocation -> condition.getConditionId());
        when(treatment.getTreatmentId()).thenReturn(TREATMENT_ID);
        when(treatmentService.getTreatmentIdByUuid(TREATMENT_UUID)).thenAnswer(invocation -> treatment.getTreatmentId());
        when(assessment.getAssessmentId()).thenReturn(ASSESSMENT_ID);
        when(assessmentService.getAssessmentIdByUuid(ASSESSMENT_UUID)).thenAnswer(invocation -> assessment.getAssessmentId());
        when(submission.getSubmissionId()).thenReturn(SUBMISSION_ID);
        when(submissionService.getSubmissionIdByUuid(SUBMISSION_UUID)).thenAnswer(invocation -> submission.getSubmissionId());
        when(questionSubmission.getQuestionSubmissionId()).thenReturn(QUESTION_SUBMISSION_ID);
        when(questionSubmissionService.getQuestionSubmissionIdByUuid(QUESTION_SUBMISSION_UUID)).thenAnswer(invocation -> questionSubmission.getQuestionSubmissionId());
    }

    private String dtoJson(UUID questionSubmissionId) {
        return "{\"questionSubmissionId\":\"" + questionSubmissionId + "\"}";
    }

    // getAnswerSubmissionsByQuestionId

    @Test
    void testGetAnswerSubmissionsByQuestionIdSuccess() throws Exception {
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().answerSubmissionId(ANSWER_SUBMISSION_UUID).build();
        when(answerSubmissionService.getAnswerSubmissions(QUESTION_SUBMISSION_ID, ANSWER_TYPE)).thenReturn(List.of(dto));

        ResponseEntity<List<AnswerSubmissionDto>> response = answerSubmissionController.getAnswerSubmissionsByQuestionId(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(dto, response.getBody().get(0));
    }

    @Test
    void testGetAnswerSubmissionsByQuestionIdNoContent() throws Exception {
        when(answerSubmissionService.getAnswerSubmissions(QUESTION_SUBMISSION_ID, ANSWER_TYPE)).thenReturn(List.of());

        ResponseEntity<List<AnswerSubmissionDto>> response = answerSubmissionController.getAnswerSubmissionsByQuestionId(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void testGetAnswerSubmissionsByQuestionIdUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<AnswerSubmissionDto>> response = answerSubmissionController.getAnswerSubmissionsByQuestionId(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(answerSubmissionService, never()).getAnswerSubmissions(anyLong(), anyString());
    }

    @Test
    void testGetAnswerSubmissionsByQuestionIdAsLearnerValidatesUser() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        when(answerSubmissionService.getAnswerSubmissions(QUESTION_SUBMISSION_ID, ANSWER_TYPE)).thenReturn(List.of());

        answerSubmissionController.getAnswerSubmissionsByQuestionId(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, httpServletRequest);

        verify(submissionService, times(1)).validateUser(EXPERIMENT_ID, USER_ID, SUBMISSION_ID);
    }

    @Test
    void testGetAnswerSubmissionsByQuestionIdInvalidUser() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        doThrow(new InvalidUserException("not valid")).when(submissionService).validateUser(EXPERIMENT_ID, USER_ID, SUBMISSION_ID);

        assertThrows(InvalidUserException.class, () -> answerSubmissionController.getAnswerSubmissionsByQuestionId(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, httpServletRequest));
    }

    @Test
    void testGetAnswerSubmissionsByQuestionIdPropagatesQuestionSubmissionNotMatching() throws Exception {
        doThrow(new QuestionSubmissionNotMatchingException("not matching")).when(apiJwtService)
            .questionSubmissionAllowed(securedInfo, ASSESSMENT_ID, SUBMISSION_ID, QUESTION_SUBMISSION_ID);

        assertThrows(QuestionSubmissionNotMatchingException.class, () -> answerSubmissionController.getAnswerSubmissionsByQuestionId(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, httpServletRequest));
    }

    // getAnswerSubmission

    @Test
    void testGetAnswerSubmissionSuccess() throws Exception {
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().answerSubmissionId(ANSWER_SUBMISSION_UUID).build();
        when(answerSubmissionService.getAnswerSubmission(ANSWER_SUBMISSION_ID, ANSWER_TYPE)).thenReturn(dto);

        ResponseEntity<AnswerSubmissionDto> response = answerSubmissionController.getAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void testGetAnswerSubmissionUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<AnswerSubmissionDto> response = answerSubmissionController.getAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void testGetAnswerSubmissionAsLearnerValidatesUser() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        answerSubmissionController.getAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, httpServletRequest);

        verify(submissionService, times(1)).validateUser(EXPERIMENT_ID, USER_ID, SUBMISSION_ID);
    }

    @Test
    void testGetAnswerSubmissionPropagatesAnswerSubmissionNotMatching() throws Exception {
        doThrow(new AnswerSubmissionNotMatchingException("not matching")).when(apiJwtService)
            .answerSubmissionAllowed(securedInfo, QUESTION_SUBMISSION_ID, ANSWER_TYPE, ANSWER_SUBMISSION_ID);

        assertThrows(AnswerSubmissionNotMatchingException.class, () -> answerSubmissionController.getAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, httpServletRequest));
    }

    @Test
    void testGetAnswerSubmissionPropagatesBadToken() throws Exception {
        doThrow(new BadTokenException("bad token")).when(apiJwtService).experimentAllowed(securedInfo, EXPERIMENT_ID);

        assertThrows(BadTokenException.class, () -> answerSubmissionController.getAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, httpServletRequest));
    }

    // postAnswerSubmissions

    @Test
    void testPostAnswerSubmissionsSuccess() throws Exception {
        edu.iu.terracotta.dao.entity.QuestionSubmission questionSubmission2 = org.mockito.Mockito.mock(edu.iu.terracotta.dao.entity.QuestionSubmission.class);
        when(questionSubmission2.getQuestionSubmissionId()).thenReturn(QUESTION_SUBMISSION_ID_2);
        when(questionSubmissionService.getQuestionSubmissionIdByUuid(QUESTION_SUBMISSION_UUID_2)).thenAnswer(invocation -> questionSubmission2.getQuestionSubmissionId());
        AnswerSubmissionDto dto1 = AnswerSubmissionDto.builder().questionSubmissionId(QUESTION_SUBMISSION_UUID).build();
        AnswerSubmissionDto dto2 = AnswerSubmissionDto.builder().questionSubmissionId(QUESTION_SUBMISSION_UUID_2).build();
        List<AnswerSubmissionDto> requestList = List.of(dto1, dto2);
        List<AnswerSubmissionDto> returnedList = List.of(dto1, dto2);
        when(answerSubmissionService.postAnswerSubmissions(requestList)).thenReturn(returnedList);

        ResponseEntity<List<AnswerSubmissionDto>> response = answerSubmissionController.postAnswerSubmissions(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, requestList, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(returnedList, response.getBody());
        verify(apiJwtService, times(1)).questionSubmissionAllowed(securedInfo, ASSESSMENT_ID, SUBMISSION_ID, QUESTION_SUBMISSION_ID);
        verify(apiJwtService, times(1)).questionSubmissionAllowed(securedInfo, ASSESSMENT_ID, SUBMISSION_ID, QUESTION_SUBMISSION_ID_2);
    }

    @Test
    void testPostAnswerSubmissionsUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);
        List<AnswerSubmissionDto> requestList = List.of(AnswerSubmissionDto.builder().questionSubmissionId(QUESTION_SUBMISSION_UUID).build());

        ResponseEntity<List<AnswerSubmissionDto>> response = answerSubmissionController.postAnswerSubmissions(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, requestList, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
        verify(answerSubmissionService, never()).postAnswerSubmissions(any());
    }

    @Test
    void testPostAnswerSubmissionsAsLearnerValidatesUser() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        List<AnswerSubmissionDto> requestList = List.of(AnswerSubmissionDto.builder().questionSubmissionId(QUESTION_SUBMISSION_UUID).build());
        when(answerSubmissionService.postAnswerSubmissions(requestList)).thenReturn(requestList);

        answerSubmissionController.postAnswerSubmissions(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, requestList, httpServletRequest);

        verify(submissionService, times(1)).validateUser(EXPERIMENT_ID, USER_ID, SUBMISSION_ID);
    }

    @Test
    void testPostAnswerSubmissionsPropagatesQuestionSubmissionNotMatchingOnSecondItem() throws Exception {
        edu.iu.terracotta.dao.entity.QuestionSubmission questionSubmission2 = org.mockito.Mockito.mock(edu.iu.terracotta.dao.entity.QuestionSubmission.class);
        when(questionSubmission2.getQuestionSubmissionId()).thenReturn(QUESTION_SUBMISSION_ID_2);
        when(questionSubmissionService.getQuestionSubmissionIdByUuid(QUESTION_SUBMISSION_UUID_2)).thenAnswer(invocation -> questionSubmission2.getQuestionSubmissionId());
        doThrow(new QuestionSubmissionNotMatchingException("not matching")).when(apiJwtService)
            .questionSubmissionAllowed(securedInfo, ASSESSMENT_ID, SUBMISSION_ID, QUESTION_SUBMISSION_ID_2);
        List<AnswerSubmissionDto> requestList = List.of(
            AnswerSubmissionDto.builder().questionSubmissionId(QUESTION_SUBMISSION_UUID).build(),
            AnswerSubmissionDto.builder().questionSubmissionId(QUESTION_SUBMISSION_UUID_2).build());

        assertThrows(QuestionSubmissionNotMatchingException.class, () -> answerSubmissionController.postAnswerSubmissions(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, requestList, httpServletRequest));
    }

    @Test
    void testPostAnswerSubmissionsPropagatesTypeNotSupported() throws Exception {
        List<AnswerSubmissionDto> requestList = List.of(AnswerSubmissionDto.builder().questionSubmissionId(QUESTION_SUBMISSION_UUID).build());
        when(answerSubmissionService.postAnswerSubmissions(requestList)).thenThrow(new TypeNotSupportedException("bad type"));

        assertThrows(TypeNotSupportedException.class, () -> answerSubmissionController.postAnswerSubmissions(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, requestList, httpServletRequest));
    }

    @Test
    void testPostAnswerSubmissionsPropagatesExceedingLimit() throws Exception {
        List<AnswerSubmissionDto> requestList = List.of(AnswerSubmissionDto.builder().questionSubmissionId(QUESTION_SUBMISSION_UUID).build());
        when(answerSubmissionService.postAnswerSubmissions(requestList)).thenThrow(new ExceedingLimitException("too many"));

        assertThrows(ExceedingLimitException.class, () -> answerSubmissionController.postAnswerSubmissions(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, requestList, httpServletRequest));
    }

    // updateAnswerSubmission

    @Test
    void testUpdateAnswerSubmissionSuccess() throws Exception {
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().answerSubmissionId(ANSWER_SUBMISSION_UUID).build();

        ResponseEntity<Void> response = answerSubmissionController.updateAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, dto, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(answerSubmissionService, times(1)).updateAnswerSubmission(dto, ANSWER_SUBMISSION_ID, ANSWER_TYPE);
    }

    @Test
    void testUpdateAnswerSubmissionUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().answerSubmissionId(ANSWER_SUBMISSION_UUID).build();

        ResponseEntity<Void> response = answerSubmissionController.updateAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, dto, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(answerSubmissionService, never()).updateAnswerSubmission(any(), anyLong(), anyString());
    }

    @Test
    void testUpdateAnswerSubmissionAsLearnerValidatesUser() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().answerSubmissionId(ANSWER_SUBMISSION_UUID).build();

        answerSubmissionController.updateAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, dto, httpServletRequest);

        verify(submissionService, times(1)).validateUser(EXPERIMENT_ID, USER_ID, SUBMISSION_ID);
    }

    // NOTE (likely bug): updateAnswerSubmission declares `throws AnswerNotMatchingException` but its
    // body wraps the service call in `catch (Exception e)` and always rethrows a new DataServiceException.
    // This means AnswerNotMatchingException (and any other exception the service throws) can never
    // actually propagate as itself from this method - it is always laundered into a DataServiceException,
    // making the declared "throws AnswerNotMatchingException" on the method signature dead/misleading.
    @Test
    void testUpdateAnswerSubmissionWrapsAnswerNotMatchingAsDataServiceException() throws Exception {
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().answerSubmissionId(ANSWER_SUBMISSION_UUID).build();
        doThrow(new AnswerNotMatchingException("not matching")).when(answerSubmissionService)
            .updateAnswerSubmission(dto, ANSWER_SUBMISSION_ID, ANSWER_TYPE);

        DataServiceException thrown = assertThrows(DataServiceException.class, () -> answerSubmissionController.updateAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, dto, httpServletRequest));
        assertTrue(thrown.getMessage().contains("Error 105"));
    }

    @Test
    void testUpdateAnswerSubmissionWrapsDataServiceException() throws Exception {
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().answerSubmissionId(ANSWER_SUBMISSION_UUID).build();
        doThrow(new DataServiceException("db down")).when(answerSubmissionService)
            .updateAnswerSubmission(dto, ANSWER_SUBMISSION_ID, ANSWER_TYPE);

        DataServiceException thrown = assertThrows(DataServiceException.class, () -> answerSubmissionController.updateAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, dto, httpServletRequest));
        assertTrue(thrown.getMessage().contains("Error 105"));
    }

    @Test
    void testUpdateAnswerSubmissionPropagatesAnswerSubmissionNotMatching() throws Exception {
        doThrow(new AnswerSubmissionNotMatchingException("not matching")).when(apiJwtService)
            .answerSubmissionAllowed(securedInfo, QUESTION_SUBMISSION_ID, ANSWER_TYPE, ANSWER_SUBMISSION_ID);
        AnswerSubmissionDto dto = AnswerSubmissionDto.builder().answerSubmissionId(ANSWER_SUBMISSION_UUID).build();

        assertThrows(AnswerSubmissionNotMatchingException.class, () -> answerSubmissionController.updateAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, dto, httpServletRequest));
    }

    // deleteAnswerSubmission

    @Test
    void testDeleteAnswerSubmissionSuccess() throws Exception {
        ResponseEntity<Void> response = answerSubmissionController.deleteAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(answerSubmissionService, times(1)).deleteAnswerSubmission(ANSWER_SUBMISSION_ID, ANSWER_TYPE);
    }

    @Test
    void testDeleteAnswerSubmissionUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = answerSubmissionController.deleteAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
        verify(answerSubmissionService, never()).deleteAnswerSubmission(anyLong(), anyString());
    }

    @Test
    void testDeleteAnswerSubmissionWrapsDataServiceException() throws Exception {
        doThrow(new DataServiceException("db down")).when(answerSubmissionService).deleteAnswerSubmission(ANSWER_SUBMISSION_ID, ANSWER_TYPE);

        DataServiceException thrown = assertThrows(DataServiceException.class, () -> answerSubmissionController.deleteAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, httpServletRequest));
        assertTrue(thrown.getMessage().contains("Error 105"));
    }

    @Test
    void testDeleteAnswerSubmissionPropagatesQuestionSubmissionNotMatching() throws Exception {
        doThrow(new QuestionSubmissionNotMatchingException("not matching")).when(apiJwtService)
            .questionSubmissionAllowed(securedInfo, ASSESSMENT_ID, SUBMISSION_ID, QUESTION_SUBMISSION_ID);

        assertThrows(QuestionSubmissionNotMatchingException.class, () -> answerSubmissionController.deleteAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, httpServletRequest));
    }

    @Test
    void testDeleteAnswerSubmissionPropagatesAnswerSubmissionNotMatching() throws Exception {
        doThrow(new AnswerSubmissionNotMatchingException("not matching")).when(apiJwtService)
            .answerSubmissionAllowed(securedInfo, QUESTION_SUBMISSION_ID, ANSWER_TYPE, ANSWER_SUBMISSION_ID);

        assertThrows(AnswerSubmissionNotMatchingException.class, () -> answerSubmissionController.deleteAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, httpServletRequest));
    }

    // postFileAnswerSubmission

    @Test
    void testPostFileAnswerSubmissionEmptyFile() throws Exception {
        when(multipartFile.isEmpty()).thenReturn(true);

        ResponseEntity<List<AnswerSubmissionDto>> response = answerSubmissionController.postFileAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, dtoJson(QUESTION_SUBMISSION_UUID),
            UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(TextConstants.FILE_MISSING, response.getBody());
        verify(apiJwtService, never()).extractValues(any(), anyBoolean());
    }

    @Test
    void testPostFileAnswerSubmissionMalformedJson() {
        assertThrows(JacksonException.class, () -> answerSubmissionController.postFileAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, "{not-json",
            UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest));
    }

    @Test
    void testPostFileAnswerSubmissionSuccess() throws Exception {
        AnswerSubmissionDto returnedDto = AnswerSubmissionDto.builder()
            .answerSubmissionId(ANSWER_SUBMISSION_UUID)
            .questionSubmissionId(QUESTION_SUBMISSION_UUID)
            .build();
        when(answerSubmissionService.handleFileAnswerSubmission(any(AnswerSubmissionDto.class), eq(multipartFile))).thenReturn(returnedDto);
        when(answerSubmissionService.buildHeaders(any(UriComponentsBuilder.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class)))
            .thenReturn(new HttpHeaders());

        ResponseEntity<List<AnswerSubmissionDto>> response = answerSubmissionController.postFileAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, dtoJson(QUESTION_SUBMISSION_UUID),
            UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(returnedDto, response.getBody().get(0));
    }

    @Test
    void testPostFileAnswerSubmissionUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<AnswerSubmissionDto>> response = answerSubmissionController.postFileAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, dtoJson(QUESTION_SUBMISSION_UUID),
            UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
        verify(answerSubmissionService, never()).handleFileAnswerSubmission(any(), any());
    }

    @Test
    void testPostFileAnswerSubmissionAsLearnerValidatesUser() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        AnswerSubmissionDto returnedDto = AnswerSubmissionDto.builder()
            .answerSubmissionId(ANSWER_SUBMISSION_UUID)
            .questionSubmissionId(QUESTION_SUBMISSION_UUID)
            .build();
        when(answerSubmissionService.handleFileAnswerSubmission(any(AnswerSubmissionDto.class), eq(multipartFile))).thenReturn(returnedDto);
        when(answerSubmissionService.buildHeaders(any(UriComponentsBuilder.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class)))
            .thenReturn(new HttpHeaders());

        answerSubmissionController.postFileAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, dtoJson(QUESTION_SUBMISSION_UUID),
            UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest);

        verify(submissionService, times(1)).validateUser(EXPERIMENT_ID, USER_ID, SUBMISSION_ID);
    }

    @Test
    void testPostFileAnswerSubmissionPropagatesExperimentNotMatching() throws Exception {
        doThrow(new ExperimentNotMatchingException("not matching")).when(apiJwtService).experimentAllowed(securedInfo, EXPERIMENT_ID);

        assertThrows(ExperimentNotMatchingException.class, () -> answerSubmissionController.postFileAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, dtoJson(QUESTION_SUBMISSION_UUID),
            UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest));
    }

    // putFileAnswerSubmission

    @Test
    void testPutFileAnswerSubmissionEmptyFile() throws Exception {
        when(multipartFile.isEmpty()).thenReturn(true);

        ResponseEntity<List<AnswerSubmissionDto>> response = answerSubmissionController.putFileAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, dtoJson(QUESTION_SUBMISSION_UUID),
            UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(TextConstants.FILE_MISSING, response.getBody());
    }

    @Test
    void testPutFileAnswerSubmissionSuccess() throws Exception {
        AnswerSubmissionDto returnedDto = AnswerSubmissionDto.builder()
            .answerSubmissionId(ANSWER_SUBMISSION_UUID)
            .questionSubmissionId(QUESTION_SUBMISSION_UUID)
            .build();
        when(answerSubmissionService.handleFileAnswerSubmissionUpdate(any(AnswerSubmissionDto.class), eq(multipartFile))).thenReturn(returnedDto);
        when(answerSubmissionService.buildHeaders(any(UriComponentsBuilder.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class)))
            .thenReturn(new HttpHeaders());

        ResponseEntity<List<AnswerSubmissionDto>> response = answerSubmissionController.putFileAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, dtoJson(QUESTION_SUBMISSION_UUID),
            UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(returnedDto, response.getBody().get(0));
    }

    // NOTE (likely bug, pre-existing): unlike getAnswerSubmission/updateAnswerSubmission/deleteAnswerSubmission/
    // downloadFileAnswerSubmission, putFileAnswerSubmission never calls apijwtService.answerSubmissionAllowed
    // for the {answerSubmissionId} path variable. The uuid path variable IS now resolved (validating that it
    // corresponds to a real answer submission of the derived type), but the resolved numeric id is never used
    // for anything further - the record actually updated is determined solely by
    // answerSubmissionDto.getAnswerSubmissionId() from the request body. There is still no check that the path
    // matches the body, nor an ownership/permission check on the answer submission being replaced.
    @Test
    void testPutFileAnswerSubmissionDoesNotValidateAnswerSubmissionIdOwnership() throws Exception {
        AnswerSubmissionDto returnedDto = AnswerSubmissionDto.builder()
            .answerSubmissionId(ANSWER_SUBMISSION_UUID)
            .questionSubmissionId(QUESTION_SUBMISSION_UUID)
            .build();
        when(answerSubmissionService.handleFileAnswerSubmissionUpdate(any(AnswerSubmissionDto.class), eq(multipartFile))).thenReturn(returnedDto);
        when(answerSubmissionService.buildHeaders(any(UriComponentsBuilder.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class)))
            .thenReturn(new HttpHeaders());

        answerSubmissionController.putFileAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, dtoJson(QUESTION_SUBMISSION_UUID),
            UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest);

        verify(apiJwtService, never()).answerSubmissionAllowed(any(), anyLong(), anyString(), anyLong());
    }

    @Test
    void testPutFileAnswerSubmissionUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<AnswerSubmissionDto>> response = answerSubmissionController.putFileAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, dtoJson(QUESTION_SUBMISSION_UUID),
            UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
        verify(answerSubmissionService, never()).handleFileAnswerSubmissionUpdate(any(), any());
    }

    @Test
    void testPutFileAnswerSubmissionPropagatesAssessmentNotMatching() throws Exception {
        doThrow(new AssessmentNotMatchingException("not matching")).when(apiJwtService)
            .assessmentAllowed(securedInfo, EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID);

        assertThrows(AssessmentNotMatchingException.class, () -> answerSubmissionController.putFileAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, dtoJson(QUESTION_SUBMISSION_UUID),
            UriComponentsBuilder.newInstance(), multipartFile, httpServletRequest));
    }

    // downloadFileAnswerSubmission

    @Test
    void testDownloadFileAnswerSubmissionSuccess() throws Exception {
        File realFile = File.createTempFile("answer-submission", ".txt");
        realFile.deleteOnExit();
        FileResponseDto fileResponseDto = FileResponseDto.builder()
            .fileName("answer.txt")
            .mimeType(MediaType.TEXT_PLAIN_VALUE)
            .file(realFile)
            .build();
        when(answerSubmissionService.getFileResponseDto(ANSWER_SUBMISSION_ID)).thenReturn(fileResponseDto);

        ResponseEntity<Resource> response = answerSubmissionController.downloadFileAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
        assertEquals(realFile.length(), response.getHeaders().getContentLength());
        assertEquals("attachment; filename=\"answer.txt\"; filename*=UTF-8''answer.txt", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertTrue(response.getBody() instanceof InputStreamResource);
    }

    @Test
    void testDownloadFileAnswerSubmissionUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Resource> response = answerSubmissionController.downloadFileAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
        verify(answerSubmissionService, never()).getFileResponseDto(anyLong());
    }

    @Test
    void testDownloadFileAnswerSubmissionPropagatesAnswerSubmissionNotMatching() throws Exception {
        doThrow(new AnswerSubmissionNotMatchingException("not matching")).when(apiJwtService)
            .answerSubmissionAllowed(securedInfo, QUESTION_SUBMISSION_ID, ANSWER_TYPE, ANSWER_SUBMISSION_ID);

        assertThrows(AnswerSubmissionNotMatchingException.class, () -> answerSubmissionController.downloadFileAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, httpServletRequest));
    }

    @Test
    void testDownloadFileAnswerSubmissionPropagatesIOException() throws Exception {
        when(answerSubmissionService.getFileResponseDto(ANSWER_SUBMISSION_ID)).thenThrow(new IOException("disk error"));

        assertThrows(IOException.class, () -> answerSubmissionController.downloadFileAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, httpServletRequest));
    }

    @Test
    void testDownloadFileAnswerSubmissionPropagatesTerracottaConnectorException() throws Exception {
        when(apiJwtService.extractValues(any(), eq(false))).thenThrow(new TerracottaConnectorException("connector down"));

        assertThrows(TerracottaConnectorException.class, () -> answerSubmissionController.downloadFileAnswerSubmission(
            EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, QUESTION_SUBMISSION_UUID, ANSWER_SUBMISSION_UUID, httpServletRequest));
    }

}
