package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.AssignmentFileArchive;
import edu.iu.terracotta.dao.model.dto.AssignmentFileArchiveDto;
import edu.iu.terracotta.dao.model.enums.AssignmentFileArchiveStatus;
import edu.iu.terracotta.dao.repository.AssignmentFileArchiveRepository;
import edu.iu.terracotta.exceptions.AssignmentFileArchiveNotFoundException;
import edu.iu.terracotta.service.app.async.AssignmentAsyncService;

public class AssignmentFileArchiveServiceImplTest extends BaseTest {

    @Mock private AssignmentFileArchiveRepository assignmentFileArchiveRepository;
    @Mock private AssignmentAsyncService asyncService;

    @InjectMocks private AssignmentFileArchiveServiceImpl assignmentFileArchiveService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    private AssignmentFileArchive buildArchive(AssignmentFileArchiveStatus status, Timestamp createdAt) {
        AssignmentFileArchive archive = AssignmentFileArchive.builder()
            .assignment(assignment)
            .owner(ltiUserEntity)
            .status(status)
            .build();
        archive.setUuid(UUID.randomUUID());
        archive.setId(1L);
        archive.setCreatedAt(createdAt);

        return archive;
    }

    @Test
    public void testProcessSuccess() throws Exception {
        AssignmentFileArchive saved = buildArchive(AssignmentFileArchiveStatus.PROCESSING, new Timestamp(System.currentTimeMillis()));
        when(assignmentFileArchiveRepository.save(any(AssignmentFileArchive.class))).thenReturn(saved);

        AssignmentFileArchiveDto dto = assignmentFileArchiveService.process(assignment, securedInfo);

        assertNotNull(dto);
        assertEquals(AssignmentFileArchiveStatus.PROCESSING, dto.getStatus());
        assertNull(dto.getFile());
        verify(asyncService).processAssignmentFileArchive(saved);
    }

    // findOutdated: the components page's "new submissions" banners

    @Test
    public void testFindOutdatedListsAndMarksArchivesWithNewerUploads() throws Exception {
        AssignmentFileArchive archive = buildArchive(AssignmentFileArchiveStatus.DOWNLOADED, new Timestamp(0));
        when(assignmentRepository.findByExposure_Experiment_ExperimentId(9L)).thenReturn(List.of(assignment));
        when(assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.of(archive));
        when(answerFileSubmissionRepository.findLatestUploadTimeByAssignmentId(anyLong())).thenReturn(Optional.of(new Timestamp(System.currentTimeMillis())));

        List<AssignmentFileArchiveDto> outdated = assignmentFileArchiveService.findOutdated(9L);

        assertEquals(1, outdated.size());
        assertEquals(AssignmentFileArchiveStatus.OUTDATED, outdated.get(0).getStatus());
        assertEquals(archive.getUuid(), outdated.get(0).getId());
        assertEquals(exposure.getUuid(), outdated.get(0).getExposureId());
        verify(assignmentFileArchiveRepository).save(archive);
    }

    @Test
    public void testFindOutdatedDoesNotResaveAnArchiveAlreadyMarkedOutdated() throws Exception {
        AssignmentFileArchive archive = buildArchive(AssignmentFileArchiveStatus.OUTDATED, new Timestamp(0));
        when(assignmentRepository.findByExposure_Experiment_ExperimentId(9L)).thenReturn(List.of(assignment));
        when(assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.of(archive));
        when(answerFileSubmissionRepository.findLatestUploadTimeByAssignmentId(anyLong())).thenReturn(Optional.of(new Timestamp(System.currentTimeMillis())));

        assertEquals(1, assignmentFileArchiveService.findOutdated(9L).size());
        verify(assignmentFileArchiveRepository, never()).save(any(AssignmentFileArchive.class));
    }

