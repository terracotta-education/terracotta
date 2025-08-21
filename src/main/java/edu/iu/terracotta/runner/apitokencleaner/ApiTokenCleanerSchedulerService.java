package edu.iu.terracotta.runner.apitokencleaner;

import java.util.Optional;

import edu.iu.terracotta.runner.apitokencleaner.model.ApiTokenCleanerScheduleResult;

public interface ApiTokenCleanerSchedulerService {

    Optional<ApiTokenCleanerScheduleResult> cleanup(int expirationTtlDays);

}
