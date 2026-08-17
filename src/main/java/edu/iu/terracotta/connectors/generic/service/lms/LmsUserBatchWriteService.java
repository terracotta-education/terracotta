package edu.iu.terracotta.connectors.generic.service.lms;

import java.util.List;
import java.util.UUID;

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatch;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;

/**
 * Commits lms_user_batch/lms_user_batch_processing writes in their own transaction (independent
 * of whatever ambient transaction the caller is running in), so a long-running, multi-page LMS
 * fetch (e.g. a full course roster) leaves an incrementally visible, durable progress trail -
 * instead of everything being invisible until (and erased if not) the caller's own transaction
 * commits.
 */
public interface LmsUserBatchWriteService {

    void startBatch(UUID batchId, Long contextId);
    void saveUsers(List<LmsUserBatch> usersToSave);
    void markFailed(UUID batchId, String message);

    /**
     * Same batchId can be marked complete/failed independently by more than one caller (e.g. the
     * LMS sync's own completion event and the outer async task that kicked it off both know how
     * to report "done" for the same tracking row) - runs in its own transaction and swallows a
     * lost optimistic-lock race rather than letting it blow up the caller, since whichever writer
     * loses just means the row already reflects the correct final state.
     */
    void updateStatus(UUID batchId, LmsUserBatchStatus status, String message);

}
