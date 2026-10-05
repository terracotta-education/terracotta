package edu.iu.terracotta.runner.distribute.impl;

import java.time.Duration;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.runner.distribute.ExperimentCopyRecoverySchedulerService;
import edu.iu.terracotta.service.app.async.ExperimentCopyRecreationAsyncService;
import edu.iu.terracotta.service.app.distribute.ExperimentCopyCandidateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.GuardLogStatement")
public class ExperimentCopyRecoverySchedulerServiceImpl implements ExperimentCopyRecoverySchedulerService {

    private final ExperimentCopyCandidateService experimentCopyCandidateService;
    private final ExperimentCopyRecreationAsyncService experimentCopyRecreationAsyncService;

    @Value("${experiment.copy.recovery.stalled.minutes:15}")
    private int stalledMinutes;

    @Value("${experiment.copy.recovery.import.stalled.minutes:60}")
    private int importStalledMinutes;

    @Value("${experiment.copy.recovery.max.attempts:3}")
    private int maxAttempts;

    @Override
    public int recover() {
        Set<Long> contextIds = experimentCopyCandidateService.resetStalledForRecovery(
            Duration.ofMinutes(stalledMinutes),
            Duration.ofMinutes(importStalledMinutes),
            maxAttempts
        );

        // as the source course's instructor, the same as the original recreation
        contextIds.forEach(experimentCopyRecreationAsyncService::recreate);

        return contextIds.size();
    }

}
