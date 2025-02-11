package edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes;

import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.condition.OutcomesConditions;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.enums.OutcomeType;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.exposure.OutcomesExposures;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ResultsOutcomesDto {

    private long experimentId;
    private OutcomesConditions conditions;
    private OutcomesExposures exposures;
    private OutcomeType outcomeType;

}
