package edu.iu.terracotta.service.app.async.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;
import edu.iu.terracotta.connectors.generic.dao.model.event.LmsUserBatchEvent;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUserBatchWriteService;

public class LmsUserBatchAsyncServiceImplTest {

    @Mock private LmsUserBatchWriteService lmsUserBatchWriteService;
    @Mock private LmsUserBatchRepository lmsUserBatchRepository;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    private LmsUserBatchAsyncServiceImpl lmsUserBatchAsyncService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        lmsUserBatchAsyncService = new LmsUserBatchAsyncServiceImpl(
            lmsUserBatchWriteService,
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

    // success() never sets a message of its own - handleBatchEvent fills one in reporting how
    // many users this sync actually retrieved, counted before the staging rows are deleted
    @Test
    public void testHandleBatchEventSetsRetrievedUserCountMessageOnCompleted() {
        UUID batchId = UUID.randomUUID();
        LmsUserBatchEvent event = LmsUserBatchEvent.builder().batchId(batchId).status(LmsUserBatchStatus.COMPLETED).build();
        when(lmsUserBatchRepository.countByBatchId(batchId)).thenReturn(42L);

        lmsUserBatchAsyncService.handleBatchEvent(event);

        InOrder inOrder = inOrder(lmsUserBatchRepository, lmsUserBatchWriteService);
        inOrder.verify(lmsUserBatchRepository).countByBatchId(batchId);
        inOrder.verify(lmsUserBatchRepository).deleteByBatchId(batchId);
        inOrder.verify(lmsUserBatchWriteService).updateStatus(batchId, LmsUserBatchStatus.COMPLETED, "Retrieved 42 users from LMS");
    }

    @Test
    public void testHandleBatchEventPassesThroughNonBlankMessage() {
        UUID batchId = UUID.randomUUID();
        LmsUserBatchEvent event = LmsUserBatchEvent.builder().batchId(batchId).status(LmsUserBatchStatus.FAILED).message("LMS error").build();

        lmsUserBatchAsyncService.handleBatchEvent(event);

        verify(lmsUserBatchRepository).deleteByBatchId(eq(batchId));
        verify(lmsUserBatchWriteService).updateStatus(batchId, LmsUserBatchStatus.FAILED, "LMS error");
    }

    // a blank event message must not blow away a previously recorded error message -
    // LmsUserBatchWriteServiceImpl.updateStatus treats a null message as "leave it alone", so a
    // blank one must be normalized to null before calling it, not passed through as-is
    @Test
    public void testHandleBatchEventNormalizesBlankMessageToNull() {
        UUID batchId = UUID.randomUUID();
        LmsUserBatchEvent event = LmsUserBatchEvent.builder().batchId(batchId).status(LmsUserBatchStatus.PROCESSED).message(" ").build();

        lmsUserBatchAsyncService.handleBatchEvent(event);

        verify(lmsUserBatchWriteService).updateStatus(eq(batchId), eq(LmsUserBatchStatus.PROCESSED), isNull());
    }

    @Test
    public void testHandleBatchEventDeletesStagingRowsRegardlessOfStatusUpdateOutcome() {
        UUID batchId = UUID.randomUUID();
        LmsUserBatchEvent event = LmsUserBatchEvent.builder().batchId(batchId).status(LmsUserBatchStatus.COMPLETED).build();

        lmsUserBatchAsyncService.handleBatchEvent(event);

        verify(lmsUserBatchRepository, times(1)).deleteByBatchId(batchId);
    }

}
