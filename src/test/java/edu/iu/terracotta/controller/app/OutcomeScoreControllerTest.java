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

    private OutcomeScoreController outcomeScoreController;

    @BeforeEach
    void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        // Constructed manually (not @InjectMocks) because ApiJwtService has two type-matching
        // mock candidates in BaseServiceTest (apiJwtService and canvasApiJwtService), and
        // Mockito's constructor injection matches by type only, with no field-name tiebreak.
        outcomeScoreController = new OutcomeScoreController(outcomeScoreService, apiJwtService);

        when(apiJwtService.extractValues(any(), anyBoolean())).thenReturn(securedInfo);
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
    }

    @Test
    void testGetAllOutcomeScoresByOutcome() throws Exception {
        OutcomeScoreDto dto = OutcomeScoreDto.builder().outcomeScoreId(1L).build();
        when(outcomeScoreService.getOutcomeScores(1L)).thenReturn(List.of(dto));

        ResponseEntity<List<OutcomeScoreDto>> response = outcomeScoreController.getAllOutcomeScoresByOutcome(1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetAllOutcomeScoresByOutcomeNoContent() throws Exception {
        when(outcomeScoreService.getOutcomeScores(1L)).thenReturn(Collections.emptyList());

        ResponseEntity<List<OutcomeScoreDto>> response = outcomeScoreController.getAllOutcomeScoresByOutcome(1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void testGetAllOutcomeScoresByOutcomeUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<OutcomeScoreDto>> response = outcomeScoreController.getAllOutcomeScoresByOutcome(1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void testGetAllOutcomeScoresByOutcomeNotMatching() throws Exception {
        doThrow(new OutcomeNotMatchingException("error")).when(apiJwtService).outcomeAllowed(securedInfo, 1L, 1L, 1L);

        assertThrows(OutcomeNotMatchingException.class, () -> outcomeScoreController.getAllOutcomeScoresByOutcome(1L, 1L, 1L, httpServletRequest));
    }

    @Test
    void testGetOutcomeScore() throws Exception {
        OutcomeScoreDto dto = OutcomeScoreDto.builder().outcomeScoreId(1L).build();
        when(outcomeScoreService.getOutcomeScore(1L)).thenReturn(outcomeScore);
        when(outcomeScoreService.toDto(outcomeScore)).thenReturn(dto);

        ResponseEntity<OutcomeScoreDto> response = outcomeScoreController.getOutcomeScore(1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void testGetOutcomeScoreUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<OutcomeScoreDto> response = outcomeScoreController.getOutcomeScore(1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testGetOutcomeScoreNotMatching() throws Exception {
        doThrow(new OutcomeScoreNotMatchingException("error")).when(apiJwtService).outcomeScoreAllowed(securedInfo, 1L, 1L);

        assertThrows(OutcomeScoreNotMatchingException.class, () -> outcomeScoreController.getOutcomeScore(1L, 1L, 1L, 1L, httpServletRequest));
    }

    @Test
    void testPostOutcomeScore() throws Exception {
        OutcomeScoreDto requestDto = OutcomeScoreDto.builder().participantId(1L).scoreNumeric(5F).build();
        OutcomeScoreDto returnedDto = OutcomeScoreDto.builder().outcomeScoreId(1L).participantId(1L).scoreNumeric(5F).build();
        HttpHeaders headers = new HttpHeaders();
        when(outcomeScoreService.postOutcomeScore(requestDto, 1L, 1L)).thenReturn(returnedDto);
        when(outcomeScoreService.buildHeaders(any(UriComponentsBuilder.class), anyLong(), anyLong(), anyLong(), anyLong())).thenReturn(headers);

        ResponseEntity<OutcomeScoreDto> response = outcomeScoreController.postOutcomeScore(1L, 1L, 1L, requestDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(returnedDto, response.getBody());
        assertEquals(headers, response.getHeaders());
    }

    @Test
    void testPostOutcomeScoreUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<OutcomeScoreDto> response = outcomeScoreController.postOutcomeScore(1L, 1L, 1L, OutcomeScoreDto.builder().build(), UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testPostOutcomeScoreNotMatching() throws Exception {
        doThrow(new OutcomeNotMatchingException("error")).when(apiJwtService).outcomeAllowed(securedInfo, 1L, 1L, 1L);

        assertThrows(OutcomeNotMatchingException.class, () -> outcomeScoreController.postOutcomeScore(1L, 1L, 1L, OutcomeScoreDto.builder().build(), UriComponentsBuilder.newInstance(), httpServletRequest));
    }

    @Test
    void testPostOutcomeScoreInvalidParticipant() throws Exception {
        doThrow(new InvalidParticipantException("error")).when(outcomeScoreService).postOutcomeScore(any(OutcomeScoreDto.class), anyLong(), anyLong());

        assertThrows(InvalidParticipantException.class, () -> outcomeScoreController.postOutcomeScore(1L, 1L, 1L, OutcomeScoreDto.builder().build(), UriComponentsBuilder.newInstance(), httpServletRequest));
    }

    @Test
    void testUpdateOutcomeScores() throws Exception {
        OutcomeScoreDto dto = OutcomeScoreDto.builder().outcomeScoreId(1L).build();

        ResponseEntity<Void> response = outcomeScoreController.updateOutcomeScores(1L, 1L, 1L, List.of(dto), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Long.valueOf(1L), dto.getOutcomeId());
        verify(apiJwtService, times(1)).outcomeScoreAllowed(securedInfo, 1L, 1L);
    }

    @Test
    void testUpdateOutcomeScoresSkipsAllowedCheckWhenIdNull() throws Exception {
        OutcomeScoreDto dto = OutcomeScoreDto.builder().build();

        ResponseEntity<Void> response = outcomeScoreController.updateOutcomeScores(1L, 1L, 1L, List.of(dto), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Long.valueOf(1L), dto.getOutcomeId());
        verify(apiJwtService, never()).outcomeScoreAllowed(any(), any(), any());
    }

    @Test
    void testUpdateOutcomeScoresUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);
        OutcomeScoreDto dto = OutcomeScoreDto.builder().outcomeScoreId(1L).build();

        ResponseEntity<Void> response = outcomeScoreController.updateOutcomeScores(1L, 1L, 1L, List.of(dto), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testUpdateOutcomeScoresInvalidParticipantPropagates() throws Exception {
        doThrow(new InvalidParticipantException("error")).when(outcomeScoreService).updateOutcomeScores(any(), anyLong());
        OutcomeScoreDto dto = OutcomeScoreDto.builder().outcomeScoreId(1L).build();

        assertThrows(InvalidParticipantException.class, () -> outcomeScoreController.updateOutcomeScores(1L, 1L, 1L, List.of(dto), httpServletRequest));
    }

    @Test
    void testUpdateOutcomeScoresDataServiceExceptionPropagates() throws Exception {
        doThrow(new DataServiceException("error")).when(outcomeScoreService).updateOutcomeScores(any(), anyLong());
        OutcomeScoreDto dto = OutcomeScoreDto.builder().outcomeScoreId(1L).build();

        assertThrows(DataServiceException.class, () -> outcomeScoreController.updateOutcomeScores(1L, 1L, 1L, List.of(dto), httpServletRequest));
    }

    @Test
    void testUpdateOutcomeScoresWrapsUnexpectedException() throws Exception {
        doThrow(new RuntimeException("boom")).when(outcomeScoreService).updateOutcomeScores(any(), anyLong());
        OutcomeScoreDto dto = OutcomeScoreDto.builder().outcomeScoreId(1L).build();

        assertThrows(DataServiceException.class, () -> outcomeScoreController.updateOutcomeScores(1L, 1L, 1L, List.of(dto), httpServletRequest));
    }

    @Test
    void testUpdateOutcomeScore() throws Exception {
        ResponseEntity<Void> response = outcomeScoreController.updateOutcomeScore(1L, 1L, 1L, 1L, OutcomeScoreDto.builder().build(), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(outcomeScoreService, times(1)).updateOutcomeScore(eq(1L), any(OutcomeScoreDto.class));
    }

    @Test
    void testUpdateOutcomeScoreUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = outcomeScoreController.updateOutcomeScore(1L, 1L, 1L, 1L, OutcomeScoreDto.builder().build(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testUpdateOutcomeScoreNotMatching() throws Exception {
        doThrow(new OutcomeScoreNotMatchingException("error")).when(apiJwtService).outcomeScoreAllowed(securedInfo, 1L, 1L);

        assertThrows(OutcomeScoreNotMatchingException.class, () -> outcomeScoreController.updateOutcomeScore(1L, 1L, 1L, 1L, OutcomeScoreDto.builder().build(), httpServletRequest));
    }

    @Test
    void testDeleteOutcomeScore() throws Exception {
        ResponseEntity<Void> response = outcomeScoreController.deleteOutcomeScore(1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testDeleteOutcomeScoreNotFound() throws Exception {
        doThrow(new EmptyResultDataAccessException(1)).when(outcomeScoreService).deleteById(1L);

        ResponseEntity<Void> response = outcomeScoreController.deleteOutcomeScore(1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testDeleteOutcomeScoreUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = outcomeScoreController.deleteOutcomeScore(1L, 1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testDeleteOutcomeScoreNotMatching() throws Exception {
        doThrow(new OutcomeNotMatchingException("error")).when(apiJwtService).outcomeAllowed(securedInfo, 1L, 1L, 1L);

        assertThrows(OutcomeNotMatchingException.class, () -> outcomeScoreController.deleteOutcomeScore(1L, 1L, 1L, 1L, httpServletRequest));
    }

}
