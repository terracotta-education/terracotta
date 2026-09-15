package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

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
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.service.app.MediaService;
import jakarta.servlet.http.HttpServletRequest;

public class MediaProfileControllerTest extends BaseTest {

    private static final long EXPERIMENT_ID = 1L;
    // matches condition.getConditionId() (the mock's globally-stubbed return value, see BaseModelTest),
    // which is what conditionService.getConditionByUuid(CONDITION_UUID) below resolves to
    private static final long CONDITION_ID = 1L;
    private static final long TREATMENT_ID = 3L;
    private static final long ASSESSMENT_ID = 4L;
    private static final long SUBMISSION_ID = 5L;
    private static final long QUESTION_ID = 6L;

    // the uuid path variable for the one question under test; overrides the shared question
    // mock's getQuestionId() (see BaseModelTest) to resolve to QUESTION_ID above
    private static final UUID QUESTION_UUID = UUID.randomUUID();

    // the uuid path variable for the one experiment under test; experiment.getExperimentId()
    // (the mock's globally-stubbed return value, see BaseModelTest) is what it resolves to
    private static final UUID EXPERIMENT_UUID = UUID.randomUUID();

    private static final UUID CONDITION_UUID = UUID.randomUUID();

    // the uuid path variable for the one treatment under test; overrides the shared treatment
    // mock's getTreatmentId() (see BaseModelTest) to resolve to TREATMENT_ID below, matching what
    // this test's apijwtService.treatmentAllowed(...) stubs already expect
    private static final UUID TREATMENT_UUID = UUID.randomUUID();

    // the uuid path variable for the one assessment under test; overrides the shared assessment
    // mock's getAssessmentId() (see BaseModelTest) to resolve to ASSESSMENT_ID above, matching
    // what this test's apijwtService stubs already expect
    private static final UUID ASSESSMENT_UUID = UUID.randomUUID();

    @Mock private MediaService mediaService;

    // ConditionService has no mock in the BaseTest hierarchy, so it must be declared locally.
    @Mock private ConditionService conditionService;

    private MediaProfileController mediaProfileController;
    private MediaEventDto mediaEventDto;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        // ApiJwtService/ApiClient/LmsUtils have multiple type-matching mocks in BaseServiceTest
        // (e.g. canvasApiJwtService also implements ApiJwtService), so @InjectMocks constructor
        // resolution by type alone is unreliable here; construct the controller explicitly instead.
        mediaProfileController = new MediaProfileController(mediaService, apiJwtService, experimentService, conditionService, treatmentService, assessmentService, questionService);
        mediaEventDto = new MediaEventDto();

        when(apiJwtService.extractValues(any(HttpServletRequest.class), eq(false))).thenReturn(securedInfo);
        when(experimentService.getExperimentByUuid(EXPERIMENT_UUID)).thenReturn(experiment);
        when(conditionService.getConditionByUuid(CONDITION_UUID)).thenReturn(condition);
        when(treatment.getTreatmentId()).thenReturn(TREATMENT_ID);
        when(treatmentService.getTreatmentByUuid(TREATMENT_UUID)).thenReturn(treatment);
        when(assessment.getAssessmentId()).thenReturn(ASSESSMENT_ID);
        when(assessmentService.getAssessmentByUuid(ASSESSMENT_UUID)).thenReturn(assessment);
        when(question.getQuestionId()).thenReturn(QUESTION_ID);
        when(questionService.getQuestionByUuid(QUESTION_UUID)).thenReturn(question);
    }

    @Test
    void testPostMediaEvent() throws Exception {
        ResponseEntity<?> response = mediaProfileController.postMediaEvent(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_ID, QUESTION_UUID, mediaEventDto, null, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(mediaService).fromDto(mediaEventDto, securedInfo, EXPERIMENT_ID, SUBMISSION_ID, QUESTION_ID);
    }

    @Test
    void testPostMediaEventExperimentNotMatching() throws Exception {
        doThrow(new ExperimentNotMatchingException("experiment not matching")).when(apiJwtService).experimentAllowed(securedInfo, EXPERIMENT_ID);

        assertThrows(
            ExperimentNotMatchingException.class,
            () -> mediaProfileController.postMediaEvent(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_ID, QUESTION_UUID, mediaEventDto, null, httpServletRequest)
        );
    }

    @Test
    void testPostMediaEventTreatmentNotMatching() throws Exception {
        doThrow(new TreatmentNotMatchingException("treatment not matching")).when(apiJwtService).treatmentAllowed(securedInfo, EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID);

        assertThrows(
            TreatmentNotMatchingException.class,
            () -> mediaProfileController.postMediaEvent(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_ID, QUESTION_UUID, mediaEventDto, null, httpServletRequest)
        );
    }

    @Test
    void testPostMediaEventSubmissionNotMatching() throws Exception {
        doThrow(new SubmissionNotMatchingException("submission not matching")).when(apiJwtService).submissionAllowed(securedInfo, ASSESSMENT_ID, SUBMISSION_ID);

        assertThrows(
            SubmissionNotMatchingException.class,
            () -> mediaProfileController.postMediaEvent(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_ID, QUESTION_UUID, mediaEventDto, null, httpServletRequest)
        );
    }

    @Test
    void testPostMediaEventQuestionNotMatching() throws Exception {
        doThrow(new QuestionNotMatchingException("question not matching")).when(apiJwtService).questionAllowed(securedInfo, ASSESSMENT_ID, QUESTION_ID);

        assertThrows(
            QuestionNotMatchingException.class,
            () -> mediaProfileController.postMediaEvent(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_ID, QUESTION_UUID, mediaEventDto, null, httpServletRequest)
        );
    }

    @Test
    void testPostMediaEventParameterMissing() throws Exception {
        doThrow(new ParameterMissingException("parameter missing")).when(mediaService).fromDto(any(MediaEventDto.class), any(), anyLong(), anyLong(), anyLong());

        assertThrows(
            ParameterMissingException.class,
            () -> mediaProfileController.postMediaEvent(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_ID, QUESTION_UUID, mediaEventDto, null, httpServletRequest)
        );
    }

    @Test
    void testPostMediaEventNoSubmissions() throws Exception {
        doThrow(new NoSubmissionsException("no submissions")).when(mediaService).fromDto(any(MediaEventDto.class), any(), anyLong(), anyLong(), anyLong());

        assertThrows(
            NoSubmissionsException.class,
            () -> mediaProfileController.postMediaEvent(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, ASSESSMENT_UUID, SUBMISSION_ID, QUESTION_UUID, mediaEventDto, null, httpServletRequest)
        );
    }

}
