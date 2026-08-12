package edu.iu.terracotta.connectors.generic.service.lms.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    public void testMarkFailedUpdatesExistingProcessingRecord() {
        UUID batchId = UUID.randomUUID();
        LmsUserBatchProcessing existing = LmsUserBatchProcessing.builder().batchId(batchId).status(LmsUserBatchStatus.IN_PROGRESS).build();
        when(lmsUserBatchProcessingRepository.findByBatchId(batchId)).thenReturn(Optional.of(existing));

        lmsUserBatchWriteService.markFailed(batchId, "canvas error");

        verify(lmsUserBatchProcessingRepository).save(existing);
        assertEquals(LmsUserBatchStatus.FAILED, existing.getStatus());
        assertEquals("canvas error", existing.getMessage());
    }

    @Test
    public void testMarkFailedCreatesRecordWhenNoneExists() {
        UUID batchId = UUID.randomUUID();
        when(lmsUserBatchProcessingRepository.findByBatchId(batchId)).thenReturn(Optional.empty());

        lmsUserBatchWriteService.markFailed(batchId, "canvas error");

        ArgumentCaptor<LmsUserBatchProcessing> captor = ArgumentCaptor.forClass(LmsUserBatchProcessing.class);
        verify(lmsUserBatchProcessingRepository).save(captor.capture());
        assertEquals(batchId, captor.getValue().getBatchId());
        assertEquals(LmsUserBatchStatus.FAILED, captor.getValue().getStatus());
        assertEquals("canvas error", captor.getValue().getMessage());
    }

    @Test
    public void testUpdateStatusSetsStatusAndMessageOnExistingRecord() {
        UUID batchId = UUID.randomUUID();
        LmsUserBatchProcessing existing = LmsUserBatchProcessing.builder().batchId(batchId).status(LmsUserBatchStatus.IN_PROGRESS).build();
        when(lmsUserBatchProcessingRepository.findByBatchId(batchId)).thenReturn(Optional.of(existing));

        lmsUserBatchWriteService.updateStatus(batchId, LmsUserBatchStatus.COMPLETED, "done");

        verify(lmsUserBatchProcessingRepository).save(existing);
        assertEquals(LmsUserBatchStatus.COMPLETED, existing.getStatus());
        assertEquals("done", existing.getMessage());
    }

    @Test
    public void testUpdateStatusCreatesRecordWhenNoneExists() {
        UUID batchId = UUID.randomUUID();
        when(lmsUserBatchProcessingRepository.findByBatchId(batchId)).thenReturn(Optional.empty());

        lmsUserBatchWriteService.updateStatus(batchId, LmsUserBatchStatus.COMPLETED, null);

        ArgumentCaptor<LmsUserBatchProcessing> captor = ArgumentCaptor.forClass(LmsUserBatchProcessing.class);
        verify(lmsUserBatchProcessingRepository).save(captor.capture());
        assertEquals(batchId, captor.getValue().getBatchId());
        assertEquals(LmsUserBatchStatus.COMPLETED, captor.getValue().getStatus());
    }

    // a null message means "leave whatever is already there alone" - e.g. a blank event message
    // must not blow away a previously recorded error message (see
    // LmsUserBatchAsyncServiceImpl.handleBatchEvent, which normalizes blank to null before
    // calling this)
    @Test
    public void testUpdateStatusPreservesExistingMessageWhenMessageArgIsNull() {
        UUID batchId = UUID.randomUUID();
        LmsUserBatchProcessing existing = LmsUserBatchProcessing.builder().batchId(batchId).status(LmsUserBatchStatus.IN_PROGRESS).message("original").build();
        when(lmsUserBatchProcessingRepository.findByBatchId(batchId)).thenReturn(Optional.of(existing));

        lmsUserBatchWriteService.updateStatus(batchId, LmsUserBatchStatus.PROCESSED, null);

        assertEquals(LmsUserBatchStatus.PROCESSED, existing.getStatus());
        assertEquals("original", existing.getMessage());
    }

}
