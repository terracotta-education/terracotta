package edu.iu.terracotta.service.app.dashboard.results;

import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.model.app.dto.dashboard.ResultsDashboardDto;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.request.ResultsOutcomesRequestDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;

public interface ResultsDashboardService {

    /**
     * Create the Results Dashboard Overview page
     *
     * @param experimentId
     * @param securedInfo
     * @return
     * @throws ExperimentNotMatchingException
     */
    ResultsDashboardDto overview(long experimentId, SecuredInfo securedInfo) throws ExperimentNotMatchingException;

    /**
     * Create the Results Dashboard Outcomes page
     *
     * @param experimentId
     * @param resultsOutcomesRequestDto
     * @return
     * @throws ExperimentNotMatchingException
     * @throws OutcomeNotMatchingException
     */
    ResultsDashboardDto outcomes(long experimentId, ResultsOutcomesRequestDto resultsOutcomesRequestDto) throws ExperimentNotMatchingException, OutcomeNotMatchingException;

}
