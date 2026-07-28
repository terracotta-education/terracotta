package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.scheduledtask.ScheduledTask;
import edu.iu.terracotta.dao.repository.ScheduledTaskRepository;
import edu.iu.terracotta.exceptions.scheduledtask.ScheduledTaskNotFound;

public class ScheduledTaskServiceImplTest extends BaseTest {

    @InjectMocks private ScheduledTaskServiceImpl scheduledTaskService;

    @Mock private ScheduledTaskRepository scheduledTaskRepository;

    private static final String TASK_NAME = "some.task.name";

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    public void testResetTask() throws ScheduledTaskNotFound {
        ScheduledTask scheduledTask = ScheduledTask.builder()
            .taskName(TASK_NAME)
            .picked(true)
            .pickedBy("worker-1")
            .build();

        when(scheduledTaskRepository.findByTaskName(TASK_NAME)).thenReturn(Optional.of(scheduledTask));
        when(scheduledTaskRepository.save(any(ScheduledTask.class))).thenReturn(scheduledTask);

        scheduledTaskService.resetTask(TASK_NAME);

        assertFalse(scheduledTask.isPicked());
        assertNull(scheduledTask.getPickedBy());
        verify(scheduledTaskRepository).save(scheduledTask);
    }

    @Test
    public void testResetTaskNotFound() {
        when(scheduledTaskRepository.findByTaskName(anyString())).thenReturn(Optional.empty());

        Exception exception = assertThrows(ScheduledTaskNotFound.class, () -> scheduledTaskService.resetTask(TASK_NAME));

        assertEquals(String.format("Scheduled task with taskName: [%s] not found.", TASK_NAME), exception.getMessage());
        verify(scheduledTaskRepository, never()).save(any(ScheduledTask.class));
    }

}
