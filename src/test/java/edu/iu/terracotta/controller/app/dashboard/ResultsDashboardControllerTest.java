package edu.iu.terracotta.controller.app.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.dashboard.ResultsDashboardDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.service.app.dashboard.results.ResultsDashboardService;
import jakarta.servlet.http.HttpServletRequest;

public class ResultsDashboardControllerTest extends BaseTest {

    private static final UUID EXPERIMENT_UUID = UUID.randomUUID();

    @Mock private ResultsDashboardService resultsDashboardService;

    private ResultsDashboardController resultsDashboardController;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        // Constructed manually rather than via @InjectMocks: ApiJwtService is also implemented by the
        // inherited canvasApiJwtService mock (see the ambiguity warning in BaseServiceTest), so
        // constructor-injection-by-type could silently wire the wrong ApiJwtService mock.
        resultsDashboardController = new ResultsDashboardController(apiJwtService, experimentService, resultsDashboardService);

        when(apiJwtService.extractValues(any(HttpServletRequest.class), anyBoolean())).thenReturn(securedInfo);
        when(experimentService.getExperimentIdByUuid(EXPERIMENT_UUID)).thenAnswer(invocation -> experiment.getExperimentId());
    }

    @Test
    void getOverviewTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        UUID experimentUuid = UUID.randomUUID();
        ResultsDashboardDto dto = ResultsDashboardDto.builder().experimentId(experimentUuid).build();
        when(resultsDashboardService.overview(1L, securedInfo)).thenReturn(dto);

        ResponseEntity<ResultsDashboardDto> response = resultsDashboardController.getOverview(EXPERIMENT_UUID, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(experimentUuid, response.getBody().getExperimentId());
    }

    @Test
    void getOverviewUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<ResultsDashboardDto> response = resultsDashboardController.getOverview(EXPERIMENT_UUID, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(resultsDashboardService, never()).overview(anyLong(), any(SecuredInfo.class));
    }

    @Test
    void getOverviewServiceExceptionTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(resultsDashboardService.overview(1L, securedInfo)).thenThrow(new RuntimeException("boom"));

        ResponseEntity<ResultsDashboardDto> response = resultsDashboardController.getOverview(EXPERIMENT_UUID, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getOverviewExperimentNotMatchingTest() throws Exception {
        doThrow(new ExperimentNotMatchingException("no match")).when(apiJwtService).experimentAllowed(securedInfo, 1L);

        assertThrows(ExperimentNotMatchingException.class, () -> resultsDashboardController.getOverview(EXPERIMENT_UUID, httpServletRequest));
    }

    @Test
    void postComparisonTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        UUID experimentUuid = UUID.randomUUID();
        ResultsDashboardDto dto = ResultsDashboardDto.builder().experimentId(experimentUuid).build();
        when(resultsDashboardService.outcomes(1L, resultsOutcomesRequestDto)).thenReturn(dto);

        ResponseEntity<ResultsDashboardDto> response = resultsDashboardController.postComparison(EXPERIMENT_UUID, resultsOutcomesRequestDto, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(experimentUuid, response.getBody().getExperimentId());
    }

    @Test
    void postComparisonUnauthorizedTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<ResultsDashboardDto> response = resultsDashboardController.postComparison(EXPERIMENT_UUID, resultsOutcomesRequestDto, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void postComparisonServiceExceptionTest() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(resultsDashboardService.outcomes(1L, resultsOutcomesRequestDto)).thenThrow(new RuntimeException("boom"));

        ResponseEntity<ResultsDashboardDto> response = resultsDashboardController.postComparison(EXPERIMENT_UUID, resultsOutcomesRequestDto, httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void postComparisonBadTokenTest() throws Exception {
        doThrow(new BadTokenException("bad token")).when(apiJwtService).experimentAllowed(securedInfo, 1L);

        assertThrows(BadTokenException.class, () -> resultsDashboardController.postComparison(EXPERIMENT_UUID, resultsOutcomesRequestDto, httpServletRequest));
    }

}
