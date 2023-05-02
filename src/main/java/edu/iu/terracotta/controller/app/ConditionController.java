package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.exceptions.ConditionsLockedException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentConditionLimitReachedException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.dto.ConditionDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = ConditionController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ConditionController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/conditions";

    @Autowired
    private ConditionService conditionService;

    @Autowired
    private APIJWTService apijwtService;

    @GetMapping
    public ResponseEntity<List<ConditionDto>> allConditionsByExperiment(@PathVariable long experimentId, HttpServletRequest req) throws ExperimentNotMatchingException, BadTokenException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        List<ConditionDto> conditionDtoList = conditionService.findAllByExperimentId(experimentId);

        if (CollectionUtils.isEmpty(conditionDtoList)) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(conditionDtoList, HttpStatus.OK);
    }

    @GetMapping("/{conditionId}")
    public ResponseEntity<ConditionDto> getCondition(@PathVariable long experimentId,
                                                     @PathVariable long conditionId,
                                                     HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ConditionNotMatchingException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.conditionAllowed(securedInfo, experimentId, conditionId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(conditionService.getCondition(conditionId), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ConditionDto> postCondition(@PathVariable long experimentId,
                                                      @RequestBody ConditionDto conditionDto,
                                                      UriComponentsBuilder ucBuilder,
                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExperimentLockedException, TitleValidationException, ConditionsLockedException, IdInPostException, DataServiceException, ExperimentConditionLimitReachedException {

        log.debug("Creating Condition for experiment ID: {}", experimentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentLocked(experimentId,true);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        ConditionDto returnedDto = conditionService.postCondition(conditionDto, experimentId);
        HttpHeaders headers = conditionService.buildHeader(ucBuilder, experimentId, returnedDto.getConditionId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

    @PutMapping("/{conditionId}")
    public ResponseEntity<Void> updateCondition(@PathVariable long experimentId,
                                                @PathVariable long conditionId,
                                                @RequestBody ConditionDto conditionDto,
                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ConditionNotMatchingException, TitleValidationException {
        log.debug("Updating condition with id {}", conditionId);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.conditionAllowed(securedInfo, experimentId, conditionId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        Map<Condition, ConditionDto> map = new HashMap<>();
        Condition condition = conditionService.findByConditionId(conditionId);
        conditionService.validateConditionName(condition.getName(), conditionDto.getName(), experimentId, conditionId, true);
        map.put(condition, conditionDto);
        conditionService.updateCondition(map);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping
    public ResponseEntity<Void> updateConditions(@PathVariable long experimentId,
                                                 @RequestBody List<ConditionDto> conditionDtoList,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, ConditionNotMatchingException, BadTokenException, DataServiceException, TitleValidationException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        conditionService.validateConditionNames(conditionDtoList,experimentId,true);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        Map<Condition, ConditionDto> map = new HashMap<>();

        for (ConditionDto conditionDto : conditionDtoList) {
            apijwtService.conditionAllowed(securedInfo, experimentId,conditionDto.getConditionId());
            Condition condition = conditionService.findByConditionId(conditionDto.getConditionId());
            log.debug("Updating condition: " + condition.getConditionId());
            map.put(condition, conditionDto);
        }

        try {
            conditionService.updateCondition(map);

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception ex) {
            throw new DataServiceException(String.format("Error 105: An error occurred trying to update the condition list. No conditions were updated. %s", ex.getMessage()), ex);
        }
    }

    @DeleteMapping("/{conditionId}")
    public ResponseEntity<Void> deleteExperiment(@PathVariable long experimentId,
                                                 @PathVariable long conditionId,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ConditionNotMatchingException, ExperimentLockedException, ConditionsLockedException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentLocked(experimentId,true);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.conditionsLocked(experimentId,true);
        apijwtService.conditionAllowed(securedInfo, experimentId, conditionId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        if (conditionService.isDefaultCondition(conditionId)) {
            return new ResponseEntity("Error 118: Cannot delete default condition. Another condition must be selected as the default condition before this condition can be deleted.", HttpStatus.CONFLICT);
        }

        try {
            conditionService.deleteById(conditionId);

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EmptyResultDataAccessException ex) {
            log.warn(ex.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}
