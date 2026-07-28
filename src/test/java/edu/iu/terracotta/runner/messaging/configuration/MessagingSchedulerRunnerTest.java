package edu.iu.terracotta.runner.messaging.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import edu.iu.terracotta.runner.messaging.MessagingSchedulerService;
import edu.iu.terracotta.runner.messaging.model.MessagingScheduleResult;
import edu.iu.terracotta.service.app.ScheduledTaskService;

public class MessagingSchedulerRunnerTest extends BaseTest {

    @Mock private ScheduledTaskService scheduledTaskService;
    @Mock private MessagingSchedulerService messagingSchedulerService;

    private MessagingSchedulerRunner messagingSchedulerRunner;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        messagingSchedulerRunner = new MessagingSchedulerRunner(scheduledTaskService);
    }

    @Test
    public void testMessagingSchedulerSendTaskResetTaskSucceeds() throws ScheduledTaskNotFound {
        Task<Void> task = messagingSchedulerRunner.messagingSchedulerSendTask(messagingSchedulerService);

        assertNotNull(task);
        verify(scheduledTaskService).resetTask(MessagingSchedulerRunner.TASK_NAME);
    }

    @Test
    public void testMessagingSchedulerSendTaskResetTaskNotFoundIsSwallowed() throws ScheduledTaskNotFound {
        doThrow(new ScheduledTaskNotFound("not found")).when(scheduledTaskService).resetTask(MessagingSchedulerRunner.TASK_NAME);

        Task<Void> task = assertDoesNotThrow(() -> messagingSchedulerRunner.messagingSchedulerSendTask(messagingSchedulerService));

        assertNotNull(task);
    }

    @Test
    public void testMessagingSchedulerSendTaskDisabledDoesNotInvokeService() {
        ReflectionTestUtils.setField(messagingSchedulerRunner, "enabled", false);

        Task<Void> task = messagingSchedulerRunner.messagingSchedulerSendTask(messagingSchedulerService);

        // ExecutionContext is never read by the runner's lambdas; null is safe here.
        task.execute(new TaskInstance<Void>(MessagingSchedulerRunner.TASK_NAME, "test-id"), null);

        verifyNoInteractions(messagingSchedulerService);
    }

    @Test
    public void testMessagingSchedulerSendTaskEnabledInvokesSendWhenResultEmpty() {
        ReflectionTestUtils.setField(messagingSchedulerRunner, "enabled", true);
        ReflectionTestUtils.setField(messagingSchedulerRunner, "interval", 1);

        when(messagingSchedulerService.send()).thenReturn(Optional.empty());

        Task<Void> task = messagingSchedulerRunner.messagingSchedulerSendTask(messagingSchedulerService);

        task.execute(new TaskInstance<Void>(MessagingSchedulerRunner.TASK_NAME, "test-id"), null);

        verify(messagingSchedulerService).send();
    }

    @Test
    public void testMessagingSchedulerSendTaskEnabledLogsResultWithoutThrowing() {
        ReflectionTestUtils.setField(messagingSchedulerRunner, "enabled", true);
        ReflectionTestUtils.setField(messagingSchedulerRunner, "interval", 1);

        MessagingScheduleResult scheduleResult = MessagingScheduleResult.builder()
            .processed(List.of())
            .build();

        when(messagingSchedulerService.send()).thenReturn(Optional.of(scheduleResult));

        Task<Void> task = messagingSchedulerRunner.messagingSchedulerSendTask(messagingSchedulerService);

        assertDoesNotThrow(() -> task.execute(new TaskInstance<Void>(MessagingSchedulerRunner.TASK_NAME, "test-id"), null));

        verify(messagingSchedulerService).send();
    }

}
