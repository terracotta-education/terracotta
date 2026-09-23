package edu.iu.terracotta.controller.app.export.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.export.data.ExperimentDataExportDto;
import edu.iu.terracotta.dao.model.enums.export.data.ExperimentDataExportStatus;
import edu.iu.terracotta.exceptions.export.data.ExperimentDataExportException;
import edu.iu.terracotta.exceptions.export.data.ExperimentDataExportNotFoundException;
import edu.iu.terracotta.service.app.export.data.ExperimentDataExportService;

public class ExperimentDataExportControllerTest extends BaseTest {

    private static final UUID EXPERIMENT_UUID = UUID.randomUUID();
    private static final long EXPERIMENT_ID = 1L;
    private static final UUID FILE_ID = UUID.randomUUID();

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
        when(experimentService.getExperimentIdByUuid(EXPERIMENT_UUID)).thenAnswer(invocation -> experiment.getExperimentId());
        when(experiment.getExperimentId()).thenReturn(EXPERIMENT_ID);
        when(apiJwtService.experimentAllowed(securedInfo, EXPERIMENT_ID)).thenReturn(experiment);
    }

    @Test
    void testProcessSuccess() throws Exception {
        ExperimentDataExportDto experimentDataExportDto = mock(ExperimentDataExportDto.class);
        when(experimentDataExportService.process(experiment, securedInfo)).thenReturn(experimentDataExportDto);

        ResponseEntity<ExperimentDataExportDto> response = experimentDataExportController.process(EXPERIMENT_UUID, httpServletRequest);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(experimentDataExportDto, response.getBody());
    }

    @Test
    void testProcessUnauthorizedBelowInstructor() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<ExperimentDataExportDto> response = experimentDataExportController.process(EXPERIMENT_UUID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testProcessServiceExceptionIsBadRequest() throws Exception {
        when(experimentDataExportService.process(experiment, securedInfo)).thenThrow(new ExperimentDataExportException("failed to process"));

        ResponseEntity<ExperimentDataExportDto> response = experimentDataExportController.process(EXPERIMENT_UUID, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testPollSuccess() throws Exception {
        ExperimentDataExportDto experimentDataExportDto = mock(ExperimentDataExportDto.class);
        when(experimentDataExportService.poll(experiment, securedInfo, false)).thenReturn(experimentDataExportDto);

        ResponseEntity<ExperimentDataExportDto> response = experimentDataExportController.poll(EXPERIMENT_UUID, false, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(experimentDataExportDto, response.getBody());
    }

    @Test
    void testPollUnauthorizedBelowInstructor() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<ExperimentDataExportDto> response = experimentDataExportController.poll(EXPERIMENT_UUID, false, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testPollServiceExceptionIsBadRequest() throws Exception {
        when(experimentDataExportService.poll(experiment, securedInfo, true)).thenThrow(new ExperimentDataExportException("failed to poll"));

        ResponseEntity<ExperimentDataExportDto> response = experimentDataExportController.poll(EXPERIMENT_UUID, true, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
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
        when(experimentService.getExperimentIdByUuid(unknownUuid)).thenThrow(new ExperimentNotMatchingException("not found"));

        ResponseEntity<List<ExperimentDataExportDto>> response = experimentDataExportController.pollList("0", false, List.of(unknownUuid), httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(experimentDataExportService, never()).poll(anyList(), any(), anyBoolean());
    }

    @Test
    void testRetrieveSuccess() throws Exception {
        File realFile = File.createTempFile("export", ".zip");
        realFile.deleteOnExit();
        ExperimentDataExportDto experimentDataExportDto = ExperimentDataExportDto.builder()
            .id(FILE_ID)
            .fileName("export.zip")
            .mimeType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .file(realFile)
            .build();
        when(experimentDataExportService.retrieve(FILE_ID, experiment, securedInfo)).thenReturn(experimentDataExportDto);

        ResponseEntity<Resource> response = experimentDataExportController.retrieve(EXPERIMENT_UUID, FILE_ID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("attachment; filename=\"export.zip\"; filename*=UTF-8''export.zip", response.getHeaders().getFirst("Content-Disposition"));
        assertEquals(realFile.length(), response.getHeaders().getContentLength());
        assertTrue(response.getBody() instanceof InputStreamResource);
    }

    @Test
    void testRetrieveOutdatedReturnsConflict() throws Exception {
        ExperimentDataExportDto experimentDataExportDto = ExperimentDataExportDto.builder().id(FILE_ID).file(null).build();
        when(experimentDataExportService.retrieve(FILE_ID, experiment, securedInfo)).thenReturn(experimentDataExportDto);

        ResponseEntity<Resource> response = experimentDataExportController.retrieve(EXPERIMENT_UUID, FILE_ID, httpServletRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void testRetrieveUnauthorizedBelowInstructor() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Resource> response = experimentDataExportController.retrieve(EXPERIMENT_UUID, FILE_ID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testRetrieveServiceExceptionIsBadRequest() throws Exception {
        when(experimentDataExportService.retrieve(FILE_ID, experiment, securedInfo)).thenThrow(new ExperimentDataExportException("failed to retrieve"));

        ResponseEntity<Resource> response = experimentDataExportController.retrieve(EXPERIMENT_UUID, FILE_ID, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testErrorAcknowledgeSuccess() throws Exception {
        ResponseEntity<ExperimentDataExportDto> response = experimentDataExportController.errorAcknowledge(EXPERIMENT_UUID, FILE_ID, ExperimentDataExportStatus.ERROR_ACKNOWLEDGED, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(experimentDataExportService).acknowledge(FILE_ID, experiment, ExperimentDataExportStatus.ERROR_ACKNOWLEDGED);
    }

    @Test
    void testErrorAcknowledgeUnauthorizedBelowInstructor() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<ExperimentDataExportDto> response = experimentDataExportController.errorAcknowledge(EXPERIMENT_UUID, FILE_ID, ExperimentDataExportStatus.ERROR_ACKNOWLEDGED, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(experimentDataExportService, never()).acknowledge(any(), any(), any());
    }

    @Test
    void testErrorAcknowledgeNotFoundReturnsBadRequest() throws Exception {
        doThrow(new ExperimentDataExportNotFoundException("not found")).when(experimentDataExportService).acknowledge(FILE_ID, experiment, ExperimentDataExportStatus.OUTDATED_ACKNOWLEDGED);

        ResponseEntity<ExperimentDataExportDto> response = experimentDataExportController.errorAcknowledge(EXPERIMENT_UUID, FILE_ID, ExperimentDataExportStatus.OUTDATED_ACKNOWLEDGED, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

}
