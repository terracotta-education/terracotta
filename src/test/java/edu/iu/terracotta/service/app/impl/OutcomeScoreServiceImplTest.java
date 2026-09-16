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
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.OutcomeScore;
import edu.iu.terracotta.dao.exceptions.OutcomeScoreNotMatchingException;
import edu.iu.terracotta.dao.model.dto.OutcomeScoreDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidParticipantException;

import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

@SuppressWarnings("unchecked")
public class OutcomeScoreServiceImplTest extends BaseTest {

    // outcome.getUuid() / outcomeScore.getUuid() aren't globally stubbed in BaseModelTest (unlike
    // experiment.getUuid()), so they - and the matching repository findByUuid lookups - are stubbed
    // here so fromDto's uuid-based resolution keeps working for the existing numeric-id test data.
    private static final UUID OUTCOME_UUID = UUID.randomUUID();
    private static final UUID OUTCOME_SCORE_UUID = UUID.randomUUID();

    @InjectMocks private OutcomeScoreServiceImpl outcomeScoreService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        when(outcomeScoreRepository.findByOutcomeScoreId(anyLong())).thenReturn(outcomeScore);
        when(outcomeScoreRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(outcome.getUuid()).thenReturn(OUTCOME_UUID);
        when(outcomeRepository.findByUuid(OUTCOME_UUID)).thenReturn(outcome);
        when(outcomeScoreRepository.findByUuid(OUTCOME_SCORE_UUID)).thenReturn(outcomeScore);
    }

    @Test
    public void testUpdateOutcomeScoresBatchesExistingAndNewScoresIntoOneSaveAll() throws DataServiceException, InvalidParticipantException {
        OutcomeScoreDto existingScoreDto = OutcomeScoreDto.builder()
            .outcomeScoreId(OUTCOME_SCORE_UUID)
            .participantId(UUID.randomUUID())
            .outcomeId(OUTCOME_UUID)
            .scoreNumeric(5F)
            .build();

        OutcomeScoreDto newScoreDto = OutcomeScoreDto.builder()
            .participantId(UUID.randomUUID())
            .outcomeId(OUTCOME_UUID)
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
        UUID notFoundParticipantUuid = UUID.randomUUID();
        OutcomeScoreDto newScoreDto = OutcomeScoreDto.builder()
            .participantId(notFoundParticipantUuid)
            .outcomeId(OUTCOME_UUID)
            .scoreNumeric(3F)
            .build();

        when(participantRepository.findByUuid(notFoundParticipantUuid)).thenReturn(Optional.empty());

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
        assertEquals(OUTCOME_UUID, dto.getOutcomeId());
        assertEquals(participant.getUuid(), dto.getParticipantId());
    }

    @Test
    public void testPostOutcomeScoreIdInPost() {
        OutcomeScoreDto dto = OutcomeScoreDto.builder().outcomeScoreId(UUID.randomUUID()).build();

        assertThrows(IdInPostException.class, () -> outcomeScoreService.postOutcomeScore(dto, 1L, 1L));
    }

    @Test
    public void testPostOutcomeScoreInvalidParticipant() {
        OutcomeScoreDto dto = OutcomeScoreDto.builder().participantId(null).build();

        assertThrows(InvalidParticipantException.class, () -> outcomeScoreService.postOutcomeScore(dto, 1L, 1L));
    }

    @Test
    public void testPostOutcomeScoreHappyPath() throws Exception {
        // outcomeId is overwritten by postOutcomeScore itself (resolved from the numeric outcomeId
        // argument via the OUTCOME_UUID stub in beforeEach), so the builder value here is a placeholder.
        OutcomeScoreDto dto = OutcomeScoreDto.builder().participantId(UUID.randomUUID()).outcomeId(OUTCOME_UUID).scoreNumeric(5F).build();
        when(outcomeScoreRepository.save(any(OutcomeScore.class))).thenReturn(outcomeScore);

        OutcomeScoreDto result = outcomeScoreService.postOutcomeScore(dto, 1L, 1L);

        assertNotNull(result);
        assertEquals(OUTCOME_UUID, dto.getOutcomeId());
        verify(outcomeScoreRepository).save(any(OutcomeScore.class));
    }

