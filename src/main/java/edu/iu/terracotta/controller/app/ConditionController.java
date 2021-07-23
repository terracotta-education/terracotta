package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.exceptions.ConditionsLockedException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.dto.ConditionDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.utils.TextConstants;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = ConditionController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ConditionController {

    static final Logger log = LoggerFactory.getLogger(ConditionController.class);
    static final String REQUEST_ROOT = "api/experiments";

    @Autowired
    ConditionService conditionService;

    @Autowired
    APIJWTService apijwtService;

    @RequestMapping(value = "/{experiment_id}/conditions", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<List<ConditionDto>> allConditionsByExperiment(@PathVariable("experiment_id") Long experimentId,
                                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            List<ConditionDto> conditionDtoList = conditionService.findAllByExperimentId(experimentId);
            if(conditionDtoList.isEmpty()){
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
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
            return new ResponseEntity<>(conditionService.getCondition(conditionId), HttpStatus.OK);
        }else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/conditions", method = RequestMethod.POST)
    public ResponseEntity<ConditionDto> postCondition(@PathVariable("experiment_id") Long experimentId,
                                                      @RequestBody ConditionDto conditionDto,
                                                      UriComponentsBuilder ucBuilder,
                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExperimentLockedException, TitleValidationException, ConditionsLockedException {

        log.info("Creating Condition : {}", conditionDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentLocked(experimentId,true);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.conditionsLocked(experimentId,true);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            if (conditionDto.getConditionId() != null){
                log.error(TextConstants.ID_IN_POST_ERROR);
                return new ResponseEntity(TextConstants.ID_IN_POST_ERROR, HttpStatus.CONFLICT);
            }
            conditionService.validateConditionName("", conditionDto.getName(), experimentId, 0L, false);

            conditionDto.setExperimentId(experimentId);
            Condition condition;
            try{
                condition = conditionService.fromDto(conditionDto);
            } catch (DataServiceException e) {
                return new ResponseEntity("Error 105: Unable to create condition:" + e.getMessage(), HttpStatus.BAD_REQUEST);
            }

            ConditionDto returnedDto = conditionService.toDto(conditionService.save(condition));
            HttpHeaders headers = conditionService.buildHeader(ucBuilder, experimentId, condition.getConditionId());
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
            throws ExperimentNotMatchingException, BadTokenException, ConditionNotMatchingException, TitleValidationException {

        log.info("Updating condition with id {}", conditionId);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.conditionAllowed(securedInfo, experimentId, conditionId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            Map<Condition, ConditionDto> map = new HashMap<>();
            Condition condition = conditionService.findByConditionId(conditionId);
            conditionService.validateConditionName(condition.getName(), conditionDto.getName(), experimentId, conditionId, true);
            map.put(condition, conditionDto);
            conditionService.updateCondition(map);
            return new ResponseEntity<>(HttpStatus.OK);
        }else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experiment_id}/conditions", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateConditions(@PathVariable("experiment_id") Long experimentId,
                                                 @RequestBody List<ConditionDto> conditionDtoList,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, ConditionNotMatchingException, BadTokenException, DataServiceException, TitleValidationException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if(apijwtService.isInstructorOrHigher(securedInfo)){
            Map<Condition, ConditionDto> map = new HashMap<>();
            for(ConditionDto conditionDto : conditionDtoList){
                apijwtService.conditionAllowed(securedInfo, experimentId,conditionDto.getConditionId());
                Condition condition = conditionService.findByConditionId(conditionDto.getConditionId());
                conditionService.validateConditionName(condition.getName(), conditionDto.getName(), experimentId, condition.getConditionId(), true);
                if(conditionService.duplicateNameInPut(map, condition)) {
                    return new ResponseEntity("Error 102: Condition names must be unique. Another condition you are trying to update already has this name.", HttpStatus.CONFLICT);
                }
                map.put(condition, conditionDto);
            }
            try{
                conditionService.updateCondition(map);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (Exception ex) {
                throw new DataServiceException("Error 105: An error occurred trying to update the condition list. No conditions were updated. " + ex.getMessage());
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/conditions/{condition_id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteExperiment(@PathVariable("experiment_id") Long experimentId,
                                                 @PathVariable("condition_id") Long conditionId,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ConditionNotMatchingException, ExperimentLockedException, ConditionsLockedException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentLocked(experimentId,true);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.conditionsLocked(experimentId,true);
        apijwtService.conditionAllowed(securedInfo, experimentId, conditionId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            if(conditionService.isDefaultCondition(conditionId)){
                return new ResponseEntity("Error 118: Cannot delete default condition. Another condition must be selected as the default condition before this condition can be deleted.", HttpStatus.CONFLICT);
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
