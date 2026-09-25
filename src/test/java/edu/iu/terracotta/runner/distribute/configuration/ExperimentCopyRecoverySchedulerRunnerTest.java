package edu.iu.terracotta.runner.distribute.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskInstance;

import edu.iu.terracotta.exceptions.scheduledtask.ScheduledTaskNotFound;
import edu.iu.terracotta.runner.distribute.ExperimentCopyRecoverySchedulerService;
import edu.iu.terracotta.service.app.ScheduledTaskService;

public class ExperimentCopyRecoverySchedulerRunnerTest {

    @Mock private ScheduledTaskService scheduledTaskService;
    @Mock private ExperimentCopyRecoverySchedulerService experimentCopyRecoverySchedulerService;

    private ExperimentCopyRecoverySchedulerRunner experimentCopyRecoverySchedulerRunner;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        experimentCopyRecoverySchedulerRunner = new ExperimentCopyRecoverySchedulerRunner(scheduledTaskService);
        ReflectionTestUtils.setField(experimentCopyRecoverySchedulerRunner, "interval", 10);
    }

    @Test
    void testResetTaskCalledSuccessfully() throws ScheduledTaskNotFound {
        ReflectionTestUtils.setField(experimentCopyRecoverySchedulerRunner, "enabled", true);

        assertDoesNotThrow(() -> experimentCopyRecoverySchedulerRunner.experimentCopyRecoverySchedulerTask(experimentCopyRecoverySchedulerService));

        verify(scheduledTaskService).resetTask(ExperimentCopyRecoverySchedulerRunner.TASK_NAME);
    }

    @Test
    void testResetTaskNotFoundExceptionDoesNotPropagate() throws ScheduledTaskNotFound {
        doThrow(new ScheduledTaskNotFound("not found")).when(scheduledTaskService).resetTask(ExperimentCopyRecoverySchedulerRunner.TASK_NAME);
        ReflectionTestUtils.setField(experimentCopyRecoverySchedulerRunner, "enabled", true);

        assertDoesNotThrow(() -> experimentCopyRecoverySchedulerRunner.experimentCopyRecoverySchedulerTask(experimentCopyRecoverySchedulerService));
    }

    @Test
    void testDisabledTaskNeverInteractsWithSchedulerService() {
        ReflectionTestUtils.setField(experimentCopyRecoverySchedulerRunner, "enabled", false);

        Task<Void> task = experimentCopyRecoverySchedulerRunner.experimentCopyRecoverySchedulerTask(experimentCopyRecoverySchedulerService);

        assertDoesNotThrow(() -> task.execute(new TaskInstance<>(ExperimentCopyRecoverySchedulerRunner.TASK_NAME, "test-id"), null));
        verifyNoInteractions(experimentCopyRecoverySchedulerService);
        verifyNoInteractions(scheduledTaskService);
    }

    @Test
    void testEnabledTaskRunsRecovery() {
        ReflectionTestUtils.setField(experimentCopyRecoverySchedulerRunner, "enabled", true);
        when(experimentCopyRecoverySchedulerService.recover()).thenReturn(2);

        Task<Void> task = experimentCopyRecoverySchedulerRunner.experimentCopyRecoverySchedulerTask(experimentCopyRecoverySchedulerService);

        assertDoesNotThrow(() -> task.execute(new TaskInstance<>(ExperimentCopyRecoverySchedulerRunner.TASK_NAME, "test-id"), null));
        verify(experimentCopyRecoverySchedulerService).recover();
    }

}
