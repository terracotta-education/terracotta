package edu.iu.terracotta.connectors.generic.service.lms.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatch;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchProcessing;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchProcessingRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;

public class LmsUserBatchWriteServiceImplTest {

    @Mock private LmsUserBatchProcessingRepository lmsUserBatchProcessingRepository;
    @Mock private LmsUserBatchRepository lmsUserBatchRepository;

    private LmsUserBatchWriteServiceImpl lmsUserBatchWriteService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        lmsUserBatchWriteService = new LmsUserBatchWriteServiceImpl(lmsUserBatchProcessingRepository, lmsUserBatchRepository);
    }

    @Test
    public void testStartBatchSavesInProgressRecord() {
        UUID batchId = UUID.randomUUID();
        when(lmsUserBatchProcessingRepository.findByBatchId(batchId)).thenReturn(Optional.empty());

        lmsUserBatchWriteService.startBatch(batchId, 123L);

        ArgumentCaptor<LmsUserBatchProcessing> captor = ArgumentCaptor.forClass(LmsUserBatchProcessing.class);
        verify(lmsUserBatchProcessingRepository).save(captor.capture());
        assertEquals(batchId, captor.getValue().getBatchId());
        assertEquals(123L, captor.getValue().getContextId());
        assertEquals(LmsUserBatchStatus.IN_PROGRESS, captor.getValue().getStatus());
    }

    // a caller (e.g. startPrepareParticipation) may have already created the
    // LmsUserBatchProcessing row for this batchId - startBatch must update that row in place
    // rather than inserting a second one for the same logical operation
    @Test
    public void testStartBatchReusesExistingRecordRatherThanCreatingNew() {
        UUID batchId = UUID.randomUUID();
        LmsUserBatchProcessing existing = LmsUserBatchProcessing.builder().batchId(batchId).status(LmsUserBatchStatus.IN_PROGRESS).build();
        when(lmsUserBatchProcessingRepository.findByBatchId(batchId)).thenReturn(Optional.of(existing));

        lmsUserBatchWriteService.startBatch(batchId, 456L);

        verify(lmsUserBatchProcessingRepository).save(existing);
        assertEquals(456L, existing.getContextId());
        assertEquals(LmsUserBatchStatus.IN_PROGRESS, existing.getStatus());
    }

    @Test
    public void testSaveUsersSavesAllWhenNonEmpty() {
        List<LmsUserBatch> users = List.of(LmsUserBatch.builder().build());

        lmsUserBatchWriteService.saveUsers(users);

        verify(lmsUserBatchRepository).saveAll(users);
    }

    @Test
    public void testSaveUsersSkipsRepositoryWhenEmpty() {
        lmsUserBatchWriteService.saveUsers(List.of());

        verify(lmsUserBatchRepository, never()).saveAll(any());
    }

    // markFailed/updateStatus deliberately use a direct UPDATE (updateStatusAndMessage) rather
    // than a read-then-save() of the @Version-checked entity - more than one writer can race to
    // record the same batchId's terminal status (see ParticipantAsyncServiceImpl.
    // prepareParticipationAsync / LmsUserBatchAsyncServiceImpl.handleBatchEvent), and a
    // read-then-save() would let whichever one commits second fail with
    // ObjectOptimisticLockingFailureException at a transaction boundary the caller can't catch.
    @Test
    public void testMarkFailedUpdatesExistingProcessingRecordWithoutReadThenSave() {
        UUID batchId = UUID.randomUUID();
        when(lmsUserBatchProcessingRepository.updateStatusAndMessage(batchId, LmsUserBatchStatus.FAILED, "canvas error")).thenReturn(1);

        lmsUserBatchWriteService.markFailed(batchId, "canvas error");

        verify(lmsUserBatchProcessingRepository).updateStatusAndMessage(batchId, LmsUserBatchStatus.FAILED, "canvas error");
        verify(lmsUserBatchProcessingRepository, never()).save(any(LmsUserBatchProcessing.class));
    }

    @Test
    public void testMarkFailedCreatesRecordWhenNoneExists() {
        UUID batchId = UUID.randomUUID();
        when(lmsUserBatchProcessingRepository.updateStatusAndMessage(batchId, LmsUserBatchStatus.FAILED, "canvas error")).thenReturn(0);

        lmsUserBatchWriteService.markFailed(batchId, "canvas error");

        ArgumentCaptor<LmsUserBatchProcessing> captor = ArgumentCaptor.forClass(LmsUserBatchProcessing.class);
        verify(lmsUserBatchProcessingRepository).save(captor.capture());
        assertEquals(batchId, captor.getValue().getBatchId());
        assertEquals(LmsUserBatchStatus.FAILED, captor.getValue().getStatus());
        assertEquals("canvas error", captor.getValue().getMessage());
    }

    @Test
    public void testUpdateStatusUpdatesExistingRecordWithoutReadThenSave() {
        UUID batchId = UUID.randomUUID();
        when(lmsUserBatchProcessingRepository.updateStatusAndMessage(batchId, LmsUserBatchStatus.COMPLETED, "done")).thenReturn(1);

        lmsUserBatchWriteService.updateStatus(batchId, LmsUserBatchStatus.COMPLETED, "done");

        verify(lmsUserBatchProcessingRepository).updateStatusAndMessage(batchId, LmsUserBatchStatus.COMPLETED, "done");
        verify(lmsUserBatchProcessingRepository, never()).save(any(LmsUserBatchProcessing.class));
    }

    @Test
    public void testUpdateStatusCreatesRecordWhenNoneExists() {
        UUID batchId = UUID.randomUUID();
        when(lmsUserBatchProcessingRepository.updateStatusAndMessage(batchId, LmsUserBatchStatus.COMPLETED, null)).thenReturn(0);

        lmsUserBatchWriteService.updateStatus(batchId, LmsUserBatchStatus.COMPLETED, null);

        ArgumentCaptor<LmsUserBatchProcessing> captor = ArgumentCaptor.forClass(LmsUserBatchProcessing.class);
        verify(lmsUserBatchProcessingRepository).save(captor.capture());
        assertEquals(batchId, captor.getValue().getBatchId());
        assertEquals(LmsUserBatchStatus.COMPLETED, captor.getValue().getStatus());
    }

    // a null message means "leave whatever is already there alone" - e.g. a blank event message
    // must not blow away a previously recorded error message. Enforced by the CASE WHEN in
    // updateStatusAndMessage's @Query itself (untestable with a mocked repository), but the null
    // must at least reach that query unchanged, not get coerced into an empty string or similar.
    @Test
    public void testUpdateStatusPassesNullMessageThroughToQuery() {
        UUID batchId = UUID.randomUUID();
        when(lmsUserBatchProcessingRepository.updateStatusAndMessage(eq(batchId), eq(LmsUserBatchStatus.PROCESSED), isNull())).thenReturn(1);

        lmsUserBatchWriteService.updateStatus(batchId, LmsUserBatchStatus.PROCESSED, null);

        verify(lmsUserBatchProcessingRepository).updateStatusAndMessage(batchId, LmsUserBatchStatus.PROCESSED, null);
    }

}
