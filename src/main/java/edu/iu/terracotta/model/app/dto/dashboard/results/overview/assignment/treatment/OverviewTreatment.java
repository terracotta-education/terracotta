package edu.iu.terracotta.model.app.dto.dashboard.results.overview.assignment.treatment;

import edu.iu.terracotta.model.app.dto.dashboard.results.overview.Overview;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class OverviewTreatment extends Overview {

    private Long assignmentId;
    private Long conditionId;

}
