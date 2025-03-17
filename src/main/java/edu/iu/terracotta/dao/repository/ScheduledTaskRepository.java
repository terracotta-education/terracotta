package edu.iu.terracotta.dao.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.scheduledtask.ScheduledTask;
import edu.iu.terracotta.dao.entity.scheduledtask.ScheduledTaskId;

public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, ScheduledTaskId> {

    Optional<ScheduledTask> findByTaskName(String taskName);

}
