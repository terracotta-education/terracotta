package edu.iu.terracotta.service.app.dashboard.results;

import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.dto.dashboard.results.overview.ResultsOverviewDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;

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
