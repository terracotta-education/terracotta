package edu.iu.terracotta.controller.app.messaging;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import edu.iu.terracotta.dao.entity.messaging.message.Message;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExposureNotMatchingException;
import edu.iu.terracotta.dao.model.dto.messaging.content.MessageContentDto;
import edu.iu.terracotta.dao.model.dto.messaging.message.MessageDto;
import edu.iu.terracotta.dao.model.dto.messaging.preview.MessagePreviewDto;
import edu.iu.terracotta.dao.model.dto.messaging.rule.MessageRuleAssignmentDto;
import edu.iu.terracotta.dao.model.dto.messaging.send.MessageSendTestDto;
import edu.iu.terracotta.exceptions.BadTokenException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerNotFoundException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageContainerOwnerNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageContentNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageNotFoundException;
import edu.iu.terracotta.exceptions.messaging.MessageNotMatchingException;
import edu.iu.terracotta.exceptions.messaging.MessageOwnerNotMatchingException;
import edu.iu.terracotta.service.app.messaging.MessageContentService;
import edu.iu.terracotta.service.app.messaging.MessageEmailService;
import edu.iu.terracotta.service.app.messaging.MessagePreviewService;
import edu.iu.terracotta.service.app.messaging.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@SuppressWarnings({"PMD.GuardLogStatement"})
@RequestMapping(value = MessageController.REQUEST_ROOT, produces = MediaType.APPLICATION_JSON_VALUE)
public class MessageController {

    public static final String REQUEST_ROOT = "api/experiments/{experimentId}/exposures/{exposureId}/messaging/container/{containerUuid}/message";

    @Autowired private ApiJwtService apiJwtService;
    @Autowired private MessageContentService contentService;
    @Autowired private MessageEmailService messageEmailService;
    @Autowired private MessagePreviewService previewService;
    @Autowired private MessageService messageService;