    @Test
    public void testFindOutdatedSkipsCurrentDismissedBuildingAndMissingArchives() throws Exception {
        Assignment current = mock(Assignment.class);
        Assignment dismissed = mock(Assignment.class);
        Assignment building = mock(Assignment.class);
        Assignment noArchive = mock(Assignment.class);
        when(current.getAssignmentId()).thenReturn(11L);
        when(dismissed.getAssignmentId()).thenReturn(12L);
        when(building.getAssignmentId()).thenReturn(13L);
        when(noArchive.getAssignmentId()).thenReturn(14L);
        when(assignmentRepository.findByExposure_Experiment_ExperimentId(9L)).thenReturn(List.of(current, dismissed, building, noArchive));

        Timestamp lastUpload = new Timestamp(1_000_000L);
        AssignmentFileArchive currentArchive = archiveFor(current, AssignmentFileArchiveStatus.DOWNLOADED, new Timestamp(2_000_000L));
        // dismissed after the last upload
        AssignmentFileArchive dismissedArchive = archiveFor(dismissed, AssignmentFileArchiveStatus.OUTDATED_ACKNOWLEDGED, new Timestamp(0));
        dismissedArchive.setUpdatedAt(new Timestamp(2_000_000L));
        AssignmentFileArchive buildingArchive = archiveFor(building, AssignmentFileArchiveStatus.PROCESSING, new Timestamp(0));

        when(assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdOrderByCreatedAtDesc(11L)).thenReturn(Optional.of(currentArchive));
        when(assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdOrderByCreatedAtDesc(12L)).thenReturn(Optional.of(dismissedArchive));
        when(assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdOrderByCreatedAtDesc(13L)).thenReturn(Optional.of(buildingArchive));
        when(assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdOrderByCreatedAtDesc(14L)).thenReturn(Optional.empty());
        when(answerFileSubmissionRepository.findLatestUploadTimeByAssignmentId(anyLong())).thenReturn(Optional.of(lastUpload));

        assertTrue(assignmentFileArchiveService.findOutdated(9L).isEmpty());
        verify(assignmentFileArchiveRepository, never()).save(any(AssignmentFileArchive.class));
    }

    private AssignmentFileArchive archiveFor(Assignment forAssignment, AssignmentFileArchiveStatus status, Timestamp createdAt) {
        AssignmentFileArchive archive = buildArchive(status, createdAt);
        archive.setAssignment(forAssignment);

        return archive;
    }

    @Test
    public void testPollNotFoundThrows() {
        when(assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.empty());

        assertThrows(AssignmentFileArchiveNotFoundException.class, () -> assignmentFileArchiveService.poll(assignment, securedInfo, false));
    }

    @Test
    public void testPollArchiveCurrentReturnsExisting() throws Exception {
        AssignmentFileArchive archive = buildArchive(AssignmentFileArchiveStatus.READY, new Timestamp(System.currentTimeMillis()));
        when(assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.of(archive));
        when(answerFileSubmissionRepository.findLatestUploadTimeByAssignmentId(anyLong())).thenReturn(Optional.empty());

        AssignmentFileArchiveDto dto = assignmentFileArchiveService.poll(assignment, securedInfo, false);

        assertEquals(AssignmentFileArchiveStatus.READY, dto.getStatus());
        verify(assignmentFileArchiveRepository, never()).save(any(AssignmentFileArchive.class));
    }

    @Test
    public void testPollOutdatedNoCreateNewReturnsOutdated() throws Exception {
        AssignmentFileArchive archive = buildArchive(AssignmentFileArchiveStatus.READY, new Timestamp(0));
        when(assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.of(archive));
        when(answerFileSubmissionRepository.findLatestUploadTimeByAssignmentId(anyLong())).thenReturn(Optional.of(new Timestamp(System.currentTimeMillis())));
        when(assignmentFileArchiveRepository.save(any(AssignmentFileArchive.class))).thenReturn(archive);

        AssignmentFileArchiveDto dto = assignmentFileArchiveService.poll(assignment, securedInfo, false);

        assertEquals(AssignmentFileArchiveStatus.OUTDATED, dto.getStatus());
        verify(assignmentFileArchiveRepository).save(archive);
        verify(asyncService, never()).processAssignmentFileArchive(any(AssignmentFileArchive.class));
    }

