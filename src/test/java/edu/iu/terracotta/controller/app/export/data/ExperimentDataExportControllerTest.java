package edu.iu.terracotta.controller.app.export.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.export.data.ExperimentDataExportDto;
import edu.iu.terracotta.service.app.export.data.ExperimentDataExportService;

public class ExperimentDataExportControllerTest extends BaseTest {

    private static final UUID EXPERIMENT_UUID = UUID.randomUUID();
    private static final long EXPERIMENT_ID = 1L;

    private ExperimentDataExportController experimentDataExportController;

    // not present in the BaseServiceTest/BaseRepositoryTest/BaseModelTest hierarchy, so it is
    // declared locally rather than reused from an inherited field
    @Mock private ExperimentDataExportService experimentDataExportService;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        // constructed manually rather than via @InjectMocks - see the note in
        // AssignmentFileArchiveControllerTest about apiJwtService colliding with
        // CanvasApiJwtServiceImpl in BaseServiceTest
        experimentDataExportController = new ExperimentDataExportController(apiJwtService, experimentService, experimentDataExportService);

        when(apiJwtService.extractValues(any(), eq(false))).thenReturn(securedInfo);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(experimentService.getExperimentByUuid(EXPERIMENT_UUID)).thenReturn(experiment);
        when(experiment.getExperimentId()).thenReturn(EXPERIMENT_ID);
        when(apiJwtService.experimentAllowed(securedInfo, EXPERIMENT_ID)).thenReturn(experiment);
    }

    // the frontend posts the experiments' uuids (ExperimentDto.experimentId) and sends a
    // placeholder "0" for the route's own experimentId segment, which this list endpoint never
    // needs - both have to be accepted as-is
    @Test
    void testPollListResolvesUuidsAndIgnoresPlaceholderPathSegment() throws Exception {
        ExperimentDataExportDto experimentDataExportDto = mock(ExperimentDataExportDto.class);
        when(experimentDataExportService.poll(List.of(experiment), securedInfo, false)).thenReturn(List.of(experimentDataExportDto));

        ResponseEntity<List<ExperimentDataExportDto>> response = experimentDataExportController.pollList("0", false, List.of(EXPERIMENT_UUID), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(experimentDataExportDto), response.getBody());
        verify(apiJwtService).experimentAllowed(securedInfo, EXPERIMENT_ID);
    }

    @Test
    void testPollListUnauthorizedBelowInstructor() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<ExperimentDataExportDto>> response = experimentDataExportController.pollList("0", false, List.of(EXPERIMENT_UUID), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(experimentDataExportService, never()).poll(anyList(), any(), anyBoolean());
    }

    @Test
    void testPollListUnknownExperimentUuidIsBadRequest() throws Exception {
        UUID unknownUuid = UUID.randomUUID();
        when(experimentService.getExperimentByUuid(unknownUuid)).thenThrow(new ExperimentNotMatchingException("not found"));

        ResponseEntity<List<ExperimentDataExportDto>> response = experimentDataExportController.pollList("0", false, List.of(unknownUuid), httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(experimentDataExportService, never()).poll(anyList(), any(), anyBoolean());
    }

}
