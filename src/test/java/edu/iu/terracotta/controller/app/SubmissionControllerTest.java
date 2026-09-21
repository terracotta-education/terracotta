package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
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
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.dao.model.dto.SubmissionDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.utils.TextConstants;

public class SubmissionControllerTest extends BaseTest {

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
    // the uuid path variable for the one submission under test; submission.getSubmissionId()
    // (the mock's globally-stubbed return value, see BaseModelTest) is what it resolves to
    private static final UUID SUBMISSION_UUID = UUID.randomUUID();

    // ConditionService has no mock in the BaseTest hierarchy, so it must be declared locally.
    @Mock private ConditionService conditionService;

    private SubmissionController submissionController;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        // Constructed manually rather than via @InjectMocks: ApiJwtService is also implemented by the
        // inherited canvasApiJwtService mock (see the ambiguity warning in BaseServiceTest), so
        // constructor-injection-by-type could silently wire the wrong ApiJwtService mock.
        submissionController = new SubmissionController(apiJwtService, experimentService, conditionService, submissionService, treatmentService, assessmentService);

        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(experimentService.getExperimentIdByUuid(EXPERIMENT_UUID)).thenAnswer(invocation -> experiment.getExperimentId());
        when(conditionService.getConditionIdByUuid(CONDITION_UUID)).thenAnswer(invocation -> condition.getConditionId());
        when(treatmentService.getTreatmentIdByUuid(TREATMENT_UUID)).thenAnswer(invocation -> treatment.getTreatmentId());
        when(assessmentService.getAssessmentIdByUuid(ASSESSMENT_UUID)).thenAnswer(invocation -> assessment.getAssessmentId());
        when(submissionService.getSubmissionIdByUuid(SUBMISSION_UUID)).thenAnswer(invocation -> submission.getSubmissionId());
    }

    @Test
    void getSubmissionsByAssessmentTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(submissionService.getSubmissions(anyLong(), anyString(), anyLong(), anyBoolean())).thenReturn(List.of(submissionDto));

        ResponseEntity<List<SubmissionDto>> response = submissionController.getSubmissionsByAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getSubmissionsByAssessmentNoContentTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(submissionService.getSubmissions(anyLong(), anyString(), anyLong(), anyBoolean())).thenReturn(List.of());

        ResponseEntity<List<SubmissionDto>> response = submissionController.getSubmissionsByAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getSubmissionsByAssessmentUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<SubmissionDto>> response = submissionController.getSubmissionsByAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void getSubmissionsByAssessmentThrowsTest() throws Exception {
        doThrow(new ExperimentNotMatchingException("error")).when(apiJwtService).experimentAllowed(any(SecuredInfo.class), anyLong());

        assertThrows(ExperimentNotMatchingException.class, () -> submissionController.getSubmissionsByAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, httpServletRequest));
    }

    @Test
    void getSubmissionTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(submissionService.getSubmission(anyLong(), anyString(), anyLong(), anyBoolean())).thenReturn(submission);
        when(submissionService.toDto(any(Submission.class), anyBoolean(), anyBoolean())).thenReturn(submissionDto);

        ResponseEntity<SubmissionDto> response = submissionController.getSubmission(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, false, false, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(submissionDto, response.getBody());
    }

    @Test
    void getSubmissionUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<SubmissionDto> response = submissionController.getSubmission(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, false, false, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void getSubmissionThrowsTest() throws Exception {
        doThrow(new SubmissionNotMatchingException("error")).when(apiJwtService).submissionAllowed(any(SecuredInfo.class), anyLong(), anyLong());

        assertThrows(SubmissionNotMatchingException.class, () -> submissionController.getSubmission(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, false, false, httpServletRequest));
    }

    @Test
    void postSubmissionDatesNotAllowedTest() throws Exception {
        when(submissionService.datesAllowed(anyLong(), anyLong(), any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<SubmissionDto> response = submissionController.postSubmission(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, submissionDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Error 128: Assignment locked", response.getBody());
    }

    @Test
    void postSubmissionUnauthorizedTest() throws Exception {
        when(submissionService.datesAllowed(anyLong(), anyLong(), any(SecuredInfo.class))).thenReturn(true);
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<SubmissionDto> response = submissionController.postSubmission(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, submissionDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void postSubmissionTest() throws Exception {
        when(submissionService.datesAllowed(anyLong(), anyLong(), any(SecuredInfo.class))).thenReturn(true);
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        when(submissionDto.getSubmissionId()).thenReturn(SUBMISSION_UUID);
        when(submissionService.postSubmission(any(SubmissionDto.class), anyLong(), any(SecuredInfo.class), anyLong(), anyBoolean())).thenReturn(submissionDto);
        when(submissionService.buildHeaders(any(UriComponentsBuilder.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class))).thenReturn(new HttpHeaders());

        ResponseEntity<SubmissionDto> response = submissionController.postSubmission(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, submissionDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(submissionDto, response.getBody());
    }

    @Test
    void postSubmissionThrowsTest() throws Exception {
        when(submissionService.datesAllowed(anyLong(), anyLong(), any(SecuredInfo.class))).thenReturn(true);
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(submissionService.postSubmission(any(SubmissionDto.class), anyLong(), any(SecuredInfo.class), anyLong(), anyBoolean())).thenThrow(new IdInPostException("error"));

        assertThrows(IdInPostException.class, () -> submissionController.postSubmission(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, submissionDto, UriComponentsBuilder.newInstance(), httpServletRequest));
    }

    @Test
    void updateSubmissionTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(submissionService.getSubmission(anyLong(), anyString(), anyLong(), anyBoolean())).thenReturn(submission);

        ResponseEntity<Void> response = submissionController.updateSubmission(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, submissionDto, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateSubmissionUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = submissionController.updateSubmission(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, submissionDto, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void updateSubmissionThrowsTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(submissionService.getSubmission(anyLong(), anyString(), anyLong(), anyBoolean())).thenReturn(submission);
        doThrow(new DataServiceException("error")).when(submissionService).updateSubmissions(anyMap(), anyBoolean());

        assertThrows(DataServiceException.class, () -> submissionController.updateSubmission(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, submissionDto, httpServletRequest));
    }

    @Test
    void updateSubmissionsTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(submissionDto.getSubmissionId()).thenReturn(SUBMISSION_UUID);
        when(submissionService.getSubmission(anyLong(), anyString(), anyLong(), anyBoolean())).thenReturn(submission);

        ResponseEntity<Void> response = submissionController.updateSubmissions(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, List.of(submissionDto), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateSubmissionsUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = submissionController.updateSubmissions(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, List.of(submissionDto), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void updateSubmissionsThrowsTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(submissionDto.getSubmissionId()).thenReturn(SUBMISSION_UUID);
        when(submissionService.getSubmission(anyLong(), anyString(), anyLong(), anyBoolean())).thenReturn(submission);
        doThrow(new RuntimeException("boom")).when(submissionService).updateSubmissions(anyMap(), anyBoolean());

        assertThrows(DataServiceException.class, () -> submissionController.updateSubmissions(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, List.of(submissionDto), httpServletRequest));
    }

    @Test
    void deleteSubmissionTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);

        ResponseEntity<Void> response = submissionController.deleteSubmission(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteSubmissionUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = submissionController.deleteSubmission(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void deleteSubmissionNotFoundTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        doThrow(new EmptyResultDataAccessException(1)).when(submissionService).deleteById(anyLong());

        ResponseEntity<Void> response = submissionController.deleteSubmission(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_UUID, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

}
