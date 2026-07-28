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
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.dao.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.dao.model.dto.OutcomeDto;
import edu.iu.terracotta.dao.model.dto.OutcomePotentialDto;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.utils.TextConstants;

public class OutcomeControllerTest extends BaseTest {

    private OutcomeController outcomeController;

    @BeforeEach
    void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        // Constructed manually (not @InjectMocks) because ApiJwtService has two type-matching
        // mock candidates in BaseServiceTest (apiJwtService and canvasApiJwtService), and
        // Mockito's constructor injection matches by type only, with no field-name tiebreak.
        outcomeController = new OutcomeController(apiJwtService, outcomeService);

        when(apiJwtService.extractValues(any(), anyBoolean())).thenReturn(securedInfo);
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
    }

    @Test
    void testAllOutcomesByExposure() throws Exception {
        OutcomeDto dto = OutcomeDto.builder().outcomeId(1L).build();
        when(outcomeService.getOutcomesForExposure(1L)).thenReturn(List.of(dto));

        ResponseEntity<List<OutcomeDto>> response = outcomeController.allOutcomesByExposure(1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testAllOutcomesByExposureNoContent() throws Exception {
        when(outcomeService.getOutcomesForExposure(1L)).thenReturn(Collections.emptyList());

        ResponseEntity<List<OutcomeDto>> response = outcomeController.allOutcomesByExposure(1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void testAllOutcomesByExposureUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<OutcomeDto>> response = outcomeController.allOutcomesByExposure(1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void testAllOutcomesByExposureNotMatching() throws Exception {
        doThrow(new ExposureNotMatchingException("error")).when(apiJwtService).exposureAllowed(securedInfo, 1L, 1L);

        assertThrows(ExposureNotMatchingException.class, () -> outcomeController.allOutcomesByExposure(1L, 1L, httpServletRequest));
    }

    @Test
    void testGetOutcomeWithUpdateScores() throws Exception {
        OutcomeDto dto = OutcomeDto.builder().outcomeId(1L).build();
        when(outcomeService.getOutcome(1L)).thenReturn(outcome);
        when(outcomeService.toDto(outcome, false)).thenReturn(dto);

        ResponseEntity<OutcomeDto> response = outcomeController.getOutcome(1L, 1L, 1L, false, true, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
        verify(outcomeService, times(1)).updateOutcomeGrades(1L, securedInfo, true);
    }

    @Test
    void testGetOutcomeSkipsUpdateScoresWhenFalse() throws Exception {
        OutcomeDto dto = OutcomeDto.builder().outcomeId(1L).build();
        when(outcomeService.getOutcome(1L)).thenReturn(outcome);
        when(outcomeService.toDto(outcome, false)).thenReturn(dto);

        ResponseEntity<OutcomeDto> response = outcomeController.getOutcome(1L, 1L, 1L, false, false, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(outcomeService, never()).updateOutcomeGrades(anyLong(), any(SecuredInfo.class), anyBoolean());
    }

    @Test
    void testGetOutcomeUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<OutcomeDto> response = outcomeController.getOutcome(1L, 1L, 1L, false, true, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testGetOutcomeNotMatching() throws Exception {
        doThrow(new OutcomeNotMatchingException("error")).when(apiJwtService).outcomeAllowed(securedInfo, 1L, 1L, 1L);

        assertThrows(OutcomeNotMatchingException.class, () -> outcomeController.getOutcome(1L, 1L, 1L, false, true, httpServletRequest));
    }

    @Test
    void testGetOutcomeApiExceptionFromUpdateGrades() throws Exception {
        doThrow(new ApiException("error")).when(outcomeService).updateOutcomeGrades(anyLong(), any(SecuredInfo.class), anyBoolean());

        assertThrows(ApiException.class, () -> outcomeController.getOutcome(1L, 1L, 1L, false, true, httpServletRequest));
    }

    @Test
    void testPostOutcome() throws Exception {
        OutcomeDto requestDto = OutcomeDto.builder().title("new outcome").build();
        OutcomeDto returnedDto = OutcomeDto.builder().outcomeId(1L).title("new outcome").build();
        HttpHeaders headers = new HttpHeaders();
        when(outcomeService.postOutcome(requestDto, 1L)).thenReturn(returnedDto);
        when(outcomeService.buildHeaders(any(UriComponentsBuilder.class), anyLong(), anyLong(), anyLong())).thenReturn(headers);

        ResponseEntity<OutcomeDto> response = outcomeController.postOutcome(1L, 1L, requestDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(returnedDto, response.getBody());
        assertEquals(headers, response.getHeaders());
    }

    @Test
    void testPostOutcomeUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<OutcomeDto> response = outcomeController.postOutcome(1L, 1L, OutcomeDto.builder().build(), UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testPostOutcomeNotMatching() throws Exception {
        doThrow(new ExposureNotMatchingException("error")).when(apiJwtService).exposureAllowed(securedInfo, 1L, 1L);

        assertThrows(ExposureNotMatchingException.class, () -> outcomeController.postOutcome(1L, 1L, OutcomeDto.builder().build(), UriComponentsBuilder.newInstance(), httpServletRequest));
    }

    @Test
    void testPostOutcomeTitleValidation() throws Exception {
        doThrow(new TitleValidationException("error")).when(outcomeService).postOutcome(any(OutcomeDto.class), anyLong());

        assertThrows(TitleValidationException.class, () -> outcomeController.postOutcome(1L, 1L, OutcomeDto.builder().build(), UriComponentsBuilder.newInstance(), httpServletRequest));
    }

    @Test
    void testUpdateOutcome() throws Exception {
        ResponseEntity<Void> response = outcomeController.updateOutcome(1L, 1L, 1L, OutcomeDto.builder().build(), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(outcomeService, times(1)).updateOutcome(eq(1L), any(OutcomeDto.class));
    }

    @Test
    void testUpdateOutcomeUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = outcomeController.updateOutcome(1L, 1L, 1L, OutcomeDto.builder().build(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testUpdateOutcomeNotMatching() throws Exception {
        doThrow(new OutcomeNotMatchingException("error")).when(apiJwtService).outcomeAllowed(securedInfo, 1L, 1L, 1L);

        assertThrows(OutcomeNotMatchingException.class, () -> outcomeController.updateOutcome(1L, 1L, 1L, OutcomeDto.builder().build(), httpServletRequest));
    }

    @Test
    void testUpdateOutcomeTitleValidation() throws Exception {
        doThrow(new TitleValidationException("error")).when(outcomeService).updateOutcome(anyLong(), any(OutcomeDto.class));

        assertThrows(TitleValidationException.class, () -> outcomeController.updateOutcome(1L, 1L, 1L, OutcomeDto.builder().build(), httpServletRequest));
    }

    @Test
    void testDeleteOutcome() throws Exception {
        ResponseEntity<Void> response = outcomeController.deleteOutcome(1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testDeleteOutcomeNotFound() throws Exception {
        doThrow(new EmptyResultDataAccessException(1)).when(outcomeService).deleteById(1L);

        ResponseEntity<Void> response = outcomeController.deleteOutcome(1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testDeleteOutcomeUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = outcomeController.deleteOutcome(1L, 1L, 1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testDeleteOutcomeNotMatching() throws Exception {
        doThrow(new OutcomeNotMatchingException("error")).when(apiJwtService).outcomeAllowed(securedInfo, 1L, 1L, 1L);

        assertThrows(OutcomeNotMatchingException.class, () -> outcomeController.deleteOutcome(1L, 1L, 1L, httpServletRequest));
    }

    @Test
    void testOutcomePotentials() throws Exception {
        OutcomePotentialDto potentialDto = OutcomePotentialDto.builder().name("potential").build();
        when(outcomeService.potentialOutcomes(1L, securedInfo)).thenReturn(List.of(potentialDto));

        ResponseEntity<List<OutcomePotentialDto>> response = outcomeController.outcomePotentials(1L, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testOutcomePotentialsUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<OutcomePotentialDto>> response = outcomeController.outcomePotentials(1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testOutcomePotentialsNotMatching() throws Exception {
        doThrow(new ExperimentNotMatchingException("error")).when(apiJwtService).experimentAllowed(securedInfo, 1L);

        assertThrows(ExperimentNotMatchingException.class, () -> outcomeController.outcomePotentials(1L, httpServletRequest));
    }

    @Test
    void testOutcomePotentialsApiException() throws Exception {
        doThrow(new ApiException("error")).when(outcomeService).potentialOutcomes(anyLong(), any(SecuredInfo.class));

        assertThrows(ApiException.class, () -> outcomeController.outcomePotentials(1L, httpServletRequest));
    }

    @Test
    void testGetOutcomesForExperiment() throws Exception {
        OutcomeDto dto = OutcomeDto.builder().outcomeId(1L).build();
        when(outcomeService.getAllByExperiment(1L)).thenReturn(List.of(dto));

        ResponseEntity<List<OutcomeDto>> response = outcomeController.getOutcomesForExperiment(1L, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetOutcomesForExperimentEmptyStillReturnsOk() throws Exception {
        // Unlike allOutcomesByExposure/getAllOutcomeScoresByOutcome, this endpoint has no
        // NO_CONTENT branch for an empty list - it always returns 200 OK, even with an empty body.
        when(outcomeService.getAllByExperiment(1L)).thenReturn(Collections.emptyList());

        ResponseEntity<List<OutcomeDto>> response = outcomeController.getOutcomesForExperiment(1L, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().size());
    }

    @Test
    void testGetOutcomesForExperimentUnauthorized() throws Exception {
        // Note: this endpoint requires isInstructorOrHigher, unlike the sibling read endpoints in
        // this controller (allOutcomesByExposure, getOutcome) which only require isLearnerOrHigher.
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<OutcomeDto>> response = outcomeController.getOutcomesForExperiment(1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testGetOutcomesForExperimentNotMatching() throws Exception {
        doThrow(new ExperimentNotMatchingException("error")).when(apiJwtService).experimentAllowed(securedInfo, 1L);

        assertThrows(ExperimentNotMatchingException.class, () -> outcomeController.getOutcomesForExperiment(1L, httpServletRequest));
    }

}
