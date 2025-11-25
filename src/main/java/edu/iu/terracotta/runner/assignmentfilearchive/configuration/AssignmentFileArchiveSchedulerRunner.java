package edu.iu.terracotta.runner.assignmentfilearchive.configuration;

import com.github.kagkarlsson.scheduler.task.schedule.Schedules;

import edu.iu.terracotta.exceptions.scheduledtask.ScheduledTaskNotFound;
import edu.iu.terracotta.runner.assignmentfilearchive.AssignmentFileArchiveSchedulerService;
import edu.iu.terracotta.runner.assignmentfilearchive.model.AssignmentFileArchiveScheduleResult;
import edu.iu.terracotta.service.app.ScheduledTaskService;

import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@SuppressWarnings({"unused", "PMD.GuardLogStatement"})
public class AssignmentFileArchiveSchedulerRunner {

    public static final String TASK_NAME = "delete_expired_assignment_file_archives";
    public static final TaskDescriptor<Void> ASSIGNMENT_FILE_DELETE_TASK = TaskDescriptor.of(TASK_NAME);

    @Autowired private ScheduledTaskService scheduledTaskService;

    @Value("${assignment.file.archive.scheduler.enabled:false}")
    private boolean enabled;

    @Value("${assignment.file.archive.scheduler.check.interval.minutes:60}")
    private int interval;

    @Bean
    Task<Void> assignmentFileArchiveDeleteSchedulerTask(AssignmentFileArchiveSchedulerService assignmentFileArchiveSchedulerService) {
        try {
            // reset task in case of dirty server shutdown
            scheduledTaskService.resetTask(TASK_NAME);
        } catch (ScheduledTaskNotFound e) {
            log.error(e.getMessage());
        }

        if (!enabled) {
            // not enabled; create one-time task to log message
            return Tasks.oneTime(ASSIGNMENT_FILE_DELETE_TASK)
                .execute(
                    (instance, ctx) -> {
                        log.info("Assignment file archive delete task [{}] is not enabled.", ASSIGNMENT_FILE_DELETE_TASK);
                    }
                );
        }

        log.info("Creating assignment file archive delete task [{}]", ASSIGNMENT_FILE_DELETE_TASK);

        return Tasks.recurring(ASSIGNMENT_FILE_DELETE_TASK, Schedules.fixedDelay(Duration.ofMinutes(interval)))
            .onDeadExecutionRevive()
            .execute(
                (instance, ctx) -> {
                    Optional<AssignmentFileArchiveScheduleResult> results = assignmentFileArchiveSchedulerService.cleanup();

                    if (results.isEmpty()) {
                        return;
                    }

                    try {
                        log.info(
                            "Task [{}] ran. Processed assignment file archives: [{}]",
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