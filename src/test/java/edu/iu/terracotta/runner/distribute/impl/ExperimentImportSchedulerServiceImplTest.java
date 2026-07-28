package edu.iu.terracotta.runner.distribute.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImport;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus;
import edu.iu.terracotta.runner.distribute.model.ExperimentImportScheduleResult;

public class ExperimentImportSchedulerServiceImplTest extends BaseTest {

    @TempDir private Path tempDir;

    private Path root;
    private ExperimentImportSchedulerServiceImpl experimentImportSchedulerService;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        experimentImportSchedulerService = new ExperimentImportSchedulerServiceImpl(experimentImportRepository);

        root = Files.createDirectory(tempDir.resolve("root"));

        ReflectionTestUtils.setField(experimentImportSchedulerService, "experimentExportLocalPathRoot", root.toString());
        ReflectionTestUtils.setField(experimentImportSchedulerService, "ttl", 7);
    }

    @Test
    void testCleanupReturnsEmptyWhenNoExpiredImports() {
        when(experimentImportRepository.findAllByUpdatedAtLessThanOrStatusIn(any(Timestamp.class), anyList())).thenReturn(List.of());

        Optional<ExperimentImportScheduleResult> result = experimentImportSchedulerService.cleanup();

        assertTrue(result.isEmpty());
        verify(experimentImportRepository, never()).save(any(ExperimentImport.class));
    }

    @Test
    void testCleanupFiltersOutAlreadyDeletedImportsInMemory() {
        // matches the repository's status/updatedAt criteria but the extra in-memory
        // isDeleted() filter should exclude it, leaving nothing to process
        ExperimentImport alreadyDeleted = ExperimentImport.builder()
            .fileName("deleted.zip")
            .fileUri("deleted.zip")
            .status(ExperimentImportStatus.DELETED)
            .build();
        alreadyDeleted.setId(1L);

        when(experimentImportRepository.findAllByUpdatedAtLessThanOrStatusIn(any(Timestamp.class), anyList())).thenReturn(List.of(alreadyDeleted));

        Optional<ExperimentImportScheduleResult> result = experimentImportSchedulerService.cleanup();

        assertTrue(result.isEmpty());
        verify(experimentImportRepository, never()).save(any(ExperimentImport.class));
    }

    @Test
    void testCleanupDeletesFileAndSetsDeletedStatus() throws IOException {
        Files.createFile(root.resolve("export.zip"));

        ExperimentImport experimentImport = ExperimentImport.builder()
            .fileName("export.zip")
            .fileUri("export.zip")
            .status(ExperimentImportStatus.COMPLETE_ACKNOWLEDGED)
            .build();
        experimentImport.setId(2L);

        when(experimentImportRepository.findAllByUpdatedAtLessThanOrStatusIn(any(Timestamp.class), anyList())).thenReturn(List.of(experimentImport));

        Optional<ExperimentImportScheduleResult> result = experimentImportSchedulerService.cleanup();

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getProcessed().size());
        assertEquals(ExperimentImportStatus.DELETED, experimentImport.getStatus());
        assertNull(result.get().getProcessed().get(0).getErrors());
        assertFalse(Files.exists(root.resolve("export.zip")));
        verify(experimentImportRepository).save(experimentImport);
    }

    @Test
    void testCleanupSetsDeletionErrorStatusWhenFileMissing() {
        // no file created at root/missing.zip; File.delete() returns false
        ExperimentImport experimentImport = ExperimentImport.builder()
            .fileName("missing.zip")
            .fileUri("missing.zip")
            .status(ExperimentImportStatus.ERROR_ACKNOWLEDGED)
            .build();
        experimentImport.setId(3L);

        when(experimentImportRepository.findAllByUpdatedAtLessThanOrStatusIn(any(Timestamp.class), anyList())).thenReturn(List.of(experimentImport));

        Optional<ExperimentImportScheduleResult> result = experimentImportSchedulerService.cleanup();

        assertTrue(result.isPresent());
        assertEquals(ExperimentImportStatus.DELETION_ERROR, experimentImport.getStatus());
        assertEquals(List.of("Failed to delete file"), result.get().getProcessed().get(0).getErrors());
        verify(experimentImportRepository).save(experimentImport);
    }

    @Test
    void testCleanupDeletesEmptyDirectoriesButKeepsNonEmptyOnes() throws IOException {
        Path emptyDir = Files.createDirectory(root.resolve("empty-dir"));
        Path nonEmptyDir = Files.createDirectory(root.resolve("non-empty-dir"));
        Files.createFile(nonEmptyDir.resolve("keep.txt"));
        Files.createFile(root.resolve("export.zip"));

        ExperimentImport experimentImport = ExperimentImport.builder()
            .fileName("export.zip")
            .fileUri("export.zip")
            .status(ExperimentImportStatus.COMPLETE_ACKNOWLEDGED)
            .build();
        experimentImport.setId(4L);

        when(experimentImportRepository.findAllByUpdatedAtLessThanOrStatusIn(any(Timestamp.class), anyList())).thenReturn(List.of(experimentImport));

        experimentImportSchedulerService.cleanup();

        assertFalse(Files.exists(emptyDir));
        assertTrue(Files.exists(nonEmptyDir));
        assertTrue(Files.exists(nonEmptyDir.resolve("keep.txt")));
    }

}
