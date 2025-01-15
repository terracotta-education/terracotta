package edu.iu.terracotta.dao.model.dto.dashboard.results.overview.condition;

import java.util.List;

import edu.iu.terracotta.dao.model.enums.ExposureTypes;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OverviewConditions {

    private List<OverviewCondition> rows;
    private ExposureTypes exposureType;

}
