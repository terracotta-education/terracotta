package edu.iu.terracotta.runner.export.data.configuration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskInstance;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.exceptions.scheduledtask.ScheduledTaskNotFound;
import edu.iu.terracotta.runner.export.data.ExperimentDataExportSchedulerService;
import edu.iu.terracotta.runner.export.data.model.ExperimentDataExportScheduleMessage;
import edu.iu.terracotta.runner.export.data.model.ExperimentDataExportScheduleResult;
import edu.iu.terracotta.service.app.ScheduledTaskService;

class ExperimentDataExportSchedulerRunnerTest extends BaseTest {

    @Mock private ScheduledTaskService scheduledTaskService;
    @Mock private ExperimentDataExportSchedulerService experimentDataExportSchedulerService;

    private ExperimentDataExportSchedulerRunner experimentDataExportSchedulerRunner;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();

        experimentDataExportSchedulerRunner = new ExperimentDataExportSchedulerRunner(scheduledTaskService);
        ReflectionTestUtils.setField(experimentDataExportSchedulerRunner, "interval", 60);
    }

    private void executeTask(Task<Void> task) {
        task.execute(new TaskInstance<>(ExperimentDataExportSchedulerRunner.TASK_NAME, "test-id"), null);
    }

    @Test
    void testResetTaskSucceeds() throws ScheduledTaskNotFound {
        ReflectionTestUtils.setField(experimentDataExportSchedulerRunner, "enabled", false);

        Task<Void> task = experimentDataExportSchedulerRunner.experimentDataExportDeleteSchedulerTask(experimentDataExportSchedulerService);

        assertNotNull(task);
        assertEquals(ExperimentDataExportSchedulerRunner.TASK_NAME, task.getName());
        verify(scheduledTaskService).resetTask(ExperimentDataExportSchedulerRunner.TASK_NAME);
    }

    @Test
    void testResetTaskNotFoundIsSwallowed() throws ScheduledTaskNotFound {
        ReflectionTestUtils.setField(experimentDataExportSchedulerRunner, "enabled", false);
        doThrow(new ScheduledTaskNotFound("task not found")).when(scheduledTaskService).resetTask(ExperimentDataExportSchedulerRunner.TASK_NAME);

        Task<Void> task = assertDoesNotThrow(
            () -> experimentDataExportSchedulerRunner.experimentDataExportDeleteSchedulerTask(experimentDataExportSchedulerService)
        );

        assertNotNull(task);
        verify(scheduledTaskService).resetTask(ExperimentDataExportSchedulerRunner.TASK_NAME);
    }

    @Test
    void testDisabledTaskNeverInvokesService() {
        ReflectionTestUtils.setField(experimentDataExportSchedulerRunner, "enabled", false);

        Task<Void> task = experimentDataExportSchedulerRunner.experimentDataExportDeleteSchedulerTask(experimentDataExportSchedulerService);

        executeTask(task);

        verifyNoInteractions(experimentDataExportSchedulerService);
    }

    @Test
    void testEnabledTaskCallsCleanupWhenResultEmpty() {
        ReflectionTestUtils.setField(experimentDataExportSchedulerRunner, "enabled", true);
        when(experimentDataExportSchedulerService.cleanup()).thenReturn(Optional.empty());

        Task<Void> task = experimentDataExportSchedulerRunner.experimentDataExportDeleteSchedulerTask(experimentDataExportSchedulerService);

        assertDoesNotThrow(() -> executeTask(task));

        verify(experimentDataExportSchedulerService).cleanup();
    }

    @Test
    void testEnabledTaskCallsCleanupAndLogsJsonWhenResultPresent() {
        ReflectionTestUtils.setField(experimentDataExportSchedulerRunner, "enabled", true);
        ExperimentDataExportScheduleResult scheduleResult = ExperimentDataExportScheduleResult.builder()
            .processed(
                List.of(
                    ExperimentDataExportScheduleMessage.builder()
                        .id(1L)
                        .fileName("export-1.zip")
                        .fileUri("export-1")
                        .build()
                )
            )
            .build();
        when(experimentDataExportSchedulerService.cleanup()).thenReturn(Optional.of(scheduleResult));

        Task<Void> task = experimentDataExportSchedulerRunner.experimentDataExportDeleteSchedulerTask(experimentDataExportSchedulerService);

        assertDoesNotThrow(() -> executeTask(task));

        verify(experimentDataExportSchedulerService).cleanup();
    }

}
