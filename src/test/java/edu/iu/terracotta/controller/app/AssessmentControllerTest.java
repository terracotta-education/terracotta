package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AssessmentDto;
import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.utils.TextConstants;

public class AssessmentControllerTest extends BaseTest {

    private static final UUID EXPERIMENT_UUID = UUID.randomUUID();

    // the uuid path variable for the one condition under test; condition.getConditionId()
    // (the mock's globally-stubbed return value, see BaseModelTest) is what it resolves to
    private static final UUID CONDITION_UUID = UUID.randomUUID();

    // the uuid path variable for the one treatment under test; treatment.getTreatmentId()
    // (the mock's globally-stubbed return value, see BaseModelTest) is what it resolves to
    private static final UUID TREATMENT_UUID = UUID.randomUUID();

    // ConditionService has no mock in the BaseTest hierarchy, so it must be declared locally.
    @Mock private ConditionService conditionService;

    private AssessmentController assessmentController;

    private final long experimentId = 1L;
    private final long conditionId = 1L;
    private final long treatmentId = 1L;
    private final long assessmentId = 1L;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        // Constructed manually rather than via @InjectMocks: ApiJwtService is also implemented by the
        // inherited canvasApiJwtService mock (see the ambiguity warning in BaseServiceTest), so
        // constructor-injection-by-type could silently wire the wrong ApiJwtService mock.
        assessmentController = new AssessmentController(apiJwtService, experimentService, conditionService, assessmentService, submissionService, treatmentService);

