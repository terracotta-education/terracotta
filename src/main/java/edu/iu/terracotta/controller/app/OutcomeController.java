package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.app.Outcome;
import edu.iu.terracotta.model.app.dto.OutcomeDto;
import edu.iu.terracotta.model.app.dto.OutcomePotentialDto;
import edu.iu.terracotta.model.app.enumerator.LmsType;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.OutcomeService;
import edu.iu.terracotta.utils.TextConstants;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
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

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.exposureAllowed(securityInfo, experimentId, exposureId);

        if(apijwtService.isLearnerOrHigher(securityInfo)){
            List<Outcome> outcomeList = outcomeService.findAllByExposureId(exposureId);

            if(outcomeList.isEmpty()){
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            List<OutcomeDto> outcomeDtoList = new ArrayList<>();
            for(Outcome outcome : outcomeList){
                outcomeDtoList.add(outcomeService.toDto(outcome, false));
            }
            return new ResponseEntity<>(outcomeDtoList, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
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

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.outcomeAllowed(securityInfo, experimentId, exposureId, outcomeId);

        if(apijwtService.isLearnerOrHigher(securityInfo)){
            //if we are here, the outcome exists...
            if (updateScores) {
                outcomeService.updateOutcomeGrades(outcomeId, securityInfo);
            }
            Optional<Outcome> outcomeSearchResult = outcomeService.findById(outcomeId);

            if(!outcomeSearchResult.isPresent()){
                log.error("Outcome in platform {} and context {} and experiment {} and exposure {} with id {} not found",
                        securityInfo.getPlatformDeploymentId(), securityInfo.getContextId(), experimentId, experimentId, outcomeId);
                return new ResponseEntity("Outcome in platform " + securityInfo.getPlatformDeploymentId() + " and context " + securityInfo.getContextId() +
                        " and experiment with id " + experimentId + " and exposure id " + exposureId + " with id " + outcomeId +  TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            } else {


                OutcomeDto outcomeDto = outcomeService.toDto(outcomeSearchResult.get(), outcomeScores);
                return new ResponseEntity<>(outcomeDto, HttpStatus.OK);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/exposures/{exposure_id}/outcomes", method = RequestMethod.POST)
    public ResponseEntity<OutcomeDto> postOutcome(@PathVariable("experiment_id") Long experimentId,
                                                  @PathVariable("exposure_id") Long exposureId,
                                                  @RequestBody OutcomeDto outcomeDto,
                                                  UriComponentsBuilder ucBuilder,
                                                  HttpServletRequest req)
            throws ExperimentNotMatchingException, ExposureNotMatchingException, BadTokenException{

        log.info("Creating Outcome: {}", outcomeDto);
        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.exposureAllowed(securityInfo, experimentId, exposureId);

        if(apijwtService.isInstructorOrHigher(securityInfo)){
            if(outcomeDto.getOutcomeId() != null){
                log.error(TextConstants.ID_IN_POST_ERROR);
                return new ResponseEntity(TextConstants.ID_IN_POST_ERROR, HttpStatus.CONFLICT);
            }
            if(!StringUtils.isAllBlank(outcomeDto.getTitle()) && outcomeDto.getTitle().length() > 255){
                return new ResponseEntity("The title must be 255 characters or less.", HttpStatus.BAD_REQUEST);
            }

            outcomeDto.setExposureId(exposureId);
            if(outcomeDto.getExternal() != null) {
                if(!outcomeDto.getExternal()){
                    outcomeDto.setLmsOutcomeId(null);
                    outcomeDto.setLmsType("NONE");
                }
            }
            Outcome outcome;
            try{
                outcome = outcomeService.fromDto(outcomeDto);
            } catch (DataServiceException ex) {
                return new ResponseEntity("Unable to create Outcome: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
            }

            Outcome outcomeSaved = outcomeService.save(outcome);
            OutcomeDto returnedDto = outcomeService.toDto(outcomeSaved, false);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/exposures/{exposure_id}/outcomes/{outcome_id}")
                    .buildAndExpand(experimentId, exposureId, outcome.getOutcomeId()).toUri());
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
            throws ExperimentNotMatchingException, OutcomeNotMatchingException, BadTokenException {

        log.info("Updating outcome with id {}", outcomeId);
        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.outcomeAllowed(securityInfo, experimentId, exposureId, outcomeId);

        if(apijwtService.isInstructorOrHigher(securityInfo)){
            Optional<Outcome> outcomeSearchResult = outcomeService.findById(outcomeId);

            if(!outcomeSearchResult.isPresent()){
                log.error("Unable to update. Outcome with id {} not found.", outcomeId);
                return new ResponseEntity("Unable to update. Outcome with id " + outcomeId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            }
            if(StringUtils.isAllBlank(outcomeDto.getTitle()) && StringUtils.isAllBlank(outcomeSearchResult.get().getTitle())){
                return new ResponseEntity("Please give the outcome a title.", HttpStatus.CONFLICT);
            }
            if(!StringUtils.isAllBlank(outcomeDto.getTitle()) && outcomeDto.getTitle().length() > 255){
                return new ResponseEntity("The title must be 255 characters or less.", HttpStatus.BAD_REQUEST);
            }
            Outcome outcomeToChange = outcomeSearchResult.get();
            //only allow external to be changed if the current value is null. (Only allow it to be changed once)
            if(outcomeToChange.getExternal() == null && outcomeDto.getExternal() != null){
                outcomeToChange.setExternal(outcomeDto.getExternal());
                if(!outcomeDto.getExternal()){
                    outcomeToChange.setLmsOutcomeId(null);
                    outcomeToChange.setLmsType(EnumUtils.getEnum(LmsType.class, LmsType.none.name()));
                } else {
                    outcomeToChange.setLmsOutcomeId(outcomeDto.getLmsOutcomeId());
                    outcomeToChange.setLmsType(EnumUtils.getEnum(LmsType.class, outcomeDto.getLmsType()));
                }
            }
            outcomeToChange.setTitle(outcomeDto.getTitle());
            outcomeToChange.setMaxPoints(outcomeDto.getMaxPoints());

            outcomeService.saveAndFlush(outcomeToChange);
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

        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.outcomeAllowed(securityInfo, experimentId, exposureId, outcomeId);

        if(apijwtService.isInstructorOrHigher(securityInfo)){
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
        SecurityInfo securityInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        if(apijwtService.isInstructorOrHigher(securityInfo)){
            List<OutcomePotentialDto> potentialDtoList = outcomeService.potentialOutcomes(experimentId);
            return new ResponseEntity<>(potentialDtoList, HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

}
