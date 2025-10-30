package edu.iu.terracotta.runner.distribute.configuration;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;

import edu.iu.terracotta.exceptions.scheduledtask.ScheduledTaskNotFound;
import edu.iu.terracotta.runner.distribute.ExperimentImportSchedulerService;
import edu.iu.terracotta.runner.distribute.model.ExperimentImportScheduleResult;
import edu.iu.terracotta.service.app.ScheduledTaskService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@SuppressWarnings({"unused", "PMD.GuardLogStatement"})
public class ExperimentImportSchedulerRunner {

    public static final String TASK_NAME = "delete_completed_experiment_imports";
    public static final TaskDescriptor<Void> EXPERIMENT_IMPORT_DELETE_TASK = TaskDescriptor.of(TASK_NAME);

    @Autowired private ScheduledTaskService scheduledTaskService;

    @Value("${experiment.import.scheduler.enabled:false}")
    private boolean enabled;

    @Value("${experiment.import.scheduler.check.interval.minutes:60}")
    private int interval;

    @Bean
    Task<Void> experimentImportDeleteSchedulerTask(ExperimentImportSchedulerService experimentImportSchedulerService) {
        try {
            // reset task in case of dirty server shutdown
            scheduledTaskService.resetTask(TASK_NAME);
        } catch (ScheduledTaskNotFound e) {
            log.error(e.getMessage());
        }

        if (!enabled) {
            // not enabled; create one-time task to log message
            return Tasks.oneTime(EXPERIMENT_IMPORT_DELETE_TASK)
                .execute(
                    (instance, ctx) -> {
                        log.info("Experiment import delete task [{}] in not enabled.", EXPERIMENT_IMPORT_DELETE_TASK);
                    }
                );
        }

        log.info("Creating experiment import delete task [{}]", EXPERIMENT_IMPORT_DELETE_TASK);

        return Tasks.recurring(EXPERIMENT_IMPORT_DELETE_TASK, Schedules.fixedDelay(Duration.ofMinutes(interval)))
            .onDeadExecutionRevive()
            .execute(
                (instance, ctx) -> {
                    Optional<ExperimentImportScheduleResult> results = experimentImportSchedulerService.cleanup();

                    if (results.isEmpty()) {
                        return;
                    }

                    try {
                        log.info("Task [{}] ran. Processed experiment imports: [{}]", TASK_NAME, new ObjectMapper().writeValueAsString(results.get()));
                    } catch (JsonProcessingException e) {
                        log.error("Error occurred writing value to JSON", e);
                    }
                }
            );
    }

}
