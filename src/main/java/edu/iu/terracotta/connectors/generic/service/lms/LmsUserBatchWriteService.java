package edu.iu.terracotta.connectors.generic.service.lms;

import java.util.List;
import java.util.UUID;

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatch;

/**
 * Commits lms_user_batch/lms_user_batch_processing writes in their own transaction (independent
 * of whatever ambient transaction the caller is running in), so a long-running, multi-page LMS
 * fetch (e.g. a full course roster) leaves an incrementally visible, durable progress trail -
 * instead of everything being invisible until (and erased if not) the caller's own transaction
 * commits.
 */
public interface LmsUserBatchWriteService {

    void startBatch(UUID batchId);
    void saveUsers(List<LmsUserBatch> usersToSave);
    void markFailed(UUID batchId, String message);

}
