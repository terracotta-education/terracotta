package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.media.MediaEventDto;
import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.exceptions.ParameterMissingException;
import edu.iu.terracotta.service.app.MediaService;
import jakarta.servlet.http.HttpServletRequest;

public class MediaProfileControllerTest extends BaseTest {

    private static final long EXPERIMENT_ID = 1L;
    private static final long CONDITION_ID = 2L;
    private static final long TREATMENT_ID = 3L;
    private static final long ASSESSMENT_ID = 4L;
    private static final long SUBMISSION_ID = 5L;
    private static final long QUESTION_ID = 6L;

    @Mock private MediaService mediaService;

    private MediaProfileController mediaProfileController;
    private MediaEventDto mediaEventDto;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        // ApiJwtService/ApiClient/LmsUtils have multiple type-matching mocks in BaseServiceTest
        // (e.g. canvasApiJwtService also implements ApiJwtService), so @InjectMocks constructor
        // resolution by type alone is unreliable here; construct the controller explicitly instead.
        mediaProfileController = new MediaProfileController(mediaService, apiJwtService);
        mediaEventDto = new MediaEventDto();

        when(apiJwtService.extractValues(any(HttpServletRequest.class), eq(false))).thenReturn(securedInfo);
    }

    @Test
    void testPostMediaEvent() throws Exception {
        ResponseEntity<?> response = mediaProfileController.postMediaEvent(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, SUBMISSION_ID, QUESTION_ID, mediaEventDto, null, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(mediaService).fromDto(mediaEventDto, securedInfo, EXPERIMENT_ID, SUBMISSION_ID, QUESTION_ID);
    }

    @Test
    void testPostMediaEventExperimentNotMatching() throws Exception {
        doThrow(new ExperimentNotMatchingException("experiment not matching")).when(apiJwtService).experimentAllowed(securedInfo, EXPERIMENT_ID);

        assertThrows(
            ExperimentNotMatchingException.class,
            () -> mediaProfileController.postMediaEvent(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, SUBMISSION_ID, QUESTION_ID, mediaEventDto, null, httpServletRequest)
        );
    }

    @Test
    void testPostMediaEventTreatmentNotMatching() throws Exception {
        doThrow(new TreatmentNotMatchingException("treatment not matching")).when(apiJwtService).treatmentAllowed(securedInfo, EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID);

        assertThrows(
            TreatmentNotMatchingException.class,
            () -> mediaProfileController.postMediaEvent(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, SUBMISSION_ID, QUESTION_ID, mediaEventDto, null, httpServletRequest)
        );
    }

    @Test
    void testPostMediaEventSubmissionNotMatching() throws Exception {
        doThrow(new SubmissionNotMatchingException("submission not matching")).when(apiJwtService).submissionAllowed(securedInfo, ASSESSMENT_ID, SUBMISSION_ID);

        assertThrows(
            SubmissionNotMatchingException.class,
            () -> mediaProfileController.postMediaEvent(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, SUBMISSION_ID, QUESTION_ID, mediaEventDto, null, httpServletRequest)
        );
    }

    @Test
    void testPostMediaEventQuestionNotMatching() throws Exception {
        doThrow(new QuestionNotMatchingException("question not matching")).when(apiJwtService).questionAllowed(securedInfo, ASSESSMENT_ID, QUESTION_ID);

        assertThrows(
            QuestionNotMatchingException.class,
            () -> mediaProfileController.postMediaEvent(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, SUBMISSION_ID, QUESTION_ID, mediaEventDto, null, httpServletRequest)
        );
    }

    @Test
    void testPostMediaEventParameterMissing() throws Exception {
        doThrow(new ParameterMissingException("parameter missing")).when(mediaService).fromDto(any(MediaEventDto.class), any(), anyLong(), anyLong(), anyLong());

        assertThrows(
            ParameterMissingException.class,
            () -> mediaProfileController.postMediaEvent(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, SUBMISSION_ID, QUESTION_ID, mediaEventDto, null, httpServletRequest)
        );
    }

    @Test
    void testPostMediaEventNoSubmissions() throws Exception {
        doThrow(new NoSubmissionsException("no submissions")).when(mediaService).fromDto(any(MediaEventDto.class), any(), anyLong(), anyLong(), anyLong());

        assertThrows(
            NoSubmissionsException.class,
            () -> mediaProfileController.postMediaEvent(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, ASSESSMENT_ID, SUBMISSION_ID, QUESTION_ID, mediaEventDto, null, httpServletRequest)
        );
    }

}
