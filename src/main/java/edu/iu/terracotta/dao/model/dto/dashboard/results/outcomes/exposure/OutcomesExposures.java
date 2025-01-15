package edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.exposure;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class OutcomesExposures {

    private List<OutcomesExposure> rows;

}
