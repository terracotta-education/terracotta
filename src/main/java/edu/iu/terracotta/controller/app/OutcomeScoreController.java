package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidParticipantException;
import edu.iu.terracotta.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.exceptions.OutcomeScoreNotMatchingException;
import edu.iu.terracotta.model.app.dto.OutcomeScoreDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.OutcomeScoreService;
import edu.iu.terracotta.utils.TextConstants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Slf4j
@Controller
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = OutcomeScoreController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class OutcomeScoreController {

    public static final String REQUEST_ROOT = "api/experiments";

    @Autowired
    private OutcomeScoreService outcomeScoreService;

    @Autowired
    private APIJWTService apijwtService;

    @RequestMapping(value = "/{experiment_id}/exposures/{exposure_id}/outcomes/{outcome_id}/outcome_scores", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<OutcomeScoreDto>> getAllOutcomeScoresByOutcome(@PathVariable("experiment_id") Long experimentId,
                                                                              @PathVariable("exposure_id") Long exposureId,
                                                                              @PathVariable("outcome_id") Long outcomeId,
                                                                              HttpServletRequest req)
            throws ExperimentNotMatchingException, OutcomeNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.outcomeAllowed(securedInfo, experimentId, exposureId, outcomeId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            List<OutcomeScoreDto> outcomeScoreList = outcomeScoreService.getOutcomeScores(outcomeId);
            if (outcomeScoreList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(outcomeScoreList, HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/exposures/{exposure_id}/outcomes/{outcome_id}/outcome_scores/{outcome_score_id}", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<OutcomeScoreDto> getOutcomeScore(@PathVariable("experiment_id") Long experimentId,
                                                           @PathVariable("exposure_id") Long exposureId,
                                                           @PathVariable("outcome_id") Long outcomeId,
                                                           @PathVariable("outcome_score_id") Long outcomeScoreId,
                                                           HttpServletRequest req)
            throws ExperimentNotMatchingException, OutcomeNotMatchingException, OutcomeScoreNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.outcomeAllowed(securedInfo, experimentId, exposureId, outcomeId);
        apijwtService.outcomeScoreAllowed(securedInfo, outcomeId, outcomeScoreId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            OutcomeScoreDto outcomeScoreDto = outcomeScoreService.toDto(outcomeScoreService.getOutcomeScore(outcomeScoreId));
            return new ResponseEntity<>(outcomeScoreDto, HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/exposures/{exposure_id}/outcomes/{outcome_id}/outcome_scores", method = RequestMethod.POST)
    public ResponseEntity<OutcomeScoreDto> postOutcomeScore(@PathVariable("experiment_id") Long experimentId,
                                                            @PathVariable("exposure_id") Long exposureId,
                                                            @PathVariable("outcome_id") Long outcomeId,
                                                            @RequestBody OutcomeScoreDto outcomeScoreDto,
                                                            UriComponentsBuilder ucBuilder,
                                                            HttpServletRequest req)
            throws ExperimentNotMatchingException, OutcomeNotMatchingException, BadTokenException, InvalidParticipantException, IdInPostException, DataServiceException {

        log.debug("Creating outcome score for outcome ID: {}", outcomeId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.outcomeAllowed(securedInfo, experimentId, exposureId, outcomeId);

        if(apijwtService.isInstructorOrHigher(securedInfo)){
            OutcomeScoreDto returnedDto = outcomeScoreService.postOutcomeScore(outcomeScoreDto, experimentId, outcomeId);
            HttpHeaders headers = outcomeScoreService.buildHeaders(ucBuilder, experimentId, exposureId, outcomeId, returnedDto.getOutcomeScoreId());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value= "/{experiment_id}/exposures/{exposure_id}/outcomes/{outcome_id}/outcome_scores/{outcome_score_id}", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateOutcomeScore(@PathVariable("experiment_id") Long experimentId,
                                              @PathVariable("exposure_id") Long exposureId,
                                              @PathVariable("outcome_id") Long outcomeId,
                                              @PathVariable("outcome_score_id") Long outcomeScoreId,
                                              @RequestBody OutcomeScoreDto outcomeScoreDto,
                                              HttpServletRequest req)
            throws ExperimentNotMatchingException, OutcomeNotMatchingException, OutcomeScoreNotMatchingException, BadTokenException {

        log.debug("Updating outcome score with id {}", outcomeScoreId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.outcomeAllowed(securedInfo, experimentId, exposureId, outcomeId);
        apijwtService.outcomeScoreAllowed(securedInfo, outcomeId, outcomeScoreId);

        if(apijwtService.isInstructorOrHigher(securedInfo)){
            outcomeScoreService.updateOutcomeScore(outcomeScoreId, outcomeScoreDto);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/exposures/{exposure_id}/outcomes/{outcome_id}/outcome_scores/{outcome_score_id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteOutcomeScore(@PathVariable("experiment_id") Long experimentId,
                                                   @PathVariable("exposure_id") Long exposureId,
                                                   @PathVariable("outcome_id") Long outcomeId,
                                                   @PathVariable("outcome_score_id") Long outcomeScoreId,
                                                   HttpServletRequest req)
            throws ExperimentNotMatchingException, OutcomeNotMatchingException, OutcomeScoreNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.outcomeAllowed(securedInfo, experimentId, exposureId, outcomeId);
        apijwtService.outcomeScoreAllowed(securedInfo, outcomeId, outcomeScoreId);

        if(apijwtService.isInstructorOrHigher(securedInfo)){
            try{
                outcomeScoreService.deleteById(outcomeScoreId);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (EmptyResultDataAccessException ex) {
                log.warn(ex.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }
}
