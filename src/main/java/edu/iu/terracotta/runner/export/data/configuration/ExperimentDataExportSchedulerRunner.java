package edu.iu.terracotta.runner.export.data.configuration;

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
import edu.iu.terracotta.runner.export.data.ExperimentDataExportSchedulerService;
import edu.iu.terracotta.runner.export.data.model.ExperimentDataExportScheduleResult;
import edu.iu.terracotta.service.app.ScheduledTaskService;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Configuration
@SuppressWarnings({"unused", "PMD.GuardLogStatement"})
public class ExperimentDataExportSchedulerRunner {

    public static final String TASK_NAME = "delete_expired_experiment_data_exports";
    public static final TaskDescriptor<Void> EXPERIMENT_DATA_EXPORT_DELETE_TASK = TaskDescriptor.of(TASK_NAME);

    @Autowired private ScheduledTaskService scheduledTaskService;

    @Value("${experiment.data.export.scheduler.enabled:false}")
    private boolean enabled;

    @Value("${experiment.data.export.scheduler.check.interval.minutes:60}")
    private int interval;

    @Bean
    Task<Void> experimentDataExportDeleteSchedulerTask(ExperimentDataExportSchedulerService experimentDataExportSchedulerService) {
        try {
            // reset task in case of dirty server shutdown
            scheduledTaskService.resetTask(TASK_NAME);
        } catch (ScheduledTaskNotFound e) {
            log.error(e.getMessage());
        }

        if (!enabled) {
            // not enabled; create one-time task to log message
            return Tasks.oneTime(EXPERIMENT_DATA_EXPORT_DELETE_TASK)
                .execute(
                    (instance, ctx) -> {
                        log.info("Experiment data export delete task [{}] is not enabled.", EXPERIMENT_DATA_EXPORT_DELETE_TASK);
                    }
                );
        }

        log.info("Creating experiment data export delete task [{}]", EXPERIMENT_DATA_EXPORT_DELETE_TASK);

        return Tasks.recurring(EXPERIMENT_DATA_EXPORT_DELETE_TASK, Schedules.fixedDelay(Duration.ofMinutes(interval)))
            .onDeadExecutionRevive()
            .execute(
                (instance, ctx) -> {
                    Optional<ExperimentDataExportScheduleResult> results = experimentDataExportSchedulerService.cleanup();

                    if (results.isEmpty()) {
                        return;
                    }

                    try {
                        log.info(
                            "Task [{}] ran. Processed experiment data exports: [{}]",
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
