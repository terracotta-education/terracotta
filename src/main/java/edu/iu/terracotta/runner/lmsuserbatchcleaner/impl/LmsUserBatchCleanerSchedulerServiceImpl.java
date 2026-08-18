package edu.iu.terracotta.runner.lmsuserbatchcleaner.impl;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchProcessing;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchProcessingRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUserBatchWriteService;
import edu.iu.terracotta.runner.lmsuserbatchcleaner.LmsUserBatchCleanerSchedulerService;
import edu.iu.terracotta.runner.lmsuserbatchcleaner.model.LmsUserBatchCleanerScheduleMessage;
import edu.iu.terracotta.runner.lmsuserbatchcleaner.model.LmsUserBatchCleanerScheduleResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.GuardLogStatement"})
public class LmsUserBatchCleanerSchedulerServiceImpl implements LmsUserBatchCleanerSchedulerService {

    private final LmsUserBatchProcessingRepository lmsUserBatchProcessingRepository;
    private final LmsUserBatchRepository lmsUserBatchRepository;
    private final LmsUserBatchWriteService lmsUserBatchWriteService;

    @Override
    public Optional<LmsUserBatchCleanerScheduleResult> cleanup(int staleTtlMinutes) {
        // a row still IN_PROGRESS past this threshold means whatever @Async task was processing
        // it never reached its own completion code - e.g. an app restart/crash mid-sync, or an
        // uncaught Error - so it never will on its own
        List<LmsUserBatchProcessing> staleBatches = lmsUserBatchProcessingRepository.findAllByStatusAndUpdatedAtBefore(
            LmsUserBatchStatus.IN_PROGRESS,
            Timestamp.from(Instant.now().minus(Duration.ofMinutes(staleTtlMinutes)))
        );

        if (CollectionUtils.isEmpty(staleBatches)) {
            // no stale batches exist; exit
            return Optional.empty();
        }

        return Optional.of(
            LmsUserBatchCleanerScheduleResult.builder()
                .processed(
                    staleBatches.stream()
                        .map(
                            staleBatch -> {
                                String error = null;
                                long deletedStagedRows = 0;

                                try {
                                    // count before deleting - staged rows this abandoned sync
                                    // already wrote to the LMS but never got to convert into
                                    // real Participant/LtiUser records or clean up itself
                                    deletedStagedRows = lmsUserBatchRepository.countByBatchId(staleBatch.getBatchId());
                                    lmsUserBatchRepository.deleteByBatchId(staleBatch.getBatchId());
                                    lmsUserBatchWriteService.markFailed(
                                        staleBatch.getBatchId(),
                                        String.format(
                                            "Sync abandoned: stuck in IN_PROGRESS for over [%d] minutes with no further progress - likely interrupted by an application restart, crash, or rejected task submission.",
                                            staleTtlMinutes
                                        )
                                    );
                                } catch (Exception e) {
                                    log.warn("Error cleaning up stale LMS user batch with batch ID: [{}]: {}", staleBatch.getBatchId(), e.getMessage());
                                    error = e.getMessage();
                                }

                                return LmsUserBatchCleanerScheduleMessage.builder()
                                    .id(staleBatch.getId())
                                    .batchId(staleBatch.getBatchId())
                                    .contextId(staleBatch.getContextId())
                                    .deletedStagedRows(deletedStagedRows)
                                    .cleanedUpAt(Timestamp.from(Instant.now()))
                                    .errors(StringUtils.isEmpty(error) ? null : List.of(error))
                                    .build();
                            }
                        )
                        .toList()
                )
                .build()
        );
    }

}
