package edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.condition;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class OutcomesConditions {

    private List<OutcomesCondition> rows;

}
