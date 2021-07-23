package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentLockedException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.Group;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = GroupController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class GroupController {

    static final Logger log = LoggerFactory.getLogger(GroupController.class);
    static final String REQUEST_ROOT = "api/experiments";

    @Autowired
    GroupService groupService;

    @Autowired
    APIJWTService apijwtService;



    @RequestMapping(value = "/{experiment_id}/groups", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<List<GroupDto>> allGroupsByExperiment(@PathVariable("experiment_id") Long experimentId,
                                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            List<GroupDto> groupList = groupService.getGroups(experimentId);
            if(groupList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(groupList, HttpStatus.OK);
        }else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/groups/{group_id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<GroupDto> getGroup(@PathVariable("experiment_id") long experimentId,
                                                   @PathVariable("group_id") long groupId,
                                                   HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, GroupNotMatchingException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.groupAllowed(securedInfo, experimentId, groupId);

        if(apijwtService.isLearnerOrHigher(securedInfo)) {
            GroupDto groupDto = groupService.toDto(groupService.getGroup(groupId));
            return new ResponseEntity<>(groupDto, HttpStatus.OK);
        }else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/groups", method = RequestMethod.POST)
    public ResponseEntity<GroupDto> postGroup(@PathVariable("experiment_id") Long experimentId,
                                                    @RequestBody GroupDto groupDto,
                                                    UriComponentsBuilder ucBuilder,
                                                    HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ExperimentLockedException {

        log.info("Creating Group : {}", groupDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentLocked(experimentId,true);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            if(groupDto.getGroupId() != null) {
                log.error(TextConstants.ID_IN_POST_ERROR);
                return new ResponseEntity(TextConstants.ID_IN_POST_ERROR, HttpStatus.CONFLICT);
            }

            groupDto.setExperimentId(experimentId);
            Group group;
            try{
                group = groupService.fromDto(groupDto);
            } catch (DataServiceException e) {
                return new ResponseEntity("Error 105: Unable to create group:" + e.getMessage(), HttpStatus.BAD_REQUEST);
            }
            GroupDto returnedDto = groupService.toDto(groupService.save(group));

            HttpHeaders headers = groupService.buildHeaders(ucBuilder, experimentId, group.getGroupId());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
        }else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/groups/{group_id}", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateGroup(@PathVariable("experiment_id") Long experimentId,
                                               @PathVariable("group_id") Long groupId,
                                               @RequestBody GroupDto groupDto,
                                               HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, GroupNotMatchingException, TitleValidationException {

        log.info("Updating group with id {}", groupId);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.groupAllowed(securedInfo, experimentId, groupId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            groupService.updateGroup(groupId, groupDto);
            return new ResponseEntity<>(HttpStatus.OK);
        }else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/groups/{group_id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteGroup(@PathVariable("experiment_id") Long experimentId,
                                               @PathVariable("group_id") Long groupId,
                                               HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, GroupNotMatchingException, ExperimentLockedException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentLocked(experimentId,true);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.groupAllowed(securedInfo, experimentId, groupId);

        if(apijwtService.isInstructorOrHigher(securedInfo)) {
            try{
                groupService.deleteById(groupId);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (EmptyResultDataAccessException ex) {
                log.error(ex.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        }else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }
}