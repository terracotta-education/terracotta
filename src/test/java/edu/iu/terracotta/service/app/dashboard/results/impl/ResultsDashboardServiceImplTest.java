package edu.iu.terracotta.service.app.dashboard.results.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.dao.model.dto.dashboard.ResultsDashboardDto;
import edu.iu.terracotta.utils.TextConstants;

public class ResultsDashboardServiceImplTest extends BaseTest {

    @InjectMocks private ResultsDashboardServiceImpl resultsDashboardService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    void testOutcomes() throws ExperimentNotMatchingException, OutcomeNotMatchingException {
        ResultsDashboardDto ret = resultsDashboardService.outcomes(1L, resultsOutcomesRequestDto);

        assertNotNull(ret);
        assertEquals(1l, ret.getExperimentId());
        assertNotNull(ret.getOutcomes());
        assertNull(ret.getOverview());
    }

    @Test
    public void testOutcomesNoExperimentMatching() throws ExperimentNotMatchingException {
        when(experimentRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(ExperimentNotMatchingException.class, () -> { resultsDashboardService.outcomes(1L, resultsOutcomesRequestDto); });

        assertEquals(TextConstants.EXPERIMENT_NOT_MATCHING, exception.getMessage());
    }

    @Test
    void testOverview() throws ExperimentNotMatchingException {
        ResultsDashboardDto ret = resultsDashboardService.overview(1L, securedInfo);

        assertNotNull(ret);
        assertEquals(1l, ret.getExperimentId());
        assertNotNull(ret.getOverview());
        assertNull(ret.getOutcomes());
    }

    @Test
    public void testOverviewNoExperimentMatching() throws ExperimentNotMatchingException {
        when(experimentRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(ExperimentNotMatchingException.class, () -> { resultsDashboardService.overview(1L, securedInfo); });

        assertEquals(TextConstants.EXPERIMENT_NOT_MATCHING, exception.getMessage());
    }

}
