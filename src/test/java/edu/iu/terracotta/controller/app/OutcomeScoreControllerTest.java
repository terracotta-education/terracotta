package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.dao.exceptions.OutcomeScoreNotMatchingException;
import edu.iu.terracotta.dao.model.dto.OutcomeScoreDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.InvalidParticipantException;
import edu.iu.terracotta.utils.TextConstants;

public class OutcomeScoreControllerTest extends BaseTest {

    // the uuid path variable for the one experiment under test; experiment.getExperimentId()
    // (the mock's globally-stubbed return value, see BaseModelTest) is what it resolves to
    private static final UUID EXPERIMENT_UUID = UUID.randomUUID();
    // likewise for exposure/outcome.getXId(), both globally stubbed to 1L
    private static final UUID EXPOSURE_UUID = UUID.randomUUID();
    private static final UUID OUTCOME_UUID = UUID.randomUUID();
    // outcomeScore.getOutcomeScoreId() is NOT globally stubbed in BaseModelTest, so stub it here to 1L
    // to match the "1L" literal already used throughout this file for the outcomeScoreId slot.
    private static final UUID OUTCOME_SCORE_UUID = UUID.randomUUID();

    private OutcomeScoreController outcomeScoreController;

    @BeforeEach
    void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        // Constructed manually (not @InjectMocks) because ApiJwtService has two type-matching
        // mock candidates in BaseServiceTest (apiJwtService and canvasApiJwtService), and
        // Mockito's constructor injection matches by type only, with no field-name tiebreak.
        outcomeScoreController = new OutcomeScoreController(outcomeScoreService, apiJwtService, experimentService, exposureService, outcomeService);

