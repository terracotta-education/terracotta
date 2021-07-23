package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.dto.ParticipantDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.utils.TextConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@SuppressWarnings({"rawtypes", "unchecked"})
@RequestMapping(value = ParticipantController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ParticipantController {

    static final Logger log = LoggerFactory.getLogger(ParticipantController.class);
    static final String REQUEST_ROOT = "api/experiments";

    @Autowired
    ParticipantService participantService;

    @Autowired
    APIJWTService apijwtService;



    @RequestMapping(value = "/{experimentId}/participants", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<ParticipantDto>> allParticipantsByExperiment(@PathVariable("experimentId") long experimentId,
                                                                            @RequestParam(name = "refresh", defaultValue = "true") boolean refresh,
                                                                            HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ParticipantNotUpdatedException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            List<Participant> currentParticipantList = participantService.findAllByExperimentId(experimentId);
            if (apijwtService.isInstructorOrHigher(securedInfo) && refresh) {
                currentParticipantList = participantService.refreshParticipants(experimentId, securedInfo, currentParticipantList);
            }
            if (currentParticipantList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(participantService.getParticipants(currentParticipantList), HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }



    @RequestMapping(value = "/{experimentId}/participants/{participant_id}", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<ParticipantDto> getParticipant(@PathVariable("experimentId") long experimentId,
                                                        @PathVariable("participant_id") long participantId,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ParticipantNotMatchingException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.participantAllowed(securedInfo, experimentId, participantId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            ParticipantDto participantDto = participantService.toDto(participantService.getParticipant(participantId));
            return new ResponseEntity<>(participantDto, HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experimentId}/participants", method = RequestMethod.POST)
    public ResponseEntity<ParticipantDto> postParticipant(@PathVariable("experimentId") long experimentId,
                                                         @RequestBody ParticipantDto participantDto,
                                                         UriComponentsBuilder ucBuilder,
                                                         HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {

        log.info("Creating Participant : {}", participantDto);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            if (participantDto.getParticipantId() != null) {
                log.error(TextConstants.ID_IN_POST_ERROR);
                return new ResponseEntity(TextConstants.ID_IN_POST_ERROR, HttpStatus.CONFLICT);
            }

            Participant participant;
            participantDto.setExperimentId(experimentId);
            try {
                participant = participantService.fromDto(participantDto);
            } catch (DataServiceException e) {
                return new ResponseEntity("Error 105: Unable to create the participant:" + e.getMessage(), HttpStatus.BAD_REQUEST);
            }
            ParticipantDto returnedDto = participantService.toDto(participantService.save(participant));

            HttpHeaders headers = participantService.buildHeaders(ucBuilder, experimentId, returnedDto.getParticipantId());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experimentId}/participants/{participant_id}", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateParticipant(@PathVariable("experimentId") long experimentId,
                                                  @PathVariable("participant_id") Long participantId,
                                                  @RequestBody ParticipantDto participantDto,
                                                  HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ParticipantNotMatchingException, DataServiceException {
        log.info("Updating Participant with id {}", participantId);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.participantAllowed(securedInfo, experimentId, participantId);

        if (apijwtService.isLearnerOrHigher(securedInfo)) {
            Map<Participant, ParticipantDto> map = new HashMap<>();
            map.put(participantService.getParticipant(participantId), participantDto);
            participantService.changeParticipant(map, experimentId);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experimentId}/participants", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateParticipants(@PathVariable("experimentId") long experimentId,
                                                   @RequestBody List<ParticipantDto> participantDtoList,
                                                   HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ParticipantNotMatchingException, DataServiceException {

        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if(apijwtService.isInstructorOrHigher(securedInfo)){
            Map<Participant, ParticipantDto> participantMap = new HashMap<>();
            for(ParticipantDto participantDto : participantDtoList) {
                apijwtService.participantAllowed(securedInfo, experimentId, participantDto.getParticipantId());
                Participant participant = participantService.getParticipant(participantDto.getParticipantId());
                participantMap.put(participant, participantDto);

            }
            try{
                participantService.changeParticipant(participantMap, experimentId);
                return new ResponseEntity<>(HttpStatus.OK);
            }catch (Exception ex) {
                throw new DataServiceException("Error 105: There was an error updating the participant list. No participants were updated.");
            }
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }



    @RequestMapping(value = "/{experimentId}/participants/{participant_id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteParticipant(@PathVariable("experimentId") long experimentId,
                                                  @PathVariable("participant_id") Long participantId,
                                                  HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ParticipantNotMatchingException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.participantAllowed(securedInfo, experimentId, participantId);

        if (apijwtService.isInstructorOrHigher(securedInfo)) {
            Participant participant = participantService.getParticipant(participantId);
            //soft delete
            participant.setDropped(true);
            participantService.saveAndFlush(participant);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }
    }
}
