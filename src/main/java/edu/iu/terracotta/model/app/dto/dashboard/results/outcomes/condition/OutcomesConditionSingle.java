package edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.condition;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class OutcomesConditionSingle extends OutcomesCondition {

    public static final String CONDITION_SINGLE_TITLE = "Assignments with only one version";

}