    @Test
    public void testPollOutdatedCreateNewProcessesReplacement() throws Exception {
        AssignmentFileArchive archive = buildArchive(AssignmentFileArchiveStatus.READY, new Timestamp(0));
        AssignmentFileArchive reprocessed = buildArchive(AssignmentFileArchiveStatus.REPROCESSING, new Timestamp(System.currentTimeMillis()));
        when(assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.of(archive));
        when(answerFileSubmissionRepository.findLatestUploadTimeByAssignmentId(anyLong())).thenReturn(Optional.of(new Timestamp(System.currentTimeMillis())));
        when(assignmentFileArchiveRepository.save(any(AssignmentFileArchive.class))).thenReturn(archive, reprocessed);

        AssignmentFileArchiveDto dto = assignmentFileArchiveService.poll(assignment, securedInfo, true);

        assertEquals(AssignmentFileArchiveStatus.REPROCESSING, dto.getStatus());
        verify(asyncService).processAssignmentFileArchive(reprocessed);
    }

    // the archive is current as long as no upload is newer than it, even if older uploads exist
    @Test
    public void testPollCurrentWhenAllUploadsPredateTheArchive() throws Exception {
        long now = System.currentTimeMillis();
        AssignmentFileArchive archive = buildArchive(AssignmentFileArchiveStatus.DOWNLOADED, new Timestamp(now));
        when(assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.of(archive));
        when(answerFileSubmissionRepository.findLatestUploadTimeByAssignmentId(anyLong())).thenReturn(Optional.of(new Timestamp(now - 60_000)));

        AssignmentFileArchiveDto dto = assignmentFileArchiveService.poll(assignment, securedInfo, false);

        assertEquals(AssignmentFileArchiveStatus.DOWNLOADED, dto.getStatus());
        verify(assignmentFileArchiveRepository, never()).save(any(AssignmentFileArchive.class));
    }

    // still being built - new uploads don't make it "outdated", and marking it would be overwritten
    // when the build finishes
    @Test
    public void testPollLeavesAnArchiveThatsStillProcessingAlone() throws Exception {
        AssignmentFileArchive archive = buildArchive(AssignmentFileArchiveStatus.PROCESSING, new Timestamp(0));
        when(assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.of(archive));
        when(answerFileSubmissionRepository.findLatestUploadTimeByAssignmentId(anyLong())).thenReturn(Optional.of(new Timestamp(System.currentTimeMillis())));

        AssignmentFileArchiveDto dto = assignmentFileArchiveService.poll(assignment, securedInfo, false);

        assertEquals(AssignmentFileArchiveStatus.PROCESSING, dto.getStatus());
        verify(assignmentFileArchiveRepository, never()).save(any(AssignmentFileArchive.class));
    }

    // dismissed "new uploads" indicator stays dismissed while nothing newer has been uploaded
    @Test
    public void testPollKeepsADismissedIndicatorDismissedWithoutNewerUploads() throws Exception {
        long now = System.currentTimeMillis();
        AssignmentFileArchive archive = buildArchive(AssignmentFileArchiveStatus.OUTDATED_ACKNOWLEDGED, new Timestamp(now - 120_000));
        archive.setUpdatedAt(new Timestamp(now - 30_000));
        when(assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.of(archive));
        // newer than the archive, but older than the dismissal
        when(answerFileSubmissionRepository.findLatestUploadTimeByAssignmentId(anyLong())).thenReturn(Optional.of(new Timestamp(now - 60_000)));

        AssignmentFileArchiveDto dto = assignmentFileArchiveService.poll(assignment, securedInfo, false);

        assertEquals(AssignmentFileArchiveStatus.OUTDATED_ACKNOWLEDGED, dto.getStatus());
        verify(assignmentFileArchiveRepository, never()).save(any(AssignmentFileArchive.class));
    }

    // ...but an upload after the dismissal raises it again
    @Test
    public void testPollRaisesADismissedIndicatorAgainForNewerUploads() throws Exception {
        long now = System.currentTimeMillis();
        AssignmentFileArchive archive = buildArchive(AssignmentFileArchiveStatus.OUTDATED_ACKNOWLEDGED, new Timestamp(now - 120_000));
        archive.setUpdatedAt(new Timestamp(now - 30_000));
        when(assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.of(archive));
        when(answerFileSubmissionRepository.findLatestUploadTimeByAssignmentId(anyLong())).thenReturn(Optional.of(new Timestamp(now)));
        when(assignmentFileArchiveRepository.save(any(AssignmentFileArchive.class))).thenReturn(archive);

        AssignmentFileArchiveDto dto = assignmentFileArchiveService.poll(assignment, securedInfo, false);

        assertEquals(AssignmentFileArchiveStatus.OUTDATED, dto.getStatus());
        verify(assignmentFileArchiveRepository).save(archive);
    }

