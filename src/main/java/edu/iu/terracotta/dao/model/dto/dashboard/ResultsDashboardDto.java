package edu.iu.terracotta.dao.model.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;

import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.ResultsOutcomesDto;
import edu.iu.terracotta.dao.model.dto.dashboard.results.overview.ResultsOverviewDto;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@SuperBuilder
@JsonInclude(NON_NULL)
public class ResultsDashboardDto {

    private long experimentId;
    private ResultsOutcomesDto outcomes;
    private ResultsOverviewDto overview;

}
