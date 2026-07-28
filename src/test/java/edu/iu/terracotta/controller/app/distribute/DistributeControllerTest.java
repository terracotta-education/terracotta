package edu.iu.terracotta.controller.app.distribute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.exceptions.ExperimentImportNotFoundException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.distribute.ExportDto;
import edu.iu.terracotta.dao.model.dto.distribute.ImportDto;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus;
import edu.iu.terracotta.exceptions.ExperimentExportException;
import edu.iu.terracotta.exceptions.ExperimentImportException;
import edu.iu.terracotta.service.app.distribute.ExperimentExportService;

public class DistributeControllerTest extends BaseTest {

    // ExperimentExportService has no mock declared anywhere in the BaseTest hierarchy, so it is declared here.
    @Mock private ExperimentExportService exportService;

    private DistributeController distributeController;

    private File tempFile;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        // ApiJwtService has two matching mocks in BaseServiceTest (apiJwtService and canvasApiJwtService),
        // so the controller is constructed manually rather than relying on @InjectMocks to avoid ambiguous wiring.
        distributeController = new DistributeController(apiJwtService, exportService, experimentImportService);

        when(apiJwtService.extractValues(any(), anyBoolean())).thenReturn(securedInfo);
        when(apiJwtService.experimentAllowed(any(), anyLong())).thenReturn(experiment);
        when(apiJwtService.experimentImportAllowed(any(), any(UUID.class))).thenReturn(experimentImport);
    }

    @AfterEach
    public void afterEach() {
        if (tempFile != null) {
            tempFile.delete();
        }
    }

    private File createTempFile() throws Exception {
        tempFile = File.createTempFile("export", ".zip");
        Files.write(tempFile.toPath(), "export contents".getBytes());

        return tempFile;
    }

    @Test
    void exportSuccessTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        ExportDto exportDto = ExportDto.builder()
            .mimeType("application/zip")
            .filename("export.zip")
            .file(createTempFile())
            .build();
        when(exportService.export(experiment)).thenReturn(exportDto);

        ResponseEntity<Resource> ret = distributeController.export(1L, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
    }

    @Test
    void exportUnauthorizedTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Resource> ret = distributeController.export(1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void exportFileNullTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        ExportDto exportDto = ExportDto.builder().file(null).build();
        when(exportService.export(experiment)).thenReturn(exportDto);

        ResponseEntity<Resource> ret = distributeController.export(1L, httpServletRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ret.getStatusCode());
        assertNull(ret.getBody());
    }

    @Test
    void exportServiceThrowsExceptionTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        doThrow(new ExperimentExportException("export failed")).when(exportService).export(experiment);

        ResponseEntity<Resource> ret = distributeController.export(1L, httpServletRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ret.getStatusCode());
    }

    @Test
    void exportFileNotFoundTest() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        ExportDto exportDto = ExportDto.builder()
            .mimeType("application/zip")
            .filename("missing.zip")
            .file(new File("/this/path/does/not/exist.zip"))
            .build();
        when(exportService.export(experiment)).thenReturn(exportDto);

        ResponseEntity<Resource> ret = distributeController.export(1L, httpServletRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ret.getStatusCode());
    }

    @Test
    void exportExperimentNotMatchingTest() throws Exception {
        doThrow(new ExperimentNotMatchingException("not matching")).when(apiJwtService).experimentAllowed(securedInfo, 1L);

        assertThrows(ExperimentNotMatchingException.class, () -> distributeController.export(1L, httpServletRequest));
    }

    @Test
    void importExperimentUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<ImportDto> ret = distributeController.importExperiment(multipartFile, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void importExperimentInvalidMimeTypeTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(experimentImportService.preprocessError(eq(multipartFile), any(String.class), eq(securedInfo))).thenReturn(importDto);

        ResponseEntity<ImportDto> ret = distributeController.importExperiment(multipartFile, httpServletRequest);

        assertEquals(HttpStatus.ACCEPTED, ret.getStatusCode());
        assertEquals(importDto, ret.getBody());
    }

    @Test
    void importExperimentSuccessTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(multipartFile.getContentType()).thenReturn("application/zip");
        when(experimentImportService.preprocess(multipartFile, securedInfo)).thenReturn(importDto);

        ResponseEntity<ImportDto> ret = distributeController.importExperiment(multipartFile, httpServletRequest);

        assertEquals(HttpStatus.ACCEPTED, ret.getStatusCode());
        assertEquals(importDto, ret.getBody());
    }

    @Test
    void importExperimentServiceThrowsExceptionTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(multipartFile.getContentType()).thenReturn("application/x-zip-compressed");
        doThrow(new ExperimentImportException("import failed")).when(experimentImportService).preprocess(multipartFile, securedInfo);

        ResponseEntity<ImportDto> ret = distributeController.importExperiment(multipartFile, httpServletRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ret.getStatusCode());
    }

    @Test
    void pollUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<ImportDto> ret = distributeController.poll(UUID.randomUUID(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void pollSuccessTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(experimentImportService.toDto(experimentImport)).thenReturn(importDto);

        ResponseEntity<ImportDto> ret = distributeController.poll(UUID.randomUUID(), httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(importDto, ret.getBody());
    }

    @Test
    void pollExceptionTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        doThrow(new RuntimeException("boom")).when(experimentImportService).toDto(experimentImport);

        ResponseEntity<ImportDto> ret = distributeController.poll(UUID.randomUUID(), httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, ret.getStatusCode());
    }

    @Test
    void pollImportNotFoundTest() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ExperimentImportNotFoundException("not found")).when(apiJwtService).experimentImportAllowed(securedInfo, id);

        assertThrows(ExperimentImportNotFoundException.class, () -> distributeController.poll(id, httpServletRequest));
    }

    @Test
    void pollAllUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<ImportDto>> ret = distributeController.pollAll(httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void pollAllSuccessTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(experimentImportService.getAll(securedInfo)).thenReturn(List.of(importDto));

        ResponseEntity<List<ImportDto>> ret = distributeController.pollAll(httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals(List.of(importDto), ret.getBody());
    }

    @Test
    void pollAllExceptionTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        doThrow(new RuntimeException("boom")).when(experimentImportService).getAll(securedInfo);

        ResponseEntity<List<ImportDto>> ret = distributeController.pollAll(httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, ret.getStatusCode());
    }

    @Test
    void acknowledgeErrorUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<ImportDto> ret = distributeController.acknowledgeError(UUID.randomUUID(), ExperimentImportStatus.ERROR_ACKNOWLEDGED, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, ret.getStatusCode());
    }

    @Test
    void acknowledgeErrorSuccessTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);

        ResponseEntity<ImportDto> ret = distributeController.acknowledgeError(UUID.randomUUID(), ExperimentImportStatus.ERROR_ACKNOWLEDGED, httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
    }

    @Test
    void acknowledgeErrorExceptionTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        doThrow(new RuntimeException("boom")).when(experimentImportService).acknowledge(experimentImport, ExperimentImportStatus.ERROR_ACKNOWLEDGED);

        ResponseEntity<ImportDto> ret = distributeController.acknowledgeError(UUID.randomUUID(), ExperimentImportStatus.ERROR_ACKNOWLEDGED, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, ret.getStatusCode());
    }

    @Test
    void acknowledgeErrorImportNotFoundTest() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ExperimentImportNotFoundException("not found")).when(apiJwtService).experimentImportAllowed(securedInfo, id);

        assertThrows(
            ExperimentImportNotFoundException.class,
            () -> distributeController.acknowledgeError(id, ExperimentImportStatus.ERROR_ACKNOWLEDGED, httpServletRequest)
        );
    }

}