    @Test
    public void testOutdatedAcknowledgeDismissesTheIndicator() throws Exception {
        AssignmentFileArchive archive = buildArchive(AssignmentFileArchiveStatus.OUTDATED, new Timestamp(0));
        when(assignmentFileArchiveRepository.findByUuidAndAssignment_AssignmentId(any(UUID.class), anyLong())).thenReturn(Optional.of(archive));

        assignmentFileArchiveService.outdatedAcknowledge(archive.getUuid(), assignment);

        assertEquals(AssignmentFileArchiveStatus.OUTDATED_ACKNOWLEDGED, archive.getStatus());
        verify(assignmentFileArchiveRepository).save(archive);
    }

    @Test
    public void testOutdatedAcknowledgeLeavesAnArchiveThatIsntOutdatedAlone() throws Exception {
        AssignmentFileArchive archive = buildArchive(AssignmentFileArchiveStatus.READY, new Timestamp(0));
        when(assignmentFileArchiveRepository.findByUuidAndAssignment_AssignmentId(any(UUID.class), anyLong())).thenReturn(Optional.of(archive));

        assignmentFileArchiveService.outdatedAcknowledge(archive.getUuid(), assignment);

        assertEquals(AssignmentFileArchiveStatus.READY, archive.getStatus());
        verify(assignmentFileArchiveRepository, never()).save(any(AssignmentFileArchive.class));
    }

    @Test
    public void testOutdatedAcknowledgeNotFoundThrows() {
        when(assignmentFileArchiveRepository.findByUuidAndAssignment_AssignmentId(any(UUID.class), anyLong())).thenReturn(Optional.empty());

        assertThrows(AssignmentFileArchiveNotFoundException.class, () -> assignmentFileArchiveService.outdatedAcknowledge(UUID.randomUUID(), assignment));
    }

    @Test
    public void testRetrieveExistingCurrentArchiveReturnsIt() throws Exception {
        AssignmentFileArchive archive = buildArchive(AssignmentFileArchiveStatus.READY, new Timestamp(System.currentTimeMillis()));
        when(assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdAndStatusInOrderByCreatedAtDesc(anyLong(), any())).thenReturn(Optional.of(archive));
        when(answerFileSubmissionRepository.findLatestUploadTimeByAssignmentId(anyLong())).thenReturn(Optional.empty());
        when(assignmentFileArchiveRepository.save(any(AssignmentFileArchive.class))).thenReturn(archive);
        when(fileStorageService.getAssignmentFileArchive(anyLong())).thenReturn(file);

        AssignmentFileArchiveDto dto = assignmentFileArchiveService.retrieve(archive.getUuid(), assignment, securedInfo);

        assertEquals(AssignmentFileArchiveStatus.DOWNLOADED, dto.getStatus());
        assertNotNull(dto.getFile());
        verify(assignmentFileArchiveRepository).save(archive);
        verify(asyncService, never()).processAssignmentFileArchive(any(AssignmentFileArchive.class));
    }

    @Test
    public void testRetrieveNoAvailableArchiveProcessesNew() throws Exception {
        when(assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdAndStatusInOrderByCreatedAtDesc(anyLong(), any())).thenReturn(Optional.empty());
        AssignmentFileArchive processed = buildArchive(AssignmentFileArchiveStatus.PROCESSING, new Timestamp(System.currentTimeMillis()));
        when(assignmentFileArchiveRepository.save(any(AssignmentFileArchive.class))).thenReturn(processed);

        AssignmentFileArchiveDto dto = assignmentFileArchiveService.retrieve(UUID.randomUUID(), assignment, securedInfo);

        assertEquals(AssignmentFileArchiveStatus.PROCESSING, dto.getStatus());
        verify(asyncService).processAssignmentFileArchive(processed);
    }