        when(experimentService.getExperimentByUuid(EXPERIMENT_UUID)).thenReturn(experiment);
        when(conditionService.getConditionByUuid(CONDITION_UUID)).thenReturn(condition);
        when(treatmentService.getTreatmentByUuid(TREATMENT_UUID)).thenReturn(treatment);
    }

    private void stubAuthorized() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
    }

    // getAssessmentByTreatment

    @Test
    void getAssessmentByTreatmentTest() throws Exception {
        stubAuthorized();
        List<AssessmentDto> list = List.of(assessmentDto);
        when(assessmentService.getAllAssessmentsByTreatment(treatmentId, false, securedInfo)).thenReturn(list);

        ResponseEntity<List<AssessmentDto>> response = assessmentController.getAssessmentByTreatment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, false, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(list, response.getBody());
    }

    @Test
    void getAssessmentByTreatmentEmptyReturnsNoContentTest() throws Exception {
        stubAuthorized();
        when(assessmentService.getAllAssessmentsByTreatment(treatmentId, false, securedInfo)).thenReturn(List.of());

        ResponseEntity<List<AssessmentDto>> response = assessmentController.getAssessmentByTreatment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, false, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getAssessmentByTreatmentUnauthorizedTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<AssessmentDto>> response = assessmentController.getAssessmentByTreatment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, false, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getAssessmentByTreatmentPropagatesExperimentNotMatchingTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.experimentAllowed(securedInfo, experimentId)).thenThrow(new ExperimentNotMatchingException("no match"));

        // BUG/design smell: getAssessmentByTreatment has no try/catch of its own, so this checked
        // exception (declared "throws") propagates straight out of the controller method with no
        // permission check having happened yet.
        assertThrows(ExperimentNotMatchingException.class, () -> assessmentController.getAssessmentByTreatment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, false, httpServletRequest));
    }

    @Test
    void getAssessmentByTreatmentPropagatesAssessmentNotMatchingTest() throws Exception {
        stubAuthorized();
        when(assessmentService.getAllAssessmentsByTreatment(treatmentId, false, securedInfo)).thenThrow(new AssessmentNotMatchingException("no match"));

        assertThrows(AssessmentNotMatchingException.class, () -> assessmentController.getAssessmentByTreatment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, false, httpServletRequest));
    }

    // getAssessment

    @Test
    void getAssessmentInstructorHappyPathTest() throws Exception {
        stubAuthorized();
        when(assessmentService.getAssessment(assessmentId)).thenReturn(assessment);
        when(assessmentService.toDto(eq(assessment), isNull(), anyBoolean(), anyBoolean(), anyBoolean(), eq(false), eq(securedInfo))).thenReturn(assessmentDto);

        ResponseEntity<AssessmentDto> response = assessmentController.getAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentId, true, true, true, null, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(assessmentDto, response.getBody());
    }

    @Test
    void getAssessmentStudentWithSubmissionIdFetchesOwnSubmissionTest() throws Exception {
        stubAuthorized();
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        when(assessmentService.getAssessment(assessmentId)).thenReturn(assessment);
        when(assessmentService.toDto(eq(assessment), eq(5L), anyBoolean(), anyBoolean(), anyBoolean(), eq(true), eq(securedInfo))).thenReturn(assessmentDto);

        ResponseEntity<AssessmentDto> response = assessmentController.getAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentId, false, false, false, 5L, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(submissionService, times(1)).getSubmission(experimentId, securedInfo.getUserId(), 5L, true);
    }

    @Test
    void getAssessmentUnauthorizedTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<AssessmentDto> response = assessmentController.getAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentId, false, false, false, null, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getAssessmentPropagatesAssessmentNotMatchingTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        org.mockito.Mockito.doThrow(new AssessmentNotMatchingException("no match")).when(apiJwtService).assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        assertThrows(AssessmentNotMatchingException.class, () -> assessmentController.getAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentId, false, false, false, null, httpServletRequest));
    }

    @Test
    void getAssessmentStudentNoSubmissionsPropagatesTest() throws Exception {
        stubAuthorized();
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        when(submissionService.getSubmission(eq(experimentId), any(), eq(5L), eq(true))).thenThrow(new NoSubmissionsException("not the student's submission"));

        assertThrows(NoSubmissionsException.class, () -> assessmentController.getAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentId, false, false, false, 5L, httpServletRequest));
    }

    @Test
    void getAssessmentPropagatesSubmissionNotMatchingTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        org.mockito.Mockito.doThrow(new SubmissionNotMatchingException("no match")).when(apiJwtService).submissionAllowed(securedInfo, assessmentId, 5L);

        assertThrows(SubmissionNotMatchingException.class, () -> assessmentController.getAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentId, false, false, false, 5L, httpServletRequest));
    }

    // postAssessment

    @Test
    void postAssessmentTest() throws Exception {
        stubAuthorized();
        UriComponentsBuilder ucBuilder = UriComponentsBuilder.newInstance();
        when(assessmentService.postAssessment(assessmentDto, treatmentId, securedInfo)).thenReturn(assessmentDto);
        when(assessmentService.buildHeaders(ucBuilder, experimentId, conditionId, treatmentId, assessmentDto.getAssessmentId())).thenReturn(new HttpHeaders());

        ResponseEntity<AssessmentDto> response = assessmentController.postAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentDto, ucBuilder, httpServletRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(assessmentDto, response.getBody());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void postAssessmentUnauthorizedTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        UriComponentsBuilder ucBuilder = UriComponentsBuilder.newInstance();

        // Declared as raw ResponseEntity on purpose: the controller's unauthorized branch returns a raw
        // `new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, ...)` whose body is a String, not an
        // AssessmentDto. Reading it through the method's declared ResponseEntity<AssessmentDto> generic
        // would make javac insert a checkcast to AssessmentDto on getBody() and throw ClassCastException.
        ResponseEntity response = assessmentController.postAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentDto, ucBuilder, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void postAssessmentPropagatesTitleValidationTest() throws Exception {
        stubAuthorized();
        UriComponentsBuilder ucBuilder = UriComponentsBuilder.newInstance();
        when(assessmentService.postAssessment(assessmentDto, treatmentId, securedInfo)).thenThrow(new TitleValidationException("bad title"));

        assertThrows(TitleValidationException.class, () -> assessmentController.postAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentDto, ucBuilder, httpServletRequest));
    }

    @Test
    void postAssessmentPropagatesTreatmentNotMatchingTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        org.mockito.Mockito.doThrow(new TreatmentNotMatchingException("no match")).when(apiJwtService).treatmentAllowed(securedInfo, experimentId, conditionId, treatmentId);
        UriComponentsBuilder ucBuilder = UriComponentsBuilder.newInstance();

        assertThrows(TreatmentNotMatchingException.class, () -> assessmentController.postAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentDto, ucBuilder, httpServletRequest));
    }

    // putAssessment

    @Test
    void putAssessmentTest() throws Exception {
        stubAuthorized();
        when(assessmentService.putAssessment(assessmentId, assessmentDto, true, securedInfo)).thenReturn(assessmentDto);

        ResponseEntity<AssessmentDto> response = assessmentController.putAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentId, assessmentDto, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(assessmentDto, response.getBody());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void putAssessmentUnauthorizedTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        // Raw ResponseEntity for the same reason as postAssessmentUnauthorizedTest above.
        ResponseEntity response = assessmentController.putAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentId, assessmentDto, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void putAssessmentPropagatesRevealResponsesValidationTest() throws Exception {
        stubAuthorized();
        when(assessmentService.putAssessment(assessmentId, assessmentDto, true, securedInfo)).thenThrow(new RevealResponsesSettingValidationException("bad setting"));

        assertThrows(RevealResponsesSettingValidationException.class, () -> assessmentController.putAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentId, assessmentDto, httpServletRequest));
    }

    // deleteAssessment

    @Test
    void deleteAssessmentTest() throws Exception {
        stubAuthorized();

        ResponseEntity<Void> response = assessmentController.deleteAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentId, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(assessmentService, times(1)).deleteById(assessmentId);
    }

    @Test
    void deleteAssessmentEmptyResultDataAccessReturnsNotFoundTest() throws Exception {
        stubAuthorized();
        org.mockito.Mockito.doThrow(new EmptyResultDataAccessException(1)).when(assessmentService).deleteById(assessmentId);

        ResponseEntity<Void> response = assessmentController.deleteAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentId, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void deleteAssessmentUnauthorizedTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        // Raw ResponseEntity: deleteAssessment declares ResponseEntity<Void> but its unauthorized branch
        // returns a raw ResponseEntity with a String body, so getBody() through the Void generic would
        // insert a checkcast to Void and throw ClassCastException against the actual String body.
        ResponseEntity response = assessmentController.deleteAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentId, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void deleteAssessmentPropagatesAssessmentNotMatchingTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        org.mockito.Mockito.doThrow(new AssessmentNotMatchingException("no match")).when(apiJwtService).assessmentAllowed(securedInfo, experimentId, conditionId, treatmentId, assessmentId);

        assertThrows(AssessmentNotMatchingException.class, () -> assessmentController.deleteAssessment(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentId, httpServletRequest));
    }

    // regrade

    @Test
    void regradeTest() throws Exception {
        stubAuthorized();

        ResponseEntity<Void> response = assessmentController.regrade(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentId, regradeDetails, httpServletRequest);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(assessmentService, times(1)).regradeQuestions(regradeDetails, assessmentId);
    }

    @Test
    void regradeUnauthorizedTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = assessmentController.regrade(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentId, regradeDetails, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void regradePropagatesConnectionExceptionTest() throws Exception {
        stubAuthorized();
        org.mockito.Mockito.doThrow(new ConnectionException("down")).when(assessmentService).regradeQuestions(regradeDetails, assessmentId);

        assertThrows(ConnectionException.class, () -> assessmentController.regrade(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentId, regradeDetails, httpServletRequest));
    }

    @Test
    void regradePropagatesTreatmentNotMatchingTest() throws Exception {
        when(apiJwtService.extractValues(httpServletRequest, false)).thenReturn(securedInfo);
        org.mockito.Mockito.doThrow(new TreatmentNotMatchingException("no match")).when(apiJwtService).treatmentAllowed(securedInfo, experimentId, conditionId, treatmentId);

        assertThrows(TreatmentNotMatchingException.class, () -> assessmentController.regrade(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, assessmentId, regradeDetails, httpServletRequest));
    }

}
