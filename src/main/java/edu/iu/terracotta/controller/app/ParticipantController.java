package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.dto.ParticipantDto;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.utils.TextConstants;
import org.apache.commons.lang3.EnumUtils;
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
import edu.iu.terracotta.model.app.enumerator.Source;

import javax.servlet.http.HttpServletRequest;
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


    /**
     * To show the experiment in a course (context) in a platform deployment.
     */
    @RequestMapping(value = "/{experimentId}/participants", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<ParticipantDto>> allParticipantsByExperiment(@PathVariable("experimentId") long experimentId,
                                                                            HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securityInfo, experimentId);

        if (apijwtService.isLearnerOrHigher(securityInfo)) {
            List<Participant> participantList =
                    participantService.findAllByExperimentId(experimentId);
            if (participantList.isEmpty()) {
                return new ResponseEntity(HttpStatus.NO_CONTENT);
                // You many decide to return HttpStatus.NOT_FOUND
            }
            List<ParticipantDto> participantDtos = new ArrayList<>();
            for (Participant participant : participantList) {
                participantDtos.add(participantService.toDto(participant));
            }
            return new ResponseEntity<>(participantDtos, HttpStatus.OK);
        } else {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * To show the an specific experiment.
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
            headers.setLocation(ucBuilder.path("/api/experiment/{experimentId}/participant/{participanId}").buildAndExpand(experimentId, participantSaved.getParticipantId()).toUri());
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

            if (!participantSearchResult.isPresent()) {
                log.error("Unable to update. Participant with id {} not found.", participantId);
                return new ResponseEntity("Unable to update. Participant with id " + participantId + TextConstants.NOT_FOUND_SUFFIX,
                        HttpStatus.NOT_FOUND);
            }
            Participant participantToChange = participantSearchResult.get();
            participantToChange.setConsent((participantDto.getConsent()));
            participantToChange.setDateGiven(participantDto.getDateGiven());
            participantToChange.setDateRevoked(participantDto.getDateRevoked());
            if(participantDto.getSource() != null) {
                participantToChange.setSource(
                        EnumUtils.getEnum(Source.class, participantDto.getSource()));
            }
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
