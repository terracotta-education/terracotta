package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.Outcome;
import edu.iu.terracotta.model.app.dto.OutcomeDto;
import edu.iu.terracotta.model.app.dto.OutcomePotentialDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.OutcomeService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@Controller
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = OutcomeController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class OutcomeController {

    static final String REQUEST_ROOT = "api/experiments";
    static final Logger log = LoggerFactory.getLogger(OutcomeController.class);

    @Autowired
    OutcomeService outcomeService;

    @Autowired
    APIJWTService apijwtService;


    @RequestMapping(value = "/{experiment_id}/exposures/{exposure_id}/outcomes", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<OutcomeDto>> allOutcomesByExposure(@PathVariable("experiment_id") Long experimentId,
                                                                  @PathVariable("exposure_id") Long exposureId,
                                                                  HttpServletRequest req)
            throws ExperimentNotMatchingException, ExposureNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if(apijwtService.isLearnerOrHigher(securedInfo)){
            List<OutcomeDto> outcomeList = outcomeService.getOutcomes(exposureId);
            if(outcomeList.isEmpty()){
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(outcomeList, HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/exposures/{exposure_id}/outcomes/{outcome_id}", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<OutcomeDto> getOutcome(@PathVariable("experiment_id") Long experimentId,
                                                 @PathVariable("exposure_id") Long exposureId,
                                                 @PathVariable("outcome_id") Long outcomeId,
                                                 @RequestParam(name = "outcome_scores", defaultValue = "false") boolean outcomeScores,
                                                 @RequestParam(name = "update_scores", defaultValue = "true") boolean updateScores,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, OutcomeNotMatchingException, BadTokenException, CanvasApiException, ParticipantNotUpdatedException, IOException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.outcomeAllowed(securedInfo, experimentId, exposureId, outcomeId);

        if(apijwtService.isLearnerOrHigher(securedInfo)){
            //if we are here, the outcome exists...
            if (updateScores) {
                outcomeService.updateOutcomeGrades(outcomeId, securedInfo);
            }
            OutcomeDto outcomeDto = outcomeService.toDto(outcomeService.getOutcome(outcomeId), outcomeScores);
            return new ResponseEntity<>(outcomeDto, HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/exposures/{exposure_id}/outcomes", method = RequestMethod.POST)
    public ResponseEntity<OutcomeDto> postOutcome(@PathVariable("experiment_id") Long experimentId,
                                                  @PathVariable("exposure_id") Long exposureId,
                                                  @RequestBody OutcomeDto outcomeDto,
                                                  UriComponentsBuilder ucBuilder,
                                                  HttpServletRequest req)
            throws ExperimentNotMatchingException, ExposureNotMatchingException, BadTokenException, TitleValidationException {

        log.info("Creating Outcome: {}", outcomeDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.exposureAllowed(securedInfo, experimentId, exposureId);

        if(apijwtService.isInstructorOrHigher(securedInfo)){
            if(outcomeDto.getOutcomeId() != null){
                log.error(TextConstants.ID_IN_POST_ERROR);
                return new ResponseEntity(TextConstants.ID_IN_POST_ERROR, HttpStatus.CONFLICT);
            }

            outcomeDto.setExposureId(exposureId);
            outcomeService.defaultOutcome(outcomeDto);
            Outcome outcome;
            try{
                outcome = outcomeService.fromDto(outcomeDto);
            } catch (DataServiceException ex) {
                return new ResponseEntity("Error 105: Unable to create Outcome: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
            }
            OutcomeDto returnedDto = outcomeService.toDto(outcomeService.save(outcome), false);
            HttpHeaders headers = outcomeService.buildHeaders(ucBuilder, experimentId, exposureId, returnedDto.getOutcomeId());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value= "/{experiment_id}/exposures/{exposure_id}/outcomes/{outcome_id}", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateOutcome(@PathVariable("experiment_id") Long experimentId,
                                              @PathVariable("exposure_id") Long exposureId,
                                              @PathVariable("outcome_id") Long outcomeId,
                                              @RequestBody OutcomeDto outcomeDto,
                                              HttpServletRequest req)
            throws ExperimentNotMatchingException, OutcomeNotMatchingException, BadTokenException, TitleValidationException {

        log.info("Updating outcome with id {}", outcomeId);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.outcomeAllowed(securedInfo, experimentId, exposureId, outcomeId);

        if(apijwtService.isInstructorOrHigher(securedInfo)){
            outcomeService.updateOutcome(outcomeId, outcomeDto);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/exposures/{exposure_id}/outcomes/{outcome_id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteOutcome(@PathVariable("experiment_id") Long experimentId,
                                              @PathVariable("exposure_id") Long exposureId,
                                              @PathVariable("outcome_id") Long outcomeId,
                                              HttpServletRequest req)
            throws ExperimentNotMatchingException, OutcomeNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.outcomeAllowed(securedInfo, experimentId, exposureId, outcomeId);

        if(apijwtService.isInstructorOrHigher(securedInfo)){
            try{
                outcomeService.deleteById(outcomeId);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (EmptyResultDataAccessException ex) {
                log.error(ex.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/outcome_potentials", method = RequestMethod.GET, produces = "application/json;")
    public ResponseEntity<List<OutcomePotentialDto>> outcomePotentials(@PathVariable("experiment_id") Long experimentId,
                                                                       HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, DataServiceException, CanvasApiException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        if(apijwtService.isInstructorOrHigher(securedInfo)){
            List<OutcomePotentialDto> potentialDtoList = outcomeService.potentialOutcomes(experimentId);
            return new ResponseEntity<>(potentialDtoList, HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }
}