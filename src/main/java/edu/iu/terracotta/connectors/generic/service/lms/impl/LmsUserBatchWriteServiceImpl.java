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
        LmsUserBatchProcessing lmsUserBatchProcessing = lmsUserBatchProcessingRepository.findByBatchId(batchId)
            .orElseGet(() -> LmsUserBatchProcessing.builder().batchId(batchId).build());

        lmsUserBatchProcessing.setStatus(LmsUserBatchStatus.FAILED);
        lmsUserBatchProcessing.setMessage(message);
        lmsUserBatchProcessingRepository.save(lmsUserBatchProcessing);
    }

}
