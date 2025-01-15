package edu.iu.terracotta.service.app.dashboard.results;

import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.ResultsOutcomesDto;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.request.ResultsOutcomesRequestDto;

public interface ResultsOutcomesService {

    /**
     * Creates the "Outcome Outcomes" data screen
     *
     * @param experiment
     * @param resultsOutcomesRequestDto
     * @return
     * @throws OutcomeNotMatchingException
     */
    ResultsOutcomesDto outcomes(Experiment experiment, ResultsOutcomesRequestDto resultsOutcomesRequestDto) throws OutcomeNotMatchingException;

}
