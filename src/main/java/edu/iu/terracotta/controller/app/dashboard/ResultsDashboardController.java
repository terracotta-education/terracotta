package edu.iu.terracotta.controller.app.dashboard;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.model.app.dto.dashboard.ResultsDashboardDto;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.request.ResultsOutcomesRequestDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.dashboard.results.ResultsDashboardService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@SuppressWarnings({"PMD.GuardLogStatement"})
@RequestMapping(value = ResultsDashboardController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ResultsDashboardController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/dashboard/results";

    @Autowired
    private APIJWTService apijwtService;

    @Autowired
    private ResultsDashboardService resultsDashboardService;

    @GetMapping("/overview")
    public ResponseEntity<ResultsDashboardDto> getOverview(@PathVariable long experimentId, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            return new ResponseEntity<>(resultsDashboardService.overview(experimentId, securedInfo), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error calculating overview for experiment ID: [{}]", experimentId, e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/outcomes")
    public ResponseEntity<ResultsDashboardDto> postComparison(@PathVariable long experimentId, @RequestBody ResultsOutcomesRequestDto resultsOutcomesRequestDto, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, OutcomeNotMatchingException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            return new ResponseEntity<>(resultsDashboardService.outcomes(experimentId, resultsOutcomesRequestDto), HttpStatus.OK);
        } catch (Exception e) {
            log.error(
                "Error calculating outcomes for experiment ID: [{}], outcome IDs: [{}], alternate ID: [{}]",
                experimentId, resultsOutcomesRequestDto.getOutcomeIds(), resultsOutcomesRequestDto.getAlternateId().getId(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

}
