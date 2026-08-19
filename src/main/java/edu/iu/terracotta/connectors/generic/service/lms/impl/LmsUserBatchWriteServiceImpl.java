package edu.iu.terracotta.connectors.generic.service.lms.impl;

import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.GuardLogStatement"})
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

        // the LMS occasionally returns a roster entry with no usable user identifier (e.g. a
        // pending/placeholder enrollment) - LtiUserEntity's constructor asserts on a blank
        // userKey, and letting one through here would blow up the whole page's transaction
        // (ParticipantRosterWriteServiceImpl.syncParticipantsPage), silently dropping every other
        // participant in that page along with it
        List<LmsUserBatch> validUsersToSave = usersToSave.stream()
            .filter(
                user -> {
                    if (StringUtils.isNotBlank(user.getUserKey())) {
                        return true;
                    }

                    log.warn("Skipping LMS user batch row with a blank user key; lmsUserId: [{}]", user.getLmsUserId());

                    return false;
                }
            )
            .toList();

        if (CollectionUtils.isEmpty(validUsersToSave)) {
            return;
        }

        lmsUserBatchRepository.saveAll(validUsersToSave);
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
        // more than one writer can independently report "done" for the same batchId (e.g. the
        // LMS sync's own completion event and the outer async task that kicked it off) - a
        // read-then-save() of the @Version-checked entity would let whichever one commits second
        // fail with ObjectOptimisticLockingFailureException, and that failure surfaces at this
        // REQUIRES_NEW transaction's AOP-driven commit, AFTER this method body returns - not
        // catchable by a caller-side try/catch around individual statements. A direct UPDATE
        // bypasses that check entirely: every such writer for a given batchId writes an
        // equivalent final state, so whichever one runs last simply wins, with no exception.
        int updated = lmsUserBatchProcessingRepository.updateStatusAndMessage(batchId, status, message);

        if (updated == 0) {
            // no row exists yet for this batchId (e.g. markFailed called before startBatch ever
            // ran) - fall back to creating one
            lmsUserBatchProcessingRepository.save(
                LmsUserBatchProcessing.builder()
                    .batchId(batchId)
                    .status(status)
                    .message(message)
                    .build()
            );
        }
    }

}
