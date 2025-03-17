package edu.iu.terracotta.runner.assignmentfilearchive;

import java.util.Optional;

import edu.iu.terracotta.runner.assignmentfilearchive.model.AssignmentFileArchiveScheduleResult;

public interface AssignmentFileArchiveSchedulerService {

    Optional<AssignmentFileArchiveScheduleResult> cleanup();

}
