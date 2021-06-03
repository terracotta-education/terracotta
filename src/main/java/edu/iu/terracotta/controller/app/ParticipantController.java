package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.dto.ParticipantDto;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.GroupService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.service.lti.AdvantageMembershipService;
import edu.iu.terracotta.service.lti.LTIDataService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping(value = ParticipantController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class ParticipantController {

    static final Logger log = LoggerFactory.getLogger(ParticipantController.class);
    static final String REQUEST_ROOT = "api/experiments";

    @Autowired
    ExperimentService experimentService;

    @Autowired
    ParticipantService participantService;

    @Autowired
    APIJWTService apijwtService;

    @Autowired
    AdvantageMembershipService advantageMembershipService;

    @Autowired
    LTIDataService ltiDataService;

    @Autowired
    GroupService groupService;


    /**
     * To show the participants in an experiment.
     */
    @RequestMapping(value = "/{experimentId}/participants", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<ParticipantDto>> allParticipantsByExperiment(@PathVariable("experimentId") long experimentId,
                                                                            @RequestParam(name = "refresh", defaultValue = "true") boolean refresh,
                                                                            HttpServletRequest req) throws ExperimentNotMatchingException, BadTokenException, ParticipantNotUpdatedException {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);

        if (apijwtService.isLearnerOrHigher(securityInfo)) {

            List<Participant> currentParticipantList =
                    participantService.findAllByExperimentId(experimentId);
            if (apijwtService.isInstructorOrHigher(securityInfo) && refresh) {
                currentParticipantList = participantService.refreshParticipants(experimentId, securityInfo, currentParticipantList);
            }
            if (currentParticipantList.isEmpty()) {
                return new ResponseEntity(HttpStatus.NO_CONTENT);
                // You many decide to return HttpStatus.NOT_FOUND
            }
            List<ParticipantDto> participantDtos = new ArrayList<>();
            for (Participant participant : currentParticipantList) {
                participantDtos.add(participantService.toDto(participant));
            }
            return new ResponseEntity<>(participantDtos, HttpStatus.OK);
        } else {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * To show the an specific participant.
     */
    @RequestMapping(value = "/{experimentId}/participants/{participant_id}", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<ParticipantDto> getExperiment(@PathVariable("experimentId") long experimentId,
                                                        @PathVariable("participant_id") long participantId,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ParticipantNotMatchingException {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.participantAllowed(securityInfo, experimentId, participantId);

        if (apijwtService.isLearnerOrHigher(securityInfo)) {

            Optional<Participant> participantSearchResult = participantService.findById(participantId);

            if (!participantSearchResult.isPresent()) {
                log.error("participant in platform {} and context {} and experiment {} with id {} not found.",
                        securityInfo.getPlatformDeploymentId(), securityInfo.getContextId(), experimentId, participantId);

                return new ResponseEntity("participant in platform " + securityInfo.getPlatformDeploymentId()
                        + " and context " + securityInfo.getContextId() + " experiment with id " + experimentId + " with id " + participantId
                        + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            } else {
                ParticipantDto participantDto = participantService.toDto(participantSearchResult.get());
                return new ResponseEntity<>(participantDto, HttpStatus.OK);
            }
        } else {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experimentId}/participants", method = RequestMethod.POST)
    public ResponseEntity<ParticipantDto> postExperiment(@PathVariable("experimentId") long experimentId,
                                                         @RequestBody ParticipantDto participantDto,
                                                         UriComponentsBuilder ucBuilder,
                                                         HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);

        if (apijwtService.isLearnerOrHigher(securityInfo)) {

            log.info("Creating Participant : {}", participantDto);
            //We check that it does not exist
            if (participantDto.getExperimentId() != null) {
                log.error(TextConstants.ID_IN_POST_ERROR);
                return new ResponseEntity(
                        TextConstants.ID_IN_POST_ERROR,
                        HttpStatus.CONFLICT);
            }

            Participant participant = null;
            participantDto.setExperimentId(experimentId);
            try {
                participant = participantService.fromDto(participantDto);
            } catch (DataServiceException e) {
                return new ResponseEntity("Unable to create the participant:" + e.getMessage(), HttpStatus.BAD_REQUEST);
            }

            Participant participantSaved = participantService.save(participant);
            ParticipantDto returnedDto = participantService.toDto(participantSaved);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(ucBuilder.path("/api/experiments/{experimentId}/participant/{participantId}").buildAndExpand(experimentId, participantSaved.getParticipantId()).toUri());
            return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
        } else {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
    }


    @RequestMapping(value = "/{experimentId}/participants/{participant_id}", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateParticipant(@PathVariable("experimentId") long experimentId,
                                                  @PathVariable("participant_id") Long participantId,
                                                  @RequestBody ParticipantDto participantDto,
                                                  HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ParticipantNotMatchingException {
        log.info("Updating Participant with id {}", participantId);
        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.participantAllowed(securityInfo, experimentId, participantId);

        if (apijwtService.isLearnerOrHigher(securityInfo)) {

            Optional<Participant> participantSearchResult = participantService.findById(participantId);
            Optional<Experiment> experiment = experimentService.findById(experimentId);
            if (!participantSearchResult.isPresent()) {
                log.error("Unable to update. Participant with id {} not found.", participantId);
                return new ResponseEntity("Unable to update. Participant with id " + participantId + TextConstants.NOT_FOUND_SUFFIX,
                        HttpStatus.NOT_FOUND);
            }
            Participant participantToChange = participantSearchResult.get();
            //If they had no consent, and now they have, we change the dategiven to now.
            //In any other case, we leave the date as it was. Ignoring any value in the PUT
            if (!participantToChange.getConsent() && participantDto.getConsent()) {
                participantToChange.setDateGiven(Timestamp.valueOf(LocalDateTime.now()));
                participantToChange.setDateRevoked(null);
            }
            //If they had consent, and now they don't have, we change the dateRevoked to now.
            //In any other case, we leave the date as it is. Ignoring any value in the PUT
            if (participantToChange.getConsent() && !participantDto.getConsent()) {
                participantToChange.setDateRevoked(Timestamp.valueOf(LocalDateTime.now()));
            }
            participantToChange.setConsent((participantDto.getConsent()));
            //NOTE: we do this... but this will be updated in the next GET participants with the real data and dropped will be overwritten.
            if (participantDto.getDropped()!=null) {
                participantToChange.setDropped(participantDto.getDropped());
            }
            if (participantDto.getGroupId()!=null && groupService.existsByExperiment_ExperimentIdAndGroupId(experiment.get().getExperimentId(), participantDto.getGroupId())){
                participantToChange.setGroup(groupService.findById(participantDto.getGroupId()).get());
            }
            //This will never happen, but is here to avoid complains from the code sniffers.
            if (!experiment.isPresent()) {
                log.error("Unable to update. Experiment with id {} not found.", experimentId);
                return new ResponseEntity("Unable to update. Experiment with id " + experimentId + TextConstants.NOT_FOUND_SUFFIX,
                        HttpStatus.NOT_FOUND);
            }
            participantToChange.setSource(experiment.get().getParticipationType());

            participantService.saveAndFlush(participantToChange);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experimentId}/participants/{participant_id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteParticipant(@PathVariable("experimentId") long experimentId,
                                                  @PathVariable("participant_id") Long participantId,
                                                  HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ParticipantNotMatchingException {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);
        apijwtService.participantAllowed(securityInfo, experimentId, participantId);

        if (apijwtService.isLearnerOrHigher(securityInfo)) {
            try {
                participantService.deleteById(participantId);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (EmptyResultDataAccessException ex) {
                log.error(ex.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
    }
}
