package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.exceptions.OutcomeScoreNotMatchingException;
import edu.iu.terracotta.model.app.OutcomeScore;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.dto.OutcomeScoreDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.OutcomeScoreService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.utils.TextConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping(value = OutcomeScoreController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class OutcomeScoreController {

    static final String REQUEST_ROOT = "api/experiments";
    static final Logger log = LoggerFactory.getLogger(OutcomeScoreController.class);

    @Autowired
    OutcomeScoreService outcomeScoreService;

    @Autowired
    ParticipantService participantService;

    @Autowired
    APIJWTService apijwtService;


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
            List<OutcomeScore> outcomeScoreList = outcomeScoreService.findAllByOutcomeId(outcomeId);

            if (outcomeScoreList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            List<OutcomeScoreDto> outcomeScoreDtoList = new ArrayList<>();
            for (OutcomeScore outcomeScore : outcomeScoreList) {
                outcomeScoreDtoList.add(outcomeScoreService.toDto(outcomeScore));
            }
            return new ResponseEntity<>(outcomeScoreDtoList, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
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
            Optional<OutcomeScore> outcomeScoreSearchResult = outcomeScoreService.findById(outcomeScoreId);

            if (!outcomeScoreSearchResult.isPresent()) {
                log.error("Outcome score in platform {} and context {} and experiment {} and exposure {} and outcome {} with id {} not found",
                        securedInfo.getPlatformDeploymentId(), securedInfo.getContextId(), experimentId, exposureId, outcomeId, outcomeScoreId);
                return new ResponseEntity("Outcome score in platform " + securedInfo.getPlatformDeploymentId() + " and context " + securedInfo.getContextId() +
                        " and experiment with id " + experimentId + " and exposure id " + experimentId + " and outcome id " + outcomeId + " with id " + outcomeScoreId +
                        TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            } else {
                OutcomeScoreDto outcomeScoreDto = outcomeScoreService.toDto(outcomeScoreSearchResult.get());
                return new ResponseEntity<>(outcomeScoreDto, HttpStatus.OK);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/exposures/{exposure_id}/outcomes/{outcome_id}/outcome_scores", method = RequestMethod.POST)
    public ResponseEntity<OutcomeScoreDto> postOutcomeScore(@PathVariable("experiment_id") Long experimentId,
                                                            @PathVariable("exposure_id") Long exposureId,
                                                            @PathVariable("outcome_id") Long outcomeId,
                                                            @RequestBody OutcomeScoreDto outcomeScoreDto,
                                                            UriComponentsBuilder ucBuilder,
                                                            HttpServletRequest req)
            throws ExperimentNotMatchingException, OutcomeNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.outcomeAllowed(securedInfo, experimentId, exposureId, outcomeId);

        if(apijwtService.isInstructorOrHigher(securedInfo)){
            if(outcomeScoreDto.getOutcomeScoreId() != null) {
                log.error(TextConstants.ID_IN_POST_ERROR);
                return new ResponseEntity(TextConstants.ID_IN_POST_ERROR, HttpStatus.CONFLICT);
            }
            if(outcomeScoreDto.getParticipantId() == null){
                log.error("Must include a valid participant id in the POST.");
                return new ResponseEntity("Must include a valid participant id in the POST", HttpStatus.BAD_REQUEST);
            }
            Optional<Participant> participant = participantService.findByParticipantIdAndExperimentId(outcomeScoreDto.getParticipantId(), experimentId);
            if(!participant.isPresent()){
                log.error("The participant provided does not belong to this experiment.");
                return new ResponseEntity("The participant provided does not belong to this experiment.", HttpStatus.CONFLICT);
            }
            outcomeScoreDto.setOutcomeId(outcomeId);
            OutcomeScore outcomeScore;
            try{
                outcomeScore = outcomeScoreService.fromDto(outcomeScoreDto);
            } catch (DataServiceException ex) {
                return new ResponseEntity("Unable to create outcome score: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
            }

            OutcomeScore outcomeScoreSaved = outcomeScoreService.save(outcomeScore);
            OutcomeScoreDto returnedDto = outcomeScoreService.toDto(outcomeScoreSaved);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/exposures/{exposure_id}/outcomes/{outcome_id}/outcome_scores/{outcome_score_id}")
                .buildAndExpand(experimentId, exposureId, outcomeId, outcomeScore.getOutcomeScoreId()).toUri());
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

        log.info("Updating outcome score with id {}", outcomeScoreId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.outcomeAllowed(securedInfo, experimentId, exposureId, outcomeId);
        apijwtService.outcomeScoreAllowed(securedInfo, outcomeId, outcomeScoreId);

        if(apijwtService.isInstructorOrHigher(securedInfo)){
            Optional<OutcomeScore> outcomeScoreSearchResult = outcomeScoreService.findById(outcomeScoreId);

            if(!outcomeScoreSearchResult.isPresent()){
                log.error("Unable to update. Outcome score with id {} not found.", outcomeScoreId);
                return new ResponseEntity("Unable to update. Outcome score with id " + outcomeScoreId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            }
            OutcomeScore outcomeScoreToChange = outcomeScoreSearchResult.get();
            outcomeScoreToChange.setScoreNumeric(outcomeScoreDto.getScoreNumeric());

            outcomeScoreService.saveAndFlush(outcomeScoreToChange);
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
                log.error(ex.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }
}
