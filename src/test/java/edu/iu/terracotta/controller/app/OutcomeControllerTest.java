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

    // the uuid path variable for the one experiment under test; experiment.getExperimentId()
    // (the mock's globally-stubbed return value, see BaseModelTest) is what it resolves to
    private static final UUID EXPERIMENT_UUID = UUID.randomUUID();
    // likewise for exposure.getExposureId() / outcome.getOutcomeId(), both globally stubbed to 1L
    private static final UUID EXPOSURE_UUID = UUID.randomUUID();
    private static final UUID OUTCOME_UUID = UUID.randomUUID();

    private OutcomeController outcomeController;

    @BeforeEach
    void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        // Constructed manually (not @InjectMocks) because ApiJwtService has two type-matching
        // mock candidates in BaseServiceTest (apiJwtService and canvasApiJwtService), and
        // Mockito's constructor injection matches by type only, with no field-name tiebreak.
        outcomeController = new OutcomeController(apiJwtService, experimentService, exposureService, outcomeService);

        when(apiJwtService.extractValues(any(), anyBoolean())).thenReturn(securedInfo);
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(experimentService.getExperimentByUuid(EXPERIMENT_UUID)).thenReturn(experiment);
        when(exposureService.getExposureByUuid(EXPOSURE_UUID)).thenReturn(exposure);
        when(outcomeService.getOutcomeByUuid(OUTCOME_UUID)).thenReturn(outcome);
    }

    @Test
    void testAllOutcomesByExposure() throws Exception {
        OutcomeDto dto = OutcomeDto.builder().outcomeId(OUTCOME_UUID).build();
        when(outcomeService.getOutcomesForExposure(1L)).thenReturn(List.of(dto));

        ResponseEntity<List<OutcomeDto>> response = outcomeController.allOutcomesByExposure(EXPERIMENT_UUID, EXPOSURE_UUID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testAllOutcomesByExposureNoContent() throws Exception {
        when(outcomeService.getOutcomesForExposure(1L)).thenReturn(Collections.emptyList());

        ResponseEntity<List<OutcomeDto>> response = outcomeController.allOutcomesByExposure(EXPERIMENT_UUID, EXPOSURE_UUID, httpServletRequest);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void testAllOutcomesByExposureUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<OutcomeDto>> response = outcomeController.allOutcomesByExposure(EXPERIMENT_UUID, EXPOSURE_UUID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(TextConstants.NOT_ENOUGH_PERMISSIONS, response.getBody());
    }

    @Test
    void testAllOutcomesByExposureNotMatching() throws Exception {
        doThrow(new ExposureNotMatchingException("error")).when(apiJwtService).exposureAllowed(securedInfo, 1L, 1L);

        assertThrows(ExposureNotMatchingException.class, () -> outcomeController.allOutcomesByExposure(EXPERIMENT_UUID, EXPOSURE_UUID, httpServletRequest));
    }

    @Test
    void testGetOutcomeWithUpdateScores() throws Exception {
        OutcomeDto dto = OutcomeDto.builder().outcomeId(OUTCOME_UUID).build();
        when(outcomeService.getOutcome(1L)).thenReturn(outcome);
        when(outcomeService.toDto(outcome, false)).thenReturn(dto);

        ResponseEntity<OutcomeDto> response = outcomeController.getOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, false, true, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
        verify(outcomeService, times(1)).updateOutcomeGrades(1L, securedInfo, true);
    }

    @Test
    void testGetOutcomeSkipsUpdateScoresWhenFalse() throws Exception {
        OutcomeDto dto = OutcomeDto.builder().outcomeId(OUTCOME_UUID).build();
        when(outcomeService.getOutcome(1L)).thenReturn(outcome);
        when(outcomeService.toDto(outcome, false)).thenReturn(dto);

        ResponseEntity<OutcomeDto> response = outcomeController.getOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, false, false, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(outcomeService, never()).updateOutcomeGrades(anyLong(), any(SecuredInfo.class), anyBoolean());
    }

    @Test
    void testGetOutcomeUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<OutcomeDto> response = outcomeController.getOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, false, true, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testGetOutcomeNotMatching() throws Exception {
        doThrow(new OutcomeNotMatchingException("error")).when(apiJwtService).outcomeAllowed(securedInfo, 1L, 1L, 1L);

        assertThrows(OutcomeNotMatchingException.class, () -> outcomeController.getOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, false, true, httpServletRequest));
    }

    @Test
    void testGetOutcomeApiExceptionFromUpdateGrades() throws Exception {
        doThrow(new ApiException("error")).when(outcomeService).updateOutcomeGrades(anyLong(), any(SecuredInfo.class), anyBoolean());

        assertThrows(ApiException.class, () -> outcomeController.getOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, false, true, httpServletRequest));
    }

    @Test
    void testPostOutcome() throws Exception {
        OutcomeDto requestDto = OutcomeDto.builder().title("new outcome").build();
        OutcomeDto returnedDto = OutcomeDto.builder().outcomeId(OUTCOME_UUID).title("new outcome").build();
        HttpHeaders headers = new HttpHeaders();
        when(outcomeService.postOutcome(requestDto, 1L)).thenReturn(returnedDto);
        when(outcomeService.buildHeaders(any(UriComponentsBuilder.class), any(UUID.class), any(UUID.class), any(UUID.class))).thenReturn(headers);

        ResponseEntity<OutcomeDto> response = outcomeController.postOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, requestDto, UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(returnedDto, response.getBody());
        assertEquals(headers, response.getHeaders());
    }

    @Test
    void testPostOutcomeUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<OutcomeDto> response = outcomeController.postOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, OutcomeDto.builder().build(), UriComponentsBuilder.newInstance(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testPostOutcomeNotMatching() throws Exception {
        doThrow(new ExposureNotMatchingException("error")).when(apiJwtService).exposureAllowed(securedInfo, 1L, 1L);

        assertThrows(ExposureNotMatchingException.class, () -> outcomeController.postOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, OutcomeDto.builder().build(), UriComponentsBuilder.newInstance(), httpServletRequest));
    }

    @Test
    void testPostOutcomeTitleValidation() throws Exception {
        doThrow(new TitleValidationException("error")).when(outcomeService).postOutcome(any(OutcomeDto.class), anyLong());

        assertThrows(TitleValidationException.class, () -> outcomeController.postOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, OutcomeDto.builder().build(), UriComponentsBuilder.newInstance(), httpServletRequest));
    }

    @Test
    void testUpdateOutcome() throws Exception {
        ResponseEntity<Void> response = outcomeController.updateOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, OutcomeDto.builder().build(), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(outcomeService, times(1)).updateOutcome(eq(1L), any(OutcomeDto.class));
    }

    @Test
    void testUpdateOutcomeUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = outcomeController.updateOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, OutcomeDto.builder().build(), httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testUpdateOutcomeNotMatching() throws Exception {
        doThrow(new OutcomeNotMatchingException("error")).when(apiJwtService).outcomeAllowed(securedInfo, 1L, 1L, 1L);

        assertThrows(OutcomeNotMatchingException.class, () -> outcomeController.updateOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, OutcomeDto.builder().build(), httpServletRequest));
    }

    @Test
    void testUpdateOutcomeTitleValidation() throws Exception {
        doThrow(new TitleValidationException("error")).when(outcomeService).updateOutcome(anyLong(), any(OutcomeDto.class));

        assertThrows(TitleValidationException.class, () -> outcomeController.updateOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, OutcomeDto.builder().build(), httpServletRequest));
    }

    @Test
    void testDeleteOutcome() throws Exception {
        ResponseEntity<Void> response = outcomeController.deleteOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testDeleteOutcomeNotFound() throws Exception {
        doThrow(new EmptyResultDataAccessException(1)).when(outcomeService).deleteById(1L);

        ResponseEntity<Void> response = outcomeController.deleteOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, httpServletRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testDeleteOutcomeUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = outcomeController.deleteOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testDeleteOutcomeNotMatching() throws Exception {
        doThrow(new OutcomeNotMatchingException("error")).when(apiJwtService).outcomeAllowed(securedInfo, 1L, 1L, 1L);

        assertThrows(OutcomeNotMatchingException.class, () -> outcomeController.deleteOutcome(EXPERIMENT_UUID, EXPOSURE_UUID, OUTCOME_UUID, httpServletRequest));
    }

    @Test
    void testOutcomePotentials() throws Exception {
        OutcomePotentialDto potentialDto = OutcomePotentialDto.builder().name("potential").build();
        when(outcomeService.potentialOutcomes(1L, securedInfo)).thenReturn(List.of(potentialDto));

        ResponseEntity<List<OutcomePotentialDto>> response = outcomeController.outcomePotentials(EXPERIMENT_UUID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testOutcomePotentialsUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<OutcomePotentialDto>> response = outcomeController.outcomePotentials(EXPERIMENT_UUID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testOutcomePotentialsNotMatching() throws Exception {
        doThrow(new ExperimentNotMatchingException("error")).when(apiJwtService).experimentAllowed(securedInfo, 1L);

        assertThrows(ExperimentNotMatchingException.class, () -> outcomeController.outcomePotentials(EXPERIMENT_UUID, httpServletRequest));
    }

    @Test
    void testOutcomePotentialsApiException() throws Exception {
        doThrow(new ApiException("error")).when(outcomeService).potentialOutcomes(anyLong(), any(SecuredInfo.class));

        assertThrows(ApiException.class, () -> outcomeController.outcomePotentials(EXPERIMENT_UUID, httpServletRequest));
    }

    @Test
    void testGetOutcomesForExperiment() throws Exception {
        OutcomeDto dto = OutcomeDto.builder().outcomeId(OUTCOME_UUID).build();
        when(outcomeService.getAllByExperiment(1L)).thenReturn(List.of(dto));

        ResponseEntity<List<OutcomeDto>> response = outcomeController.getOutcomesForExperiment(EXPERIMENT_UUID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetOutcomesForExperimentEmptyStillReturnsOk() throws Exception {
        // Unlike allOutcomesByExposure/getAllOutcomeScoresByOutcome, this endpoint has no
        // NO_CONTENT branch for an empty list - it always returns 200 OK, even with an empty body.
        when(outcomeService.getAllByExperiment(1L)).thenReturn(Collections.emptyList());

        ResponseEntity<List<OutcomeDto>> response = outcomeController.getOutcomesForExperiment(EXPERIMENT_UUID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().size());
    }

    @Test
    void testGetOutcomesForExperimentUnauthorized() throws Exception {
        // Note: this endpoint requires isInstructorOrHigher, unlike the sibling read endpoints in
        // this controller (allOutcomesByExposure, getOutcome) which only require isLearnerOrHigher.
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<List<OutcomeDto>> response = outcomeController.getOutcomesForExperiment(EXPERIMENT_UUID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testGetOutcomesForExperimentNotMatching() throws Exception {
        doThrow(new ExperimentNotMatchingException("error")).when(apiJwtService).experimentAllowed(securedInfo, 1L);

        assertThrows(ExperimentNotMatchingException.class, () -> outcomeController.getOutcomesForExperiment(EXPERIMENT_UUID, httpServletRequest));
    }

}
