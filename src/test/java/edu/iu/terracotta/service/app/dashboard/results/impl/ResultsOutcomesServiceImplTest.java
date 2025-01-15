package edu.iu.terracotta.service.app.dashboard.results.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.ResultsOutcomesDto;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.enums.AlternateIdType;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.enums.OutcomeType;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.exposure.OutcomesExposureOverall;
import edu.iu.terracotta.utils.TextConstants;

public class ResultsOutcomesServiceImplTest extends BaseTest {

    @InjectMocks private ResultsOutcomesServiceImpl resultsOutcomesService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        when(assessmentRepository.findByTreatment_Assignment_AssignmentId(anyLong())).thenReturn(Collections.singletonList(assessment));
        when(exposureGroupConditionRepository.findByCondition_Experiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(exposureGroupCondition));
        when(participantRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(participant));

        when(outcome.getMaxPoints()).thenReturn(10F);
        when(outcome.getOutcomeScores()).thenReturn(Collections.singletonList(outcomeScore));
        when(outcomeScore.getScoreNumeric()).thenReturn(5F);
    }

    @Test
    public void testOutcomes() throws OutcomeNotMatchingException {
        ResultsOutcomesDto ret = resultsOutcomesService.outcomes(experiment, resultsOutcomesRequestDto);

        assertNotNull(ret);
        assertNotNull(ret.getExperimentId());
        assertEquals(1L, ret.getExperimentId());

        assertNotNull(ret.getConditions());
        assertNotNull(ret.getConditions().getRows());
        assertEquals(1, ret.getConditions().getRows().size());
        assertEquals(CONDITION_TITLE, ret.getConditions().getRows().get(0).getTitle());
        assertEquals(0.5, ret.getConditions().getRows().get(0).getMean());
        assertEquals(1, ret.getConditions().getRows().get(0).getNumber());
        assertEquals(0.0, ret.getConditions().getRows().get(0).getStandardDeviation());

        assertNotNull(ret.getExposures());
        assertNotNull(ret.getExposures().getRows());
        assertEquals(2, ret.getExposures().getRows().size());
        assertEquals(0.5, ret.getExposures().getRows().get(0).getMean());
        assertEquals(1, ret.getExposures().getRows().get(0).getNumber());
        assertEquals(0.0, ret.getExposures().getRows().get(0).getStandardDeviation());
        assertEquals(EXPOSURE_TITLE, ret.getExposures().getRows().get(0).getTitle());
        assertEquals(0.5, ret.getExposures().getRows().get(1).getMean());
        assertEquals(1, ret.getExposures().getRows().get(1).getNumber());
        assertEquals(0.0, ret.getExposures().getRows().get(1).getStandardDeviation());
        assertEquals(OutcomesExposureOverall.EXPOSURE_OVERALL_TITLE, ret.getExposures().getRows().get(1).getTitle());
    }

    @Test
    public void testOutcomesExternal() throws OutcomeNotMatchingException {
        when(outcome.getExternal()).thenReturn(true);

        ResultsOutcomesDto ret = resultsOutcomesService.outcomes(experiment, resultsOutcomesRequestDto);

        assertNotNull(ret);
        assertNotNull(ret.getExperimentId());
        assertEquals(1L, ret.getExperimentId());

        assertNotNull(ret.getConditions());
        assertNotNull(ret.getConditions().getRows());
        assertEquals(1, ret.getConditions().getRows().size());
        assertEquals(CONDITION_TITLE, ret.getConditions().getRows().get(0).getTitle());
        assertEquals(1.0, ret.getConditions().getRows().get(0).getMean());
        assertEquals(1, ret.getConditions().getRows().get(0).getNumber());
        assertEquals(0.0, ret.getConditions().getRows().get(0).getStandardDeviation());

        assertNotNull(ret.getExposures());
        assertNotNull(ret.getExposures().getRows());
        assertEquals(2, ret.getExposures().getRows().size());
        assertEquals(1.0, ret.getExposures().getRows().get(0).getMean());
        assertEquals(1, ret.getExposures().getRows().get(0).getNumber());
        assertEquals(0.0, ret.getExposures().getRows().get(0).getStandardDeviation());
        assertEquals(EXPOSURE_TITLE, ret.getExposures().getRows().get(0).getTitle());
        assertEquals(1.0, ret.getExposures().getRows().get(1).getMean());
        assertEquals(1, ret.getExposures().getRows().get(1).getNumber());
        assertEquals(0.0, ret.getExposures().getRows().get(1).getStandardDeviation());
        assertEquals(OutcomesExposureOverall.EXPOSURE_OVERALL_TITLE, ret.getExposures().getRows().get(1).getTitle());
    }

    @Test
    public void testOutcomesNoOutcomeForId() throws OutcomeNotMatchingException {
        when(outcomeRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(OutcomeNotMatchingException.class, () -> { resultsOutcomesService.outcomes(experiment, resultsOutcomesRequestDto); });

        assertEquals(TextConstants.OUTCOME_NOT_MATCHING, exception.getMessage());
    }

    @Test
    public void testOutcomesOutcomeNotInExperiment() throws OutcomeNotMatchingException {
        when(outcomeRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(OutcomeNotMatchingException.class, () -> { resultsOutcomesService.outcomes(experiment, resultsOutcomesRequestDto); });

        assertEquals(TextConstants.OUTCOME_NOT_MATCHING, exception.getMessage());
    }

    @Test
    public void testOutcomesNoOutcomeScores() throws OutcomeNotMatchingException {
        when(outcome.getOutcomeScores()).thenReturn(Collections.emptyList());

        ResultsOutcomesDto ret = resultsOutcomesService.outcomes(experiment, resultsOutcomesRequestDto);

        assertNotNull(ret);
        assertNotNull(ret.getExperimentId());
        assertEquals(1L, ret.getExperimentId());

        assertNotNull(ret.getConditions());
        assertNotNull(ret.getConditions().getRows());
        assertEquals(1, ret.getConditions().getRows().size());
        assertEquals(CONDITION_TITLE, ret.getConditions().getRows().get(0).getTitle());
        assertEquals(0.0, ret.getConditions().getRows().get(0).getMean());
        assertEquals(0, ret.getConditions().getRows().get(0).getNumber());
        assertEquals(0.0, ret.getConditions().getRows().get(0).getStandardDeviation());

        assertNotNull(ret.getExposures());
        assertNotNull(ret.getExposures().getRows());
        assertEquals(2, ret.getExposures().getRows().size());
        assertEquals(0.0, ret.getExposures().getRows().get(0).getMean());
        assertEquals(0, ret.getExposures().getRows().get(0).getNumber());
        assertEquals(0.0, ret.getExposures().getRows().get(0).getStandardDeviation());
        assertEquals(EXPOSURE_TITLE, ret.getExposures().getRows().get(0).getTitle());
        assertEquals(0.0, ret.getExposures().getRows().get(1).getMean());
        assertEquals(0, ret.getExposures().getRows().get(1).getNumber());
        assertEquals(0.0, ret.getExposures().getRows().get(1).getStandardDeviation());
        assertEquals(OutcomesExposureOverall.EXPOSURE_OVERALL_TITLE, ret.getExposures().getRows().get(1).getTitle());
    }

    @Test
    public void testOutcomesNoExposureGroupCondition() throws OutcomeNotMatchingException {
        when(exposure.getExposureId()).thenReturn(1L, 2L, 3L);

        ResultsOutcomesDto ret = resultsOutcomesService.outcomes(experiment, resultsOutcomesRequestDto);

        assertNotNull(ret);
        assertNotNull(ret.getExperimentId());
        assertEquals(1L, ret.getExperimentId());

        assertNotNull(ret.getConditions());
        assertNotNull(ret.getConditions().getRows());
        assertEquals(1, ret.getConditions().getRows().size());
        assertEquals(CONDITION_TITLE, ret.getConditions().getRows().get(0).getTitle());
        assertEquals(0.0, ret.getConditions().getRows().get(0).getMean());
        assertEquals(0, ret.getConditions().getRows().get(0).getNumber());
        assertEquals(0.0, ret.getConditions().getRows().get(0).getStandardDeviation());

        assertNotNull(ret.getExposures());
        assertNotNull(ret.getExposures().getRows());
        assertEquals(2, ret.getExposures().getRows().size());
        assertEquals(0.5, ret.getExposures().getRows().get(0).getMean());
        assertEquals(1, ret.getExposures().getRows().get(0).getNumber());
        assertEquals(0.0, ret.getExposures().getRows().get(0).getStandardDeviation());
        assertEquals(EXPOSURE_TITLE, ret.getExposures().getRows().get(0).getTitle());
        assertEquals(0.5, ret.getExposures().getRows().get(1).getMean());
        assertEquals(1, ret.getExposures().getRows().get(1).getNumber());
        assertEquals(0.0, ret.getExposures().getRows().get(1).getStandardDeviation());
        assertEquals(OutcomesExposureOverall.EXPOSURE_OVERALL_TITLE, ret.getExposures().getRows().get(1).getTitle());
    }

    @Test
    public void testOutcomesNoConsentedParticipantsInGroup() throws OutcomeNotMatchingException {
        when(group.getGroupId()).thenReturn(1L, 2L);

        ResultsOutcomesDto ret = resultsOutcomesService.outcomes(experiment, resultsOutcomesRequestDto);

        assertNotNull(ret);
        assertNotNull(ret.getExperimentId());
        assertEquals(1L, ret.getExperimentId());

        assertNotNull(ret.getConditions());
        assertNotNull(ret.getConditions().getRows());
        assertEquals(1, ret.getConditions().getRows().size());
        assertEquals(CONDITION_TITLE, ret.getConditions().getRows().get(0).getTitle());
        assertEquals(0.0, ret.getConditions().getRows().get(0).getMean());
        assertEquals(0, ret.getConditions().getRows().get(0).getNumber());
        assertEquals(0.0, ret.getConditions().getRows().get(0).getStandardDeviation());

        assertNotNull(ret.getExposures());
        assertNotNull(ret.getExposures().getRows());
        assertEquals(2, ret.getExposures().getRows().size());
        assertEquals(0.5, ret.getExposures().getRows().get(0).getMean());
        assertEquals(1, ret.getExposures().getRows().get(0).getNumber());
        assertEquals(0.0, ret.getExposures().getRows().get(0).getStandardDeviation());
        assertEquals(EXPOSURE_TITLE, ret.getExposures().getRows().get(0).getTitle());
        assertEquals(0.5, ret.getExposures().getRows().get(1).getMean());
        assertEquals(1, ret.getExposures().getRows().get(1).getNumber());
        assertEquals(0.0, ret.getExposures().getRows().get(1).getStandardDeviation());
        assertEquals(OutcomesExposureOverall.EXPOSURE_OVERALL_TITLE, ret.getExposures().getRows().get(1).getTitle());
    }

    @Test
    public void testOutcomesAlternateIdAverageAssignmentScore() throws OutcomeNotMatchingException {
        when(resultsOutcomesRequestDto.getOutcomeIds()).thenReturn(null);
        when(resultsOutcomesRequestDto.getAlternateId()).thenReturn(alternateIdDto);

        ResultsOutcomesDto ret = resultsOutcomesService.outcomes(experiment, resultsOutcomesRequestDto);

        assertNotNull(ret);
        assertNotNull(ret.getExperimentId());
        assertEquals(1L, ret.getExperimentId());
        assertEquals(OutcomeType.AVERAGE_ASSIGNMENT_SCORE, ret.getOutcomeType());

        assertNotNull(ret.getConditions());
        assertNotNull(ret.getConditions().getRows());
        assertEquals(1, ret.getConditions().getRows().size());
        assertEquals(OUTCOME_TITLE, ret.getConditions().getRows().get(0).getTitle());
        assertEquals(0.0, ret.getConditions().getRows().get(0).getMean());
        assertEquals(0, ret.getConditions().getRows().get(0).getNumber());
        assertEquals(0.0, ret.getConditions().getRows().get(0).getStandardDeviation());

        assertNotNull(ret.getExposures());
        assertNotNull(ret.getExposures().getRows());
        assertEquals(1, ret.getExposures().getRows().size());
        assertEquals(0.0, ret.getExposures().getRows().get(0).getMean());
        assertEquals(0, ret.getExposures().getRows().get(0).getNumber());
        assertEquals(0.0, ret.getExposures().getRows().get(0).getStandardDeviation());
        assertEquals(EXPOSURE_TITLE, ret.getExposures().getRows().get(0).getTitle());
        assertEquals(OutcomesExposureOverall.EXPOSURE_OVERALL_TITLE, ret.getExposures().getRows().get(0).getTitle());
    }

    @Test
    public void testOutcomesAlternateIdTimeOnTask() throws OutcomeNotMatchingException {
        when(resultsOutcomesRequestDto.getOutcomeIds()).thenReturn(null);
        when(resultsOutcomesRequestDto.getAlternateId()).thenReturn(alternateIdDto);
        when(alternateIdDto.getId()).thenReturn(AlternateIdType.TIME_ON_TASK.name());

        ResultsOutcomesDto ret = resultsOutcomesService.outcomes(experiment, resultsOutcomesRequestDto);

        assertNotNull(ret);
        assertNotNull(ret.getExperimentId());
        assertEquals(1L, ret.getExperimentId());
        assertEquals(OutcomeType.TIME_ON_TASK, ret.getOutcomeType());

        assertNotNull(ret.getConditions());
        assertNotNull(ret.getConditions().getRows());
        assertEquals(1, ret.getConditions().getRows().size());
        assertEquals(OUTCOME_TITLE, ret.getConditions().getRows().get(0).getTitle());
        assertEquals(0.0, ret.getConditions().getRows().get(0).getMean());
        assertEquals(0, ret.getConditions().getRows().get(0).getNumber());
        assertEquals(0.0, ret.getConditions().getRows().get(0).getStandardDeviation());

        assertNotNull(ret.getExposures());
        assertNotNull(ret.getExposures().getRows());
        assertEquals(1, ret.getExposures().getRows().size());
        assertEquals(0.0, ret.getExposures().getRows().get(0).getMean());
        assertEquals(0, ret.getExposures().getRows().get(0).getNumber());
        assertEquals(0.0, ret.getExposures().getRows().get(0).getStandardDeviation());
        assertEquals(EXPOSURE_TITLE, ret.getExposures().getRows().get(0).getTitle());
        assertEquals(OutcomesExposureOverall.EXPOSURE_OVERALL_TITLE, ret.getExposures().getRows().get(0).getTitle());
    }

    @Test
    public void testOutcomesNoOutcomesAllNull() throws OutcomeNotMatchingException {
        when(resultsOutcomesRequestDto.getOutcomeIds()).thenReturn(null);
        when(resultsOutcomesRequestDto.getAlternateId()).thenReturn(alternateIdDto);
        when(alternateIdDto.getId()).thenReturn(null);
        when(alternateIdDto.getExposures()).thenReturn(null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> { resultsOutcomesService.outcomes(experiment, resultsOutcomesRequestDto); });

        assertEquals("Exception occurred processing outcomes for experiment ID: [1]. Request must contain at least one outcome.", exception.getMessage());
    }

    @Test
    public void testOutcomesNoOutcomesNoOutcomeIdsOnlyAlternateId() throws OutcomeNotMatchingException {
        when(resultsOutcomesRequestDto.getOutcomeIds()).thenReturn(null);
        when(resultsOutcomesRequestDto.getAlternateId()).thenReturn(alternateIdDto);
        when(alternateIdDto.getId()).thenReturn(AlternateIdType.AVERAGE_ASSIGNMENT_SCORE.name());
        when(alternateIdDto.getExposures()).thenReturn(null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> { resultsOutcomesService.outcomes(experiment, resultsOutcomesRequestDto); });

        assertEquals("Exception occurred processing outcomes for experiment ID: [1]. Request must contain at least one outcome.", exception.getMessage());
    }

    @Test
    public void testOutcomesNoOutcomesNoOutcomeIdsOnlyAlternateExposures() throws OutcomeNotMatchingException {
        when(resultsOutcomesRequestDto.getOutcomeIds()).thenReturn(null);
        when(resultsOutcomesRequestDto.getAlternateId()).thenReturn(alternateIdDto);
        when(alternateIdDto.getId()).thenReturn(null);
        when(alternateIdDto.getExposures()).thenReturn(Collections.singletonList(1L));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> { resultsOutcomesService.outcomes(experiment, resultsOutcomesRequestDto); });

        assertEquals("Exception occurred processing outcomes for experiment ID: [1]. Request must contain at least one outcome.", exception.getMessage());
    }

}
