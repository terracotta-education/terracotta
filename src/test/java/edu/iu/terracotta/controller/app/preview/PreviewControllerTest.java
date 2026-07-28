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
import edu.iu.terracotta.service.app.preview.TreatmentPreviewService;
import jakarta.servlet.http.HttpServletRequest;

public class PreviewControllerTest extends BaseTest {

    private static final long EXPERIMENT_ID = 1L;
    private static final long CONDITION_ID = 2L;
    private static final long TREATMENT_ID = 3L;
    private static final String OWNER_ID = "owner-1";

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
        previewController = new PreviewController(apiJwtService, treatmentPreviewService);
        previewId = UUID.randomUUID();

        when(apiJwtService.extractValues(any(HttpServletRequest.class), eq(false))).thenReturn(securedInfo);
    }

    @Test
    void testGetTreatmentPreview() throws Exception {
        when(treatmentPreview.getUuid()).thenReturn(previewId);
        when(treatmentPreviewService.create(TREATMENT_ID, EXPERIMENT_ID, CONDITION_ID, OWNER_ID)).thenReturn(treatmentPreview);

        String ret = previewController.getTreatmentPreview(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, OWNER_ID, httpServletRequest);

        assertTrue(Strings.CS.contains(ret, "experiment=" + EXPERIMENT_ID), ret);
        assertTrue(Strings.CS.contains(ret, "condition=" + CONDITION_ID), ret);
        assertTrue(Strings.CS.contains(ret, "treatment=" + TREATMENT_ID), ret);
        assertTrue(Strings.CS.contains(ret, "previewId=" + previewId), ret);
        assertTrue(Strings.CS.contains(ret, "ownerId=" + OWNER_ID), ret);
    }

    @Test
    void testGetTreatmentPreviewId() throws Exception {
        when(treatmentPreviewService.getTreatmentPreview(previewId, TREATMENT_ID, EXPERIMENT_ID, CONDITION_ID, OWNER_ID, securedInfo)).thenReturn(treatmentPreviewDto);

        ResponseEntity<TreatmentPreviewDto> response = previewController.getTreatmentPreviewId(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, previewId, OWNER_ID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(treatmentPreviewDto, response.getBody());
    }

    @Test
    void testGetTreatmentPreviewIdTreatmentNotMatching() throws Exception {
        when(treatmentPreviewService.getTreatmentPreview(any(UUID.class), anyLong(), anyLong(), anyLong(), anyString(), any())).thenThrow(new TreatmentNotMatchingException("treatment not matching"));

        ResponseEntity<TreatmentPreviewDto> response = previewController.getTreatmentPreviewId(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, previewId, OWNER_ID, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetTreatmentPreviewIdAssessmentNotMatching() throws Exception {
        when(treatmentPreviewService.getTreatmentPreview(any(UUID.class), anyLong(), anyLong(), anyLong(), anyString(), any())).thenThrow(new AssessmentNotMatchingException("assessment not matching"));

        ResponseEntity<TreatmentPreviewDto> response = previewController.getTreatmentPreviewId(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, previewId, OWNER_ID, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetTreatmentPreviewComplete() throws Exception {
        String ret = previewController.getTreatmentPreviewComplete(EXPERIMENT_ID, CONDITION_ID, TREATMENT_ID, OWNER_ID, httpServletRequest);

        assertEquals("redirect:/app/app.html?treatmentPreview=true&complete=true", ret);
    }

}
