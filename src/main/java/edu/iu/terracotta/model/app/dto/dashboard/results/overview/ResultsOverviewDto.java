package edu.iu.terracotta.model.app.dto.dashboard.results.overview;

import edu.iu.terracotta.model.app.dto.dashboard.results.overview.assignment.OverviewAssignments;
import edu.iu.terracotta.model.app.dto.dashboard.results.overview.condition.OverviewConditions;
import edu.iu.terracotta.model.app.dto.dashboard.results.overview.participant.OverviewParticipant;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class ResultsOverviewDto {

    private OverviewAssignments assignments;
    private OverviewConditions conditions;
    private OverviewParticipant participants;

}
