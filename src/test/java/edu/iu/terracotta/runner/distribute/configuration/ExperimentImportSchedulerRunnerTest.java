package edu.iu.terracotta.runner.distribute.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import edu.iu.terracotta.runner.distribute.ExperimentImportSchedulerService;
import edu.iu.terracotta.runner.distribute.model.ExperimentImportScheduleResult;
import edu.iu.terracotta.service.app.ScheduledTaskService;

public class ExperimentImportSchedulerRunnerTest extends BaseTest {

    @Mock private ScheduledTaskService scheduledTaskService;
    @Mock private ExperimentImportSchedulerService experimentImportSchedulerService;

    private ExperimentImportSchedulerRunner experimentImportSchedulerRunner;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        experimentImportSchedulerRunner = new ExperimentImportSchedulerRunner(scheduledTaskService);
        ReflectionTestUtils.setField(experimentImportSchedulerRunner, "interval", 60);
    }

    @Test
    void testResetTaskCalledSuccessfully() throws ScheduledTaskNotFound {
        ReflectionTestUtils.setField(experimentImportSchedulerRunner, "enabled", false);

        assertDoesNotThrow(() -> experimentImportSchedulerRunner.experimentImportDeleteSchedulerTask(experimentImportSchedulerService));

        verify(scheduledTaskService).resetTask(ExperimentImportSchedulerRunner.TASK_NAME);
    }

    @Test
    void testResetTaskNotFoundExceptionDoesNotPropagate() throws ScheduledTaskNotFound {
        doThrow(new ScheduledTaskNotFound("not found")).when(scheduledTaskService).resetTask(ExperimentImportSchedulerRunner.TASK_NAME);
        ReflectionTestUtils.setField(experimentImportSchedulerRunner, "enabled", false);

        assertDoesNotThrow(() -> experimentImportSchedulerRunner.experimentImportDeleteSchedulerTask(experimentImportSchedulerService));
    }

    @Test
    void testDisabledTaskNeverInteractsWithSchedulerService() {
        ReflectionTestUtils.setField(experimentImportSchedulerRunner, "enabled", false);

        Task<Void> task = experimentImportSchedulerRunner.experimentImportDeleteSchedulerTask(experimentImportSchedulerService);

        assertDoesNotThrow(() -> task.execute(new TaskInstance<>(ExperimentImportSchedulerRunner.TASK_NAME, "test-id"), null));
        verifyNoInteractions(experimentImportSchedulerService);
    }

    @Test
    void testEnabledTaskWithEmptyResultCallsCleanup() {
        ReflectionTestUtils.setField(experimentImportSchedulerRunner, "enabled", true);
        when(experimentImportSchedulerService.cleanup()).thenReturn(Optional.empty());

        Task<Void> task = experimentImportSchedulerRunner.experimentImportDeleteSchedulerTask(experimentImportSchedulerService);

        assertDoesNotThrow(() -> task.execute(new TaskInstance<>(ExperimentImportSchedulerRunner.TASK_NAME, "test-id"), null));
        verify(experimentImportSchedulerService).cleanup();
    }

    @Test
    void testEnabledTaskWithResultDoesNotThrow() {
        ReflectionTestUtils.setField(experimentImportSchedulerRunner, "enabled", true);
        when(experimentImportSchedulerService.cleanup()).thenReturn(
            Optional.of(ExperimentImportScheduleResult.builder().processed(List.of()).build())
        );

        Task<Void> task = experimentImportSchedulerRunner.experimentImportDeleteSchedulerTask(experimentImportSchedulerService);

        assertDoesNotThrow(() -> task.execute(new TaskInstance<>(ExperimentImportSchedulerRunner.TASK_NAME, "test-id"), null));
        verify(experimentImportSchedulerService).cleanup();
    }

}
