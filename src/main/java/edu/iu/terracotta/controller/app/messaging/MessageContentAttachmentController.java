package edu.iu.terracotta.controller.app.messaging;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.dao.model.dto.messaging.content.MessageContentAttachmentDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerNotFoundException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerOwnerNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageContentNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageNotFoundException;
import edu.iu.terracotta.exceptions.messaging.MessageNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageOwnerNotMatchingException;
import edu.iu.terracotta.service.app.messaging.MessageContentAttachmentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@SuppressWarnings({"PMD.GuardLogStatement"})
@RequestMapping(value = MessageContentAttachmentController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class MessageContentAttachmentController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/exposures/{exposureId}/messaging/container/{containerUuid}/message/{messageUuid}/content/{contentUuid}/file";

    @Autowired private ApiJwtService apiJwtService;
    @Autowired private MessageContentAttachmentService messageContentAttachmentService;

    @GetMapping
    public ResponseEntity<List<MessageContentAttachmentDto>> get(@PathVariable long experimentId, @PathVariable long exposureId, @PathVariable UUID containerUuid, @PathVariable UUID messageUuid, @PathVariable UUID contentUuid, HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apiJwtService.extractValues(req, false);

        if (!apiJwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        MessageContent messageContent;

        try {
            apiJwtService.messagingContainerAllowed(securedInfo, exposureId, containerUuid);
            apiJwtService.messagingAllowed(securedInfo, containerUuid, messageUuid);
            messageContent = apiJwtService.messagingContentAllowed(securedInfo, messageUuid, contentUuid);
            apiJwtService.experimentAllowed(securedInfo, experimentId);
            apiJwtService.exposureAllowed(securedInfo, experimentId, exposureId);
        } catch (BadTokenException | ExperimentNotMatchingException | MessageOwnerNotMatchingException | MessageNotMatchingException | MessageContainerOwnerNotMatchingException | MessageContainerNotMatchingException | ExposureNotMatchingException | MessageContainerNotFoundException | MessageNotFoundException | MessageContentNotMatchingException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            return new ResponseEntity<>(messageContentAttachmentService.get(messageContent), HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

}
