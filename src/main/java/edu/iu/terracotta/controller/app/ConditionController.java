package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.dto.ConditionDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.utils.TextConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

@Controller
@RequestMapping(value = ConditionController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ConditionController {

    static final Logger log = LoggerFactory.getLogger(ConditionController.class);
    static final String REQUEST_ROOT = "api/experiments";

    @Autowired
    ConditionService conditionService;

    @Autowired
    APIJWTService apijwtService;

    @Autowired
    ExperimentService experimentService;

    @RequestMapping(value = "/{experiment_id}/conditions", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<List<ConditionDto>> allConditionsByExperiment(@PathVariable("experiment_id") Long experimentId,
                                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            List<Condition> conditionList =
                    conditionService.findAllByExperimentId(experimentId);
            if(conditionList.isEmpty()){
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            List<ConditionDto> conditionDtoList = new ArrayList<>();
            for(Condition condition : conditionList) {
                conditionDtoList.add(conditionService.toDto(condition));
            }
            return new ResponseEntity<>(conditionDtoList, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<ConditionDto> getCondition(@PathVariable("experiment_id") long experimentId,
                                                     @PathVariable("condition_id") long conditionId,
                                                     HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ConditionNotMatchingException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.conditionAllowed(securedInfo, experimentId, conditionId);

        if(apijwtService.isLearnerOrHigher(securedInfo)){
            Optional<Condition> condition = conditionService.findById(conditionId);

            if(!condition.isPresent()) {
                log.error("condition {} in experiment {} not found.", conditionId, experimentId);
                return new ResponseEntity("condition " + conditionId + " in experiment " + experimentId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            } else {
                ConditionDto conditionDto = conditionService.toDto(condition.get());
                return new ResponseEntity<>(conditionDto, HttpStatus.OK);
            }
        }else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/conditions", method = RequestMethod.POST)
    public ResponseEntity<ConditionDto> postCondition(@PathVariable("experiment_id") Long experimentId,
                                                      @RequestBody ConditionDto conditionDto,
                                                      UriComponentsBuilder ucBuilder,
                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {

        log.info("Creating Condition : {}", conditionDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            if (conditionDto.getConditionId() != null){
                log.error(TextConstants.ID_IN_POST_ERROR);
                return new ResponseEntity(TextConstants.ID_IN_POST_ERROR, HttpStatus.CONFLICT);
            }

            if(!StringUtils.isAllBlank(conditionDto.getName())){
                if(conditionDto.getName().length() >= 255){
                    return new ResponseEntity("A condition name must be 255 characters or less.", HttpStatus.BAD_REQUEST);
                }
                if(conditionService.nameAlreadyExists(conditionDto.getName(), experimentId, 0L)){
                    return new ResponseEntity("Cannot create condition. A condition with name \"" + conditionDto.getName() + "\" already exists.", HttpStatus.CONFLICT);
                }
            }

            conditionDto.setExperimentId(experimentId);
            Condition condition;
            try{
                condition = conditionService.fromDto(conditionDto);
            } catch (DataServiceException e) {
                return new ResponseEntity("Unable to create condition:" + e.getMessage(), HttpStatus.BAD_REQUEST);
            }

            Condition conditionSaved = conditionService.save(condition);
            ConditionDto returnedDto = conditionService.toDto(conditionSaved);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/conditions/{condition_id}")
                    .buildAndExpand(experimentId, condition.getConditionId()).toUri());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
        }else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateCondition(@PathVariable("experiment_id") Long experimentId,
                                                @PathVariable("condition_id") Long conditionId,
                                                @RequestBody ConditionDto conditionDto,
                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ConditionNotMatchingException {

        log.info("Updating condition with id {}", conditionId);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.conditionAllowed(securedInfo, experimentId, conditionId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            Optional<Condition> conditionSearchResult = conditionService.findById(conditionId);

            if(!conditionSearchResult.isPresent()) {
                log.error("Unable to update. Condition with id {} not found.", conditionId);
                return new ResponseEntity("Unable to update, Condition with id " + conditionId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            }
            if(StringUtils.isAllBlank(conditionDto.getName()) && StringUtils.isAllBlank(conditionSearchResult.get().getName())){
                return new ResponseEntity("Please give the condition a name.", HttpStatus.CONFLICT);
            }
            if(!StringUtils.isBlank(conditionDto.getName())){
                if(conditionDto.getName().length() > 255){
                    return new ResponseEntity("Condition name must be 255 characters or less.", HttpStatus.BAD_REQUEST);
                }
                if(conditionService.nameAlreadyExists(conditionDto.getName(), experimentId, conditionId)){
                    return new ResponseEntity("Unable to create the condition. A condition with title \"" + conditionDto.getName() + "\" already exists in this experiment.", HttpStatus.CONFLICT);
                }
            }
            Condition conditionToChange = conditionSearchResult.get();
            conditionToChange.setName(conditionDto.getName());
            conditionToChange.setDefaultCondition(conditionDto.getDefaultCondition());
            conditionToChange.setDistributionPct((conditionDto.getDistributionPct()));

            conditionService.saveAndFlush(conditionToChange);
            return new ResponseEntity<>(HttpStatus.OK);
        }else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateConditions(@PathVariable("experiment_id") Long experimentId,
                                                 @RequestBody List<ConditionDto> conditionDtoList,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, ConditionNotMatchingException, BadTokenException, DataServiceException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if(apijwtService.isInstructorOrHigher(securedInfo)){
            List<Condition> conditionList = new ArrayList<>();

            for(ConditionDto conditionDto : conditionDtoList){
                apijwtService.conditionAllowed(securedInfo, experimentId,conditionDto.getConditionId());
                Optional<Condition> condition = conditionService.findById(conditionDto.getConditionId());
                if(condition.isPresent()){
                    Condition conditionToChange = condition.get();
                    if(StringUtils.isAllBlank(conditionDto.getName()) && StringUtils.isAllBlank(conditionToChange.getName())){
                        return new ResponseEntity("Please give the condition a name.", HttpStatus.CONFLICT);
                    }
                    if(!StringUtils.isBlank(conditionDto.getName())){
                        if(conditionDto.getName().length() > 255){
                            return new ResponseEntity("Condition name must be 255 characters or less.", HttpStatus.BAD_REQUEST);
                        }
                        if(conditionService.nameAlreadyExists(conditionDto.getName(), experimentId, conditionToChange.getConditionId())){
                            return new ResponseEntity("Unable to create the condition. A condition with title \"" + conditionDto.getName() + "\" already exists in this experiment.", HttpStatus.CONFLICT);
                        }
                    }
                    conditionToChange.setName(conditionDto.getName());
                    conditionToChange.setDefaultCondition(conditionDto.getDefaultCondition());
                    conditionToChange.setDistributionPct(conditionDto.getDistributionPct());
                    for(Condition conditionInList : conditionList){
                        if(conditionToChange.getName().equals(conditionInList.getName())){
                            return new ResponseEntity("Conditions cannot have identical names.", HttpStatus.CONFLICT);
                        }
                    }
                    conditionList.add(conditionToChange);
                } else {
                    return new ResponseEntity("Unable to update. Condition with id " + conditionDto.getConditionId() + " not found.", HttpStatus.NOT_FOUND);
                }
            }

            try{
                conditionService.saveAllConditions(conditionList);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (Exception ex) {
                throw new DataServiceException("An error occurred trying to update the condition list. No conditions were updated. " + ex.getMessage());
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteExperiment(@PathVariable("experiment_id") Long experimentId,
                                                 @PathVariable("condition_id") Long conditionId,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ConditionNotMatchingException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.conditionAllowed(securedInfo, experimentId, conditionId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            if(conditionService.isDefaultCondition(conditionId)){
                return new ResponseEntity("Cannot delete default condition. If you would like to delete this condition, please select another condition to be the default condition.", HttpStatus.CONFLICT);
            }
            try {
                conditionService.deleteById(conditionId);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (EmptyResultDataAccessException ex) {
                log.error(ex.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else{
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }
}
