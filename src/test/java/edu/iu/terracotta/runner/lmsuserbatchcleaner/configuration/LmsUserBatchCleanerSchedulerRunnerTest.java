package edu.iu.terracotta.runner.lmsuserbatchcleaner.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskInstance;

import edu.iu.terracotta.exceptions.scheduledtask.ScheduledTaskNotFound;
import edu.iu.terracotta.runner.lmsuserbatchcleaner.LmsUserBatchCleanerSchedulerService;
import edu.iu.terracotta.runner.lmsuserbatchcleaner.model.LmsUserBatchCleanerScheduleMessage;
import edu.iu.terracotta.runner.lmsuserbatchcleaner.model.LmsUserBatchCleanerScheduleResult;
import edu.iu.terracotta.service.app.ScheduledTaskService;

public class LmsUserBatchCleanerSchedulerRunnerTest {

    @Mock private ScheduledTaskService scheduledTaskService;
    @Mock private LmsUserBatchCleanerSchedulerService lmsUserBatchCleanerSchedulerService;

    private LmsUserBatchCleanerSchedulerRunner lmsUserBatchCleanerSchedulerRunner;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        lmsUserBatchCleanerSchedulerRunner = new LmsUserBatchCleanerSchedulerRunner(scheduledTaskService);
    }

    @Test
    public void testLmsUserBatchCleanerTaskResetTaskSucceeds() throws ScheduledTaskNotFound {
        Task<Void> task = lmsUserBatchCleanerSchedulerRunner.lmsUserBatchCleanerTask(lmsUserBatchCleanerSchedulerService);

        assertNotNull(task);
        verify(scheduledTaskService).resetTask(LmsUserBatchCleanerSchedulerRunner.TASK_NAME);
    }

    @Test
    public void testLmsUserBatchCleanerTaskResetTaskNotFoundIsSwallowed() throws ScheduledTaskNotFound {
        doThrow(new ScheduledTaskNotFound("not found")).when(scheduledTaskService).resetTask(LmsUserBatchCleanerSchedulerRunner.TASK_NAME);

        Task<Void> task = assertDoesNotThrow(() -> lmsUserBatchCleanerSchedulerRunner.lmsUserBatchCleanerTask(lmsUserBatchCleanerSchedulerService));

        assertNotNull(task);
    }

    @Test
    public void testLmsUserBatchCleanerTaskDisabledDoesNotInvokeService() {
        ReflectionTestUtils.setField(lmsUserBatchCleanerSchedulerRunner, "enabled", false);

        Task<Void> task = lmsUserBatchCleanerSchedulerRunner.lmsUserBatchCleanerTask(lmsUserBatchCleanerSchedulerService);

        // ExecutionContext is never read by the runner's lambdas; null is safe here.
        task.execute(new TaskInstance<Void>(LmsUserBatchCleanerSchedulerRunner.TASK_NAME, "test-id"), null);

        verifyNoInteractions(lmsUserBatchCleanerSchedulerService);
    }

    @Test
    public void testLmsUserBatchCleanerTaskEnabledInvokesCleanupWhenResultEmpty() {
        ReflectionTestUtils.setField(lmsUserBatchCleanerSchedulerRunner, "enabled", true);
        ReflectionTestUtils.setField(lmsUserBatchCleanerSchedulerRunner, "interval", 60);
        ReflectionTestUtils.setField(lmsUserBatchCleanerSchedulerRunner, "staleTtlMinutes", 90);

        when(lmsUserBatchCleanerSchedulerService.cleanup(90)).thenReturn(Optional.empty());

        Task<Void> task = lmsUserBatchCleanerSchedulerRunner.lmsUserBatchCleanerTask(lmsUserBatchCleanerSchedulerService);

        task.execute(new TaskInstance<Void>(LmsUserBatchCleanerSchedulerRunner.TASK_NAME, "test-id"), null);

        verify(lmsUserBatchCleanerSchedulerService).cleanup(90);
    }

    @Test
    public void testLmsUserBatchCleanerTaskEnabledLogsResultWithoutThrowing() {
        ReflectionTestUtils.setField(lmsUserBatchCleanerSchedulerRunner, "enabled", true);
        ReflectionTestUtils.setField(lmsUserBatchCleanerSchedulerRunner, "interval", 60);
        ReflectionTestUtils.setField(lmsUserBatchCleanerSchedulerRunner, "staleTtlMinutes", 60);

        LmsUserBatchCleanerScheduleResult scheduleResult = LmsUserBatchCleanerScheduleResult.builder()
            .processed(
                List.of(
                    LmsUserBatchCleanerScheduleMessage.builder().id(1L).batchId(UUID.randomUUID()).contextId(42L).deletedStagedRows(5L).build(),
                    LmsUserBatchCleanerScheduleMessage.builder().id(2L).batchId(UUID.randomUUID()).contextId(43L).deletedStagedRows(0L).build()
                )
            )
            .build();

        when(lmsUserBatchCleanerSchedulerService.cleanup(60)).thenReturn(Optional.of(scheduleResult));

        Task<Void> task = lmsUserBatchCleanerSchedulerRunner.lmsUserBatchCleanerTask(lmsUserBatchCleanerSchedulerService);

        assertDoesNotThrow(() -> task.execute(new TaskInstance<Void>(LmsUserBatchCleanerSchedulerRunner.TASK_NAME, "test-id"), null));

        verify(lmsUserBatchCleanerSchedulerService).cleanup(60);
    }

}
