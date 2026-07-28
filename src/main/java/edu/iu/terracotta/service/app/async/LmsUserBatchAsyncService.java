package edu.iu.terracotta.service.app.async;

import java.util.UUID;

import edu.iu.terracotta.connectors.generic.dao.model.event.LmsUserBatchEvent;

public interface LmsUserBatchAsyncService {

    void handleBatchEvent(LmsUserBatchEvent lmsUserBatchEvent);
    void success(UUID batchId);
    void processed(UUID batchId, String message);
    void fail(UUID batchId, String message);

}
