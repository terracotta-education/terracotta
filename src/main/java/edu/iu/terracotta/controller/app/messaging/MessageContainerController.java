package edu.iu.terracotta.controller.app.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.dao.model.dto.messaging.container.MessageContainerDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerNotFoundException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerOwnerNotMatchingException;
import edu.iu.terracotta.service.app.messaging.MessageContainerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@SuppressWarnings({"PMD.GuardLogStatement"})
@RequestMapping(value = MessageContainerController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class MessageContainerController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/exposures/{exposureId}/messaging/container";

    @Autowired private ApiJwtService apiJwtService;
    @Autowired private MessageContainerService messageContainerService;

    @GetMapping
    public ResponseEntity<List<MessageContainerDto>> getAll(@PathVariable long experimentId, @PathVariable long exposureId, HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apiJwtService.extractValues(req, false);

        if (!apiJwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            apiJwtService.experimentAllowed(securedInfo, experimentId);
            apiJwtService.exposureAllowed(securedInfo, experimentId, exposureId);
        } catch (BadTokenException | ExperimentNotMatchingException | ExposureNotMatchingException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(messageContainerService.getAll(experimentId, exposureId, securedInfo), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<MessageContainerDto> post(@PathVariable long experimentId, @PathVariable long exposureId, @RequestParam(name = "single", defaultValue = "false") boolean single, HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apiJwtService.extractValues(req, false);

        if (!apiJwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Exposure exposure = null;

        try {
            apiJwtService.experimentAllowed(securedInfo, experimentId);
            exposure = apiJwtService.exposureAllowed(securedInfo, experimentId, exposureId);
        } catch (BadTokenException | ExperimentNotMatchingException | ExposureNotMatchingException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(messageContainerService.create(exposure, single, securedInfo), HttpStatus.OK);
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<MessageContainerDto> put(@PathVariable long experimentId, @PathVariable long exposureId, @PathVariable UUID uuid, @RequestBody MessageContainerDto messageContainerDto, HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apiJwtService.extractValues(req, false);

        if (!apiJwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        MessageContainer messageContainer;

        try {
            messageContainer = apiJwtService.messagingContainerAllowed(securedInfo, exposureId, uuid);
            apiJwtService.experimentAllowed(securedInfo, experimentId);
            apiJwtService.exposureAllowed(securedInfo, experimentId, exposureId);

            if (exposureId != messageContainerDto.getExposureId()) {
                move(experimentId, exposureId, uuid, messageContainerDto, req);
                messageContainer = apiJwtService.messagingContainerAllowed(securedInfo, exposureId, uuid);
            }
        } catch (BadTokenException | ExperimentNotMatchingException | MessageContainerOwnerNotMatchingException | MessageContainerNotMatchingException | ExposureNotMatchingException | MessageContainerNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            return new ResponseEntity<>(messageContainerService.update(messageContainerDto, messageContainer), HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping
    public ResponseEntity<List<MessageContainerDto>> putAll(@PathVariable long experimentId, @PathVariable long exposureId, @RequestBody List<MessageContainerDto> messageContainerDtos, HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apiJwtService.extractValues(req, false);

        if (!apiJwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        List<MessageContainer> messageContainers = new ArrayList<>();

        try {
            for (MessageContainerDto messageContainerDto : messageContainerDtos) {
                apiJwtService.messagingContainerAllowed(securedInfo, exposureId, messageContainerDto.getId());
                messageContainers.add(apiJwtService.messagingContainerAllowed(securedInfo, exposureId, messageContainerDto.getId()));
            }

            apiJwtService.experimentAllowed(securedInfo, experimentId);
            apiJwtService.exposureAllowed(securedInfo, experimentId, exposureId);
        } catch (BadTokenException | ExperimentNotMatchingException | MessageContainerOwnerNotMatchingException | MessageContainerNotMatchingException | ExposureNotMatchingException | MessageContainerNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            return new ResponseEntity<>(messageContainerService.updateAll(messageContainerDtos, messageContainers), HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<MessageContainerDto> delete(@PathVariable long experimentId, @PathVariable long exposureId, @PathVariable UUID uuid, HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apiJwtService.extractValues(req, false);

        if (!apiJwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        MessageContainer messageContainer;

        try {
            messageContainer = apiJwtService.messagingContainerAllowed(securedInfo, exposureId, uuid);
            apiJwtService.experimentAllowed(securedInfo, experimentId);
            apiJwtService.exposureAllowed(securedInfo, experimentId, exposureId);
        } catch (BadTokenException | ExperimentNotMatchingException | MessageContainerOwnerNotMatchingException | MessageContainerNotMatchingException | ExposureNotMatchingException | MessageContainerNotFoundException e) {
            log.error("Exception occurred deleting message with UUID: [{}] for user lmsUserId: [{}]", uuid, securedInfo.getLmsUserId());
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            return new ResponseEntity<>(messageContainerService.delete(messageContainer), HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.OK);
        }
    }

    @PostMapping("/{uuid}/move")
    public ResponseEntity<MessageContainerDto> move(@PathVariable long experimentId, @PathVariable long exposureId, @PathVariable UUID uuid, @RequestBody MessageContainerDto messageContainerDto, HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apiJwtService.extractValues(req, false);

        if (!apiJwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Exposure newExposure;
        MessageContainer messageContainer;

        try {
            messageContainer = apiJwtService.messagingContainerAllowed(securedInfo, exposureId, uuid);
            apiJwtService.experimentAllowed(securedInfo, experimentId);
            apiJwtService.exposureAllowed(securedInfo, experimentId, exposureId);
            newExposure = apiJwtService.exposureAllowed(securedInfo, experimentId, messageContainerDto.getExposureId());
        } catch (BadTokenException | ExperimentNotMatchingException | ExposureNotMatchingException | MessageContainerOwnerNotMatchingException | MessageContainerNotMatchingException | MessageContainerNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            return new ResponseEntity<>(messageContainerService.move(newExposure, messageContainer), HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{uuid}/duplicate")
    public ResponseEntity<MessageContainerDto> duplicate(@PathVariable long experimentId, @PathVariable long exposureId, @PathVariable UUID uuid, HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apiJwtService.extractValues(req, false);

        if (!apiJwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Exposure exposure;
        MessageContainer messageContainer;

        try {
            messageContainer = apiJwtService.messagingContainerAllowed(securedInfo, exposureId, uuid);
            apiJwtService.experimentAllowed(securedInfo, experimentId);
            exposure = apiJwtService.exposureAllowed(securedInfo, experimentId, exposureId);
        } catch (BadTokenException | ExperimentNotMatchingException | ExposureNotMatchingException | MessageContainerOwnerNotMatchingException | MessageContainerNotMatchingException | MessageContainerNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            return new ResponseEntity<>(messageContainerService.duplicate(messageContainer, exposure), HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

}
