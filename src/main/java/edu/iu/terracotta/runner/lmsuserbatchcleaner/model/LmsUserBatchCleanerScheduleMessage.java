package edu.iu.terracotta.runner.lmsuserbatchcleaner.model;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LmsUserBatchCleanerScheduleMessage {

    private long id;
    private UUID batchId;
    private Long contextId;
    private long deletedStagedRows;
    private Timestamp cleanedUpAt;
    private List<String> errors;

}
