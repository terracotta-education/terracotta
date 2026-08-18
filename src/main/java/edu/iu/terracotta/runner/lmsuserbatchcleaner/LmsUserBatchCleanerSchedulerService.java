package edu.iu.terracotta.runner.lmsuserbatchcleaner;

import java.util.Optional;

import edu.iu.terracotta.runner.lmsuserbatchcleaner.model.LmsUserBatchCleanerScheduleResult;

public interface LmsUserBatchCleanerSchedulerService {

    Optional<LmsUserBatchCleanerScheduleResult> cleanup(int staleTtlMinutes);

}
