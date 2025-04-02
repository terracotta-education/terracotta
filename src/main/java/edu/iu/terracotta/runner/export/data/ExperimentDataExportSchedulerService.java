package edu.iu.terracotta.runner.export.data;

import java.util.Optional;

import edu.iu.terracotta.runner.export.data.model.ExperimentDataExportScheduleResult;

public interface ExperimentDataExportSchedulerService {

    Optional<ExperimentDataExportScheduleResult> cleanup();

}
