package edu.iu.terracotta.dao.model.dto.dashboard.results.overview.assignment.treatment;

import edu.iu.terracotta.dao.model.dto.dashboard.results.overview.Overview;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@SuperBuilder
public class OverviewTreatment extends Overview {

    private UUID assignmentId;
    private UUID conditionId;

}