    @PutMapping("/{uuid}")
    public ResponseEntity<MessageDto> put(@PathVariable long experimentId, @PathVariable long exposureId, @PathVariable UUID containerUuid, @PathVariable UUID uuid, @RequestBody MessageDto messageDto, HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apiJwtService.extractValues(req, false);

        if (!apiJwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        MessageContainer messageContainer;
        Message message;

        try {
            apiJwtService.experimentAllowed(securedInfo, experimentId);
            apiJwtService.exposureAllowed(securedInfo, experimentId, exposureId);
            messageContainer = apiJwtService.messagingContainerAllowed(securedInfo, exposureId, containerUuid);
            message = apiJwtService.messagingAllowed(securedInfo, containerUuid, uuid);
        } catch (BadTokenException | ExperimentNotMatchingException | MessageOwnerNotMatchingException | MessageNotMatchingException | MessageContainerOwnerNotMatchingException | MessageContainerNotMatchingException | ExposureNotMatchingException | MessageContainerNotFoundException | MessageNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            return new ResponseEntity<>(messageService.put(messageDto, exposureId, messageContainer, message), HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/assignments")
    public ResponseEntity<List<MessageRuleAssignmentDto>> getAssignments(@PathVariable long experimentId, @PathVariable long exposureId, @PathVariable long containerUuid, HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apiJwtService.extractValues(req, false);

        if (!apiJwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            return new ResponseEntity<>(messageService.getAssignments(securedInfo), HttpStatus.OK);
        } catch (ApiException | TerracottaConnectorException e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{uuid}/preview")
    public ResponseEntity<MessagePreviewDto> preview(@PathVariable long experimentId, @PathVariable long exposureId, @PathVariable UUID containerUuid, @PathVariable UUID uuid, @RequestBody MessagePreviewDto messagePreviewDto, HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apiJwtService.extractValues(req, false);

        if (!apiJwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }


        Message message = null;

        try {
            apiJwtService.experimentAllowed(securedInfo, experimentId);
            apiJwtService.exposureAllowed(securedInfo, experimentId, exposureId);
            apiJwtService.messagingContainerAllowed(securedInfo, exposureId, containerUuid);
            message = apiJwtService.messagingAllowed(securedInfo, containerUuid, uuid);
        } catch (BadTokenException | ExperimentNotMatchingException | MessageOwnerNotMatchingException | MessageNotMatchingException | MessageContainerOwnerNotMatchingException | MessageContainerNotMatchingException | ExposureNotMatchingException | MessageContainerNotFoundException | MessageNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            return new ResponseEntity<>(previewService.preview(messagePreviewDto, message), HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{uuid}/sendtest")
    public ResponseEntity<Void> sendTest(@PathVariable long experimentId, @PathVariable long exposureId, @PathVariable UUID containerUuid, @PathVariable UUID uuid, @RequestBody MessageSendTestDto messageSendTestDto, HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apiJwtService.extractValues(req, false);

        if (!apiJwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Message message = null;

        try {
            apiJwtService.experimentAllowed(securedInfo, experimentId);
            apiJwtService.exposureAllowed(securedInfo, experimentId, exposureId);
            apiJwtService.messagingContainerAllowed(securedInfo, exposureId, containerUuid);
            message = apiJwtService.messagingAllowed(securedInfo, containerUuid, uuid);
        } catch (BadTokenException | ExperimentNotMatchingException | MessageOwnerNotMatchingException | MessageNotMatchingException | MessageContainerOwnerNotMatchingException | MessageContainerNotMatchingException | ExposureNotMatchingException | MessageContainerNotFoundException | MessageNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            messageEmailService.sendTest(message, messageSendTestDto);

            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{messageUuid}/content/{uuid}/piped/file")
    public ResponseEntity<MessageDto> pipedTextCsv(@PathVariable long experimentId, @PathVariable long exposureId, @PathVariable UUID containerUuid, @PathVariable UUID messageUuid, @PathVariable UUID uuid, @RequestParam MultipartFile file, HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apiJwtService.extractValues(req, false);

        if (!apiJwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Message message = null;

        try {
            apiJwtService.experimentAllowed(securedInfo, experimentId);
            apiJwtService.exposureAllowed(securedInfo, experimentId, exposureId);
            apiJwtService.messagingContainerAllowed(securedInfo, exposureId, containerUuid);
            message = apiJwtService.messagingAllowed(securedInfo, containerUuid, messageUuid);
            apiJwtService.messagingContentAllowed(securedInfo, messageUuid, uuid);
        } catch (BadTokenException | ExperimentNotMatchingException | MessageOwnerNotMatchingException | MessageNotMatchingException | MessageContainerOwnerNotMatchingException | MessageContainerNotMatchingException | ExposureNotMatchingException | MessageContainerNotFoundException | MessageNotFoundException | MessageContentNotMatchingException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            return new ResponseEntity<>(messageService.processPipedTextCsvFile(message, file), HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{messageUuid}/content/{uuid}/piped/updatePlaceholders")
    public ResponseEntity<MessageContentDto> updatePlaceholders(@PathVariable long experimentId, @PathVariable long exposureId, @PathVariable UUID containerUuid, @PathVariable UUID messageUuid, @PathVariable UUID uuid, @RequestBody MessageContentDto contentDto, HttpServletRequest req) throws NumberFormatException, TerracottaConnectorException {
        SecuredInfo securedInfo = apiJwtService.extractValues(req, false);

        if (!apiJwtService.isInstructorOrHigher(securedInfo)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        MessageContent content;

        try {
            apiJwtService.experimentAllowed(securedInfo, experimentId);
            apiJwtService.exposureAllowed(securedInfo, experimentId, exposureId);
            apiJwtService.messagingContainerAllowed(securedInfo, exposureId, containerUuid);
            apiJwtService.messagingAllowed(securedInfo, containerUuid, messageUuid);
            content = apiJwtService.messagingContentAllowed(securedInfo, messageUuid, uuid);
        } catch (BadTokenException | ExperimentNotMatchingException | MessageOwnerNotMatchingException | MessageNotMatchingException | MessageContainerOwnerNotMatchingException | MessageContainerNotMatchingException | ExposureNotMatchingException | MessageContainerNotFoundException | MessageNotFoundException | MessageContentNotMatchingException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        try {
            return new ResponseEntity<>(contentService.updatePlaceholders(content, contentDto), HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

}
