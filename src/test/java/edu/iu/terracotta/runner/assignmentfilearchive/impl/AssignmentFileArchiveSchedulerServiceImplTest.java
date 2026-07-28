package edu.iu.terracotta.runner.assignmentfilearchive.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.AssignmentFileArchive;
import edu.iu.terracotta.dao.model.enums.AssignmentFileArchiveStatus;
import edu.iu.terracotta.dao.repository.AssignmentFileArchiveRepository;
import edu.iu.terracotta.runner.assignmentfilearchive.model.AssignmentFileArchiveScheduleMessage;
import edu.iu.terracotta.runner.assignmentfilearchive.model.AssignmentFileArchiveScheduleResult;

public class AssignmentFileArchiveSchedulerServiceImplTest extends BaseTest {

    @Mock private AssignmentFileArchiveRepository assignmentFileArchiveRepository;

    private AssignmentFileArchiveSchedulerServiceImpl assignmentFileArchiveSchedulerService;

    @TempDir private Path archiveRoot;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        assignmentFileArchiveSchedulerService = new AssignmentFileArchiveSchedulerServiceImpl(assignmentFileArchiveRepository);

        ReflectionTestUtils.setField(assignmentFileArchiveSchedulerService, "assignmentFileArchiveLocalPathRoot", archiveRoot.toString());
        ReflectionTestUtils.setField(assignmentFileArchiveSchedulerService, "ttl", 7);

        // keeps archiveRoot from ever being seen as an empty directory by the directory-cleanup phase,
        // isolating each test's directory assertions from that unrelated side effect.
        Files.writeString(archiveRoot.resolve(".keep"), "keep");
    }

    @Test
    public void testCleanupNoExpiredArchivesReturnsEmptyOptional() {
        when(assignmentFileArchiveRepository.findAllByUpdatedAtLessThanAndStatusIn(any(Timestamp.class), anyList())).thenReturn(List.of());

        Optional<AssignmentFileArchiveScheduleResult> result = assignmentFileArchiveSchedulerService.cleanup();

        assertTrue(result.isEmpty());
        verify(assignmentFileArchiveRepository, never()).save(any(AssignmentFileArchive.class));
    }

    @Test
    public void testCleanupDeletesFileSuccessfully() throws Exception {
        AssignmentFileArchive archive = AssignmentFileArchive.builder()
            .fileName("archive.txt")
            .fileUri("archive123")
            .status(AssignmentFileArchiveStatus.READY)
            .build();
        archive.setId(1L);

        Files.writeString(archiveRoot.resolve("archive123" + AssignmentFileArchive.COMPRESSED_FILE_EXTENSION), "content");

        when(assignmentFileArchiveRepository.findAllByUpdatedAtLessThanAndStatusIn(any(Timestamp.class), anyList())).thenReturn(List.of(archive));
        when(assignmentFileArchiveRepository.save(any(AssignmentFileArchive.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<AssignmentFileArchiveScheduleResult> result = assignmentFileArchiveSchedulerService.cleanup();

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getProcessed().size());

        AssignmentFileArchiveScheduleMessage message = result.get().getProcessed().get(0);

        assertEquals(1L, message.getId());
        assertEquals("archive.txt", message.getFileName());
        assertEquals("archive123", message.getFileUri());
        assertNotNull(message.getDeletedAt());
        assertNull(message.getErrors());

        assertEquals(AssignmentFileArchiveStatus.DELETED, archive.getStatus());
        verify(assignmentFileArchiveRepository).save(archive);
        assertFalse(Files.exists(archiveRoot.resolve("archive123.zip")));
    }

    @Test
    public void testCleanupDeleteFailureSetsErrorMessageButStillMarksDeleted() {
        // the underlying file is deliberately never created, so File.delete() naturally returns false
        AssignmentFileArchive archive = AssignmentFileArchive.builder()
            .fileName("missing-archive.txt")
            .fileUri("missing-archive")
            .status(AssignmentFileArchiveStatus.ERROR)
            .build();
        archive.setId(2L);

        when(assignmentFileArchiveRepository.findAllByUpdatedAtLessThanAndStatusIn(any(Timestamp.class), anyList())).thenReturn(List.of(archive));
        when(assignmentFileArchiveRepository.save(any(AssignmentFileArchive.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<AssignmentFileArchiveScheduleResult> result = assignmentFileArchiveSchedulerService.cleanup();

        assertTrue(result.isPresent());

        AssignmentFileArchiveScheduleMessage message = result.get().getProcessed().get(0);

        assertEquals(List.of("Failed to delete file"), message.getErrors());
        assertEquals(AssignmentFileArchiveStatus.DELETED, archive.getStatus());
        verify(assignmentFileArchiveRepository).save(archive);
    }

    @Test
    public void testCleanupRemovesEmptyDirectoriesButKeepsNonEmptyOnes() throws Exception {
        AssignmentFileArchive archive = AssignmentFileArchive.builder()
            .fileName("archive.txt")
            .fileUri("archive456")
            .status(AssignmentFileArchiveStatus.OUTDATED)
            .build();
        archive.setId(3L);

        Files.writeString(archiveRoot.resolve("archive456" + AssignmentFileArchive.COMPRESSED_FILE_EXTENSION), "content");

        Path emptyDir = Files.createDirectory(archiveRoot.resolve("empty-dir"));
        Path nonEmptyDir = Files.createDirectory(archiveRoot.resolve("keep-dir"));
        Files.writeString(nonEmptyDir.resolve("file.txt"), "keep me");

        when(assignmentFileArchiveRepository.findAllByUpdatedAtLessThanAndStatusIn(any(Timestamp.class), anyList())).thenReturn(List.of(archive));
        when(assignmentFileArchiveRepository.save(any(AssignmentFileArchive.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<AssignmentFileArchiveScheduleResult> result = assignmentFileArchiveSchedulerService.cleanup();

        assertTrue(result.isPresent());
        assertFalse(Files.exists(emptyDir));
        assertTrue(Files.exists(nonEmptyDir));
        assertTrue(Files.exists(nonEmptyDir.resolve("file.txt")));
        assertTrue(Files.exists(archiveRoot));
    }

}
