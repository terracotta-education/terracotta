package edu.iu.terracotta.dao.model.dto.dashboard.results.overview.condition;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class OverviewConditionSingle extends OverviewCondition {

    public static final String CONDITION_SINGLE_TITLE = "Components with only one version";

}
