package edu.iu.terracotta.service.app.async.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchProcessing;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;
import edu.iu.terracotta.connectors.generic.dao.model.event.LmsUserBatchEvent;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchProcessingRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;

public class LmsUserBatchAsyncServiceImplTest {

    @Mock private LmsUserBatchProcessingRepository lmsUserBatchProcessingRepository;
    @Mock private LmsUserBatchRepository lmsUserBatchRepository;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    private LmsUserBatchAsyncServiceImpl lmsUserBatchAsyncService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        lmsUserBatchAsyncService = new LmsUserBatchAsyncServiceImpl(
            lmsUserBatchProcessingRepository,
            lmsUserBatchRepository,
            applicationEventPublisher
        );
    }

    // handleBatchEvent must fire on AFTER_COMPLETION, not AFTER_COMMIT: an AFTER_COMMIT-only
    // listener is simply never invoked when the transaction it was registered against rolls back
    // instead of commits, which would silently drop a fail() published right before a rollback.
    @Test
    public void testHandleBatchEventListensOnAfterCompletionNotAfterCommitOnly() throws NoSuchMethodException {
        Method handleBatchEvent = LmsUserBatchAsyncServiceImpl.class.getMethod("handleBatchEvent", LmsUserBatchEvent.class);
        TransactionalEventListener annotation = handleBatchEvent.getAnnotation(TransactionalEventListener.class);

        assertEquals(TransactionPhase.AFTER_COMPLETION, annotation.phase());
    }

    @Test
    public void testSuccessPublishesCompletedEvent() {
        UUID batchId = UUID.randomUUID();

        lmsUserBatchAsyncService.success(batchId);

        ArgumentCaptor<LmsUserBatchEvent> captor = ArgumentCaptor.forClass(LmsUserBatchEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertEquals(batchId, captor.getValue().batchId());
        assertEquals(LmsUserBatchStatus.COMPLETED, captor.getValue().status());
        assertNull(captor.getValue().message());
    }

    @Test
    public void testProcessedPublishesProcessedEventWithMessage() {
        UUID batchId = UUID.randomUUID();

        lmsUserBatchAsyncService.processed(batchId, "no users to update");

        ArgumentCaptor<LmsUserBatchEvent> captor = ArgumentCaptor.forClass(LmsUserBatchEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertEquals(batchId, captor.getValue().batchId());
        assertEquals(LmsUserBatchStatus.PROCESSED, captor.getValue().status());
        assertEquals("no users to update", captor.getValue().message());
    }

    @Test
    public void testFailPublishesFailedEventWithMessage() {
        UUID batchId = UUID.randomUUID();

        lmsUserBatchAsyncService.fail(batchId, "LMS error");

        ArgumentCaptor<LmsUserBatchEvent> captor = ArgumentCaptor.forClass(LmsUserBatchEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertEquals(batchId, captor.getValue().batchId());
        assertEquals(LmsUserBatchStatus.FAILED, captor.getValue().status());
        assertEquals("LMS error", captor.getValue().message());
    }

    @Test
    public void testHandleBatchEventCreatesNewProcessingRecordWhenNoneExists() {
        UUID batchId = UUID.randomUUID();
        LmsUserBatchEvent event = LmsUserBatchEvent.builder().batchId(batchId).status(LmsUserBatchStatus.COMPLETED).build();
        when(lmsUserBatchProcessingRepository.findByBatchId(batchId)).thenReturn(Optional.empty());
        when(lmsUserBatchProcessingRepository.save(any(LmsUserBatchProcessing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        lmsUserBatchAsyncService.handleBatchEvent(event);

        verify(lmsUserBatchRepository).deleteByBatchId(batchId);
        ArgumentCaptor<LmsUserBatchProcessing> captor = ArgumentCaptor.forClass(LmsUserBatchProcessing.class);
        verify(lmsUserBatchProcessingRepository).save(captor.capture());
        assertEquals(LmsUserBatchStatus.COMPLETED, captor.getValue().getStatus());
        assertNull(captor.getValue().getMessage());
    }

    @Test
    public void testHandleBatchEventUpdatesExistingProcessingRecord() {
        UUID batchId = UUID.randomUUID();
        LmsUserBatchEvent event = LmsUserBatchEvent.builder().batchId(batchId).status(LmsUserBatchStatus.FAILED).message("LMS error").build();
        LmsUserBatchProcessing existing = LmsUserBatchProcessing.builder().batchId(batchId).status(LmsUserBatchStatus.IN_PROGRESS).build();
        when(lmsUserBatchProcessingRepository.findByBatchId(batchId)).thenReturn(Optional.of(existing));
        when(lmsUserBatchProcessingRepository.save(any(LmsUserBatchProcessing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        lmsUserBatchAsyncService.handleBatchEvent(event);

        verify(lmsUserBatchRepository).deleteByBatchId(eq(batchId));
        verify(lmsUserBatchProcessingRepository).save(existing);
        assertEquals(LmsUserBatchStatus.FAILED, existing.getStatus());
        assertEquals("LMS error", existing.getMessage());
    }

    @Test
    public void testHandleBatchEventDoesNotOverwriteMessageWhenBlank() {
        UUID batchId = UUID.randomUUID();
        LmsUserBatchEvent event = LmsUserBatchEvent.builder().batchId(batchId).status(LmsUserBatchStatus.PROCESSED).message(" ").build();
        LmsUserBatchProcessing existing = LmsUserBatchProcessing.builder().batchId(batchId).status(LmsUserBatchStatus.IN_PROGRESS).message("original").build();
        when(lmsUserBatchProcessingRepository.findByBatchId(batchId)).thenReturn(Optional.of(existing));
        when(lmsUserBatchProcessingRepository.save(any(LmsUserBatchProcessing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        lmsUserBatchAsyncService.handleBatchEvent(event);

        assertEquals("original", existing.getMessage());
    }

    @Test
    public void testHandleBatchEventDeletesBeforeUpdatingStatus() {
        UUID batchId = UUID.randomUUID();
        LmsUserBatchEvent event = LmsUserBatchEvent.builder().batchId(batchId).status(LmsUserBatchStatus.COMPLETED).build();
        when(lmsUserBatchProcessingRepository.findByBatchId(batchId)).thenReturn(Optional.empty());
        when(lmsUserBatchProcessingRepository.save(any(LmsUserBatchProcessing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        lmsUserBatchAsyncService.handleBatchEvent(event);

        verify(lmsUserBatchRepository, times(1)).deleteByBatchId(batchId);
        verify(lmsUserBatchProcessingRepository, times(1)).findByBatchId(batchId);
    }

}
