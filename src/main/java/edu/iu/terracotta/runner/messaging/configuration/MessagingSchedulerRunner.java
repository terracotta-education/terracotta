package edu.iu.terracotta.runner.messaging.configuration;

import com.github.kagkarlsson.scheduler.task.schedule.Schedules;

import edu.iu.terracotta.exceptions.scheduledtask.ScheduledTaskNotFound;
import edu.iu.terracotta.runner.messaging.MessagingSchedulerService;
import edu.iu.terracotta.runner.messaging.configuration.model.MessagingScheduleResult;
import edu.iu.terracotta.service.app.ScheduledTaskService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@SuppressWarnings({"PMD.GuardLogStatement"})
public class MessagingSchedulerRunner {

    public static final String TASK_NAME = "send_queued_messages";
    public static final TaskDescriptor<Void> MESSAGING_SEND_TASK = TaskDescriptor.of(TASK_NAME);

    @Autowired private ScheduledTaskService scheduledTaskService;

    @Value("${app.messaging.scheduler.enabled:false}")
    private boolean enabled;

    @Value("${app.messaging.scheduler.check.interval.minutes:1}")
    private int interval;

    @Bean
    Task<Void> messagingSchedulerSendTask(MessagingSchedulerService messagingSchedulerService) {
        try {
            // reset task in case of dirty server shutdown
            scheduledTaskService.resetTask(TASK_NAME);
        } catch (ScheduledTaskNotFound e) {
            log.error(e.getMessage());
        }

        if (!enabled) {
            // not enabled; create one-time task to log message
            return Tasks.oneTime(MESSAGING_SEND_TASK)
                .execute(
                    (instance, ctx) -> {
                        log.info("Messaging send task [{}] in not enabled.", MESSAGING_SEND_TASK);
                    }
                );
        }

        log.info("Creating messaging send task [{}]", MESSAGING_SEND_TASK);

        return Tasks.recurring(MESSAGING_SEND_TASK, Schedules.fixedDelay(Duration.ofMinutes(interval)))
            .onDeadExecutionRevive()
            .execute(
                (instance, ctx) -> {
                    Optional<MessagingScheduleResult> results = messagingSchedulerService.send();

                    if (results.isEmpty()) {
                        return;
                    }

                    try {
                        log.info("Task [{}] ran. Processed messages: [{}]", MESSAGING_SEND_TASK, new ObjectMapper().writeValueAsString(results.get()));
                    } catch (JsonProcessingException e) {
                        log.error("Error occurred writing value to JSON", e);
                    }
                }
            );
    }

}
