package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.model.app.Group;
import edu.iu.terracotta.model.app.dto.GroupDto;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.GroupService;
import edu.iu.terracotta.utils.TextConstants;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping(value = GroupController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class GroupController {

    static final Logger log = LoggerFactory.getLogger(GroupController.class);
    static final String REQUEST_ROOT = "api/experiments";

    @Autowired
    GroupService groupService;

    @Autowired
    APIJWTService apijwtService;

    @Autowired
    ExperimentService experimentService;

    @RequestMapping(value = "/{experiment_id}/groups", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<List<GroupDto>> allGroupsByExperiment(@PathVariable("experiment_id") Long experimentId,
                                                                      HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {
        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);

        //TODO should this be Learner or higher? Is there a reason a student would need to see group?
        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            List<Group> groupList =
                    groupService.findAllByExperimentId(experimentId);
            if(groupList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            List<GroupDto> groupDtoList = new ArrayList<>();
            for(Group group : groupList) {
                groupDtoList.add(groupService.toDto(group));
            }
            return new ResponseEntity<>(groupDtoList, HttpStatus.OK);
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

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.groupAllowed(securityInfo, experimentId, groupId);

        if(apijwtService.isLearnerOrHigher(securityInfo)) {
            Optional<Group> group = groupService.findOneByGroupId(groupId);

            if(!group.isPresent()) {
                log.error("group {} in experiment {} not found.", groupId, experimentId);
                return new ResponseEntity("group " + groupId + " in experiment " + experimentId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            } else {
                GroupDto groupDto = groupService.toDto(group.get());
                return new ResponseEntity<>(groupDto, HttpStatus.OK);
            }
        }else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/groups", method = RequestMethod.POST)
    public ResponseEntity<GroupDto> postGroup(@PathVariable("experiment_id") Long experimentId,
                                                    @RequestBody GroupDto groupDto,
                                                    UriComponentsBuilder ucBuilder,
                                                    HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {

        log.info("Creating Group : {}", groupDto);
        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
            if(groupDto.getGroupId() != null) {
                log.error("Cannot include id in the POST endpoint. To modify existing groups you must use PUT");
                return new ResponseEntity("Cannot include id in the POST endpoint. To modify existing groups you must use PUT", HttpStatus.CONFLICT);
            }

            if(!StringUtils.isAllBlank(groupDto.getName()) && groupDto.getName().length() > 255){
                return new ResponseEntity("Title must be 255 characters or less.", HttpStatus.BAD_REQUEST);
            }

            groupDto.setExperimentId(experimentId);
            Group group;
            try{
                group = groupService.fromDto(groupDto);
            } catch (DataServiceException e) {
                return new ResponseEntity("Unable to create group:" + e.getMessage(), HttpStatus.BAD_REQUEST);
            }

            Group groupSaved = groupService.save(group);
            GroupDto returnedDto = groupService.toDto(groupSaved);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(ucBuilder.path("/api/experiment/{experiment_id}/groups/{id}").buildAndExpand(group.getExperiment().getExperimentId(), group.getGroupId()).toUri());
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
            throws ExperimentNotMatchingException, BadTokenException, GroupNotMatchingException {

        log.info("Updating group with id {}", groupId);
        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.groupAllowed(securityInfo, experimentId, groupId);

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
            Optional<Group> groupSearchResult = groupService.findById(groupId);

            if(!groupSearchResult.isPresent()) {
                log.error("Unable to update. Group with id {} not found.", groupId);
                return new ResponseEntity("Unable to update. Group with id  " + groupId + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            }

            if(StringUtils.isAllBlank(groupDto.getName()) && StringUtils.isAllBlank(groupSearchResult.get().getName())){
                return new ResponseEntity("Please give the group a name.", HttpStatus.CONFLICT);
            }
            if(!StringUtils.isAllBlank(groupDto.getName()) && groupDto.getName().length() > 255){
                return new ResponseEntity("The title must be 255 characters or less.", HttpStatus.BAD_REQUEST);
            }
            Group groupToChange = groupSearchResult.get();
            groupToChange.setName(groupDto.getName());

            groupService.saveAndFlush(groupToChange);
            return new ResponseEntity<>(HttpStatus.OK);
        }else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experiment_id}/groups/{group_id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteGroup(@PathVariable("experiment_id") Long experimentId,
                                               @PathVariable("group_id") Long groupId,
                                               HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, GroupNotMatchingException {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.groupAllowed(securityInfo, experimentId, groupId);

        if(apijwtService.isInstructorOrHigher(securityInfo)) {
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
