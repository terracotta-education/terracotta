package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.security.app.roles.InstructorOrHigher;
import edu.iu.terracotta.security.app.roles.LearnerOrHigher;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.exceptions.ConditionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.ConditionDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.ConditionsLockedException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentConditionLimitReachedException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.ConditionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = ConditionController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ConditionController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/conditions";

    private final ConditionService conditionService;
    private final ApiJwtService apijwtService;
    private final ExperimentService experimentService;

    @GetMapping
    @LearnerOrHigher
    public ResponseEntity<List<ConditionDto>> allConditionsByExperiment(@PathVariable("experimentId") UUID experimentUuid, HttpServletRequest req) throws ExperimentNotMatchingException, BadTokenException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        List<ConditionDto> conditionDtoList = conditionService.findAllByExperimentId(experimentId);

        if (CollectionUtils.isEmpty(conditionDtoList)) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(conditionDtoList, HttpStatus.OK);
    }

    @GetMapping("/{conditionId}")
    @LearnerOrHigher
    public ResponseEntity<ConditionDto> getCondition(@PathVariable("experimentId") UUID experimentUuid,
                                                     @PathVariable("conditionId") UUID conditionUuid,
                                                     HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ConditionNotMatchingException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        long conditionId = conditionService.getConditionIdByUuid(conditionUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.conditionAllowed(securedInfo, experimentId, conditionId);

        return new ResponseEntity<>(conditionService.getCondition(conditionId), HttpStatus.OK);
    }

    @PostMapping
    @InstructorOrHigher
    public ResponseEntity<ConditionDto> postCondition(@PathVariable("experimentId") UUID experimentUuid,
                                                      @RequestBody(required = false) ConditionDto conditionDto,
                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExperimentLockedException, TitleValidationException, ConditionsLockedException, IdInPostException, DataServiceException, ExperimentConditionLimitReachedException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentLocked(experimentId,true);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (conditionDto == null) {
            conditionDto = ConditionDto.builder().build();
        }

        ConditionDto returnedDto = conditionService.postCondition(conditionDto, experimentId);
        log.debug("Created condition ID: [{}] for experiment ID: [{}]", returnedDto.getConditionId(), experimentUuid);

        return new ResponseEntity<>(returnedDto, HttpStatus.CREATED);
    }

    @PutMapping("/{conditionId}")
    @InstructorOrHigher
    public ResponseEntity<Void> updateCondition(@PathVariable("experimentId") UUID experimentUuid,
                                                @PathVariable("conditionId") UUID conditionUuid,
                                                @RequestBody ConditionDto conditionDto,
                                                HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ConditionNotMatchingException, TitleValidationException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        long conditionId = conditionService.getConditionIdByUuid(conditionUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.conditionAllowed(securedInfo, experimentId, conditionId);

        Map<Condition, ConditionDto> map = new HashMap<>();
        Condition condition = conditionService.findByConditionId(conditionId);
        conditionService.validateConditionName(condition.getName(), conditionDto.getName(), experimentId, conditionId, true);
        map.put(condition, conditionDto);
        conditionService.updateCondition(map);
        log.debug("Updated condition ID: [{}]", conditionUuid);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping
    @InstructorOrHigher
    public ResponseEntity<Void> updateConditions(@PathVariable("experimentId") UUID experimentUuid,
                                                 @RequestBody List<ConditionDto> conditionDtoList,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, ConditionNotMatchingException, BadTokenException, DataServiceException, TitleValidationException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        conditionService.validateConditionNames(conditionDtoList,experimentId,true);

        Map<Condition, ConditionDto> map = new HashMap<>();

        for (ConditionDto conditionDto : conditionDtoList) {
            long conditionId = conditionService.getConditionIdByUuid(conditionDto.getConditionId());
            apijwtService.conditionAllowed(securedInfo, experimentId, conditionId);
            Condition condition = conditionService.findByConditionId(conditionId);
            map.put(condition, conditionDto);
        }

        try {
            conditionService.updateCondition(map);
            log.debug("Updated condition IDs: {}", conditionDtoList.stream().map(ConditionDto::getConditionId).toList());

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception ex) {
            throw new DataServiceException(String.format("Error 105: An error occurred trying to update the condition list. No conditions were updated. %s", ex.getMessage()), ex);
        }
    }

    @DeleteMapping("/{conditionId}")
    @InstructorOrHigher
    public ResponseEntity<Void> deleteCondition(@PathVariable("experimentId") UUID experimentUuid,
                                                 @PathVariable("conditionId") UUID conditionUuid,
                                                 HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ConditionNotMatchingException, ExperimentLockedException, ConditionsLockedException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        long conditionId = conditionService.getConditionIdByUuid(conditionUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentLocked(experimentId,true);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.conditionsLocked(experimentId,true);
        apijwtService.conditionAllowed(securedInfo, experimentId, conditionId);

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
