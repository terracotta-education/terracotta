package edu.iu.terracotta.service.app.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.entity.scheduledtask.ScheduledTask;
import edu.iu.terracotta.dao.repository.ScheduledTaskRepository;
import edu.iu.terracotta.exceptions.scheduledtask.ScheduledTaskNotFound;
import edu.iu.terracotta.service.app.ScheduledTaskService;

@Service
public class ScheduledTaskServiceImpl implements ScheduledTaskService {

    @Autowired private ScheduledTaskRepository scheduledTaskRepository;

    @Override
    public void resetTask(String taskName) throws ScheduledTaskNotFound {
        ScheduledTask scheduledtask = scheduledTaskRepository.findByTaskName(taskName)
            .orElseThrow(() -> new ScheduledTaskNotFound(String.format("Scheduled task with taskName: [%s] not found.", taskName)));

        scheduledtask.setPicked(false);
        scheduledtask.setPickedBy(null);

        scheduledTaskRepository.save(scheduledtask);
    }

}
