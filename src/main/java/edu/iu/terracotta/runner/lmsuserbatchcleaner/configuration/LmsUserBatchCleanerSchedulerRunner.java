package edu.iu.terracotta.runner.lmsuserbatchcleaner.configuration;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;

import edu.iu.terracotta.exceptions.scheduledtask.ScheduledTaskNotFound;
import edu.iu.terracotta.runner.lmsuserbatchcleaner.LmsUserBatchCleanerSchedulerService;
import edu.iu.terracotta.runner.lmsuserbatchcleaner.model.LmsUserBatchCleanerScheduleResult;
import edu.iu.terracotta.service.app.ScheduledTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Configuration
@RequiredArgsConstructor
@SuppressWarnings({"PMD.GuardLogStatement"})
public class LmsUserBatchCleanerSchedulerRunner {

    public static final String TASK_NAME = "cleanup_stale_lms_user_batches";
    public static final TaskDescriptor<Void> LMS_USER_BATCH_CLEANER_TASK = TaskDescriptor.of(TASK_NAME);

    private final ScheduledTaskService scheduledTaskService;

    @Value("${lms.user.batch.cleaner.scheduler.enabled:false}")
    private boolean enabled;

    @Value("${lms.user.batch.cleaner.scheduler.check.interval.minutes:60}")
    private int interval;

    @Value("${lms.user.batch.cleaner.scheduler.stale.ttl.minutes:60}")
    private int staleTtlMinutes;

    @Bean
    Task<Void> lmsUserBatchCleanerTask(LmsUserBatchCleanerSchedulerService lmsUserBatchCleanerSchedulerService) {
        try {
            // reset task in case of dirty server shutdown
            scheduledTaskService.resetTask(TASK_NAME);
        } catch (ScheduledTaskNotFound e) {
            log.error(e.getMessage());
        }

        if (!enabled) {
            // not enabled; create one-time task to log message
            return Tasks.oneTime(LMS_USER_BATCH_CLEANER_TASK)
                .execute(
                    (instance, ctx) -> {
                        log.info("LMS user batch cleaner task [{}] is not enabled.", LMS_USER_BATCH_CLEANER_TASK.getTaskName());
                    }
                );
        }

        log.info("Creating LMS user batch cleaner task [{}]", LMS_USER_BATCH_CLEANER_TASK.getTaskName());

        return Tasks.recurring(LMS_USER_BATCH_CLEANER_TASK, Schedules.fixedDelay(Duration.ofMinutes(interval)))
            .onDeadExecutionRevive()
            .execute(
                (instance, ctx) -> {
                    Optional<LmsUserBatchCleanerScheduleResult> results = lmsUserBatchCleanerSchedulerService.cleanup(staleTtlMinutes);

                    if (results.isEmpty()) {
                        return;
                    }

                    try {
                        log.info(
                            "Task [{}] ran. Cleaned up stale LMS user batches: [{}]",
                            TASK_NAME,
                            JsonMapper.builder()
                                .build()
                                .writeValueAsString(results.get())
                        );
                    } catch (JacksonException e) {
                        log.error("Error occurred writing value to JSON", e);
                    }
                }
            );
    }
}
