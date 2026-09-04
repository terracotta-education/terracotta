package edu.iu.terracotta.runner.export.data.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.export.data.ExperimentDataExport;
import edu.iu.terracotta.dao.model.enums.export.data.ExperimentDataExportStatus;
import edu.iu.terracotta.dao.repository.export.data.ExperimentDataExportRepository;
import edu.iu.terracotta.runner.export.data.model.ExperimentDataExportScheduleMessage;
import edu.iu.terracotta.runner.export.data.model.ExperimentDataExportScheduleResult;

@SuppressWarnings("unchecked")
class ExperimentDataExportSchedulerServiceImplTest extends BaseTest {

    @Mock private ExperimentDataExportRepository experimentDataExportRepository;

    @TempDir private Path tempDir;

    private ExperimentDataExportSchedulerServiceImpl experimentDataExportSchedulerService;

    @BeforeEach
    void beforeEach() throws IOException {
        MockitoAnnotations.openMocks(this);
        setup();

        experimentDataExportSchedulerService = new ExperimentDataExportSchedulerServiceImpl(experimentDataExportRepository);
        ReflectionTestUtils.setField(experimentDataExportSchedulerService, "experimentDataExportLocalPathRoot", tempDir.toString());
        ReflectionTestUtils.setField(experimentDataExportSchedulerService, "ttl", 7);

        // sentinel that is never touched by production logic; keeps tempDir root
        // from ever being considered an "empty directory" by the trailing cleanup
        // pass, isolating that behavior to the test that specifically exercises it
        Path sentinel = Files.createDirectories(tempDir.resolve("keep-alive"));
        Files.createFile(sentinel.resolve(".keep"));
    }

    private ExperimentDataExport buildExport(long id, String fileUri) {
        ExperimentDataExport export = ExperimentDataExport.builder()
            .fileName("export-" + id + ".zip")
            .fileUri(fileUri)
            .status(ExperimentDataExportStatus.READY)
            .build();
        export.setId(id);

        return export;
    }

    private void mockExpiredExports(List<ExperimentDataExport> exports) {
        when(experimentDataExportRepository.findAllByUpdatedAtLessThanAndStatusIn(any(Timestamp.class), anyList()))
            .thenReturn(exports);
    }

    @Test
    void testCleanupNoExpiredExportsReturnsEmpty() {
        ArgumentCaptor<List<ExperimentDataExportStatus>> statusesCaptor = ArgumentCaptor.forClass(List.class);
        when(experimentDataExportRepository.findAllByUpdatedAtLessThanAndStatusIn(any(Timestamp.class), statusesCaptor.capture()))
            .thenReturn(Collections.emptyList());

        Optional<ExperimentDataExportScheduleResult> result = experimentDataExportSchedulerService.cleanup();

        assertTrue(result.isEmpty());
        // confirms the queried status set, including READY_ACKNOWLEDGED which the
        // sibling AssignmentFileArchive scheduler's cleanup query does not include
        assertEquals(
            List.of(
                ExperimentDataExportStatus.DOWNLOADED,
                ExperimentDataExportStatus.ERROR,
                ExperimentDataExportStatus.ERROR_ACKNOWLEDGED,
                ExperimentDataExportStatus.OUTDATED,
                ExperimentDataExportStatus.READY,
                ExperimentDataExportStatus.READY_ACKNOWLEDGED
            ),
            statusesCaptor.getValue()
        );
        verify(experimentDataExportRepository, never()).save(any());
    }

