package edu.iu.terracotta.service.app.async.impl;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;
import edu.iu.terracotta.connectors.generic.dao.model.event.LmsUserBatchEvent;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUserBatchWriteService;
import edu.iu.terracotta.service.app.async.LmsUserBatchAsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.GuardLogStatement"})
public class LmsUserBatchAsyncServiceImpl implements LmsUserBatchAsyncService {

    private final LmsUserBatchWriteService lmsUserBatchWriteService;
    private final LmsUserBatchRepository lmsUserBatchRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    // AFTER_COMPLETION (not AFTER_COMMIT) so a fail() published just before the triggering
    // transaction rolls back still gets recorded - AFTER_COMMIT-only listeners are simply never
    // invoked when the transaction they were registered against rolls back instead of commits.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
    public void handleBatchEvent(LmsUserBatchEvent lmsUserBatchEvent) {
        // delete the data rows - runs in THIS transaction, so it must not depend on the
        // updateStatus call below (a separate bean's own REQUIRES_NEW transaction) succeeding
        lmsUserBatchRepository.deleteByBatchId(lmsUserBatchEvent.batchId());

        try {
            // a genuine cross-bean call into LmsUserBatchWriteServiceImpl's own REQUIRES_NEW
            // transaction - its commit (and any optimistic-lock failure) happens synchronously
            // as part of this call, so it's catchable here without affecting the delete above
            lmsUserBatchWriteService.updateStatus(
                lmsUserBatchEvent.batchId(),
                lmsUserBatchEvent.status(),
                StringUtils.isNotBlank(lmsUserBatchEvent.message()) ? lmsUserBatchEvent.message() : null
            );
        } catch (ObjectOptimisticLockingFailureException e) {
            // prepareParticipationAsync's own completion write (see ParticipantAsyncServiceImpl)
            // already raced this same row and won - losing this race just means that write
            // already reflects the correct final status, so this one is redundant, not lost
            log.debug("Skipped batch status update for batch ID: [{}] - already updated by the caller's own completion handler", lmsUserBatchEvent.batchId(), e);
        }
    }

    @Override
    public void success(UUID batchId) {
        applicationEventPublisher.publishEvent(
            LmsUserBatchEvent.builder()
                .batchId(batchId)
                .status(LmsUserBatchStatus.COMPLETED)
                .build()
            );
    }

    @Override
    public void processed(UUID batchId, String message) {
        applicationEventPublisher.publishEvent(
            LmsUserBatchEvent.builder()
                .batchId(batchId)
                .message(message)
                .status(LmsUserBatchStatus.PROCESSED)
                .build()
            );
    }

    @Override
    public void fail(UUID batchId, String message) {
        applicationEventPublisher.publishEvent(
            LmsUserBatchEvent.builder()
                .batchId(batchId)
                .message(message)
                .status(LmsUserBatchStatus.FAILED)
                .build()
            );
    }

}
