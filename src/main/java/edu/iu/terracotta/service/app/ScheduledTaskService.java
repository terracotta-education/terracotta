package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.scheduledtask.ScheduledTaskNotFound;

public interface ScheduledTaskService {

    void resetTask(String taskName) throws ScheduledTaskNotFound;

}
