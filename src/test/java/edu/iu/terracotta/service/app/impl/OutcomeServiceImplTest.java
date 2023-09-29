package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.BaseTest;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.Outcome;
import edu.iu.terracotta.model.app.OutcomeScore;
import edu.iu.terracotta.model.app.dto.OutcomeDto;
import edu.iu.terracotta.model.app.dto.OutcomePotentialDto;
import edu.iu.terracotta.model.app.enumerator.LmsType;

public class OutcomeServiceImplTest extends BaseTest {

    @InjectMocks private OutcomeServiceImpl outcomeService;

    @BeforeEach
    public void beforeEach() throws ParticipantNotUpdatedException, ExperimentNotMatchingException {
        MockitoAnnotations.openMocks(this);

        setup();

        when(outcomeRepository.existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndOutcomeId(anyLong(), anyLong(), anyLong())).thenReturn(true);

        when(outcome.getExternal()).thenReturn(true);
        when(outcome.getLmsType()).thenReturn(LmsType.none);
        when(outcomeDto.getOutcomeId()).thenReturn(null);
    }

    @Test
    public void testGetOutcomesForExposure() {
        List<OutcomeDto> retVal = outcomeService.getOutcomesForExposure(1l);

        assertEquals(1, retVal.size());
    }

    @Test
    public void testUpdateOutcomeGrades() throws CanvasApiException, IOException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException {
        outcomeService.updateOutcomeGrades(1l, securedInfo);

        verify(outcomeScoreRepository, never()).save(any(OutcomeScore.class));
    }

    @Test
    public void testUpdateOutcomeGradesNoEmail() throws CanvasApiException, IOException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException {
        when(ltiUserEntity.getEmail()).thenReturn(null);

        outcomeService.updateOutcomeGrades(1l, securedInfo);

        verify(outcomeScoreRepository, never()).save(any(OutcomeScore.class));
    }

    @Test
    public void testUpdateOutcomeGradesNoScores() throws CanvasApiException, IOException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException {
        when(outcome.getOutcomeScores()).thenReturn(Collections.emptyList());

        outcomeService.updateOutcomeGrades(1l, securedInfo);

        verify(outcomeScoreRepository).save(any(OutcomeScore.class));
    }

    @Test
    public void testUpdateOutcomeGradesNoScoresEmailNull() throws CanvasApiException, IOException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException {
        when(outcome.getOutcomeScores()).thenReturn(Collections.emptyList());
        when(ltiUserEntity.getEmail()).thenReturn(null);

        outcomeService.updateOutcomeGrades(1l, securedInfo);

        verify(outcomeScoreRepository).save(any(OutcomeScore.class));
    }

    @Test
    public void testDefaultOutcome() {
        assertDoesNotThrow(() -> {
            outcomeService.defaultOutcome(outcomeDto);
        });
    }

    @Test
    public void testBuildHeaders() {
        HttpHeaders retVal = outcomeService.buildHeaders(UriComponentsBuilder.newInstance(), 0, 0, 0);

        assertNotNull(retVal);
    }

    @Test
    public void testPostOutcome() throws IdInPostException, DataServiceException, TitleValidationException {
        OutcomeDto retVal = outcomeService.postOutcome(outcomeDto, 0);

        assertNotNull(retVal);
    }

    @Test
    public void testGetOutcome() {
        Outcome retVal = outcomeService.getOutcome(0l);

        assertNotNull(retVal);
    }

    @Test
    public void testUpdateOutcome() throws TitleValidationException {
        when(outcome.getExternal()).thenReturn(null);
        outcomeService.updateOutcome(0, outcomeDto);

        verify(outcomeRepository).saveAndFlush(any(Outcome.class));
    }

    @Test
    public void testUpdateOutcomeIsExternal() throws TitleValidationException {
        when(outcome.getExternal()).thenReturn(null);
        when(outcomeDto.getExternal()).thenReturn(true);
        outcomeService.updateOutcome(0, outcomeDto);

        verify(outcomeRepository).saveAndFlush(any(Outcome.class));
    }

    @Test
    public void testPotentialOutcomes() throws DataServiceException, CanvasApiException {
        List<OutcomePotentialDto> retVal = outcomeService.potentialOutcomes(0, securedInfo);

        assertNotNull(retVal);
    }

    @Test
    public void testDeleteById() {
        assertDoesNotThrow(() -> {
            outcomeService.deleteById(0);
        });
    }

    @Test
    public void testToDto() {
        OutcomeDto retVal = outcomeService.toDto(outcome, true);

        assertNotNull(retVal);
    }
}