    @Test
    public void testPostOutcomeScoreFromDtoOutcomeNotFound() {
        OutcomeScoreDto dto = OutcomeScoreDto.builder().participantId(UUID.randomUUID()).outcomeId(OUTCOME_UUID).build();
        when(outcomeRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(DataServiceException.class, () -> outcomeScoreService.postOutcomeScore(dto, 1L, 1L));

        assertEquals("Error 105: Unable to create outcome score: The outcome for the outcome score does not exist.", exception.getMessage());
    }

    @Test
    public void testFromDtoParticipantNotFound() {
        // validateParticipant (called before fromDto in postOutcomeScore) now shares the exact same
        // findByUuid resolution as fromDto's own participant lookup, so a not-found participant is
        // always caught there first (as InvalidParticipantException) and this path is unreachable via
        // postOutcomeScore - exercised directly against fromDto instead, mirroring the direct-fromDto
        // "not found" tests used elsewhere in this migration (e.g. ConditionServiceImplTest).
        UUID participantUuid = UUID.randomUUID();
        OutcomeScoreDto dto = OutcomeScoreDto.builder().participantId(participantUuid).outcomeId(OUTCOME_UUID).build();
        when(participantRepository.findByUuid(participantUuid)).thenReturn(Optional.empty());

        Exception exception = assertThrows(DataServiceException.class, () -> outcomeScoreService.fromDto(dto));

        assertEquals("The participant for the outcome score does not exist.", exception.getMessage());
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
        UUID participantUuid = UUID.randomUUID();
        when(participantRepository.findByUuid(participantUuid)).thenReturn(Optional.empty());

        Exception exception = assertThrows(InvalidParticipantException.class, () -> outcomeScoreService.validateParticipant(participantUuid, 1L));

        assertEquals("Error 109: The participant provided does not belong to this experiment.", exception.getMessage());
    }

    @Test
    public void testValidateParticipantWrongExperimentThrows() {
        UUID participantUuid = UUID.randomUUID();
        when(participantRepository.findByUuid(participantUuid)).thenReturn(Optional.of(participant));

        Exception exception = assertThrows(InvalidParticipantException.class, () -> outcomeScoreService.validateParticipant(participantUuid, 2L));

        assertEquals("Error 109: The participant provided does not belong to this experiment.", exception.getMessage());
    }

    @Test
    public void testValidateParticipantValid() {
        assertDoesNotThrow(() -> outcomeScoreService.validateParticipant(UUID.randomUUID(), 1L));
    }

    @Test
    public void testBuildHeaders() {
        UUID experimentUuid = UUID.randomUUID();
        UUID exposureUuid = UUID.randomUUID();
        UUID outcomeUuid = UUID.randomUUID();
        UUID outcomeScoreUuid = UUID.randomUUID();

        HttpHeaders headers = outcomeScoreService.buildHeaders(UriComponentsBuilder.newInstance(), experimentUuid, exposureUuid, outcomeUuid, outcomeScoreUuid);

        assertNotNull(headers);
        assertNotNull(headers.getLocation());
        assertTrue(headers.getLocation().toString().contains("/api/experiments/" + experimentUuid + "/exposures/" + exposureUuid + "/outcomes/" + outcomeUuid + "/outcome_scores/" + outcomeScoreUuid));
    }

    @Test
    public void testGetOutcomeScoreByUuidFound() throws Exception {
        // outcomeScore.getUuid() isn't globally stubbed in BaseModelTest, so stub it locally.
        UUID uuid = UUID.randomUUID();
        when(outcomeScore.getUuid()).thenReturn(uuid);
        when(outcomeScoreRepository.findByUuid(uuid)).thenReturn(outcomeScore);

        OutcomeScore retVal = outcomeScoreService.getOutcomeScoreByUuid(uuid);

        assertEquals(outcomeScore, retVal);
    }

    @Test
    public void testGetOutcomeScoreByUuidNotFoundThrows() {
        UUID uuid = UUID.randomUUID();
        when(outcomeScoreRepository.findByUuid(uuid)).thenReturn(null);

        Exception exception = assertThrows(OutcomeScoreNotMatchingException.class, () -> outcomeScoreService.getOutcomeScoreByUuid(uuid));

        assertTrue(exception.getMessage().startsWith("Error 108"));
    }

}
