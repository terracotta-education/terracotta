package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.dto.GroupDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.GroupService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = GroupController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class GroupController {

    private static final Logger log = LoggerFactory.getLogger(GroupController.class);
    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/groups";

    @Autowired
    private GroupService groupService;

    @Autowired
    private APIJWTService apijwtService;

    @GetMapping
    public ResponseEntity<List<GroupDto>> allGroupsByExperiment(@PathVariable long experimentId,
                                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        List<GroupDto> groupList = groupService.getGroups(experimentId);

        if(groupList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(groupList, HttpStatus.OK);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDto> getGroup(@PathVariable long experimentId,
                                                   @PathVariable long groupId,
                                                   HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, GroupNotMatchingException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.groupAllowed(securedInfo, experimentId, groupId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        GroupDto groupDto = groupService.toDto(groupService.getGroup(groupId));

        return new ResponseEntity<>(groupDto, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<GroupDto> postGroup(@PathVariable long experimentId,
                                                    @RequestBody GroupDto groupDto,
                                                    UriComponentsBuilder ucBuilder,
                                                    HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExperimentLockedException, IdInPostException, DataServiceException {

        log.debug("Creating Group : {}", groupDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentLocked(experimentId,true);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        GroupDto returnedDto = groupService.postGroup(groupDto, experimentId);
        HttpHeaders headers = groupService.buildHeaders(ucBuilder, experimentId, returnedDto.getGroupId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<Void> updateGroup(@PathVariable long experimentId,
                                               @PathVariable long groupId,
                                               @RequestBody GroupDto groupDto,
                                               HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, GroupNotMatchingException, TitleValidationException {

        log.debug("Updating group with id {}", groupId);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.groupAllowed(securedInfo, experimentId, groupId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        groupService.updateGroup(groupId, groupDto);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable long experimentId,
                                               @PathVariable long groupId,
                                               HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, GroupNotMatchingException, ExperimentLockedException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentLocked(experimentId,true);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.groupAllowed(securedInfo, experimentId, groupId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        try{
            groupService.deleteById(groupId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EmptyResultDataAccessException ex) {
            log.warn(ex.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}
