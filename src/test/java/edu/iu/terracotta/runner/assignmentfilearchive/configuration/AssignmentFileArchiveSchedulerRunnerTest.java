package edu.iu.terracotta.runner.assignmentfilearchive.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
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
import edu.iu.terracotta.runner.assignmentfilearchive.AssignmentFileArchiveSchedulerService;
import edu.iu.terracotta.runner.assignmentfilearchive.model.AssignmentFileArchiveScheduleMessage;
import edu.iu.terracotta.runner.assignmentfilearchive.model.AssignmentFileArchiveScheduleResult;
import edu.iu.terracotta.service.app.ScheduledTaskService;

public class AssignmentFileArchiveSchedulerRunnerTest extends BaseTest {

    @Mock private ScheduledTaskService scheduledTaskService;
    @Mock private AssignmentFileArchiveSchedulerService assignmentFileArchiveSchedulerService;

    private AssignmentFileArchiveSchedulerRunner assignmentFileArchiveSchedulerRunner;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        assignmentFileArchiveSchedulerRunner = new AssignmentFileArchiveSchedulerRunner(scheduledTaskService);
    }

    @Test
    public void testAssignmentFileArchiveDeleteSchedulerTaskResetTaskSucceeds() throws Exception {
        Task<Void> task = assignmentFileArchiveSchedulerRunner.assignmentFileArchiveDeleteSchedulerTask(assignmentFileArchiveSchedulerService);

        assertNotNull(task);
        verify(scheduledTaskService).resetTask(AssignmentFileArchiveSchedulerRunner.TASK_NAME);
    }

    @Test
    public void testAssignmentFileArchiveDeleteSchedulerTaskResetTaskNotFoundIsSwallowed() throws Exception {
        doThrow(new ScheduledTaskNotFound("not found")).when(scheduledTaskService).resetTask(AssignmentFileArchiveSchedulerRunner.TASK_NAME);

        Task<Void> task = assertDoesNotThrow(() -> assignmentFileArchiveSchedulerRunner.assignmentFileArchiveDeleteSchedulerTask(assignmentFileArchiveSchedulerService));

        assertNotNull(task);
    }

    @Test
    public void testAssignmentFileArchiveDeleteSchedulerTaskDisabledDoesNotInvokeService() throws Exception {
        ReflectionTestUtils.setField(assignmentFileArchiveSchedulerRunner, "enabled", false);

        Task<Void> task = assignmentFileArchiveSchedulerRunner.assignmentFileArchiveDeleteSchedulerTask(assignmentFileArchiveSchedulerService);

        // ExecutionContext is never read by the runner's lambdas; null is safe here.
        task.execute(new TaskInstance<Void>(AssignmentFileArchiveSchedulerRunner.TASK_NAME, "test-id"), null);

        verifyNoInteractions(assignmentFileArchiveSchedulerService);
    }

    @Test
    public void testAssignmentFileArchiveDeleteSchedulerTaskEnabledInvokesCleanupWhenResultEmpty() throws Exception {
        ReflectionTestUtils.setField(assignmentFileArchiveSchedulerRunner, "enabled", true);
        ReflectionTestUtils.setField(assignmentFileArchiveSchedulerRunner, "interval", 60);

        when(assignmentFileArchiveSchedulerService.cleanup()).thenReturn(Optional.empty());

        Task<Void> task = assignmentFileArchiveSchedulerRunner.assignmentFileArchiveDeleteSchedulerTask(assignmentFileArchiveSchedulerService);

        task.execute(new TaskInstance<Void>(AssignmentFileArchiveSchedulerRunner.TASK_NAME, "test-id"), null);

        verify(assignmentFileArchiveSchedulerService).cleanup();
    }

    @Test
    public void testAssignmentFileArchiveDeleteSchedulerTaskEnabledLogsResultWithoutThrowing() throws Exception {
        ReflectionTestUtils.setField(assignmentFileArchiveSchedulerRunner, "enabled", true);
        ReflectionTestUtils.setField(assignmentFileArchiveSchedulerRunner, "interval", 60);

        AssignmentFileArchiveScheduleResult scheduleResult = AssignmentFileArchiveScheduleResult.builder()
            .processed(
                List.of(
                    AssignmentFileArchiveScheduleMessage.builder()
                        .id(1L)
                        .fileName("archive-1.zip")
                        .fileUri("archive-1")
                        .deletedAt(Timestamp.from(Instant.now()))
                        .build(),
                    AssignmentFileArchiveScheduleMessage.builder()
                        .id(2L)
                        .fileName("archive-2.zip")
                        .fileUri("archive-2")
                        .deletedAt(Timestamp.from(Instant.now()))
                        .errors(List.of("Failed to delete file"))
                        .build()
                )
            )
            .build();

        when(assignmentFileArchiveSchedulerService.cleanup()).thenReturn(Optional.of(scheduleResult));

        Task<Void> task = assignmentFileArchiveSchedulerRunner.assignmentFileArchiveDeleteSchedulerTask(assignmentFileArchiveSchedulerService);

        assertDoesNotThrow(() -> task.execute(new TaskInstance<Void>(AssignmentFileArchiveSchedulerRunner.TASK_NAME, "test-id"), null));

        verify(assignmentFileArchiveSchedulerService).cleanup();
    }

}
