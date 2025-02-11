package edu.iu.terracotta.service.app.dashboard.results;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.model.dto.dashboard.results.overview.ResultsOverviewDto;

public interface ResultsOverviewService {

    /**
     * Creates the "Descriptive Overview" data screen
     *
     * @param experiment
     * @param securedInfo
     * @return
     */
    ResultsOverviewDto overview(Experiment experiment, SecuredInfo securedInfo);

}
