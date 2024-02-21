package edu.iu.terracotta.model.app.dto.dashboard.results.overview.assignment;

import edu.iu.terracotta.model.app.dto.dashboard.results.overview.Overview;
import edu.iu.terracotta.model.app.dto.dashboard.results.overview.assignment.treatment.OverviewTreatments;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class OverviewAssignment extends Overview {

    private boolean open;
    private OverviewTreatments treatments;

}
