package edu.iu.terracotta.controller.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.model.dto.ParticipantDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.ParticipantAlreadyStartedException;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping(value = ParticipantController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
@SuppressWarnings({"rawtypes", "unchecked", "PMD.GuardLogStatement", "PMD.PreserveStackTrace", "squid:S112"})
public class ParticipantController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/participants";

    @Autowired private ParticipantService participantService;
    @Autowired private ApiJwtService apijwtService;

    @GetMapping
    public ResponseEntity<List<ParticipantDto>> allParticipantsByExperiment(@PathVariable long experimentId,
                                                                            @RequestParam(name = "refresh", defaultValue = "true") boolean refresh,
                                                                            HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ParticipantNotUpdatedException, NumberFormatException, TerracottaConnectorException {

        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        List<Participant> currentParticipantList = participantService.findAllByExperimentId(experimentId);

        if (apijwtService.isInstructorOrHigher(securedInfo) && refresh) {
            currentParticipantList = participantService.refreshParticipants(experimentId, currentParticipantList);
        }

        if (currentParticipantList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(participantService.getParticipants(currentParticipantList, experimentId, securedInfo.getUserId(), !apijwtService.isInstructorOrHigher(securedInfo), securedInfo), HttpStatus.OK);
    }

    @GetMapping("/{participantId}")
    public ResponseEntity<ParticipantDto> getParticipant(@PathVariable long experimentId,
                                                        @PathVariable long participantId,
                                                        HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ParticipantNotMatchingException, InvalidUserException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.participantAllowed(securedInfo, experimentId, participantId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        ParticipantDto participantDto = participantService.toDto(
            participantService.getParticipant(participantId, experimentId, securedInfo.getUserId(),
            !apijwtService.isInstructorOrHigher(securedInfo)),
            securedInfo
        );

        return new ResponseEntity<>(participantDto, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ParticipantDto> postParticipant(@PathVariable long experimentId,
                                                         @RequestBody ParticipantDto participantDto,
                                                         UriComponentsBuilder ucBuilder,
                                                         HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, IdInPostException, DataServiceException, NumberFormatException, TerracottaConnectorException {
        log.debug("Creating Participant for experiment ID: {}", experimentId);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isLearnerOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        ParticipantDto returnedDto = participantService.postParticipant(participantDto, experimentId, securedInfo);
        HttpHeaders headers = participantService.buildHeaders(ucBuilder, experimentId, returnedDto.getParticipantId());

        return new ResponseEntity<>(returnedDto, headers, HttpStatus.CREATED);
    }

    @PutMapping("/{participantId}")
    public ResponseEntity<ParticipantDto> updateParticipant(@PathVariable long experimentId,
                                                  @PathVariable long participantId,
                                                  @RequestBody ParticipantDto participantDto,
                                                  HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ParticipantNotMatchingException, DataServiceException, InvalidUserException, NumberFormatException, TerracottaConnectorException {
        log.debug("Updating Participant with id {}", participantId);
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.participantAllowed(securedInfo, experimentId, participantId);
        List<Participant> participants = new ArrayList<>();

        if (apijwtService.isInstructorOrHigher(securedInfo)) {
            participants = participantService.changeParticipant(
                Collections.singletonMap(
                    participantService.getParticipant(participantId, experimentId, securedInfo.getUserId(), false),
                    participantDto),
                experimentId,
                securedInfo
            );

            return new ResponseEntity<>(participantService.toDto(participants.get(0), securedInfo), HttpStatus.OK);
        }

        if (apijwtService.isLearner(securedInfo)) {
            // Regardless of whether consent is changed or not, record a grade of  1.0 point for the consent assignment
            Participant participant = participantService.getParticipant(participantId, experimentId, securedInfo.getUserId(), true);

            try {
                participantService.postConsentSubmission(participant, securedInfo);
            } catch (ConnectionException e) {
                throw new RuntimeException("Failed to post grade to the LMS for consent submission", e);
            }

            try {
                return new ResponseEntity<>(participantService.toDto(participantService.changeConsent(participantDto, securedInfo, experimentId), securedInfo), HttpStatus.OK);
            } catch (ParticipantAlreadyStartedException e) {
                log.debug("Participant {} has already started: {}", participantId, e.getMessage());
                return new ResponseEntity(
                    "Error 149: Grade for consent assignment has been posted, but consent " +
                    "to participate in the study cannot be updated since student has already accessed an assignment.",
                    HttpStatus.UNAUTHORIZED
                );
            }
        }

        return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
    }

    @PutMapping
    public ResponseEntity<Void> updateParticipants(@PathVariable long experimentId,
                                                   @RequestBody List<ParticipantDto> participantDtoList,
                                                   HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ParticipantNotMatchingException, DataServiceException, InvalidUserException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req, false);
        apijwtService.experimentAllowed(securedInfo, experimentId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        Map<Participant, ParticipantDto> participantMap = new HashMap<>();

        for (ParticipantDto participantDto : participantDtoList) {
            apijwtService.participantAllowed(securedInfo, experimentId, participantDto.getParticipantId());
            Participant participant = participantService.getParticipant(participantDto.getParticipantId(), experimentId, securedInfo.getUserId(), false);
            log.debug("Updating participant with id: {}", participant.getParticipantId());
            participantMap.put(participant, participantDto);
        }

        try {
            participantService.changeParticipant(participantMap, experimentId, securedInfo);

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception ex) {
            throw new DataServiceException("Error 105: There was an error updating the participant list. No participants were updated.");
        }
    }

    @DeleteMapping("/{participantId}")
    public ResponseEntity<Void> deleteParticipant(@PathVariable long experimentId,
                                                  @PathVariable Long participantId,
                                                  HttpServletRequest req)
            throws ExperimentNotMatchingException, BadTokenException, ParticipantNotMatchingException, InvalidUserException, NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apijwtService.extractValues(req,false);
        apijwtService.experimentAllowed(securedInfo, experimentId);
        apijwtService.participantAllowed(securedInfo, experimentId, participantId);

        if (!apijwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity(TextConstants.NOT_ENOUGH_PERMISSIONS, HttpStatus.UNAUTHORIZED);
        }

        Participant participant = participantService.getParticipant(participantId, experimentId, securedInfo.getUserId(), false);
        // soft delete
        participant.setDropped(true);
        participantService.saveAndFlush(participant);

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
