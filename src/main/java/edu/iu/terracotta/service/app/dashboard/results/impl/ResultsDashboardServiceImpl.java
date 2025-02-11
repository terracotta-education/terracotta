package edu.iu.terracotta.service.app.dashboard.results.impl;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.dao.model.dto.dashboard.ResultsDashboardDto;
import edu.iu.terracotta.dao.model.dto.dashboard.ResultsDashboardDto.ResultsDashboardDtoBuilder;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.request.ResultsOutcomesRequestDto;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.service.app.dashboard.results.ResultsOutcomesService;
import edu.iu.terracotta.service.app.dashboard.results.ResultsDashboardService;
import edu.iu.terracotta.service.app.dashboard.results.ResultsOverviewService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"rawtypes", "PMD.GuardLogStatement"})
public class ResultsDashboardServiceImpl implements ResultsDashboardService {

    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private ResultsOutcomesService resultsOutcomesService;
    @Autowired private ResultsOverviewService resultsOverviewService;

    @Override
    public ResultsDashboardDto overview(long experimentId, SecuredInfo securedInfo) throws ExperimentNotMatchingException {
        log.info("Starting results overview dashboard calculations for experiment ID: [{}]", experimentId);
        Optional<Experiment> experiment = experimentRepository.findById(experimentId);

        if (experiment.isEmpty()) {
            throw new ExperimentNotMatchingException(TextConstants.EXPERIMENT_NOT_MATCHING);
        }

        ResultsDashboardDtoBuilder resultsDashboardDto = ResultsDashboardDto.builder()
            .experimentId(experimentId)
            .overview(resultsOverviewService.overview(experiment.get(), securedInfo));

        log.info("Finished results overview dashboard calculations for experiment ID: [{}]", experimentId);

        return resultsDashboardDto.build();
    }

    @Override
    public ResultsDashboardDto outcomes(long experimentId, ResultsOutcomesRequestDto resultsOutcomesRequestDto) throws ExperimentNotMatchingException, OutcomeNotMatchingException {
        log.info("Starting results outcomes dashboard calculations for experiment ID: [{}], outcomes IDs: [{}], alternate ID: [{}]",
            experimentId,
            resultsOutcomesRequestDto.getOutcomeIds(),
            resultsOutcomesRequestDto.getAlternateId().getId()
        );
        Optional<Experiment> experiment = experimentRepository.findById(experimentId);

        if (experiment.isEmpty()) {
            throw new ExperimentNotMatchingException(TextConstants.EXPERIMENT_NOT_MATCHING);
        }

        ResultsDashboardDtoBuilder resultsDashboardDto = ResultsDashboardDto.builder()
            .experimentId(experimentId)
            .outcomes(resultsOutcomesService.outcomes(experiment.get(), resultsOutcomesRequestDto));

        log.info("Finished results outcomes dashboard calculations for experiment ID: [{}], outcomes IDs: [{}], alternate ID: [{}]",
            experimentId,
            resultsOutcomesRequestDto.getOutcomeIds(),
            resultsOutcomesRequestDto.getAlternateId().getId()
        );

        return resultsDashboardDto.build();
    }

}
