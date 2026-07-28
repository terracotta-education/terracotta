package edu.iu.terracotta.runner.apitokencleaner.configuration;

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
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.exceptions.scheduledtask.ScheduledTaskNotFound;
import edu.iu.terracotta.runner.apitokencleaner.ApiTokenCleanerSchedulerService;
import edu.iu.terracotta.runner.apitokencleaner.model.ApiTokenCleanerScheduleMessage;
import edu.iu.terracotta.runner.apitokencleaner.model.ApiTokenCleanerScheduleResult;
import edu.iu.terracotta.service.app.ScheduledTaskService;

public class ApiTokenCleanerSchedulerRunnerTest extends BaseTest {

    @Mock private ScheduledTaskService scheduledTaskService;
    @Mock private ApiTokenCleanerSchedulerService apiTokenCleanerSchedulerService;

    private ApiTokenCleanerSchedulerRunner apiTokenCleanerSchedulerRunner;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        apiTokenCleanerSchedulerRunner = new ApiTokenCleanerSchedulerRunner(scheduledTaskService);
    }

    @Test
    public void testApiTokenCleanerTaskResetTaskSucceeds() throws ScheduledTaskNotFound {
        Task<Void> task = apiTokenCleanerSchedulerRunner.apiTokenCleanerTask(apiTokenCleanerSchedulerService);

        assertNotNull(task);
        verify(scheduledTaskService).resetTask(ApiTokenCleanerSchedulerRunner.TASK_NAME);
    }

    @Test
    public void testApiTokenCleanerTaskResetTaskNotFoundIsSwallowed() throws ScheduledTaskNotFound {
        doThrow(new ScheduledTaskNotFound("not found")).when(scheduledTaskService).resetTask(ApiTokenCleanerSchedulerRunner.TASK_NAME);

        Task<Void> task = assertDoesNotThrow(() -> apiTokenCleanerSchedulerRunner.apiTokenCleanerTask(apiTokenCleanerSchedulerService));

        assertNotNull(task);
    }

    @Test
    public void testApiTokenCleanerTaskDisabledDoesNotInvokeService() {
        ReflectionTestUtils.setField(apiTokenCleanerSchedulerRunner, "enabled", false);

        Task<Void> task = apiTokenCleanerSchedulerRunner.apiTokenCleanerTask(apiTokenCleanerSchedulerService);

        // ExecutionContext is never read by the runner's lambdas; null is safe here.
        task.execute(new TaskInstance<Void>(ApiTokenCleanerSchedulerRunner.TASK_NAME, "test-id"), null);

        verifyNoInteractions(apiTokenCleanerSchedulerService);
    }

    @Test
    public void testApiTokenCleanerTaskEnabledInvokesCleanupWhenResultEmpty() {
        ReflectionTestUtils.setField(apiTokenCleanerSchedulerRunner, "enabled", true);
        ReflectionTestUtils.setField(apiTokenCleanerSchedulerRunner, "interval", 60);
        ReflectionTestUtils.setField(apiTokenCleanerSchedulerRunner, "expirationTtlDays", 45);

        when(apiTokenCleanerSchedulerService.cleanup(45)).thenReturn(Optional.empty());

        Task<Void> task = apiTokenCleanerSchedulerRunner.apiTokenCleanerTask(apiTokenCleanerSchedulerService);

        task.execute(new TaskInstance<Void>(ApiTokenCleanerSchedulerRunner.TASK_NAME, "test-id"), null);

        verify(apiTokenCleanerSchedulerService).cleanup(45);
    }

    @Test
    public void testApiTokenCleanerTaskEnabledLogsResultWithoutThrowing() {
        ReflectionTestUtils.setField(apiTokenCleanerSchedulerRunner, "enabled", true);
        ReflectionTestUtils.setField(apiTokenCleanerSchedulerRunner, "interval", 60);
        ReflectionTestUtils.setField(apiTokenCleanerSchedulerRunner, "expirationTtlDays", 30);

        ApiTokenCleanerScheduleResult scheduleResult = ApiTokenCleanerScheduleResult.builder()
            .processed(
                List.of(
                    ApiTokenCleanerScheduleMessage.builder().id(1L).lmsUserName("user-1").userId(1L).lmsConnector(LmsConnector.BRIGHTSPACE).build(),
                    ApiTokenCleanerScheduleMessage.builder().id(2L).lmsUserName("user-2").userId(2L).lmsConnector(LmsConnector.BRIGHTSPACE).build()
                )
            )
            .build();

        when(apiTokenCleanerSchedulerService.cleanup(30)).thenReturn(Optional.of(scheduleResult));

        Task<Void> task = apiTokenCleanerSchedulerRunner.apiTokenCleanerTask(apiTokenCleanerSchedulerService);

        assertDoesNotThrow(() -> task.execute(new TaskInstance<Void>(ApiTokenCleanerSchedulerRunner.TASK_NAME, "test-id"), null));

        verify(apiTokenCleanerSchedulerService).cleanup(30);
    }

}
