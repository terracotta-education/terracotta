package edu.iu.terracotta.runner.distribute.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ExperimentImportScheduleResult {

    List<ExperimentImportScheduleMessage> processed;

}