    @Test
    public void testFindLatestAvailableArchiveEmptyWhenNoneFound() throws Exception {
        when(assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdAndStatusInOrderByCreatedAtDesc(anyLong(), any())).thenReturn(Optional.empty());

        assertTrue(assignmentFileArchiveService.findLatestAvailableArchive(1L).isEmpty());
    }

    @Test
    public void testFindLatestAvailableArchiveEmptyWhenOutdated() throws Exception {
        AssignmentFileArchive archive = buildArchive(AssignmentFileArchiveStatus.READY, new Timestamp(0));
        when(assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdAndStatusInOrderByCreatedAtDesc(anyLong(), any())).thenReturn(Optional.of(archive));
        when(answerFileSubmissionRepository.findLatestUploadTimeByAssignmentId(anyLong())).thenReturn(Optional.of(new Timestamp(System.currentTimeMillis())));

        assertTrue(assignmentFileArchiveService.findLatestAvailableArchive(1L).isEmpty());
    }

    @Test
    public void testFindLatestAvailableArchivePresentWhenCurrent() throws Exception {
        AssignmentFileArchive archive = buildArchive(AssignmentFileArchiveStatus.READY, new Timestamp(System.currentTimeMillis()));
        when(assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdAndStatusInOrderByCreatedAtDesc(anyLong(), any())).thenReturn(Optional.of(archive));
        when(answerFileSubmissionRepository.findLatestUploadTimeByAssignmentId(anyLong())).thenReturn(Optional.empty());

        Optional<AssignmentFileArchive> retVal = assignmentFileArchiveService.findLatestAvailableArchive(1L);

        assertTrue(retVal.isPresent());
        assertEquals(archive, retVal.get());
    }

    @Test
    public void testErrorAcknowledgeSuccess() throws Exception {
        AssignmentFileArchive archive = buildArchive(AssignmentFileArchiveStatus.ERROR, new Timestamp(System.currentTimeMillis()));
        when(assignmentFileArchiveRepository.findByUuidAndAssignment_AssignmentId(any(UUID.class), anyLong())).thenReturn(Optional.of(archive));

        assignmentFileArchiveService.errorAcknowledge(archive.getUuid(), assignment);

        assertEquals(AssignmentFileArchiveStatus.ERROR_ACKNOWLEDGED, archive.getStatus());
        verify(assignmentFileArchiveRepository).save(archive);
    }

    @Test
    public void testErrorAcknowledgeNotFoundThrows() {
        when(assignmentFileArchiveRepository.findByUuidAndAssignment_AssignmentId(any(UUID.class), anyLong())).thenReturn(Optional.empty());

        assertThrows(AssignmentFileArchiveNotFoundException.class, () -> assignmentFileArchiveService.errorAcknowledge(UUID.randomUUID(), assignment));
    }

    @Test
    public void testToDtoIncludeFileContentAppendsExtensionAndFile() throws Exception {
        AssignmentFileArchive archive = buildArchive(AssignmentFileArchiveStatus.READY, new Timestamp(System.currentTimeMillis()));
        archive.setFileName("my-archive");
        archive.setMimeType(AssignmentFileArchive.MIME_TYPE);
        when(fileStorageService.getAssignmentFileArchive(anyLong())).thenReturn(file);

        AssignmentFileArchiveDto dto = assignmentFileArchiveService.toDto(archive, true);

        assertEquals("my-archive" + AssignmentFileArchive.COMPRESSED_FILE_EXTENSION, dto.getFileName());
        assertEquals(AssignmentFileArchive.MIME_TYPE, dto.getMimeType());
        assertEquals(file, dto.getFile());
    }

    @Test
    public void testToDtoExcludeFileContentAndBlankFileName() throws Exception {
        AssignmentFileArchive archive = buildArchive(AssignmentFileArchiveStatus.READY, new Timestamp(System.currentTimeMillis()));

        AssignmentFileArchiveDto dto = assignmentFileArchiveService.toDto(archive, false);

        assertNull(dto.getFileName());
        assertNull(dto.getFile());
    }

}
