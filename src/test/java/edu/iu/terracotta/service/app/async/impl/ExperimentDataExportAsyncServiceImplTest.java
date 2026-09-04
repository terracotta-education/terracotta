package edu.iu.terracotta.service.app.async.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.export.data.ExperimentDataExport;
import edu.iu.terracotta.dao.model.enums.export.data.ExperimentDataExportStatus;
import edu.iu.terracotta.dao.repository.export.data.ExperimentDataExportRepository;
import edu.iu.terracotta.exceptions.export.data.ExperimentDataExportException;
import edu.iu.terracotta.service.app.ExportService;

public class ExperimentDataExportAsyncServiceImplTest extends BaseTest {

    @Mock private ExperimentDataExportRepository experimentDataExportRepository;
    @Mock private ExportService exportService;

    @InjectMocks private ExperimentDataExportAsyncServiceImpl experimentDataExportAsyncService;

    @TempDir private Path dataExportRoot;

    private ExperimentDataExport experimentDataExport;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        ReflectionTestUtils.setField(experimentDataExportAsyncService, "experimentDataExportLocalPathRoot", dataExportRoot.toString());

        when(experiment.getTitle()).thenReturn("My Experiment");

        experimentDataExport = ExperimentDataExport.builder().experiment(experiment).build();
        experimentDataExport.setId(1L);
        experimentDataExport.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        when(experimentDataExportRepository.findById(1L)).thenReturn(Optional.of(experimentDataExport));
    }

    @Test
    public void testProcessNotFoundThrows() {
        when(experimentDataExportRepository.findById(99L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(ExperimentDataExportException.class, () -> experimentDataExportAsyncService.process(99L, securedInfo));

        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    public void testProcessSuccessSavesReadyStatusAndUploadsZip() throws Exception {
        File source1 = Files.createTempFile("export-source-1", ".csv").toFile();
        File source2 = Files.createTempFile("export-source-2", ".csv").toFile();
        Files.writeString(source1.toPath(), "id,name\n1,foo\n");
        Files.writeString(source2.toPath(), "id,name\n2,bar\n");

        Map<String, String> files = new LinkedHashMap<>();
        files.put("data.csv", source1.getAbsolutePath());
        files.put("meta.csv", source2.getAbsolutePath());

        when(exportService.getFiles(anyLong(), eq(securedInfo))).thenReturn(files);

        experimentDataExportAsyncService.process(1L, securedInfo);

        assertEquals(ExperimentDataExportStatus.READY, experimentDataExport.getStatus());
        assertTrue(experimentDataExport.getFileName().contains("My_Experiment"));
        verify(experimentDataExportRepository).save(experimentDataExport);

        ArgumentCaptor<File> zipCaptor = ArgumentCaptor.forClass(File.class);
        verify(fileStorageService).saveExperimentDataExport(eq(experimentDataExport), zipCaptor.capture());

        File zipFile = zipCaptor.getValue();
        assertTrue(zipFile.exists());

        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry dataEntry = zip.getEntry("data.csv");
            ZipEntry metaEntry = zip.getEntry("meta.csv");
            assertTrue(dataEntry != null);
            assertTrue(metaEntry != null);
        } finally {
            Files.deleteIfExists(zipFile.toPath());
        }

        // source files are deleted after being zipped
        assertTrue(Files.notExists(source1.toPath()));
        assertTrue(Files.notExists(source2.toPath()));
    }

    @Test
    public void testProcessErrorSetsErrorStatusAndWrapsException() throws Exception {
        when(exportService.getFiles(anyLong(), eq(securedInfo))).thenThrow(new IOException("boom"));

        Exception exception = assertThrows(ExperimentDataExportException.class, () -> experimentDataExportAsyncService.process(1L, securedInfo));

        assertEquals("Error processing experiment data export", exception.getMessage());
        assertTrue(exception.getCause() instanceof IOException);
        assertEquals(ExperimentDataExportStatus.ERROR, experimentDataExport.getStatus());
        verify(experimentDataExportRepository).save(experimentDataExport);
    }

}
