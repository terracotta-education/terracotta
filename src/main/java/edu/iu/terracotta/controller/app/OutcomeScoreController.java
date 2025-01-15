package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.dao.exceptions.OutcomeScoreNotMatchingException;
import edu.iu.terracotta.dao.model.dto.OutcomeScoreDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidParticipantException;
import edu.iu.terracotta.service.app.OutcomeScoreService;
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
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@Controller
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = OutcomeScoreController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class OutcomeScoreController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/exposures/{exposureId}/outcomes/{outcomeId}/outcome_scores";

    @Autowired private OutcomeScoreService outcomeScoreService;
    @Autowired private ApiJwtService apijwtService;

    @GetMapping
    public ResponseEntity<List<OutcomeScoreDto>> getAllOutcomeScoresByOutcome(@PathVariable long experimentId,
                                                                              @PathVariable long exposureId,
                                                                              @PathVariable long outcomeId,
                                                                              HttpServletRequest req)
            throws ExperimentNotMatchingException, OutcomeNotMatchingException, BadTokenException, NumberFormatException, TerracottaConnectorException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.outcomeAllowed(securedInfo, experimentId, exposureId, outcomeId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        List<OutcomeScoreDto> outcomeScoreList = outcomeScoreService.getOutcomeScores(outcomeId);

        if (outcomeScoreList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(outcomeScoreList, HttpStatus.OK);
    }

    @GetMapping("/{outcomeScoreId}")
    public ResponseEntity<OutcomeScoreDto> getOutcomeScore(@PathVariable long experimentId,
                                                           @PathVariable long exposureId,
                                                           @PathVariable long outcomeId,
                                                           @PathVariable long outcomeScoreId,
                                                           HttpServletRequest req)
            throws ExperimentNotMatchingException, OutcomeNotMatchingException, OutcomeScoreNotMatchingException, BadTokenException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.outcomeAllowed(securedInfo, experimentId, exposureId, outcomeId);
        apijwtService.outcomeScoreAllowed(securedInfo, outcomeId, outcomeScoreId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        OutcomeScoreDto outcomeScoreDto = outcomeScoreService.toDto(outcomeScoreService.getOutcomeScore(outcomeScoreId));

        return new ResponseEntity<>(outcomeScoreDto, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<OutcomeScoreDto> postOutcomeScore(@PathVariable long experimentId,
                                                            @PathVariable long exposureId,
                                                            @PathVariable long outcomeId,
                                                            @RequestBody OutcomeScoreDto outcomeScoreDto,
                                                            UriComponentsBuilder ucBuilder,
                                                            HttpServletRequest req)
            throws ExperimentNotMatchingException, OutcomeNotMatchingException, BadTokenException, InvalidParticipantException, IdInPostException, DataServiceException, NumberFormatException, TerracottaConnectorException {
        log.debug("Creating outcome score for outcome ID: {}", outcomeId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.outcomeAllowed(securedInfo, experimentId, exposureId, outcomeId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        OutcomeScoreDto returnedDto = outcomeScoreService.postOutcomeScore(outcomeScoreDto, experimentId, outcomeId);
        HttpHeaders headers = outcomeScoreService.buildHeaders(ucBuilder, experimentId, exposureId, outcomeId, returnedDto.getOutcomeScoreId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

    @PutMapping("/{outcomeScoreId}")
    public ResponseEntity<Void> updateOutcomeScore(@PathVariable long experimentId,
                                              @PathVariable long exposureId,
                                              @PathVariable long outcomeId,
                                              @PathVariable long outcomeScoreId,
                                              @RequestBody OutcomeScoreDto outcomeScoreDto,
                                              HttpServletRequest req)
            throws ExperimentNotMatchingException, OutcomeNotMatchingException, OutcomeScoreNotMatchingException, BadTokenException, NumberFormatException, TerracottaConnectorException {
        log.debug("Updating outcome score with id {}", outcomeScoreId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.outcomeAllowed(securedInfo, experimentId, exposureId, outcomeId);
        apijwtService.outcomeScoreAllowed(securedInfo, outcomeId, outcomeScoreId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        outcomeScoreService.updateOutcomeScore(outcomeScoreId, outcomeScoreDto);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{outcomeScoreId}")
    public ResponseEntity<Void> deleteOutcomeScore(@PathVariable long experimentId,
                                                   @PathVariable long exposureId,
                                                   @PathVariable long outcomeId,
                                                   @PathVariable long outcomeScoreId,
                                                   HttpServletRequest req)
            throws ExperimentNotMatchingException, OutcomeNotMatchingException, OutcomeScoreNotMatchingException, BadTokenException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.outcomeAllowed(securedInfo, experimentId, exposureId, outcomeId);
        apijwtService.outcomeScoreAllowed(securedInfo, outcomeId, outcomeScoreId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        try {
            outcomeScoreService.deleteById(outcomeScoreId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EmptyResultDataAccessException ex) {
            log.warn(ex.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}
