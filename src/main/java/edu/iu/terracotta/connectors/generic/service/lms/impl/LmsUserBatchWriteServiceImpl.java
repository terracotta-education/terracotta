package edu.iu.terracotta.connectors.generic.service.lms.impl;

import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatch;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchProcessing;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchProcessingRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUserBatchWriteService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LmsUserBatchWriteServiceImpl implements LmsUserBatchWriteService {

    private final LmsUserBatchProcessingRepository lmsUserBatchProcessingRepository;
    private final LmsUserBatchRepository lmsUserBatchRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startBatch(UUID batchId, Long contextId) {
        // reuse the existing row if the caller already created one for this batchId (see
        // ParticipantServiceImpl.startPrepareParticipation) instead of always inserting a new
        // one, which previously left two LmsUserBatchProcessing rows behind for one logical
        // refresh
        LmsUserBatchProcessing lmsUserBatchProcessing = lmsUserBatchProcessingRepository.findByBatchId(batchId)
            .orElseGet(() -> LmsUserBatchProcessing.builder().batchId(batchId).build());

        lmsUserBatchProcessing.setContextId(contextId);
        lmsUserBatchProcessing.setStatus(LmsUserBatchStatus.IN_PROGRESS);
        lmsUserBatchProcessingRepository.save(lmsUserBatchProcessing);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveUsers(List<LmsUserBatch> usersToSave) {
        if (CollectionUtils.isEmpty(usersToSave)) {
            return;
        }

        lmsUserBatchRepository.saveAll(usersToSave);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID batchId, String message) {
        // self-invokes updateStatus's body directly (bypassing its own @Transactional, since
        // self-invocation never goes through the proxy) - this method's own REQUIRES_NEW already
        // establishes the independent-transaction guarantee callers of markFailed rely on
        updateStatus(batchId, LmsUserBatchStatus.FAILED, message);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(UUID batchId, LmsUserBatchStatus status, String message) {
        // NOTE: deliberately does not catch ObjectOptimisticLockingFailureException here - with
        // plain save() (not saveAndFlush()), the actual UPDATE/version check is deferred to this
        // REQUIRES_NEW transaction's commit, which Spring's @Transactional AOP advice runs AFTER
        // this method body returns. A try/catch in here cannot see that failure. Callers on a
        // DIFFERENT bean calling this method DO see it synchronously (the proxy's commit is part
        // of their call), so they're the right place to catch a lost race - see
        // ParticipantAsyncServiceImpl.updateBatchStatus / LmsUserBatchAsyncServiceImpl.handleBatchEvent
        LmsUserBatchProcessing lmsUserBatchProcessing = lmsUserBatchProcessingRepository.findByBatchId(batchId)
            .orElseGet(() -> LmsUserBatchProcessing.builder().batchId(batchId).build());

        lmsUserBatchProcessing.setStatus(status);

        // null means "leave whatever message is already there" (e.g. a blank event message
        // shouldn't blow away a previously recorded error) - pass an empty string to clear it
        if (message != null) {
            lmsUserBatchProcessing.setMessage(message);
        }

        lmsUserBatchProcessingRepository.save(lmsUserBatchProcessing);
    }

}
