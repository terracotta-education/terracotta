package edu.iu.terracotta.connectors.generic.dao.repository.lms;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchProcessing;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;

public interface LmsUserBatchProcessingRepository extends JpaRepository<LmsUserBatchProcessing, Long> {

    Optional<LmsUserBatchProcessing> findByBatchId(UUID batchId);
    Optional<LmsUserBatchProcessing> findFirstByContextIdAndStatus(Long contextId, LmsUserBatchStatus status);
    Optional<LmsUserBatchProcessing> findFirstByContextIdOrderByCreatedAtDesc(Long contextId);
    // excludes the batch currently being processed - used by the debounce check so a batch's own
    // just-created IN_PROGRESS row doesn't make it look like the context was already synced
    Optional<LmsUserBatchProcessing> findFirstByContextIdAndBatchIdNotOrderByCreatedAtDesc(Long contextId, UUID batchId);
    // a row left in this status past the given threshold means whatever was processing it (an
    // @Async task) never reached its own completion code - e.g. an app restart/crash mid-sync, or
    // an uncaught Error - see LmsUserBatchCleanerSchedulerServiceImpl
    List<LmsUserBatchProcessing> findAllByStatusAndUpdatedAtBefore(LmsUserBatchStatus status, Timestamp threshold);

    /**
     * More than one writer can race to record the same batchId's terminal status (e.g. the LMS
     * sync's own completion event and the outer async task that kicked it off both know how to
     * report "done" for the same tracking row) - a direct, unconditional UPDATE deliberately
     * bypasses the entity's @Version optimistic-lock check, since every such writer for a given
     * batchId writes an equivalent final state here; whichever one runs last simply wins, with no
     * exception, instead of racing on entity_version. A null message leaves the existing message
     * untouched (see LmsUserBatchWriteServiceImpl.updateStatus).
     *
     * @return the number of rows updated - 0 means no row exists yet for this batchId
     */
    @Modifying
    @Transactional
    @Query("UPDATE LmsUserBatchProcessing p SET p.status = :status, p.message = CASE WHEN :message IS NULL THEN p.message ELSE :message END WHERE p.batchId = :batchId")
    int updateStatusAndMessage(@Param("batchId") UUID batchId, @Param("status") LmsUserBatchStatus status, @Param("message") String message);

}