    @Test
    void testCleanupBothFilesDeletedSuccessfully() throws IOException {
        ExperimentDataExport export = buildExport(1L, "export-1");
        Files.createFile(tempDir.resolve("export-1"));
        Files.createFile(tempDir.resolve("export-1" + ExperimentDataExport.COMPRESSED_FILE_EXTENSION));
        mockExpiredExports(List.of(export));

        Optional<ExperimentDataExportScheduleResult> result = experimentDataExportSchedulerService.cleanup();

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getProcessed().size());
        ExperimentDataExportScheduleMessage message = result.get().getProcessed().get(0);
        assertNull(message.getErrors());
        assertEquals(1L, message.getId());
        assertEquals("export-1", message.getFileUri());
        assertEquals("export-1.zip", message.getFileName());
        assertNotNull(message.getDeletedAt());
        assertFalse(Files.exists(tempDir.resolve("export-1")));
        assertFalse(Files.exists(tempDir.resolve("export-1" + ExperimentDataExport.COMPRESSED_FILE_EXTENSION)));
        assertEquals(ExperimentDataExportStatus.DELETED, export.getStatus());
        verify(experimentDataExportRepository).save(export);
    }

    @Test
    void testCleanupCompressedFileMissingRecordsError() throws IOException {
        ExperimentDataExport export = buildExport(2L, "export-2");
        Files.createFile(tempDir.resolve("export-2"));
        // no compressed counterpart created; its delete() will return false
        mockExpiredExports(List.of(export));

        Optional<ExperimentDataExportScheduleResult> result = experimentDataExportSchedulerService.cleanup();

        ExperimentDataExportScheduleMessage message = result.get().getProcessed().get(0);
        assertEquals(List.of("Failed to delete file URI: [export-2]"), message.getErrors());
        // status is still moved to DELETED unconditionally, even on delete failure
        assertEquals(ExperimentDataExportStatus.DELETED, export.getStatus());
        verify(experimentDataExportRepository).save(export);
    }

    @Test
    void testCleanupPrimaryFileMissingRecordsError() throws IOException {
        ExperimentDataExport export = buildExport(3L, "export-3");
        // primary file never created; its delete() will return false
        Files.createFile(tempDir.resolve("export-3" + ExperimentDataExport.COMPRESSED_FILE_EXTENSION));
        mockExpiredExports(List.of(export));

        Optional<ExperimentDataExportScheduleResult> result = experimentDataExportSchedulerService.cleanup();

        ExperimentDataExportScheduleMessage message = result.get().getProcessed().get(0);
        assertEquals(List.of("Failed to delete file URI: [export-3]"), message.getErrors());
        assertEquals(ExperimentDataExportStatus.DELETED, export.getStatus());
    }

    @Test
    void testCleanupSecurityExceptionOnFirstDeleteMessageSurvives() {
        ExperimentDataExport export = buildExport(4L, "export-4");
        mockExpiredExports(List.of(export));

        String firstPath = String.format("%s/%s", tempDir, "export-4");

        try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class, CALLS_REAL_METHODS)) {
            pathsMock.when(() -> Paths.get(firstPath)).thenThrow(new SecurityException("first delete denied"));

            Optional<ExperimentDataExportScheduleResult> result = experimentDataExportSchedulerService.cleanup();

            // the exception is thrown before the second delete is even attempted,
            // so `deleted` stays empty; anyMatch() on an empty list is false, so
            // the generic "Failed to delete" message never overwrites this error
            ExperimentDataExportScheduleMessage message = result.get().getProcessed().get(0);
            assertEquals(List.of("first delete denied"), message.getErrors());
        }

        assertEquals(ExperimentDataExportStatus.DELETED, export.getStatus());
    }

    @Test
    void testCleanupSecurityExceptionOnSecondDeleteMessageSurvivesWhenFirstSucceeded() throws IOException {
        ExperimentDataExport export = buildExport(5L, "export-5");
        Files.createFile(tempDir.resolve("export-5"));
        mockExpiredExports(List.of(export));

        String secondPath = String.format("%s/%s%s", tempDir, "export-5", ExperimentDataExport.COMPRESSED_FILE_EXTENSION);

        try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class, CALLS_REAL_METHODS)) {
            pathsMock.when(() -> Paths.get(secondPath)).thenThrow(new SecurityException("second delete denied"));

            Optional<ExperimentDataExportScheduleResult> result = experimentDataExportSchedulerService.cleanup();

            // first delete succeeded (true), so deleted = [true]; anyMatch(!x) is
            // false and the exception message is preserved as-is
            ExperimentDataExportScheduleMessage message = result.get().getProcessed().get(0);
            assertEquals(List.of("second delete denied"), message.getErrors());
        }

        assertEquals(ExperimentDataExportStatus.DELETED, export.getStatus());
    }

    @Test
    void testCleanupSecurityExceptionOnSecondDeleteMessageSurvivesWhenFirstAlreadyFailed() {
        ExperimentDataExport export = buildExport(6L, "export-6");
        // primary file never created; its delete() returns false (not an exception)
        mockExpiredExports(List.of(export));

        String secondPath = String.format("%s/%s%s", tempDir, "export-6", ExperimentDataExport.COMPRESSED_FILE_EXTENSION);

        try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class, CALLS_REAL_METHODS)) {
            pathsMock.when(() -> Paths.get(secondPath)).thenThrow(new SecurityException("second delete denied"));

            Optional<ExperimentDataExportScheduleResult> result = experimentDataExportSchedulerService.cleanup();

            ExperimentDataExportScheduleMessage message = result.get().getProcessed().get(0);
            assertEquals(List.of("second delete denied"), message.getErrors());
        }

        assertEquals(ExperimentDataExportStatus.DELETED, export.getStatus());
    }

    @Test
    void testCleanupDeletesEmptyDirectoriesButKeepsNonEmptyOnes() throws IOException {
        ExperimentDataExport export = buildExport(7L, "export-7");
        Files.createFile(tempDir.resolve("export-7"));
        Files.createFile(tempDir.resolve("export-7" + ExperimentDataExport.COMPRESSED_FILE_EXTENSION));
        mockExpiredExports(List.of(export));

        Path emptyDir = Files.createDirectories(tempDir.resolve("empty-dir"));
        Path nonEmptyDir = Files.createDirectories(tempDir.resolve("non-empty-dir"));
        Files.createFile(nonEmptyDir.resolve("child.txt"));

        experimentDataExportSchedulerService.cleanup();

        assertFalse(Files.exists(emptyDir));
        assertTrue(Files.exists(nonEmptyDir));
        assertTrue(Files.exists(nonEmptyDir.resolve("child.txt")));
    }

}
