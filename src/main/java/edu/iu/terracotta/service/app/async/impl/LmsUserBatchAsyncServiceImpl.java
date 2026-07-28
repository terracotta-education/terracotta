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

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchProcessing;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;
import edu.iu.terracotta.connectors.generic.dao.model.event.LmsUserBatchEvent;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchProcessingRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;
import edu.iu.terracotta.service.app.async.LmsUserBatchAsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LmsUserBatchAsyncServiceImpl implements LmsUserBatchAsyncService {

    private final LmsUserBatchProcessingRepository lmsUserBatchProcessingRepository;
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
        // delete the data rows
        lmsUserBatchRepository.deleteByBatchId(lmsUserBatchEvent.batchId());

        // set the batch status; if one is not available, create it
        LmsUserBatchProcessing lmsUserBatchProcessing = lmsUserBatchProcessingRepository.findByBatchId(lmsUserBatchEvent.batchId())
            .orElse(LmsUserBatchProcessing.builder().build());

        lmsUserBatchProcessing.setStatus(lmsUserBatchEvent.status());

        if (StringUtils.isNotBlank(lmsUserBatchEvent.message())) {
            lmsUserBatchProcessing.setMessage(lmsUserBatchEvent.message());
        }

        lmsUserBatchProcessingRepository.save(lmsUserBatchProcessing);
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
