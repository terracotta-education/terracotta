package edu.iu.terracotta.connectors.generic.dao.model.event;

import java.util.UUID;

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;
import lombok.Builder;

@Builder
public record LmsUserBatchEvent(

    UUID batchId,
    LmsUserBatchStatus status,
    String message

) {

}
