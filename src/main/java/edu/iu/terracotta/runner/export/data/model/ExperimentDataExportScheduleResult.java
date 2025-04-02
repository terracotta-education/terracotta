package edu.iu.terracotta.runner.export.data.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ExperimentDataExportScheduleResult {

    private List<ExperimentDataExportScheduleMessage> processed;

}
