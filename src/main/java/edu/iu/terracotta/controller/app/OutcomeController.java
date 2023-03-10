package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.dto.OutcomeDto;
import edu.iu.terracotta.model.app.dto.OutcomePotentialDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.OutcomeService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@Slf4j
@Controller
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = OutcomeController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class OutcomeController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}";

    @Autowired
    private OutcomeService outcomeService;

    @Autowired
    private APIJWTService apijwtService;

    @GetMapping("/exposures/{exposureId}/outcomes")
    public ResponseEntity<List<OutcomeDto>> allOutcomesByExposure(@PathVariable long experimentId,
                                                                  @PathVariable long exposureId,
                                                                  HttpServletRequest req)
            throws ExperimentNotMatchingException, ExposureNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        List<OutcomeDto> outcomeList = outcomeService.getOutcomes(exposureId);

        if (outcomeList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(outcomeList, HttpStatus.OK);
    }

    @GetMapping("/exposures/{exposureId}/outcomes/{outcomeId}")
    public ResponseEntity<OutcomeDto> getOutcome(@PathVariable long experimentId,
                                                 @PathVariable long exposureId,
                                                 @PathVariable long outcomeId,
                                                 @RequestParam(name = "outcome_scores", defaultValue = "false") boolean outcomeScores,
                                                 @RequestParam(name = "update_scores", defaultValue = "true") boolean updateScores,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, OutcomeNotMatchingException, BadTokenException, CanvasApiException, ParticipantNotUpdatedException, IOException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.outcomeAllowed(securedInfo, experimentId, exposureId, outcomeId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if (updateScores) {
            outcomeService.updateOutcomeGrades(outcomeId, securedInfo);
        }

        OutcomeDto outcomeDto = outcomeService.toDto(outcomeService.getOutcome(outcomeId), outcomeScores);

        return new ResponseEntity<>(outcomeDto, HttpStatus.OK);
    }

    @PostMapping("/exposures/{exposureId}/outcomes")
    public ResponseEntity<OutcomeDto> postOutcome(@PathVariable long experimentId,
                                                  @PathVariable long exposureId,
                                                  @RequestBody OutcomeDto outcomeDto,
                                                  UriComponentsBuilder ucBuilder,
                                                  HttpServletRequest req)
            throws ExperimentNotMatchingException, ExposureNotMatchingException, BadTokenException, TitleValidationException, IdInPostException, DataServiceException {
        log.debug("Creating Outcome: {}", outcomeDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        OutcomeDto returnedDto = outcomeService.postOutcome(outcomeDto, exposureId);
        HttpHeaders headers = outcomeService.buildHeaders(ucBuilder, experimentId, exposureId, returnedDto.getOutcomeId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

    @PutMapping("/exposures/{exposureId}/outcomes/{outcomeId}")
    public ResponseEntity<Void> updateOutcome(@PathVariable long experimentId,
                                              @PathVariable long exposureId,
                                              @PathVariable long outcomeId,
                                              @RequestBody OutcomeDto outcomeDto,
                                              HttpServletRequest req)
            throws ExperimentNotMatchingException, OutcomeNotMatchingException, BadTokenException, TitleValidationException {

        log.debug("Updating outcome with id {}", outcomeId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.outcomeAllowed(securedInfo, experimentId, exposureId, outcomeId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

            outcomeService.updateOutcome(outcomeId, outcomeDto);

            return new ResponseEntity<>(HttpStatus.OK);
    }


    @DeleteMapping("/exposures/{exposureId}/outcomes/{outcomeId}")
    public ResponseEntity<Void> deleteOutcome(@PathVariable long experimentId,
                                              @PathVariable long exposureId,
                                              @PathVariable long outcomeId,
                                              HttpServletRequest req)
            throws ExperimentNotMatchingException, OutcomeNotMatchingException, BadTokenException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.outcomeAllowed(securedInfo, experimentId, exposureId, outcomeId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        try{
            outcomeService.deleteById(outcomeId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EmptyResultDataAccessException ex) {
            log.warn(ex.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/outcome_potentials")
    public ResponseEntity<List<OutcomePotentialDto>> outcomePotentials(@PathVariable long experimentId, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, DataServiceException, CanvasApiException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        List<OutcomePotentialDto> potentialDtoList = outcomeService.potentialOutcomes(experimentId, securedInfo);

        return new ResponseEntity<>(potentialDtoList, HttpStatus.OK);
    }

}
