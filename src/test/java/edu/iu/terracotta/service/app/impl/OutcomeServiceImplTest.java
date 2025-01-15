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

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Outcome;
import edu.iu.terracotta.dao.entity.OutcomeScore;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.model.dto.OutcomeDto;
import edu.iu.terracotta.dao.model.dto.OutcomePotentialDto;
import edu.iu.terracotta.dao.model.enums.LmsType;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;

public class OutcomeServiceImplTest extends BaseTest {

    @InjectMocks private OutcomeServiceImpl outcomeService;

    @BeforeEach
    public void beforeEach() {
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
    public void testUpdateOutcomeGrades() throws IOException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException, ApiException, NumberFormatException, TerracottaConnectorException {
        outcomeService.updateOutcomeGrades(1l, securedInfo, false);

        verify(outcomeScoreRepository, never()).save(any(OutcomeScore.class));
    }

    @Test
    public void testUpdateOutcomeGradesNoEmail() throws IOException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException, ApiException, NumberFormatException, TerracottaConnectorException {
        when(ltiUserEntity.getEmail()).thenReturn(null);

        outcomeService.updateOutcomeGrades(1l, securedInfo, false);

        verify(outcomeScoreRepository, never()).save(any(OutcomeScore.class));
    }

    @Test
    public void testUpdateOutcomeGradesNoScores() throws IOException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException, ApiException, NumberFormatException, TerracottaConnectorException {
        when(outcome.getOutcomeScores()).thenReturn(Collections.emptyList());

        outcomeService.updateOutcomeGrades(1l, securedInfo, false);

        verify(outcomeScoreRepository).save(any(OutcomeScore.class));
    }

    @Test
    public void testUpdateOutcomeGradesNoScoresEmailNull() throws IOException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException, ApiException, NumberFormatException, TerracottaConnectorException {
        when(outcome.getOutcomeScores()).thenReturn(Collections.emptyList());
        when(ltiUserEntity.getEmail()).thenReturn(null);

        outcomeService.updateOutcomeGrades(1l, securedInfo, false);

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
    public void testPotentialOutcomes() throws DataServiceException, ApiException, TerracottaConnectorException {
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

    @Test
    public void testGetAllByExperiment() {
        List<OutcomeDto> retVal = outcomeService.getAllByExperiment(0);

        assertNotNull(retVal);
        assertEquals(1, retVal.size());
    }

}
