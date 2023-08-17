package edu.iu.terracotta.model.app.dto.dashboard.results.overview.condition;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class OverviewConditionSingle extends OverviewCondition {

    public static final String CONDITION_SINGLE_TITLE = "Assignments with only one version";

}
