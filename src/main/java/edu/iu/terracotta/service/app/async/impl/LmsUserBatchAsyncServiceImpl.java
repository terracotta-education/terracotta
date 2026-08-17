package edu.iu.terracotta.service.app.async.impl;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
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
        UUID batchId = lmsUserBatchEvent.batchId();

        // success() never sets a message of its own - report how many users this sync actually
        // retrieved, counted before the staging rows below are deleted
        String message = lmsUserBatchEvent.status() == LmsUserBatchStatus.COMPLETED ?
            String.format("Retrieved %d users from LMS", lmsUserBatchRepository.countByBatchId(batchId)) :
            lmsUserBatchEvent.message();

        lmsUserBatchRepository.deleteByBatchId(batchId);

        // updateStatus uses a direct UPDATE that bypasses the entity's optimistic-lock check, so
        // it can't fail here even if prepareParticipationAsync's own completion write (see
        // ParticipantAsyncServiceImpl) races this same batchId - see
        // LmsUserBatchWriteServiceImpl.updateStatus
        lmsUserBatchWriteService.updateStatus(
            batchId,
            lmsUserBatchEvent.status(),
            StringUtils.isNotBlank(message) ? message : null
        );
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
