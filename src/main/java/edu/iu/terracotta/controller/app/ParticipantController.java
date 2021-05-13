package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.dto.ExperimentDto;
import edu.iu.terracotta.model.app.dto.ParticipantDto;
import edu.iu.terracotta.model.app.enumerator.DistributionTypes;
import edu.iu.terracotta.model.app.enumerator.ExposureTypes;
import edu.iu.terracotta.model.app.enumerator.ParticipationTypes;
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
    @RequestMapping(value = "/{experimentId}/participants/", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<List<ParticipantDto>> allParticipantsByExperiment(@PathVariable("experimentId") long experimentId, HttpServletRequest req) {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        if (securityInfo==null){
            log.error(TextConstants.BAD_TOKEN);
            return new ResponseEntity(TextConstants.BAD_TOKEN, HttpStatus.UNAUTHORIZED);
        }
        if (!experimentService.experimentBelongsToDeploymentAndCourse(experimentId, securityInfo.getPlatformDeploymentId(), securityInfo.getContextId())){
            return new ResponseEntity(TextConstants.EXPERIMENT_NOT_MATCHING , HttpStatus.UNAUTHORIZED);
        }

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
    @RequestMapping(value = "/{experimentId}/participants/{id}", method = RequestMethod.GET, produces = "application/json;")
    @ResponseBody
    public ResponseEntity<ParticipantDto> getExperiment(@PathVariable("experimentId") long experimentId, @PathVariable("id") long id, HttpServletRequest req) {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        if (securityInfo==null){
            log.error(TextConstants.BAD_TOKEN);
            return new ResponseEntity(TextConstants.BAD_TOKEN, HttpStatus.UNAUTHORIZED);
        }

        if (!experimentService.experimentBelongsToDeploymentAndCourse(experimentId, securityInfo.getPlatformDeploymentId(), securityInfo.getContextId())){
            return new ResponseEntity(TextConstants.EXPERIMENT_NOT_MATCHING , HttpStatus.UNAUTHORIZED);
        }

        if (apijwtService.isLearnerOrHigher(securityInfo)) {

            Optional<Participant> participantSearchResult = participantService.findById(id);

            if (!participantSearchResult.isPresent()) {
                log.error("participant in platform {} and context {} and experiment {} with id {} not found.", securityInfo.getPlatformDeploymentId(), securityInfo.getContextId(), experimentId, id);

                return new ResponseEntity("participant in platform " + securityInfo.getPlatformDeploymentId()
                        + " and context " + securityInfo.getContextId() + " experiment with id " + experimentId + " with id " + id
                        + TextConstants.NOT_FOUND_SUFFIX, HttpStatus.NOT_FOUND);
            } else {
                ParticipantDto participantDto = participantService.toDto(participantSearchResult.get());
                return new ResponseEntity<>(participantDto, HttpStatus.OK);
            }
        } else {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experimentId}/participants/", method = RequestMethod.POST)
    public ResponseEntity<ParticipantDto> postExperiment(@PathVariable("experimentId") long experimentId, @RequestBody ParticipantDto participantDto, UriComponentsBuilder ucBuilder, HttpServletRequest req) {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        if (securityInfo==null){
            log.error(TextConstants.BAD_TOKEN);
            return new ResponseEntity(TextConstants.BAD_TOKEN, HttpStatus.UNAUTHORIZED);
        }

        if (!experimentService.experimentBelongsToDeploymentAndCourse(experimentId, securityInfo.getPlatformDeploymentId(), securityInfo.getContextId())){
            return new ResponseEntity(TextConstants.EXPERIMENT_NOT_MATCHING , HttpStatus.UNAUTHORIZED);
        }

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


    @RequestMapping(value = "/{experimentId}/participants/{id}", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateParticipant(@PathVariable("experimentId") long experimentId, @PathVariable("id") Long id, @RequestBody ParticipantDto participantDto, HttpServletRequest req) {
        log.info("Updating Participant with id {}", id);
        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        if (securityInfo==null){
            log.error(TextConstants.BAD_TOKEN);
            return new ResponseEntity(TextConstants.BAD_TOKEN, HttpStatus.UNAUTHORIZED);
        }

        if (!experimentService.experimentBelongsToDeploymentAndCourse(experimentId, securityInfo.getPlatformDeploymentId(), securityInfo.getContextId())){
            return new ResponseEntity(TextConstants.EXPERIMENT_NOT_MATCHING , HttpStatus.UNAUTHORIZED);
        }

        if (apijwtService.isLearnerOrHigher(securityInfo)) {

            Optional<Participant> participantSearchResult = participantService.findById(id);

            if (!participantSearchResult.isPresent()) {
                log.error("Unable to update. PArticipant with id {} not found.", id);
                return new ResponseEntity("Unable to update. Participant with id " + id + TextConstants.NOT_FOUND_SUFFIX,
                        HttpStatus.NOT_FOUND);
            }
            Participant participantToChange = participantSearchResult.get();
            //TODO, at this moment there is nothing to edit in a participant, but... later we will add the consent, but maybe it will be changed in its own controller
            participantService.saveAndFlush(participantToChange);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/{experimentId}/participants/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteParticipant(@PathVariable("experimentId") long experimentId, @PathVariable("id") Long id, HttpServletRequest req) {

        SecurityInfo securityInfo = apijwtService.extractValues(req,false);
        if (securityInfo==null){
            log.error(TextConstants.BAD_TOKEN);
            return new ResponseEntity(TextConstants.BAD_TOKEN, HttpStatus.UNAUTHORIZED);
        }

        if (!experimentService.experimentBelongsToDeploymentAndCourse(experimentId, securityInfo.getPlatformDeploymentId(), securityInfo.getContextId())){
            return new ResponseEntity(TextConstants.EXPERIMENT_NOT_MATCHING , HttpStatus.UNAUTHORIZED);
        }

        if (apijwtService.isLearnerOrHigher(securityInfo)) {
            try {
                participantService.deleteById(id);
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