        when(apiJwtService.extractValues(any(), anyBoolean())).thenReturn(securedInfo);
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(experimentService.getExperimentByUuid(EXPERIMENT_UUID)).thenReturn(experiment);
        when(exposureService.getExposureByUuid(EXPOSURE_UUID)).thenReturn(exposure);
        when(outcomeService.getOutcomeByUuid(OUTCOME_UUID)).thenReturn(outcome);
        when(outcomeScore.getOutcomeScoreId()).thenReturn(1L);
        when(outcomeScoreService.getOutcomeScoreByUuid(OUTCOME_SCORE_UUID)).thenReturn(outcomeScore);
    }

    @Test
    void testGetAllOutcomeScoresByOutcome() throws Exception {
        OutcomeScoreDto dto = OutcomeScoreDto.builder().outcomeScoreId(OUTCOME_SCORE_UUID).build();
        when(outcomeScoreService.getOutcomeScores(1L)).thenReturn(List.of(dto));

        ResponseEntity<List<OutcomeScoreDto>> response = outcomeScoreController.getAllOutcomeScoresByOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetAllOutcomeScoresByOutcomeNoContent() throws Exception {
        when(outcomeScoreService.getOutcomeScores(1L)).thenReturn(Collections.emptyList());

        ResponseEntity<List<OutcomeScoreDto>> response = outcomeScoreController.getAllOutcomeScoresByOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void testGetAllOutcomeScoresByOutcomeUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<OutcomeScoreDto>> response = outcomeScoreController.getAllOutcomeScoresByOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void testGetAllOutcomeScoresByOutcomeNotMatching() throws Exception {
        doThrow(new OutcomeNotMatchingException("error")).when(apiJwtService).outcomeAllowed(securedInfo, 1L, 1L, 1L);

        assertThrows(OutcomeNotMatchingException.class, () -> outcomeScoreController.getAllOutcomeScoresByOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, httpServletRequest));
    }

    @Test
    void testGetOutcomeScore() throws Exception {
        OutcomeScoreDto dto = OutcomeScoreDto.builder().outcomeScoreId(OUTCOME_SCORE_UUID).build();
        when(outcomeScoreService.getOutcomeScore(1L)).thenReturn(outcomeScore);
        when(outcomeScoreService.toDto(outcomeScore)).thenReturn(dto);

        ResponseEntity<OutcomeScoreDto> response = outcomeScoreController.getOutcomeScore(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, OUTCOME_SCORE_UUID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void testGetOutcomeScoreUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<OutcomeScoreDto> response = outcomeScoreController.getOutcomeScore(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, OUTCOME_SCORE_UUID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testGetOutcomeScoreNotMatching() throws Exception {
        doThrow(new OutcomeScoreNotMatchingException("error")).when(apiJwtService).outcomeScoreAllowed(securedInfo, 1L, 1L);

        assertThrows(OutcomeScoreNotMatchingException.class, () -> outcomeScoreController.getOutcomeScore(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, OUTCOME_SCORE_UUID, httpServletRequest));
    }

    @Test
    void testPostOutcomeScore() throws Exception {
        OutcomeScoreDto requestDto = OutcomeScoreDto.builder().participantId(1L).scoreNumeric(5F).build();
        OutcomeScoreDto returnedDto = OutcomeScoreDto.builder().outcomeScoreId(OUTCOME_SCORE_UUID).participantId(1L).scoreNumeric(5F).build();
        HttpHeaders headers = new HttpHeaders();
        when(outcomeScoreService.postOutcomeScore(requestDto, 1L, 1L)).thenReturn(returnedDto);
        when(outcomeScoreService.buildHeaders(any(UriComponentsBuilder.class), any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class))).thenReturn(headers);

        ResponseEntity<OutcomeScoreDto> response = outcomeScoreController.postOutcomeScore(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, requestDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(returnedDto, response.getBody());
        assertEquals(headers, response.getHeaders());
    }

    @Test
    void testPostOutcomeScoreUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<OutcomeScoreDto> response = outcomeScoreController.postOutcomeScore(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, OutcomeScoreDto.builder().build(), UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testPostOutcomeScoreNotMatching() throws Exception {
        doThrow(new OutcomeNotMatchingException("error")).when(apiJwtService).outcomeAllowed(securedInfo, 1L, 1L, 1L);

        assertThrows(OutcomeNotMatchingException.class, () -> outcomeScoreController.postOutcomeScore(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, OutcomeScoreDto.builder().build(), UriComponentsBuilder.newInstance(), httpServletRequest));
    }

    @Test
    void testPostOutcomeScoreInvalidParticipant() throws Exception {
        doThrow(new InvalidParticipantException("error")).when(outcomeScoreService).postOutcomeScore(any(OutcomeScoreDto.class), anyLong(), anyLong());

        assertThrows(InvalidParticipantException.class, () -> outcomeScoreController.postOutcomeScore(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, OutcomeScoreDto.builder().build(), UriComponentsBuilder.newInstance(), httpServletRequest));
    }

    @Test
    void testUpdateOutcomeScores() throws Exception {
        OutcomeScoreDto dto = OutcomeScoreDto.builder().outcomeScoreId(OUTCOME_SCORE_UUID).build();

        ResponseEntity<Void> response = outcomeScoreController.updateOutcomeScores(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, List.of(dto), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(OUTCOME_UUID, dto.getOutcomeId());
        verify(apiJwtService, times(1)).outcomeScoreAllowed(securedInfo, 1L, 1L);
    }

    @Test
    void testUpdateOutcomeScoresSkipsAllowedCheckWhenIdNull() throws Exception {
        OutcomeScoreDto dto = OutcomeScoreDto.builder().build();

        ResponseEntity<Void> response = outcomeScoreController.updateOutcomeScores(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, List.of(dto), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(OUTCOME_UUID, dto.getOutcomeId());
        verify(apiJwtService, never()).outcomeScoreAllowed(any(), any(), any());
    }

    @Test
    void testUpdateOutcomeScoresUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        OutcomeScoreDto dto = OutcomeScoreDto.builder().outcomeScoreId(OUTCOME_SCORE_UUID).build();

        ResponseEntity<Void> response = outcomeScoreController.updateOutcomeScores(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, List.of(dto), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testUpdateOutcomeScoresInvalidParticipantPropagates() throws Exception {
        doThrow(new InvalidParticipantException("error")).when(outcomeScoreService).updateOutcomeScores(any(), anyLong());
        OutcomeScoreDto dto = OutcomeScoreDto.builder().outcomeScoreId(OUTCOME_SCORE_UUID).build();

        assertThrows(InvalidParticipantException.class, () -> outcomeScoreController.updateOutcomeScores(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, List.of(dto), httpServletRequest));
    }

    @Test
    void testUpdateOutcomeScoresDataServiceExceptionPropagates() throws Exception {
        doThrow(new DataServiceException("error")).when(outcomeScoreService).updateOutcomeScores(any(), anyLong());
        OutcomeScoreDto dto = OutcomeScoreDto.builder().outcomeScoreId(OUTCOME_SCORE_UUID).build();

        assertThrows(DataServiceException.class, () -> outcomeScoreController.updateOutcomeScores(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, List.of(dto), httpServletRequest));
    }

    @Test
    void testUpdateOutcomeScoresWrapsUnexpectedException() throws Exception {
        doThrow(new RuntimeException("boom")).when(outcomeScoreService).updateOutcomeScores(any(), anyLong());
        OutcomeScoreDto dto = OutcomeScoreDto.builder().outcomeScoreId(OUTCOME_SCORE_UUID).build();

        assertThrows(DataServiceException.class, () -> outcomeScoreController.updateOutcomeScores(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, List.of(dto), httpServletRequest));
    }

    @Test
    void testUpdateOutcomeScore() throws Exception {
        ResponseEntity<Void> response = outcomeScoreController.updateOutcomeScore(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, OUTCOME_SCORE_UUID, OutcomeScoreDto.builder().build(), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(outcomeScoreService, times(1)).updateOutcomeScore(eq(1L), any(OutcomeScoreDto.class));
    }

    @Test
    void testUpdateOutcomeScoreUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = outcomeScoreController.updateOutcomeScore(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, OUTCOME_SCORE_UUID, OutcomeScoreDto.builder().build(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testUpdateOutcomeScoreNotMatching() throws Exception {
        doThrow(new OutcomeScoreNotMatchingException("error")).when(apiJwtService).outcomeScoreAllowed(securedInfo, 1L, 1L);

        assertThrows(OutcomeScoreNotMatchingException.class, () -> outcomeScoreController.updateOutcomeScore(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, OUTCOME_SCORE_UUID, OutcomeScoreDto.builder().build(), httpServletRequest));
    }

    @Test
    void testDeleteOutcomeScore() throws Exception {
        ResponseEntity<Void> response = outcomeScoreController.deleteOutcomeScore(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, OUTCOME_SCORE_UUID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testDeleteOutcomeScoreNotFound() throws Exception {
        doThrow(new EmptyResultDataAccessException(1)).when(outcomeScoreService).deleteById(1L);

        ResponseEntity<Void> response = outcomeScoreController.deleteOutcomeScore(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, OUTCOME_SCORE_UUID, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testDeleteOutcomeScoreUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = outcomeScoreController.deleteOutcomeScore(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, OUTCOME_SCORE_UUID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testDeleteOutcomeScoreNotMatching() throws Exception {
        doThrow(new OutcomeNotMatchingException("error")).when(apiJwtService).outcomeAllowed(securedInfo, 1L, 1L, 1L);

        assertThrows(OutcomeNotMatchingException.class, () -> outcomeScoreController.deleteOutcomeScore(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, OUTCOME_SCORE_UUID, httpServletRequest));
    }

}
