package edu.iu.terracotta.service.app.dashboard.results;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.dao.model.dto.dashboard.ResultsDashboardDto;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.request.ResultsOutcomesRequestDto;

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
