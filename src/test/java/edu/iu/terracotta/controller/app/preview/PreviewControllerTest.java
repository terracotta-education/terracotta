package edu.iu.terracotta.controller.app.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.preview.TreatmentPreview;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.preview.TreatmentPreviewDto;
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.service.app.preview.TreatmentPreviewService;
import jakarta.servlet.http.HttpServletRequest;

public class PreviewControllerTest extends BaseTest {

    private static final long EXPERIMENT_ID = 1L;
    private static final UUID EXPERIMENT_UUID = UUID.randomUUID();
    // matches condition.getConditionId() (the mock's globally-stubbed return value, see BaseModelTest),
    // which is what conditionService.getConditionIdByUuid(CONDITION_UUID) below resolves to
    private static final long CONDITION_ID = 1L;
    private static final UUID CONDITION_UUID = UUID.randomUUID();
    private static final long TREATMENT_ID = 3L;

    // the uuid path variable for the one treatment under test; overrides the shared treatment
    // mock's getTreatmentId() (see BaseModelTest) to resolve to TREATMENT_ID above, matching what
    // this test's treatmentPreviewService stubs already expect
    private static final UUID TREATMENT_UUID = UUID.randomUUID();
    private static final String OWNER_ID = "owner-1";

    // ConditionService has no mock in the BaseTest hierarchy, so it must be declared locally.
    @Mock private ConditionService conditionService;
    @Mock private TreatmentPreviewService treatmentPreviewService;
    @Mock private TreatmentPreview treatmentPreview;
    @Mock private TreatmentPreviewDto treatmentPreviewDto;

    private PreviewController previewController;
    private UUID previewId;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        // ApiJwtService has multiple type-matching mocks in BaseServiceTest (e.g. canvasApiJwtService
        // also implements it), so @InjectMocks constructor resolution by type alone is unreliable;
        // construct the controller explicitly instead.
        previewController = new PreviewController(apiJwtService, experimentService, conditionService, treatmentService, treatmentPreviewService);
        previewId = UUID.randomUUID();

        when(apiJwtService.extractValues(any(HttpServletRequest.class), eq(false))).thenReturn(securedInfo);
        when(experimentService.getExperimentIdByUuid(EXPERIMENT_UUID)).thenAnswer(invocation -> experiment.getExperimentId());
        when(conditionService.getConditionIdByUuid(CONDITION_UUID)).thenAnswer(invocation -> condition.getConditionId());
        when(treatment.getTreatmentId()).thenReturn(TREATMENT_ID);
        when(treatmentService.getTreatmentIdByUuid(TREATMENT_UUID)).thenAnswer(invocation -> treatment.getTreatmentId());
    }

    @Test
    void testGetTreatmentPreview() throws Exception {
        when(treatmentPreview.getUuid()).thenReturn(previewId);
        when(treatmentPreviewService.create(TREATMENT_ID, EXPERIMENT_ID, CONDITION_ID, OWNER_ID)).thenReturn(treatmentPreview);

        String ret = previewController.getTreatmentPreview(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, OWNER_ID, httpServletRequest);

        assertTrue(Strings.CS.contains(ret, "experiment=" + EXPERIMENT_UUID), ret);
        assertTrue(Strings.CS.contains(ret, "condition=" + CONDITION_UUID), ret);
        assertTrue(Strings.CS.contains(ret, "treatment=" + TREATMENT_UUID), ret);
        assertTrue(Strings.CS.contains(ret, "previewId=" + previewId), ret);
        assertTrue(Strings.CS.contains(ret, "ownerId=" + OWNER_ID), ret);
    }

    @Test
    void testGetTreatmentPreviewId() throws Exception {
        when(treatmentPreviewService.getTreatmentPreview(previewId, TREATMENT_ID, EXPERIMENT_ID, CONDITION_ID, OWNER_ID, securedInfo)).thenReturn(treatmentPreviewDto);

        ResponseEntity<TreatmentPreviewDto> response = previewController.getTreatmentPreviewId(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, previewId, OWNER_ID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(treatmentPreviewDto, response.getBody());
    }

    @Test
    void testGetTreatmentPreviewIdTreatmentNotMatching() throws Exception {
        when(treatmentPreviewService.getTreatmentPreview(any(UUID.class), anyLong(), anyLong(), anyLong(), anyString(), any())).thenThrow(new TreatmentNotMatchingException("treatment not matching"));

        ResponseEntity<TreatmentPreviewDto> response = previewController.getTreatmentPreviewId(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, previewId, OWNER_ID, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetTreatmentPreviewIdAssessmentNotMatching() throws Exception {
        when(treatmentPreviewService.getTreatmentPreview(any(UUID.class), anyLong(), anyLong(), anyLong(), anyString(), any())).thenThrow(new AssessmentNotMatchingException("assessment not matching"));

        ResponseEntity<TreatmentPreviewDto> response = previewController.getTreatmentPreviewId(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, previewId, OWNER_ID, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetTreatmentPreviewComplete() throws Exception {
        String ret = previewController.getTreatmentPreviewComplete(EXPERIMENT_UUID, CONDITION_UUID, TREATMENT_UUID, OWNER_ID, httpServletRequest);

        assertEquals("redirect:/app/app.html?treatmentPreview=true&complete=true", ret);
    }

}
