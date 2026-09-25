package edu.iu.terracotta.runner.distribute.configuration;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;

import edu.iu.terracotta.exceptions.scheduledtask.ScheduledTaskNotFound;
import edu.iu.terracotta.runner.distribute.ExperimentCopyRecoverySchedulerService;
import edu.iu.terracotta.service.app.ScheduledTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Periodically restarts copied-experiment recreations that stopped part-way through - e.g. the
 * server was restarted while one ran. db-scheduler runs it on one server at a time.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ExperimentCopyRecoverySchedulerRunner {

    public static final String TASK_NAME = "recover_stalled_experiment_copies";
    public static final TaskDescriptor<Void> EXPERIMENT_COPY_RECOVERY_TASK = TaskDescriptor.of(TASK_NAME);

    private final ScheduledTaskService scheduledTaskService;

    @Value("${experiment.copy.recovery.scheduler.enabled:false}")
    private boolean enabled;

    @Value("${experiment.copy.recovery.scheduler.check.interval.minutes:10}")
    private int interval;

    @Bean
    Task<Void> experimentCopyRecoverySchedulerTask(ExperimentCopyRecoverySchedulerService experimentCopyRecoverySchedulerService) {
        if (!enabled) {
            // not enabled; create one-time task to log message
            return Tasks.oneTime(EXPERIMENT_COPY_RECOVERY_TASK)
                .execute(
                    (instance, ctx) -> {
                        log.info("Experiment copy recovery task [{}] in not enabled.", EXPERIMENT_COPY_RECOVERY_TASK);
                    }
                );
        }

        try {
            // reset task in case of dirty server shutdown
            scheduledTaskService.resetTask(TASK_NAME);
        } catch (ScheduledTaskNotFound e) {
            log.error(e.getMessage());
        }

        log.info("Creating experiment copy recovery task [{}]", EXPERIMENT_COPY_RECOVERY_TASK);

        return Tasks.recurring(EXPERIMENT_COPY_RECOVERY_TASK, Schedules.fixedDelay(Duration.ofMinutes(interval)))
            .onDeadExecutionRevive()
            .execute(
                (instance, ctx) -> {
                    int restarted = experimentCopyRecoverySchedulerService.recover();

                    if (restarted > 0) {
                        log.info("Task [{}] ran. Restarted experiment recreation in [{}] course(s)", TASK_NAME, restarted);
                    }
                }
            );
    }

}
