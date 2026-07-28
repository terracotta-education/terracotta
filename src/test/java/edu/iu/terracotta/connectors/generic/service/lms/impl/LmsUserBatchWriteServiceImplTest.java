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

        lmsUserBatchWriteService.startBatch(batchId);

        ArgumentCaptor<LmsUserBatchProcessing> captor = ArgumentCaptor.forClass(LmsUserBatchProcessing.class);
        verify(lmsUserBatchProcessingRepository).save(captor.capture());
        assertEquals(batchId, captor.getValue().getBatchId());
        assertEquals(LmsUserBatchStatus.IN_PROGRESS, captor.getValue().getStatus());
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

}
