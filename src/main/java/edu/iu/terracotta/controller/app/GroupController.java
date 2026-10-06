package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.security.app.roles.InstructorOrHigher;
import edu.iu.terracotta.security.app.roles.LearnerOrHigher;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.dao.model.dto.GroupDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
import java.util.UUID;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement"})
@RequestMapping(value = GroupController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class GroupController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/groups";

    private final GroupService groupService;
    private final ApiJwtService apijwtService;
    private final ExperimentService experimentService;

    @GetMapping
    @LearnerOrHigher
    public ResponseEntity<List<GroupDto>> allGroupsByExperiment(@PathVariable("experimentId") UUID experimentUuid, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        List<GroupDto> groupList = groupService.getGroups(experimentId, securedInfo);

        if (groupList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(groupList, HttpStatus.OK);
    }

    @GetMapping("/{groupId}")
    @LearnerOrHigher
    public ResponseEntity<GroupDto> getGroup(@PathVariable("experimentId") UUID experimentUuid, @PathVariable("groupId") UUID groupUuid, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, GroupNotMatchingException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        long groupId = groupService.getGroupIdByUuid(groupUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.groupAllowed(securedInfo, experimentId, groupId);

        GroupDto groupDto = groupService.toDto(groupService.getGroup(groupId), securedInfo);

        return new ResponseEntity<>(groupDto, HttpStatus.OK);
    }

    @PostMapping
    @InstructorOrHigher
    public ResponseEntity<GroupDto> postGroup(@PathVariable("experimentId") UUID experimentUuid,
                                                    @RequestBody GroupDto groupDto,
                                                    UriComponentsBuilder ucBuilder,
                                                    HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExperimentLockedException, IdInPostException, DataServiceException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentLocked(experimentId,true);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        GroupDto returnedDto = groupService.postGroup(groupDto, experimentId, securedInfo);
        log.debug("Created group ID: [{}] for experiment ID: [{}]", returnedDto.getGroupId(), experimentUuid);
        HttpHeaders headers = groupService.buildHeaders(ucBuilder, experimentUuid, returnedDto.getGroupId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

    @PostMapping("/create")
    @InstructorOrHigher
    public ResponseEntity<Void> createGroups(@PathVariable("experimentId") UUID experimentUuid, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExperimentLockedException, DataServiceException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentLocked(experimentId,true);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        groupService.createAndAssignGroupsToConditionsAndExposures(experimentId, securedInfo, false);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/{groupId}")
    @InstructorOrHigher
    public ResponseEntity<Void> updateGroup(@PathVariable("experimentId") UUID experimentUuid,
                                               @PathVariable("groupId") UUID groupUuid,
                                               @RequestBody GroupDto groupDto,
                                               HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, GroupNotMatchingException, TitleValidationException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        long groupId = groupService.getGroupIdByUuid(groupUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.groupAllowed(securedInfo, experimentId, groupId);

        groupService.updateGroup(groupId, groupDto);
        log.debug("Updated group ID: [{}]", groupUuid);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{groupId}")
    @InstructorOrHigher
    public ResponseEntity<Void> deleteGroup(@PathVariable("experimentId") UUID experimentUuid, @PathVariable("groupId") UUID groupUuid, HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, GroupNotMatchingException, ExperimentLockedException, NumberFormatException, TerracottaConnectorException {
        long experimentId = experimentService.getExperimentIdByUuid(experimentUuid);
        long groupId = groupService.getGroupIdByUuid(groupUuid);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentLocked(experimentId,true);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.groupAllowed(securedInfo, experimentId, groupId);

        try {
            groupService.deleteById(groupId);
        } catch (EmptyResultDataAccessException ex) {
            log.warn(ex.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
