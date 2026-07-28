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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import edu.iu.terracotta.utils.TextConstants;

public class SubmissionControllerTest extends BaseTest {

    private static final long EXPERIMENT_ID = 1L;
    private static final long CONDITION_ID = 1L;
    private static final long TREATMENT_ID = 1L;
    private static final long ASSESSMENT_ID = 1L;
    private static final long SUBMISSION_ID = 1L;

    private SubmissionController submissionController;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        // Constructed manually rather than via @InjectMocks: ApiJwtService is also implemented by the
        // inherited canvasApiJwtService mock (see the ambiguity warning in BaseServiceTest), so
        // constructor-injection-by-type could silently wire the wrong ApiJwtService mock.
        submissionController = new SubmissionController(apiJwtService, submissionService);

        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
    }

    @Test
    void getSubmissionsByAssessmentTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(submissionService.getSubmissions(anyLong(), anyString(), anyLong(), anyBoolean())).thenReturn(List.of(submissionDto));

        ResponseEntity<List<SubmissionDto>> response = submissionController.getSubmissionsByAssessment(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getSubmissionsByAssessmentNoContentTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(submissionService.getSubmissions(anyLong(), anyString(), anyLong(), anyBoolean())).thenReturn(List.of());

        ResponseEntity<List<SubmissionDto>> response = submissionController.getSubmissionsByAssessment(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getSubmissionsByAssessmentUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<SubmissionDto>> response = submissionController.getSubmissionsByAssessment(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void getSubmissionsByAssessmentThrowsTest() throws Exception {
        doThrow(new ExperimentNotMatchingException("error")).when(apiJwtService).experimentAllowed(any(SecuredInfo.class), anyLong());

        assertThrows(ExperimentNotMatchingException.class, () -> submissionController.getSubmissionsByAssessment(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, httpServletRequest));
    }

    @Test
    void getSubmissionTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(submissionService.getSubmission(anyLong(), anyString(), anyLong(), anyBoolean())).thenReturn(submission);
        when(submissionService.toDto(any(Submission.class), anyBoolean(), anyBoolean())).thenReturn(submissionDto);

        ResponseEntity<SubmissionDto> response = submissionController.getSubmission(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, SUBMISSION_ID, false, false, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(submissionDto, response.getBody());
    }

    @Test
    void getSubmissionUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<SubmissionDto> response = submissionController.getSubmission(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, SUBMISSION_ID, false, false, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void getSubmissionThrowsTest() throws Exception {
        doThrow(new SubmissionNotMatchingException("error")).when(apiJwtService).submissionAllowed(any(SecuredInfo.class), anyLong(), anyLong());

        assertThrows(SubmissionNotMatchingException.class, () -> submissionController.getSubmission(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, SUBMISSION_ID, false, false, httpServletRequest));
    }

    @Test
    void postSubmissionDatesNotAllowedTest() throws Exception {
        when(submissionService.datesAllowed(anyLong(), anyLong(), any(SecuredInfo.class))).thenReturn(false);

        ResponseEntity<SubmissionDto> response = submissionController.postSubmission(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, submissionDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Error 128: Assignment locked", response.getBody());
    }

    @Test
    void postSubmissionUnauthorizedTest() throws Exception {
        when(submissionService.datesAllowed(anyLong(), anyLong(), any(SecuredInfo.class))).thenReturn(true);
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<SubmissionDto> response = submissionController.postSubmission(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, submissionDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void postSubmissionTest() throws Exception {
        when(submissionService.datesAllowed(anyLong(), anyLong(), any(SecuredInfo.class))).thenReturn(true);
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        when(submissionDto.getSubmissionId()).thenReturn(SUBMISSION_ID);
        when(submissionService.postSubmission(any(SubmissionDto.class), anyLong(), any(SecuredInfo.class), anyLong(), anyBoolean())).thenReturn(submissionDto);
        when(submissionService.buildHeaders(any(UriComponentsBuilder.class), anyLong(), anyLong(), anyLong(), anyLong(), anyLong())).thenReturn(new HttpHeaders());

        ResponseEntity<SubmissionDto> response = submissionController.postSubmission(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, submissionDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(submissionDto, response.getBody());
    }

    @Test
    void postSubmissionThrowsTest() throws Exception {
        when(submissionService.datesAllowed(anyLong(), anyLong(), any(SecuredInfo.class))).thenReturn(true);
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(submissionService.postSubmission(any(SubmissionDto.class), anyLong(), any(SecuredInfo.class), anyLong(), anyBoolean())).thenThrow(new IdInPostException("error"));

        assertThrows(IdInPostException.class, () -> submissionController.postSubmission(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, submissionDto, UriComponentsBuilder.newInstance(), httpServletRequest));
    }

    @Test
    void updateSubmissionTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(submissionService.getSubmission(anyLong(), anyString(), anyLong(), anyBoolean())).thenReturn(submission);

        ResponseEntity<Void> response = submissionController.updateSubmission(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, SUBMISSION_ID, submissionDto, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateSubmissionUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = submissionController.updateSubmission(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, SUBMISSION_ID, submissionDto, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void updateSubmissionThrowsTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(submissionService.getSubmission(anyLong(), anyString(), anyLong(), anyBoolean())).thenReturn(submission);
        doThrow(new DataServiceException("error")).when(submissionService).updateSubmissions(anyMap(), anyBoolean());

        assertThrows(DataServiceException.class, () -> submissionController.updateSubmission(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, SUBMISSION_ID, submissionDto, httpServletRequest));
    }

    @Test
    void updateSubmissionsTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(submissionDto.getSubmissionId()).thenReturn(SUBMISSION_ID);
        when(submissionService.getSubmission(anyLong(), anyString(), anyLong(), anyBoolean())).thenReturn(submission);

        ResponseEntity<Void> response = submissionController.updateSubmissions(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, List.of(submissionDto), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateSubmissionsUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = submissionController.updateSubmissions(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, List.of(submissionDto), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void updateSubmissionsThrowsTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(submissionDto.getSubmissionId()).thenReturn(SUBMISSION_ID);
        when(submissionService.getSubmission(anyLong(), anyString(), anyLong(), anyBoolean())).thenReturn(submission);
        doThrow(new RuntimeException("boom")).when(submissionService).updateSubmissions(anyMap(), anyBoolean());

        assertThrows(DataServiceException.class, () -> submissionController.updateSubmissions(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, List.of(submissionDto), httpServletRequest));
    }

    @Test
    void deleteSubmissionTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);

        ResponseEntity<Void> response = submissionController.deleteSubmission(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, SUBMISSION_ID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteSubmissionUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = submissionController.deleteSubmission(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, SUBMISSION_ID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void deleteSubmissionNotFoundTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        doThrow(new EmptyResultDataAccessException(1)).when(submissionService).deleteById(anyLong());

        ResponseEntity<Void> response = submissionController.deleteSubmission(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, SUBMISSION_ID, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

}
