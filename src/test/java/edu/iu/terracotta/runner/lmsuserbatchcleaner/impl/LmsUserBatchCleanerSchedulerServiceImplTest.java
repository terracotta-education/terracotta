package edu.iu.terracotta.runner.lmsuserbatchcleaner.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchProcessing;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchProcessingRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUserBatchWriteService;
import edu.iu.terracotta.runner.lmsuserbatchcleaner.model.LmsUserBatchCleanerScheduleMessage;
import edu.iu.terracotta.runner.lmsuserbatchcleaner.model.LmsUserBatchCleanerScheduleResult;

public class LmsUserBatchCleanerSchedulerServiceImplTest {

    private static final int STALE_TTL_MINUTES = 60;

    @Mock private LmsUserBatchProcessingRepository lmsUserBatchProcessingRepository;
    @Mock private LmsUserBatchRepository lmsUserBatchRepository;
    @Mock private LmsUserBatchWriteService lmsUserBatchWriteService;

    private LmsUserBatchCleanerSchedulerServiceImpl lmsUserBatchCleanerSchedulerService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        lmsUserBatchCleanerSchedulerService = new LmsUserBatchCleanerSchedulerServiceImpl(
            lmsUserBatchProcessingRepository,
            lmsUserBatchRepository,
            lmsUserBatchWriteService
        );
    }

    @Test
    public void testCleanupNoStaleBatchesFound() {
        when(lmsUserBatchProcessingRepository.findAllByStatusAndUpdatedAtBefore(eq(LmsUserBatchStatus.IN_PROGRESS), any(Timestamp.class))).thenReturn(List.of());

        Optional<LmsUserBatchCleanerScheduleResult> result = lmsUserBatchCleanerSchedulerService.cleanup(STALE_TTL_MINUTES);

        assertTrue(result.isEmpty());
        verify(lmsUserBatchWriteService, never()).markFailed(any(UUID.class), anyString());
    }

    @Test
    public void testCleanupMarksStaleBatchFailedAndDeletesStagedRows() {
        LmsUserBatchProcessing staleBatch = buildStaleBatch(1L, UUID.randomUUID(), 42L);

        when(lmsUserBatchProcessingRepository.findAllByStatusAndUpdatedAtBefore(eq(LmsUserBatchStatus.IN_PROGRESS), any(Timestamp.class))).thenReturn(List.of(staleBatch));
        when(lmsUserBatchRepository.countByBatchId(staleBatch.getBatchId())).thenReturn(5L);

        Optional<LmsUserBatchCleanerScheduleResult> result = lmsUserBatchCleanerSchedulerService.cleanup(STALE_TTL_MINUTES);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getProcessed().size());

        LmsUserBatchCleanerScheduleMessage message = result.get().getProcessed().get(0);

        assertEquals(staleBatch.getId(), message.getId());
        assertEquals(staleBatch.getBatchId(), message.getBatchId());
        assertEquals(staleBatch.getContextId(), message.getContextId());
        assertEquals(5, message.getDeletedStagedRows());
        assertNotNull(message.getCleanedUpAt());
        assertNull(message.getErrors());

        verify(lmsUserBatchRepository).deleteByBatchId(staleBatch.getBatchId());
        verify(lmsUserBatchWriteService).markFailed(eq(staleBatch.getBatchId()), anyString());
    }

    @Test
    public void testCleanupCapturesErrorWhenMarkFailedThrows() {
        LmsUserBatchProcessing staleBatch = buildStaleBatch(1L, UUID.randomUUID(), 42L);
        String errorMessage = "update failed";

        when(lmsUserBatchProcessingRepository.findAllByStatusAndUpdatedAtBefore(eq(LmsUserBatchStatus.IN_PROGRESS), any(Timestamp.class))).thenReturn(List.of(staleBatch));
        doThrow(new RuntimeException(errorMessage)).when(lmsUserBatchWriteService).markFailed(any(UUID.class), anyString());

        Optional<LmsUserBatchCleanerScheduleResult> result = lmsUserBatchCleanerSchedulerService.cleanup(STALE_TTL_MINUTES);

        assertTrue(result.isPresent());
        assertEquals(List.of(errorMessage), result.get().getProcessed().get(0).getErrors());
    }

    @Test
    public void testCleanupProcessesMultipleStaleBatchesIndependently() {
        LmsUserBatchProcessing succeeds = buildStaleBatch(1L, UUID.randomUUID(), 42L);
        LmsUserBatchProcessing fails = buildStaleBatch(2L, UUID.randomUUID(), 43L);
        String errorMessage = "update failed";

        when(lmsUserBatchProcessingRepository.findAllByStatusAndUpdatedAtBefore(eq(LmsUserBatchStatus.IN_PROGRESS), any(Timestamp.class))).thenReturn(List.of(succeeds, fails));
        doThrow(new RuntimeException(errorMessage)).when(lmsUserBatchWriteService).markFailed(eq(fails.getBatchId()), anyString());

        Optional<LmsUserBatchCleanerScheduleResult> result = lmsUserBatchCleanerSchedulerService.cleanup(STALE_TTL_MINUTES);

        assertTrue(result.isPresent());
        assertEquals(2, result.get().getProcessed().size());

        assertNull(result.get().getProcessed().get(0).getErrors());
        assertEquals(List.of(errorMessage), result.get().getProcessed().get(1).getErrors());

        verify(lmsUserBatchWriteService).markFailed(eq(succeeds.getBatchId()), anyString());
        verify(lmsUserBatchWriteService).markFailed(eq(fails.getBatchId()), anyString());
    }

    private LmsUserBatchProcessing buildStaleBatch(long id, UUID batchId, long contextId) {
        LmsUserBatchProcessing lmsUserBatchProcessing = LmsUserBatchProcessing.builder()
            .id(id)
            .batchId(batchId)
            .contextId(contextId)
            .status(LmsUserBatchStatus.IN_PROGRESS)
            .build();

        Timestamp staleTimestamp = Timestamp.from(Instant.now().minus(2, ChronoUnit.HOURS));
        lmsUserBatchProcessing.setCreatedAt(staleTimestamp);
        lmsUserBatchProcessing.setUpdatedAt(staleTimestamp);

        return lmsUserBatchProcessing;
    }

}
