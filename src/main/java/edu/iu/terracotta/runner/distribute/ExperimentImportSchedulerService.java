package edu.iu.terracotta.runner.distribute;

import java.util.Optional;

import edu.iu.terracotta.runner.distribute.model.ExperimentImportScheduleResult;

public interface ExperimentImportSchedulerService {

    Optional<ExperimentImportScheduleResult> cleanup();

}
