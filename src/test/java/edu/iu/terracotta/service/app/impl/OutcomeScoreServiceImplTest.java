package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.OutcomeScore;
import edu.iu.terracotta.dao.model.dto.OutcomeScoreDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidParticipantException;

import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

@SuppressWarnings("unchecked")
public class OutcomeScoreServiceImplTest extends BaseTest {

    @InjectMocks private OutcomeScoreServiceImpl outcomeScoreService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        when(outcomeScoreRepository.findByOutcomeScoreId(anyLong())).thenReturn(outcomeScore);
        when(participantRepository.findByIdAndExperiment_ExperimentId(anyLong(), anyLong())).thenReturn(Optional.of(participant));
        when(outcomeScoreRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void testUpdateOutcomeScoresBatchesExistingAndNewScoresIntoOneSaveAll() throws DataServiceException, InvalidParticipantException {
        OutcomeScoreDto existingScoreDto = OutcomeScoreDto.builder()
            .outcomeScoreId(1L)
            .participantId(1L)
            .outcomeId(1L)
            .scoreNumeric(5F)
            .build();

        OutcomeScoreDto newScoreDto = OutcomeScoreDto.builder()
            .participantId(2L)
            .outcomeId(1L)
            .scoreNumeric(3F)
            .build();

        outcomeScoreService.updateOutcomeScores(List.of(existingScoreDto, newScoreDto), 1L);

        // existing score is updated in place; no per-item save/saveAndFlush calls
        verify(outcomeScore).setScoreNumeric(5F);
        verify(outcomeScoreRepository, never()).save(any(OutcomeScore.class));
        verify(outcomeScoreRepository, never()).saveAndFlush(any(OutcomeScore.class));

        ArgumentCaptor<List<OutcomeScore>> outcomeScoresCaptor = ArgumentCaptor.forClass(List.class);
        verify(outcomeScoreRepository).saveAll(outcomeScoresCaptor.capture());

        List<OutcomeScore> savedOutcomeScores = outcomeScoresCaptor.getValue();
        assertEquals(2, savedOutcomeScores.size());
        assertEquals(outcomeScore, savedOutcomeScores.get(0));
        assertEquals(3F, savedOutcomeScores.get(1).getScoreNumeric());
    }

    @Test
    public void testUpdateOutcomeScoresValidatesParticipantForNewScores() {
        OutcomeScoreDto newScoreDto = OutcomeScoreDto.builder()
            .participantId(99L)
            .outcomeId(1L)
            .scoreNumeric(3F)
            .build();

        when(participantRepository.findByIdAndExperiment_ExperimentId(anyLong(), anyLong())).thenReturn(Optional.empty());

        assertThrows(
            InvalidParticipantException.class,
            () -> outcomeScoreService.updateOutcomeScores(List.of(newScoreDto), 1L)
        );

        verify(outcomeScoreRepository, never()).saveAll(anyList());
    }

    @Test
    public void testUpdateOutcomeScoresEmptyList() throws DataServiceException, InvalidParticipantException {
        outcomeScoreService.updateOutcomeScores(Collections.emptyList(), 1L);

        verify(outcomeScoreRepository).saveAll(Collections.emptyList());
    }

    @Test
    public void testGetOutcomeScores() {
        List<OutcomeScoreDto> result = outcomeScoreService.getOutcomeScores(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testGetOutcomeScoresEmpty() {
        when(outcomeScoreRepository.findByOutcome_OutcomeId(anyLong())).thenReturn(null);

        List<OutcomeScoreDto> result = outcomeScoreService.getOutcomeScores(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetOutcomeScore() {
        OutcomeScore result = outcomeScoreService.getOutcomeScore(1L);

        assertNotNull(result);
        verify(outcomeScoreRepository).findByOutcomeScoreId(1L);
    }

    @Test
    public void testToDto() {
        OutcomeScoreDto dto = outcomeScoreService.toDto(outcomeScore);

        assertNotNull(dto);
        assertEquals(outcome.getOutcomeId(), dto.getOutcomeId());
        assertEquals(participant.getParticipantId(), dto.getParticipantId());
    }

    @Test
    public void testPostOutcomeScoreIdInPost() {
        OutcomeScoreDto dto = OutcomeScoreDto.builder().outcomeScoreId(1L).build();

        assertThrows(IdInPostException.class, () -> outcomeScoreService.postOutcomeScore(dto, 1L, 1L));
    }

    @Test
    public void testPostOutcomeScoreInvalidParticipant() {
        OutcomeScoreDto dto = OutcomeScoreDto.builder().participantId(null).build();

        assertThrows(InvalidParticipantException.class, () -> outcomeScoreService.postOutcomeScore(dto, 1L, 1L));
    }

    @Test
    public void testPostOutcomeScoreHappyPath() throws Exception {
        OutcomeScoreDto dto = OutcomeScoreDto.builder().participantId(1L).outcomeId(1L).scoreNumeric(5F).build();
        when(outcomeScoreRepository.save(any(OutcomeScore.class))).thenReturn(outcomeScore);

        OutcomeScoreDto result = outcomeScoreService.postOutcomeScore(dto, 1L, 1L);

        assertNotNull(result);
        assertEquals(1L, dto.getOutcomeId());
        verify(outcomeScoreRepository).save(any(OutcomeScore.class));
    }

    @Test
    public void testPostOutcomeScoreFromDtoOutcomeNotFound() {
        OutcomeScoreDto dto = OutcomeScoreDto.builder().participantId(1L).outcomeId(99L).build();
        when(outcomeRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(DataServiceException.class, () -> outcomeScoreService.postOutcomeScore(dto, 1L, 1L));

        assertEquals("Error 105: Unable to create outcome score: The outcome for the outcome score does not exist.", exception.getMessage());
    }

    @Test
    public void testPostOutcomeScoreFromDtoParticipantNotFound() {
        OutcomeScoreDto dto = OutcomeScoreDto.builder().participantId(1L).outcomeId(1L).build();
        when(participantRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(DataServiceException.class, () -> outcomeScoreService.postOutcomeScore(dto, 1L, 1L));

        assertEquals("Error 105: Unable to create outcome score: The participant for the outcome score does not exist.", exception.getMessage());
    }

    @Test
    public void testUpdateOutcomeScoreSingle() {
        OutcomeScoreDto dto = OutcomeScoreDto.builder().scoreNumeric(9F).build();

        outcomeScoreService.updateOutcomeScore(1L, dto);

        verify(outcomeScore).setScoreNumeric(9F);
        verify(outcomeScoreRepository).saveAndFlush(outcomeScore);
    }

    @Test
    public void testDeleteById() {
        outcomeScoreService.deleteById(1L);

        verify(outcomeScoreRepository).deleteByOutcomeScoreId(1L);
    }

    @Test
    public void testValidateParticipantNullId() {
        Exception exception = assertThrows(InvalidParticipantException.class, () -> outcomeScoreService.validateParticipant(null, 1L));

        assertEquals("Error 105: Must include a valid participant id in the POST", exception.getMessage());
    }

    @Test
    public void testValidateParticipantNotBelongToExperiment() {
        when(participantRepository.findByIdAndExperiment_ExperimentId(anyLong(), anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(InvalidParticipantException.class, () -> outcomeScoreService.validateParticipant(1L, 1L));

        assertEquals("Error 109: The participant provided does not belong to this experiment.", exception.getMessage());
    }

    @Test
    public void testValidateParticipantValid() {
        assertDoesNotThrow(() -> outcomeScoreService.validateParticipant(1L, 1L));
    }

    @Test
    public void testBuildHeaders() {
        HttpHeaders headers = outcomeScoreService.buildHeaders(UriComponentsBuilder.newInstance(), 1L, 2L, 3L, 4L);

        assertNotNull(headers);
        assertNotNull(headers.getLocation());
        assertTrue(headers.getLocation().toString().contains("/api/experiments/1/exposures/2/outcomes/3/outcome_scores/4"));
    }

}
