package edu.iu.terracotta.model.app.dto.dashboard.results.overview.assignment;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OverviewAssignments {

    private List<OverviewAssignment> rows;

}
