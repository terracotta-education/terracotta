package edu.iu.terracotta.runner.apitokencleaner.configuration;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;

import edu.iu.terracotta.exceptions.scheduledtask.ScheduledTaskNotFound;
import edu.iu.terracotta.runner.apitokencleaner.ApiTokenCleanerSchedulerService;
import edu.iu.terracotta.runner.apitokencleaner.model.ApiTokenCleanerScheduleResult;
import edu.iu.terracotta.service.app.ScheduledTaskService;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Configuration
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ApiTokenCleanerSchedulerRunner {

    public static final String TASK_NAME = "delete_expired_api_tokens";
    public static final TaskDescriptor<Void> API_TOKEN_CLEANER_TASK = TaskDescriptor.of(TASK_NAME);

    @Autowired private ScheduledTaskService scheduledTaskService;

    @Value("${api.token.cleaner.scheduler.enabled:false}")
    private boolean enabled;

    @Value("${api.token.cleaner.scheduler.check.interval.minutes:60}")
    private int interval;

    @Value("${api.token.cleaner.scheduler.expiration.ttl.days:30}")
    private int expirationTtlDays;

    @Bean
    Task<Void> apiTokenCleanerTask(ApiTokenCleanerSchedulerService apiTokenCleanerSchedulerService) {
        try {
            // reset task in case of dirty server shutdown
            scheduledTaskService.resetTask(TASK_NAME);
        } catch (ScheduledTaskNotFound e) {
            log.error(e.getMessage());
        }

        if (!enabled) {
            // not enabled; create one-time task to log message
            return Tasks.oneTime(API_TOKEN_CLEANER_TASK)
                .execute(
                    (instance, ctx) -> {
                        log.info("API token cleaner task [{}] is not enabled.", API_TOKEN_CLEANER_TASK.getTaskName());
                    }
                );
        }

        log.info("Creating API token cleaner task [{}]", API_TOKEN_CLEANER_TASK.getTaskName());

        return Tasks.recurring(API_TOKEN_CLEANER_TASK, Schedules.fixedDelay(Duration.ofMinutes(interval)))
            .onDeadExecutionRevive()
            .execute(
                (instance, ctx) -> {
                    Optional<ApiTokenCleanerScheduleResult> results = apiTokenCleanerSchedulerService.cleanup(expirationTtlDays);

                    if (results.isEmpty()) {
                        return;
                    }

                    try {
                        log.info(
                            "Task [{}] ran. Deleted API tokens: [{}]",
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
