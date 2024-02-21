package edu.iu.terracotta.service.app.dashboard.results;

import edu.iu.terracotta.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.ResultsOutcomesDto;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.request.ResultsOutcomesRequestDto;

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
